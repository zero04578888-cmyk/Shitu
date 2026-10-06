package com.shitu.app.model

/**
 * 第十二阶段：岗位系统（工作条线）。
 *
 * 玩家所在的条线决定：更容易遇到哪类任务、哪些治理指标更容易做出成绩、
 * 以及这条线本身的风险高低。条线是推导出来的，不单独存档，
 * 因此旧存档不会因为新增条线而失效。
 */
enum class JobTrack(
    val label: String,
    val description: String,
    /** 对口事件标签，权重更高。 */
    val tags: Set<EventTag>,
    /** 该条线更容易推动的治理指标。 */
    val focusIndicators: Set<Indicator>,
    /** 条线自带的风险倾向，会加到廉政与官场风险的压力上。 */
    val riskBias: Int
) {
    OFFICE(
        "办公室系统",
        "写材料、协调会务、盯着领导关注的事，离核心近，也最容易熬人。",
        setOf(EventTag.MATERIAL, EventTag.MEETING, EventTag.LEADER_TASK),
        setOf(Indicator.OFFICIAL_SATISFACTION),
        2
    ),
    ORGANIZATION(
        "组织人事",
        "管干部、管考核、管任免，信息最多，人情也最难还。",
        setOf(EventTag.CAREER, EventTag.ASSESSMENT, EventTag.MEETING),
        setOf(Indicator.OFFICIAL_SATISFACTION),
        6
    ),
    PROPAGANDA(
        "宣传系统",
        "管舆情、管口径、管形象，一句话说错就可能被放大。",
        setOf(EventTag.PUBLIC_OPINION, EventTag.MATERIAL, EventTag.CAREER),
        setOf(Indicator.APPROVAL),
        5
    ),
    ECONOMY(
        "经济发展",
        "抓项目、抓招商、抓产业，成绩看得见，压力也很实在。",
        setOf(EventTag.PROJECT, EventTag.RESEARCH, EventTag.LEADER_TASK),
        setOf(Indicator.ECONOMY, Indicator.INFRASTRUCTURE),
        4
    ),
    FINANCE(
        "财政系统",
        "管钱袋子，谁都要找你，但每一笔都要说得清楚。",
        setOf(EventTag.PROJECT, EventTag.INSPECTION, EventTag.COORDINATION),
        setOf(Indicator.ECONOMY),
        5
    ),
    AGRICULTURE(
        "农业系统",
        "跑田间地头，事情琐碎，但群众基础最扎实。",
        setOf(EventTag.RURAL, EventTag.MASS_APPEAL, EventTag.DISASTER),
        setOf(Indicator.LIVELIHOOD, Indicator.ENVIRONMENT),
        2
    ),
    HOUSING(
        "住建系统",
        "管工程、管房子、管拆迁，最容易出成绩，也最容易出事。",
        setOf(EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.INTEGRITY),
        setOf(Indicator.INFRASTRUCTURE),
        9
    ),
    EDUCATION(
        "教育系统",
        "管学校、管老师、管招生，短期不出彩，长期见口碑。",
        setOf(EventTag.RESEARCH, EventTag.COMPLAINT, EventTag.FAMILY),
        setOf(Indicator.EDUCATION),
        3
    ),
    HEALTH(
        "卫生系统",
        "管医院、管防疫、管应急救治，突发情况永远不打招呼。",
        setOf(EventTag.EMERGENCY, EventTag.COMPLAINT, EventTag.HEALTH),
        setOf(Indicator.MEDICAL, Indicator.SECURITY),
        3
    ),
    EMERGENCY(
        "应急管理",
        "盯安全、盯灾害、盯事故，平时没人想得起，出事第一个到。",
        setOf(EventTag.EMERGENCY, EventTag.DISASTER, EventTag.ACCIDENT, EventTag.FLOOD),
        setOf(Indicator.SECURITY),
        4
    ),
    GRASSROOTS(
        "基层治理",
        "直面群众，纠纷、诉求、琐事一肩挑，最能练人。",
        setOf(EventTag.MASS_APPEAL, EventTag.PETITION, EventTag.COMPLAINT, EventTag.RURAL),
        setOf(Indicator.LIVELIHOOD, Indicator.APPROVAL),
        3
    ),
    GENERAL_ADMIN(
        "综合行政",
        "承上启下，什么都要管一点，什么都不专。",
        setOf(EventTag.COORDINATION, EventTag.MEETING, EventTag.INSPECTION),
        setOf(Indicator.OFFICIAL_SATISFACTION, Indicator.APPROVAL),
        4
    );

    companion object {
        fun byName(value: String?): JobTrack? = entries.firstOrNull { it.name == value }

        /** 由单位平台与职业路线推导出当前条线。 */
        fun of(unit: UnitType, route: CareerRoute?): JobTrack = when (route) {
            CareerRoute.ORGANIZATION_ROUTE -> ORGANIZATION
            CareerRoute.ECONOMY_ROUTE -> ECONOMY
            CareerRoute.REFORM_ROUTE ->
                if (unit == UnitType.TOWN || unit == UnitType.STREET) HOUSING else PROPAGANDA
            CareerRoute.PROFESSIONAL -> when (unit) {
                UnitType.BUSINESS -> HEALTH
                UnitType.COUNTY -> FINANCE
                UnitType.CITY -> EDUCATION
                else -> AGRICULTURE
            }
            CareerRoute.WRITING -> OFFICE
            CareerRoute.MANAGEMENT -> GENERAL_ADMIN
            CareerRoute.MASS_WORK ->
                if (unit == UnitType.BUSINESS) EMERGENCY else GRASSROOTS
            null -> when (unit) {
                UnitType.TOWN, UnitType.STREET -> GRASSROOTS
                UnitType.COUNTY, UnitType.GENERAL -> GENERAL_ADMIN
                UnitType.CITY -> OFFICE
                UnitType.BUSINESS -> EMERGENCY
            }
        }
    }
}
