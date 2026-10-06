package com.shitu.app.game

import com.shitu.app.model.Attr
import com.shitu.app.model.HiddenKey

/** 一次季度演算的结果：结算后的状态，以及需要提醒玩家的内容。 */
data class ConsequenceResult(
    val state: GameState,
    val notes: List<String>
)

/**
 * 暗线演算。
 *
 * 事件里的大量选择只改变「隐性状态」（压力、健康隐患、廉政压力、社会影响、人际网络），
 * 玩家当季看不到直接收益。这个引擎在每个季度末把这些隐性状态换算成真实后果，
 * 让前面几个季度的选择在很久之后才显出分量。
 *
 * 所有阈值与数值都是游戏模拟参数，仅用于玩法平衡。
 */
object ConsequenceEngine {

    // 触发阈值（游戏模拟参数）
    const val STRESS_HEAVY = 70
    const val STRESS_MILD = 50
    const val HEALTH_RISK_HEAVY = 60
    const val HEALTH_RISK_MILD = 30
    const val RISK_PRESSURE_HEAVY = 70
    const val RISK_PRESSURE_MILD = 40
    const val RISK_PRESSURE_CALM = 10
    const val SOCIAL_INFLUENCE_STRONG = 60
    const val SOCIAL_INFLUENCE_MILD = 30
    const val NETWORK_STRONG = 60
    const val NETWORK_MILD = 30
    const val NETWORK_ISOLATED = 10

    // 精力与健康的危险线，越过之后会反过来加重隐性状态
    const val ENERGY_DRAINED = 30
    const val HEALTH_FRAGILE = 40

    // 隐性状态的季度自然消退
    const val RISK_PRESSURE_DECAY = 2
    const val SOCIAL_INFLUENCE_DECAY = 2
    const val NETWORK_DECAY = 1

    fun settle(state: GameState): ConsequenceResult {
        val hidden = state.player.hidden
        var attrs = state.player.attributes
        var newHidden = hidden
        val notes = mutableListOf<String>()

        // 压力：先消耗精力，长期高压再伤身体
        when {
            hidden.stress >= STRESS_HEAVY -> {
                attrs = attrs
                    .withValue(Attr.ENERGY, attrs.energy - 5)
                    .withValue(Attr.HEALTH, attrs.health - 3)
                notes += "长期高压正在消耗你的精力与健康。"
            }
            hidden.stress >= STRESS_MILD -> {
                attrs = attrs.withValue(Attr.ENERGY, attrs.energy - 2)
            }
        }

        // 健康隐患：拖得越久，代价越大
        when {
            hidden.healthRisk >= HEALTH_RISK_HEAVY -> {
                attrs = attrs.withValue(Attr.HEALTH, attrs.health - 3)
                notes += "身体发出的警告越来越频繁，你一直没有时间去查。"
            }
            hidden.healthRisk >= HEALTH_RISK_MILD -> {
                attrs = attrs
                    .withValue(Attr.HEALTH, attrs.health - 1)
                    .withValue(Attr.ENERGY, attrs.energy - 1)
            }
        }

        // 廉政压力：旧事不会自己消失
        when {
            hidden.riskPressure >= RISK_PRESSURE_HEAVY -> {
                attrs = attrs
                    .withValue(Attr.INTEGRITY_RISK, attrs.integrityRisk + 3)
                    .withValue(Attr.REPUTATION, attrs.reputation - 2)
                notes += "几件旧事被重新提起，廉政风险在上升。"
            }
            hidden.riskPressure >= RISK_PRESSURE_MILD -> {
                attrs = attrs.withValue(Attr.INTEGRITY_RISK, attrs.integrityRisk + 2)
            }
            hidden.riskPressure <= RISK_PRESSURE_CALM -> {
                attrs = attrs.withValue(Attr.INTEGRITY_RISK, attrs.integrityRisk - 1)
            }
        }

        // 社会影响：公开场合的积累会慢慢变成声望
        when {
            hidden.socialInfluence >= SOCIAL_INFLUENCE_STRONG -> {
                attrs = attrs.withValue(Attr.REPUTATION, attrs.reputation + 2)
            }
            hidden.socialInfluence >= SOCIAL_INFLUENCE_MILD -> {
                attrs = attrs.withValue(Attr.REPUTATION, attrs.reputation + 1)
            }
        }

        // 人际网络：平时攒下的关系会在日常工作中体现出来
        when {
            hidden.network >= NETWORK_STRONG -> {
                attrs = attrs
                    .withValue(Attr.COLLEAGUES, attrs.colleagues + 2)
                    .withValue(Attr.LEADER_TRUST, attrs.leaderTrust + 1)
            }
            hidden.network >= NETWORK_MILD -> {
                attrs = attrs.withValue(Attr.COLLEAGUES, attrs.colleagues + 1)
            }
            hidden.network <= NETWORK_ISOLATED -> {
                attrs = attrs.withValue(Attr.COLLEAGUES, attrs.colleagues - 1)
            }
        }

        // 透支会反过来推高压力与健康隐患，形成恶性循环
        if (attrs.energy <= ENERGY_DRAINED) {
            newHidden = newHidden.withValue(HiddenKey.STRESS, newHidden.stress + 4)
        }
        if (attrs.health <= HEALTH_FRAGILE) {
            newHidden = newHidden.withValue(HiddenKey.HEALTH_RISK, newHidden.healthRisk + 3)
        }

        // 隐性状态随时间自然消退
        newHidden = newHidden
            .withValue(HiddenKey.RISK_PRESSURE, newHidden.riskPressure - RISK_PRESSURE_DECAY)
            .withValue(HiddenKey.SOCIAL_INFLUENCE, newHidden.socialInfluence - SOCIAL_INFLUENCE_DECAY)
            .withValue(HiddenKey.NETWORK, newHidden.network - NETWORK_DECAY)

        val updated = state.copy(
            player = state.player.copy(attributes = attrs, hidden = newHidden)
        )
        return ConsequenceResult(state = updated, notes = notes)
    }
}
