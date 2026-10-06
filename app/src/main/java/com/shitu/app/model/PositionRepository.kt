package com.shitu.app.model

/**
 * 第十九阶段：具体职务与仕途路线数据层。
 *
 * 职级（rank）只表示干部层级；玩家具体做什么，由本仓库按
 * 「城市行政层级 + 任职单位 + 履历路线 + 城市属性」动态生成。
 *
 * 设计规则：
 * - 每个有效职级至少给出 [MIN_OPTIONS] 个具体职务，最多 [MAX_OPTIONS] 个；
 * - 前三项不设任何软性门槛，且职位池为空时会兜底到默认职务，
 *   因此「满足晋升条件却无职位可选」在数据层就不可能发生；
 * - 同一职级内的职务拥有不同的部门、路线、影响力、履历价值与对口事件；
 *   当前职位还会通过 [PromotionPosition.nextPositions] 影响下一次职位池，
 *   让玩家自己走出一条仕途路线，而不是所有人一条直线。
 *
 * 全部为虚构游戏职位，不对应现实任免。
 */
object PositionRepository {

    /** 同一职级至少给出的具体职务数量。 */
    const val MIN_OPTIONS = 3

    /** 同一职级最多给出的具体职务数量。 */
    const val MAX_OPTIONS = 6

    /** 博士专属起点：中央选调生（副处级）。不进入普通晋升职位池，只作为起点身份。 */
    const val CENTRAL_SELECTED_ID = "pos_central_selected"

    /** 中央选调生的职务名。 */
    const val CENTRAL_SELECTED_NAME = "中央选调生"

    /** 中央选调生的起始职级下标（副处级）。 */
    val CENTRAL_SELECTED_RANK_INDEX: Int =
        POSITION_LADDER.indexOfFirst { it.name == "副处级" }.takeIf { it >= 0 } ?: 5

    /**
     * 中央选调生档案：博士学历的固定起点。
     *
     * 它有真实的部门、条线、影响力与对口事件标签，
     * 因此初始属性、事件池、履历与权力计算都会真正生效，
     * 而不是只改一个显示名称。晋升到正处级后即回到普通职位池。
     */
    val centralSelected: PromotionPosition by lazy {
        val level = positionAt(CENTRAL_SELECTED_RANK_INDEX)
        PromotionPosition(
            id = CENTRAL_SELECTED_ID,
            rankIndex = CENTRAL_SELECTED_RANK_INDEX,
            title = CENTRAL_SELECTED_NAME,
            description = "面向重点院校博士的政策研究与综合协调岗位，起点为副处级。",
            scope = level.scope,
            departmentType = "中央机关",
            routeType = PositionRoute.POLICY.label,
            jurisdiction = "中央机关",
            powerLevel = level.powerLevel,
            politicalInfluence = 66,
            economicInfluence = 52,
            organizationalInfluence = 62,
            publicInfluence = 46,
            focus = "政策研究、综合协调与文稿",
            eventTags = setOf(EventTag.RESEARCH, EventTag.MATERIAL, EventTag.COORDINATION),
            nextRoutes = listOf(PositionRoute.ORGANIZATION.label, PositionRoute.GENERAL_GOVERNMENT.label),
            route = PositionRoute.POLICY,
            careerValue = 18
        )
    }

    /** 职务模板：只描述数据，不参与判定。 */
    private data class Blueprint(
        val id: String,
        val title: String,
        val description: String,
        val departmentType: String,
        val routeType: String,
        val jurisdiction: String,
        val focus: String,
        val political: Int,
        val economic: Int,
        val organizational: Int,
        val public: Int,
        val tags: Set<EventTag>,
        val nextRoutes: List<String>,
        val minMerit: Int = 0,
        val minApproval: Int = 0,
        val minLeaderTrust: Int = 0,
        /** 所属仕途路线；为空时由 [routeType] 推断。 */
        val route: PositionRoute? = null,
        /** 履历价值；0 表示按职级与权力自动推算。 */
        val careerValue: Int = 0,
        /** 后续更容易出现的具体职位 id；只是倾向，不锁定路线。 */
        val nextPositions: List<String> = emptyList()
    ) {
        fun toOption(rankIndex: Int): PromotionPosition {
            val level = positionAt(rankIndex)
            return PromotionPosition(
                id = id,
                rankIndex = rankIndex,
                title = title,
                description = description,
                scope = level.scope,
                minMerit = minMerit,
                minApproval = minApproval,
                minLeaderTrust = minLeaderTrust,
                departmentType = departmentType,
                routeType = routeType,
                jurisdiction = jurisdiction,
                powerLevel = level.powerLevel,
                politicalInfluence = political.coerceIn(0, 100),
                economicInfluence = economic.coerceIn(0, 100),
                organizationalInfluence = organizational.coerceIn(0, 100),
                publicInfluence = public.coerceIn(0, 100),
                focus = focus,
                eventTags = tags,
                nextRoutes = nextRoutes,
                route = route ?: PositionRoute.fromLegacyRouteType(routeType),
                careerValue = if (careerValue > 0) {
                    careerValue
                } else {
                    (level.powerLevel / 2 + rankIndex - 2).coerceIn(2, 40)
                },
                previousPositions = previousOf(id),
                nextPositions = nextPositions
            )
        }
    }

    /**
     * 某个职级的通用职务池（不含城市主官职务）。
     *
     * 每个职级都至少 [MIN_OPTIONS] 项，且前三项不设任何软性门槛，
     * 保证玩家一旦满足晋升硬条件就一定拿得到多个具体职务可选。
     */
    fun genericOptions(
        rankIndex: Int,
        unit: UnitType = UnitType.GENERAL,
        routeType: String = ""
    ): List<PromotionPosition> {
        val rank = rankIndex.coerceIn(0, POSITION_LADDER.size - 1)
        if (rank < FIRST_GRADE_INDEX) return emptyList()
        val blueprints = blueprintsOf(rank)
        if (blueprints.isEmpty()) return emptyList()
        // 与玩家当前条线一致的职务排在同级前面，但界面不做任何推荐标注。
        val ordered = if (routeType.isBlank()) {
            blueprints
        } else {
            blueprints.sortedByDescending { if (it.routeType == routeType) 1 else 0 }
        }
        return ordered.take(MAX_OPTIONS).map { it.toOption(rank) }
    }

    /**
     * 某个职级在指定城市下的完整职务池：
     * 城市主官职务（真实城市层级推导）+ 城市属性对口职务 + 通用职务。
     *
     * [from] 是玩家当前的具体职务：其 [PromotionPosition.nextPositions] 与路线
     * 会提高对口职位的排序权重，实现「当前职位影响下一次职位池」。
     * [seed] 只用于稳定的同分排序，保证同一存档每次打开顺序一致。
     *
     * 无论排序结果如何，返回列表至少包含 [MIN_OPTIONS] 项，
     * 因此满足条件的玩家不可能没有职位可选。
     */
    fun optionsFor(
        rankIndex: Int,
        cityId: String?,
        unit: UnitType,
        routeType: String = "",
        from: PromotionPosition? = null,
        seed: Long = 0L
    ): List<PromotionPosition> {
        val rank = rankIndex.coerceIn(0, POSITION_LADDER.size - 1)
        if (rank < FIRST_GRADE_INDEX) return emptyList()
        val city = CityDataRepository.findOrDefault(cityId)
        val pool = (cityOptionsOf(city, rank) + traitOptionsOf(city, rank, unit) + genericOptions(rank, unit, routeType))
            .distinctBy { it.title }
        if (pool.isEmpty()) return emptyList()
        val ranked = pool.sortedByDescending { affinityOf(it, from, city, routeType, seed) }
        val selected = ranked.take(MAX_OPTIONS).toMutableList()
        // 保证至少 MIN_OPTIONS 项：不足时用不设门槛的职位补齐。
        if (selected.size < MIN_OPTIONS) {
            pool.filter { it.matches(0, 0, 0) }.forEach { candidate ->
                if (selected.size < MIN_OPTIONS && selected.none { it.id == candidate.id }) {
                    selected.add(candidate)
                }
            }
        }
        return selected
    }

    /**
     * 精确匹配玩家已任命的职务：先按 id，再按名称。
     * 只有真正命中池内职务时才返回；旧存档只有职级、或名称无法识别时返回 null。
     * 需要「兜底出一个可显示职务」时用 [resolve]。
     */
    fun match(
        rankIndex: Int,
        cityId: String?,
        unit: UnitType,
        positionId: String?,
        positionName: String?
    ): PromotionPosition? {
        val rank = rankIndex.coerceIn(0, POSITION_LADDER.size - 1)
        if (rank < FIRST_GRADE_INDEX) return null
        // 博士专属起点：中央选调生不属于普通晋升职位池，单独精确匹配。
        val trimmedId = positionId?.trim().orEmpty()
        val trimmedName = positionName?.trim().orEmpty()
        if (rank == CENTRAL_SELECTED_RANK_INDEX &&
            (trimmedId == CENTRAL_SELECTED_ID || trimmedName == CENTRAL_SELECTED_NAME)
        ) {
            return centralSelected
        }
        val pool = optionsFor(rank, cityId, unit)
        pool.firstOrNull { it.id == positionId?.takeIf { id -> id.isNotBlank() } }?.let { return it }
        return pool.firstOrNull { it.title == positionName?.trim() && it.title.isNotBlank() }
    }

    /**
     * 解析玩家当前任职的具体职务：
     * 先精确匹配，匹配不到时按职级在该城市的默认职务兜底。
     * 旧存档没有 positionId 时也能拿到一个合理结果，不会崩溃。
     */
    fun resolve(
        rankIndex: Int,
        cityId: String?,
        unit: UnitType,
        positionId: String?,
        positionName: String?
    ): PromotionPosition? {
        val rank = rankIndex.coerceIn(0, POSITION_LADDER.size - 1)
        if (rank < FIRST_GRADE_INDEX) return null
        return match(rankIndex, cityId, unit, positionId, positionName)
            ?: optionsFor(rank, cityId, unit).firstOrNull()
    }

    /** 按职位 id 取回具体职务模板；找不到返回 null。 */
    fun optionById(positionId: String?): PromotionPosition? {
        val id = positionId?.takeIf { it.isNotBlank() } ?: return null
        if (id == CENTRAL_SELECTED_ID) return centralSelected
        for (rank in FIRST_GRADE_INDEX until POSITION_LADDER.size) {
            blueprintsOf(rank).firstOrNull { it.id == id }?.let { return it.toOption(rank) }
        }
        return null
    }

    /** 职位 id 所属的仕途路线；找不到返回 null。 */
    fun routeOf(positionId: String?): PositionRoute? = optionById(positionId)?.route

    /** 职位 id 的履历价值；找不到返回 0。 */
    fun careerValueOf(positionId: String?): Int = optionById(positionId)?.careerValue ?: 0

    /** 职位 id 对应的职级下标；找不到返回 -1。 */
    fun rankOfId(positionId: String?): Int {
        val id = positionId?.takeIf { it.isNotBlank() } ?: return -1
        if (id == CENTRAL_SELECTED_ID) return CENTRAL_SELECTED_RANK_INDEX
        for (rank in FIRST_GRADE_INDEX until POSITION_LADDER.size) {
            if (blueprintsOf(rank).any { it.id == id }) return rank
        }
        return -1
    }

    /** 反向推导：哪些职位把 [id] 列为后续方向。 */
    private fun previousOf(id: String): List<String> {
        if (id.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        for (rank in FIRST_GRADE_INDEX until POSITION_LADDER.size) {
            blueprintsOf(rank).forEach { blueprint ->
                if (blueprint.nextPositions.contains(id)) result.add(blueprint.id)
            }
        }
        return result.distinct()
    }

    /**
     * 职位与玩家当前状态的匹配权重：
     * - 当前职位的后续方向（nextPositions）最优先；
     * - 同路线、当前条线与城市属性顺路依次加分；
     * - [seed] 只用于稳定同分，保证顺序可复现。
     *
     * 权重只影响展示顺序，不构成任何晋升门槛。
     */
    private fun affinityOf(
        option: PromotionPosition,
        from: PromotionPosition?,
        city: CityData,
        routeType: String,
        seed: Long
    ): Int {
        var score = option.careerValue / 2
        if (from != null) {
            if (from.nextPositions.contains(option.id)) score += 70
            if (option.previousPositions.contains(from.id)) score += 40
            if (from.route == option.route) score += 26
            if (option.requiredRoutes.contains(from.route)) score += 12
        }
        if (routeType.isNotBlank() && option.routeType == routeType) score += 10
        if (city.id.isNotBlank() && option.cityId == city.id) score += 18
        score += when {
            city.trait == RegionTrait.ECONOMIC_HUB && option.route == PositionRoute.ECONOMY -> 14
            city.trait == RegionTrait.INDUSTRIAL && option.route == PositionRoute.SCIENCE_TECH -> 14
            city.trait == RegionTrait.AGRICULTURAL && option.route == PositionRoute.LOCAL_GOVERNANCE -> 12
            city.trait == RegionTrait.TOURISM && option.route == PositionRoute.CULTURE -> 14
            city.eventTags.contains("technology") && option.route == PositionRoute.SCIENCE_TECH -> 14
            city.eventTags.contains("finance") && option.route == PositionRoute.ECONOMY -> 12
            else -> 0
        }
        val jitter = ((seed + option.id.hashCode().toLong()) % 7L + 7L) % 7L
        return score + jitter.toInt()
    }

    /** 城市主官职务：由 [CityDataRepository] 的行政层级规则推导，名称带真实城市。 */
    private fun cityOptionsOf(city: CityData, rankIndex: Int): List<PromotionPosition> =
        CityDataRepository.positionSpecs(city)
            .filter { it.rankIndex == rankIndex }
            .map { spec ->
                val node = PositionCatalog.cityNode(city, spec.role)
                PromotionPosition(
                    id = node.id,
                    rankIndex = rankIndex,
                    title = spec.role.label,
                    description = "${city.name} · ${spec.role.summary}",
                    scope = positionAt(rankIndex).scope,
                    cityId = city.id,
                    departmentType = city.name,
                    routeType = PositionCatalog.routeOf(spec.role),
                    jurisdiction = "${city.province} · ${city.name}",
                    powerLevel = spec.powerLevel,
                    politicalInfluence = spec.politicalInfluence,
                    economicInfluence = spec.economicInfluence,
                    organizationalInfluence = spec.organizationalInfluence,
                    publicInfluence = spec.publicInfluence,
                    focus = focusOf(spec.role),
                    eventTags = tagsOf(spec.role),
                    nextRoutes = nextRoutesOf(spec.role),
                    route = routeOfRole(spec.role),
                    careerValue = cityCareerValue(city, spec.powerLevel)
                )
            }

    /** 城市职位的路线标签。 */
    private fun routeOfRole(role: CityPositionRole): PositionRoute = when (role) {
        CityPositionRole.PARTY_SECRETARY -> PositionRoute.ORGANIZATION
        CityPositionRole.MAYOR -> PositionRoute.GENERAL_GOVERNMENT
        CityPositionRole.DEPUTY_SECRETARY -> PositionRoute.ORGANIZATION
        CityPositionRole.STANDING_COMMITTEE -> PositionRoute.GENERAL_GOVERNMENT
        CityPositionRole.EXECUTIVE_VICE_MAYOR -> PositionRoute.ECONOMY
        CityPositionRole.BUREAU_HEAD -> PositionRoute.POLICY
    }

    /** 城市职位的履历价值：城市含权量与职位权力共同决定。 */
    private fun cityCareerValue(city: CityData, powerLevel: Int): Int =
        (city.powerWeight / 6 + powerLevel / 2).coerceIn(4, 40)

    /** 城市属性对口职务：经济城市偏招商财政、旅游城市偏文旅、科教城市偏科技。 */
    private fun traitOptionsOf(city: CityData, rankIndex: Int, unit: UnitType): List<PromotionPosition> {
        if (rankIndex !in 4..6) return emptyList()
        val spec = when {
            city.trait == RegionTrait.ECONOMIC_HUB ->
                TraitBlueprint("trait_finance", "财政金融部门负责人", "财政系统", PositionRoute.ECONOMY, "预算、资金与金融协调", setOf(EventTag.PROJECT, EventTag.INSPECTION))
            city.trait == RegionTrait.INDUSTRIAL ->
                TraitBlueprint("trait_industry", "工业和信息化部门负责人", "经济发展", PositionRoute.SCIENCE_TECH, "产业、工业与技术改造", setOf(EventTag.PROJECT, EventTag.RESEARCH))
            city.trait == RegionTrait.AGRICULTURAL ->
                TraitBlueprint("trait_rural", "农业农村部门负责人", "经济发展", PositionRoute.LOCAL_GOVERNANCE, "农业、农村与乡村振兴", setOf(EventTag.RURAL, EventTag.MASS_APPEAL))
            city.trait == RegionTrait.TOURISM ->
                TraitBlueprint("trait_culture", "文化旅游部门负责人", "宣传文化", PositionRoute.CULTURE, "文旅、宣传与城市形象", setOf(EventTag.PUBLIC_OPINION, EventTag.PROJECT))
            city.eventTags.contains("technology") ->
                TraitBlueprint("trait_tech", "科技创新部门负责人", "经济发展", PositionRoute.SCIENCE_TECH, "科技、创新与产业升级", setOf(EventTag.RESEARCH, EventTag.PROJECT))
            city.eventTags.contains("finance") ->
                TraitBlueprint("trait_trade", "商务贸易部门负责人", "经济发展", PositionRoute.ECONOMY, "商贸、外资与开放合作", setOf(EventTag.PROJECT, EventTag.COORDINATION))
            else -> return emptyList()
        }
        return listOf(
            PromotionPosition(
                id = spec.id + "_$rankIndex",
                rankIndex = rankIndex,
                title = spec.title,
                description = "${city.name} · ${city.trait.label}属性对口职务。",
                scope = positionAt(rankIndex).scope,
                cityId = city.id,
                departmentType = city.name + "部门",
                routeType = spec.routeType,
                jurisdiction = "${city.province} · ${city.name}",
                powerLevel = positionAt(rankIndex).powerLevel,
                politicalInfluence = 42,
                economicInfluence = 62,
                organizationalInfluence = 48,
                publicInfluence = 46,
                focus = spec.focus,
                eventTags = spec.tags,
                nextRoutes = listOf(spec.routeType, "综合行政"),
                route = spec.route,
                careerValue = (rankIndex - 1) * 3
            )
        )
    }

    private data class TraitBlueprint(
        val id: String,
        val title: String,
        val routeType: String,
        val route: PositionRoute,
        val focus: String,
        val tags: Set<EventTag>
    )

    private fun focusOf(role: CityPositionRole): String = when (role) {
        CityPositionRole.PARTY_SECRETARY -> "全面领导、组织人事与重大决策"
        CityPositionRole.MAYOR -> "经济发展、财政与城市治理"
        CityPositionRole.DEPUTY_SECRETARY -> "统筹协调、党务与干部工作"
        CityPositionRole.STANDING_COMMITTEE -> "重大决策、分工负责与条线管理"
        CityPositionRole.EXECUTIVE_VICE_MAYOR -> "财政、项目与要素调度"
        CityPositionRole.BUREAU_HEAD -> "行业管理、专项工作与执行落实"
    }

    private fun tagsOf(role: CityPositionRole): Set<EventTag> = when (role) {
        CityPositionRole.PARTY_SECRETARY -> setOf(EventTag.LEADER_TALK, EventTag.CAREER, EventTag.COORDINATION)
        CityPositionRole.MAYOR -> setOf(EventTag.PROJECT, EventTag.PUBLIC_OPINION, EventTag.LEADER_TASK)
        CityPositionRole.DEPUTY_SECRETARY -> setOf(EventTag.COORDINATION, EventTag.MEETING, EventTag.CAREER)
        CityPositionRole.STANDING_COMMITTEE -> setOf(EventTag.MEETING, EventTag.LEADER_TALK, EventTag.INSPECTION)
        CityPositionRole.EXECUTIVE_VICE_MAYOR -> setOf(EventTag.PROJECT, EventTag.INSPECTION, EventTag.ACCEPTANCE)
        CityPositionRole.BUREAU_HEAD -> setOf(EventTag.PROJECT, EventTag.MATERIAL, EventTag.ACCEPTANCE)
    }

    private fun nextRoutesOf(role: CityPositionRole): List<String> = when (role) {
        CityPositionRole.PARTY_SECRETARY -> listOf("组织人事", "综合行政")
        CityPositionRole.MAYOR -> listOf("经济发展", "财政系统")
        CityPositionRole.DEPUTY_SECRETARY -> listOf("组织人事")
        CityPositionRole.STANDING_COMMITTEE -> listOf("综合行政", "政法系统")
        CityPositionRole.EXECUTIVE_VICE_MAYOR -> listOf("财政系统", "经济发展")
        CityPositionRole.BUREAU_HEAD -> listOf("部门管理")
    }

    /**
     * 各职级的通用职务模板。
     *
     * 前三项不设门槛；第四项以后为条件更好的方向，只有数值更高时才出现，
     * 但绝不会因此卡住晋升。
     */
    private fun blueprintsOf(rankIndex: Int): List<Blueprint> = when (rankIndex) {
        3 -> listOf(
            Blueprint(
                "pos_3_town_deputy_secretary", "乡镇党委副书记", "基层党委的日常工作，抓党建也抓发展。",
                "党委", "基层治理", "乡镇（街道）", "基层党建、干部队伍与统筹协调",
                52, 44, 55, 58, setOf(EventTag.RURAL, EventTag.MASS_APPEAL, EventTag.LEADER_TALK),
                listOf("组织人事", "基层治理"), route = PositionRoute.LOCAL_GOVERNANCE, careerValue = 6,
                nextPositions = listOf("pos_4_town_secretary", "pos_4_town_head", "pos_4_organization")
            ),
            Blueprint(
                "pos_3_department_deputy", "县直部门副职", "进入部门班子，开始管人管事。",
                "部门", "部门管理", "县直部门", "日常行政与队伍管理",
                40, 42, 46, 44, setOf(EventTag.PROJECT, EventTag.COORDINATION),
                listOf("部门管理"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 4,
                nextPositions = listOf("pos_4_department_head", "pos_4_gov_office_head", "pos_4_finance_head")
            ),
            Blueprint(
                "pos_3_town_deputy", "乡镇（街道）副职", "分管一块具体工作，直接面对群众和现场。",
                "政府", "基层治理", "乡镇（街道）", "民生事务、矛盾化解与现场处置",
                44, 48, 44, 62, setOf(EventTag.MASS_APPEAL, EventTag.COMPLAINT, EventTag.RURAL),
                listOf("基层治理", "综合行政"), route = PositionRoute.LOCAL_GOVERNANCE, careerValue = 5,
                nextPositions = listOf("pos_4_town_head", "pos_4_town_secretary")
            ),
            Blueprint(
                "pos_3_gov_office_deputy", "县政府办公室副主任", "在枢纽位置上服务决策、协调各方。",
                "政府办公室", "综合行政", "县政府办公室", "综合协调、材料与督办",
                48, 42, 50, 40, setOf(EventTag.MATERIAL, EventTag.COORDINATION, EventTag.MEETING),
                listOf("综合行政"), route = PositionRoute.POLICY, careerValue = 5,
                nextPositions = listOf("pos_4_gov_office_head", "pos_4_organization"),
                minLeaderTrust = 45
            ),
            Blueprint(
                "pos_3_finance_deputy", "县财政局副局长", "在财政与资金条线上积累专业资历。",
                "财政", "财政系统", "县直部门", "预算、资金与财政管理",
                46, 56, 48, 42, setOf(EventTag.PROJECT, EventTag.MATERIAL, EventTag.INSPECTION),
                listOf("财政系统", "经济发展"), route = PositionRoute.ECONOMY, careerValue = 6,
                nextPositions = listOf("pos_4_finance_head", "pos_4_department_head"),
                minLeaderTrust = 50
            ),
            Blueprint(
                "pos_3_culture_deputy", "县文化旅游部门副职", "在宣传文旅条线上做城市形象与公共服务。",
                "宣传", "宣传文化", "县直部门", "文旅、宣传与公共服务",
                44, 46, 44, 58, setOf(EventTag.PUBLIC_OPINION, EventTag.PROJECT, EventTag.RESEARCH),
                listOf("宣传文化", "综合行政"), route = PositionRoute.CULTURE, careerValue = 4,
                nextPositions = listOf("pos_4_department_head", "pos_4_gov_office_head"),
                minLeaderTrust = 50
            )
        )
        4 -> listOf(
            Blueprint(
                "pos_4_town_secretary", "乡党委书记", "基层党委主要负责人，抓班子、带队伍、管方向。",
                "党委", "组织人事", "乡镇（街道）", "基层党建、干部队伍与一方发展",
                58, 50, 60, 62, setOf(EventTag.RURAL, EventTag.LEADER_TALK, EventTag.COORDINATION),
                listOf("组织人事", "基层治理"), route = PositionRoute.LOCAL_GOVERNANCE, careerValue = 9,
                nextPositions = listOf("pos_5_deputy_secretary", "pos_5_county_deputy_head", "pos_5_standing_committee", "pos_5_key_town_head")
            ),
            Blueprint(
                "pos_4_town_head", "乡镇长", "负责政府行政事务，经济、财政、民生一起抓。",
                "政府", "基层治理", "乡镇（街道）", "经济、财政、民生与行政事务",
                50, 58, 52, 66, setOf(EventTag.RURAL, EventTag.PROJECT, EventTag.MASS_APPEAL),
                listOf("基层治理", "经济发展"), route = PositionRoute.LOCAL_GOVERNANCE, careerValue = 8,
                nextPositions = listOf("pos_5_county_deputy_head", "pos_5_key_town_head", "pos_5_deputy_secretary")
            ),
            Blueprint(
                "pos_4_gov_office_head", "县政府办公室主任", "县政府的枢纽岗位，协调与督办并重。",
                "政府办公室", "综合行政", "县政府办公室", "综合协调、督查督办与材料",
                56, 50, 58, 46, setOf(EventTag.MATERIAL, EventTag.MEETING, EventTag.COORDINATION),
                listOf("综合行政", "组织人事"), route = PositionRoute.POLICY, careerValue = 9,
                nextPositions = listOf("pos_5_county_deputy_head", "pos_5_city_bureau_deputy", "pos_5_deputy_secretary")
            ),
            Blueprint(
                "pos_4_department_head", "县直部门局长", "主持一个县直部门，掌握一条线的资源与项目。",
                "政府", "部门管理", "县直部门", "行业管理与项目推进",
                52, 56, 52, 50, setOf(EventTag.PROJECT, EventTag.INSPECTION, EventTag.ACCEPTANCE),
                listOf("部门管理", "经济发展"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 8,
                nextPositions = listOf("pos_5_county_deputy_head", "pos_5_county_bureau_deputy", "pos_5_city_bureau_deputy"),
                minMerit = 8
            ),
            Blueprint(
                "pos_4_finance_head", "县财政局局长", "掌握全县预算与资金调度，专业门槛高。",
                "财政", "财政系统", "县直部门", "预算编制、资金调度与财政监督",
                54, 62, 54, 48, setOf(EventTag.MATERIAL, EventTag.PROJECT, EventTag.INSPECTION),
                listOf("财政系统", "经济发展"), route = PositionRoute.ECONOMY, careerValue = 10,
                nextPositions = listOf("pos_5_county_deputy_head", "pos_5_city_bureau_deputy", "pos_5_county_bureau_deputy"),
                minMerit = 8
            ),
            Blueprint(
                "pos_4_organization", "县委组织部相关负责人", "在干部与组织工作中参与把关。",
                "党委", "组织人事", "县委部门", "干部考察、组织建设与人才工作",
                60, 44, 64, 42, setOf(EventTag.CAREER, EventTag.LEADER_TALK, EventTag.MATERIAL),
                listOf("组织人事"), route = PositionRoute.ORGANIZATION, careerValue = 9,
                nextPositions = listOf("pos_5_deputy_secretary", "pos_5_standing_committee"),
                minLeaderTrust = 55
            )
        )
        5 -> listOf(
            Blueprint(
                "pos_5_county_deputy_head", "副县长", "在县级平台上分管一块工作，承上启下。",
                "政府", "综合行政", "县政府", "分管领域、跨部门协调与督查",
                62, 62, 60, 58, setOf(EventTag.PROJECT, EventTag.COORDINATION, EventTag.INSPECTION),
                listOf("经济发展", "综合行政"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 13,
                nextPositions = listOf("pos_6_county_head", "pos_6_county_secretary", "pos_6_development_zone")
            ),
            Blueprint(
                "pos_5_deputy_secretary", "县委副书记", "协助书记统筹，组织与协调权重高。",
                "党委", "组织人事", "县委", "统筹协调、党务与干部工作",
                70, 56, 68, 56, setOf(EventTag.COORDINATION, EventTag.LEADER_TALK, EventTag.CAREER),
                listOf("组织人事", "综合行政"), route = PositionRoute.ORGANIZATION, careerValue = 14,
                nextPositions = listOf("pos_6_county_secretary", "pos_6_county_head")
            ),
            Blueprint(
                "pos_5_county_bureau_deputy", "县直部门副职", "负责一条线的业务与队伍。",
                "政府", "部门管理", "县直部门", "业务条线管理与执行",
                52, 56, 54, 50, setOf(EventTag.PROJECT, EventTag.ACCEPTANCE),
                listOf("部门管理"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 11,
                nextPositions = listOf("pos_6_city_bureau_head", "pos_6_county_head", "pos_6_city_finance_head")
            ),
            Blueprint(
                "pos_5_city_bureau_deputy", "市直部门副职", "进入市级机关班子，视野更宽。",
                "政府", "部门管理", "市直部门", "行业政策与全市条线指导",
                56, 58, 58, 48, setOf(EventTag.MATERIAL, EventTag.RESEARCH, EventTag.PROJECT),
                listOf("部门管理", "综合行政"), route = PositionRoute.POLICY, careerValue = 13,
                nextPositions = listOf("pos_6_city_bureau_head", "pos_6_city_finance_head", "pos_6_development_zone")
            ),
            Blueprint(
                "pos_5_standing_committee", "县委常委", "进入县级班子，参与重大决策。",
                "党委", "组织人事", "县委", "重大决策、分工负责与干部工作",
                70, 56, 66, 54, setOf(EventTag.MEETING, EventTag.LEADER_TALK, EventTag.CAREER),
                listOf("组织人事", "综合行政"), route = PositionRoute.ORGANIZATION, careerValue = 15,
                nextPositions = listOf("pos_6_county_secretary", "pos_6_county_head"),
                minLeaderTrust = 62
            ),
            Blueprint(
                "pos_5_key_town_head", "重点乡镇主要负责人", "在经济强镇或重点镇挑大梁。",
                "政府", "基层治理", "重点乡镇", "镇域经济、治理与重大项目",
                56, 64, 56, 66, setOf(EventTag.PROJECT, EventTag.RURAL, EventTag.MASS_APPEAL),
                listOf("基层治理", "经济发展"), route = PositionRoute.LOCAL_GOVERNANCE, careerValue = 12,
                nextPositions = listOf("pos_6_development_zone", "pos_6_county_head", "pos_6_city_bureau_head")
            )
        )
        6 -> listOf(
            Blueprint(
                "pos_6_county_head", "县长", "主政一个县域，经济、民生、稳定一肩挑。",
                "政府", "综合行政", "县政府", "全域行政、财政与民生",
                68, 70, 66, 68, setOf(EventTag.RURAL, EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.LEADER_TASK),
                listOf("经济发展", "综合行政"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 16,
                nextPositions = listOf("pos_7_deputy_mayor", "pos_7_deputy_secretary")
            ),
            Blueprint(
                "pos_6_county_secretary", "县委书记", "一个县的全面领导，抓方向也抓班子。",
                "党委", "组织人事", "县委", "全面领导、组织人事与重大决策",
                76, 64, 74, 62, setOf(EventTag.COORDINATION, EventTag.LEADER_TALK, EventTag.CAREER),
                listOf("组织人事", "综合行政"), route = PositionRoute.ORGANIZATION, careerValue = 17,
                nextPositions = listOf("pos_7_deputy_secretary", "pos_7_deputy_mayor")
            ),
            Blueprint(
                "pos_6_city_bureau_head", "市直部门局长", "主持一个市直部门，统筹全市一条线。",
                "政府", "部门管理", "市直部门", "行业政策、项目与全市统筹",
                58, 64, 60, 52, setOf(EventTag.PROJECT, EventTag.INSPECTION, EventTag.RESEARCH),
                listOf("部门管理", "经济发展"), route = PositionRoute.POLICY, careerValue = 15,
                nextPositions = listOf("pos_7_deputy_mayor", "pos_7_province_deputy")
            ),
            Blueprint(
                "pos_6_development_zone", "开发区主要负责人", "在发展与改革一线挑担子。",
                "政府", "经济发展", "开发区", "招商引资、项目建设与产业培育",
                58, 72, 58, 56, setOf(EventTag.PROJECT, EventTag.RESEARCH, EventTag.ACCEPTANCE),
                listOf("经济发展"), route = PositionRoute.ECONOMY, careerValue = 17,
                nextPositions = listOf("pos_7_deputy_mayor", "pos_7_key_region_deputy"),
                minMerit = 15
            ),
            Blueprint(
                "pos_6_city_finance_head", "市财政局局长", "统筹全市财政，专业与责任并重。",
                "财政", "财政系统", "市直部门", "全市预算、资金与财政风险",
                58, 70, 58, 50, setOf(EventTag.MATERIAL, EventTag.PROJECT, EventTag.INSPECTION),
                listOf("财政系统", "经济发展"), route = PositionRoute.ECONOMY, careerValue = 17,
                nextPositions = listOf("pos_7_deputy_mayor", "pos_7_province_deputy"),
                minMerit = 15
            )
        )
        7 -> listOf(
            Blueprint(
                "pos_7_deputy_mayor", "副市长", "进入市一级班子，参与一个地区的整体决策。",
                "政府", "综合行政", "市政府", "分管领域、区域统筹与督查",
                72, 72, 68, 66, setOf(EventTag.PROJECT, EventTag.COORDINATION, EventTag.PUBLIC_OPINION),
                listOf("经济发展", "综合行政"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 19,
                nextPositions = listOf("pos_8_mayor", "pos_8_province_department_head")
            ),
            Blueprint(
                "pos_7_deputy_secretary", "市委副书记", "协助书记统筹，组织与协调权重高。",
                "党委", "组织人事", "市委", "统筹协调、党务与干部工作",
                80, 66, 78, 62, setOf(EventTag.COORDINATION, EventTag.LEADER_TALK, EventTag.CAREER),
                listOf("组织人事"), route = PositionRoute.ORGANIZATION, careerValue = 21,
                nextPositions = listOf("pos_8_secretary", "pos_8_mayor")
            ),
            Blueprint(
                "pos_7_province_deputy", "省直部门副职", "在省级机关分管一条线，视野更宽。",
                "政府", "部门管理", "省直部门", "全省条线政策与业务指导",
                68, 70, 70, 54, setOf(EventTag.RESEARCH, EventTag.MATERIAL, EventTag.INSPECTION),
                listOf("部门管理"), route = PositionRoute.POLICY, careerValue = 19,
                nextPositions = listOf("pos_8_province_department_head", "pos_8_province_key_department")
            ),
            Blueprint(
                "pos_7_key_region_deputy", "重要地区副职", "在重点区域参与班子决策，平台更高。",
                "党委", "综合行政", "重要地区", "区域发展、重大项目与综合治理",
                74, 70, 72, 62, setOf(EventTag.PROJECT, EventTag.CAREER, EventTag.COORDINATION),
                listOf("综合行政", "经济发展"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 22,
                nextPositions = listOf("pos_8_mayor", "pos_8_province_key_department"),
                minMerit = 22
            )
        )
        8 -> listOf(
            Blueprint(
                "pos_8_mayor", "市长", "主政一座地级市，经济、民生与治理全面负责。",
                "政府", "综合行政", "市政府", "全域行政、财政、民生与城市治理",
                78, 78, 74, 70, setOf(EventTag.PROJECT, EventTag.PUBLIC_OPINION, EventTag.MASS_APPEAL),
                listOf("经济发展", "综合行政"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 24,
                nextPositions = listOf("pos_9_sub_provincial_head", "pos_9_province_deputy", "pos_9_key_city_deputy")
            ),
            Blueprint(
                "pos_8_secretary", "市委书记", "一个市的全面领导，抓方向、抓班子、抓大局。",
                "党委", "组织人事", "市委", "全面领导、组织人事与重大决策",
                84, 70, 82, 66, setOf(EventTag.COORDINATION, EventTag.LEADER_TALK, EventTag.CAREER),
                listOf("组织人事", "综合行政"), route = PositionRoute.ORGANIZATION, careerValue = 26,
                nextPositions = listOf("pos_9_sub_provincial_head", "pos_9_key_city_deputy")
            ),
            Blueprint(
                "pos_8_province_department_head", "省政府部门厅长", "主持省政府一个部门，统筹全省一条线。",
                "政府", "部门管理", "省直部门", "全省行业政策、项目与监管",
                72, 74, 72, 56, setOf(EventTag.RESEARCH, EventTag.PROJECT, EventTag.INSPECTION),
                listOf("部门管理", "综合行政"), route = PositionRoute.POLICY, careerValue = 22,
                nextPositions = listOf("pos_9_province_deputy", "pos_9_key_city_deputy")
            ),
            Blueprint(
                "pos_8_province_key_department", "省级重要部门主要负责人", "在关键部门承担全省性责任。",
                "党委", "组织人事", "省委部门", "干部、组织与重大专项",
                82, 66, 80, 58, setOf(EventTag.CAREER, EventTag.MEETING, EventTag.LEADER_TALK),
                listOf("组织人事"), route = PositionRoute.ORGANIZATION, careerValue = 26,
                nextPositions = listOf("pos_9_province_deputy", "pos_9_sub_provincial_head"),
                minMerit = 30
            )
        )
        9 -> listOf(
            Blueprint(
                "pos_9_province_deputy", "省级部门副职", "在省级班子分管一块工作，责任与影响都很大。",
                "政府", "部门管理", "省直部门", "全省条线与班子分工",
                74, 74, 72, 60, setOf(EventTag.RESEARCH, EventTag.MATERIAL, EventTag.INSPECTION),
                listOf("部门管理"), route = PositionRoute.POLICY, careerValue = 26,
                nextPositions = listOf("pos_10_province_head", "pos_10_key_province_head")
            ),
            Blueprint(
                "pos_9_sub_provincial_head", "副省级城市正职", "到副省级城市主政一方，平台与挑战同时升级。",
                "政府", "综合行政", "副省级城市", "城市全域治理与重大决策",
                84, 82, 80, 76, setOf(EventTag.PROJECT, EventTag.COORDINATION, EventTag.CAREER),
                listOf("综合行政", "经济发展"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 30,
                nextPositions = listOf("pos_10_municipality_deputy", "pos_10_key_province_head", "pos_10_province_head")
            ),
            Blueprint(
                "pos_9_key_city_deputy", "重要城市副职", "在重点城市参与班子决策。",
                "党委", "组织人事", "重要城市", "班子分工、组织协调与专项工作",
                80, 72, 78, 68, setOf(EventTag.CAREER, EventTag.MEETING, EventTag.LEADER_TALK),
                listOf("组织人事", "综合行政"), route = PositionRoute.ORGANIZATION, careerValue = 27,
                nextPositions = listOf("pos_10_municipality_deputy", "pos_10_province_head")
            )
        )
        10 -> listOf(
            Blueprint(
                "pos_10_province_head", "省级部门主要负责人", "主持省级部门，统筹全省一个领域的资源。",
                "政府", "部门管理", "省直部门", "全省领域统筹与政策制定",
                78, 78, 76, 62, setOf(EventTag.RESEARCH, EventTag.MEETING, EventTag.MATERIAL),
                listOf("部门管理", "综合行政"), route = PositionRoute.POLICY, careerValue = 29,
                nextPositions = listOf("pos_11_national_deputy", "pos_11_key_institution")
            ),
            Blueprint(
                "pos_10_municipality_deputy", "直辖市副职", "在直辖市班子里分管重要领域。",
                "政府", "综合行政", "直辖市", "超大城市治理与重大专项",
                86, 86, 82, 80, setOf(EventTag.PROJECT, EventTag.LEADER_TASK, EventTag.PUBLIC_OPINION),
                listOf("综合行政", "经济发展"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 33,
                nextPositions = listOf("pos_11_municipality_head", "pos_11_national_deputy")
            ),
            Blueprint(
                "pos_10_key_province_head", "重要省份行政主官", "在重要省份主持政府工作。",
                "政府", "综合行政", "省级行政区", "全省行政、经济与民生",
                88, 86, 84, 78, setOf(EventTag.MASS_APPEAL, EventTag.PROJECT, EventTag.CAREER),
                listOf("综合行政", "经济发展"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 34,
                nextPositions = listOf("pos_11_municipality_head", "pos_11_key_institution", "pos_11_national_deputy")
            )
        )
        11 -> listOf(
            Blueprint(
                "pos_11_national_deputy", "国家层面副职", "在国家层面参与重大决策。",
                "国家层面", "综合行政", "国家层面", "重大决策、跨区域统筹",
                92, 82, 88, 72, setOf(EventTag.RESEARCH, EventTag.MEETING, EventTag.CAREER),
                listOf("综合行政"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 36,
                nextPositions = listOf("pos_12_national_head", "pos_12_national_institution")
            ),
            Blueprint(
                "pos_11_municipality_head", "直辖市正职", "主政一座直辖市，全面领导。",
                "党委", "组织人事", "直辖市", "全面领导与重大决策",
                96, 84, 92, 78, setOf(EventTag.COORDINATION, EventTag.CAREER, EventTag.LEADER_TALK),
                listOf("组织人事", "综合行政"), route = PositionRoute.ORGANIZATION, careerValue = 38,
                nextPositions = listOf("pos_12_national_head", "pos_12_national_deputy")
            ),
            Blueprint(
                "pos_11_key_institution", "重要机构主要负责人", "在关键机构承担全局性责任。",
                "国家层面", "综合行政", "国家层面", "全局统筹、改革与政策",
                92, 80, 90, 70, setOf(EventTag.RESEARCH, EventTag.CAREER, EventTag.MEETING),
                listOf("综合行政"), route = PositionRoute.POLICY, careerValue = 36,
                nextPositions = listOf("pos_12_national_institution", "pos_12_national_head")
            )
        )
        12 -> listOf(
            Blueprint(
                "pos_12_national_head", "国家层面主要负责人", "站到职业生涯的最高处，守成与进取同样重要。",
                "国家层面", "综合行政", "国家层面", "全局领导与重大决策",
                98, 88, 94, 80, setOf(EventTag.RESEARCH, EventTag.CAREER),
                listOf("综合行政"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 40,
                minMerit = 60
            ),
            Blueprint(
                "pos_12_national_deputy", "国家层面重要副职", "在最高层级承担分工与专项。",
                "国家层面", "综合行政", "国家层面", "分工负责与专项统筹",
                96, 86, 92, 78, setOf(EventTag.RESEARCH, EventTag.MEETING),
                listOf("综合行政"), route = PositionRoute.GENERAL_GOVERNMENT, careerValue = 38,
                minMerit = 62
            ),
            Blueprint(
                "pos_12_national_institution", "国家层面重要机构负责人", "主持关键机构，责任重大。",
                "国家层面", "部门管理", "国家层面", "机构统筹与政策落实",
                94, 84, 92, 74, setOf(EventTag.RESEARCH, EventTag.MATERIAL),
                listOf("综合行政"), route = PositionRoute.POLICY, careerValue = 36,
                minMerit = 64
            )
        )
        else -> emptyList()
    }
}