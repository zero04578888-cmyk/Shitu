package com.shitu.app.game

import com.shitu.app.model.DistrictDelta
import com.shitu.app.model.EconomicPhase
import com.shitu.app.model.EraState
import com.shitu.app.model.MeritKind
import com.shitu.app.model.NewsCategory
import com.shitu.app.model.NewsLevel
import com.shitu.app.model.PolicyTag
import com.shitu.app.model.RegionTrait

/** 世界事件影响的地区范围。 */
enum class WorldEventScope {
    /** 影响所有地区。 */
    GLOBAL,

    /** 只影响玩家当前任职的地区。 */
    PLAYER_REGION,

    /** 随机影响一个其它地区。 */
    RANDOM_REGION
}

/** 一次世界事件带来的效果，全部为虚构的游戏参数。 */
data class WorldEventEffect(
    val economyIndex: Int = 0,
    val momentum: Int = 0,
    val district: DistrictDelta = DistrictDelta(),
    val playerFinance: Int = 0,
    val playerRisk: Int = 0,
    val playerApproval: Int = 0,
    val playerMerit: Map<MeritKind, Int> = emptyMap()
) {
    val isEmpty: Boolean
        get() = economyIndex == 0 && momentum == 0 && district.isEmpty &&
            playerFinance == 0 && playerRisk == 0 && playerApproval == 0 && playerMerit.isEmpty()
}

/**
 * 一个世界事件模板。世界事件不是「玩家点一下」，而是世界自己在运转时发生的事，
 * 会同时影响经济、地区指标、玩家财政或风险，并写进【世界动态】。
 */
data class WorldEventTemplate(
    val id: String,
    val title: String,
    val detail: String,
    val category: NewsCategory = NewsCategory.WORLD,
    val level: NewsLevel = NewsLevel.MEDIUM,
    val scope: WorldEventScope = WorldEventScope.GLOBAL,
    val weight: Int = 5,
    /** 触发条件：经济景气指数区间。 */
    val minEconomy: Int = 0,
    val maxEconomy: Int = 100,
    /** 触发条件：玩家所在地区的环境指数区间。 */
    val minEnvironment: Int = 0,
    val maxEnvironment: Int = 100,
    /** 只在这些时代 / 周期 / 地区特色下出现；留空表示不限制。 */
    val eras: Set<EraState> = emptySet(),
    val phases: Set<EconomicPhase> = emptySet(),
    val traits: Set<RegionTrait> = emptySet(),
    /** 最早在第几年之后才可能出现。 */
    val minYear: Int = 1,
    val effect: WorldEventEffect = WorldEventEffect()
)

/** 一条长期因果链：玩家过去累计的某种政策取向，会在若干年后以结果的形式回到世界。 */
data class CausalChain(
    val id: String,
    val tag: PolicyTag,
    /** 需要累计的政策强度门槛。 */
    val threshold: Int,
    /** 最早在第几年之后结算。 */
    val minYear: Int = 2,
    /** 只在这些条件下触发（环境上下限）。 */
    val maxEnvironment: Int = 100,
    val minEnvironment: Int = 0,
    val title: String,
    val detail: String,
    val level: NewsLevel = NewsLevel.MEDIUM,
    val scope: WorldEventScope = WorldEventScope.PLAYER_REGION,
    val effect: WorldEventEffect = WorldEventEffect()
)

/**
 * 第十三阶段：世界事件库与长期因果链。
 * 全部为虚构设定，不涉及任何现实事件、地区或人物。
 */
object WorldEventLibrary {

    // ===== 世界事件（36 个） =====

    val events: List<WorldEventTemplate> = listOf(
        // —— 经济周期 ——
        WorldEventTemplate(
            id = "we_boom_expansion", title = "整体经济扩张", category = NewsCategory.ECONOMY,
            detail = "订单变多、企业开工率上升，各行各业都在扩招。",
            level = NewsLevel.MEDIUM, phases = setOf(EconomicPhase.BOOM),
            effect = WorldEventEffect(economyIndex = 3, momentum = 2, district = DistrictDelta(economy = 2, employment = 2, industry = 1))
        ),
        WorldEventTemplate(
            id = "we_boom_investment_wave", title = "投资热潮", category = NewsCategory.ECONOMY,
            detail = "外来资本开始涌入，谈项目的企业明显多了起来。",
            level = NewsLevel.HIGH, phases = setOf(EconomicPhase.BOOM, EconomicPhase.GROWTH), minEconomy = 62,
            effect = WorldEventEffect(economyIndex = 2, district = DistrictDelta(economy = 2, industry = 3, service = 1, finance = 6))
        ),
        WorldEventTemplate(
            id = "we_slowdown_orders", title = "订单增速放缓", category = NewsCategory.ECONOMY,
            detail = "企业反映订单不如上季度，开始压减新的投入。",
            level = NewsLevel.MEDIUM, phases = setOf(EconomicPhase.SLOWDOWN, EconomicPhase.STABLE),
            effect = WorldEventEffect(economyIndex = -2, momentum = -1, district = DistrictDelta(economy = -2, industry = -2, employment = -1))
        ),
        WorldEventTemplate(
            id = "we_recession_layoffs", title = "企业收缩裁员", category = NewsCategory.ECONOMY,
            detail = "几家规模企业缩减产能，就业压力开始显现。",
            level = NewsLevel.HIGH, phases = setOf(EconomicPhase.RECESSION), minYear = 2,
            effect = WorldEventEffect(economyIndex = -3, district = DistrictDelta(economy = -3, employment = -4, finance = -5))
        ),
        WorldEventTemplate(
            id = "we_recovery_restart", title = "企业陆续复工", category = NewsCategory.ECONOMY,
            detail = "最困难的时候过去了，停摆的产线重新转了起来。",
            level = NewsLevel.MEDIUM, phases = setOf(EconomicPhase.RECOVERY),
            effect = WorldEventEffect(economyIndex = 2, momentum = 1, district = DistrictDelta(economy = 2, employment = 2))
        ),
        WorldEventTemplate(
            id = "we_industry_enter", title = "新产业项目落地", category = NewsCategory.ECONOMY,
            detail = "一家外地企业决定在本地设厂，正在对接用地与用工。",
            level = NewsLevel.HIGH, minEconomy = 50, traits = setOf(RegionTrait.ECONOMIC_HUB, RegionTrait.INDUSTRIAL, RegionTrait.BALANCED),
            effect = WorldEventEffect(district = DistrictDelta(industry = 4, economy = 2, employment = 2, environment = -1, finance = 5))
        ),
        WorldEventTemplate(
            id = "we_industry_exit", title = "老企业外迁", category = NewsCategory.ECONOMY,
            detail = "一家老企业把产线搬去了成本更低的地方，本地少了一块税源。",
            level = NewsLevel.HIGH, minYear = 3, maxEnvironment = 55,
            effect = WorldEventEffect(district = DistrictDelta(industry = -3, employment = -2, finance = -6))
        ),
        WorldEventTemplate(
            id = "we_industry_upgrade", title = "传统产业技改", category = NewsCategory.ECONOMY,
            detail = "几家企业联合做技术改造，单位能耗降了下来。",
            level = NewsLevel.MEDIUM, minYear = 3, phases = setOf(EconomicPhase.GROWTH, EconomicPhase.BOOM),
            effect = WorldEventEffect(district = DistrictDelta(industry = 2, technology = 2, environment = 2, economy = 1))
        ),
        WorldEventTemplate(
            id = "we_logistics_route", title = "物流通道打通", category = NewsCategory.ECONOMY,
            detail = "一条新的货运线路开通，本地货物出得去、原料进得来。",
            level = NewsLevel.MEDIUM, traits = setOf(RegionTrait.BALANCED, RegionTrait.AGRICULTURAL, RegionTrait.INDUSTRIAL),
            effect = WorldEventEffect(district = DistrictDelta(logistics = 5, economy = 2, service = 1))
        ),
        WorldEventTemplate(
            id = "we_tech_breakthrough", title = "数字产业突破", category = NewsCategory.ECONOMY,
            detail = "本地一家科技企业拿到重要订单，带动上下游一起忙起来。",
            level = NewsLevel.HIGH, minEconomy = 58, traits = setOf(RegionTrait.ECONOMIC_HUB, RegionTrait.BALANCED),
            effect = WorldEventEffect(district = DistrictDelta(technology = 5, economy = 2, employment = 1))
        ),
        // —— 自然与灾害 ——
        WorldEventTemplate(
            id = "we_flood", title = "暴雨洪涝", category = NewsCategory.REGION,
            detail = "连续强降雨，部分低洼地段受淹，农业与交通受到影响。",
            level = NewsLevel.HIGH, weight = 6, minYear = 1,
            effect = WorldEventEffect(district = DistrictDelta(agriculture = -3, infrastructure = -2, environment = 1, finance = -6, security = -1), playerFinance = -5, playerRisk = 1)
        ),
        WorldEventTemplate(
            id = "we_drought", title = "持续干旱", category = NewsCategory.REGION,
            detail = "降水偏少，农业用水吃紧，群众开始反映灌溉问题。",
            level = NewsLevel.MEDIUM, minYear = 2, traits = setOf(RegionTrait.AGRICULTURAL, RegionTrait.UNDERDEVELOPED),
            effect = WorldEventEffect(district = DistrictDelta(agriculture = -4, economy = -1), playerApproval = -1)
        ),
        WorldEventTemplate(
            id = "we_heat_wave", title = "持续高温", category = NewsCategory.REGION,
            detail = "用电负荷创下新高，部分企业被要求错峰生产。",
            level = NewsLevel.MEDIUM, minYear = 2,
            effect = WorldEventEffect(district = DistrictDelta(industry = -1, environment = -1, medical = -1))
        ),
        WorldEventTemplate(
            id = "we_storm_damage", title = "大风冰雹损坏设施", category = NewsCategory.REGION,
            detail = "一场强对流天气刮坏了部分厂房屋顶和农业大棚。",
            level = NewsLevel.MEDIUM, weight = 4,
            effect = WorldEventEffect(district = DistrictDelta(infrastructure = -2, agriculture = -2, finance = -4))
        ),
        WorldEventTemplate(
            id = "we_pollution_spike", title = "空气质量下降", category = NewsCategory.REGION,
            detail = "监测数据显示空气指标变差，环保部门开始排查。",
            level = NewsLevel.MEDIUM, maxEnvironment = 48,
            effect = WorldEventEffect(district = DistrictDelta(environment = -3, medical = -1), playerApproval = -2)
        ),
        WorldEventTemplate(
            id = "we_eco_restoration", title = "生态修复见效", category = NewsCategory.REGION,
            detail = "一批生态修复项目完成，河湖水质和绿化明显改善。",
            level = NewsLevel.MEDIUM, minEnvironment = 55,
            effect = WorldEventEffect(district = DistrictDelta(environment = 3, attractiveness = 2, tourism = 1))
        ),
        // —— 人口与人才 ——
        WorldEventTemplate(
            id = "we_population_inflow", title = "人口净流入", category = NewsCategory.REGION,
            detail = "因为就业机会增多，周边不少人选择来这里工作生活。",
            level = NewsLevel.MEDIUM, minEconomy = 58,
            effect = WorldEventEffect(district = DistrictDelta(population = 3, employment = 1, attractiveness = 1, infrastructure = -1))
        ),
        WorldEventTemplate(
            id = "we_population_outflow", title = "人口外流", category = NewsCategory.REGION,
            detail = "年轻人外出务工的多了，本地劳动力开始紧张。",
            level = NewsLevel.MEDIUM, maxEnvironment = 45, minYear = 3,
            effect = WorldEventEffect(district = DistrictDelta(population = -3, employment = -1, attractiveness = -1))
        ),
        WorldEventTemplate(
            id = "we_talent_inflow", title = "人才引进计划", category = NewsCategory.REGION,
            detail = "一批专业人才落户本地，教育医疗和产业都跟着受益。",
            level = NewsLevel.MEDIUM, minEconomy = 55,
            effect = WorldEventEffect(district = DistrictDelta(technology = 2, education = 2, medical = 2, population = 1))
        ),
        WorldEventTemplate(
            id = "we_talent_outflow", title = "骨干人才流失", category = NewsCategory.REGION,
            detail = "几位业务骨干被外地单位挖走，队伍青黄不接。",
            level = NewsLevel.MEDIUM, minYear = 3,
            effect = WorldEventEffect(district = DistrictDelta(technology = -2, education = -1, service = -1))
        ),
        // —— 财政 ——
        WorldEventTemplate(
            id = "we_fiscal_tight", title = "财政收支吃紧", category = NewsCategory.ECONOMY,
            detail = "刚性支出增加、收入跟不上，各部门的预算都要压一压。",
            level = NewsLevel.HIGH, maxEconomy = 46,
            effect = WorldEventEffect(district = DistrictDelta(finance = -8), playerFinance = -6, playerRisk = 1)
        ),
        WorldEventTemplate(
            id = "we_transfer_payment", title = "上级转移支付到位", category = NewsCategory.ECONOMY,
            detail = "一笔专项转移支付拨付下来，缓了一口气。",
            level = NewsLevel.MEDIUM, maxEconomy = 58,
            effect = WorldEventEffect(district = DistrictDelta(finance = 10), playerFinance = 12)
        ),
        WorldEventTemplate(
            id = "we_tax_reform", title = "税费政策调整", category = NewsCategory.ECONOMY,
            detail = "新的减负政策落地，企业负担轻了，短期财政要过紧日子。",
            level = NewsLevel.MEDIUM, minYear = 4,
            effect = WorldEventEffect(district = DistrictDelta(finance = -5, industry = 2, service = 2))
        ),
        WorldEventTemplate(
            id = "we_debt_pressure", title = "债务压力显现", category = NewsCategory.ECONOMY,
            detail = "到期债务集中，平台公司资金周转出现困难。",
            level = NewsLevel.HIGH, minYear = 5, maxEconomy = 50,
            effect = WorldEventEffect(district = DistrictDelta(finance = -9), playerRisk = 3)
        ),
        // —— 基建与服务 ——
        WorldEventTemplate(
            id = "we_infra_upgrade", title = "基础设施升级", category = NewsCategory.REGION,
            detail = "一批道路、管网完成升级，通行和排水都顺畅了不少。",
            level = NewsLevel.MEDIUM, minYear = 2,
            effect = WorldEventEffect(district = DistrictDelta(infrastructure = 4, economy = 1, attractiveness = 1))
        ),
        WorldEventTemplate(
            id = "we_transport_hub", title = "交通枢纽扩建", category = NewsCategory.REGION,
            detail = "客货运枢纽扩建完成，人流物流集散能力明显提升。",
            level = NewsLevel.HIGH, minYear = 4, traits = setOf(RegionTrait.ECONOMIC_HUB, RegionTrait.BALANCED),
            effect = WorldEventEffect(district = DistrictDelta(logistics = 4, economy = 3, service = 2, finance = 4))
        ),
        WorldEventTemplate(
            id = "we_utility_shortage", title = "公用设施紧张", category = NewsCategory.REGION,
            detail = "人口增长快于配套建设，供水供电开始出现紧张。",
            level = NewsLevel.MEDIUM, minYear = 3,
            effect = WorldEventEffect(district = DistrictDelta(infrastructure = -2, employment = -1), playerApproval = -2)
        ),
        WorldEventTemplate(
            id = "we_public_project", title = "重大公共项目开工", category = NewsCategory.PROJECT,
            detail = "一项关系民生的公共工程正式动工，居民关注度很高。",
            level = NewsLevel.HIGH, minYear = 2,
            effect = WorldEventEffect(district = DistrictDelta(infrastructure = 2, employment = 2, finance = -3), playerApproval = 1)
        ),
        WorldEventTemplate(
            id = "we_public_service_gap", title = "公共服务供给不足", category = NewsCategory.REGION,
            detail = "部分区域教育医疗资源紧张，群众意见开始集中。",
            level = NewsLevel.MEDIUM, maxEnvironment = 60,
            effect = WorldEventEffect(district = DistrictDelta(education = -1, medical = -1), playerApproval = -3)
        ),
        WorldEventTemplate(
            id = "we_safety_campaign", title = "安全生产大检查", category = NewsCategory.REGION,
            detail = "上级部署安全生产检查，一批隐患被要求限期整改。",
            level = NewsLevel.MEDIUM, minYear = 2,
            effect = WorldEventEffect(district = DistrictDelta(security = 3, industry = -1, finance = -2))
        ),
        WorldEventTemplate(
            id = "we_health_outbreak", title = "季节性流感高发", category = NewsCategory.REGION,
            detail = "医院门诊量明显上升，医疗系统承受压力。",
            level = NewsLevel.MEDIUM, minYear = 1,
            effect = WorldEventEffect(district = DistrictDelta(medical = -2, service = -1), playerApproval = -2)
        ),
        WorldEventTemplate(
            id = "we_education_reform", title = "教育资源整合", category = NewsCategory.REGION,
            detail = "几所学校完成整合，优质教育资源覆盖面扩大。",
            level = NewsLevel.MEDIUM, minYear = 3,
            effect = WorldEventEffect(district = DistrictDelta(education = 4, attractiveness = 2))
        ),
        WorldEventTemplate(
            id = "we_culture_event", title = "举办大型文化活动", category = NewsCategory.REGION,
            detail = "一场文化活动吸引了不少外地游客，餐饮住宿都跟着热闹。",
            level = NewsLevel.MEDIUM, traits = setOf(RegionTrait.TOURISM, RegionTrait.BALANCED),
            effect = WorldEventEffect(district = DistrictDelta(tourism = 4, service = 3, environment = -1))
        ),
        // —— 区域发展与治理 ——
        WorldEventTemplate(
            id = "we_neighbor_competition", title = "周边地区加大招商力度", category = NewsCategory.REGION,
            detail = "邻近地区在招商上开出更优条件，本地项目面临被分流。",
            level = NewsLevel.MEDIUM, minEconomy = 50, minYear = 3,
            effect = WorldEventEffect(district = DistrictDelta(industry = -2, finance = -4), playerRisk = 1)
        ),
        WorldEventTemplate(
            id = "we_region_award", title = "获得上级表彰", category = NewsCategory.REGION,
            detail = "本地某项工作得到上级肯定，被作为典型经验推广。",
            level = NewsLevel.HIGH, minEconomy = 60,
            effect = WorldEventEffect(district = DistrictDelta(finance = 6, attractiveness = 2), playerApproval = 2, playerMerit = mapOf(MeritKind.INNOVATION to 3))
        ),
        WorldEventTemplate(
            id = "we_inspection_round", title = "上级专项检查", category = NewsCategory.REGION,
            detail = "检查组分批下沉，工作台账和现场都要经得起看。",
            level = NewsLevel.MEDIUM, minYear = 2,
            effect = WorldEventEffect(district = DistrictDelta(finance = -3), playerRisk = 2)
        ),
        WorldEventTemplate(
            id = "we_market_prosperity", title = "消费市场回暖", category = NewsCategory.ECONOMY,
            detail = "商圈人流增加，小店和餐饮的生意明显好起来。",
            level = NewsLevel.MEDIUM, minEconomy = 52,
            effect = WorldEventEffect(district = DistrictDelta(service = 4, economy = 2, employment = 2))
        )
    )

    private val byId: Map<String, WorldEventTemplate> = events.associateBy { it.id }

    fun find(id: String?): WorldEventTemplate? = id?.let { byId[it] }

    /** 当前条件下可能发生的世界事件。 */
    fun candidates(state: GameState): List<WorldEventTemplate> {
        val economy = state.world.economy
        val environment = state.world.districtOf(state.regionId).environment
        val trait = RegionEngine.regionOf(state).trait
        val recent = state.world.news.takeLast(12).map { it.id }.toSet()
        return events.filter { event ->
            if (state.year < event.minYear) return@filter false
            if (economy.index < event.minEconomy || economy.index > event.maxEconomy) return@filter false
            if (environment < event.minEnvironment || environment > event.maxEnvironment) return@filter false
            if (event.phases.isNotEmpty() && economy.phase !in event.phases) return@filter false
            if (event.eras.isNotEmpty() && state.world.era !in event.eras) return@filter false
            if (event.traits.isNotEmpty() && trait !in event.traits) return@filter false
            // 避免同一件事短时间内反复出现。
            if (recent.any { it.startsWith("we_${event.id}_") }) return@filter false
            true
        }
    }

    /** 按权重抽一个世界事件；条件不满足时返回 null。 */
    fun pick(state: GameState, rng: GameRng): WorldEventTemplate? {
        val pool = candidates(state)
        if (pool.isEmpty()) return null
        val total = pool.sumOf { it.weight.coerceAtLeast(1) }
        var roll = rng.nextInt(total)
        for (event in pool) {
            roll -= event.weight.coerceAtLeast(1)
            if (roll < 0) return event
        }
        return pool.last()
    }

    // ===== 长期因果链（12 条） =====

    val chains: List<CausalChain> = listOf(
        CausalChain(
            id = "causal_industry_pollution", tag = PolicyTag.INDUSTRY, threshold = 8, minYear = 2,
            title = "工业扩张带来环境压力", level = NewsLevel.HIGH,
            detail = "前几年上马的工业项目陆续投产，产能起来了，排放和能耗也跟了上来。",
            effect = WorldEventEffect(district = DistrictDelta(environment = -6, medical = -2, economy = 1), playerApproval = -2, playerRisk = 2)
        ),
        CausalChain(
            id = "causal_pollution_complaint", tag = PolicyTag.INDUSTRY, threshold = 14, minYear = 3,
            maxEnvironment = 46,
            title = "群众集中反映环境问题", level = NewsLevel.HIGH,
            detail = "长期积累的环境问题开始集中反映，投诉量明显上升。",
            effect = WorldEventEffect(district = DistrictDelta(environment = -2, attractiveness = -3), playerApproval = -5, playerRisk = 3)
        ),
        CausalChain(
            id = "causal_green_recovery", tag = PolicyTag.ENVIRONMENT, threshold = 6, minYear = 2,
            title = "生态改善带动人气", level = NewsLevel.MEDIUM,
            detail = "持续的环境投入开始见效，空气和水质改善带来了新的发展机会。",
            effect = WorldEventEffect(district = DistrictDelta(environment = 3, attractiveness = 4, tourism = 3, medical = 1))
        ),
        CausalChain(
            id = "causal_education_talent", tag = PolicyTag.EDUCATION, threshold = 6, minYear = 2,
            title = "教育投入留住人才", level = NewsLevel.MEDIUM,
            detail = "教育资源改善后，越来越多家庭愿意留下来，人才结构也在变好。",
            effect = WorldEventEffect(district = DistrictDelta(education = 2, technology = 3, population = 2, attractiveness = 2))
        ),
        CausalChain(
            id = "causal_infra_logistics", tag = PolicyTag.INFRASTRUCTURE, threshold = 8, minYear = 2,
            title = "基建完善激活物流", level = NewsLevel.MEDIUM,
            detail = "前期的道路与管网投入开始转化成物流和产业优势。",
            effect = WorldEventEffect(district = DistrictDelta(logistics = 5, economy = 3, employment = 2))
        ),
        CausalChain(
            id = "causal_welfare_satisfaction", tag = PolicyTag.WELFARE, threshold = 6, minYear = 2,
            title = "民生改善赢得口碑", level = NewsLevel.MEDIUM,
            detail = "一以贯之地抓民生，群众对治理的认可度在稳步上升。",
            effect = WorldEventEffect(district = DistrictDelta(attractiveness = 3, service = 2, medical = 1), playerApproval = 3)
        ),
        CausalChain(
            id = "causal_finance_austerity", tag = PolicyTag.FINANCE, threshold = 6, minYear = 2,
            title = "收紧支出留下欠账", level = NewsLevel.MEDIUM,
            detail = "长期控制支出，财政是稳住了，但教育和医疗的短板也更明显了。",
            effect = WorldEventEffect(district = DistrictDelta(finance = 6, education = -2, medical = -2), playerApproval = -2)
        ),
        CausalChain(
            id = "causal_agri_upgrade", tag = PolicyTag.AGRICULTURE, threshold = 6, minYear = 2,
            title = "农业升级稳住基本盘", level = NewsLevel.MEDIUM,
            detail = "农业基础条件改善，农民收入稳了，人口外流也慢了下来。",
            effect = WorldEventEffect(district = DistrictDelta(agriculture = 4, population = 2, environment = 2, economy = 1))
        ),
        CausalChain(
            id = "causal_tourism_boom", tag = PolicyTag.TOURISM, threshold = 6, minYear = 2,
            title = "文旅热度带来新课题", level = NewsLevel.MEDIUM,
            detail = "文旅项目带来人流和收入，也对环境承载和秩序提出了更高要求。",
            effect = WorldEventEffect(district = DistrictDelta(tourism = 4, service = 3, economy = 2, environment = -2), playerApproval = 1)
        ),
        CausalChain(
            id = "causal_digital_governance", tag = PolicyTag.TECHNOLOGY, threshold = 6, minYear = 2,
            title = "数字治理提升效率", level = NewsLevel.MEDIUM,
            detail = "前期投入的数字化系统开始发挥作用，办事效率和企业体验都在改善。",
            effect = WorldEventEffect(district = DistrictDelta(technology = 2, service = 3, finance = 3, economy = 1))
        ),
        CausalChain(
            id = "causal_overinvestment", tag = PolicyTag.FINANCE, threshold = 16, minYear = 3,
            title = "过度投资埋下隐患", level = NewsLevel.HIGH,
            detail = "摊子铺得太大，资金链开始吃紧，几个项目面临调整。",
            effect = WorldEventEffect(district = DistrictDelta(finance = -10, industry = -1), playerFinance = -8, playerRisk = 3)
        ),
        CausalChain(
            id = "causal_integrity_risk", tag = PolicyTag.INTEGRITY, threshold = 8, minYear = 3,
            title = "发展与廉政的平衡", level = NewsLevel.MEDIUM,
            detail = "一路大干快上之后，廉政与合规的压力也开始显现。",
            effect = WorldEventEffect(playerRisk = 4, district = DistrictDelta(finance = -3))
        )
    )

    private val chainById: Map<String, CausalChain> = chains.associateBy { it.id }

    fun chain(id: String?): CausalChain? = id?.let { chainById[it] }
}