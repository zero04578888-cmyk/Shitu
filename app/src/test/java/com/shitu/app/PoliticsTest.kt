package com.shitu.app

import com.shitu.app.game.AllianceCatalog
import com.shitu.app.game.CrisisCatalog
import com.shitu.app.game.EventEngine
import com.shitu.app.game.EventLibrary
import com.shitu.app.game.FactionCatalog
import com.shitu.app.game.FactionEventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.IntelCatalog
import com.shitu.app.game.PoliticalDecisionLibrary
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.model.Faction
import com.shitu.app.model.IntelLevel
import com.shitu.app.model.PoliticalImpact
import com.shitu.app.model.PoliticalTendency
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 第九阶段：派系、政治资源、情报、联盟、危机、长期后果与存档。
class PoliticsTest {

    private fun newGame(seed: Long = 77L): GameState =
        GameEngine.newGame("政治测试", "男", "硕士", "机关科员", seed)

    private fun GameState.playing(eventId: String): GameState =
        copy(currentEventId = eventId, phase = GamePhase.EVENT, outcome = null)

    @Test
    fun fourMajorFactionsHaveCatalogInfo() {
        val major = listOf(Faction.LOCAL, Faction.PRAGMATIC, Faction.REFORM, Faction.CONSERVATIVE)
        assertTrue(major.size >= 4)
        assertEquals(Faction.entries.size, FactionCatalog.infos.size)
        major.forEach { faction ->
            val info = FactionCatalog.infoOf(faction)
            assertTrue(info.ideology.isNotEmpty())
            assertTrue(info.focus.isNotEmpty())
            assertTrue(info.goal.isNotEmpty())
        }
        // 五个核心领导人物各自代表一个派系。
        val leaders = FactionCatalog.infos.mapNotNull { it.coreNpcId }
        assertTrue(leaders.size >= 5)
    }

    @Test
    fun relationshipSupportOnlyAddsAndStaysBounded() {
        val state = newGame()
        val support = PoliticsEngine.relationshipSupport(state)
        assertTrue("关系助力不应为负：$support", support >= 0)
        assertTrue("关系助力不应过高：$support", support <= PoliticsEngine.MAX_RELATIONSHIP_SUPPORT)
    }

    @Test
    fun politicalCapitalIsClamped() {
        val state = newGame()
        assertTrue(state.politics.capital in 0..100)
        assertEquals(100, state.politics.withCapital(150).capital)
        assertEquals(0, state.politics.withCapital(-30).capital)
        assertTrue(PoliticsEngine.capitalGain(state) in 0..4)
    }

    @Test
    fun majorChoiceAppliesPoliticsHistoryAndFlag() {
        var state = newGame().playing("maj_reform_pilot")
        val before = state.politics.capital
        state = GameEngine.resolveChoice(state, 0)
        assertEquals(before + 6, state.politics.capital)
        assertTrue(state.politics.tendencyOf(PoliticalTendency.REFORMIST) >= 3)
        assertTrue(state.politics.majorChoices.contains("maj_reform_pilot"))
        assertTrue(state.flags.contains("major_maj_reform_pilot"))
        assertEquals(1, state.politics.history.size)
        assertEquals("一项有争议的试点", state.politics.history.first().title)
    }

    @Test
    fun intelIsRecoveredFromCatalog() {
        var state = newGame().playing("fac_intel_gossip")
        state = GameEngine.resolveChoice(state, 0)
        assertTrue(state.politics.intel.any { it.id == "intel_vacancy" })
        val item = state.politics.intel.first { it.id == "intel_vacancy" }
        assertEquals(IntelLevel.KEY, item.level)
        assertTrue(item.detail.isNotEmpty())
    }

    @Test
    fun allianceStartsAndExpiresOnSchedule() {
        val state = newGame()
        val withAlly = state.copy(
            politics = PoliticsEngine.applyChoice(state, PoliticalImpact(startAlliance = "ally_pragmatic"))
        )
        assertEquals(1, withAlly.politics.alliances.size)
        assertTrue(withAlly.politics.activeAlliances(state.absoluteQuarter).isNotEmpty())
        // 联盟持续 8 个季度，进入第 5 年后应当到期。
        val later = withAlly.copy(year = 5)
        val (ticked, notes) = PoliticsEngine.applyYearTick(later, GameRng(later.rngState))
        assertTrue(ticked.alliances.isEmpty())
        assertTrue(notes.any { it.contains("到期") })
    }

    @Test
    fun politicsNeverExposesRankingsOrRivals() {
        val state = newGame()
        // 关系只提供正向助力，且被限定在固定上限内，不会形成排名。
        assertTrue(PoliticsEngine.relationshipSupport(state) in 0..PoliticsEngine.MAX_RELATIONSHIP_SUPPORT)
        assertTrue(PoliticsEngine.reputation(state) in 0..100)
        assertTrue(PoliticsEngine.capitalGain(state) in 0..4)
    }

    @Test
    fun crisisStartsAndResolvesThroughImpact() {
        val state = newGame()
        val started = PoliticsEngine.applyChoice(state, PoliticalImpact(startCrisis = "crisis_opinion"))
        assertNotNull(started.crisis)
        assertEquals("crisis_opinion", started.crisis?.id)
        val resolved = PoliticsEngine.applyChoice(
            state.copy(politics = started),
            PoliticalImpact(resolveCrisis = true)
        )
        assertNull(resolved.crisis)
    }

    @Test
    fun crisisEventOnlyAppearsWhileCrisisActive() {
        val calm = newGame()
        assertFalse(EventEngine.eligibleEvents(calm).any { it.crisisOnly })
        val crisis = calm.copy(
            politics = PoliticsEngine.applyChoice(calm, PoliticalImpact(startCrisis = "crisis_opinion"))
        )
        assertTrue(EventEngine.eligibleEvents(crisis).any { it.crisisOnly })
    }

    @Test
    fun reputationStaysWithinRange() {
        val state = newGame()
        val reputation = PoliticsEngine.reputation(state)
        assertTrue(reputation in 0..100)
        assertTrue(PoliticsEngine.reputationLabel(reputation).isNotEmpty())
    }

    @Test
    fun stageNineLibrariesHaveEnoughEvents() {
        assertTrue(PoliticalDecisionLibrary.events.size >= 10)
        assertTrue(FactionEventLibrary.events.size >= 10)
        assertTrue(PoliticalDecisionLibrary.events.all { it.major })
        // 事件池里确实包含这些事件。
        val ids = EventLibrary.events.map { it.id }.toSet()
        assertTrue(ids.containsAll(PoliticalDecisionLibrary.events.map { it.id }))
        assertTrue(ids.containsAll(FactionEventLibrary.events.map { it.id }))
    }

    @Test
    fun pastMajorChoiceChangesLaterOutcome() {
        // 没有做过试点：同一选项不会出现额外结果。
        val plain = GameEngine.resolveChoice(newGame().playing("fac_reform_aftermath"), 0)
        assertFalse(plain.outcome!!.outcome.contains("更有分量"))

        // 做过试点：履历留下 flag，后续事件产生额外结果。
        var veteran = newGame().playing("maj_reform_pilot")
        veteran = GameEngine.resolveChoice(veteran, 0)
        veteran = veteran.copy(year = 3).playing("fac_reform_aftermath")
        val boosted = GameEngine.resolveChoice(veteran, 0)
        assertTrue(boosted.outcome!!.outcome.contains("更有分量"))
    }

    @Test
    fun politicsRoundTripsThroughSaveCodec() {
        var state = newGame()
        state = GameEngine.resolveChoice(state.playing("maj_reform_pilot"), 0)
        state = state.copy(
            politics = state.politics
                .withIntel(IntelCatalog.find("intel_inspection")!!)
                .withAlliance(AllianceCatalog.toAlliance(AllianceCatalog.find("ally_local")!!, state.absoluteQuarter))
                .withCapital(55)
        )
        state = state.copy(
            politics = state.politics.copy(crisis = CrisisCatalog.find("crisis_doubt"))
        )

        val text = GameCodec.encode(state)
        val restored = GameCodec.decode(text)
        assertNotNull("政治状态无法还原", restored)
        val other = restored!!

        assertEquals(state.politics.capital, other.politics.capital)
        PoliticalTendency.entries.forEach { tendency ->
            assertEquals(
                "路线 ${tendency.name} 未保留",
                state.politics.tendencyOf(tendency),
                other.politics.tendencyOf(tendency)
            )
        }
        Faction.entries.forEach { faction ->
            assertEquals(
                "派系 ${faction.name} 影响力未保留",
                state.politics.influenceOf(faction),
                other.politics.influenceOf(faction)
            )
        }
        assertEquals(state.politics.intel, other.politics.intel)
        assertEquals(state.politics.alliances, other.politics.alliances)
        assertEquals(state.politics.history, other.politics.history)
        assertEquals(state.politics.majorChoices, other.politics.majorChoices)
        assertEquals(state.politics.crisis, other.politics.crisis)
        assertEquals("重复编码应当完全一致", text, GameCodec.encode(other))
    }
}
