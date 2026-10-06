package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameSave
import com.shitu.app.game.GameState
import com.shitu.app.util.GameCodec
import com.shitu.app.util.LoadResult
import com.shitu.app.util.SaveCodec
import com.shitu.app.util.SaveIntegrity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 第十五阶段：完整存档与进度系统测试。
 * 覆盖 GameSave 信封编解码、元信息、旧存档兼容与损坏保护。
 */
class PhaseFifteenTest {

    private fun game(seed: Long = 15L): GameState =
        GameEngine.newGame("存档", "男", "本科", "乡镇公务员", seed)

    private fun played(steps: Int = 24, seed: Long = 5L): GameState {
        var state = game(seed)
        repeat(steps) { step ->
            state = when (state.phase) {
                GamePhase.EVENT -> GameEngine.resolveChoice(state, step % 3)
                GamePhase.OUTCOME -> GameEngine.advance(state)
                GamePhase.REVIEW -> GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> return state
            }
        }
        return state
    }

    @Test
    fun roundTripKeepsStateAndMetadata() {
        val save = GameSave(startedAt = 1_700_000_000_000L, savedAt = 1_700_000_999_000L, state = played())
        val decoded = SaveCodec.decode(SaveCodec.encode(save))
        assertNotNull(decoded)
        assertEquals(save.startedAt, decoded!!.startedAt)
        assertEquals(save.savedAt, decoded.savedAt)
        assertEquals(save.version, decoded.version)
        assertEquals(save.state.year, decoded.state.year)
        assertEquals(save.state.quarter, decoded.state.quarter)
        assertEquals(save.state.player.name, decoded.state.player.name)
        assertEquals(save.state.player.positionIndex, decoded.state.player.positionIndex)
        assertEquals(save.state.triggeredEvents, decoded.state.triggeredEvents)
        assertEquals(save.state.log.size, decoded.state.log.size)
    }

    @Test
    fun roundTripPreservesDecisionsAndRoute() {
        val state = played(steps = 60)
        val decoded = SaveCodec.decode(SaveCodec.encode(GameSave(state = state)))
        assertNotNull(decoded)
        assertEquals(state.phase, decoded!!.state.phase)
        assertEquals(state.route, decoded.state.route)
        assertEquals(state.promotionCount, decoded.state.promotionCount)
        assertEquals(state.currentEventId, decoded.state.currentEventId)
        assertEquals(state.pendingChains.map { it.chainId }, decoded.state.pendingChains.map { it.chainId })
    }

    @Test
    fun envelopeCarriesGameCodecFormatVersion() {
        val text = SaveCodec.encode(GameSave(state = game()))
        assertTrue(text.startsWith("#SHITU_SAVE"))
        // SaveIntegrity 仍能从信封正文里读出 GameCodec 的格式版本。
        assertEquals(GameCodec.FORMAT_VERSION, SaveIntegrity.versionOf(text))
    }

    @Test
    fun legacyTextWithoutEnvelopeStillDecodes() {
        val legacy = GameCodec.encode(game())
        val decoded = SaveCodec.decode(legacy)
        assertNotNull(decoded)
        assertEquals(0L, decoded!!.startedAt)
        assertEquals(0L, decoded.savedAt)
        assertEquals(GameSave.DEFAULT_VERSION, decoded.version)
        assertEquals(GameCodec.FORMAT_VERSION, SaveIntegrity.versionOf(legacy))
    }

    @Test
    fun legacyBackupLikeTextKeepsWorking() {
        val legacy = GameCodec.encode(played(steps = 12))
        val decoded = SaveCodec.decode(legacy)
        assertNotNull(decoded)
        assertEquals(SaveCodec.decode(legacy)!!.state.year, decoded!!.state.year)
    }

    @Test
    fun garbageReturnsNull() {
        assertNull(SaveCodec.decode("这不是存档"))
        assertNull(SaveCodec.decode(""))
        assertNull(SaveCodec.decode("#SHITU_SAVE\nenvelope\t1\nstartedAt\t1\nsavedAt\t2\n乱码"))
    }

    @Test
    fun unknownEnvelopeVersionStillDecodes() {
        val body = GameCodec.encode(game())
        val text = "#SHITU_SAVE\nenvelope\t99\nstartedAt\t5\nsavedAt\t6\n" + body
        val decoded = SaveCodec.decode(text)
        assertNotNull(decoded)
        assertEquals(99, decoded!!.version)
        assertEquals(5L, decoded.startedAt)
        assertEquals(6L, decoded.savedAt)
    }

    @Test
    fun malformedMetadataFallsBackToDefaults() {
        val body = GameCodec.encode(game())
        val text = "#SHITU_SAVE\nenvelope\tabc\nstartedAt\txyz\nsavedAt\t\n" + body
        val decoded = SaveCodec.decode(text)
        assertNotNull(decoded)
        assertEquals(SaveCodec.ENVELOPE_VERSION, decoded!!.version)
        assertEquals(0L, decoded.startedAt)
        assertEquals(0L, decoded.savedAt)
    }

    @Test
    fun derivedFieldsMatchState() {
        val state = played(steps = 30)
        val save = GameSave(state = state)
        assertEquals(state.player.name, save.playerName)
        assertEquals(state.player.positionName, save.positionName)
        assertEquals(state.player.positionIndex, save.positionLevel)
        assertEquals(state.player.unit.label, save.unitLabel)
        assertEquals(state.governance.jurisdiction.name, save.regionName)
        assertEquals(state.governance.merit.overall, save.merit)
        assertEquals(state.governance.jurisdiction.approval, save.approval)
        assertEquals(state.player.attributes.wealth, save.wealth)
        assertEquals(state.player.attributes.health, save.health)
        assertEquals(state.player.attributes.energy, save.energy)
        assertEquals(state.governance.finance, save.finance)
        assertEquals(state.triggeredEvents, save.triggeredEvents)
        assertEquals(state.log.size, save.completedEventCount)
        assertEquals(state.log.size, save.decisionCount)
        assertEquals(state.promotionCount, save.mainLineProgress)
        assertEquals(state.pendingChains.size, save.pendingChainCount)
        assertEquals(state.route?.label ?: "未确定", save.routeLabel)
        assertEquals(state.career.unlockedAchievements, save.unlockedAchievements)
    }

    @Test
    fun quarterMapsToMonth() {
        for (quarter in 1..4) {
            val save = GameSave(state = game().copy(quarter = quarter))
            assertEquals((quarter - 1) * 3 + 1, save.month)
        }
    }

    @Test
    fun freshGameIsNotFinished() {
        val save = GameSave(state = game())
        assertFalse(save.isFinished)
        assertNull(save.endingTitle)
    }

    @Test
    fun summaryLineMentionsNameAndYear() {
        val state = game()
        val line = GameSave(state = state).summaryLine()
        assertTrue(line.contains(state.player.name))
        assertTrue(line.contains("第 ${state.year} 年"))
    }

    @Test
    fun saveCodecIsDeterministic() {
        val save = GameSave(startedAt = 10L, savedAt = 20L, state = played(steps = 8))
        assertEquals(SaveCodec.encode(save), SaveCodec.encode(save))
    }

    @Test
    fun loadResultSuccessExposesState() {
        val state = game()
        val result: LoadResult = LoadResult.Success(GameSave(startedAt = 1L, savedAt = 2L, state = state))
        assertTrue(result is LoadResult.Success)
        assertEquals(state.year, (result as LoadResult.Success).state.year)
        assertEquals(1L, result.save.startedAt)
        assertEquals(2L, result.save.savedAt)
        assertFalse(result.recovered)
    }

    @Test
    fun checksumCoversEnvelopeMetadata() {
        val a = SaveCodec.encode(GameSave(startedAt = 1L, savedAt = 1L, state = game()))
        val b = SaveCodec.encode(GameSave(startedAt = 1L, savedAt = 2L, state = game()))
        assertTrue(SaveIntegrity.checksum(a) != SaveIntegrity.checksum(b))
        assertTrue(SaveIntegrity.verify(a, SaveIntegrity.checksum(a)))
        assertFalse(SaveIntegrity.verify(a, SaveIntegrity.checksum(b)))
    }
}