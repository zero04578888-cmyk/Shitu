package com.shitu.app.game

/**
 * 降职 / 停职机制。
 *
 * 只在「连续失败」或「重大风险」时触发，绝不随机降职。
 * 触发门槛很高，正常游玩不会无故被降。
 */
object DemotionEngine {

    /** 最基层不再下降。 */
    const val SAFE_MIN_INDEX = 1

    enum class Kind { DEMOTION, SUSPENSION }

    data class Reason(val text: String, val kind: Kind)

    fun reasonFor(
        state: GameState,
        grade: String,
        reviewScore: Int,
        warnings: Int
    ): Reason? {
        // 已经退休 / 退居二线者不再参与任何降职调整。
        if (!state.player.inActiveCareer) return null
        if (state.player.positionIndex < SAFE_MIN_INDEX) return null
        val attrs = state.player.attributes
        val repeatedFailure = grade == "不称职" && warnings >= 3
        val severeIntegrity = attrs.integrityRisk >= 88 && state.relationship.risk >= 70
        val crisisFallout = state.flags.contains("crisis_severe") && reviewScore < 52
        return when {
            severeIntegrity -> Reason(
                "廉政与官场风险叠加，组织决定对你停职检查并调整岗位。",
                Kind.SUSPENSION
            )
            repeatedFailure -> Reason(
                "连续考核不称职，组织决定对你的职务作出调整。",
                Kind.DEMOTION
            )
            crisisFallout -> Reason(
                "重大失误的影响尚未消除，你被调整到更低的职位。",
                Kind.DEMOTION
            )
            else -> null
        }
    }
}