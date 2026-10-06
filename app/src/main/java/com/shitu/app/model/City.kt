package com.shitu.app.model

/**
 * 第十六阶段：真实中国城市体系 —— 城市行政层级。
 *
 * 一个城市只会落在其中一个层级上，因此不存在「重复计算」：
 * 计划单列市同属副省级体系，职位等级与副省级城市同档，
 * 但单独列出以便界面与数据层区分。
 *
 * [mayorRankIndex] / [secretaryRankIndex] 是该层级党政主官对应的职级序号
 * （见 [POSITION_LADDER]），这是「职位等级计算规则」，不是单纯改 UI 文字。
 */
enum class CityLevel(
    val label: String,
    val shortLabel: String,
    /** 层级权重，用于城市含权量与职位等级推导。 */
    val tier: Int,
    /** 市长对应职级序号。 */
    val mayorRankIndex: Int,
    /** 市委书记对应职级序号。 */
    val secretaryRankIndex: Int
) {
    MUNICIPALITY("直辖市", "直辖市", 5, 10, 11),
    SUB_PROVINCIAL("副省级城市", "副省级", 4, 9, 9),
    SEPARATELY_PLANNED("计划单列市", "计划单列", 4, 9, 9),
    PROVINCIAL_CAPITAL("省会城市", "省会", 3, 8, 8),
    ORDINARY("普通地级市", "地级市", 2, 8, 8);

    /** 是否属于副省级体系（含计划单列市）。二者同档，不重复计算。 */
    val isSubProvincialTier: Boolean
        get() = this == SUB_PROVINCIAL || this == SEPARATELY_PLANNED

    companion object {
        fun byName(value: String?): CityLevel =
            entries.firstOrNull { it.name == value } ?: ORDINARY

        fun byCode(code: String): CityLevel = when (code.trim().uppercase()) {
            "M" -> MUNICIPALITY
            "SP" -> SUB_PROVINCIAL
            "SPP" -> SEPARATELY_PLANNED
            "CAP" -> PROVINCIAL_CAPITAL
            else -> ORDINARY
        }
    }
}

/**
 * 同一城市内部的不同领导职务。
 * 即使是相近职级，其权力结构也必须不同：书记与市长、副书记、常委、
 * 常务副市长、委办局负责人分别拥有不同的影响力侧重。
 */
enum class CityPositionRole(val label: String, val summary: String) {
    PARTY_SECRETARY("市委书记", "全面领导，政治与组织影响力最高。"),
    MAYOR("市长", "行政主官，经济与资源调度能力最强。"),
    DEPUTY_SECRETARY("市委副书记", "协助书记统筹，组织与协调权重高。"),
    STANDING_COMMITTEE("市委常委", "参与重大决策，政治影响力突出。"),
    EXECUTIVE_VICE_MAYOR("常务副市长", "分管财政与项目，经济调度权大。"),
    BUREAU_HEAD("委办局主要负责人", "条线主官，专业与执行影响力为主。")
}

/**
 * 城市内某个领导职务的权力档案。
 * 全部为 0～100 的游戏化数值，由 [PositionPowerCalculator] 参与最终权力计算。
 */
data class CityPositionSpec(
    val cityId: String,
    val role: CityPositionRole,
    val title: String,
    val rankIndex: Int,
    val powerLevel: Int,
    val politicalInfluence: Int,
    val economicInfluence: Int,
    val organizationalInfluence: Int,
    val publicInfluence: Int,
    val promotionPotential: Int
)

/**
 * 真实中国城市的游戏化档案。
 *
 * 城市名称、行政层级、是否省会 / 副省级 / 计划单列 / 直辖市均以现实为准；
 * 经济、人口、财政、产业、文化、科教、国际化等为「归一化到 0～100 的游戏数值」，
 * 现实量级只作为底层依据，不直接进入游戏公式。
 *
 * [dataSource] / [dataYear] 记录数据来源与年份，便于将来更新。
 */
data class CityData(
    val id: String,
    val name: String,
    val province: String,
    val level: CityLevel,
    /** 大区，例如「华东」。 */
    val region: String,
    /** 城市含权量：综合政治地位、经济、人口、战略、科教、国际化等得出的综合城市权重。 */
    val powerWeight: Int,
    val economic: Int,
    val politics: Int,
    val culture: Int,
    val education: Int,
    val technology: Int,
    val transportation: Int,
    val internationalInfluence: Int,
    val strategicImportance: Int,
    val gdp: Int,
    val gdpGrowth: Int,
    val population: Int,
    val fiscalStrength: Int,
    val industrialStrength: Int,
    val serviceIndustryStrength: Int,
    val foreignTrade: Int,
    val investmentStrength: Int,
    val historyLevel: Int,
    val tourismLevel: Int,
    val trait: RegionTrait,
    val potential: Int,
    /** 治理难度：城市体量与要求越高，做事难度越大（游戏模拟参数）。 */
    val challenge: Int,
    val industries: List<String>,
    val eventTags: Set<String>,
    val summary: String,
    val dataSource: String = "公开资料整理（游戏化归一化）",
    val dataYear: Int = 2025
) {
    val isMunicipality: Boolean get() = level == CityLevel.MUNICIPALITY
    val isSubProvincial: Boolean get() = level.isSubProvincialTier
    val isSeparatelyPlanned: Boolean get() = level == CityLevel.SEPARATELY_PLANNED
    val isProvincialCapital: Boolean
        get() = isMunicipality || level == CityLevel.PROVINCIAL_CAPITAL || name in CAPITAL_CITIES

    val cityType: String get() = level.label
    val levelLabel: String get() = level.label

    /** 文化影响力：由文化底蕴与城市层级共同决定。 */
    val cultureInfluence: Int get() = (culture * 70 + level.tier * 6).coerceIn(0, 100)

    /** 教育水平（别名，便于界面语义统一）。 */
    val educationLevel: Int get() = education

    /** 文化资源：由历史底蕴与旅游资源推导。 */
    val culturalResources: Int get() = (historyLevel * 55 + tourismLevel * 45) / 100

    /** 媒体影响力：国际化与文化的综合。 */
    val mediaInfluence: Int get() = (culture * 55 + internationalInfluence * 45) / 100

    /** 人均经济水平：经济总量越高质量越高，人口规模越大摊薄。 */
    val gdpPerCapita: Int get() = (gdp * 72 + (100 - population) * 28) / 100

    /** 政治重要性：层级越高、政治权重越大。 */
    val politicalImportance: Int get() = (level.tier * 13 + politics * 35 / 100).coerceIn(0, 100)

    /** 中央层面重要性。 */
    val centralImportance: Int get() = (level.tier * 15 + strategicImportance * 25 / 100).coerceIn(0, 100)

    /** 区域辐射能力。 */
    val regionalImportance: Int
        get() = (economic * 40 + strategicImportance * 35 + transportation * 25) / 100

    /** 行政重要性。 */
    val administrativeImportance: Int get() = (level.tier * 18).coerceIn(0, 100)

    val industryText: String get() = if (industries.isEmpty()) "综合" else industries.joinToString("、")

    /** 星级（1～5），用于界面展示，不暗示任何「推荐」。 */
    fun stars(value: Int): Int = ((value.coerceIn(0, 100) + 19) / 20).coerceIn(1, 5)

    companion object {
        /** 同时是省会但按副省级管理的城市（用于 [isProvincialCapital] 判定）。 */
        private val CAPITAL_CITIES = setOf(
            "广州", "成都", "武汉", "南京", "杭州", "西安", "济南"
        )
    }
}

/** 省级行政区档案，由城市数据聚合得到。 */
data class ProvinceData(
    val name: String,
    val region: String,
    val cityIds: List<String>,
    val capitalCityId: String?
) {
    val cityCount: Int get() = cityIds.size
}

/** 玩家的一段城市任职经历，进入存档形成全国仕途履历。 */
data class CityStint(
    val cityId: String,
    val cityName: String,
    val province: String,
    val positionLabel: String,
    val startAbsQuarter: Int,
    val endAbsQuarter: Int = -1
) {
    val isCurrent: Boolean get() = endAbsQuarter < 0

    /** 展示用职位名；旧存档的「主任科员」等名会迁移为「副科级·部门副职」。 */
    val displayPositionLabel: String get() = migrateLegacyPositionText(positionLabel)
}