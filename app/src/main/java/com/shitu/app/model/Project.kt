package com.shitu.app.model

/**
 * 第十二阶段：重大项目。
 *
 * 项目是「跨年度、要花钱、会带来长期影响」的治理手段，
 * 与一次性的政务措施区分开：项目一旦开工，就要连续投入若干个季度，
 * 中途停工会留下烂尾，完工才会兑现政绩与民望。
 * 全部为虚构的游戏设定。
 */
enum class ProjectCategory(val label: String) {
    INDUSTRY("产业"),
    INFRASTRUCTURE("基建"),
    PUBLIC_SERVICE("公共服务"),
    ENVIRONMENT("环保"),
    AGRICULTURE("农业"),
    DIGITAL("数字政务"),
    TOURISM("文旅"),
    HOUSING("安居")
}

enum class ProjectStatus(val label: String) {
    ONGOING("在建"),
    /** 第十三阶段：建成后进入运营期，持续产生收益，之后才最终收官。 */
    OPERATING("运营"),
    COMPLETED("已完工"),
    STALLED("停滞"),
    CANCELLED("已叫停")
}

/** 项目完工后的一次性回报。 */
data class ProjectReward(
    val indicators: Map<Indicator, Int> = emptyMap(),
    val merit: Map<MeritKind, Int> = emptyMap(),
    val financeGain: Int = 0,
    val approval: Int = 0,
    val risk: Int = 0,
    val record: String = ""
)

/**
 * 一个项目的静态设定。
 *
 * @param budget 总预算（万元），按季度分期投入。
 * @param quarters 建设周期（季度数）。
 * @param perQuarter 每季度建设带来的持续影响。
 * @param traitBonus 在对应特色地区会额外加成。
 */
data class ProjectProfile(
    val id: String,
    val label: String,
    val category: ProjectCategory,
    val description: String,
    val budget: Int,
    val quarters: Int,
    val minPositionIndex: Int = 0,
    val perQuarter: GovernanceEffect = GovernanceEffect(),
    val reward: ProjectReward = ProjectReward(),
    val traitBonus: Set<RegionTrait> = emptySet(),
    val risk: Int = 0
) {
    /** 每季度需要投入的财政。 */
    val quarterlyCost: Int get() = (budget / quarters).coerceAtLeast(1)

    /**
     * 第十三阶段：运营期长度（季度）。建设周期的一半，2～12 个季度，
     * 也就是 6～36 个月的运营期。运营期内持续产生财政收益。
     */
    val operationQuarters: Int get() = (quarters / 2).coerceIn(2, 12)

    /** 运营期每季度带来的财政收益。 */
    val revenuePerQuarter: Int get() = (budget / 16).coerceAtLeast(2)

    /** 这个项目在长期政策累积里代表的政策方向。 */
    val policyTags: Set<PolicyTag>
        get() = when (category) {
            ProjectCategory.INDUSTRY -> setOf(PolicyTag.INDUSTRY, PolicyTag.FINANCE)
            ProjectCategory.INFRASTRUCTURE -> setOf(PolicyTag.INFRASTRUCTURE)
            ProjectCategory.PUBLIC_SERVICE -> setOf(PolicyTag.WELFARE, PolicyTag.HEALTH)
            ProjectCategory.ENVIRONMENT -> setOf(PolicyTag.ENVIRONMENT)
            ProjectCategory.AGRICULTURE -> setOf(PolicyTag.AGRICULTURE)
            ProjectCategory.DIGITAL -> setOf(PolicyTag.TECHNOLOGY, PolicyTag.REFORM)
            ProjectCategory.TOURISM -> setOf(PolicyTag.TOURISM, PolicyTag.SERVICE)
            ProjectCategory.HOUSING -> setOf(PolicyTag.WELFARE, PolicyTag.INFRASTRUCTURE)
        }
}

/** 正在推进的项目状态（进入存档）。 */
data class ActiveProject(
    val id: String,
    val startedQuarter: Int,
    val invested: Int = 0,
    val progress: Int = 0,
    val status: ProjectStatus = ProjectStatus.ONGOING,
    val note: String = "",
    /** 运营期还剩多少季度；运营中才有效。 */
    val operationLeft: Int = 0,
    /** 建设过程中遇到过的波折次数，用于展示与统计。 */
    val incidents: Int = 0
) {
    val isActive: Boolean get() = status == ProjectStatus.ONGOING || status == ProjectStatus.OPERATING

    val isUnderConstruction: Boolean get() = status == ProjectStatus.ONGOING

    val isOperating: Boolean get() = status == ProjectStatus.OPERATING
}

/** 项目图鉴：14 个虚构项目，覆盖经济、民生、环保、数字化等方向。 */
object ProjectCatalog {

    val projects: List<ProjectProfile> = listOf(
        ProjectProfile(
            id = "proj_industrial_park", label = "产业园区一期",
            category = ProjectCategory.INDUSTRY,
            description = "平整土地、通水通电，引来第一批制造企业。投入大，但能长期养财政。",
            budget = 120, quarters = 6, minPositionIndex = 3,
            perQuarter = GovernanceEffect(indicators = mapOf(Indicator.INFRASTRUCTURE to 2)),
            reward = ProjectReward(
                indicators = mapOf(Indicator.ECONOMY to 8, Indicator.LIVELIHOOD to 2),
                merit = mapOf(MeritKind.ECONOMIC to 9, MeritKind.FINANCE to 4),
                financeGain = 20,
                record = "主持产业园区一期建设，为当地引来第一批规模企业。"
            ),
            traitBonus = setOf(RegionTrait.ECONOMIC_HUB, RegionTrait.INDUSTRIAL),
            risk = 3
        ),
        ProjectProfile(
            id = "proj_road", label = "县乡道路改造",
            category = ProjectCategory.INFRASTRUCTURE,
            description = "把通往几个偏远乡镇的破损道路翻修一遍，群众感受最直接。",
            budget = 80, quarters = 4,
            perQuarter = GovernanceEffect(indicators = mapOf(Indicator.INFRASTRUCTURE to 2)),
            reward = ProjectReward(
                indicators = mapOf(Indicator.INFRASTRUCTURE to 7, Indicator.APPROVAL to 4),
                merit = mapOf(MeritKind.LIVELIHOOD to 5, MeritKind.ECONOMIC to 2),
                record = "完成县乡道路改造，解决沿线群众出行难问题。"
            ),
            traitBonus = setOf(RegionTrait.AGRICULTURAL, RegionTrait.UNDERDEVELOPED)
        ),
        ProjectProfile(
            id = "proj_hospital", label = "县医院扩建",
            category = ProjectCategory.PUBLIC_SERVICE,
            description = "新增住院楼与设备，缓解群众看病往市里跑的问题。",
            budget = 100, quarters = 5, minPositionIndex = 3,
            perQuarter = GovernanceEffect(indicators = mapOf(Indicator.MEDICAL to 1)),
            reward = ProjectReward(
                indicators = mapOf(Indicator.MEDICAL to 8, Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 3),
                merit = mapOf(MeritKind.LIVELIHOOD to 8),
                record = "推动县医院扩建，基层就医条件明显改善。"
            )
        ),
        ProjectProfile(
            id = "proj_school", label = "新校区建设",
            category = ProjectCategory.PUBLIC_SERVICE,
            description = "把最拥挤的那所中学搬出来，短期只有支出，长期见口碑。",
            budget = 90, quarters = 4, minPositionIndex = 3,
            perQuarter = GovernanceEffect(indicators = mapOf(Indicator.EDUCATION to 1)),
            reward = ProjectReward(
                indicators = mapOf(Indicator.EDUCATION to 8, Indicator.APPROVAL to 3),
                merit = mapOf(MeritKind.LIVELIHOOD to 7),
                record = "建成新校区，缓解城区学位紧张。"
            )
        ),
        ProjectProfile(
            id = "proj_sewage", label = "污水管网治理",
            category = ProjectCategory.ENVIRONMENT,
            description = "把老城区的雨污分流做起来，见效慢，但环境指标会实实在在地变好。",
            budget = 85, quarters = 5,
            perQuarter = GovernanceEffect(indicators = mapOf(Indicator.ENVIRONMENT to 1)),
            reward = ProjectReward(
                indicators = mapOf(Indicator.ENVIRONMENT to 9, Indicator.LIVELIHOOD to 2, Indicator.APPROVAL to 2),
                merit = mapOf(MeritKind.INNOVATION to 5, MeritKind.LIVELIHOOD to 3),
                record = "完成污水管网治理，多年黑臭水体得到整治。"
            ),
            traitBonus = setOf(RegionTrait.INDUSTRIAL, RegionTrait.RESOURCE)
        ),
        ProjectProfile(
            id = "proj_rural_water", label = "农村饮水安全工程",
            category = ProjectCategory.AGRICULTURE,
            description = "给几个高坡村打深井、铺管道，是最容易被忽略也最该做的事。",
            budget = 60, quarters = 3,
            reward = ProjectReward(
                indicators = mapOf(Indicator.LIVELIHOOD to 7, Indicator.APPROVAL to 5),
                merit = mapOf(MeritKind.LIVELIHOOD to 6),
                record = "解决高坡村饮水安全问题。"
            ),
            traitBonus = setOf(RegionTrait.AGRICULTURAL, RegionTrait.UNDERDEVELOPED)
        ),
        ProjectProfile(
            id = "proj_digital", label = "数字政务平台",
            category = ProjectCategory.DIGITAL,
            description = "让群众在手机上就能办成事，做出成绩也容易被上级看见。",
            budget = 70, quarters = 4, minPositionIndex = 3,
            reward = ProjectReward(
                indicators = mapOf(Indicator.OFFICIAL_SATISFACTION to 6, Indicator.LIVELIHOOD to 3),
                merit = mapOf(MeritKind.INNOVATION to 8),
                record = "牵头建设数字政务平台，办事效率明显提升。"
            ),
            traitBonus = setOf(RegionTrait.ECONOMIC_HUB, RegionTrait.BALANCED)
        ),
        ProjectProfile(
            id = "proj_tourism", label = "文旅线路开发",
            category = ProjectCategory.TOURISM,
            description = "把山水资源串成一条线，收入来得慢，但环境要求高。",
            budget = 75, quarters = 5,
            reward = ProjectReward(
                indicators = mapOf(Indicator.ECONOMY to 6, Indicator.ENVIRONMENT to 3, Indicator.APPROVAL to 3),
                merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.INNOVATION to 4),
                financeGain = 12,
                record = "打造本地文旅线路，带动服务业增收。"
            ),
            traitBonus = setOf(RegionTrait.TOURISM)
        ),
        ProjectProfile(
            id = "proj_old_town", label = "老旧小区改造",
            category = ProjectCategory.HOUSING,
            description = "外墙、管网、停车位一起改，事情琐碎，矛盾也多。",
            budget = 65, quarters = 4,
            reward = ProjectReward(
                indicators = mapOf(Indicator.LIVELIHOOD to 6, Indicator.INFRASTRUCTURE to 4, Indicator.APPROVAL to 3),
                merit = mapOf(MeritKind.LIVELIHOOD to 6),
                risk = 2,
                record = "完成老旧小区改造，群众居住条件改善。"
            )
        ),
        ProjectProfile(
            id = "proj_flood_control", label = "防洪排涝工程",
            category = ProjectCategory.INFRASTRUCTURE,
            description = "汛期之前把河道和泵站整一遍，平时看不见，关键时刻救命。",
            budget = 95, quarters = 5, minPositionIndex = 3,
            perQuarter = GovernanceEffect(indicators = mapOf(Indicator.SECURITY to 1)),
            reward = ProjectReward(
                indicators = mapOf(Indicator.SECURITY to 8, Indicator.INFRASTRUCTURE to 4),
                merit = mapOf(MeritKind.SECURITY to 8),
                record = "建成防洪排涝工程，汛期安全得到保障。"
            )
        ),
        ProjectProfile(
            id = "proj_agri_upgrade", label = "高标准农田",
            category = ProjectCategory.AGRICULTURE,
            description = "把小田并大田、修渠修路，让种地的人真正省力。",
            budget = 55, quarters = 3,
            reward = ProjectReward(
                indicators = mapOf(Indicator.LIVELIHOOD to 4, Indicator.ECONOMY to 3),
                merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.LIVELIHOOD to 4),
                record = "推进高标准农田建设，农业基础条件改善。"
            ),
            traitBonus = setOf(RegionTrait.AGRICULTURAL)
        ),
        ProjectProfile(
            id = "proj_market", label = "农贸市场升级",
            category = ProjectCategory.PUBLIC_SERVICE,
            description = "把脏乱差的老市场翻新，是小事，但天天有人用。",
            budget = 40, quarters = 2,
            reward = ProjectReward(
                indicators = mapOf(Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 4, Indicator.ENVIRONMENT to 2),
                merit = mapOf(MeritKind.LIVELIHOOD to 4),
                record = "完成农贸市场升级改造。"
            )
        ),
        ProjectProfile(
            id = "proj_emergency", label = "应急指挥中心",
            category = ProjectCategory.DIGITAL,
            description = "把气象、水利、消防的数据接到一个屏幕上，出事时能快一步。",
            budget = 50, quarters = 3, minPositionIndex = 3,
            reward = ProjectReward(
                indicators = mapOf(Indicator.SECURITY to 6, Indicator.OFFICIAL_SATISFACTION to 3),
                merit = mapOf(MeritKind.SECURITY to 5, MeritKind.INNOVATION to 3),
                record = "建成应急指挥中心，突发事件处置效率提升。"
            )
        ),
        ProjectProfile(
            id = "proj_green_belt", label = "环城绿化带",
            category = ProjectCategory.ENVIRONMENT,
            description = "沿河沿路种树，投入不大，但能让群众看见变化。",
            budget = 35, quarters = 2,
            reward = ProjectReward(
                indicators = mapOf(Indicator.ENVIRONMENT to 5, Indicator.APPROVAL to 3),
                merit = mapOf(MeritKind.INNOVATION to 3, MeritKind.LIVELIHOOD to 2),
                record = "建成环城绿化带。"
            )
        )
    )

    private val byId: Map<String, ProjectProfile> = projects.associateBy { it.id }

    fun find(id: String?): ProjectProfile? = id?.let { byId[it] }

    /** 按地区特色给出有加成的项目。 */
    fun forTrait(trait: RegionTrait): List<ProjectProfile> =
        projects.filter { trait in it.traitBonus }

    /** 当前职位可以启动的项目。 */
    fun availableFor(positionIndex: Int, trait: RegionTrait): List<ProjectProfile> =
        projects.filter { it.minPositionIndex <= positionIndex }
            .sortedByDescending { trait in it.traitBonus }
}
