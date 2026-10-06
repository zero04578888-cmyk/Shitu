package com.shitu.app.game

import com.shitu.app.model.LifeRecord
import com.shitu.app.model.positionAt

/** 把一局已结束的游戏整理成一条人生记录。 */
object ArchiveFactory {

    fun lifeRecordOf(state: GameState): LifeRecord? {
        val ending = state.ending ?: return null
        val player = state.player
        return LifeRecord(
            name = player.name,
            endingId = ending.id,
            endingTitle = ending.title,
            endingCategory = ending.category,
            positionName = player.positionName,
            positionTitle = state.displayPositionTitle,
            unitLabel = player.unit.label,
            serviceYears = player.serviceYears,
            promotionCount = state.promotionCount,
            reviewCount = state.reviewCount,
            achievements = state.career.unlockedAchievements.toList().sorted(),
            endingTier = ending.tier,
            lifeScore = CareerEngine.evaluate(state).overall,
            highestPosition = positionAt(state.career.highestPositionIndex).name
        )
    }

    /** 这一局让玩家看清了哪些发展路线。 */
    fun discoveredRoutesOf(state: GameState): Set<String> = buildSet {
        state.route?.let { add(it.name) }
        add(state.politics.dominantTendency.name)
    }

    /** 这一局建立了较深联系的人物。 */
    fun discoveredNpcsOf(state: GameState): Set<String> =
        NpcCatalog.npcs
            .filter {
                state.relationship.relationOf(it.id) >= 20 ||
                    state.relationship.trustOf(it.id) >= 30
            }
            .map { it.id }
            .toSet()
}
