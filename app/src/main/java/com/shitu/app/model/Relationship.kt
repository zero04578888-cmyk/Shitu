package com.shitu.app.model

/**
 * 官场关系状态。全部进入存档，读档后关系、势力、风险、事件进度都会保留。
 */
data class RelationshipState(
    /** npcId -> 关系值（-100 ~ +100） */
    val relations: Map<String, Int> = emptyMap(),
    /** npcId -> 信任度（0 ~ 100） */
    val trust: Map<String, Int> = emptyMap(),
    /** 势力 -> 关系值（-100 ~ +100） */
    val factions: Map<Faction, Int> = emptyMap(),
    /** 官场风险（0 ~ 100） */
    val risk: Int = 0,
    /** "动作:npcId" -> 可以再次使用的季度序号 */
    val cooldown: Map<String, Int> = emptyMap(),
    /** 掌握玩家某些底细的人物 */
    val secrets: Set<String> = emptySet(),
    /** 已经摸清底细的人物（可以看精确数值） */
    val scouted: Set<String> = emptySet(),
    /** npcId -> 动态状态 */
    val statuses: Map<String, NpcStatus> = emptyMap()
) {
    fun relationOf(npcId: String): Int = relations[npcId] ?: 0

    fun trustOf(npcId: String): Int = trust[npcId] ?: 0

    fun factionOf(faction: Faction): Int = factions[faction] ?: 0

    fun attitudeOf(npcId: String): NpcAttitude = NpcAttitude.of(relationOf(npcId))

    fun readyAt(action: String, npcId: String): Int = cooldown["$action:$npcId"] ?: 0

    fun isReady(action: String, npcId: String, absoluteQuarter: Int): Boolean =
        readyAt(action, npcId) <= absoluteQuarter

    fun knowsSecret(npcId: String): Boolean = npcId in secrets

    fun isScouted(npcId: String): Boolean = npcId in scouted

    fun statusOf(npcId: String): NpcStatus = statuses[npcId] ?: NpcStatus.NORMAL

    fun withRelation(npcId: String, value: Int): RelationshipState =
        copy(relations = relations + (npcId to value.coerceIn(RELATION_MIN, RELATION_MAX)))

    fun withTrust(npcId: String, value: Int): RelationshipState =
        copy(trust = trust + (npcId to value.coerceIn(TRUST_MIN, TRUST_MAX)))

    fun withFaction(faction: Faction, value: Int): RelationshipState =
        copy(factions = factions + (faction to value.coerceIn(RELATION_MIN, RELATION_MAX)))

    fun withRisk(value: Int): RelationshipState =
        copy(risk = value.coerceIn(RISK_MIN, RISK_MAX))

    fun withCooldown(action: String, npcId: String, readyAt: Int): RelationshipState =
        copy(cooldown = cooldown + ("$action:$npcId" to readyAt))

    fun withSecret(npcId: String): RelationshipState = copy(secrets = secrets + npcId)

    fun withScouted(npcId: String): RelationshipState = copy(scouted = scouted + npcId)

    fun withStatus(npcId: String, status: NpcStatus): RelationshipState =
        copy(statuses = statuses + (npcId to status))

    companion object {
        const val RELATION_MIN = -100
        const val RELATION_MAX = 100
        const val TRUST_MIN = 0
        const val TRUST_MAX = 100
        const val RISK_MIN = 0
        const val RISK_MAX = 100

        val EMPTY = RelationshipState()
    }
}

/** 一次选择对官场关系的影响。 */
data class PoliticalEffect(
    val relations: Map<String, Int> = emptyMap(),
    val trust: Map<String, Int> = emptyMap(),
    val factions: Map<Faction, Int> = emptyMap(),
    val risk: Int = 0,
    /** 让某些人物从此掌握你的底细。 */
    val secrets: Set<String> = emptySet(),
    /** 改变某些人物的动态状态。 */
    val statuses: Map<String, NpcStatus> = emptyMap()
) {
    val isEmpty: Boolean
        get() = relations.isEmpty() && trust.isEmpty() &&
            factions.isEmpty() && risk == 0 && secrets.isEmpty() && statuses.isEmpty()
}

/** 事件的官场前置条件。全部满足才会出现。 */
data class PoliticalGate(
    val minFaction: Map<Faction, Int> = emptyMap(),
    val minNpcRelation: Map<String, Int> = emptyMap(),
    val minRisk: Int? = null,
    val maxRisk: Int? = null
) {
    val isEmpty: Boolean
        get() = minFaction.isEmpty() && minNpcRelation.isEmpty() &&
            minRisk == null && maxRisk == null
}
