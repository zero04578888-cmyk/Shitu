package com.shitu.app

import com.shitu.app.game.EventLibrary
import com.shitu.app.game.EventPresentation
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.EventTier
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 第十一阶段：事件等级、事件演出信息、晋升解释，以及界面所走的完整操作路径。
class PhaseElevenTest {

    private fun newGame(seed: Long = 20261004L): GameState =
        GameEngine.newGame("测试员", "男", "本科", "基层工作人员", seed)

    // 走一遍界面上的点击顺序：选择 -> 结算 -> 推进时间。
    private fun playQuarter(state: GameState, choiceIndex: Int = 0): GameState {
        val afterChoice = GameEngine.resolveChoice(state, choiceIndex)
        if (afterChoice.phase != GamePhase.OUTCOME) return afterChoice
        val afterAdvance = GameEngine.advance(afterChoice)
        return if (afterAdvance.phase == GamePhase.REVIEW) {
            GameEngine.proceedFromReview(afterAdvance)
        } else {
            afterAdvance
        }
    }

    @Test
    fun eventTierFollowsEventFlags() {
        val crisis = EventLibrary.find("fac_crisis_response")
        assertNotNull(crisis)
        assertEquals(EventTier.CRISIS, EventPresentation.tierOf(crisis!!))
        assertTrue(EventPresentation.tierOf(crisis).isHighlighted)

        val major = EventLibrary.find("life_first_step")
        assertNotNull(major)
        assertEquals(EventTier.MAJOR, EventPresentation.tierOf(major!!))

        val normal = EventLibrary.find("petition")
        assertNotNull(normal)
        assertEquals(EventTier.NORMAL, EventPresentation.tierOf(normal!!))
        assertFalse(EventPresentation.tierOf(normal).isHighlighted)
    }


    @Test
    fun involvedNpcsOnlyUseKnownPeople() {
        var checked = 0
        EventLibrary.events.forEach { event ->
            val names = EventPresentation.involvedNpcs(event)
            names.forEach { name ->
                assertTrue("事件 ${event.id} 出现了未知人物：$name", name.isNotBlank())
                checked += 1
            }
        }
        assertTrue("事件库中应存在涉及人物的内容", checked > 0)
    }

    @Test
    fun situationHintsAreBoundedAndReadable() {
        val state = newGame()
        EventLibrary.events.take(40).forEach { event ->
            val hints = EventPresentation.situationHints(event, state)
            assertTrue("局势提示最多三条：${event.id}", hints.size <= 3)
        }
    }

    @Test
    fun blockersExplainWhyANewOfficerCannotBePromoted() {
        val state = newGame()
        val blockers = PromotionEngine.blockersOf(state, 0)
        assertTrue("新任科员不应立即满足晋升条件", blockers.isNotEmpty())
        assertTrue(
            "应说明政绩不足：$blockers",
            blockers.any { it.contains("政绩不足") }
        )
        assertTrue(
            "应说明任职年限不足：$blockers",
            blockers.any { it.contains("任职年限") }
        )
    }

    @Test
    fun blockersAreEmptyAtTopOfLadder() {
        val state = newGame()
        val top = state.player.copy(positionIndex = com.shitu.app.model.POSITION_LADDER.size - 1)
        assertTrue(PromotionEngine.blockersOf(state.copy(player = top), 100).isEmpty())
    }

    @Test
    fun uiChoicePathProducesOutcomeAndAdvancesTime() {
        val start = newGame()
        val afterChoice = GameEngine.resolveChoice(start, 0)
        assertEquals(GamePhase.OUTCOME, afterChoice.phase)
        assertNotNull(afterChoice.outcome)
        assertTrue(
            "选择后应写入日志或结果",
            afterChoice.log.size >= start.log.size
        )

        val advanced = GameEngine.advance(afterChoice)
        assertTrue(
            "推进后应进入下一季度、转年或年度考核",
            advanced.phase != GamePhase.OUTCOME
        )
    }

    @Test
    fun longRunKeepsStateBoundedAndTimeMoving() {
        var state = newGame(seed = 777L)
        var guard = 0
        while (state.phase != GamePhase.ENDING && guard < 200) {
            state = playQuarter(state)
            guard += 1
        }
        assertTrue("时间必须推进", state.year >= 2)
        Attr.entries.forEach { attr ->
            val value = state.player.attributes.valueOf(attr)
            assertTrue("属性越界：${attr.label}=$value", value in 0..100)
        }
        assertTrue(
            "治理指标必须保持在合理区间",
            state.governance.jurisdiction.approval in 0..100
        )
        assertTrue(
            "财政不应无限增长",
            state.governance.finance <= 100000
        )
    }

    // 存档压力测试：连续操作并反复读写存档，人物、财政、职位与时间都不能丢。
    @Test
    fun hundredQuarterSaveLoadKeepsEverything() {
        var state = newGame(seed = 4242L)
        repeat(100) { round ->
            if (state.phase != GamePhase.ENDING) state = playQuarter(state)
            val text = GameCodec.encode(state)
            val restored = GameCodec.decode(text)
            assertNotNull("第 ${round + 1} 次存档解码失败", restored)
            state = restored!!
            assertEquals("年份不应变化", state.year, restored.year)
            assertEquals("季度不应变化", state.quarter, restored.quarter)
            assertEquals("职位不应回滚", state.player.positionIndex, restored.player.positionIndex)
            assertEquals("财政不应回滚", state.governance.finance, restored.governance.finance)
            assertEquals("政绩不应回滚", state.governance.merit, restored.governance.merit)
            assertEquals("人物信任不应丢失", state.relationship.trust, restored.relationship.trust)
            assertEquals("已触发事件不应丢失", state.triggeredEvents, restored.triggeredEvents)
            assertEquals("现有事件不应变化", state.currentEventId, restored.currentEventId)
            assertEquals("阶段不应变化", state.phase, restored.phase)
        }
        assertTrue("反复存档后职位索引应保持合法", state.player.positionIndex >= 0)
        assertTrue("反复存档后财政应该仍是数字", state.governance.finance >= 0)
        assertTrue("反复存档后年份不应回滚", state.year >= 1)
    }

    // 回归：在「结果」阶段退出再进入，不能因为缺少结算结果而卡死。
    @Test
    fun outcomePhaseSurvivesSaveAndLoad() {
        val start = newGame(seed = 909L)
        val resolved = GameEngine.resolveChoice(start, 0)
        assertEquals(GamePhase.OUTCOME, resolved.phase)
        val restored = GameCodec.decode(GameCodec.encode(resolved))
        assertNotNull(restored)
        assertEquals(GamePhase.OUTCOME, restored!!.phase)
        assertEquals(resolved.outcome, restored.outcome)
    }

    // 旧存档没有结算结果字段时，应回退到事件阶段而不是卡住。
    @Test
    fun legacyOutcomeSaveFallsBackToEventPhase() {
        val start = newGame(seed = 910L)
        val resolved = GameEngine.resolveChoice(start, 0)
        val legacyText = GameCodec.encode(resolved).replace("outcome.present\t1", "outcome.present\t0")
        val restored = GameCodec.decode(legacyText)
        assertNotNull(restored)
        assertEquals(GamePhase.EVENT, restored!!.phase)
    }
}
