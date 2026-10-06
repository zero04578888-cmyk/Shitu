package com.shitu.app.model

/**
 * 一次晋升中可供选择的「具体职位」。
 *
 * 只要玩家满足该职级的全部硬条件，就一定能拿到任职资格；
 * 具体职位只是让玩家选择去向，任何一个选项都不依赖「是否有空位」。
 * 所有名称都是虚构的游戏职位，不对应现实任免。
 */
data class PromotionPosition(
    val id: String,
    val rankIndex: Int,
    val title: String,
    val description: String,
    val scope: PositionScope,
    /** 额外的软性门槛：数值越高、可选的具体职位越多，但不会卡住晋升。 */
    val minMerit: Int = 0,
    val minApproval: Int = 0,
    val minLeaderTrust: Int = 0,
    /** 城市主官职位所属城市；通用职位为空。 */
    val cityId: String = "",
    /** 所属部门类型，例如「党委」「政府」「经济」「组织人事」。 */
    val departmentType: String = "",
    /** 条线，用于事件池与后续晋升路线。 */
    val routeType: String = "",
    /** 管辖范围说明，例如「县直部门」「乡镇」。 */
    val jurisdiction: String = "",
    /** 权力等级，默认沿用职级阶梯。 */
    val powerLevel: Int = positionAt(rankIndex).powerLevel,
    /** 政治影响力 0～100。 */
    val politicalInfluence: Int = 0,
    /** 经济影响力 0～100。 */
    val economicInfluence: Int = 0,
    /** 组织影响力 0～100。 */
    val organizationalInfluence: Int = 0,
    /** 公众影响力 0～100。 */
    val publicInfluence: Int = 0,
    /** 该职务对口的突出方向，供界面客观展示；不做任何推荐排序。 */
    val focus: String = "",
    /** 对口事件标签：任职后相关事件出现概率提高。 */
    val eventTags: Set<EventTag> = emptySet(),
    /** 后续可晋升方向，用于路线记录。 */
    val nextRoutes: List<String> = emptyList(),
    /** 第十九阶段：所属仕途路线。 */
    val route: PositionRoute = PositionRoute.GENERAL_GOVERNMENT,
    /** 履历价值：重点岗位与重要城市岗位更高，只影响机会权重，不是硬门槛。 */
    val careerValue: Int = 0,
    /** 可由哪些职位转入本职位（职位 id）。 */
    val previousPositions: List<String> = emptyList(),
    /** 后续更容易出现的职位 id；只是倾向，不锁定路线。 */
    val nextPositions: List<String> = emptyList(),
    /** 与本职位衔接顺畅的路线。 */
    val requiredRoutes: List<PositionRoute> = emptyList()
) {
    val isDefault: Boolean get() = id.startsWith("default_")

    /** 路线名称，用于「主要方向」展示。 */
    val routeLabel: String get() = route.label

    /** 主要方向说明：路线重点 + 本职位职责侧重。 */
    val directionText: String get() = route.label + " · " + route.focus

    /** 履历价值档位，仅作展示。 */
    val careerValueLabel: String
        get() = when {
            careerValue >= 24 -> "极高"
            careerValue >= 16 -> "很高"
            careerValue >= 9 -> "较高"
            careerValue >= 4 -> "中等"
            else -> "一般"
        }

    /** 该职务的四个影响力维度，按固定顺序展示。 */
    val influenceSummary: List<Pair<String, Int>>
        get() = listOf(
            "政治" to politicalInfluence,
            "经济" to economicInfluence,
            "组织" to organizationalInfluence,
            "公众" to publicInfluence
        ).filter { it.second > 0 }

    /** 所属职级名称，例如「正厅级」。 */
    val rankName: String get() = positionAt(rankIndex).name

    /** 具体职位名称，例如「市长」。 */
    val positionName: String get() = title

    /** 完整显示名：「职级·具体职位」，例如「正厅级·市长」。 */
    val displayTitle: String get() = positionDisplay(rankName, title)

    fun matches(merit: Int, approval: Int, leaderTrust: Int): Boolean =
        merit >= minMerit && approval >= minApproval && leaderTrust >= minLeaderTrust

    /** 门槛说明，用于界面展示；没有门槛时返回空串。 */
    val gateText: String
        get() = buildList {
            if (minMerit > 0) add("政绩 $minMerit")
            if (minApproval > 0) add("民望 $minApproval")
            if (minLeaderTrust > 0) add("领导信任 $minLeaderTrust")
        }.joinToString(" · ")
}

/**
 * 各职级对应的具体职位池。
 *
 * 设计原则：低职级一个岗位；处级以上给出多个方向，让玩家选择去向。
 * 这里只是「可选项」，即使一条都没有匹配，也会由 [defaultFor] 兜底，
 * 保证满足条件的玩家一定可以完成晋升。
 */
object PromotionPositionCatalog {

    /**
     * 某个职级的通用具体职务池（不含城市主官职务）。
     *
     * 实际生成在 [PositionRepository]：同一个职级至少给出 3 个、
     * 最多 6 个具体职务，并按城市类型追加对口职务。
     */
    fun optionsFor(rankIndex: Int): List<PromotionPosition> =
        PositionRepository.genericOptions(rankIndex, UnitType.GENERAL)


    /**
     * 默认合法职位：只要该职级存在，就一定返回一个可用职位。
     * 用于随机职位数据为空、或所有具体职位都不满足额外门槛时的兜底。
     */
    fun defaultFor(rankIndex: Int): PromotionPosition {
        val level = positionAt(rankIndex)
        val title = level.title.ifBlank { "${level.name}岗位" }
        return PromotionPosition(
            id = "default_$rankIndex",
            rankIndex = rankIndex,
            title = title,
            description = "由组织按程序安排的具体职位，条件达标即可任职。",
            scope = level.scope
        )
    }
}