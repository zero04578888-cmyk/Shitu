package com.shitu.app.model

/** 辖区治理指标。财政单独用金额表示，不在这里。 */
enum class Indicator(val label: String) {
    ECONOMY("经济"),
    LIVELIHOOD("民生"),
    SECURITY("治安"),
    EDUCATION("教育"),
    MEDICAL("医疗"),
    INFRASTRUCTURE("基础设施"),
    ENVIRONMENT("环境"),
    APPROVAL("民望"),
    OFFICIAL_SATISFACTION("官员满意度");

    companion object {
        fun byName(value: String?): Indicator? = entries.firstOrNull { it.name == value }
    }
}

/** 辖区层级。官职越高，管的盘子越大。 */
enum class JurisdictionLevel(
    val label: String,
    val defaultName: String,
    val population: Int,
    val baseFinance: Int,
    val actionPoints: Int
) {
    TOWNSHIP("乡镇", "乡镇驻地", 4, 45, 2),
    STREET("街道", "城区街道", 8, 55, 2),
    KEY_TOWN("重点乡镇", "城关镇", 12, 70, 3),
    COUNTY("县区", "市辖区", 45, 110, 4);
}

/**
 * 玩家当前任职的辖区。所有指标都是 0～100 的治理指数，
 * 财政是结余金额（万元），治理压力由指标推导，不单独存档。
 */
data class Jurisdiction(
    val name: String,
    val level: JurisdictionLevel,
    val population: Int,
    val economy: Int = 50,
    val livelihood: Int = 50,
    val security: Int = 55,
    val education: Int = 50,
    val medical: Int = 48,
    val infrastructure: Int = 50,
    val environment: Int = 52,
    val approval: Int = 50,
    val officialSatisfaction: Int = 60
) {
    val levelLabel: String get() = level.label

    /** 治理压力：民望、治安、民生、环境越差，压力越高。 */
    val pressure: Int
        get() = (
            100 - (
                approval * 35 + security * 25 + livelihood * 20 + environment * 20
                ) / 100
            ).coerceIn(0, 100)

    fun valueOf(indicator: Indicator): Int = when (indicator) {
        Indicator.ECONOMY -> economy
        Indicator.LIVELIHOOD -> livelihood
        Indicator.SECURITY -> security
        Indicator.EDUCATION -> education
        Indicator.MEDICAL -> medical
        Indicator.INFRASTRUCTURE -> infrastructure
        Indicator.ENVIRONMENT -> environment
        Indicator.APPROVAL -> approval
        Indicator.OFFICIAL_SATISFACTION -> officialSatisfaction
    }

    fun withValue(indicator: Indicator, value: Int): Jurisdiction {
        val v = value.coerceIn(0, 100)
        return when (indicator) {
            Indicator.ECONOMY -> copy(economy = v)
            Indicator.LIVELIHOOD -> copy(livelihood = v)
            Indicator.SECURITY -> copy(security = v)
            Indicator.EDUCATION -> copy(education = v)
            Indicator.MEDICAL -> copy(medical = v)
            Indicator.INFRASTRUCTURE -> copy(infrastructure = v)
            Indicator.ENVIRONMENT -> copy(environment = v)
            Indicator.APPROVAL -> copy(approval = v)
            Indicator.OFFICIAL_SATISFACTION -> copy(officialSatisfaction = v)
        }
    }

    /** 综合发展水平，用于考核与政绩展示。 */
    val development: Int
        get() = (
            economy * 20 + livelihood * 18 + education * 14 + medical * 12 +
                infrastructure * 16 + environment * 10 + security * 10
            ) / 100
}
