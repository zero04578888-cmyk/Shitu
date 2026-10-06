package com.shitu.app.model

/** 事件标签，用于按单位平台调整出现概率。 */
enum class EventTag {
    DISASTER,
    MASS_APPEAL,
    PETITION,
    ACCIDENT,
    PROJECT,
    RURAL,
    FLOOD,
    EMERGENCY,
    INSPECTION,
    MATERIAL,
    LEADER_TASK,
    COLLEAGUE,
    COORDINATION,
    COMPLAINT,
    PUBLIC_OPINION,
    INTEGRITY,
    GIFT,
    OVERTIME,
    FAMILY,
    HEALTH,
    LEADER_TALK,
    ASSESSMENT,
    MEETING,
    ACCEPTANCE,
    RESEARCH,
    CAREER
}

/**
 * 游戏事件。每个事件至少三个选项。
 *
 * @param chainId 事件链标识，配合 [chainStep] 与 [chainAfterQuarters] 形成长期因果。
 * @param units 为空表示所有单位都可能遇到。
 * @param minIntegrityRisk / maxFamilyRelation 等条件用于让特定状态触发特定事件；
 *        min* 系列的门槛都表示「达到该值及以上才会出现」。
 */
data class GameEvent(
    val id: String,
    val title: String,
    val background: String,
    val description: String,
    val tags: Set<EventTag>,
    val choices: List<Choice>,
    val weight: Int = 10,
    val minPositionIndex: Int = 0,
    val maxPositionIndex: Int = POSITION_LADDER.size - 1,
    val units: Set<UnitType> = emptySet(),
    /**
     * 第十六阶段：城市门控。
     * [requiredCityTags] 非空时，只有玩家任职城市带有其中任意一个标签，事件才会出现；
     * [forbiddenCityTags] 非空时，命中任意一个标签就不出现。
     * 标签与 [CityData.eventTags] 对应，例如 technology / finance / tourism。
     */
    val requiredCityTags: Set<String> = emptySet(),
    val forbiddenCityTags: Set<String> = emptySet(),
    val chainId: String? = null,
    val chainStep: Int = 0,
    val chainAfterQuarters: Int = 0,
    val repeatable: Boolean = false,
    val cooldownQuarters: Int = 12,
    val minFamilyRelation: Int? = null,
    val maxFamilyRelation: Int? = null,
    val minIntegrityRisk: Int? = null,
    val minStress: Int? = null,
    val minHealthRisk: Int? = null,
    val minRiskPressure: Int? = null,
    val minSocialInfluence: Int? = null,
    val minNetwork: Int? = null,
    /** 官场关系方面的前置条件。 */
    val politicalGate: PoliticalGate = PoliticalGate(),
    /** 辖区治理方面的前置条件。 */
    val governanceGate: GovernanceGate = GovernanceGate(),
    /** 里程碑事件：满足条件时优先于普通随机事件出现。 */
    val priority: Boolean = false,
    /** 重大抉择：选择会进入履历，并可能影响后续事件。 */
    val major: Boolean = false,
    /** 只在存在政治危机时出现的应对事件。 */
    val crisisOnly: Boolean = false,
    /** 显式事件等级；未指定时按 major / priority / crisisOnly 自动推断。 */
    val tier: EventTier = EventTier.NORMAL,
    /** 权限域要求；为空表示只按 minPositionIndex 判断。 */
    val requiredScope: PositionScope? = null
) {
    init {
        require(choices.size >= 3) { "事件 $id 至少需要三个选项" }
    }

    /** 事件允许出现的最低职级，是 minPositionIndex 的语义化别名。 */
    val requiredRank: Int get() = minPositionIndex

    /** 事件允许出现的最高职级。 */
    val maxRank: Int get() = maxPositionIndex

    /** 界面使用的最终等级。 */
    val effectiveTier: EventTier
        get() = when {
            tier != EventTier.NORMAL -> tier
            crisisOnly -> EventTier.CRISIS
            major -> EventTier.MAJOR
            priority -> EventTier.IMPORTANT
            else -> EventTier.NORMAL
        }
}
