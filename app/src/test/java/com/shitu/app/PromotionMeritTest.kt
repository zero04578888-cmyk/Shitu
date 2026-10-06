package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.Attributes
import com.shitu.app.model.HiddenStats
import com.shitu.app.model.Merit
import com.shitu.app.model.MeritKind
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.positionAt
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 综合政绩晋升门槛：首级晋升统一为 5 分，且判定与界面共用同一数值。
 */
class PromotionMeritTest {

    private fun GameState.fullMerit(value: Int): GameState {
        var merit: Merit = governance.merit
        MeritKind.entries.forEach { merit = merit.withValue(it, value) }
        return copy(governance = governance.copy(merit = merit))
    }

    private fun firstPromotionState(merit: Int): GameState {
        val base = GameEngine.newGame("门槛测试者", "男", "硕士", "机关科员", 21L)
        return base.copy(
            player = base.player.copy(
                positionIndex = 0,
                yearsInPosition = 3,
                attributes = Attributes(
                    work = 95, ability = 92, politics = 92, writing = 85,
                    coordination = 88, masses = 92, leaderTrust = 95, colleagues = 85,
                    reputation = 80, integrityRisk = 4, health = 88, energy = 88,
                    family = 78, wealth = 60
                ),
                hidden = HiddenStats(opportunity = 95, mentor = 90)
            )
        ).fullMerit(merit)
    }

    private fun meritBlockerOf(state: GameState): Boolean =
        PromotionEngine.blockersOf(state, 90).any { it.contains("政绩不足") }

    @Test
    fun meritThresholdStartsAtFiveForFirstPromotion() {
        assertEquals(5, PromotionEngine.MERIT_BASE)
        assertEquals(5, PromotionEngine.requiredMerit(positionAt(1)))
        assertEquals("科员的第一步门槛应为 5", 5, PromotionEngine.requiredMerit(POSITION_LADDER[1]))
    }

    @Test
    fun meritThresholdRisesGentlyAndStaysReasonable() {
        val required = (1 until POSITION_LADDER.size).map { PromotionEngine.requiredMerit(POSITION_LADDER[it]) }
        assertEquals(required.sorted(), required)
        assertTrue("高级职位应当更难：$required", required.last() > required.first())
        assertTrue("最高门槛不应失控：$required", required.last() <= 60)
        // 处级以前保持温和，避免中期卡关。
        assertTrue("处级门槛应当温和：$required", required[5] <= 20)
    }

    @Test
    fun meritFourBlocksButFiveSatisfiesThisCondition() {
        assertTrue("综合政绩 4 不应满足", meritBlockerOf(firstPromotionState(4)))
        assertFalse("综合政绩 5 应当满足这一项", meritBlockerOf(firstPromotionState(5)))
    }

    @Test
    fun meritTenAlsoSatisfies() {
        assertFalse(meritBlockerOf(firstPromotionState(10)))
    }

    @Test
    fun promotionDecisionActuallyUsesTheFiveThreshold() {
        val four = firstPromotionState(4)
        for (seed in 1L..40L) {
            assertFalse(
                "政绩 4 时无论运气如何都不应晋升",
                PromotionEngine.evaluate(four, 90, GameRng(seed)).granted
            )
        }
        val five = firstPromotionState(5)
        var granted = false
        for (seed in 1L..40L) {
            if (PromotionEngine.evaluate(five, 90, GameRng(seed)).granted) {
                granted = true
                break
            }
        }
        assertTrue("政绩 5 且其余条件达标时应当能够晋升", granted)
    }

    @Test
    fun reloadKeepsPositionAndAppliesNewRule() {
        val state = firstPromotionState(5)
        val decoded = GameCodec.decode(GameCodec.encode(state))
        assertNotNull("存档应当可以读取", decoded)
        val other = decoded!!
        assertEquals("职位不能变化", state.player.positionIndex, other.player.positionIndex)
        assertEquals("政绩不能变化", 5, other.governance.merit.overall)
        assertFalse("重新进入后新规则立即生效", meritBlockerOf(other))
    }

    @Test
    fun requirementListShowsOverFiveNotEleven() {
        val satisfied = PromotionEngine.requirementList(firstPromotionState(7))
            .first { it.label == "综合政绩" }
        assertEquals(5, satisfied.required)
        assertEquals("7 / 5", satisfied.display)
        assertTrue(satisfied.met)

        val unmet = PromotionEngine.requirementList(firstPromotionState(3))
            .first { it.label == "综合政绩" }
        assertEquals(5, unmet.required)
        assertEquals("3 / 5", unmet.display)
        assertFalse(unmet.met)
    }
}