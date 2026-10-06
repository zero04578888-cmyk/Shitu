package com.shitu.app.model

/**
 * 第十二阶段：世界状态与仕途世界的动态部分。
 *
 * 第十三阶段把这里扩展成「世界会自己运转」的载体：
 * 经济周期、时代、地区动态、政策记录、决策记忆、世界动态、NPC 自主状态、
 * 关系网络与岗位空缺都保存在这里。所有字段都有默认值，旧存档读入后即为默认世界。
 */
data class WorldState(
    val economyClimate: Int = 50,
    val environmentClimate: Int = 50,
    val npcStatuses: Map<String, NpcStatus> = emptyMap(),
    val builtProjects: List<String> = emptyList(),
    val majorFlags: Set<String> = emptySet(),
    val research: List<ResearchNote> = emptyList(),
    // ===== 第十三阶段 =====
    /** 绝对月序号（从 0 开始），世界模拟的时间基准。 */
    val month: Int = 0,
    val economy: GlobalEconomy = GlobalEconomy(),
    val era: EraState = EraState.DEVELOPMENT,
    val districts: Map<String, DistrictState> = emptyMap(),
    val policies: List<PolicyRecord> = emptyList(),
    val decisions: List<DecisionRecord> = emptyList(),
    val news: List<WorldNews> = emptyList(),
    val npcs: Map<String, NpcRuntime> = emptyMap(),
    /** NPC 之间的关系网络，键为「较小的 id|较大的 id」。 */
    val npcRelations: Map<String, Int> = emptyMap(),
    val vacancies: List<Vacancy> = emptyList()
) {

    // ===== 人物状态 =====

    fun statusOf(npcId: String): NpcStatus = npcStatuses[npcId] ?: NpcStatus.NORMAL

    fun withStatus(npcId: String, status: NpcStatus): WorldState =
        copy(npcStatuses = npcStatuses + (npcId to status))

    fun runtimeOf(npcId: String): NpcRuntime = npcs[npcId] ?: NpcRuntime(npcId)

    fun withRuntime(runtime: NpcRuntime): WorldState =
        copy(npcs = npcs + (runtime.npcId to runtime.normalized()))

    // ===== 项目 =====

    fun withBuilt(projectId: String): WorldState =
        if (projectId in builtProjects) this else copy(builtProjects = builtProjects + projectId)

    fun hasBuilt(projectId: String): Boolean = projectId in builtProjects

    // ===== 调研 =====

    fun withResearch(note: ResearchNote): WorldState {
        val next = research.filterNot { it.key == note.key } + note
        return copy(research = next.takeLast(MAX_RESEARCH))
    }

    fun hasResearch(key: String): Boolean = research.any { it.key == key }

    // ===== 标记 =====

    fun withFlag(flag: String): WorldState =
        if (flag in majorFlags) this else copy(majorFlags = majorFlags + flag)

    fun hasFlag(flag: String): Boolean = flag in majorFlags

    // ===== 地区 =====

    /** 取一个地区的动态状态；没有就按静态设定现算一个，保证不会空指针。 */
    fun districtOf(regionId: String): DistrictState =
        districts[regionId] ?: DistrictState.fromRegion(RegionCatalog.findOrDefault(regionId))

    fun withDistrict(district: DistrictState): WorldState =
        copy(districts = districts + (district.id to district.normalized()))

    // ===== 经济与时代 =====

    fun withEconomy(next: GlobalEconomy): WorldState = copy(economy = next)

    fun withEra(next: EraState): WorldState = copy(era = next)

    // ===== 政策与决策记忆 =====

    fun withPolicy(record: PolicyRecord): WorldState =
        copy(policies = (policies.filterNot { it.id == record.id } + record).takeLast(MAX_POLICIES))

    fun withDecision(record: DecisionRecord): WorldState =
        copy(decisions = (decisions.filterNot { it.id == record.id } + record).takeLast(MAX_DECISIONS))

    /** 某个政策方向在最近 [withinYears] 年内的累计强度。 */
    fun policyPressure(tag: PolicyTag, currentYear: Int, withinYears: Int = 8): Int =
        policies.filter { currentYear - it.year < withinYears }
            .filter { tag in it.tags }
            .sumOf { it.magnitude }

    /** 与某个方向相关的决策记忆。 */
    fun decisionsWith(tag: PolicyTag): List<DecisionRecord> = decisions.filter { tag in it.tags }

    // ===== 世界动态 =====

    fun withNews(item: WorldNews): WorldState =
        copy(news = (news.filterNot { it.id == item.id } + item).takeLast(MAX_NEWS))

    fun newsOfYear(year: Int): List<WorldNews> = news.filter { it.year == year }

    fun recentNews(limit: Int): List<WorldNews> = news.takeLast(limit).reversed()

    // ===== NPC 关系网络 =====

    fun relationOf(a: String, b: String): Int = npcRelations[relationKey(a, b)] ?: 0

    fun withNpcRelation(a: String, b: String, delta: Int): WorldState = this.withNpcRelationValue(
        a, b, (relationOf(a, b) + delta).coerceIn(RELATION_MIN, RELATION_MAX)
    )

    fun withNpcRelationValue(a: String, b: String, value: Int): WorldState =
        copy(npcRelations = npcRelations + (relationKey(a, b) to value.coerceIn(RELATION_MIN, RELATION_MAX)))

    // ===== 岗位空缺 =====

    fun withVacancy(vacancy: Vacancy): WorldState =
        copy(vacancies = vacancies.filterNot { it.positionIndex == vacancy.positionIndex && !it.filled } + vacancy)

    fun openVacancy(positionIndex: Int): Vacancy? =
        vacancies.firstOrNull { it.positionIndex == positionIndex && !it.filled }

    fun fillVacancy(positionIndex: Int): WorldState = copy(
        vacancies = vacancies.map {
            if (it.positionIndex == positionIndex && !it.filled) it.copy(filled = true) else it
        }
    )

    /** 清理过期空缺，避免列表无限增长。 */
    fun pruneVacancies(currentMonth: Int): WorldState = copy(
        vacancies = (vacancies.filter { it.filled } + vacancies.filter { !it.filled })
            .filter { it.filled || currentMonth - it.createdMonth <= VACANCY_TTL_MONTHS }
            .takeLast(MAX_VACANCIES)
    )

    companion object {
        const val MAX_RESEARCH = 24
        const val MAX_NEWS = 80
        const val MAX_POLICIES = 160
        const val MAX_DECISIONS = 120
        const val MAX_VACANCIES = 24
        const val RELATION_MIN = -100
        const val RELATION_MAX = 100
        const val VACANCY_TTL_MONTHS = 24

        fun relationKey(a: String, b: String): String = if (a <= b) "$a|$b" else "$b|$a"
    }
}

/** 一次调任机会。玩家可以接受或拒绝。 */
data class TransferOffer(
    val regionId: String,
    val reason: String,
    /** 过了这个绝对季度就失效。 */
    val expiresQuarter: Int,
    /** 该机会的门槛加成（0～3）。 */
    val tier: Int = 1,
    val positionIndex: Int = 0
)

/** 调研得到的一条治理信息，可解锁政策或项目。 */
data class ResearchNote(
    val id: String,
    val label: String,
    val detail: String,
    val year: Int = 1,
    val quarter: Int = 1
) {
    /** 调研记录的稳定标识（按区域+主题）。 */
    val key: String get() = id
}

/** 每天/每季度的工作安排。安排会影响精力、关系与事件概率。 */
enum class WorkSlot(val label: String, val description: String) {
    MEETING("开会协调", "把各方拢到一起，关系与协调见长，但最耗精力。"),
    RESEARCH("下基层调研", "掌握真实情况，能解锁项目与政策。"),
    MATERIAL("处理材料", "把事写清楚，上级看得见，但离群众远。"),
    RECEPTION("接访群众", "面对面解决问题，民望与群众基础上升。"),
    STUDY("学习充电", "个人能力成长快，短期治理成绩少。"),
    FAMILY("陪伴家人", "家庭关系与压力恢复，工作推进慢一些。"),
    REST("休息调整", "精力恢复最快，其他方面按部就班。")
}

/** 本季度的工作安排（三个时段）。 */
data class WorkPlan(
    val slots: List<WorkSlot> = emptyList(),
    /** 该安排对应的绝对季度，-1 表示尚未安排。 */
    val plannedQuarter: Int = -1
) {
    val isPlanned: Boolean get() = slots.isNotEmpty()

    fun countOf(slot: WorkSlot): Int = slots.count { it == slot }

    companion object {
        const val SLOT_COUNT = 3
    }
}