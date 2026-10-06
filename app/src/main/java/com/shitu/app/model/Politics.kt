package com.shitu.app.model

/** 政治倾向。由长期行为累积形成，不是一次性选择。 */
enum class PoliticalTendency(val label: String, val description: String) {
    PRAGMATIC("实干", "看重把事情做成，用政绩说话。"),
    BALANCED("平衡", "尽量不站队，在各方之间留余地。"),
    REFORMIST("改革", "愿意为做事担风险，倾向新办法。"),
    STEADY("稳健", "按规矩来，先保证不出事。");

    companion object {
        fun byName(value: String?): PoliticalTendency? = entries.firstOrNull { it.name == value }
    }
}

/** 情报等级。 */
enum class IntelLevel(val label: String) {
    COMMON("普通"), IMPORTANT("重要"), KEY("关键");

    companion object {
        fun byName(value: String?): IntelLevel = entries.firstOrNull { it.name == value } ?: COMMON
    }
}

/** 一条情报。合法渠道获得的公开或游戏内合理信息。 */
data class IntelItem(
    val id: String,
    val title: String,
    val detail: String,
    val level: IntelLevel,
    val year: Int = 1,
    val quarter: Int = 1
)

/** 临时联盟。到期自动解散，也可能因为事件提前结束。 */
data class Alliance(
    val id: String,
    val label: String,
    val npcId: String? = null,
    val faction: Faction? = null,
    val untilQuarter: Int = 0,
    val note: String = ""
) {
    fun isActive(absoluteQuarter: Int): Boolean = absoluteQuarter < untilQuarter
}


/** 政治危机。 */
data class CrisisState(
    val id: String,
    val title: String,
    val detail: String,
    val severity: Int,
    val year: Int
)

/** 履历中的一条重大经历。 */
data class HistoryEntry(
    val year: Int,
    val quarter: Int,
    val title: String,
    val choice: String,
    val consequence: String
)

/** 第九阶段：派系格局、情报与联盟的全部状态。 */
data class PoliticalState(
    val capital: Int = 0,
    val tendencies: Map<PoliticalTendency, Int> = emptyMap(),
    val factionInfluence: Map<Faction, Int> = emptyMap(),
    val intel: List<IntelItem> = emptyList(),
    val alliances: List<Alliance> = emptyList(),
    val crisis: CrisisState? = null,
    val history: List<HistoryEntry> = emptyList(),
    val majorChoices: Set<String> = emptySet()
) {
    fun tendencyOf(tendency: PoliticalTendency): Int = tendencies[tendency] ?: 0

    val dominantTendency: PoliticalTendency
        get() = PoliticalTendency.entries.maxByOrNull { tendencyOf(it) } ?: PoliticalTendency.BALANCED

    fun influenceOf(faction: Faction): Int = factionInfluence[faction] ?: 0

    fun activeAlliances(absoluteQuarter: Int): List<Alliance> =
        alliances.filter { it.isActive(absoluteQuarter) }

    fun withCapital(value: Int): PoliticalState = copy(capital = value.coerceIn(0, 100))

    fun withTendency(tendency: PoliticalTendency, delta: Int): PoliticalState =
        copy(tendencies = tendencies + (tendency to (tendencyOf(tendency) + delta).coerceAtLeast(0)))

    fun withInfluence(faction: Faction, delta: Int): PoliticalState =
        copy(factionInfluence = factionInfluence + (faction to (influenceOf(faction) + delta).coerceIn(0, 100)))

    fun withIntel(item: IntelItem): PoliticalState {
        val next = intel.filterNot { it.id == item.id } + item
        return copy(intel = next.takeLast(MAX_INTEL))
    }

    fun withAlliance(alliance: Alliance): PoliticalState =
        copy(alliances = alliances.filterNot { it.id == alliance.id } + alliance)

    fun withoutAlliance(id: String): PoliticalState =
        copy(alliances = alliances.filterNot { it.id == id })


    fun withHistory(entry: HistoryEntry): PoliticalState =
        copy(history = (history + entry).takeLast(MAX_HISTORY))

    fun withMajorChoice(id: String): PoliticalState = copy(majorChoices = majorChoices + id)

    companion object {
        const val MAX_INTEL = 20
        const val MAX_HISTORY = 40
    }
}

/** 一次选择带来的政治影响（第九阶段）。 */
data class PoliticalImpact(
    val capital: Int = 0,
    val spendCapital: Int = 0,
    val tendencies: Map<PoliticalTendency, Int> = emptyMap(),
    val factionInfluence: Map<Faction, Int> = emptyMap(),
    val intelId: String? = null,
    val startAlliance: String? = null,
    val endAlliance: String? = null,
    val startCrisis: String? = null,
    val resolveCrisis: Boolean = false,
    val history: String? = null
) {
    val isEmpty: Boolean
        get() = capital == 0 && spendCapital == 0 && tendencies.isEmpty() &&
            factionInfluence.isEmpty() && intelId == null && startAlliance == null &&
            endAlliance == null && startCrisis == null && !resolveCrisis && history == null
}
