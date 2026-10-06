package com.shitu.app.model

/**
 * 职位权限域。职级越高，能处理与触发的事件类型越多。
 * 仅作为游戏门控参数，不代表现实行政制度。
 */
enum class PositionScope(val label: String, val description: String) {
    BASE("基层事务", "基层民生、日常行政"),
    DEPARTMENT("部门管理", "地方治理、项目审批、部门管理"),
    COUNTY("县域治理", "重大项目、干部管理、地方经济"),
    CITY("区域统筹", "区域发展、重大政策、跨地区事务"),
    PROVINCE("全局统筹", "全国性政策、重大改革、经济与政治事务"),
    NATIONAL("国家层面", "最终剧情与特殊事件")
}

/**
 * 职级阶梯中的一级。
 *
 * 前十个字段沿用旧结构，新增字段全部带默认值，
 * 因此旧代码与旧存档都能平滑演进。所有门槛都是游戏模拟参数。
 */
data class PositionLevel(
    val index: Int,
    val name: String,
    val tier: Int,
    val minYearsInPosition: Int,
    val requiredWork: Int,
    val requiredAbility: Int,
    val requiredPolitics: Int,
    val requiredLeaderTrust: Int,
    val requiredMasses: Int,
    val maxIntegrityRisk: Int,
    /** 正式职位名称（普适版本，按单位细化见 positionTitleOf）。 */
    val title: String = "",
    /** 权力等级，1 起。 */
    val powerLevel: Int = index + 1,
    /** 每月基础收入，进入季度结算。 */
    val monthlyIncome: Int = 6 + index * 3,
    /** 在岗每年对政绩的自然影响。 */
    val monthlyMerit: Int = 0,
    /** 在岗每年对民望的影响。 */
    val approvalEffect: Int = 0,
    /** 在岗每年对威望（个人声望）的影响。 */
    val reputationEffect: Int = 0,
    /** 权限域，按有效职级档位 [tier] 展开，跳过旧版残留职级。 */
    val scope: PositionScope = when {
        tier <= 0 -> PositionScope.BASE
        tier <= 2 -> PositionScope.DEPARTMENT
        tier <= 4 -> PositionScope.COUNTY
        tier <= 6 -> PositionScope.CITY
        tier <= 8 -> PositionScope.PROVINCE
        else -> PositionScope.NATIONAL
    },
    /** 晋升到本级所需的威望。 */
    val requiredReputation: Int = 0,
    /** 晋升到本级所需的个人财富。 */
    val requiredWealth: Int = 0,
    /** 晋升到本级所需的已完成事件数量。 */
    val requiredCompletedEvents: Int = 0,
    /**
     * 是否为旧版残留职级（副主任科员 / 主任科员）。
     * 只为读取旧存档而保留：玩家晋升路径会跳过它们，界面也不再作为晋升职位展示。
     * 旧存档若停在这两级，读取时由 [normalizePositionIndex] 归一到副科级。
     */
    val legacy: Boolean = false
)

/**
 * 职级阶梯：科员起步，最高正国级。全部为虚构游戏设定。
 *
 * 其中「副主任科员 / 主任科员」是旧版本遗留的两级，标记 [PositionLevel.legacy]：
 * 新玩家的晋升路径会跳过它们（科员之后直接进入副科级），
 * 旧存档停在这两级时也会被归一到副科级，避免出现「职位变低」的观感。
 *
 * 有效晋升路线：
 * 科员 → 副科级·部门副职 → 正科级·部门负责人 → 副处级·副县长/副局长
 * → 正处级·县长/局长 → 副厅级·副市长/副厅长 → 正厅级·市长/厅长/市委书记 …
 *
 * [tier] 是有效档位（连续递增，用于晋升曲线），与数组下标 [index] 不再一一对应。
 */
val POSITION_LADDER: List<PositionLevel> = listOf(
    PositionLevel(0, "科员", 0, 2, 45, 40, 40, 35, 40, 60,
        title = "科员", powerLevel = 1, monthlyIncome = 6),
    // ===== 以下两级为旧版残留，仅用于兼容旧存档，不进入晋升路径 =====
    PositionLevel(1, "副主任科员", 0, 2, 48, 44, 43, 40, 43, 57,
        title = "部门副职", powerLevel = 2, monthlyIncome = 9, legacy = true),
    PositionLevel(2, "主任科员", 0, 3, 54, 50, 48, 46, 48, 52,
        title = "部门负责人", powerLevel = 3, monthlyIncome = 12, legacy = true),
    PositionLevel(3, "副科级", 1, 2, 52, 48, 46, 44, 46, 55,
        title = "部门副职", powerLevel = 4, monthlyIncome = 15,
        requiredReputation = 12),
    PositionLevel(4, "正科级", 2, 3, 60, 56, 54, 52, 54, 48,
        title = "部门负责人", powerLevel = 5, monthlyIncome = 18,
        approvalEffect = 1, requiredReputation = 20),
    PositionLevel(5, "副处级", 3, 4, 68, 63, 61, 60, 60, 42,
        title = "副县长", powerLevel = 6, monthlyIncome = 21,
        approvalEffect = 1, requiredReputation = 28),
    PositionLevel(6, "正处级", 4, 4, 74, 69, 67, 66, 64, 38,
        title = "县长", powerLevel = 7, monthlyIncome = 24,
        monthlyMerit = 1, approvalEffect = 1, requiredReputation = 34,
        requiredCompletedEvents = 3),
    PositionLevel(7, "副厅级", 5, 5, 79, 74, 72, 71, 67, 35,
        title = "副市长", powerLevel = 8, monthlyIncome = 27,
        monthlyMerit = 1, approvalEffect = 1, reputationEffect = 1,
        requiredReputation = 40, requiredWealth = 12, requiredCompletedEvents = 6),
    PositionLevel(8, "正厅级", 6, 6, 83, 78, 76, 75, 70, 33,
        title = "市长", powerLevel = 9, monthlyIncome = 30,
        monthlyMerit = 1, approvalEffect = 2, reputationEffect = 1,
        requiredReputation = 46, requiredWealth = 22, requiredCompletedEvents = 9),
    PositionLevel(9, "副部级", 7, 7, 87, 82, 80, 79, 73, 31,
        title = "省级部门副职", powerLevel = 10, monthlyIncome = 33,
        monthlyMerit = 2, approvalEffect = 2, reputationEffect = 2,
        requiredReputation = 52, requiredWealth = 32, requiredCompletedEvents = 12),
    PositionLevel(10, "正部级", 8, 7, 90, 85, 83, 83, 76, 29,
        title = "省级部门主要负责人", powerLevel = 11, monthlyIncome = 36,
        monthlyMerit = 2, approvalEffect = 2, reputationEffect = 2,
        requiredReputation = 58, requiredWealth = 42, requiredCompletedEvents = 15),
    PositionLevel(11, "副国级", 9, 8, 92, 88, 86, 86, 79, 27,
        title = "国家层面副职", powerLevel = 12, monthlyIncome = 39,
        monthlyMerit = 2, approvalEffect = 3, reputationEffect = 3,
        requiredReputation = 64, requiredWealth = 52, requiredCompletedEvents = 18),
    PositionLevel(12, "正国级", 10, 9, 95, 90, 90, 90, 83, 25,
        title = "国家层面主要负责人", powerLevel = 13, monthlyIncome = 42,
        monthlyMerit = 3, approvalEffect = 3, reputationEffect = 3,
        requiredReputation = 70, requiredWealth = 62, requiredCompletedEvents = 21)
)

fun positionAt(index: Int): PositionLevel =
    POSITION_LADDER[index.coerceIn(0, POSITION_LADDER.size - 1)]

/** 是否已经走到阶梯顶端。 */
fun isTopPosition(index: Int): Boolean = index >= POSITION_LADDER.size - 1

/** 玩家晋升路径上的第一个正式职级：副科级。 */
const val FIRST_GRADE_INDEX = 3

/** 是否为旧版残留职级（副主任科员 / 主任科员）。 */
fun isLegacyRank(index: Int): Boolean = POSITION_LADDER.getOrNull(index)?.legacy == true

/**
 * 旧版残留职位名（副主任科员 / 主任科员及其单位前缀变体）。
 * 旧存档的任命字段里可能保存着这些名字，读取与展示时都应视为无效，
 * 由当前职级重新推导具体职位，避免出现「副科级·主任科员」这类观感倒退。
 */
fun isLegacyPositionName(name: String?): Boolean {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isEmpty()) return false
    return trimmed.contains("主任科员") || trimmed.contains("副主任科员")
}

/** 旧版残留职位名迁移后的兜底完整职位名，例如「副科级·部门副职」。 */
fun legacyPositionFallbackTitle(): String =
    positionDisplay(positionAt(FIRST_GRADE_INDEX).name, positionAt(FIRST_GRADE_INDEX).title)

/**
 * 把旧存档历史文本里残留的「副主任科员 / 主任科员」替换为归一化后的完整职位名，
 * 例如「晋升 主任科员」→「晋升 副科级·部门副职」。
 *
 * 只用于展示（履历、日志等），不参与任何判定，也不改动晋升逻辑。
 */
fun migrateLegacyPositionText(
    text: String,
    replacement: String = legacyPositionFallbackTitle()
): String {
    if (text.isEmpty() || replacement.isEmpty()) return text
    var result = text
    if (result.contains("副主任科员")) result = result.replace("副主任科员", replacement)
    if (result.contains("主任科员")) result = result.replace("主任科员", replacement)
    return result
}

/**
 * 玩家的下一个有效职级：跳过旧版残留的「副主任科员 / 主任科员」两级。
 * 因此科员（0）之后的第一次晋升直接进入副科级，不会出现职位观感倒退。
 */
fun nextPromotionIndex(current: Int): Int {
    val top = POSITION_LADDER.size - 1
    return when {
        current < FIRST_GRADE_INDEX -> FIRST_GRADE_INDEX
        current >= top -> top
        else -> current + 1
    }
}

/** 降职时的上一个有效职级，同样跳过旧版残留职级，最低落到科员。 */
fun previousPromotionIndex(current: Int): Int =
    if (current <= FIRST_GRADE_INDEX) 0 else current - 1

/** 旧存档归一化：落在旧版残留职级上时统一落到副科级（只升不降）。 */
fun normalizePositionIndex(index: Int): Int =
    if (isLegacyRank(index)) FIRST_GRADE_INDEX else index.coerceIn(0, POSITION_LADDER.size - 1)

/**
 * 旧存档职级映射：早期梯队只有 9 级，读取时换算到新的 13 级，
 * 保证老玩家的相对位置不倒退。
 */
fun migrateLegacyPositionIndex(old: Int): Int = when (old.coerceIn(0, 8)) {
    0 -> 0
    1 -> 1
    2 -> 2
    3 -> 2
    4 -> 2
    5 -> 3
    6 -> 4
    7 -> 5
    else -> 6
}

/** 单位对应的科室名称，用于生成正式职位名称。 */
fun departmentOf(unit: UnitType): String = when (unit) {
    UnitType.TOWN -> "乡镇党政办公室"
    UnitType.STREET -> "街道综合办公室"
    UnitType.COUNTY -> "县区综合部门"
    UnitType.CITY -> "市级综合部门"
    UnitType.GENERAL -> "综合办公室"
    UnitType.BUSINESS -> "业务科室"
}

/**
 * 正式职位名称（不含职级前缀）。
 * 由职级数据推导，例如 3 →「部门副职」、5 →「副县长」、8 →「市长」。
 * 具体到某座城市时，界面会优先采用城市主官职位（市委书记 / 市长等）。
 */
fun positionTitleOf(unit: UnitType, index: Int): String =
    when (val safe = index.coerceIn(0, POSITION_LADDER.size - 1)) {
        0 -> "${unit.label}科员"
        else -> positionAt(safe).title.ifBlank { unit.label + "负责人" }
    }

/** 工作单位与平台特点，只作为游戏特色出现。 */
enum class UnitType(
    val label: String,
    val feature: String,
    val affinity: Set<EventTag>,
    val workGrowth: Int,
    val relationGrowth: Int,
    val materialGrowth: Int
) {
    TOWN(
        "乡镇",
        "群众事件多、突发任务多，工作强度高",
        setOf(EventTag.MASS_APPEAL, EventTag.DISASTER, EventTag.RURAL),
        2, 2, 0
    ),
    STREET(
        "街道",
        "城市治理与协调任务密集",
        setOf(EventTag.MASS_APPEAL, EventTag.COORDINATION, EventTag.COMPLAINT),
        1, 1, 0
    ),
    COUNTY(
        "区县机关",
        "承上启下，检查与材料任务并存",
        setOf(EventTag.INSPECTION, EventTag.MATERIAL, EventTag.MEETING),
        1, 1, 1
    ),
    CITY(
        "市级机关",
        "材料、协调、领导交办任务多",
        setOf(EventTag.MATERIAL, EventTag.LEADER_TASK, EventTag.COORDINATION),
        0, 1, 2
    ),
    GENERAL(
        "综合部门",
        "事务繁杂，跨部门协作频繁",
        setOf(EventTag.COORDINATION, EventTag.MEETING, EventTag.LEADER_TASK),
        1, 2, 1
    ),
    BUSINESS(
        "业务部门",
        "专业项目与验收任务多",
        setOf(EventTag.PROJECT, EventTag.ACCEPTANCE, EventTag.RESEARCH),
        2, 0, 2
    );

    companion object {
        fun byName(value: String): UnitType = entries.firstOrNull { it.name == value } ?: TOWN
    }
}