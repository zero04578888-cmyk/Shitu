package com.shitu.app.game

import com.shitu.app.model.Attr
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.model.PositionScope
import com.shitu.app.model.positionAt

/**
 * 第十五阶段：职位引擎。
 *
 * 把职级数据真正接进游戏逻辑：
 * - 月度收入进入季度结算；
 * - 权力等级与权限域决定事件门控；
 * - 在岗影响（政绩 / 民望 / 威望）在年度考核时结算一次。
 *
 * 全部为游戏模拟参数，不代表现实规则。
 */
object PositionEngine {

    const val TENURE_MERIT_FROM = 6
    const val TENURE_APPROVAL_FROM = 4
    const val TENURE_REPUTATION_FROM = 7

    /** 当前职级的月度基础收入。 */
    fun salaryOf(positionIndex: Int): Int = positionAt(positionIndex).monthlyIncome

    /** 当前职级对应的权限域。 */
    fun scopeOf(positionIndex: Int): PositionScope = positionAt(positionIndex).scope

    /** 该事件是否超出玩家当前职级的权限。 */
    fun canHandleEvent(
        state: GameState,
        minPositionIndex: Int,
        requiredScope: PositionScope? = null
    ): Boolean {
        if (state.player.positionIndex < minPositionIndex) return false
        if (requiredScope == null) return true
        return scopeOf(state.player.positionIndex).ordinal >= requiredScope.ordinal
    }

    data class TenureEffect(
        val merit: Int,
        val approval: Int,
        val reputation: Int
    ) {
        val isEmpty: Boolean get() = merit == 0 && approval == 0 && reputation == 0
    }

    /** 在岗一年的岗位影响。 */
    fun tenureEffectOf(positionIndex: Int): TenureEffect {
        val level = positionAt(positionIndex)
        return TenureEffect(
            merit = if (level.index >= TENURE_MERIT_FROM) level.monthlyMerit else 0,
            approval = if (level.index >= TENURE_APPROVAL_FROM) level.approvalEffect else 0,
            reputation = if (level.index >= TENURE_REPUTATION_FROM) level.reputationEffect else 0
        )
    }

    /**
     * 年度在岗结算：把职位自带的政绩 / 民望 / 威望影响落到状态上。
     * 所有数值都有上下限保护，不会出现溢出。
     */
    fun applyTenure(state: GameState, positionIndex: Int = state.player.positionIndex): GameState {
        val effect = tenureEffectOf(positionIndex)
        if (effect.isEmpty) return state

        var merit = state.governance.merit
        if (effect.merit != 0) {
            MeritKind.entries.forEach { kind ->
                merit = merit.withValue(kind, merit.valueOf(kind) + effect.merit)
            }
        }
        var jurisdiction = state.governance.jurisdiction
        if (effect.approval != 0) {
            jurisdiction = jurisdiction.withValue(
                Indicator.APPROVAL,
                jurisdiction.approval + effect.approval
            )
        }
        var attributes = state.player.attributes
        if (effect.reputation != 0) {
            attributes = attributes.withValue(
                Attr.REPUTATION,
                attributes.reputation + effect.reputation
            )
        }
        return state.copy(
            player = state.player.copy(attributes = attributes),
            governance = state.governance.copy(merit = merit, jurisdiction = jurisdiction)
        )
    }
}