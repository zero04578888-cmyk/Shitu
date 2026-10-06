package com.shitu.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.util.LoadResult
import com.shitu.app.util.SaveManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// 真机/模拟器上的存档读写验证。
@RunWith(AndroidJUnit4::class)
class SaveManagerInstrumentedTest {

    @Test
    fun autoSaveAndManualSaveRoundTrip() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = SaveManager(context)
        manager.clearAll()

        val state = GameEngine.newGame("设备测试", "女", "本科", "街道公务员")
        manager.saveAuto(state)
        manager.saveManual(state)

        assertTrue(manager.hasAutoSave())
        assertTrue(manager.hasManualSave())

        val auto = manager.loadAuto()
        assertTrue(auto is LoadResult.Success)
        val restored = (auto as LoadResult.Success).state
        assertEquals(state.player.name, restored.player.name)
        assertEquals(state.player.attributes, restored.player.attributes)
        assertEquals(state.currentEventId, restored.currentEventId)

        val manual = manager.loadManual()
        assertTrue(manual is LoadResult.Success)
        assertEquals(
            (manual as LoadResult.Success).state.player.unit,
            state.player.unit
        )

        manager.clearAll()
        assertTrue(!manager.hasAutoSave())
        assertTrue(!manager.hasManualSave())
    }

    // 结局收集：只有真正结束的人生才会写进图鉴，重复读取不会重复计数。
    @Test
    fun endedCareerIsRecordedInArchive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = SaveManager(context)
        manager.clearAll()
        manager.clearArchive()

        val unfinished = GameEngine.newGame("未完成", "男", "本科", "机关科员")
        assertTrue("进行中的游戏不应写入图鉴", !manager.recordEnding(unfinished))
        assertTrue(manager.loadArchive().records.isEmpty())

        val finished = playToEnding(unfinished)
        assertTrue("结束的人生应当写入图鉴", manager.recordEnding(finished))
        assertEquals(1, manager.loadArchive().records.size)
        assertEquals(finished.ending?.id, manager.loadArchive().records.first().endingId)

        assertTrue("同一次人生不应重复计数", !manager.recordEnding(finished))
        assertEquals(1, manager.loadArchive().records.size)

        manager.clearArchive()
        assertTrue(manager.loadArchive().records.isEmpty())
    }

    private fun playToEnding(start: GameState): GameState {
        var state = start
        var step = 0
        while (!state.isFinished && step < 3000) {
            step += 1
            when (state.phase) {
                GamePhase.EVENT -> state = GameEngine.resolveChoice(state, step % 3)
                GamePhase.OUTCOME -> state = GameEngine.advance(state)
                GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> break
            }
        }
        return state
    }
}
