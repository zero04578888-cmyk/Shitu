package com.shitu.app.game

import com.shitu.app.model.POSITION_LADDER

/** 65 岁人生节点的两种选择。 */
enum class RetirementChoice(val type: String, val label: String) {
    /** 办理退休，进入人生总结。 */
    RETIREMENT("RETIREMENT", "退休"),
    /** 退居二线，继续有限的退休后内容，但不能晋升。 */
    SECOND_LINE("SECOND_LINE", "退居二线");

    companion object {
        fun byType(type: String?): RetirementChoice? = entries.firstOrNull { it.type == type }
    }
}

/**
 * 退休规则。
 *
 * 规则很简单，也必须是确定的：
 * - 年龄达到 [RETIREMENT_AGE] 且职级低于 [EXEMPT_RANK_INDEX]（副国级）时，
 *   必须停止正常仕途，进入「退休 / 退居二线」人生节点；
 * - 职级达到副国级及以上者不受年龄限制，可以继续任职；
 * - 判断一律使用职级下标（RankData 的真实等级顺序），不做字符串比较。
 *
 * 年龄系统复用 [com.shitu.app.model.Player.age]：每年 +1，季度不增加。
 */
object RetirementEngine {

    /** 强制退休年龄。 */
    const val RETIREMENT_AGE: Int = 65

    /**
     * 不受年龄限制的起始职级下标：副国级。
     *
     * 由职级阶梯按名称一次性解析为数字下标，之后所有判断都用数值比较
     * （positionIndex >= EXEMPT_RANK_INDEX），既符合等级顺序，也不会被字符串比较坑到。
     */
    val EXEMPT_RANK_INDEX: Int = POSITION_LADDER
        .indexOfFirst { it.name == "副国级" }
        .takeIf { it >= 0 }
        ?: 11

    /** 该职级是否不受 65 岁限制。 */
    fun isExempt(rankIndex: Int): Boolean = rankIndex >= EXEMPT_RANK_INDEX

    /**
     * 是否正处于「退休节点」：年龄已到但还没有做出选择。
     * 只有低于副国级、且仍在一线仕途中的玩家才会触发。
     */
    fun isDue(state: GameState): Boolean =
        isDue(
            age = state.player.age,
            rankIndex = state.player.positionIndex,
            inActiveCareer = state.player.inActiveCareer
        )

    /**
     * 同上，但由调用方显式传入年龄与职级下标。
     *
     * 年度考核结算会用到：玩家在 64 岁这一年获得晋升资格、恰好在该次结算中迈入
     * 65 岁时，需要按「晋升后的职级」判断是否豁免，避免把本该进入副国级的玩家
     * 在任命生效前就提前拦下。
     */
    fun isDue(age: Int, rankIndex: Int, inActiveCareer: Boolean): Boolean =
        inActiveCareer && age >= RETIREMENT_AGE && !isExempt(rankIndex)

    /** 退休状态文案，用于个人信息展示；未涉及退休时返回空串。 */
    fun statusLabel(state: GameState): String = when {
        state.player.retired -> "已退休"
        state.player.retirementType == RetirementChoice.SECOND_LINE.type -> "退居二线"
        isDue(state) -> "退休节点"
        else -> ""
    }

    /** 玩家是否已经退出正常晋升通道（退休或退居二线）。 */
    fun hasLeftActiveCareer(state: GameState): Boolean = !state.player.inActiveCareer
}