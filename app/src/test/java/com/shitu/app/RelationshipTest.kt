package com.shitu.app

import com.shitu.app.game.EventEngine
import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.Interaction
import com.shitu.app.game.NpcCatalog
import com.shitu.app.game.PoliticalEngine
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.RelationshipEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.Faction
import com.shitu.app.model.NpcAttitude
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.model.RelationshipState
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 官场关系与势力：关系计算、互动、势力连带、风险、存档。
class RelationshipTest {

    private fun newGame(seed: Long = 11L): GameState =
        GameEngine.newGame("关系测试", "男", "本科", "机关科员", seed)

    private fun GameState.rel(state: RelationshipState): GameState =
        copy(relationship = state)

    private fun GameState.energy(value: Int): GameState =
        copy(
            player = player.copy(
                attributes = player.attributes.withValue(Attr.ENERGY, value)
            )
        )

    private fun GameState.wealth(value: Int): GameState =
        copy(
            player = player.copy(
                attributes = player.attributes.withValue(Attr.WEALTH, value)
            )
        )

    private fun GameState.playing(eventId: String): GameState =
        copy(currentEventId = eventId, phase = GamePhase.EVENT, outcome = null)

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
    fun attitudeBandsFollowTheSpecifiedRanges() {
        assertEquals(NpcAttitude.HOSTILE, NpcAttitude.of(-100))
        assertEquals(NpcAttitude.HOSTILE, NpcAttitude.of(-60))
        assertEquals(NpcAttitude.COLD, NpcAttitude.of(-59))
        assertEquals(NpcAttitude.COLD, NpcAttitude.of(-20))
        assertEquals(NpcAttitude.NEUTRAL, NpcAttitude.of(-19))
        assertEquals(NpcAttitude.NEUTRAL, NpcAttitude.of(19))
        assertEquals(NpcAttitude.FRIENDLY, NpcAttitude.of(20))
        assertEquals(NpcAttitude.FRIENDLY, NpcAttitude.of(59))
        assertEquals(NpcAttitude.CLOSE, NpcAttitude.of(60))
        assertEquals(NpcAttitude.CLOSE, NpcAttitude.of(100))
    }

    @Test
    fun relationGainDiminishesAsItGrows() {
        val low = RelationshipEngine.scaledGain(10, 0)
        val mid = RelationshipEngine.scaledGain(10, 50)
        val high = RelationshipEngine.scaledGain(10, 90)
        assertTrue("关系低时收益应当更高", low > mid)
        assertTrue("关系高时收益应当更低", mid > high)
        assertTrue("正向收益至少为 1", high >= 1)
    }

    @Test
    fun supportingAFactionCostsTheRivalFaction() {
        val shifted = RelationshipEngine.shiftFactions(
            RelationshipState.EMPTY,
            mapOf(Faction.REFORM to 10)
        )
        assertEquals(10, shifted.factionOf(Faction.REFORM))
        assertTrue("改革派的对立方应当下降", shifted.factionOf(Faction.CONSERVATIVE) < 0)
        assertTrue("地方势力同样与改革派对立", shifted.factionOf(Faction.LOCAL) < 0)
        assertEquals("无关势力不受影响", 0, shifted.factionOf(Faction.CORE))
    }

    @Test
    fun visitCostsEnergyAndImprovesRelation() {
        val npc = NpcCatalog.find("chen_jianguo")!!
        val before = newGame().energy(60)
        val result = PoliticalEngine.interact(before, npc.id, Interaction.VISIT)
        assertTrue("拜访应当提高关系", result.state.relationship.relationOf(npc.id) > 0)
        assertTrue("拜访应当提高信任", result.state.relationship.trustOf(npc.id) > 25)
        assertEquals(56, result.state.player.attributes.energy)
    }

    @Test
    fun interactionIsBlockedWithoutEnoughEnergy() {
        val npc = NpcCatalog.find("chen_jianguo")!!
        val tired = newGame().energy(2)
        assertFalse(PoliticalEngine.isAvailable(tired, npc, Interaction.VISIT))
        assertEquals(
            "精力不足",
            PoliticalEngine.unavailabilityReason(tired, npc, Interaction.VISIT)
        )
        val result = PoliticalEngine.interact(tired, npc.id, Interaction.VISIT)
        assertEquals("做不了的动作不应改变状态", tired.relationship, result.state.relationship)
    }

    @Test
    fun giftRaisesRelationAndRiskAndGoesOnCooldown() {
        val npc = NpcCatalog.find("zheng_haitao")!!
        val state = newGame().energy(60).wealth(50)
        val first = PoliticalEngine.interact(state, npc.id, Interaction.GIFT)
        assertTrue(first.state.relationship.relationOf(npc.id) > 0)
        assertEquals(
            "送礼必须留下风险",
            PoliticalEngine.GIFT_RISK,
            first.state.relationship.risk
        )
        assertFalse(
            "冷却期内不能连续送礼",
            PoliticalEngine.isAvailable(first.state, npc, Interaction.GIFT)
        )
        val later = first.state.copy(year = 2, quarter = 1)
        assertTrue(
            "过了冷却期可以再送",
            PoliticalEngine.isAvailable(later, npc, Interaction.GIFT)
        )
    }

    @Test
    fun askHelpRequiresEnoughRelation() {
        val npc = NpcCatalog.find("chen_jianguo")!!
        val distant = newGame().energy(60)
        assertFalse(PoliticalEngine.isAvailable(distant, npc, Interaction.ASK_HELP))

        val close = distant.rel(distant.relationship.withRelation(npc.id, 50))
        assertTrue(PoliticalEngine.isAvailable(close, npc, Interaction.ASK_HELP))
        val result = PoliticalEngine.interact(close, npc.id, Interaction.ASK_HELP)
        assertTrue(
            "求助要动用人情，关系会略降",
            result.state.relationship.relationOf(npc.id) < 50
        )
        assertTrue(
            "求助应当换来领导信任",
            result.state.player.attributes.leaderTrust >
                close.player.attributes.leaderTrust
        )
    }

    @Test
    fun befriendShiftsFactionAndRefuseAvoidsRisk() {
        val reform = NpcCatalog.find("gao_zhiqiang")!!
        val state = newGame().energy(60).rel(
            newGame().relationship.withRisk(30)
        )
        val befriended = PoliticalEngine.interact(state, reform.id, Interaction.BEFRIEND)
        assertTrue(
            "结交应当拉近对方势力",
            befriended.state.relationship.factionOf(Faction.REFORM) > 0
        )
        assertTrue(
            "结交改革派会让保守派不满",
            befriended.state.relationship.factionOf(Faction.CONSERVATIVE) < 0
        )

        val refused = PoliticalEngine.interact(state, reform.id, Interaction.REFUSE)
        assertTrue("拒绝会得罪人", refused.state.relationship.relationOf(reform.id) < 0)
        assertTrue(
            "拒绝合作可以避开风险",
            refused.state.relationship.risk < state.relationship.risk
        )
    }

    @Test
    fun observeRevealsTheOtherSideWithoutChangingRelation() {
        val npc = NpcCatalog.find("ma_wenbin")!!
        val state = newGame().energy(60)
        val result = PoliticalEngine.interact(state, npc.id, Interaction.OBSERVE)
        assertTrue("观察后应当能看清底细", result.state.relationship.isScouted(npc.id))
        assertEquals(
            "观察不应改变关系",
            0,
            result.state.relationship.relationOf(npc.id)
        )
    }

    @Test
    fun politicalEventsAreGatedByRisk() {
        val calm = newGame()
        val calmIds = EventEngine.eligibleEvents(calm).map { it.id }
        assertFalse("风险不高时不应被谈话", "pol_talk" in calmIds)
        assertFalse("风险不高时不应被专项检查", "pol_investigation" in calmIds)

        val risky = newGame().rel(calm.relationship.withRisk(85))
        val riskyIds = EventEngine.eligibleEvents(risky).map { it.id }
        assertTrue("风险偏高时应当出现谈话提醒", "pol_talk" in riskyIds)
        assertTrue("风险很高时应当出现专项检查", "pol_investigation" in riskyIds)
    }

    @Test
    fun politicalEventsAreGatedByFactionRelation() {
        val plain = newGame()
        assertFalse(
            "没有改革派关系时不应出现借调机会",
            "pol_transfer_offer" in EventEngine.eligibleEvents(plain).map { it.id }
        )
        val connected = newGame().rel(
            plain.relationship.withFaction(Faction.REFORM, 40)
        )
        assertTrue(
            "改革派关系到位后应当出现借调机会",
            "pol_transfer_offer" in EventEngine.eligibleEvents(connected).map { it.id }
        )
    }

    @Test
    fun choicesChangeRelationsThroughTheNormalEventFlow() {
        val npcId = "lin_xiaoyun"
        val state = newGame().playing("pol_subordinate_help")
        val after = GameEngine.resolveChoice(state, 0)
        assertTrue(
            "选项应当改变人物关系",
            after.relationship.relationOf(npcId) > state.relationship.relationOf(npcId)
        )
        val record = after.outcome
        assertNotNull(record)
        assertTrue(
            "结果面板应当展示官场影响",
            record!!.relationDeltas.isNotEmpty()
        )
    }

    @Test
    fun relationshipStateSurvivesSaveAndLoad() {
        var state = newGame().energy(80).wealth(50)
        state = PoliticalEngine.interact(state, "zheng_haitao", Interaction.GIFT).state
        state = PoliticalEngine.interact(state, "gao_zhiqiang", Interaction.BEFRIEND).state
        state = PoliticalEngine.interact(state, "ma_wenbin", Interaction.OBSERVE).state

        val encoded = GameCodec.encode(state)
        val restored = GameCodec.decode(encoded)
        assertNotNull(restored)
        assertEquals(state.relationship, restored!!.relationship)
        assertTrue("风险必须保留", restored.relationship.risk > 0)
        assertTrue("已摸清的人物必须保留", restored.relationship.isScouted("ma_wenbin"))
    }

    @Test
    fun riskDecaysEachQuarter() {
        val state = newGame().rel(newGame().relationship.withRisk(40))
        val settled = PoliticalEngine.settleRisk(state)
        assertEquals(
            40 - RelationshipEngine.RISK_DECAY,
            settled.relationship.risk
        )
    }

    @Test
    fun highRiskBlocksPromotionButRelationsOnlyHelpAfterRequirements() {
        val base = newGame()
        val strong = base.withGovernance(merit = 70, approval = 70).copy(
            player = base.player.copy(
                positionIndex = 2,
                yearsInPosition = 5,
                attributes = base.player.attributes
                    .withValue(Attr.WORK, 95)
                    .withValue(Attr.ABILITY, 92)
                    .withValue(Attr.POLITICS, 92)
                    .withValue(Attr.LEADER_TRUST, 95)
                    .withValue(Attr.MASSES, 92)
                    .withValue(Attr.INTEGRITY_RISK, 5)
            )
        )
        val risky = strong.rel(strong.relationship.withRisk(PromotionEngine.RISK_BLOCK_THRESHOLD))
        repeat(30) { seed ->
            assertFalse(
                "风险过高时不应晋升",
                PromotionEngine.evaluate(risky, 92, GameRng(seed.toLong() + 1)).granted
            )
        }

        val weak = base.copy(
            player = base.player.copy(
                positionIndex = 2,
                yearsInPosition = 5,
                attributes = base.player.attributes
                    .withValue(Attr.WORK, 10)
                    .withValue(Attr.ABILITY, 10)
                    .withValue(Attr.POLITICS, 10)
            )
        )
        val wellConnected = weak.rel(
            weak.relationship
                .withFaction(Faction.CORE, 100)
                .withRelation("chen_jianguo", 100)
                .withTrust("chen_jianguo", 100)
        )
        repeat(30) { seed ->
            assertFalse(
                "能力与实绩不达标时，关系再好也不能晋升",
                PromotionEngine.evaluate(wellConnected, 92, GameRng(seed.toLong() + 1)).granted
            )
        }

        val connected = strong.rel(
            strong.relationship.withFaction(Faction.CORE, 80)
        )
        // 关系不再决定能不能晋升：硬条件达标就一定晋升，不会被别人卡住。
        assertTrue(
            "硬条件达标即应晋升",
            PromotionEngine.evaluate(strong, 92, GameRng(1L)).granted
        )
        assertTrue(
            "关系好同样正常晋升",
            PromotionEngine.evaluate(connected, 92, GameRng(1L)).granted
        )
        // 关系好带来的是额外助力，而不是门槛。
        assertTrue(
            "势力关系到位应当提供更多助力" +
                "（${PoliticsEngine.relationshipSupport(connected)} vs ${PoliticsEngine.relationshipSupport(strong)}）",
            PoliticsEngine.relationshipSupport(connected) > PoliticsEngine.relationshipSupport(strong)
        )
    }

    @Test
    fun politicalEventLibraryIsSubstantialAndStartsItsOwnChain() {
        val political = EventLibrary.events.filter { it.id.startsWith("pol_") }
        assertTrue("官场事件不应少于 10 个", political.size >= 10)
        assertTrue(
            "应当有事件可以开启人情链",
            EventLibrary.events.any { event ->
                event.choices.any { it.startsChain == "favor" }
            }
        )
        val steps = EventLibrary.stepsOf("favor").map { it.chainStep }
        assertEquals(listOf(1, 2, 3), steps)
    }

    @Test
    fun factionAndNpcCatalogsMeetTheRequirements() {
        assertTrue("势力不应少于 4 个", Faction.entries.size >= 4)
        assertTrue("关键人物不应少于 5 名", NpcCatalog.npcs.size >= 5)
        NpcCatalog.npcs.forEach { npc ->
            assertTrue("${npc.name} 缺少官职", npc.title.isNotBlank())
            assertTrue("${npc.name} 缺少性格标签", npc.personality.isNotEmpty())
            assertTrue("${npc.name} 影响力越界", npc.influence in 0..100)
        }
        val ids = NpcCatalog.npcs.map { it.id }
        assertEquals("人物 id 不应重复", ids.size, ids.toSet().size)
    }

    // 端到端验收：互动 → 事件 → 关系变化 → 存盘 → 退出 → 重新进入。
    @Test
    fun acceptanceFlowKeepsRelationshipDataAcrossSaveAndLoad() {
        var state = GameEngine.newGame("验收", "男", "本科", "乡镇公务员", 2026L)

        // 1. 与人物互动
        state = PoliticalEngine.interact(state, "chen_jianguo", Interaction.VISIT).state
        state = PoliticalEngine.interact(state, "zheng_haitao", Interaction.GIFT).state
        assertTrue("互动后应当有关系", state.relationship.relationOf("zheng_haitao") > 0)
        assertTrue("送礼后应当有风险", state.relationship.risk > 0)

        // 2. 触发官场事件并做出选择
        state = state.copy(
            currentEventId = "pol_policy_question",
            phase = GamePhase.EVENT,
            outcome = null
        )
        state = GameEngine.resolveChoice(state, 0)
        assertTrue("选择应当改变势力关系", state.relationship.factionOf(Faction.REFORM) > 0)

        // 3. 开启一条事件链，确认进度会被保存
        state = state.copy(
            currentEventId = "pol_favor_start",
            phase = GamePhase.EVENT,
            outcome = null
        )
        state = GameEngine.resolveChoice(state, 0)
        assertTrue(
            "选择后应当有待触发的事件链",
            state.pendingChains.any { it.chainId == "favor" }
        )

        // 4. 保存并重新载入
        val saved = GameCodec.encode(state)
        val reloaded = GameCodec.decode(saved)
        assertNotNull("存档应当可以读回", reloaded)
        assertEquals(state.relationship, reloaded!!.relationship)
        assertEquals(state.pendingChains, reloaded.pendingChains)
        assertTrue(reloaded.relationship.risk > 0)
        assertTrue(reloaded.pendingChains.isNotEmpty())

        // 5. 读档后继续推进，流程不中断
        val advanced = GameEngine.advance(reloaded)
        assertTrue("读档后应当可以继续推进", advanced.year >= 1)
        assertEquals(
            "季度推进后风险应当按规则自然消退",
            state.relationship.risk - RelationshipEngine.RISK_DECAY,
            advanced.relationship.risk
        )
    }
}
