package com.shitu.app.model

/**
 * 隐藏变量。玩家只能看到部分变量的模糊提示，看不到完整数值。
 */
enum class HiddenKey(val label: String, val visible: Boolean) {
    OPPORTUNITY("机遇值", false),
    MENTOR("贵人值", false),
    RISK_PRESSURE("风险值", false),
    SOCIAL_INFLUENCE("社会影响", true),
    STRESS("压力值", true),
    HEALTH_RISK("健康隐患", false),
    NETWORK("人际网络", true);

    companion object {
        fun byName(value: String): HiddenKey? = entries.firstOrNull { it.name == value }
    }
}

data class HiddenStats(
    val opportunity: Int = 30,
    val mentor: Int = 15,
    val riskPressure: Int = 5,
    val socialInfluence: Int = 10,
    val stress: Int = 20,
    val healthRisk: Int = 5,
    val network: Int = 15
) {
    fun valueOf(key: HiddenKey): Int = when (key) {
        HiddenKey.OPPORTUNITY -> opportunity
        HiddenKey.MENTOR -> mentor
        HiddenKey.RISK_PRESSURE -> riskPressure
        HiddenKey.SOCIAL_INFLUENCE -> socialInfluence
        HiddenKey.STRESS -> stress
        HiddenKey.HEALTH_RISK -> healthRisk
        HiddenKey.NETWORK -> network
    }

    fun withValue(key: HiddenKey, value: Int): HiddenStats = when (key) {
        HiddenKey.OPPORTUNITY -> copy(opportunity = clampAttr(value))
        HiddenKey.MENTOR -> copy(mentor = clampAttr(value))
        HiddenKey.RISK_PRESSURE -> copy(riskPressure = clampAttr(value))
        HiddenKey.SOCIAL_INFLUENCE -> copy(socialInfluence = clampAttr(value))
        HiddenKey.STRESS -> copy(stress = clampAttr(value))
        HiddenKey.HEALTH_RISK -> copy(healthRisk = clampAttr(value))
        HiddenKey.NETWORK -> copy(network = clampAttr(value))
    }

    fun normalized(): HiddenStats = HiddenKey.entries.fold(this) { acc, key ->
        acc.withValue(key, acc.valueOf(key))
    }

    fun apply(delta: HiddenDelta): HiddenStats = delta.entries().fold(this) { acc, pair ->
        acc.withValue(pair.first, acc.valueOf(pair.first) + pair.second)
    }
}

data class HiddenDelta(
    val opportunity: Int = 0,
    val mentor: Int = 0,
    val riskPressure: Int = 0,
    val socialInfluence: Int = 0,
    val stress: Int = 0,
    val healthRisk: Int = 0,
    val network: Int = 0
) {
    fun valueOf(key: HiddenKey): Int = when (key) {
        HiddenKey.OPPORTUNITY -> opportunity
        HiddenKey.MENTOR -> mentor
        HiddenKey.RISK_PRESSURE -> riskPressure
        HiddenKey.SOCIAL_INFLUENCE -> socialInfluence
        HiddenKey.STRESS -> stress
        HiddenKey.HEALTH_RISK -> healthRisk
        HiddenKey.NETWORK -> network
    }

    fun entries(): List<Pair<HiddenKey, Int>> =
        HiddenKey.entries.map { it to valueOf(it) }.filter { it.second != 0 }
}
