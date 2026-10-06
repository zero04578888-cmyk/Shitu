package com.shitu.app.game

import com.shitu.app.model.Faction
import com.shitu.app.model.PoliticalImpact
import com.shitu.app.model.PoliticalState
import com.shitu.app.model.PoliticalTendency

/**
 * 政治引擎：派系格局、政治资源、情报与联盟。
 *
 * 设计原则：政治资源与声望都不是可以无限刷的货币，
 * 而是「治理 → 政绩 → 评价 → 资源 → 机会」这条因果链的中间产物。
 *
 * 本引擎不参与任何比较、排名或岗位争夺判定：
 * 玩家能否晋升只取决于自己的条件是否达标，其他 NPC 的存在不会卡住玩家。
 * 所有数值都是游戏模拟参数。
 */
object PoliticsEngine {

    fun initial(): PoliticalState = PoliticalState(
        capital = 20,
        factionInfluence = mapOf(
            Faction.LOCAL to 24,
            Faction.PRAGMATIC to 18,
            Faction.REFORM to 16,
            Faction.CONSERVATIVE to 18,
            Faction.CORE to 14,
            Faction.NEUTRAL to 10
        )
    )

    /** 官场声望：政绩、民望、人物评价、派系评价综合而成。 */
    fun reputation(state: GameState): Int {
        val attrs = state.player.attributes
        val gov = state.governance
        val faction = RelationshipEngine.bestFactionRelation(state.relationship).coerceAtLeast(0)
        return (
            attrs.leaderTrust * 20 + attrs.colleagues * 12 + attrs.reputation * 12 +
                faction * 14 + gov.merit.overall * 30 + gov.jurisdiction.approval * 12
            ) / 100
    }

    fun reputationLabel(value: Int): String = when {
        value >= 80 -> "举足轻重"
        value >= 65 -> "有分量"
        value >= 45 -> "稳步上升"
        value >= 30 -> "还在积累"
        else -> "边缘"
    }

    /** 政治资源每季度自然积累，来自声望、势力与联盟。 */
    fun capitalGain(state: GameState): Int {
        val rep = reputation(state)
        val faction = RelationshipEngine.bestFactionRelation(state.relationship).coerceAtLeast(0)
        val alliance = state.politics.activeAlliances(state.absoluteQuarter).size
        return (rep / 30 + faction / 40 + alliance).coerceIn(0, 4)
    }

    /**
     * 人物与派系带来的助力，0 ~ 20。
     *
     * 只作为加分项存在：关系好会让晋升更顺，关系普通也不会阻碍正常晋升，
     * 更不会有人抢走岗位。
     */
    fun relationshipSupport(state: GameState): Int {
        val faction = (RelationshipEngine.bestFactionRelation(state.relationship) / 8).coerceIn(0, 12)
        val npc = (RelationshipEngine.bestNpcSupport(state.relationship) / 12).coerceIn(0, 8)
        return (faction + npc).coerceIn(0, MAX_RELATIONSHIP_SUPPORT)
    }

    /** 把一次选择的政治影响落到政治上。历史的写入在 GameEngine 里完成。 */
    fun applyChoice(state: GameState, impact: PoliticalImpact): PoliticalState {
        if (impact.isEmpty) return state.politics
        var politics = state.politics
        if (impact.spendCapital > 0) politics = politics.withCapital(politics.capital - impact.spendCapital)
        if (impact.capital != 0) politics = politics.withCapital(politics.capital + impact.capital)
        impact.tendencies.forEach { (tendency, delta) ->
            if (delta != 0) politics = politics.withTendency(tendency, delta)
        }
        impact.factionInfluence.forEach { (faction, delta) ->
            if (delta != 0) politics = politics.withInfluence(faction, delta)
        }
        impact.intelId?.let { id ->
            IntelCatalog.find(id)?.let { item ->
                politics = politics.withIntel(item.copy(year = state.year, quarter = state.quarter))
            }
        }
        impact.startAlliance?.let { id ->
            AllianceCatalog.find(id)?.let { template ->
                politics = politics.withAlliance(AllianceCatalog.toAlliance(template, state.absoluteQuarter))
            }
        }
        impact.endAlliance?.let { id -> politics = politics.withoutAlliance(id) }
        impact.startCrisis?.let { id ->
            CrisisCatalog.find(id)?.let { crisis ->
                politics = politics.copy(crisis = crisis.copy(year = state.year))
            }
        }
        if (impact.resolveCrisis) politics = politics.copy(crisis = null)
        return politics
    }

    /** 年度结算：资源积累、联盟到期、派系格局漂移。 */
    fun applyYearTick(state: GameState, rng: GameRng): Pair<PoliticalState, List<String>> {
        var politics = state.politics
        val notes = mutableListOf<String>()

        val gain = capitalGain(state) * 2
        if (gain > 0) {
            politics = politics.withCapital(politics.capital + gain)
            notes += "一年的经营让你积累了 $gain 点政治资源。"
        }

        val expired = politics.alliances.filterNot { it.isActive(state.absoluteQuarter) }
        if (expired.isNotEmpty()) {
            politics = politics.copy(alliances = politics.alliances.filter { it.isActive(state.absoluteQuarter) })
            notes += expired.joinToString("、") { it.label } + "到期，关系回到平常。"
        }

        // 玩家的长期路线会让对应派系的话语权上升，对立派系相应下降。
        val favored = when (politics.dominantTendency) {
            PoliticalTendency.PRAGMATIC -> Faction.PRAGMATIC
            PoliticalTendency.REFORMIST -> Faction.REFORM
            PoliticalTendency.STEADY -> Faction.CONSERVATIVE
            PoliticalTendency.BALANCED -> Faction.NEUTRAL
        }
        politics = politics.withInfluence(favored, 1 + rng.nextInt(2))
        favored.rivals.forEach { rival -> politics = politics.withInfluence(rival, -1) }
        return politics to notes
    }

    /**
     * 年末判断是否触发政治危机。同一时间只保留一场危机。
     *
     * 只看玩家自身的风险与治理状态，与他人表现无关。
     */
    fun crisisAfterYear(state: GameState): String? {
        if (state.politics.crisis != null) return null
        return when {
            state.relationship.risk >= 65 -> "crisis_doubt"
            state.player.attributes.integrityRisk >= 70 -> "crisis_integrity"
            state.governance.jurisdiction.approval <= 30 -> "crisis_opinion"
            else -> null
        }
    }

    const val MAX_RELATIONSHIP_SUPPORT = 20
}