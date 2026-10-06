package com.shitu.app.model

/** 政绩的五个维度。综合政绩由它们加权得出，不允许只刷一项。 */
enum class MeritKind(val label: String, val weight: Int) {
    ECONOMIC("经济绩效", 30),
    LIVELIHOOD("民生绩效", 25),
    SECURITY("治安绩效", 20),
    FINANCE("财政绩效", 15),
    INNOVATION("创新绩效", 10);

    companion object {
        fun byName(value: String?): MeritKind? = entries.firstOrNull { it.name == value }
    }
}

data class Merit(
    val economic: Int = 0,
    val livelihood: Int = 0,
    val security: Int = 0,
    val finance: Int = 0,
    val innovation: Int = 0
) {
    fun valueOf(kind: MeritKind): Int = when (kind) {
        MeritKind.ECONOMIC -> economic
        MeritKind.LIVELIHOOD -> livelihood
        MeritKind.SECURITY -> security
        MeritKind.FINANCE -> finance
        MeritKind.INNOVATION -> innovation
    }

    fun withValue(kind: MeritKind, value: Int): Merit {
        val v = value.coerceIn(0, 100)
        return when (kind) {
            MeritKind.ECONOMIC -> copy(economic = v)
            MeritKind.LIVELIHOOD -> copy(livelihood = v)
            MeritKind.SECURITY -> copy(security = v)
            MeritKind.FINANCE -> copy(finance = v)
            MeritKind.INNOVATION -> copy(innovation = v)
        }
    }

    /** 综合政绩：五项加权，任何一项过低都会拉低总分。 */
    val overall: Int
        get() = (
            economic * 30 + livelihood * 25 + security * 20 +
                finance * 15 + innovation * 10
            ) / 100

    /** 最弱的一项，用于提示玩家短板。 */
    val weakest: MeritKind
        get() = MeritKind.entries.minBy { valueOf(it) }

    val isBlank: Boolean
        get() = economic == 0 && livelihood == 0 && security == 0 &&
            finance == 0 && innovation == 0
}

/** 政务大厅的行动分类。 */
enum class GovernanceCategory(val label: String) {
    ECONOMY("经济"),
    LIVELIHOOD("民生"),
    INFRASTRUCTURE("基建"),
    SECURITY("治安"),
    ENVIRONMENT("环境"),
    PERSONAL("个人")
}

/**
 * 一项政务措施或个人事项。
 * 资源开销（财政 / 个人存款 / 精力 / 行动点）是防止无限投资的硬约束。
 */
data class GovernanceAction(
    val id: String,
    val label: String,
    val description: String,
    val category: GovernanceCategory,
    val financeCost: Int = 0,
    val savingsCost: Int = 0,
    val energyCost: Int = 3,
    val cooldownQuarters: Int = 4,
    val effect: GovernanceEffect = GovernanceEffect()
)

/** 一次治理行为带来的变化。 */
data class GovernanceEffect(
    val indicators: Map<Indicator, Int> = emptyMap(),
    val merit: Map<MeritKind, Int> = emptyMap(),
    val finance: Int = 0,
    val savings: Int = 0,
    val risk: Int = 0,
    /** 对玩家自身属性的影响（个人事项会用到）。 */
    val attributes: Map<Attr, Int> = emptyMap(),
    val hidden: Map<HiddenKey, Int> = emptyMap(),
    val completedFlag: String? = null
) {
    val isEmpty: Boolean
        get() = indicators.isEmpty() && merit.isEmpty() && finance == 0 &&
            savings == 0 && risk == 0 && attributes.isEmpty() &&
            hidden.isEmpty() && completedFlag == null
}

/** 事件对辖区状态的要求。 */
data class IndicatorGate(
    val indicator: Indicator,
    val min: Int? = null,
    val max: Int? = null
)

data class GovernanceGate(
    val indicators: List<IndicatorGate> = emptyList(),
    val minFinance: Int? = null,
    val maxFinance: Int? = null
) {
    val isEmpty: Boolean
        get() = indicators.isEmpty() && minFinance == null && maxFinance == null
}

/**
 * 政务进度。行动点每季度重置，措施冷却与已完成政策进入存档。
 */
data class GovernanceState(
    val jurisdiction: Jurisdiction,
    val merit: Merit = Merit(),
    val finance: Int = 0,
    val actionsUsed: Int = 0,
    val completed: Set<String> = emptySet(),
    val cooldowns: Map<String, Int> = emptyMap(),
    val savings: Int = 0,
    val salaryPerQuarter: Int = 0
) {
    val actionPoints: Int get() = jurisdiction.level.actionPoints

    val actionsLeft: Int get() = (actionPoints - actionsUsed).coerceAtLeast(0)

    fun readyAt(actionId: String): Int = cooldowns[actionId] ?: 0

    fun isReady(actionId: String, absoluteQuarter: Int): Boolean =
        readyAt(actionId) <= absoluteQuarter

    fun withIndicator(indicator: Indicator, delta: Int): GovernanceState {
        val current = jurisdiction.valueOf(indicator)
        return copy(jurisdiction = jurisdiction.withValue(indicator, current + delta))
    }

    fun withMerit(kind: MeritKind, delta: Int): GovernanceState {
        val current = merit.valueOf(kind)
        return copy(merit = merit.withValue(kind, current + delta))
    }

    fun withFinance(delta: Int): GovernanceState =
        copy(finance = (finance + delta).coerceAtLeast(0))

    fun withSavings(delta: Int): GovernanceState =
        copy(savings = (savings + delta).coerceAtLeast(0))

    fun withCooldown(actionId: String, readyAt: Int): GovernanceState =
        copy(cooldowns = cooldowns + (actionId to readyAt))

    fun withCompleted(flag: String): GovernanceState = copy(completed = completed + flag)
}
