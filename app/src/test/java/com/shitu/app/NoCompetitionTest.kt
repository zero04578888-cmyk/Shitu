package com.shitu.app

import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.NpcCatalog
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.WorldSimulationEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.EventTag
import com.shitu.app.model.Faction
import com.shitu.app.model.Indicator
import com.shitu.app.model.LifeStage
import com.shitu.app.model.MeritKind
import com.shitu.app.model.NpcTier
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 删除「同级竞争」机制后的回归测试。
 *
 * 目标：晋升只取决于玩家自己的硬条件，任何 NPC、关系或所谓「竞争对手」
 * 都不能阻止玩家晋升；关系只作为额外帮助存在。
 */
class NoCompetitionTest {

    private fun newGame(seed: Long = 31L): GameState =
        GameEngine.newGame("晋升测试", "男", "本科", "机关科员", seed)

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

    /** 一个硬条件全部达标的玩家：只差组织程序，不该再被任何人卡住。 */
    private fun qualified(): GameState {
        val base = newGame().withGovernance(merit = 70, approval = 70)
        return base.copy(
            player = base.player.copy(
                positionIndex = 2,
                yearsInPosition = 4,
                attributes = base.player.attributes
                    .withValue(Attr.WORK, 95)
                    .withValue(Attr.ABILITY, 92)
                    .withValue(Attr.POLITICS, 92)
                    .withValue(Attr.LEADER_TRUST, 95)
                    .withValue(Attr.MASSES, 92)
                    .withValue(Attr.REPUTATION, 80)
                    .withValue(Attr.COLLEAGUES, 80)
                    .withValue(Attr.INTEGRITY_RISK, 5)
            )
        )
    }

    private fun GameState.withRelationship(next: com.shitu.app.model.RelationshipState): GameState =
        copy(relationship = next)

    @Test
    fun qualifiedPlayerPromotesWithoutAnyCompetition() {
        val state = qualified()
        val result = PromotionEngine.evaluate(state, 90, GameRng(1L))
        assertTrue("条件达标即应晋升：${result.text}", result.granted)
        assertTrue("晋升说明应提到任职资格：${result.text}", result.text.contains("任职资格"))
    }

    @Test
    fun manyNpcsDoNotBlockPromotion() {
        val state = qualified()
        // 游戏里存在大量 NPC；他们不会参与玩家的晋升判定。
        assertTrue("NPC 数量应当可观：${NpcCatalog.npcs.size}", NpcCatalog.npcs.size >= 20)
        assertTrue(NpcCatalog.npcs.any { it.tier == NpcTier.COLLEAGUE })
        val result = PromotionEngine.evaluate(state, 90, GameRng(2L))
        assertTrue("有 NPC 存在时依然应当正常晋升：${result.text}", result.granted)
    }

    @Test
    fun lowRelationsDoNotBlockBasicPromotion() {
        var rel = qualified().relationship
        Faction.entries.forEach { faction -> rel = rel.withFaction(faction, -40) }
        rel = rel.withRelation("chen_jianguo", -60).withTrust("chen_jianguo", 0)
        val cold = qualified().withRelationship(rel)

        // 关系很差时，助力为 0，但不构成任何门槛。
        assertEquals(0, PoliticsEngine.relationshipSupport(cold))
        val result = PromotionEngine.evaluate(cold, 90, GameRng(3L))
        assertTrue("关系低不应影响基础晋升：${result.text}", result.granted)
    }

    @Test
    fun highRelationsAddHelpButAreNotRequired() {
        var warm = qualified().relationship
        warm = warm.withFaction(Faction.CORE, 100)
            .withRelation("chen_jianguo", 100)
            .withTrust("chen_jianguo", 100)
        val connected = qualified().withRelationship(warm)

        val support = PoliticsEngine.relationshipSupport(connected)
        assertTrue("关系好应当带来额外助力：$support", support > 0)
        assertTrue("助力应当有上限：$support", support <= PoliticsEngine.MAX_RELATIONSHIP_SUPPORT)

        val result = PromotionEngine.evaluate(connected, 90, GameRng(4L))
        assertTrue("关系好同样正常晋升：${result.text}", result.granted)
        assertTrue("关系好时应当有额外帮助的反馈：${result.text}", result.text.contains("公道话"))
    }

    @Test
    fun positionSystemWorksWithoutAnyCompetitorData() {
        val state = qualified()
        // 世界模拟照常运转，岗位机会照常给出，不需要任何「竞争对手」数据。
        val world = WorldSimulationEngine.ensureWorld(state)
        assertNotNull(world.world)
        assertTrue(WorldSimulationEngine.promotionOpportunityBonus(world) >= 0)

        val status = PromotionEngine.statusOf(state, 90)
        assertTrue(status.ready)
        assertTrue(status.blockers.isEmpty())
        assertEquals("副科级", status.nextName)
    }

    @Test
    fun careerSurvivesSaveAndReloadAfterRemovingCompetition() {
        val state = qualified()
        val text = GameCodec.encode(state)
        val restored = GameCodec.decode(text)
        assertNotNull("存档无法还原", restored)
        val other = restored!!
        assertEquals(state.player.positionIndex, other.player.positionIndex)
        assertEquals(state.player.yearsInPosition, other.player.yearsInPosition)
        assertEquals(state.player.attributes.work, other.player.attributes.work)
        assertEquals(state.governance.merit.overall, other.governance.merit.overall)
        assertEquals(text, GameCodec.encode(other))
        assertTrue(PromotionEngine.evaluate(other, 90, GameRng(5L)).granted)
    }

    @Test
    fun legacySaveWithCompetitionFieldsStillLoads() {
        val state = qualified()
        // 旧存档里可能残留 pol.rival.* / pol.comp.* 字段，读取时应当直接忽略。
        val legacy = GameCodec.encode(state) +
            "pol.rival.count\t2\n" +
            "pol.rival.0.id\tsome_npc\n" +
            "pol.comp.present\t1\n" +
            "pol.comp.position\t副科级\n"
        val restored = GameCodec.decode(legacy)
        assertNotNull("含旧竞争字段的存档仍应可读", restored)
        assertEquals(state.player.positionIndex, restored!!.player.positionIndex)
        assertEquals(state.player.positionTitle, restored.player.positionTitle)
    }

    @Test
    fun relationshipSupportStaysBoundedForExtremeValues() {
        var rel = qualified().relationship
        Faction.entries.forEach { faction -> rel = rel.withFaction(faction, 100) }
        NpcCatalog.npcs.forEach { npc ->
            rel = rel.withRelation(npc.id, 100).withTrust(npc.id, 100)
        }
        val maximal = qualified().withRelationship(rel)
        val support = PoliticsEngine.relationshipSupport(maximal)
        assertTrue("极端关系下助力仍应受控：$support", support in 0..PoliticsEngine.MAX_RELATIONSHIP_SUPPORT)
    }

    @Test
    fun eventCatalogNoLongerUsesCompetitionVocabulary() {
        assertFalse(
            "事件库里不应再有竞争标签",
            EventTag.entries.any { it.name == "COMPETITION" }
        )
        assertFalse(
            "人生阶段里不应再有竞争语义",
            LifeStage.entries.any { it.name.contains("COMPETITION") }
        )
        assertTrue(
            "事件标题里不应出现职位竞争：${EventLibrary.events.map { it.title }.filter { it.contains("竞争") }}",
            EventLibrary.events.none { it.title.contains("竞争") || it.description.contains("竞争对手") }
        )
    }
}