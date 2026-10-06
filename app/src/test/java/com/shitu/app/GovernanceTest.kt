package com.shitu.app

import com.shitu.app.game.AnnualReviewEngine
import com.shitu.app.game.EventEngine
import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.GovernanceCatalog
import com.shitu.app.game.GovernanceEngine
import com.shitu.app.game.JurisdictionFactory
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.GovernanceCategory
import com.shitu.app.model.Indicator
import com.shitu.app.model.JurisdictionLevel
import com.shitu.app.model.MeritKind
import com.shitu.app.model.UnitType
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 第八阶段：辖区、政务行动、财政、政绩、民望、考核与晋升联动、存档。
class GovernanceTest {

    private fun newGame(seed: Long = 21L): GameState =
        GameEngine.newGame("政务测试", "男", "本科", "乡镇公务员", seed)

    private fun GameState.energy(value: Int): GameState =
        copy(player = player.copy(attributes = player.attributes.withValue(Attr.ENERGY, value)))

    private fun GameState.withFinance(value: Int): GameState =
        copy(governance = governance.copy(finance = value))

    private fun GameState.withIndicator(indicator: Indicator, value: Int): GameState =
        copy(governance = governance.copy(
            jurisdiction = governance.jurisdiction.withValue(indicator, value)
        ))

    private fun GameState.withMerit(value: Int): GameState {
        var merit = governance.merit
        MeritKind.entries.forEach { merit = merit.withValue(it, value) }
        return copy(governance = governance.copy(merit = merit))
    }

    private fun GameState.playing(eventId: String): GameState =
        copy(currentEventId = eventId, phase = GamePhase.EVENT, outcome = null)

    @Test
    fun jurisdictionMatchesThePositionLevel() {
        assertEquals(
            JurisdictionLevel.TOWNSHIP,
            JurisdictionFactory.levelFor(UnitType.TOWN, 0)
        )
        assertEquals(
            JurisdictionLevel.KEY_TOWN,
            JurisdictionFactory.levelFor(UnitType.TOWN, 4)
        )
        assertEquals(
            JurisdictionLevel.COUNTY,
            JurisdictionFactory.levelFor(UnitType.COUNTY, 6)
        )
        val township = JurisdictionFactory.profileOf(JurisdictionLevel.TOWNSHIP)
        val county = JurisdictionFactory.profileOf(JurisdictionLevel.COUNTY)
        assertTrue("辖区越大人口越多", county.population > township.population)
        assertTrue("辖区越大行动点越多", county.level.actionPoints > township.level.actionPoints)
        Indicator.entries.forEach { indicator ->
            assertTrue(
                "指标越界：${indicator.label}",
                township.valueOf(indicator) in 0..100
            )
        }
        assertTrue("治理压力应当在 0～100", township.pressure in 0..100)
    }

    @Test
    fun upgradingJurisdictionKeepsHalfOfTheOldResults() {
        val current = JurisdictionFactory.profileOf(JurisdictionLevel.TOWNSHIP)
            .withValue(Indicator.ECONOMY, 80)
        val upgraded = JurisdictionFactory.upgraded(current, JurisdictionLevel.KEY_TOWN)
        val fresh = JurisdictionFactory.profileOf(JurisdictionLevel.KEY_TOWN)
        val expected = (fresh.economy + 80) / 2
        assertEquals(expected, upgraded.economy)
        assertEquals(JurisdictionLevel.KEY_TOWN, upgraded.level)
    }

    @Test
    fun actionPointsLimitHowMuchYouCanDoEachQuarter() {
        var state = newGame().energy(100)
        val points = state.governance.actionPoints
        assertTrue("每季度至少要有行动点", points >= 2)

        repeat(points) {
            val action = GovernanceEngine.availableActions(state).firstOrNull()
            assertNotNull("应当还有可做的事", action)
            state = GovernanceEngine.perform(state, action!!.id).state
        }
        assertEquals(0, state.governance.actionsLeft)
        assertEquals(
            "行动点用完后应当被拒绝",
            "本季度行动点已经用完",
            GovernanceEngine.unavailabilityReason(state, GovernanceCatalog.actions.first())
        )
    }

    @Test
    fun financeLimitsInvestment() {
        val broke = newGame().energy(100).withFinance(0)
        val road = GovernanceCatalog.find("act_road")!!
        assertFalse(GovernanceEngine.isAvailable(broke, road))
        assertEquals(
            "财政拿不出这笔钱",
            GovernanceEngine.unavailabilityReason(broke, road)
        )
    }

    @Test
    fun performingAnActionCostsResourcesAndRaisesMerit() {
        val state = newGame().energy(60).withFinance(60)
        val road = GovernanceCatalog.find("act_road")!!
        val before = state.governance
        val outcome = GovernanceEngine.perform(state, road.id)

        assertEquals(
            before.finance - road.financeCost,
            outcome.state.governance.finance
        )
        assertEquals(
            before.actionsUsed + 1,
            outcome.state.governance.actionsUsed
        )
        assertTrue(
            "应当提高基础设施",
            outcome.state.governance.jurisdiction.infrastructure > before.jurisdiction.infrastructure
        )
        assertTrue(
            "应当计入政绩",
            outcome.state.governance.merit.overall > before.merit.overall
        )
        assertFalse(
            "刚做完的措施要进入冷却",
            outcome.state.governance.isReady(road.id, outcome.state.absoluteQuarter)
        )
        assertTrue(outcome.message.isNotBlank())
    }

    @Test
    fun personalActionsUseSavingsInsteadOfPublicMoney() {
        val action = GovernanceCatalog.find("self_family")!!
        val state = newGame().energy(60)
            .copy(governance = newGame().governance.copy(savings = 50, finance = 0))
        val outcome = GovernanceEngine.perform(state, action.id)
        assertEquals(
            "个人事项不动用财政",
            state.governance.finance,
            outcome.state.governance.finance
        )
        assertEquals(
            state.governance.savings - action.savingsCost,
            outcome.state.governance.savings
        )
        assertTrue(
            "个人事项应当改善家庭关系",
            outcome.state.player.attributes.family > state.player.attributes.family
        )
    }

    @Test
    fun quarterlySettlementPaysSalaryAndResetsActionPoints() {
        var state = newGame().energy(100)
        state = GovernanceEngine.perform(state, GovernanceCatalog.actions.first().id).state
        assertTrue(state.governance.actionsUsed > 0)

        val (settled, _) = GovernanceEngine.settleQuarter(state)
        assertEquals("行动点每季度重置", 0, settled.governance.actionsUsed)
        assertTrue(
            "工资应当进入个人存款",
            settled.governance.savings > state.governance.savings
        )
        val expectedNet = GovernanceEngine.quarterlyIncome(state) -
            GovernanceEngine.quarterlyExpense(state)
        assertEquals(
            (state.governance.finance + expectedNet).coerceAtLeast(0),
            settled.governance.finance
        )
    }

    @Test
    fun meritDecaysEveryYearSoItCannotBeFarmedOnce() {
        val state = newGame().withMerit(60).withFinance(20)
        val settled = GovernanceEngine.settleYear(state.governance)
        MeritKind.entries.forEach { kind ->
            assertEquals(
                "${kind.label} 应当自然衰减",
                60 - GovernanceEngine.MERIT_ANNUAL_DECAY,
                settled.merit.valueOf(kind)
            )
        }
    }

    @Test
    fun incidentsAppearOnlyWhenTheJurisdictionHasTheProblem() {
        val healthy = newGame()
            .withFinance(120)
            .withIndicator(Indicator.ENVIRONMENT, 80)
            .withIndicator(Indicator.APPROVAL, 80)
        val healthyIds = EventEngine.eligibleEvents(healthy).map { it.id }
        assertFalse("财政充裕时不应出现财政危机", "gov_inc_finance_crisis" in healthyIds)
        assertFalse("环境良好时不应出现排污事件", "gov_inc_pollution" in healthyIds)
        assertFalse("民望高时不应出现集体投诉", "gov_inc_petition" in healthyIds)

        val troubled = newGame()
            .withFinance(10)
            .withIndicator(Indicator.ENVIRONMENT, 35)
            .withIndicator(Indicator.APPROVAL, 30)
        val troubledIds = EventEngine.eligibleEvents(troubled).map { it.id }
        assertTrue("财政见底应当触发财政危机", "gov_inc_finance_crisis" in troubledIds)
        assertTrue("环境差应当触发排污事件", "gov_inc_pollution" in troubledIds)
        assertTrue("民望低应当触发集体投诉", "gov_inc_petition" in troubledIds)
    }

    @Test
    fun contentMeetsTheMinimumStandard() {
        val policies = EventLibrary.events.count { it.id.startsWith("gov_policy_") }
        val incidents = EventLibrary.events.count { it.id.startsWith("gov_inc_") }
        assertTrue("政策选择不应少于 10 个，实际 $policies", policies >= 10)
        assertTrue("突发治理事件不应少于 15 个，实际 $incidents", incidents >= 15)
        assertTrue("政务措施不应少于 10 项", GovernanceCatalog.actions.size >= 10)
        assertTrue(
            "政务措施应当覆盖多个类别",
            GovernanceCategory.entries.count { GovernanceCatalog.byCategory(it).isNotEmpty() } >= 5
        )
        Indicator.entries.forEach { indicator ->
            assertTrue("${indicator.label} 应当被某个措施影响",
                GovernanceCatalog.actions.any { it.effect.indicators.containsKey(indicator) })
        }
    }

    @Test
    fun annualReviewIncludesGovernance() {
        val base = newGame()
        val strong = base.withMerit(80)
            .withIndicator(Indicator.APPROVAL, 85)
            .withIndicator(Indicator.SECURITY, 85)
            .withFinance(90)
        assertTrue(
            "治理成绩应当影响年度考核",
            AnnualReviewEngine.conduct(strong).score >
                AnnualReviewEngine.conduct(base).score
        )
        assertTrue(
            "考核明细应当包含政绩与民望",
            AnnualReviewEngine.conduct(strong).items.any { it.label == "综合政绩" } &&
                AnnualReviewEngine.conduct(strong).items.any { it.label == "民望" }
        )
    }

    @Test
    fun promotionRequiresGovernanceResults() {
        val base = newGame()
        val strong = base.copy(
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
        repeat(20) { seed ->
            assertFalse(
                "没有政绩不应晋升",
                PromotionEngine.evaluate(strong, 92, GameRng(seed.toLong() + 1)).granted
            )
        }

        val qualified = strong.withMerit(80)
            .withIndicator(Indicator.APPROVAL, 80)
        var granted = false
        for (seed in 1L..60L) {
            if (PromotionEngine.evaluate(qualified, 92, GameRng(seed)).granted) {
                granted = true
                break
            }
        }
        assertTrue("政绩与民望达标后应当存在晋升机会", granted)
    }

    @Test
    fun governanceStateSurvivesSaveAndLoad() {
        var state = newGame().energy(100).withFinance(80)
        state = GovernanceEngine.perform(state, "act_school").state
        state = GovernanceEngine.perform(state, "act_security").state
        val (settled, _) = GovernanceEngine.settleQuarter(state)

        val restored = GameCodec.decode(GameCodec.encode(settled))
        assertNotNull(restored)
        assertEquals(settled.governance, restored!!.governance)
        assertTrue("辖区名称应当保留", restored.governance.jurisdiction.name.isNotBlank())
        assertTrue("政绩应当保留", restored.governance.merit.overall > 0)
        assertTrue("冷却记录应当保留", restored.governance.cooldowns.isNotEmpty())
    }

    @Test
    fun acceptanceFlowCoversGovernanceAndPersistence() {
        var state = GameEngine.newGame("验收八", "男", "本科", "乡镇公务员", 2026L)
        // 处理政务
        state = GovernanceEngine.perform(state, "act_jobs").state
        assertTrue("执行措施后政绩应当上升", state.governance.merit.overall > 0)
        // 触发治理事件并选择
        state = state.playing("gov_policy_budget")
        state = GameEngine.resolveChoice(state, 1)
        assertTrue(
            "政策选择应当改变辖区指标",
            state.governance.jurisdiction.education > 50
        )
        assertNotNull(state.outcome)
        assertTrue(
            "结算面板应当展示辖区变化",
            state.outcome!!.governanceDeltas.isNotEmpty()
        )
        // 推进季度结算
        state = GameEngine.advance(state)
        assertEquals("行动点应当重置", 0, state.governance.actionsUsed)
        // 存档往返
        val restored = GameCodec.decode(GameCodec.encode(state))
        assertNotNull(restored)
        assertEquals(state.governance, restored!!.governance)
        // 继续推进不中断
        val next = GameEngine.advance(restored.copy(phase = GamePhase.OUTCOME))
        assertTrue(next.year >= 1)
    }

    @Test
    fun personalLifeActionsCoverEverydayNeeds() {
        // 第二阶段要求的调研/休息，以及个人生活的交通/日常消费都应当存在。
        listOf("self_survey", "self_rest", "self_transport", "self_daily").forEach { id ->
            val action = GovernanceCatalog.find(id)
            assertNotNull("缺少个人事项 $id", action)
            assertEquals(GovernanceCategory.PERSONAL, action!!.category)
        }
        val before = newGame().energy(20)
        val after = GovernanceEngine.perform(before, "self_rest").state
        assertTrue("休息应当恢复精力", after.player.attributes.energy > before.player.attributes.energy)
        val message = GovernanceEngine.perform(before, "self_rest").message
        assertTrue("反馈应当说明属性变化，实际：$message", message.contains("精力"))
    }
}
