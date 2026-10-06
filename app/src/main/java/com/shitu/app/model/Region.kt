package com.shitu.app.model

/**
 * 城市职能特色（游戏内部分类，用于事件倾向与项目加成）。
 *
 * 第十六阶段起，游戏使用真实中国城市数据；这里的分类只是玩法标签，
 * 便于让不同城市有不同的事件倾向与治理难度，不是对城市的现实评价。
 */
enum class RegionTrait(val label: String, val summary: String) {
    ECONOMIC_HUB("经济强区", "经济与财政基础好，但对能力和标准的要求也更高。"),
    INDUSTRIAL("工业重镇", "工业底子厚，税收可观，污染治理压力大。"),
    AGRICULTURAL("农业大县", "农业占比高，财政偏紧，民生事务繁杂。"),
    TOURISM("旅游名城", "服务业与名气是优势，对环境与秩序要求高。"),
    RESOURCE("资源型地区", "靠资源吃饭，收益快，转型压力也大。"),
    UNDERDEVELOPED("后发潜力地区", "经济体量相对不大，但政策空间与追赶潜力大。"),
    BALANCED("综合型地区", "各项均衡，没有明显短板，也没有特别长处。")
}

/**
 * 地区发展阶段。由治理水平推导，不单独存档，避免与指标不同步。
 */
enum class RegionStage(val label: String, val description: String) {
    LAGGING("落后", "底子薄弱，先解决基本运转问题。"),
    STARTING("起步", "开始有起色，需要把势头稳住。"),
    DEVELOPING("发展", "进入上升期，机会与问题同时变多。"),
    IMPROVING("改善", "各项指标明显好转，群众感受得到变化。"),
    PROSPEROUS("繁荣", "整体进入良性循环，但高位守成更难。");

    companion object {
        fun of(development: Int): RegionStage = when {
            development < 42 -> LAGGING
            development < 50 -> STARTING
            development < 58 -> DEVELOPING
            development < 68 -> IMPROVING
            else -> PROSPEROUS
        }
    }
}

/**
 * 治理形势。与「发展阶段」构成两条独立的线：
 * 发展水平可以上升，但短期形势仍可能因为风险与压力变差。
 */
enum class RegionCondition(val label: String, val description: String) {
    STABLE("稳定", "局面平稳，按计划推进即可。"),
    PRESSURE("承压", "矛盾开始积累，需要主动处置。"),
    CRISIS("危机", "问题集中爆发，处理不好会连累仕途。"),
    RECOVERING("恢复", "危机刚过，重点是重建信心。");

    companion object {
        fun of(pressure: Int, risk: Int, approval: Int): RegionCondition = when {
            pressure >= 72 || risk >= 70 -> CRISIS
            pressure >= 58 || approval <= 38 -> PRESSURE
            pressure >= 48 -> RECOVERING
            else -> STABLE
        }
    }
}

/**
 * 一座真实任职城市的运行档案（辖区口径）。
 * 由 [CityDataRepository] 的真实城市数据经游戏化缩放得到，
 * 指标沿用辖区的九个维度，另外补充产业结构、发展潜力与治理难度。
 */
data class RegionProfile(
    val id: String,
    val name: String,
    /** 省级行政区，例如「广东省」。 */
    val province: String = "",
    /** 真实城市行政层级。 */
    val cityLevel: CityLevel = CityLevel.ORDINARY,
    /** 城市含权量（0～100）。 */
    val powerWeight: Int = 50,
    /** 城市产业与战略标签，用于事件池门控。 */
    val eventTags: Set<String> = emptySet(),
    val level: JurisdictionLevel,
    val trait: RegionTrait,
    /** 低层级岗位时的驻地名称，例如「深圳城区」。 */
    val seatName: String,
    val population: Int,
    val economy: Int,
    val livelihood: Int,
    val security: Int,
    val education: Int,
    val medical: Int,
    val infrastructure: Int,
    val environment: Int,
    val approval: Int,
    val officialSatisfaction: Int,
    val industry: Int,
    val agriculture: Int,
    val service: Int,
    /** 发展潜力：越高越容易靠项目做出成绩。 */
    val potential: Int,
    /** 治理难度：越高说明任务越重、标准越高（游戏模拟参数）。 */
    val challenge: Int,
    val financeBase: Int,
    val summary: String
) {
    val traitLabel: String get() = trait.label
    /** 是否直辖市。 */
    val isMunicipality: Boolean get() = cityLevel == CityLevel.MUNICIPALITY
    /** 是否属于副省级体系（含计划单列市）。 */
    val isSubProvincial: Boolean get() = cityLevel.isSubProvincialTier
    val cityTypeLabel: String get() = cityLevel.label
}
