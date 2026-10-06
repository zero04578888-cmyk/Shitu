package com.shitu.app.model

/** 一次走完的人生。用于结局图鉴与「最近的人生」列表。 */
data class LifeRecord(
    val name: String,
    val endingId: String,
    val endingTitle: String,
    val endingCategory: String,
    val positionName: String,
    val unitLabel: String,
    val serviceYears: Int,
    val promotionCount: Int,
    val reviewCount: Int,
    /** 这一局解锁的成就 id。 */
    val achievements: List<String> = emptyList(),
    /** 结局等级与人生评价综合分，作为档案里的亮点。 */
    val endingTier: String = "",
    val lifeScore: Int = 0,
    /** 走到过的最高职级名称。 */
    val highestPosition: String = "",
    /** 完整职位显示名「职级·具体职位」，如「正厅级·市长」；旧档案为空。 */
    val positionTitle: String = ""
) {
    /** 展示用职位名：优先完整职位名，回退最高职级名，并迁移旧版本残留名称。 */
    val displayPositionLabel: String
        get() = migrateLegacyPositionText(positionTitle.ifBlank { positionName })
}

/**
 * 玩家的收集档案。与存档分离，删除存档不会清空已解锁的结局与成就。
 * 多周目信息（已发现路线、已认识人物）也保存在这里。
 */
data class LifeArchive(
    val records: List<LifeRecord> = emptyList(),
    val achievements: Set<String> = emptySet(),
    val discoveredRoutes: Set<String> = emptySet(),
    val discoveredNpcs: Set<String> = emptySet(),
    val highestPositionIndex: Int = 0
) {
    val unlockedIds: Set<String> get() = records.mapTo(LinkedHashSet()) { it.endingId }

    val unlockedCount: Int get() = unlockedIds.size

    fun countOf(endingId: String): Int = records.count { it.endingId == endingId }

    /**
     * 追加一次人生。连续重复的同一条记录会被忽略，
     * 因此读档后重复结算同一结局不会重复计数。
     */
    fun appended(record: LifeRecord): LifeArchive {
        if (records.lastOrNull() == record) return this
        val next = records + record
        return copy(records = next.takeLast(MAX_RECORDS))
    }

    /** 合并一局带来的成就、路线与人物发现，并处理跨周目的元成就。 */
    fun mergedWith(record: LifeRecord, routes: Set<String>, npcs: Set<String>): LifeArchive {
        val withData = copy(
            achievements = achievements + record.achievements,
            discoveredRoutes = discoveredRoutes + routes,
            discoveredNpcs = discoveredNpcs + npcs,
            highestPositionIndex = maxOf(highestPositionIndex, positionIndexOf(record))
        )
        return if (withData.unlockedCount >= COLLECTOR_THRESHOLD) {
            withData.copy(achievements = withData.achievements + AchievementCatalog.META_COLLECTOR)
        } else {
            withData
        }
    }

    private fun positionIndexOf(record: LifeRecord): Int =
        POSITION_LADDER.indexOfFirst { it.name == record.highestPosition }.coerceAtLeast(0)

    companion object {
        const val MAX_RECORDS = 30
        const val COLLECTOR_THRESHOLD = 5
        val EMPTY = LifeArchive()
    }
}