package com.shitu.app.model

/**
 * 第十九阶段：仕途路线标签。
 *
 * 职级决定高度，具体职务与路线决定风格：
 * 同一个职级下，不同路线会给出不同的后续职位池，最终形成各不相同的仕途路线。
 *
 * 全部为虚构的游戏路线，不对应现实任免体系。
 */
enum class PositionRoute(val label: String, val focus: String) {
    LOCAL_GOVERNANCE("地方治理", "基层治理 / 群众工作 / 一方发展"),
    ECONOMY("经济管理", "财政 / 产业 / 项目 / 招商"),
    ORGANIZATION("组织人事", "干部工作 / 组织建设 / 班子管理"),
    POLICY("政策综合", "综合协调 / 政策研究 / 材料督办"),
    CULTURE("宣传文旅", "宣传 / 文化 / 旅游 / 城市形象"),
    SCIENCE_TECH("科技产业", "科技 / 创新 / 产业升级"),
    GENERAL_GOVERNMENT("综合行政", "行政运行 / 部门管理 / 统筹执行");

    companion object {
        fun byName(value: String?): PositionRoute? = entries.firstOrNull { it.name == value }

        /** 由旧版条线文本推断路线，保证旧存档与旧数据可用。 */
        fun fromLegacyRouteType(routeType: String?): PositionRoute {
            val text = routeType.orEmpty()
            return when {
                text.contains("基层") -> LOCAL_GOVERNANCE
                text.contains("组织") -> ORGANIZATION
                text.contains("财政") || text.contains("经济") -> ECONOMY
                text.contains("宣传") || text.contains("文化") -> CULTURE
                text.contains("科技") -> SCIENCE_TECH
                text.contains("政策") || text.contains("材料") -> POLICY
                else -> GENERAL_GOVERNMENT
            }
        }
    }
}