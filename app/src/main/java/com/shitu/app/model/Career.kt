package com.shitu.app.model

/** 可成长的能力项。经验升级时按主导方向提升，设有上限。 */
enum class CareerAbility(val label: String, val description: String) {
    LEADERSHIP("领导力", "带队伍、定方向、压得住阵脚。"),
    DECISION("决策能力", "在信息不全的时候敢于拍板。"),
    COMMUNICATION("沟通能力", "把复杂的事讲明白，把不同的人拢到一起。"),
    EXECUTION("执行力", "定下来的事能一层层落实到位。"),
    SPECIALTY("专业能力", "在某个领域说得出内行话。");

    companion object {
        fun byName(value: String?): CareerAbility? = entries.firstOrNull { it.name == value }
    }
}

/** 履历条目类型。 */
enum class CareerRecordKind(val label: String) {
    START("参加工作"),
    PROMOTION("职务变动"),
    TRANSFER("岗位调整"),
    PROJECT("重要任务"),
    HONOR("考核评价"),
    SETBACK("挫折失误"),
    FAMILY("家庭大事"),
    CHOICE("重大抉择"),
    ACHIEVEMENT("成就"),
    RETIRE("退休")
}

/** 一条履历。所有经历都进入存档。 */
data class CareerRecord(
    val year: Int,
    val quarter: Int,
    val age: Int,
    val title: String,
    val detail: String,
    val kind: CareerRecordKind,
    val positionName: String = "",
    /** 完整职位显示名「职级·具体职位」，如「正厅级·市长」；旧记录为空。 */
    val positionTitle: String = "",
    /** 任职地区（辖区）名称，用于履历时间线展示。 */
    val regionName: String = "",
    /** 职务变动前的职级名称（晋升 / 降职 / 调任）。 */
    val fromPosition: String = "",
    /** 职务变动后的职级名称。 */
    val toPosition: String = ""
) {
    /** 展示用标题；旧存档残留的「主任科员」等名会迁移为完整职位名。 */
    val displayTitle: String get() = migrateLegacyPositionText(title)

    /** 展示用详情；同样做旧职位名迁移。 */
    val displayDetail: String get() = migrateLegacyPositionText(detail)

    /** 展示用职位名：优先完整职位名，回退具体职位名，并迁移旧名。 */
    val displayPositionLabel: String
        get() = migrateLegacyPositionText(positionTitle.ifBlank { positionName })

    /** 展示用变动前职级名，迁移旧版本残留名称。 */
    val displayFromPosition: String get() = migrateLegacyPositionText(fromPosition)

    /** 展示用变动后职级名，迁移旧版本残留名称。 */
    val displayToPosition: String get() = migrateLegacyPositionText(toPosition)
}

/** NPC 对玩家某件重要行为的长期记忆。会进入评价与结局原因。 */
data class CareerMemory(
    val npcId: String,
    val year: Int,
    val summary: String,
    val relationBias: Int,
    val trustBias: Int
)

/** 个人资产。来源全部为合法收入（工资与奖金）。 */
data class PersonalAssets(
    val housing: Int = 0,
    val vehicle: Int = 0
) {
    val housingLabel: String
        get() = when (housing) {
            0 -> "单位宿舍 / 租房"
            1 -> "已购住房"
            else -> "改善型住房"
        }

    val vehicleLabel: String
        get() = when (vehicle) {
            0 -> "公共交通"
            1 -> "已购代步车"
            else -> "改善型用车"
        }

    fun withHousing(level: Int): PersonalAssets = copy(housing = level.coerceIn(0, MAX_LEVEL))

    fun withVehicle(level: Int): PersonalAssets = copy(vehicle = level.coerceIn(0, MAX_LEVEL))

    companion object {
        const val MAX_LEVEL = 2
    }
}

/**
 * 第十阶段：完整仕途人生档案。
 *
 * 记录成长、履历、人物记忆与个人资产。等级由经验推导，避免出现两份不一致的数据。
 * 所有数值都是游戏模拟参数。
 */
data class CareerProfile(
    val experience: Int = 0,
    val leadership: Int = 30,
    val decision: Int = 30,
    val communication: Int = 30,
    val execution: Int = 30,
    val specialty: Int = 30,
    val positionsHeld: Int = 1,
    val highestPositionIndex: Int = 0,
    val personalActionsUsed: Int = 0,
    val records: List<CareerRecord> = emptyList(),
    val memories: List<CareerMemory> = emptyList(),
    val assets: PersonalAssets = PersonalAssets(),
    val unlockedAchievements: Set<String> = emptySet(),
    val priorLives: Int = 0,
    /**
     * 第十九阶段：走过的具体职务 id（按任职先后）。
     * 用于展示仕途路线与估算履历价值；旧存档为空，不影响任何逻辑。
     */
    val positionHistory: List<String> = emptyList()
) {
    val level: Int
        get() = (experience / EXPERIENCE_PER_LEVEL + 1).coerceAtMost(MAX_LEVEL)

    val experienceInLevel: Int
        get() = if (level >= MAX_LEVEL) EXPERIENCE_PER_LEVEL else experience % EXPERIENCE_PER_LEVEL

    /** 距离下一级还差多少经验；满级返回 0。 */
    val experienceToNext: Int
        get() = if (level >= MAX_LEVEL) 0 else level * EXPERIENCE_PER_LEVEL - experience

    fun valueOf(ability: CareerAbility): Int = when (ability) {
        CareerAbility.LEADERSHIP -> leadership
        CareerAbility.DECISION -> decision
        CareerAbility.COMMUNICATION -> communication
        CareerAbility.EXECUTION -> execution
        CareerAbility.SPECIALTY -> specialty
    }

    fun withValue(ability: CareerAbility, value: Int): CareerProfile {
        val v = value.coerceIn(0, MAX_ABILITY)
        return when (ability) {
            CareerAbility.LEADERSHIP -> copy(leadership = v)
            CareerAbility.DECISION -> copy(decision = v)
            CareerAbility.COMMUNICATION -> copy(communication = v)
            CareerAbility.EXECUTION -> copy(execution = v)
            CareerAbility.SPECIALTY -> copy(specialty = v)
        }
    }

    /** 综合能力，用于评价与晋升参考。 */
    val overallAbility: Int
        get() = (leadership + decision + communication + execution + specialty) / 5

    fun withRecord(record: CareerRecord): CareerProfile =
        copy(records = (records + record).takeLast(MAX_RECORDS))

    fun withMemory(memory: CareerMemory): CareerProfile =
        copy(memories = (memories + memory).takeLast(MAX_MEMORIES))

    fun memoryOf(npcId: String): List<CareerMemory> = memories.filter { it.npcId == npcId }

    fun withAchievement(id: String): CareerProfile = copy(unlockedAchievements = unlockedAchievements + id)

    fun recentRecords(count: Int): List<CareerRecord> = records.takeLast(count).reversed()

    companion object {
        const val EXPERIENCE_PER_LEVEL = 100
        const val MAX_LEVEL = 10
        const val MAX_ABILITY = 100
        const val MAX_RECORDS = 80
        const val MAX_MEMORIES = 40
    }
}

/** 人生评价的维度。 */
enum class LifeDimension(val label: String, val weight: Int) {
    GOVERNANCE("政绩", 22),
    ABILITY("能力", 16),
    REPUTATION("威望", 14),
    INTEGRITY("廉洁表现", 14),
    LEADERSHIP("领导评价", 12),
    RELATIONS("人际关系", 8),
    FAMILY("家庭平衡", 8),
    CAREER("职业履历", 6);

    companion object {
        fun byName(value: String?): LifeDimension? = entries.firstOrNull { it.name == value }
    }
}

/** 多维度人生评价，综合分只是参考，不单独决定结局。 */
data class LifeEvaluation(
    val governance: Int = 0,
    val ability: Int = 0,
    val reputation: Int = 0,
    val integrity: Int = 0,
    val leadership: Int = 0,
    val relations: Int = 0,
    val family: Int = 0,
    val career: Int = 0
) {
    fun scoreOf(dimension: LifeDimension): Int = when (dimension) {
        LifeDimension.GOVERNANCE -> governance
        LifeDimension.ABILITY -> ability
        LifeDimension.REPUTATION -> reputation
        LifeDimension.INTEGRITY -> integrity
        LifeDimension.LEADERSHIP -> leadership
        LifeDimension.RELATIONS -> relations
        LifeDimension.FAMILY -> family
        LifeDimension.CAREER -> career
    }

    val overall: Int
        get() = LifeDimension.entries.sumOf { scoreOf(it) * it.weight } / 100

    val rating: String
        get() = when {
            overall >= 88 -> "卓越"
            overall >= 76 -> "优秀"
            overall >= 64 -> "良好"
            overall >= 50 -> "合格"
            else -> "平庸"
        }

    val weakest: LifeDimension
        get() = LifeDimension.entries.minBy { scoreOf(it) }

    val strongest: LifeDimension
        get() = LifeDimension.entries.maxBy { scoreOf(it) }
}

/** 统一结局判定的输出。 */
data class CareerEndingResult(
    val id: String,
    val title: String,
    val category: String,
    val tier: String,
    val description: String,
    val reasons: List<String>,
    val keyNodes: List<String>,
    val keyChoices: List<String> = emptyList()
)

/** 成就定义。 */
data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val tier: Int
)

/** 一件成就加上解锁状态。 */
data class AchievementItem(
    val def: AchievementDef,
    val unlocked: Boolean
)
