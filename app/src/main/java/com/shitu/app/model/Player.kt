package com.shitu.app.model

/**
 * 玩家角色。所有字段都可被存档完整还原。
 */
data class Player(
    val name: String,
    val gender: String,
    val education: String,
    val startTrack: String,
    val unit: UnitType,
    val positionIndex: Int,
    val attributes: Attributes,
    val hidden: HiddenStats,
    val family: FamilyState,
    val ageAtStart: Int = 24,
    val serviceYears: Int = 0,
    val yearsInPosition: Int = 0,
    val traits: List<String> = emptyList(),
    /** 当前职务的开始时间（绝对季度序号）。旧存档为 0，读取时按任职年限回推。 */
    val positionStartAbs: Int = 0,
    /** 玩家在晋升时选定的具体职位名称；旧存档为空，继续用默认职务名。 */
    val appointmentTitle: String = "",
    /** 晋升时选定的具体职位 id；旧存档为空，读取时按城市与职级解析。 */
    val appointmentId: String = "",
    /** 任命城市 id；旧存档为空，读取时回退到当前任职城市。 */
    val appointmentCityId: String = "",
    /**
     * 出生年份（架空纪年）。
     * 旧存档为 0，读取时按开局年龄回退，不会闪退。
     */
    val birthYear: Int = 0,
    /** 是否已经按「退休」处理；退居二线为 false。 */
    val retired: Boolean = false,
    /** 退休类型：空 / "RETIREMENT"（退休）/ "SECOND_LINE"（退居二线）。 */
    val retirementType: String = "",
    /** 办理退休 / 退居二线时的年龄。 */
    val retirementAge: Int = 0,
    /** 办理退休 / 退居二线时的年份。 */
    val retirementYear: Int = 0
) {
    val position: PositionLevel get() = positionAt(positionIndex)

    /** 职级名称，如「正科级」。 */
    val positionName: String get() = position.name

    /** 当前职级（统一命名，等价于 [positionName]）。 */
    val currentRank: String get() = position.name

    /**
     * 归一化后的任命职位名：旧版残留名（主任科员 / 副主任科员）一律视为无效，
     * 由当前职级重新推导，避免旧存档显示「副科级·主任科员」这类倒退职位。
     */
    val effectiveAppointmentTitle: String
        get() = if (isLegacyPositionName(appointmentTitle)) "" else appointmentTitle

    /** 具体职位名称，如「市长」：优先取晋升时选定的职位，否则按单位推导。 */
    val currentPositionName: String
        get() = effectiveAppointmentTitle.ifBlank { positionTitleOf(unit, positionIndex) }

    /** 正式职位名称（具体职位）：保留旧调用点，等价于 [currentPositionName]。 */
    val positionTitle: String get() = currentPositionName

    /** 完整职位显示名「职级·具体职位」，如「正厅级·市长」。 */
    val displayTitle: String get() = positionDisplay(currentRank, currentPositionName)

    /** 是否已经明确到具体职位（否则需要用城市与职级解析默认职位）。 */
    val hasExplicitPosition: Boolean get() = effectiveAppointmentTitle.isNotBlank() || appointmentId.isNotBlank()

    val age: Int get() = ageAtStart + serviceYears

    /** 有效出生年份：旧存档没有 birthYear 时按开局年龄回推。 */
    val effectiveBirthYear: Int
        get() = if (birthYear != 0) birthYear else GameCalendar.birthYearOf(ageAtStart)

    /** 是否仍在正常仕途（未退休、未退居二线）。 */
    val inActiveCareer: Boolean get() = !retired && retirementType.isBlank()
}