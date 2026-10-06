package com.shitu.app.model

/**
 * 第十三阶段：动态世界与长期因果。
 *
 * 这里放「世界自己会变化」的那部分状态：经济周期、时代、地区动态、
 * 人口与产业、政策记录、决策记忆、世界动态、NPC 自主状态、关系网络与岗位空缺。
 * 全部为虚构的游戏设定，不涉及任何现实事件或人物。
 */

/** 经济周期阶段。带惯性：繁荣之后更容易走向稳定或放缓，衰退之后更容易复苏。 */
enum class EconomicPhase(
    val label: String,
    val description: String,
    /** 对财政收入的影响（每季度）。 */
    val incomeMod: Int,
    /** 对产业与企业投资意愿的影响。 */
    val industryMod: Int
) {
    BOOM("繁荣", "各行各业都在扩张，财政宽裕，招商最容易谈成。", 3, 3),
    GROWTH("增长", "整体向上，机会多于风险。", 2, 2),
    STABLE("稳定", "不温不火，按部就班。", 0, 0),
    SLOWDOWN("放缓", "增速下来了，投资开始观望。", -1, -1),
    RECESSION("衰退", "企业收缩、财政吃紧，项目容易延期。", -3, -3),
    RECOVERY("复苏", "最困难的时候过去了，但底子还薄。", 1, 1);

    companion object {
        fun byName(value: String?): EconomicPhase? = entries.firstOrNull { it.name == value }
    }
}

/**
 * 全球经济状态。
 *
 * @param phase 当前周期阶段
 * @param index 景气指数（0～100）
 * @param momentum 动能（-20～20），决定指数往哪边走
 * @param monthsInPhase 已持续月数，用来保证周期不会一月一变
 * @param history 最近经历过的阶段，用于展示与判断
 */
data class GlobalEconomy(
    val phase: EconomicPhase = EconomicPhase.STABLE,
    val index: Int = 50,
    val momentum: Int = 0,
    val monthsInPhase: Int = 0,
    val history: List<EconomicPhase> = emptyList()
) {
    fun withPhase(next: EconomicPhase): GlobalEconomy = copy(
        phase = next,
        monthsInPhase = 0,
        history = (history + next).takeLast(MAX_HISTORY)
    )

    companion object {
        const val MAX_HISTORY = 12
    }
}

/** 时代阶段。由经济、环境与长期改革压力推导，不单独写入选择。 */
enum class EraState(val label: String, val description: String) {
    DEVELOPMENT("发展期", "底子薄、任务重，重在把经济做起来。"),
    ADJUSTMENT("调整期", "发展带来压力，需要在增长与治理之间找平衡。"),
    REFORM("改革期", "旧办法不够用了，改革的机会与阻力同样大。"),
    PROSPERITY("繁荣期", "整体向好，守成与提质的压力更大。"),
    PRESSURE("压力期", "经济或环境吃紧，处置不好会连着出问题。");

    companion object {
        /**
         * 时代由多个条件共同决定，变化有先后顺序：
         * 先看压力，再看繁荣，然后是调整与改革，最后才是平稳发展。
         */
        fun of(economyIndex: Int, environment: Int, reformPressure: Int): EraState = when {
            economyIndex <= 35 || environment <= 32 -> PRESSURE
            economyIndex >= 72 && environment >= 45 -> PROSPERITY
            reformPressure >= 8 -> REFORM
            environment <= 46 || economyIndex <= 52 -> ADJUSTMENT
            else -> DEVELOPMENT
        }
    }
}

/** 六大产业。地区的产业结构会随政策与项目变化。 */
enum class Industry(val label: String) {
    MANUFACTURING("工业"),
    AGRICULTURE("农业"),
    TOURISM("旅游"),
    TECHNOLOGY("科技"),
    SERVICE("服务业"),
    LOGISTICS("物流");

    companion object {
        fun byName(value: String?): Industry? = entries.firstOrNull { it.name == value }
    }
}

/**
 * 一个地区的动态状态。静态设定在 [RegionProfile]，这里只放会变的量。
 * 所有指标都是 0～100 的指数，人口也是指数（不是真实人数）。
 */
data class DistrictState(
    val id: String,
    val population: Int = 50,
    val economy: Int = 50,
    val industry: Int = 40,
    val agriculture: Int = 40,
    val service: Int = 40,
    val technology: Int = 30,
    val tourism: Int = 20,
    val logistics: Int = 30,
    val employment: Int = 50,
    val education: Int = 50,
    val medical: Int = 50,
    val security: Int = 55,
    val infrastructure: Int = 50,
    val environment: Int = 55,
    val attractiveness: Int = 50,
    val finance: Int = 60
) {
    fun valueOf(industry: Industry): Int = when (industry) {
        Industry.MANUFACTURING -> this.industry
        Industry.AGRICULTURE -> agriculture
        Industry.TOURISM -> tourism
        Industry.TECHNOLOGY -> technology
        Industry.SERVICE -> service
        Industry.LOGISTICS -> logistics
    }

    fun withIndustry(industry: Industry, value: Int): DistrictState {
        val v = value.coerceIn(0, 100)
        return when (industry) {
            Industry.MANUFACTURING -> copy(industry = v)
            Industry.AGRICULTURE -> copy(agriculture = v)
            Industry.TOURISM -> copy(tourism = v)
            Industry.TECHNOLOGY -> copy(technology = v)
            Industry.SERVICE -> copy(service = v)
            Industry.LOGISTICS -> copy(logistics = v)
        }
    }

    /** 主导产业，用于界面展示与 NPC 行为倾向。 */
    val dominantIndustry: Industry
        get() = Industry.entries.maxByOrNull { valueOf(it) } ?: Industry.SERVICE

    /** 综合发展水平，用于时代与排名。 */
    val development: Int
        get() = (economy * 24 + employment * 16 + infrastructure * 16 +
            education * 12 + medical * 12 + environment * 10 + security * 10) / 100

    /** 把一组增量作用到地区上，所有指标都会被夹在合理区间内。 */
    fun apply(delta: DistrictDelta): DistrictState = copy(
        population = population + delta.population,
        economy = economy + delta.economy,
        industry = industry + delta.industry,
        agriculture = agriculture + delta.agriculture,
        service = service + delta.service,
        technology = technology + delta.technology,
        tourism = tourism + delta.tourism,
        logistics = logistics + delta.logistics,
        employment = employment + delta.employment,
        education = education + delta.education,
        medical = medical + delta.medical,
        security = security + delta.security,
        infrastructure = infrastructure + delta.infrastructure,
        environment = environment + delta.environment,
        attractiveness = attractiveness + delta.attractiveness,
        finance = finance + delta.finance
    ).normalized()
    fun normalized(): DistrictState = copy(
        population = population.coerceIn(MIN_POPULATION, MAX_POPULATION),
        economy = economy.coerceIn(0, 100),
        industry = industry.coerceIn(0, 100),
        agriculture = agriculture.coerceIn(0, 100),
        service = service.coerceIn(0, 100),
        technology = technology.coerceIn(0, 100),
        tourism = tourism.coerceIn(0, 100),
        logistics = logistics.coerceIn(0, 100),
        employment = employment.coerceIn(0, 100),
        education = education.coerceIn(0, 100),
        medical = medical.coerceIn(0, 100),
        security = security.coerceIn(0, 100),
        infrastructure = infrastructure.coerceIn(0, 100),
        environment = environment.coerceIn(0, 100),
        attractiveness = attractiveness.coerceIn(0, 100),
        finance = finance.coerceIn(0, MAX_FINANCE)
    )

    companion object {
        const val MIN_POPULATION = 1
        const val MAX_POPULATION = 160
        const val MAX_FINANCE = 9999

        /** 由地区静态设定生成初始动态状态。 */
        fun fromRegion(region: RegionProfile): DistrictState = DistrictState(
            id = region.id,
            population = region.population.coerceIn(MIN_POPULATION, MAX_POPULATION),
            economy = region.economy,
            industry = region.industry,
            agriculture = region.agriculture,
            service = region.service,
            technology = ((region.industry + region.service) / 2).coerceIn(0, 100),
            tourism = if (region.trait == RegionTrait.TOURISM) {
                (region.service + 12).coerceAtMost(100)
            } else {
                (region.service / 2).coerceIn(0, 100)
            },
            logistics = ((region.industry + region.agriculture) / 3 + 18).coerceIn(0, 100),
            employment = region.economy,
            education = region.education,
            medical = region.medical,
            security = region.security,
            infrastructure = region.infrastructure,
            environment = region.environment,
            attractiveness = (region.livelihood + region.environment + region.education + region.medical) / 4,
            finance = (region.financeBase / 2).coerceIn(0, MAX_FINANCE)
        )
    }
}

/**
 * 地区指标的一组增量。世界事件与长期政策后果都用它来批量修改地区。
 * 所有字段默认 0，未涉及的方向保持原样。
 */
data class DistrictDelta(
    val population: Int = 0,
    val economy: Int = 0,
    val industry: Int = 0,
    val agriculture: Int = 0,
    val service: Int = 0,
    val technology: Int = 0,
    val tourism: Int = 0,
    val logistics: Int = 0,
    val employment: Int = 0,
    val education: Int = 0,
    val medical: Int = 0,
    val security: Int = 0,
    val infrastructure: Int = 0,
    val environment: Int = 0,
    val attractiveness: Int = 0,
    val finance: Int = 0
) {
    val isEmpty: Boolean
        get() = population == 0 && economy == 0 && industry == 0 && agriculture == 0 &&
            service == 0 && technology == 0 && tourism == 0 && logistics == 0 &&
            employment == 0 && education == 0 && medical == 0 && security == 0 &&
            infrastructure == 0 && environment == 0 && attractiveness == 0 && finance == 0

    /** 与另一个增量相加，便于叠加多个来源。 */
    operator fun plus(other: DistrictDelta): DistrictDelta = DistrictDelta(
        population = population + other.population,
        economy = economy + other.economy,
        industry = industry + other.industry,
        agriculture = agriculture + other.agriculture,
        service = service + other.service,
        technology = technology + other.technology,
        tourism = tourism + other.tourism,
        logistics = logistics + other.logistics,
        employment = employment + other.employment,
        education = education + other.education,
        medical = medical + other.medical,
        security = security + other.security,
        infrastructure = infrastructure + other.infrastructure,
        environment = environment + other.environment,
        attractiveness = attractiveness + other.attractiveness,
        finance = finance + other.finance
    )

    /** 按比例缩放（用于按地区特色加权）。 */
    fun scaled(factor: Int): DistrictDelta = DistrictDelta(
        population = population * factor,
        economy = economy * factor,
        industry = industry * factor,
        agriculture = agriculture * factor,
        service = service * factor,
        technology = technology * factor,
        tourism = tourism * factor,
        logistics = logistics * factor,
        employment = employment * factor,
        education = education * factor,
        medical = medical * factor,
        security = security * factor,
        infrastructure = infrastructure * factor,
        environment = environment * factor,
        attractiveness = attractiveness * factor,
        finance = finance * factor
    )
}
/** 政策方向标签，用于长期后果的累积计算。 */
enum class PolicyTag(val label: String) {
    INDUSTRY("工业"),
    AGRICULTURE("农业"),
    TOURISM("旅游"),
    TECHNOLOGY("科技"),
    SERVICE("服务业"),
    LOGISTICS("物流"),
    WELFARE("民生"),
    EDUCATION("教育"),
    HEALTH("医疗"),
    ENVIRONMENT("环境"),
    INFRASTRUCTURE("基建"),
    FINANCE("财政"),
    SECURITY("治安"),
    REFORM("改革"),
    TALENT("人才"),
    INTEGRITY("廉政");

    companion object {
        fun byName(value: String?): PolicyTag? = entries.firstOrNull { it.name == value }
    }
}

/** 一条政策记录。玩家过去的治理取向会在这里累积，并影响未来。 */
data class PolicyRecord(
    val id: String,
    val year: Int,
    val month: Int,
    val title: String,
    val direction: String,
    val regionId: String,
    val tags: Set<PolicyTag>,
    val magnitude: Int,
    val summary: String = ""
)

/** 一条重大决策记忆。未来事件可以读取。 */
data class DecisionRecord(
    val id: String,
    val year: Int,
    val month: Int,
    val title: String,
    val choice: String,
    val consequence: String,
    val knownBy: List<String> = emptyList(),
    val regionId: String = "",
    val tags: Set<PolicyTag> = emptySet()
)

/** 世界动态分类。 */
enum class NewsCategory(val label: String) {
    ECONOMY("经济"),
    REGION("地区"),
    NPC("人物"),
    FACTION("派系"),
    PROJECT("项目"),
    POLICY("政策"),
    WORLD("世界"),
    PLAYER("玩家");

    companion object {
        fun byName(value: String?): NewsCategory? = entries.firstOrNull { it.name == value }
    }
}

/** 世界动态重要程度，决定是否需要在界面上突出。 */
enum class NewsLevel(val label: String, val weight: Int) {
    LOW("一般", 0),
    MEDIUM("关注", 1),
    HIGH("重要", 2);

    companion object {
        fun byName(value: String?): NewsLevel = entries.firstOrNull { it.name == value } ?: LOW
    }
}

/** 一条世界动态。全部是虚构内容。 */
data class WorldNews(
    val id: String,
    val year: Int,
    val month: Int,
    val category: NewsCategory,
    val title: String,
    val detail: String,
    val level: NewsLevel = NewsLevel.LOW,
    val regionId: String = ""
)

/** 一个岗位空缺。由退休、调任、晋升或组织调整产生。 */
data class Vacancy(
    val positionIndex: Int,
    val regionId: String,
    val reason: String,
    val createdMonth: Int,
    val filled: Boolean = false
)

/**
 * 一名 NPC 的动态状态。静态设定在 [NpcProfile]，这里只存会变的部分。
 * 即使是 NPC，也会自己成长、争取岗位、建立关系。
 */
data class NpcRuntime(
    val npcId: String,
    val ability: Int = 50,
    val merit: Int = 40,
    val influence: Int = 50,
    val positionIndex: Int = 0,
    val active: Boolean = true,
    val lastActionMonth: Int = -1,
    val agenda: String = ""
) {
    val power: Int get() = (ability * 30 + merit * 30 + influence * 25 + positionIndex * 15) / 100

    fun normalized(): NpcRuntime = copy(
        ability = ability.coerceIn(0, 100),
        merit = merit.coerceIn(0, 100),
        influence = influence.coerceIn(0, 100),
        positionIndex = positionIndex.coerceIn(0, POSITION_LADDER.size - 1)
    )
}