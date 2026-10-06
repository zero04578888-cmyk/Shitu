package com.shitu.app.model

/**
 * 第十六阶段：任职城市图鉴。
 *
 * 这里不再有虚构城市。数据全部来自 [CityDataRepository] 的**真实中国城市**，
 * 本对象只负责把城市档案转换成游戏运行所需的 [RegionProfile]（辖区口径）。
 *
 * 转换规则是明确的游戏化缩放，不是把现实 GDP 原样搬进游戏：
 *  - 经济类指标：`28 + 城市值 * 55 / 100`，落在 50～83；
 *  - 人口：`20 + 城市值 * 50 / 100`，落在 40～70；
 *  - 财政：`60 + 城市财政 * 105 / 100`，落在 100～165；
 * 其余指标由城市的经济、产业、文旅、科教、交通等属性推导。
 */
object RegionCatalog {

    /** 全部可任职城市（真实城市）。 */
    val regions: List<RegionProfile> = CityDataRepository.cities.map { toProfile(it) }

    private val byId: Map<String, RegionProfile> = regions.associateBy { it.id }

    /** 旧存档兜底城市：普通地级市。 */
    val default: RegionProfile = find(CityDataRepository.default.id) ?: regions.first()

    fun find(id: String?): RegionProfile? = id?.let { byId[it] }

    fun findOrDefault(id: String?): RegionProfile = find(id) ?: default

    fun randomSeeded(seed: Long): RegionProfile =
        find(CityDataRepository.randomSeeded(seed).id) ?: default

    /** 按特色筛选，用于调任时给出不同取向的候选城市。 */
    fun withTrait(trait: RegionTrait): List<RegionProfile> =
        regions.filter { it.trait == trait }

    /** 按城市行政层级筛选。 */
    fun byLevel(level: CityLevel): List<RegionProfile> =
        regions.filter { it.cityLevel == level }

    /** 直辖市数量。 */
    fun municipalitiesCount(): Int = regions.count { it.isMunicipality }

    /** 副省级体系（含计划单列市）数量。 */
    fun subProvincialCount(): Int = regions.count { it.isSubProvincial }

    /** 覆盖的省级行政区数量。 */
    fun provinceCount(): Int = regions.map { it.province }.distinct().size

    /** 城市对应的完整数据档案。 */
    fun cityOf(region: RegionProfile): CityData =
        CityDataRepository.findOrDefault(region.id)

    /** 城市对应的完整数据档案（按 id）。 */
    fun cityOf(id: String?): CityData = CityDataRepository.findOrDefault(id)

    private fun toProfile(city: CityData): RegionProfile {
        fun scaled(value: Int): Int = (28 + value * 55 / 100).coerceIn(0, 100)
        val security = (44 + city.economic * 18 / 100).coerceIn(0, 100)
        val environment = (40 + city.tourismLevel * 30 / 100 + (100 - city.industrialStrength) * 20 / 100)
            .coerceIn(0, 100)
        val approval = (48 + (city.culture - 60) / 5).coerceIn(40, 64)
        val official = (52 + (city.challenge - 60) / 6).coerceIn(42, 66)
        val agriculture = if (city.trait == RegionTrait.AGRICULTURAL) {
            80
        } else {
            (34 + (100 - city.economic) / 3).coerceIn(20, 70)
        }
        return RegionProfile(
            id = city.id,
            name = city.name,
            province = city.province,
            cityLevel = city.level,
            powerWeight = city.powerWeight,
            eventTags = city.eventTags,
            level = JurisdictionLevel.COUNTY,
            trait = city.trait,
            seatName = city.name + "城区",
            population = (20 + city.population * 50 / 100).coerceIn(10, 90),
            economy = scaled(city.economic),
            livelihood = scaled((city.serviceIndustryStrength * 45 + city.economic * 30 + city.tourismLevel * 25) / 100),
            security = security,
            education = scaled(city.education),
            medical = scaled((city.education + city.serviceIndustryStrength) / 2),
            infrastructure = scaled((city.economic + city.transportation) / 2),
            environment = environment,
            approval = approval,
            officialSatisfaction = official,
            industry = city.industrialStrength,
            agriculture = agriculture,
            service = city.serviceIndustryStrength,
            potential = city.potential,
            challenge = city.challenge,
            financeBase = (10 + city.fiscalStrength * 185 / 100).coerceIn(40, 210),
            summary = city.summary
        )
    }
}