package com.shitu.app.game

import com.shitu.app.model.Attr
import com.shitu.app.model.Faction
import com.shitu.app.model.HiddenKey
import com.shitu.app.model.NpcProfile
import com.shitu.app.model.NpcTier
import com.shitu.app.model.PoliticalEffect
import com.shitu.app.model.RelationshipState

/** 官场互动动作。 */
enum class Interaction(
    val label: String,
    val description: String,
    val energyCost: Int
) {
    VISIT("拜访", "登门坐一会儿，关系会好一些", 4),
    CONSULT("请教", "请教政策与业务，第一次有实质收获", 5),
    GIFT("送礼", "短期拉近关系，但会留下风险", 3),
    ASK_HELP("求助", "关系够硬时，借用对方一份力", 3),
    REPORT("汇报", "向上级汇报工作，让领导记得你", 3),
    BEFRIEND("结交", "主动靠拢对方所在的圈子", 4),
    REFUSE("拒绝合作", "划清界限避开麻烦，但会得罪人", 0),
    OBSERVE("观察态度", "摸清对方底细和真实态度", 1)
}

/** 一次互动的结果。 */
data class InteractionResult(
    val state: GameState,
    val message: String
)

/**
 * 官场互动。
 *
 * 互动不是免费按钮：每一次都消耗精力，送礼还要花钱和承担风险，
 * 关系越好收益越小，结交一方会得罪另一方。
 */
object PoliticalEngine {

    const val INITIAL_TRUST = 25
    const val GIFT_COOLDOWN_QUARTERS = 4
    const val CONSULT_ONCE_COOLDOWN = 10000
    const val GIFT_RISK = 6
    const val GIFT_WEALTH_COST = 4
    const val ASK_HELP_RELATION = 40

    /**
     * 开局关系：所有人对你都是「普通」，信任度从同一水平起步；
     * 起步单位决定你天然更靠近哪股势力。
     */
    fun initialRelationship(unit: com.shitu.app.model.UnitType): RelationshipState {
        val trust = NpcCatalog.npcs.associate { it.id to INITIAL_TRUST }
        val factions = when (unit) {
            com.shitu.app.model.UnitType.TOWN,
            com.shitu.app.model.UnitType.STREET -> mapOf(Faction.LOCAL to 10)
            com.shitu.app.model.UnitType.COUNTY -> mapOf(Faction.PRAGMATIC to 8)
            com.shitu.app.model.UnitType.BUSINESS -> mapOf(Faction.PRAGMATIC to 6)
            com.shitu.app.model.UnitType.CITY,
            com.shitu.app.model.UnitType.GENERAL -> mapOf(Faction.CORE to 8)
        }
        return RelationshipState(trust = trust, factions = factions)
    }

    fun canReport(npc: NpcProfile): Boolean =
        npc.tier == NpcTier.COUNTY_LEADER ||
            npc.tier == NpcTier.CITY_LEADER ||
            npc.tier == NpcTier.ORGANIZATION ||
            npc.faction == Faction.CORE

    /** 返回不能执行的原因；可以执行时返回 null。 */
    fun unavailabilityReason(
        state: GameState,
        npc: NpcProfile,
        action: Interaction
    ): String? {
        val attrs = state.player.attributes
        val rel = state.relationship
        return when {
            attrs.energy < action.energyCost -> "精力不足"
            action == Interaction.GIFT && attrs.wealth < GIFT_WEALTH_COST -> "手头不宽裕"
            action == Interaction.GIFT && !rel.isReady("gift", npc.id, state.absoluteQuarter) ->
                "最近送得太勤，先缓一缓"
            action == Interaction.ASK_HELP && rel.relationOf(npc.id) < ASK_HELP_RELATION ->
                "关系还不到能开口的程度"
            action == Interaction.REPORT && !canReport(npc) -> "对方不是你能汇报的对象"
            else -> null
        }
    }

    fun isAvailable(state: GameState, npc: NpcProfile, action: Interaction): Boolean =
        unavailabilityReason(state, npc, action) == null

    fun availableActions(state: GameState, npc: NpcProfile): List<Interaction> =
        Interaction.entries.filter { isAvailable(state, npc, it) }

    fun interact(state: GameState, npcId: String, action: Interaction): InteractionResult {
        val npc = NpcCatalog.find(npcId)
            ?: return InteractionResult(state, "没有找到这个人。")
        unavailabilityReason(state, npc, action)?.let { reason ->
            return InteractionResult(state, "现在做不了：$reason。")
        }

        var attrs = state.player.attributes
        var hidden = state.player.hidden
        var rel = state.relationship
        val message: String

        fun spendEnergy(cost: Int) {
            attrs = attrs.withValue(Attr.ENERGY, attrs.energy - cost)
        }

        fun gainRelation(base: Int) {
            val current = rel.relationOf(npc.id)
            rel = rel.withRelation(npc.id, current + RelationshipEngine.scaledGain(base, current))
        }

        fun gainTrust(delta: Int) {
            rel = rel.withTrust(npc.id, rel.trustOf(npc.id) + delta)
        }

        fun shiftFaction(faction: Faction, delta: Int) {
            rel = RelationshipEngine.shiftFactions(rel, mapOf(faction to delta))
        }

        when (action) {
            Interaction.VISIT -> {
                spendEnergy(action.energyCost)
                gainRelation(6)
                gainTrust(2)
                message = "你在${npc.name}办公室坐了大半个小时，聊的都是家常。"
            }

            Interaction.CONSULT -> {
                spendEnergy(action.energyCost)
                val firstTime = rel.isReady("consult", npc.id, state.absoluteQuarter)
                if (firstTime) {
                    attrs = attrs
                        .withValue(Attr.POLITICS, attrs.politics + 2)
                        .withValue(Attr.ABILITY, attrs.ability + 1)
                    gainTrust(5)
                    gainRelation(3)
                    rel = rel.withCooldown(
                        "consult",
                        npc.id,
                        state.absoluteQuarter + CONSULT_ONCE_COOLDOWN
                    )
                    message = "${npc.name}把当年踩过的坑讲给你听，你记了两页纸。"
                } else {
                    gainTrust(4)
                    gainRelation(2)
                    message = "你又问了几个具体问题，对方答得很简短。"
                }
            }

            Interaction.GIFT -> {
                spendEnergy(action.energyCost)
                attrs = attrs.withValue(Attr.WEALTH, attrs.wealth - GIFT_WEALTH_COST)
                gainRelation(10)
                gainTrust(1)
                rel = rel.withRisk(rel.risk + GIFT_RISK)
                rel = rel.withCooldown(
                    "gift",
                    npc.id,
                    state.absoluteQuarter + GIFT_COOLDOWN_QUARTERS
                )
                message = "东西不贵，但意思到了。${npc.name}推辞了两句，还是收下了。"
            }

            Interaction.ASK_HELP -> {
                spendEnergy(action.energyCost)
                val current = rel.relationOf(npc.id)
                rel = rel.withRelation(npc.id, current - 3)
                gainTrust(-1)
                attrs = attrs.withValue(Attr.LEADER_TRUST, attrs.leaderTrust + 3)
                hidden = hidden.withValue(HiddenKey.MENTOR, hidden.mentor + 3)
                if (npc.influence >= 60) {
                    hidden = hidden.withValue(HiddenKey.OPPORTUNITY, hidden.opportunity + 5)
                }
                message = "${npc.name}没有多问，只说了一句「到时候我提一句」。"
            }

            Interaction.REPORT -> {
                spendEnergy(action.energyCost)
                attrs = attrs.withValue(Attr.LEADER_TRUST, attrs.leaderTrust + 2)
                gainRelation(2)
                shiftFaction(npc.faction, 1)
                message = "你把最近的进度讲了一遍，${npc.name}听完点了点头。"
            }

            Interaction.BEFRIEND -> {
                spendEnergy(action.energyCost)
                gainRelation(8)
                gainTrust(2)
                shiftFaction(npc.faction, 2)
                message = buildString {
                    append("你主动凑了过去，圈子里的门算是推开了一条缝。")
                    if (npc.faction.rivals.isNotEmpty()) append("有人看在眼里，记在了心里。")
                }
            }

            Interaction.REFUSE -> {
                val current = rel.relationOf(npc.id)
                rel = rel.withRelation(npc.id, current - 12)
                gainTrust(-8)
                shiftFaction(npc.faction, -2)
                rel = rel.withRisk(rel.risk - 3)
                message = "你把话说清楚了。${npc.name}脸上的笑淡了一层。"
            }

            Interaction.OBSERVE -> {
                spendEnergy(action.energyCost)
                rel = rel.withScouted(npc.id)
                val attitude = rel.attitudeOf(npc.id).label
                val secretNote = if (rel.knowsSecret(npc.id)) {
                    "对方手里似乎捏着你的一些旧事。"
                } else {
                    "暂时看不出对方手里有你的把柄。"
                }
                message = "你摸清了${npc.name}的底细：目前态度是「$attitude」。$secretNote"
            }
        }

        val updated = state.copy(
            player = state.player.copy(attributes = attrs, hidden = hidden),
            relationship = rel
        )
        return InteractionResult(updated, message)
    }

    /** 季度自然消退：风险会被时间冲淡，但不会自动归零。 */
    fun settleRisk(state: GameState): GameState {
        val rel = state.relationship
        if (rel.risk <= 0) return state
        return state.copy(
            relationship = rel.withRisk(rel.risk - RelationshipEngine.RISK_DECAY)
        )
    }

    /** 风险带来的季度后果：关系松动、领导信任下降。 */
    fun settleConsequences(state: GameState): Pair<GameState, List<String>> {
        val rel = state.relationship
        val notes = mutableListOf<String>()
        var attrs = state.player.attributes
        when {
            rel.risk >= 80 -> {
                attrs = attrs
                    .withValue(Attr.LEADER_TRUST, attrs.leaderTrust - 4)
                    .withValue(Attr.REPUTATION, attrs.reputation - 3)
                notes += "关于你的说法已经传开了，领导开始有意回避你。"
            }
            rel.risk >= 60 -> {
                attrs = attrs.withValue(Attr.LEADER_TRUST, attrs.leaderTrust - 2)
                notes += "有些风言风语传到了领导那里。"
            }
        }
        if (notes.isEmpty()) return state to emptyList()
        return state.copy(player = state.player.copy(attributes = attrs)) to notes
    }
}
