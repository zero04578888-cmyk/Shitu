package com.shitu.app.game

import com.shitu.app.model.AchievementCatalog
import com.shitu.app.model.Attr
import com.shitu.app.model.CareerAbility
import com.shitu.app.model.CareerMemory
import com.shitu.app.model.CareerProfile
import com.shitu.app.model.CareerRecord
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.Choice
import com.shitu.app.model.LifeDimension
import com.shitu.app.model.LifeEvaluation
import com.shitu.app.model.PoliticalTendency

/**
 * 第十阶段：仕途人生引擎。
 *
 * 负责个人成长（经验与能力）、履历、人物记忆、人生评价与成就。
 * 这里不改变时间的推进方式（仍按季度结算），只把长期积累真正接进因果链。
 * 所有数值都是游戏模拟参数。
 */
object CareerEngine {

    fun initial(priorLives: Int = 0): CareerProfile = CareerProfile(priorLives = priorLives)

    // ===== 成长 =====

    /** 一次选择带来的经验值：做过实事、承担重要任务的经验更多。 */
    fun experienceForChoice(choice: Choice, major: Boolean): Int {
        val effect = choice.effects
        val base = 2 +
            (effect.work / 4) +
            (effect.ability / 4) +
            (effect.coordination / 5) +
            (effect.writing / 5)
        val importance = if (major) 4 else 0
        return (base + importance).coerceIn(1, 12)
    }

    /** 增加经验；跨过等级门槛时按路线提升对应能力（有上限）。 */
    fun gainExperience(state: GameState, amount: Int): GameState {
        if (amount <= 0) return state
        val before = state.career
        val after = before.copy(experience = before.experience + amount)
        val levels = after.level - before.level
        var grown = after
        if (levels > 0) {
            val primary = primaryAbility(state.route)
            repeat(levels) {
                CareerAbility.entries.forEach { ability ->
                    val bonus = if (ability == primary) 3 else 1
                    grown = grown.withValue(ability, grown.valueOf(ability) + bonus)
                }
            }
        }
        return state.copy(career = grown)
    }

    /** 成长的主导方向由发展路线决定。 */
    fun primaryAbility(route: CareerRoute?): CareerAbility = when (route) {
        CareerRoute.MANAGEMENT -> CareerAbility.LEADERSHIP
        CareerRoute.PROFESSIONAL -> CareerAbility.SPECIALTY
        CareerRoute.WRITING -> CareerAbility.COMMUNICATION
        CareerRoute.MASS_WORK -> CareerAbility.EXECUTION
        CareerRoute.ECONOMY_ROUTE -> CareerAbility.DECISION
        CareerRoute.ORGANIZATION_ROUTE -> CareerAbility.COMMUNICATION
        CareerRoute.REFORM_ROUTE -> CareerAbility.DECISION
        null -> CareerAbility.DECISION
    }

    // ===== 履历 =====

    fun record(
        state: GameState,
        kind: CareerRecordKind,
        title: String,
        detail: String,
        year: Int = state.year,
        quarter: Int = state.quarter,
        fromPosition: String = "",
        toPosition: String = ""
    ): GameState {
        val entry = CareerRecord(
            year = year,
            quarter = quarter,
            age = state.player.age,
            title = title,
            detail = detail,
            kind = kind,
            positionName = state.player.positionName,
            positionTitle = state.displayPositionTitle,
            regionName = state.governance.jurisdiction.name,
            fromPosition = fromPosition,
            toPosition = toPosition
        )
        return state.copy(career = state.career.withRecord(entry))
    }

    // ===== 人物记忆 =====

    fun remember(
        state: GameState,
        npcId: String,
        summary: String,
        relationBias: Int,
        trustBias: Int
    ): GameState {
        if (relationBias == 0 && trustBias == 0) return state
        val memory = CareerMemory(
            npcId = npcId,
            year = state.year,
            summary = summary,
            relationBias = relationBias,
            trustBias = trustBias
        )
        return state.copy(career = state.career.withMemory(memory))
    }

    /** 把选择里对人物的明显影响记成长期记忆。 */
    fun rememberFromChoice(state: GameState, choice: Choice, summary: String): GameState {
        var result = state
        choice.political.relations.forEach { (npcId, delta) ->
            if (delta >= 3 || delta <= -3) {
                result = remember(result, npcId, summary, delta / 2, 0)
            }
        }
        choice.political.trust.forEach { (npcId, delta) ->
            if (delta >= 5 || delta <= -5) {
                result = remember(result, npcId, summary, 0, delta / 2)
            }
        }
        return result
    }

    // ===== 岗位同步 =====

    fun syncPosition(state: GameState): GameState {
        val career = state.career
        val index = state.player.positionIndex
        val synced = if (index > career.highestPositionIndex) {
            state.copy(career = career.copy(highestPositionIndex = index))
        } else {
            state
        }
        // 第十六阶段：把当前职级同步到正在任职的城市履历段。
        val stints = synced.careerCities
        if (stints.isEmpty()) return synced
        val label = synced.displayPositionTitle
        val updated = stints.map { stint ->
            if (stint.isCurrent && stint.positionLabel != label) stint.copy(positionLabel = label) else stint
        }
        return if (updated == stints) synced else synced.copy(careerCities = updated)
    }

    /** 每季度重置个人行动次数。 */
    fun resetQuarter(state: GameState): GameState =
        state.copy(career = state.career.copy(personalActionsUsed = 0))

    // ===== 人生评价 =====

    fun evaluate(state: GameState): LifeEvaluation {
        val attrs = state.player.attributes
        val gov = state.governance
        val jurisdiction = gov.jurisdiction
        val reviews = state.reviews
        val reviewAvg = if (reviews.isEmpty()) 50 else reviews.map { it.score }.average().toInt()

        val governance = average(
            gov.merit.overall,
            jurisdiction.approval,
            jurisdiction.security,
            GovernanceEngine.governanceScore(state)
        )

        val ability = average(
            attrs.ability,
            attrs.work,
            state.career.overallAbility,
            reviewAvg
        )

        val reputation = average(
            attrs.reputation,
            PoliticsEngine.reputation(state),
            attrs.masses
        )

        val integrity = average(
            100 - attrs.integrityRisk,
            100 - state.relationship.risk,
            if (state.warningCount == 0) 100 else (100 - state.warningCount * 25).coerceAtLeast(0)
        )

        val leadership = average(
            attrs.leaderTrust,
            RelationshipEngine.bestFactionRelation(state.relationship).coerceAtLeast(0),
            RelationshipEngine.bestNpcSupport(state.relationship)
        )

        val npcAverage = if (NpcCatalog.npcs.isEmpty()) 0 else
            NpcCatalog.npcs.map { state.relationship.relationOf(it.id) }.average().toInt()
        val relations = average(
            attrs.colleagues,
            attrs.masses,
            (npcAverage + 100) / 2,
            factionAverage(state)
        )

        val family = average(attrs.family, attrs.health, state.player.hidden.stress.let { 100 - it })

        val career = average(
            state.player.positionIndex * 100 / (com.shitu.app.model.POSITION_LADDER.size - 1),
            (state.player.serviceYears * 100 / GameEngine.MAX_SERVICE_YEARS),
            state.promotionCount * 14,
            (state.career.records.size * 100 / 40),
            reviewAvg
        )

        return LifeEvaluation(
            governance = governance,
            ability = ability,
            reputation = reputation,
            integrity = integrity,
            leadership = leadership,
            relations = relations,
            family = family,
            career = career
        )
    }

    private fun factionAverage(state: GameState): Int {
        val values = com.shitu.app.model.Faction.entries.map { state.relationship.factionOf(it) }
        if (values.isEmpty()) return 50
        return ((values.average() + 100) / 2).toInt()
    }

    private fun average(vararg values: Int): Int =
        if (values.isEmpty()) 0 else (values.sum().toDouble() / values.size).toInt().coerceIn(0, 100)

    // ===== 成就 =====

    /** 检查并解锁新成就，返回更新后的状态。 */
    fun checkAchievements(state: GameState): GameState {
        val earned = earnedAchievements(state).filterNot { it in state.career.unlockedAchievements }
        if (earned.isEmpty()) return state
        var updated = state
        earned.forEach { id ->
            updated = updated.copy(career = updated.career.withAchievement(id))
            // 把解锁时间写进履历，成就页据此显示解锁年份。
            val def = AchievementCatalog.find(id) ?: return@forEach
            updated = record(updated, CareerRecordKind.ACHIEVEMENT, def.title, def.description)
        }
        return updated
    }

    private fun earnedAchievements(state: GameState): Set<String> {
        val attrs = state.player.attributes
        val gov = state.governance
        val career = state.career
        val politics = state.politics
        val result = LinkedHashSet<String>()

        if (state.promotionCount >= 1) result += "first_promotion"
        if (state.player.serviceYears >= 10) result += "decade_service"
        if (state.player.serviceYears >= GameEngine.MAX_SERVICE_YEARS) result += "thirty_years"
        if (state.player.positionIndex >= com.shitu.app.model.POSITION_LADDER.size - 1) {
            result += "top_position"
        }
        if (gov.merit.overall >= 80) result += "merit_master"
        if (gov.jurisdiction.approval >= 85) result += "approval_high"
        if (state.player.serviceYears >= 10 && attrs.integrityRisk <= 10 &&
            state.relationship.risk <= 15
        ) {
            result += "clean_record"
        }
        if (state.player.serviceYears >= 5 && state.player.hidden.stress <= 15) result += "stress_free"
        if (attrs.family >= 80) result += "family_first"
        if (attrs.family >= 70 && attrs.health >= 70 && state.player.positionIndex >= 3) {
            result += "balanced_master"
        }
        if (politics.tendencyOf(PoliticalTendency.REFORMIST) >= 10) result += "reform_pioneer"
        if (politics.tendencyOf(PoliticalTendency.STEADY) >= 10) result += "steady_hand"
        if (politics.tendencyOf(PoliticalTendency.PRAGMATIC) >= 10) result += "pragmatist"
        val closeNpcs = NpcCatalog.npcs.count { state.relationship.relationOf(it.id) >= 60 }
        if (closeNpcs >= 3) result += "network_weaver"
        if (RelationshipEngine.bestFactionRelation(state.relationship) >= 60) result += "faction_backing"
        if (state.flags.contains("resolved_crisis")) result += "crisis_survivor"
        if (politics.intel.size >= 5) result += "intel_master"
        if (politics.alliances.isNotEmpty()) result += "ally_builder"
        if (politics.majorChoices.size >= 5) result += "decision_maker"
        val realRecords = career.records.filterNot { it.kind == CareerRecordKind.ACHIEVEMENT }
        if (realRecords.size >= 20) result += "record_keeper"
        if (career.level >= CareerProfile.MAX_LEVEL) result += "ability_peak"
        if (career.positionsHeld >= 5) result += "veteran_posts"
        if (career.priorLives >= 1) result += "restart_life"
        if (realRecords.map { it.kind }.distinct().size >= 6) result += "all_rounder"

        return result
    }

    fun achievementItems(state: GameState) =
        AchievementCatalog.itemsOf(state.career.unlockedAchievements)
}
