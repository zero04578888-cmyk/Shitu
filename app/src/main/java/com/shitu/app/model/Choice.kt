package com.shitu.app.model

/** 选择风格，仅用于界面提示，不直接参与数值计算。 */
enum class Attitude(val label: String) {
    BOLD("果断"),
    STEADY("稳健"),
    CAUTIOUS("保守"),
    RISKY("冒险")
}

/**
 * 一个事件选项。同一事件的三个选项必须产生不同结果。
 */
data class Choice(
    val text: String,
    val attitude: Attitude,
    val outcome: String,
    val effects: AttrDelta,
    val hiddenEffects: HiddenDelta = HiddenDelta(),
    val startsChain: String? = null,
    val setsFlag: String? = null,
    val setsRoute: CareerRoute? = null,
    val political: PoliticalEffect = PoliticalEffect(),
    /** 第九阶段：政治资源、路线倾向、情报、联盟、危机等影响。 */
    val politics: PoliticalImpact = PoliticalImpact(),
    val governance: GovernanceEffect = GovernanceEffect(),
    val hint: String = "",
    val conditionalBonus: List<ConditionalEffect> = emptyList()
)

/**
 * 条件加成：如果玩家此前留下过某个标记，本次选择会产生额外结果。
 * 这是事件链「长期因果」的实现方式。
 */
data class ConditionalEffect(
    val requiredFlag: String,
    val effects: AttrDelta = AttrDelta(),
    val hiddenEffects: HiddenDelta = HiddenDelta(),
    val extraOutcome: String = ""
)
