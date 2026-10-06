package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// 存档编解码：完整往返、损坏数据、越界数据。
class GameCodecTest {

    private fun playedState(steps: Int = 30, seed: Long = 99L): GameState {
        var state = GameEngine.newGame("张三", "男", "硕士", "机关科员", seed)
        repeat(steps) { step ->
            when (state.phase) {
                GamePhase.EVENT -> state = GameEngine.resolveChoice(state, step % 3)
                GamePhase.OUTCOME -> state = GameEngine.advance(state)
                GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> return state
            }
        }
        return state
    }

    @Test
    fun roundTripPreservesWholeState() {
        val state = playedState()
        val text = GameCodec.encode(state)
        val restored = GameCodec.decode(text)
        assertNotNull("存档无法还原", restored)
        val other = restored!!

        assertEquals(state.player.name, other.player.name)
        assertEquals(state.player.gender, other.player.gender)
        assertEquals(state.player.education, other.player.education)
        assertEquals(state.player.unit, other.player.unit)
        assertEquals(state.player.positionIndex, other.player.positionIndex)
        assertEquals(state.player.attributes, other.player.attributes)
        assertEquals(state.player.hidden, other.player.hidden)
        assertEquals(state.player.family, other.player.family)
        assertEquals(state.player.serviceYears, other.player.serviceYears)
        assertEquals(state.year, other.year)
        assertEquals(state.quarter, other.quarter)
        assertEquals(state.reviewCount, other.reviewCount)
        assertEquals(state.rngState, other.rngState)
        assertEquals(state.reachedTopYear, other.reachedTopYear)
        assertEquals(state.totalWorkGained, other.totalWorkGained)
        assertEquals(state.log, other.log)
        assertEquals(state.reviews, other.reviews)
        assertEquals(state.flags, other.flags)
        assertEquals(state.pendingChains, other.pendingChains)
        assertEquals(state.eventCooldown, other.eventCooldown)
        assertEquals(state.triggeredEvents, other.triggeredEvents)
        assertEquals("重复编码应当完全一致", text, GameCodec.encode(other))
    }

    @Test
    fun finishedGameKeepsEnding() {
        var state = playedState(steps = 400, seed = 31L)
        var guard = 0
        while (!state.isFinished && guard < 3000) {
            guard += 1
            when (state.phase) {
                GamePhase.EVENT -> state = GameEngine.resolveChoice(state, guard % 3)
                GamePhase.OUTCOME -> state = GameEngine.advance(state)
                GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> break
            }
        }
        assertNotNull(state.ending)
        val restored = GameCodec.decode(GameCodec.encode(state))
        assertNotNull(restored)
        assertEquals(state.ending!!.id, restored!!.ending?.id)
        assertEquals(state.ending!!.title, restored.ending?.title)
        assertEquals(state.ending!!.highlights, restored.ending?.highlights)
        assertEquals(GamePhase.ENDING, restored.phase)
    }

    @Test
    fun corruptedSaveDataIsRejected() {
        assertNull(GameCodec.decode(""))
        assertNull(GameCodec.decode("完全不是存档内容"))
        assertNull(GameCodec.decode("version\tnot-a-number\n"))
        assertNull(GameCodec.decode("version\t1\nplayer.name\t张三\n"))
        val text = GameCodec.encode(playedState())
        assertNull("被截断的存档应当无法读取", GameCodec.decode(text.substring(0, text.length / 2)))
    }

    @Test
    fun outOfRangeValuesAreClampedOnLoad() {
        val state = playedState()
        val tampered = state.copy(
            player = state.player.copy(
                attributes = state.player.attributes.copy(work = 999, health = -50, integrityRisk = 400)
            )
        )
        val restored = GameCodec.decode(GameCodec.encode(tampered))
        assertNotNull(restored)
        assertEquals(100, restored!!.player.attributes.work)
        assertEquals(0, restored.player.attributes.health)
        assertEquals(100, restored.player.attributes.integrityRisk)
    }
}
