package com.shitu.app.game

import com.shitu.app.model.GameEvent
import com.shitu.app.model.EventTag
import com.shitu.app.model.JobTrack
import com.shitu.app.model.LifeStage
import com.shitu.app.model.RegionCatalog

// 事件调度：到期的链式事件优先，其次按权重随机，最后兜底。
object EventEngine {

    fun pickNext(state: GameState, rng: GameRng): GameEvent? {
        pickDueChainEvent(state)?.let { return it }
        pickMilestoneEvent(state)?.let { return it }
        pickRandom(state, rng)?.let { return it }
        return fallback(state)
    }

    // 里程碑事件：例如选择发展路线，满足条件时优先出现。
    fun pickMilestoneEvent(state: GameState): GameEvent? =
        eligibleEvents(state).firstOrNull { it.priority }

    // 到期的事件链节点。
    fun pickDueChainEvent(state: GameState): GameEvent? {
        val due = state.pendingChains
            .filter { it.dueQuarter <= state.absoluteQuarter }
            .sortedWith(compareBy({ it.dueQuarter }, { it.chainId }))
        for (chain in due) {
            val event = EventLibrary.stepsOf(chain.chainId)
                .firstOrNull { it.chainStep == chain.nextStep }
            if (event != null) return event
        }
        return null
    }

    // 符合条件的随机事件，按权重抽取。单位平台会提高对口事件的权重。
    fun pickRandom(state: GameState, rng: GameRng): GameEvent? {
        val candidates = eligibleEvents(state).filterNot { it.priority }
        if (candidates.isEmpty()) return null
        val recentTitles = state.log.takeLast(2).map { it.title }.toSet()
        val weights = candidates.map { event -> weightOf(state, event, recentTitles) }
        val total = weights.sum()
        var roll = rng.nextInt(total)
        for (index in candidates.indices) {
            roll -= weights[index]
            if (roll < 0) return candidates[index]
        }
        return candidates.last()
    }

    /**
     * 单个事件的最终抽取权重。公开出来便于测试与调优：
     * 基础权重 + 单位平台 + 发展路线 + 岗位条线 + 人生阶段 + 当前状态。
     */
    fun weightOf(state: GameState, event: GameEvent, recentTitles: Set<String> = emptySet()): Int {
        val unitAffinity = if (event.tags.any { it in state.player.unit.affinity }) 6 else 0
        val affinity = unitAffinity + RouteEngine.eventAffinity(state.route, event)
        // 第十二阶段：岗位条线与人生阶段都会让对口事件更常出现。
        val track = JobTrack.of(state.player.unit, state.route)
        val lifeStage = LifeStage.of(state.player.positionIndex, state.player.serviceYears, state.player.age)
        val stageTags = lifeStageTags(lifeStage)
        val trackAffinity = if (event.tags.any { it in track.tags }) TRACK_AFFINITY else 0
        val stageAffinity = if (event.tags.any { it in stageTags }) STAGE_AFFINITY else 0
        // 第十八阶段：玩家选定的具体职务会让对口事件更常出现。
        val positionTags = state.currentPositionTags
        val positionAffinity =
            if (positionTags.isNotEmpty() && event.tags.any { it in positionTags }) POSITION_AFFINITY else 0
        var weight = event.weight + affinity + trackAffinity + stageAffinity +
            positionAffinity + stateAffinity(state, event)
        // 避免同一件事连续出现。
        if (event.title in recentTitles) weight -= 5
        // 概率上限：任何单一事件都不能压过其他所有事件。
        return weight.coerceIn(
            1,
            event.weight + MAX_STATE_AFFINITY + TRACK_AFFINITY + STAGE_AFFINITY + POSITION_AFFINITY
        )
    }

    /**
     * 第十一阶段：按玩家当前状态动态调整事件概率。
     * 低民望更容易遇到投诉，低治安更容易遇到事故，高风险更容易遇到检查，
     * 政绩好则会遇到更多项目机会。每个方向都有上限，避免某一类事件刷屏。
     */
    private fun stateAffinity(state: GameState, event: GameEvent): Int {
        val attrs = state.player.attributes
        val jurisdiction = state.governance.jurisdiction
        var bonus = 0

        if (jurisdiction.approval <= 45 && event.tags.any { it in APPEAL_TAGS }) bonus += 6
        if (jurisdiction.security <= 45 && event.tags.any { it in SECURITY_TAGS }) bonus += 6
        if (jurisdiction.environment <= 45 && event.tags.any { it in ENVIRONMENT_TAGS }) bonus += 5
        if (attrs.integrityRisk >= 45 && event.tags.any { it in INTEGRITY_TAGS }) bonus += 6
        if (state.governance.finance <= 40 && event.tags.any { it in FINANCE_TAGS }) bonus += 6
        if (state.governance.merit.overall >= 60 && event.tags.any { it in OPPORTUNITY_TAGS }) bonus += 5
        if (attrs.family <= 40 && EventTag.FAMILY in event.tags) bonus += 5
        if (attrs.energy <= 25 && EventTag.OVERTIME in event.tags) bonus -= 4

        return bonus.coerceIn(-6, MAX_STATE_AFFINITY)
    }

    private const val MAX_STATE_AFFINITY = 8

    /** 岗位条线对口事件的权重加成。 */
    private const val TRACK_AFFINITY = 5

    /** 人生阶段对口事件的权重加成。 */
    private const val STAGE_AFFINITY = 3

    /** 具体职务对口事件的权重加成（第十八阶段：职位选择影响事件池）。 */
    private const val POSITION_AFFINITY = 4

    /** 不同人生阶段更常遇到的不同议题，避免 25 岁和 55 岁撞到同一批事。 */
    fun lifeStageTags(stage: LifeStage): Set<EventTag> = when (stage) {
        LifeStage.EARLY, LifeStage.GRASSROOTS -> setOf(
            EventTag.MATERIAL, EventTag.OVERTIME, EventTag.RURAL,
            EventTag.COMPLAINT, EventTag.PETITION
        )
        LifeStage.GROWTH -> setOf(
            EventTag.MATERIAL, EventTag.PROJECT, EventTag.RESEARCH, EventTag.COORDINATION
        )
        LifeStage.IMPORTANT, LifeStage.MID_CAREER -> setOf(
            EventTag.PROJECT, EventTag.ASSESSMENT, EventTag.CAREER,
            EventTag.MEETING, EventTag.INSPECTION
        )
        LifeStage.CORE, LifeStage.PEAK -> setOf(
            EventTag.CAREER, EventTag.ASSESSMENT, EventTag.INSPECTION,
            EventTag.ACCEPTANCE, EventTag.PUBLIC_OPINION
        )
        LifeStage.LATE -> setOf(
            EventTag.FAMILY, EventTag.HEALTH, EventTag.ACCEPTANCE, EventTag.CAREER
        )
    }

    /** 当前人生阶段（供测试与界面复用）。 */
    fun lifeStageOf(state: GameState): LifeStage =
        LifeStage.of(state.player.positionIndex, state.player.serviceYears, state.player.age)

    private val APPEAL_TAGS = setOf(
        EventTag.COMPLAINT, EventTag.MASS_APPEAL, EventTag.PETITION, EventTag.PUBLIC_OPINION
    )
    private val SECURITY_TAGS = setOf(
        EventTag.ACCIDENT, EventTag.EMERGENCY, EventTag.DISASTER, EventTag.FLOOD
    )
    private val ENVIRONMENT_TAGS = setOf(EventTag.DISASTER, EventTag.EMERGENCY, EventTag.RURAL)
    private val INTEGRITY_TAGS = setOf(EventTag.INSPECTION, EventTag.INTEGRITY, EventTag.GIFT)
    private val FINANCE_TAGS = setOf(EventTag.PROJECT, EventTag.RURAL, EventTag.LEADER_TASK)
    private val OPPORTUNITY_TAGS = setOf(EventTag.PROJECT, EventTag.LEADER_TASK, EventTag.RESEARCH)

    fun eligibleEvents(state: GameState): List<GameEvent> {
        val player = state.player
        val abs = state.absoluteQuarter
        return EventLibrary.randomPool.filter { event ->
            if (event.crisisOnly && state.politics.crisis == null) return@filter false
            if (event.minPositionIndex > player.positionIndex) return@filter false
            if (event.maxPositionIndex < player.positionIndex) return@filter false
            // 第十五阶段：职位权限域门控，低于要求权限的事件不会出现。
            if (!PositionEngine.canHandleEvent(state, event.minPositionIndex, event.requiredScope)) {
                return@filter false
            }
            if (event.units.isNotEmpty() && player.unit !in event.units) return@filter false
            // 第十六阶段：城市决定事件池，只有符合当前任职城市标签的事件才会出现。
            if (!meetsCityGate(state, event)) return@filter false
            if (event.repeatable) {
                val ready = state.eventCooldown[event.id] ?: 0
                if (ready > abs) return@filter false
            } else if (event.id in state.triggeredEvents) {
                return@filter false
            }
            val family = player.attributes.family
            if (event.minFamilyRelation != null && family < event.minFamilyRelation) return@filter false
            if (event.maxFamilyRelation != null && family > event.maxFamilyRelation) return@filter false
            if (event.minIntegrityRisk != null && player.attributes.integrityRisk < event.minIntegrityRisk) {
                return@filter false
            }
            val hidden = player.hidden
            if (event.minStress != null && hidden.stress < event.minStress) return@filter false
            if (event.minHealthRisk != null && hidden.healthRisk < event.minHealthRisk) return@filter false
            if (event.minRiskPressure != null && hidden.riskPressure < event.minRiskPressure) {
                return@filter false
            }
            if (event.minSocialInfluence != null && hidden.socialInfluence < event.minSocialInfluence) {
                return@filter false
            }
            if (event.minNetwork != null && hidden.network < event.minNetwork) return@filter false
            if (!meetsPoliticalGate(state, event)) return@filter false
            if (!GovernanceEngine.meetsGate(state, event.governanceGate)) return@filter false
            true
        }
    }

    /** 当前任职城市的事件标签；旧存档或未知城市回退到默认城市。 */
    fun cityTags(state: GameState): Set<String> =
        RegionCatalog.cityOf(state.regionId).eventTags

    /** 城市事件池门控。 */
    private fun meetsCityGate(state: GameState, event: GameEvent): Boolean {
        if (event.requiredCityTags.isNotEmpty()) {
            val tags = cityTags(state)
            if (event.requiredCityTags.none { it in tags }) return false
        }
        if (event.forbiddenCityTags.isNotEmpty()) {
            val tags = cityTags(state)
            if (event.forbiddenCityTags.any { it in tags }) return false
        }
        return true
    }

    // 官场前置条件：势力关系、人物关系、风险区间。
    private fun meetsPoliticalGate(state: GameState, event: GameEvent): Boolean {
        val gate = event.politicalGate
        if (gate.isEmpty) return true
        val rel = state.relationship
        if (gate.minRisk != null && rel.risk < gate.minRisk) return false
        if (gate.maxRisk != null && rel.risk > gate.maxRisk) return false
        gate.minFaction.forEach { (faction, required) ->
            if (rel.factionOf(faction) < required) return false
        }
        gate.minNpcRelation.forEach { (npcId, required) ->
            if (rel.relationOf(npcId) < required) return false
        }
        return true
    }

    // 兜底：保证任何时候都有事件可以推进。忽略冷却，只要求职位匹配。
    private fun fallback(state: GameState): GameEvent? {
        val ids = listOf("overtime_task", "urgent_material", "data_report", "year_end_summary")
        val player = state.player
        return ids.mapNotNull { EventLibrary.find(it) }
            .firstOrNull { it.minPositionIndex <= player.positionIndex && it.maxPositionIndex >= player.positionIndex }
            ?: EventLibrary.find("overtime_task")
    }
}
