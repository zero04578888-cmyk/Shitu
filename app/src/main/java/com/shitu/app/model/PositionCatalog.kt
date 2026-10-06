package com.shitu.app.model

/**
 * 职位的完整显示名：「职级·具体职位」。
 *
 * 例如 rankName = "正厅级"、positionName = "市长" -> "正厅级·市长"。
 * 职级名与具体职位名相同时只显示一次（例如「科员」），
 * 避免出现「科员·科员」这种重复。
 *
 * 不要把完整名称硬编码成一个字符串：职级与具体职位始终分开保存，
 * 显示时统一由这里拼接。
 */
fun positionDisplay(rankName: String, positionName: String): String {
    val rank = rankName.trim()
    val name = positionName.trim()
    return when {
        name.isEmpty() -> rank
        rank.isEmpty() || name == rank -> name
        else -> "$rank·$name"
    }
}

/**
 * 一个具体职位的完整档案。
 *
 * 职级（[rankName]）与具体职位（[positionName]）分开存放，
 * 城市、部门、权力、影响力、威望、权限域与条线都作为独立字段，
 * UI 只通过 [display] 取展示名称。
 */
data class PositionNode(
    val id: String,
    val rankIndex: Int,
    val rankName: String,
    val positionName: String,
    val cityId: String = "",
    val cityName: String = "",
    val department: String = "",
    val powerLevel: Int = 0,
    /** 影响力 0～100（政治 / 经济 / 组织 / 公众影响力的综合）。 */
    val influence: Int = 0,
    /** 威望 0～100。 */
    val prestige: Int = 0,
    val scope: PositionScope = PositionScope.BASE,
    /** 条线类型，例如「组织人事」「经济发展」。 */
    val routeType: String = ""
) {
    /** 展示名称：「职级·具体职位」。 */
    val display: String get() = positionDisplay(rankName, positionName)

    val isDefault: Boolean get() = id.startsWith("default_")
}

/**
 * 职位数据层：由 [CityData] + [CityPositionRole] + [POSITION_LADDER] 共同推导，
 * 而不是靠字符串判断城市等级。
 *
 * - 城市主官职位（市委书记 / 市长 / 副市长等）由城市行政层级推出对应职级；
 * - 通用职位沿用晋升职位池，保证 [PromotionEngine] 的判定不变；
 * - 任何情况下都能给出一个默认合法职位，杜绝「晋升时没有具体职位」。
 */
object PositionCatalog {

    /** 职级名称，例如 8 -> 「正厅级」。 */
    fun rankNameOf(rankIndex: Int): String = positionAt(rankIndex).name

    /**
     * 某座城市在指定职级上的领导职位。
     *
     * 上海市 rank 11 -> 市委书记（副国级）、rank 10 -> 市长（正部级）；
     * 深圳市 rank 9 -> 市委书记 / 市长（副部级）；
     * 普通地级市 rank 8 -> 市委书记 / 市长（正厅级）。
     */
    fun cityNodes(city: CityData, rankIndex: Int): List<PositionNode> =
        CityDataRepository.positionSpecs(city)
            .filter { it.rankIndex == rankIndex }
            .map { spec -> nodeOf(city, spec) }

    /** 城市内某个具体职务对应的职位档案。 */
    fun cityNode(city: CityData, role: CityPositionRole): PositionNode =
        nodeOf(city, CityDataRepository.specFor(city, role))

    private fun nodeOf(city: CityData, spec: CityPositionSpec): PositionNode {
        val rankName = rankNameOf(spec.rankIndex)
        return PositionNode(
            id = "city_${city.id}_${spec.role.name.lowercase()}",
            rankIndex = spec.rankIndex,
            rankName = rankName,
            positionName = spec.role.label,
            cityId = city.id,
            cityName = city.name,
            department = city.name,
            powerLevel = spec.powerLevel,
            influence = maxOf(
                spec.politicalInfluence,
                spec.economicInfluence,
                spec.organizationalInfluence,
                spec.publicInfluence
            ).coerceIn(0, 100),
            prestige = spec.publicInfluence.coerceIn(0, 100),
            scope = positionAt(spec.rankIndex).scope,
            routeType = routeOf(spec.role)
        )
    }

    /** 城市职务对应的条线。 */
    fun routeOf(role: CityPositionRole): String = when (role) {
        CityPositionRole.PARTY_SECRETARY -> "组织人事"
        CityPositionRole.MAYOR -> "经济发展"
        CityPositionRole.DEPUTY_SECRETARY -> "组织人事"
        CityPositionRole.STANDING_COMMITTEE -> "综合行政"
        CityPositionRole.EXECUTIVE_VICE_MAYOR -> "财政系统"
        CityPositionRole.BUREAU_HEAD -> "部门管理"
    }

    /** 通用（非城市主官）职位：沿用晋升职位池的具体职位名与门槛。 */
    fun genericNodes(rankIndex: Int, unit: UnitType): List<PositionNode> =
        PromotionPositionCatalog.optionsFor(rankIndex).map { option ->
            PositionNode(
                id = option.id,
                rankIndex = rankIndex,
                rankName = rankNameOf(rankIndex),
                positionName = option.title,
                department = departmentOf(unit),
                powerLevel = positionAt(rankIndex).powerLevel,
                influence = (34 + rankIndex * 5).coerceIn(0, 100),
                prestige = (26 + rankIndex * 5).coerceIn(0, 100),
                scope = positionAt(rankIndex).scope
            )
        }

    /** 指定职级下所有可用具体职位：城市主官 + 通用职位，按职位名去重。 */
    fun nodesFor(rankIndex: Int, cityId: String?, unit: UnitType): List<PositionNode> {
        val city = CityDataRepository.findOrDefault(cityId)
        return (cityNodes(city, rankIndex) + genericNodes(rankIndex, unit))
            .distinctBy { it.positionName }
    }

    /** 默认合法职位：任何职级都能给出一个具体职位，保证晋升不会被卡住。 */
    fun defaultNode(rankIndex: Int, cityId: String?, unit: UnitType): PositionNode {
        val safeRank = rankIndex.coerceIn(0, POSITION_LADDER.size - 1)
        nodesFor(safeRank, cityId, unit).firstOrNull()?.let { return it }
        val level = positionAt(safeRank)
        return PositionNode(
            id = "default_$safeRank",
            rankIndex = safeRank,
            rankName = level.name,
            positionName = level.title.ifBlank { "${level.name}岗位" },
            department = departmentOf(unit),
            powerLevel = level.powerLevel,
            influence = (34 + safeRank * 5).coerceIn(0, 100),
            prestige = (26 + safeRank * 5).coerceIn(0, 100),
            scope = level.scope
        )
    }

    /**
     * 解析玩家的当前职位：
     * 优先用存档里的具体职位（[positionId] / [positionName]），
     * 否则按当前城市与职级生成默认职位。旧存档因此可以平滑升级。
     */
    fun resolveNode(
        rankIndex: Int,
        cityId: String?,
        unit: UnitType,
        positionId: String,
        positionName: String
    ): PositionNode {
        val safeRank = rankIndex.coerceIn(0, POSITION_LADDER.size - 1)
        val candidates = nodesFor(safeRank, cityId, unit)
        if (positionId.isNotBlank()) {
            candidates.firstOrNull { it.id == positionId }?.let { return it }
            // 城市变了但职位 id 仍在池中：按 id 前缀匹配条线职务。
            candidates.firstOrNull { it.id.endsWith(positionId) }?.let { return it }
        }
        if (positionName.isNotBlank()) {
            candidates.firstOrNull { it.positionName == positionName }?.let { return it }
            val level = positionAt(safeRank)
            return PositionNode(
                id = positionId.ifBlank { "resolved_${safeRank}_$positionName" },
                rankIndex = safeRank,
                rankName = level.name,
                positionName = positionName,
                cityId = CityDataRepository.findOrDefault(cityId).id,
                cityName = CityDataRepository.findOrDefault(cityId).name,
                department = departmentOf(unit),
                powerLevel = level.powerLevel,
                influence = (34 + safeRank * 5).coerceIn(0, 100),
                prestige = (26 + safeRank * 5).coerceIn(0, 100),
                scope = level.scope
            )
        }
        return defaultNode(safeRank, cityId, unit)
    }

    /**
     * 晋升时可供选择的具体职位：城市主官在前，通用职务与城市属性职务在后。
     * 数据由 [PositionRepository] 统一生成，保证同一职级至少 3 个可选职务。
     */
    fun promotionOptions(
        rankIndex: Int,
        cityId: String?,
        unit: UnitType,
        from: PromotionPosition? = null,
        seed: Long = 0L
    ): List<PromotionPosition> =
        PositionRepository.optionsFor(rankIndex, cityId, unit, from = from, seed = seed)
}