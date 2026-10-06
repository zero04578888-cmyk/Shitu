package com.shitu.app.game

import com.shitu.app.model.Faction
import com.shitu.app.model.NpcProfile
import com.shitu.app.model.NpcAttitude
import com.shitu.app.model.NpcStatus
import com.shitu.app.model.PoliticalEffect
import com.shitu.app.model.RelationshipState

/**
 * 关系计算。
 *
 * 设计原则：不能做成「点一下加一点」。
 * 1. 正向收益随关系升高而递减，关系越好越难再往上走；
 * 2. 靠拢某个势力会同步得罪与它对立的一方；
 * 3. 送礼、结交会累积官场风险，风险本身有代价。
 *
 * 所有数值都是游戏模拟参数。
 */
object RelationshipEngine {

    /** 关系每提高一档，边际收益递减。 */
    fun scaledGain(base: Int, current: Int): Int {
        if (base <= 0) return base
        val room = RelationshipState.RELATION_MAX - current
        val scaled = base * room / RelationshipState.RELATION_MAX
        return scaled.coerceIn(1, base * 2)
    }

    /** 把一次政治影响应用到关系状态上，包含势力之间的连带反应。 */
    fun applyEffect(state: RelationshipState, effect: PoliticalEffect): RelationshipState {
        if (effect.isEmpty) return state
        var result = state
        effect.relations.forEach { (npcId, delta) ->
            result = result.withRelation(npcId, result.relationOf(npcId) + delta)
        }
        effect.trust.forEach { (npcId, delta) ->
            result = result.withTrust(npcId, result.trustOf(npcId) + delta)
        }
        result = shiftFactions(result, effect.factions)
        if (effect.risk != 0) result = result.withRisk(result.risk + effect.risk)
        effect.secrets.forEach { npcId -> result = result.withSecret(npcId) }
        effect.statuses.forEach { (npcId, status) -> result = result.withStatus(npcId, status) }
        return result
    }

    /**
     * 势力关系变化。靠拢一方时，与它对立的一方会同步下降。
     * 这保证「支持某一方」是一个有代价的决定。
     */
    fun shiftFactions(
        state: RelationshipState,
        changes: Map<Faction, Int>
    ): RelationshipState {
        if (changes.isEmpty()) return state
        val combined = LinkedHashMap<Faction, Int>()
        changes.forEach { (faction, delta) ->
            combined[faction] = (combined[faction] ?: 0) + delta
            if (delta > 0) {
                val backlash = -(delta / 2)
                if (backlash != 0) {
                    faction.rivals.forEach { rival ->
                        combined[rival] = (combined[rival] ?: 0) + backlash
                    }
                }
            }
        }
        var result = state
        combined.forEach { (faction, delta) ->
            if (delta != 0) result = result.withFaction(faction, result.factionOf(faction) + delta)
        }
        return result
    }

    /** 风险每季度自然消退一点，做过的事会慢慢被淡忘。 */
    const val RISK_DECAY = 1

    fun riskLabel(risk: Int): String = when {
        risk >= 80 -> "很高"
        risk >= 60 -> "偏高"
        risk >= 35 -> "需要留意"
        risk >= 15 -> "略有"
        else -> "干净"
    }

    fun attitudeOf(value: Int): NpcAttitude = NpcAttitude.of(value)

    /** 人物是否能帮上忙（关系与信任同时达标）。 */
    fun canHelp(state: RelationshipState, npc: NpcProfile): Boolean =
        state.relationOf(npc.id) >= 20 &&
            state.trustOf(npc.id) >= 30 &&
            !state.statusOf(npc.id).outOfReach

    /** 人物当前是否对你明显不满。仅用于界面提示，不影响任何晋升判定。 */
    fun canHinder(state: RelationshipState, npc: NpcProfile): Boolean =
        state.relationOf(npc.id) <= -20

    /** 最强的势力关系，用于晋升判定。 */
    fun bestFactionRelation(state: RelationshipState): Int =
        Faction.entries.maxOfOrNull { state.factionOf(it) } ?: 0

    /** 关系最好的人物能提供的助力。 */
    fun bestNpcSupport(state: RelationshipState): Int =
        NpcCatalog.npcs.maxOfOrNull { npc ->
            val relation = state.relationOf(npc.id)
            val status = state.statusOf(npc.id)
            if (relation >= 40 && state.trustOf(npc.id) >= 40 && !status.outOfReach) {
                relation * effectiveInfluence(state, npc) / 100
            } else {
                0
            }
        } ?: 0

    /** 静态影响力加上动态状态后的实际影响力。 */
    fun effectiveInfluence(state: RelationshipState, npc: NpcProfile): Int =
        (npc.influence + state.statusOf(npc.id).influenceDelta).coerceIn(0, 100)

    /** 状态在界面上的说明。 */
    fun statusNote(status: NpcStatus): String = when (status) {
        NpcStatus.NORMAL -> "目前一切如常。"
        NpcStatus.TRUSTED -> "正在被重用，说话比之前管用。"
        NpcStatus.DISTANT -> "最近被冷落，找他办事要谨慎。"
        NpcStatus.INVESTIGATED -> "正在被调查，这时候最好离远一点。"
        NpcStatus.PROMOTED -> "已经高升，位置更高了。"
        NpcStatus.TRANSFERRED -> "已经调离本地，联系少了。"
    }
}
