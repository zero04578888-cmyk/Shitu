package com.shitu.app

import com.shitu.app.game.AnnualReviewEngine
import com.shitu.app.game.CareerEngine
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.game.ReviewExplainer
import com.shitu.app.game.SimulationTestEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.EndingCatalog
import com.shitu.app.model.MeritKind
import com.shitu.app.util.GameCodec
import com.shitu.app.util.SaveIntegrity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 第十四阶段：V1.0 发布候选版的稳定性测试。
 * 覆盖存档完整性、旧存档兼容、时间推进、数值保护、成就幂等与长期模拟。
 */
class PhaseFourteenTest {

    private fun game(seed: Long = 14L, priorLives: Int = 0): GameState =
        GameEngine.newGame("稳定", "男", "本科", "乡镇公务员", seed, priorLives = priorLives)

    private fun played(steps: Int = 40, seed: Long = 7L): GameState {
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

    private fun assertAttributesInRange(state: GameState) {
        Attr.entries.forEach { attr ->
            val value = state.player.attributes.valueOf(attr)
            assertTrue("属性 ${attr.name}=$value 越界", value in 0..100)
        }
        state.player.hidden.let { hidden ->
            hidden.entriesAll().forEach { (label, value) ->
                assertTrue("隐藏值 $label=$value 越界", value in 0..100)
            }
        }
    }

    private fun com.shitu.app.model.HiddenStats.entriesAll(): List<Pair<String, Int>> =
        listOf(
            "机遇" to opportunity,
            "贵人" to mentor,
            "风险" to riskPressure,
            "社会影响" to socialInfluence,
            "压力" to stress,
            "健康隐患" to healthRisk,
            "网络" to network
        )

    // ===== 存档完整性 =====

    @Test
    fun checksumIsStableForSameText() {
        val text = "version\t5\nplayer.name\t张三\n"
        assertEquals(SaveIntegrity.checksum(text), SaveIntegrity.checksum(text))
    }

    @Test
    fun checksumChangesWhenTextChanges() {
        val a = SaveIntegrity.checksum("version\t5\nname\t甲")
        val b = SaveIntegrity.checksum("version\t5\nname\t乙")
        assertFalse("不同内容不应得到相同校验和", a == b)
    }

    @Test
    fun verifyAcceptsLegacySaveWithoutChecksum() {
        assertTrue(SaveIntegrity.verify("version\t5\n", null))
        assertTrue(SaveIntegrity.verify("version\t5\n", ""))
    }

    @Test
    fun verifyRejectsMismatchedChecksum() {
        val text = "version\t5\n"
        assertFalse(SaveIntegrity.verify(text, "deadbeef"))
        assertTrue(SaveIntegrity.verify(text, SaveIntegrity.checksum(text)))
    }

    @Test
    fun versionReadsGameCodecHeader() {
        val text = GameCodec.encode(game())
        assertEquals(GameCodec.FORMAT_VERSION, SaveIntegrity.versionOf(text))
    }

    @Test
    fun versionIsZeroForGarbage() {
        assertEquals(0, SaveIntegrity.versionOf("这不是存档"))
        assertEquals(0, SaveIntegrity.versionOf(""))
    }

    @Test
    fun decidePrefersTrustworthyPrimary() {
        val primary = "a"
        assertEquals(
            SaveIntegrity.Decision.PRIMARY,
            SaveIntegrity.decide(primary, SaveIntegrity.checksum(primary), "b", SaveIntegrity.checksum("b"))
        )
    }

    @Test
    fun decideFallsBackToBackupWhenPrimaryCorrupt() {
        val backup = "b"
        assertEquals(
            SaveIntegrity.Decision.BACKUP,
            SaveIntegrity.decide("a", "bad", backup, SaveIntegrity.checksum(backup))
        )
    }

    @Test
    fun decideReportsCorruptWhenBothUnusable() {
        assertEquals(SaveIntegrity.Decision.CORRUPT, SaveIntegrity.decide("a", "bad", "b", "bad"))
    }

    @Test
    fun decideReportsNoneWhenNothingStored() {
        assertEquals(SaveIntegrity.Decision.NONE, SaveIntegrity.decide(null, null, null, null))
    }

    @Test
    fun legacySaveWithoutChecksumIsTreatedAsTrustworthy() {
        // 旧版本存档没有校验和：主存档应当直接可用，而不是被判为损坏。
        assertEquals(
            SaveIntegrity.Decision.PRIMARY,
            SaveIntegrity.decide("version\t5\n", null, null, null)
        )
    }

    // ===== 存档兼容 =====

    @Test
    fun decodeRejectsFutureFormatVersion() {
        val text = GameCodec.encode(game()).replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t99")
        assertNull("来自更高版本的存档应当被拒绝而不是崩溃", GameCodec.decode(text))
    }

    @Test
    fun decodeSurvivesTruncatedText() {
        val text = GameCodec.encode(game())
        val truncated = text.substring(0, text.length / 2)
        val result = runCatching { GameCodec.decode(truncated) }
        assertTrue("截断的存档不应抛异常", result.isSuccess)
    }

    @Test
    fun savedResumeKeepsRegionName() {
        val state = played()
        val decoded = GameCodec.decode(GameCodec.encode(state))
        assertNotNull(decoded)
        assertEquals(
            state.career.records.map { it.regionName },
            decoded!!.career.records.map { it.regionName }
        )
    }

    @Test
    fun legacyResumeWithoutRegionStillDecodes() {
        val state = played()
        val stripped = GameCodec.encode(state)
            .lineSequence()
            .filterNot { it.startsWith("career.record.") && it.contains(".region\t") }
            .joinToString("\n")
        val decoded = GameCodec.decode(stripped)
        assertNotNull("缺少新增字段的旧存档仍应能读取", decoded)
        assertTrue(decoded!!.career.records.all { it.regionName.isEmpty() })
    }

    // ===== 时间推进 =====

    @Test
    fun advanceDoesNothingOutsideOutcomePhase() {
        val state = game()
        assertEquals(GamePhase.EVENT, state.phase)
        assertEquals(state, GameEngine.advance(state))
    }

    @Test
    fun resolvingOutsideEventPhaseIsIgnored() {
        val afterChoice = GameEngine.resolveChoice(game(), 0)
        assertEquals(GamePhase.OUTCOME, afterChoice.phase)
        // 同一选择重复点击不会再次结算。
        assertEquals(afterChoice, GameEngine.resolveChoice(afterChoice, 0))
    }

    @Test
    fun advanceMovesExactlyOneQuarter() {
        val settled = GameEngine.resolveChoice(game(), 0)
        val next = GameEngine.advance(settled)
        assertEquals(1, next.year)
        assertEquals(2, next.quarter)
    }

    @Test
    fun fullYearProducesOneReviewAndAdvancesYear() {
        var state = game()
        var guard = 0
        while (state.year == 1 && guard < 400) {
            guard += 1
            state = when (state.phase) {
                GamePhase.EVENT -> GameEngine.resolveChoice(state, 0)
                GamePhase.OUTCOME -> GameEngine.advance(state)
                GamePhase.REVIEW -> GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> break
            }
        }
        assertTrue("一年之后应当进入第 2 年，实际 ${state.year}", state.year >= 2)
        assertTrue("一年应当至少完成一次年度考核", state.reviewCount >= 1)
    }

    @Test
    fun proceedFromReviewRequiresReviewPhase() {
        val state = game()
        assertEquals(state, GameEngine.proceedFromReview(state))
    }

    // ===== 数值保护 =====

    @Test
    fun attributesClampToRange() {
        val attrs = game().player.attributes
        assertEquals(100, attrs.withValue(Attr.WORK, 9999).work)
        assertEquals(0, attrs.withValue(Attr.WORK, -9999).work)
        assertEquals(50, attrs.withValue(Attr.WORK, 50).work)
    }

    @Test
    fun hiddenStatsClampToRange() {
        val hidden = game().player.hidden
        assertEquals(100, hidden.withValue(com.shitu.app.model.HiddenKey.STRESS, 10_000).stress)
        assertEquals(0, hidden.withValue(com.shitu.app.model.HiddenKey.STRESS, -10_000).stress)
    }

    @Test
    fun meritAndFinanceNeverGoNegative() {
        val state = game()
        assertEquals(0, state.governance.merit.withValue(MeritKind.ECONOMIC, -50).economic)
        assertEquals(0, state.governance.withFinance(-99_999).finance)
    }

    // ===== 成就幂等 =====

    @Test
    fun achievementsUnlockOnceAndDoNotDuplicateRecords() {
        val base = game().copy(promotionCount = 1)
        val first = CareerEngine.checkAchievements(base)
        assertTrue(first.career.unlockedAchievements.contains("first_promotion"))
        val second = CareerEngine.checkAchievements(first)
        assertEquals(first.career.unlockedAchievements, second.career.unlockedAchievements)
        assertEquals(first.career.records.size, second.career.records.size)
    }

    @Test
    fun repeatedAchievementCheckFromScratchMatches() {
        val base = game().copy(promotionCount = 1)
        val a = CareerEngine.checkAchievements(base)
        val b = CareerEngine.checkAchievements(base)
        assertEquals(a.career.records.size, b.career.records.size)
        assertEquals(a.career.unlockedAchievements, b.career.unlockedAchievements)
    }

    // ===== 多周目隔离 =====

    @Test
    fun secondPlaythroughStartsCleanButKeepsPriorLives() {
        val second = game(seed = 21L, priorLives = 1)
        assertEquals(1, second.career.priorLives)
        assertTrue("新周目的成就应当从零开始", second.career.unlockedAchievements.isEmpty())
        assertTrue("新周目不应继承上一局的履历", second.career.records.size <= 1)
    }

    @Test
    fun archiveAppendIsIdempotent() {
        val state = played(seed = 5L)
        var reached = state
        var guard = 0
        while (!reached.isFinished && guard < 4000) {
            guard += 1
            reached = when (reached.phase) {
                GamePhase.EVENT -> GameEngine.resolveChoice(reached, 0)
                GamePhase.OUTCOME -> GameEngine.advance(reached)
                GamePhase.REVIEW -> GameEngine.proceedFromReview(reached)
                GamePhase.ENDING -> break
            }
        }
        if (!reached.isFinished) return
        val record = com.shitu.app.game.ArchiveFactory.lifeRecordOf(reached) ?: return
        val archive = com.shitu.app.model.LifeArchive.EMPTY.appended(record).appended(record)
        assertEquals("同一个结局不应被重复计入", 1, archive.records.size)
    }

    // ===== 年度考核可解释化 =====

    @Test
    fun reviewExplanationExplainsFreshState() {
        val state = game()
        val review = AnnualReviewEngine.conduct(state)
        val explanation = ReviewExplainer.of(state, review)
        assertTrue(explanation.contributions.isNotEmpty())
        assertTrue(explanation.promotionConditions.isNotEmpty())
        assertTrue(explanation.opportunityLine.isNotEmpty())
        assertTrue(explanation.resultLine.isNotEmpty())
    }

    @Test
    fun freshPlayerIsNotReadyForPromotion() {
        val state = game()
        val review = AnnualReviewEngine.conduct(state)
        val explanation = ReviewExplainer.of(state, review)
        assertFalse("新入职玩家不应直接满足晋升硬条件", explanation.readyForPromotion)
    }

    // ===== 结局图鉴 =====

    @Test
    fun endingCatalogMatchesDocumentedCount() {
        assertTrue("结局数量应不少于 12", EndingCatalog.total >= 12)
        assertEquals(27, EndingCatalog.total)
    }

    @Test
    fun lockedEndingsAreHidden() {
        val gallery = EndingCatalog.galleryOf(com.shitu.app.model.LifeArchive.EMPTY)
        assertTrue(gallery.all { !it.unlocked })
        assertEquals(EndingCatalog.total, gallery.size)
    }

    // ===== 长期模拟与数值保护 =====

    @Test
    fun thirtyYearSimulationStaysInRange() {
        val (state, report) = SimulationTestEngine.simulateWithReport(game(seed = 33L), 30)
        assertTrue("人口不应为负", report.minPopulation >= 0)
        assertTrue("民望应在 0..100", report.minApproval >= 0 && report.maxApproval <= 100)
        assertTrue("风险应在 0..100", report.minRisk >= 0 && report.maxRisk <= 100)
        assertTrue("派系影响力不应超过 100", report.maxFactionInfluence in 0..100)
        assertAttributesInRange(state)
        assertTrue(state.year <= 32)
    }

    @Test
    fun tenYearSimulationNeverCorruptsState() {
        val (state, _) = SimulationTestEngine.simulateWithReport(game(seed = 101L), 10)
        assertAttributesInRange(state)
        val reencoded = GameCodec.decode(GameCodec.encode(state))
        assertNotNull("长期模拟后的状态必须仍然可以存档与还原", reencoded)
    }
}