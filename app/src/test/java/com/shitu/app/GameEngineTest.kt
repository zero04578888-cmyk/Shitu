package com.shitu.app

import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.GovernanceEngine
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.AnnualReviewEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.Attributes
import com.shitu.app.model.HiddenStats
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 游戏主流程：开局、选择、推进时间、年度考核、晋升。
class GameEngineTest {

    private fun newGame(seed: Long = 12345L): GameState =
        GameEngine.newGame("测试员", "男", "本科", "基层工作人员", seed)

    // 给测试用的“有政绩”的候选人：政绩与民望都达标，其余条件由测试自己控制。
    private fun GameState.withGovernance(merit: Int, approval: Int): GameState {
        var m = governance.merit
        MeritKind.entries.forEach { m = m.withValue(it, merit) }
        return copy(
            governance = governance.copy(
                merit = m,
                jurisdiction = governance.jurisdiction.withValue(Indicator.APPROVAL, approval)
            )
        )
    }

    @Test
    fun newGameCreatesPlayableState() {
        val state = newGame()
        assertTrue(state.currentEventId.isNotEmpty())
        assertNotNull(EventLibrary.find(state.currentEventId))
        assertEquals(GamePhase.EVENT, state.phase)
        assertEquals(1, state.year)
        assertEquals(1, state.quarter)
        assertEquals(0, state.player.serviceYears)
        Attr.entries.forEach { attr ->
            val value = state.player.attributes.valueOf(attr)
            assertTrue("初始属性越界：${attr.label}=$value", value in 0..100)
        }
    }

    @Test
    fun choiceChangesAttributesAccordingToEffects() {
        val state = newGame()
        val event = GameEngine.currentEvent(state)
        assertNotNull(event)
        val choice = event!!.choices[0]
        val before = state.player.attributes
        val after = GameEngine.resolveChoice(state, 0)

        Attr.entries.forEach { attr ->
            val expected = (before.valueOf(attr) + choice.effects.valueOf(attr)).coerceIn(0, 100)
            assertEquals(
                "属性 ${attr.label} 变化不正确",
                expected,
                after.player.attributes.valueOf(attr)
            )
        }
        assertEquals(GamePhase.OUTCOME, after.phase)
        assertEquals(1, after.log.size)
        assertNotNull(after.outcome)
    }

    @Test
    fun outOfRangeChoiceIsIgnored() {
        val state = newGame()
        val after = GameEngine.resolveChoice(state, 99)
        assertEquals(state.log.size, after.log.size)
        assertEquals(GamePhase.EVENT, after.phase)
    }

    @Test
    fun fourQuartersProduceAnnualReview() {
        var state = newGame(seed = 777L)
        repeat(4) {
            if (state.phase == GamePhase.EVENT) {
                state = GameEngine.resolveChoice(state, 0)
            }
            if (state.phase == GamePhase.OUTCOME) {
                state = GameEngine.advance(state)
            }
            if (state.phase == GamePhase.REVIEW) {
                state = GameEngine.proceedFromReview(state)
            }
        }
        assertEquals(1, state.reviewCount)
        assertEquals(2, state.year)
        assertEquals(1, state.quarter)
        assertEquals(1, state.reviews.size)
        val review = state.reviews.first()
        assertTrue(review.score in 0..100)
        assertTrue(review.grade.isNotEmpty())
        assertTrue(review.items.isNotEmpty())
    }

    @Test
    fun reviewScoreUsesSimulationWeights() {
        val state = newGame()
        val attributeScore = (
            AnnualReviewEngine.WEIGHTS.sumOf { (attr, weight) ->
                state.player.attributes.valueOf(attr) * weight
            } / 100 - state.player.attributes.integrityRisk / 5
            ).coerceIn(0, 100)
        val governanceScore = GovernanceEngine.governanceScore(state)
        val expected = (
            attributeScore * (100 - AnnualReviewEngine.GOVERNANCE_WEIGHT) +
                governanceScore * AnnualReviewEngine.GOVERNANCE_WEIGHT
            ) / 100
        assertEquals(expected, AnnualReviewEngine.conduct(state).score)
    }

    @Test
    fun promotionRequiresConditionsAndOpportunity() {
        val base = newGame()
        val strong = base.withGovernance(merit = 70, approval = 70).copy(
            player = base.player.copy(
                positionIndex = 2,
                yearsInPosition = 5,
                attributes = Attributes(
                    work = 95, ability = 92, politics = 92, writing = 80,
                    coordination = 85, masses = 92, leaderTrust = 95,
                    colleagues = 85, reputation = 80, integrityRisk = 5,
                    health = 85, energy = 85, family = 75, wealth = 60
                ),
                hidden = HiddenStats(opportunity = 95, mentor = 90)
            )
        )
        var granted = false
        for (seed in 1L..60L) {
            if (PromotionEngine.evaluate(strong, 92, GameRng(seed)).granted) {
                granted = true
                break
            }
        }
        assertTrue("条件具备时应存在晋升机会", granted)

        val risky = strong.copy(
            player = strong.player.copy(
                attributes = strong.player.attributes.copy(integrityRisk = 90)
            )
        )
        assertFalse("廉政风险过高时不应晋升", PromotionEngine.evaluate(risky, 92, GameRng(7L)).granted)

        val junior = strong.copy(player = strong.player.copy(positionIndex = 0, yearsInPosition = 0))
        assertFalse("任职年限不足时不应晋升", PromotionEngine.evaluate(junior, 92, GameRng(7L)).granted)
    }

    @Test
    fun attributesStayInRangeDuringLongRun() {
        var state = newGame(seed = 4242L)
        repeat(120) { step ->
            when (state.phase) {
                GamePhase.EVENT -> state = GameEngine.resolveChoice(state, step % 3)
                GamePhase.OUTCOME -> state = GameEngine.advance(state)
                GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> return
            }
            Attr.entries.forEach { attr ->
                val value = state.player.attributes.valueOf(attr)
                assertTrue("长时间模拟后属性越界：${attr.label}=$value", value in 0..100)
            }
        }
    }

    @Test
    fun longRunReachesAnEnding() {
        var state = newGame(seed = 20261004L)
        var guard = 0
        while (!state.isFinished && guard < 2000) {
            guard += 1
            when (state.phase) {
                GamePhase.EVENT -> state = GameEngine.resolveChoice(state, guard % 3)
                GamePhase.OUTCOME -> state = GameEngine.advance(state)
                GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> break
            }
        }
        assertTrue("完整人生应当产生结局", state.isFinished)
        assertNotNull(state.ending)
        assertTrue(state.ending!!.title.isNotEmpty())
        assertTrue(state.ending!!.highlights.isNotEmpty())
    }
}
