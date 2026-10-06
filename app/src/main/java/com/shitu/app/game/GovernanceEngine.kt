package com.shitu.app.game

import com.shitu.app.model.Attr
import com.shitu.app.model.CareerAbility
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.GovernanceCategory
import com.shitu.app.model.GovernanceAction
import com.shitu.app.model.GovernanceEffect
import com.shitu.app.model.GovernanceState
import com.shitu.app.model.Indicator
import com.shitu.app.model.JurisdictionLevel
import com.shitu.app.model.MeritKind

/** 一次政务行动的结果。 */
data class ActionOutcome(
    val state: GameState,
    val message: String
)

/**
 * 政务治理引擎。
 *
 * 资源约束是核心：每季度行动点有限，财政与个人存款有限，
 * 措施还有冷却。所以不可能靠一直点同一件事刷满政绩。
 * 所有数值都是游戏模拟参数。
 */
object GovernanceEngine {

    /** 每年政绩自然衰减，必须持续做事才保得住。 */
    const val MERIT_ANNUAL_DECAY = 3

    /** 行政开支基数。 */
    const val ADMIN_EXPENSE_BASE = 6

    fun initial(unit: com.shitu.app.model.UnitType, positionIndex: Int): GovernanceState {
        val jurisdiction = JurisdictionFactory.forPlayer(unit, positionIndex)
        return GovernanceState(
            jurisdiction = jurisdiction,
            finance = jurisdiction.level.baseFinance,
            salaryPerQuarter = salaryOf(positionIndex)
        )
    }

    fun salaryOf(positionIndex: Int): Int = PositionEngine.salaryOf(positionIndex)

    // ===== 措施可用性 =====

    fun unavailabilityReason(state: GameState, action: GovernanceAction): String? {
        val gov = state.governance
        val attrs = state.player.attributes
        return when {
            gov.actionsLeft <= 0 -> "本季度行动点已经用完"
            attrs.energy < action.energyCost -> "精力不足"
            gov.finance < action.financeCost -> "财政拿不出这笔钱"
            gov.savings < action.savingsCost -> "个人存款不够"
            !gov.isReady(action.id, state.absoluteQuarter) -> "刚做过，还需要时间"
            else -> null
        }
    }

    fun isAvailable(state: GameState, action: GovernanceAction): Boolean =
        unavailabilityReason(state, action) == null

    fun availableActions(state: GameState): List<GovernanceAction> =
        GovernanceCatalog.actions.filter { isAvailable(state, it) }

    // ===== 执行措施 =====

    fun perform(state: GameState, actionId: String): ActionOutcome {
        val action = GovernanceCatalog.find(actionId)
            ?: return ActionOutcome(state, "没有这项措施。")
        unavailabilityReason(state, action)?.let { reason ->
            return ActionOutcome(state, "现在做不了：$reason。")
        }

        var attrs = state.player.attributes
        var hidden = state.player.hidden
        var relationship = state.relationship
        var gov = state.governance

        attrs = attrs.withValue(Attr.ENERGY, attrs.energy - action.energyCost)
        val effect = action.effect
        effect.attributes.forEach { (attr, delta) ->
            attrs = attrs.withValue(attr, attrs.valueOf(attr) + delta)
        }
        effect.hidden.forEach { (key, delta) ->
            hidden = hidden.withValue(key, hidden.valueOf(key) + delta)
        }
        gov = applyToJurisdiction(gov, effect)
        gov = gov
            .withFinance(-action.financeCost)
            .withSavings(-action.savingsCost)
            .copy(
                actionsUsed = gov.actionsUsed + 1,
                salaryPerQuarter = salaryOf(state.player.positionIndex)
            )
        gov = gov.withCooldown(action.id, state.absoluteQuarter + action.cooldownQuarters)
        effect.completedFlag?.let { gov = gov.withCompleted(it) }
        if (effect.risk != 0) {
            relationship = relationship.withRisk(relationship.risk + effect.risk)
        }

        // 第十阶段：个人事项带来成长、资产与履历。
        var career = state.career
        when (action.id) {
            "self_study" -> career = career.withValue(
                CareerAbility.SPECIALTY,
                career.valueOf(CareerAbility.SPECIALTY) + 2
            )
            "self_home" -> career = career.copy(
                assets = career.assets.withHousing(career.assets.housing + 1)
            )
            "self_transport" -> career = career.copy(
                assets = career.assets.withVehicle(career.assets.vehicle + 1)
            )
        }

        // 第十三阶段：政务措施会作为一种政策取向记入世界，影响长期后果。
        val updated = WorldSimulationEngine.recordPolicy(
            state.copy(
                player = state.player.copy(attributes = attrs, hidden = hidden),
                governance = gov,
                relationship = relationship,
                career = career
            ),
            title = action.label,
            direction = action.category.label,
            tags = WorldSimulationEngine.policyTagsOf(action),
            magnitude = 2
        )
        val experienced = CareerEngine.gainExperience(
            updated,
            if (action.category == GovernanceCategory.PERSONAL) 3 else 2
        )
        val withRecord = when (action.id) {
            "self_study" -> CareerEngine.record(
                experienced, CareerRecordKind.PROJECT, "学习进修", "利用业余时间完成了一次专业进修。"
            )
            "self_home" -> CareerEngine.record(
                experienced, CareerRecordKind.FAMILY, "改善住房", "用积蓄改善了家里的居住条件。"
            )
            else -> experienced
        }
        return ActionOutcome(
            CareerEngine.checkAchievements(withRecord),
            describe(action, effect)
        )
    }

    /** 把治理影响应用到辖区与政绩上。 */
    fun applyToJurisdiction(gov: GovernanceState, effect: GovernanceEffect): GovernanceState {
        var result = gov
        effect.indicators.forEach { (indicator, delta) ->
            result = result.withIndicator(indicator, delta)
        }
        effect.merit.forEach { (kind, delta) -> result = result.withMerit(kind, delta) }
        if (effect.finance != 0) result = result.withFinance(effect.finance)
        if (effect.savings != 0) result = result.withSavings(effect.savings)
        return result
    }

    private fun describe(action: GovernanceAction, effect: GovernanceEffect): String {
        val parts = mutableListOf<String>()
        effect.indicators.forEach { (indicator, delta) ->
            parts += "${indicator.label} ${signed(delta)}"
        }
        effect.merit.forEach { (kind, delta) -> parts += "${kind.label} ${signed(delta)}" }
        effect.attributes.forEach { (attr, delta) -> parts += "${attr.label} ${signed(delta)}" }
        effect.hidden.forEach { (key, delta) ->
            if (key.visible) parts += "${key.label} ${signed(delta)}"
        }
        if (action.financeCost > 0) parts += "财政 -${action.financeCost}"
        if (action.savingsCost > 0) parts += "个人存款 -${action.savingsCost}"
        if (effect.risk != 0) parts += "官场风险 ${signed(effect.risk)}"
        return "「${action.label}」完成：" + parts.joinToString("，")
    }

    // ===== 结算 =====

    /** 季度收入：来自产业、人口与上级转移支付。 */
    fun quarterlyIncome(state: GameState): Int {
        val jurisdiction = state.governance.jurisdiction
        // 第十三阶段：经济周期会影响自然财政收入。
        return jurisdiction.level.baseFinance / 3 +
            jurisdiction.economy / 8 +
            jurisdiction.population / 4 +
            WorldSimulationEngine.incomeModifier(state)
    }

    /** 季度支出：行政开支随级别和基建规模上升。 */
    fun quarterlyExpense(state: GameState): Int {
        val jurisdiction = state.governance.jurisdiction
        return ADMIN_EXPENSE_BASE +
            state.player.positionIndex * 2 +
            jurisdiction.infrastructure / 25
    }

    /**
     * 季度结算：重置行动点、结财政账、发工资。
     * 只在财政见底时提醒一次，避免日志被刷屏。
     */
    fun settleQuarter(state: GameState): Pair<GameState, List<String>> {
        val gov = state.governance
        val income = quarterlyIncome(state)
        val expense = quarterlyExpense(state)
        val salary = salaryOf(state.player.positionIndex)
        val net = income - expense
        val settled = gov.copy(
            actionsUsed = 0,
            finance = (gov.finance + net).coerceAtLeast(0),
            savings = gov.savings + salary,
            salaryPerQuarter = salary
        )
        val notes = mutableListOf<String>()
        if (settled.finance <= 0 && gov.finance > 0) {
            notes += "${gov.jurisdiction.name}的财政已经见底，很多事暂时做不了。"
        }
        return state.copy(governance = settled) to notes
    }

    /** 年度结算：政绩自然衰减，年末财政状况计入财政绩效。 */
    fun settleYear(governance: GovernanceState): GovernanceState {
        var gov = governance
        MeritKind.entries.forEach { kind ->
            val current = gov.merit.valueOf(kind)
            gov = gov.copy(merit = gov.merit.withValue(kind, (current - MERIT_ANNUAL_DECAY).coerceAtLeast(0)))
        }
        val financeAdjust = when {
            gov.finance >= 80 -> 5
            gov.finance >= 40 -> 2
            gov.finance <= 5 -> -5
            else -> 0
        }
        if (financeAdjust != 0) {
            val current = gov.merit.valueOf(MeritKind.FINANCE)
            gov = gov.copy(
                merit = gov.merit.withValue(
                    MeritKind.FINANCE,
                    (current + financeAdjust).coerceIn(0, 100)
                )
            )
        }
        return gov
    }

    // ===== 考核用指标 =====

    /** 财政健康度（0～100）。 */
    fun financeScore(state: GameState): Int {
        val target = (state.governance.jurisdiction.level.baseFinance * 3 / 2).coerceAtLeast(1)
        return (state.governance.finance * 100 / target).coerceIn(0, 100)
    }

    /** 治理综合得分：政绩、民望、治安、财政、发展水平。 */
    fun governanceScore(state: GameState): Int {
        val gov = state.governance
        val jurisdiction = gov.jurisdiction
        return (
            gov.merit.overall * 40 +
                jurisdiction.approval * 25 +
                jurisdiction.security * 15 +
                financeScore(state) * 10 +
                jurisdiction.development * 10
            ) / 100
    }

    /** 事件对辖区的要求是否满足。 */
    fun meetsGate(state: GameState, gate: com.shitu.app.model.GovernanceGate): Boolean {
        if (gate.isEmpty) return true
        val gov = state.governance
        if (gate.minFinance != null && gov.finance < gate.minFinance) return false
        if (gate.maxFinance != null && gov.finance > gate.maxFinance) return false
        gate.indicators.forEach { requirement ->
            val value = gov.jurisdiction.valueOf(requirement.indicator)
            if (requirement.min != null && value < requirement.min) return false
            if (requirement.max != null && value > requirement.max) return false
        }
        return true
    }

    /** 民望档位描述。 */
    fun approvalLabel(approval: Int): String = when {
        approval >= 80 -> "很有口碑"
        approval >= 65 -> "群众认可"
        approval >= 45 -> "平稳"
        approval >= 30 -> "有怨言"
        else -> "意见很大"
    }

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
}
