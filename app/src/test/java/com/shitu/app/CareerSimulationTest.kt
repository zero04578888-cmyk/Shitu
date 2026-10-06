package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.model.Attr
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 完整人生模拟：验证长流程不会崩溃、结局确实由数据决定。
class CareerSimulationTest {

    private fun playCareer(seed: Long, choicePicker: (Int) -> Int): GameState {
        var state = GameEngine.newGame("模拟者", "男", "本科", "乡镇公务员", seed)
        var step = 0
        while (!state.isFinished && step < 3000) {
            step += 1
            when (state.phase) {
                GamePhase.EVENT -> state = GameEngine.resolveChoice(state, choicePicker(step))
                GamePhase.OUTCOME -> state = GameEngine.advance(state)
                GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> break
            }
        }
        return state
    }

    @Test
    fun everyCareerTerminatesWithAValidEnding() {
        val endings = LinkedHashMap<String, Int>()
        val positions = LinkedHashMap<String, Int>()
        val strategies: List<(Int) -> Int> = listOf(
            { 0 },
            { 1 },
            { 2 },
            { step -> step % 3 },
            { step -> (step * 7) % 3 }
        )
        var totalYears = 0
        var runs = 0
        var longCareers = 0
        for (strategy in strategies) {
            for (seed in 1L..16L) {
                val state = playCareer(seed, strategy)
                runs += 1
                assertTrue("seed=$seed 的模拟没有产生结局", state.isFinished)
                val ending = state.ending
                assertTrue("ending 为空", ending != null)
                assertTrue("结局标题为空", ending!!.title.isNotBlank())
                assertTrue("结局缺少数据快照", ending.highlights.isNotEmpty())
                assertTrue("日志为空", state.log.isNotEmpty())
                assertTrue("服务年限异常：${state.player.serviceYears}", state.player.serviceYears in 1..GameEngine.MAX_SERVICE_YEARS)
                Attr.entries.forEach { attr ->
                    val value = state.player.attributes.valueOf(attr)
                    assertTrue("属性越界：${attr.label}=$value", value in 0..100)
                }
                endings[ending.id] = (endings[ending.id] ?: 0) + 1
                positions[state.player.positionName] = (positions[state.player.positionName] ?: 0) + 1
                totalYears += state.player.serviceYears
                if (state.player.serviceYears >= 15) longCareers += 1
            }
        }
        println("结局分布：$endings")
        println("最终职级分布：$positions")
        println("平均服务年限：${totalYears.toDouble() / runs}（共 $runs 局）")
        // 样本只覆盖 5 种固定选择策略，因此要求至少出现 4 种不同结局。
        assertTrue("结局种类过少：$endings", endings.size >= 4)
        assertTrue("多数人生过早结束，平衡异常：$endings", longCareers >= runs / 2)
    }

    @Test
    fun playStyleChangesTheEnding() {
        val cautious = playCareer(1L) { 2 }.ending?.id
        val steady = playCareer(1L) { 1 }.ending?.id
        val bold = playCareer(1L) { 0 }.ending?.id
        assertTrue("谨慎型玩法应当产生结局", cautious != null)
        assertTrue("稳健型玩法应当产生结局", steady != null)
        assertTrue("果断型玩法应当产生结局", bold != null)
        assertNotEquals("不同的选择风格不应产生完全相同的结局", cautious, steady)
    }

    @Test
    fun eventChainsTriggerDuringLongCareers() {
        val chainEventIds = setOf(
            "road_funding", "road_check", "road_finish",
            "project_bid", "project_adjust", "project_result",
            "rural_industry", "rural_dividend",
            "petition_followup", "integrity_talk", "family_repair"
        )
        var triggered = emptySet<String>()
        for (seed in 100L..145L) {
            val state = playCareer(seed) { 0 }
            triggered = state.triggeredEvents.intersect(chainEventIds)
            if (triggered.isNotEmpty()) break
        }
        assertTrue("长时间游玩没有触发任何事件链节点", triggered.isNotEmpty())
    }

    @Test
    fun differentChoicesProduceDifferentFutures() {
        val bold = playCareer(7L) { 0 }
        val cautious = playCareer(7L) { 2 }
        assertNotEquals(
            "不同选择不应产生完全相同的结局",
            GameCodec.encode(bold),
            GameCodec.encode(cautious)
        )
    }

    @Test
    fun seedReproducesTheSameCareer() {
        val first = playCareer(555L) { it % 3 }
        val second = playCareer(555L) { it % 3 }
        assertEquals(
            "相同种子与相同选择应当完全复现",
            GameCodec.encode(first),
            GameCodec.encode(second)
        )
    }
}
