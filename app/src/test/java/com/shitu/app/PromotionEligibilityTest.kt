package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.NpcCatalog
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.WorldSimulationEngine
import com.shitu.app.model.AnnualReview
import com.shitu.app.model.Attr
import com.shitu.app.model.Faction
import com.shitu.app.model.Indicator
import com.shitu.app.model.LogEntry
import com.shitu.app.model.MeritKind
import com.shitu.app.model.PromotionPositionCatalog
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 条件制晋升的回归测试。
 *
 * 规则：当前职位的全部硬条件满足 -> 立即获得晋升资格并晋升。
 * 不存在空位、名额、概率、竞争或 NPC 占位等任何额外门槛。
 */
class PromotionEligibilityTest {

    private fun newGame(seed: Long = 11L): GameState =
        GameEngine.newGame("资格测试", "男", "本科", "机关科员", seed)

    private fun GameState.withMeritAndApproval(merit: Int, approval: Int): GameState {
        var m = governance.merit
        MeritKind.entries.forEach { m = m.withValue(it, merit) }
        return copy(
            governance = governance.copy(
                merit = m,
                jurisdiction = governance.jurisdiction.withValue(Indicator.APPROVAL, approval)
            )
        )
    }

    private fun GameState.withReview(score: Int): GameState = copy(
        reviews = reviews + AnnualReview(
            year = year,
            score = score,
            grade = "优秀",
            summary = "测试用年度评价",
            items = emptyList(),
            promotionText = "",
            promotionGranted = false
        )
    )

    private fun GameState.withAttrs(vararg pairs: Pair<Attr, Int>): GameState {
        var attrs = player.attributes
        pairs.forEach { (key, value) -> attrs = attrs.withValue(key, value) }
        return copy(player = player.copy(attributes = attrs))
    }

    /** 科员刚够第一次晋升（副科级）的全部硬条件。 */
    private fun firstPromotionReady(): GameState {
        val base = newGame().withMeritAndApproval(merit = 20, approval = 70)
        val positioned = base.copy(player = base.player.copy(positionIndex = 0, yearsInPosition = 3))
        return positioned.withAttrs(
            Attr.WORK to 70,
            Attr.ABILITY to 70,
            Attr.POLITICS to 70,
            Attr.LEADER_TRUST to 70,
            Attr.MASSES to 70,
            Attr.INTEGRITY_RISK to 0
        ).withReview(90)
    }

    /** 副国级晋正国级：只差组织程序的高职级玩家。 */
    private fun topRungReady(merit: Int): GameState {
        val base = newGame(seed = 5L).withMeritAndApproval(merit = merit, approval = 99)
        val positioned = base.copy(
            player = base.player.copy(positionIndex = 11, yearsInPosition = 20),
            relationship = base.relationship.withFaction(Faction.CORE, 90),
            log = (0 until 30).map { index ->
                LogEntry(
                    year = base.year,
                    quarter = base.quarter,
                    title = "履历事件 $index",
                    choiceText = "按程序办理",
                    outcome = "留档",
                    deltas = emptyList()
                )
            }
        )
        return positioned.withAttrs(
            Attr.WORK to 100,
            Attr.ABILITY to 100,
            Attr.POLITICS to 100,
            Attr.LEADER_TRUST to 100,
            Attr.MASSES to 95,
            Attr.REPUTATION to 100,
            Attr.COLLEAGUES to 100,
            Attr.INTEGRITY_RISK to 0,
            Attr.WEALTH to 100
        ).withReview(95)
    }

    @Test
    fun unmetConditionsCannotPromote() {
        val fresh = newGame()
        assertFalse("新入职不应具备晋升资格", PromotionEngine.isEligible(fresh, 90))
        for (seed in 1L..20L) {
            assertFalse(
                "硬条件不足时不应晋升（seed=$seed）",
                PromotionEngine.evaluate(fresh, 90, GameRng(seed)).granted
            )
        }
    }

    @Test
    fun missingSingleConditionCannotPromote() {
        val ready = firstPromotionReady()
        assertTrue("先确认基准状态本身达标", PromotionEngine.isEligible(ready, 90))

        val missingYears = ready.copy(player = ready.player.copy(yearsInPosition = 0))
        assertEquals("只应剩任职年限这一条阻碍", 1, PromotionEngine.blockersOf(missingYears, 90).size)
        assertFalse(PromotionEngine.isEligible(missingYears, 90))
        assertFalse(PromotionEngine.evaluate(missingYears, 90, GameRng(3L)).granted)

        val missingMerit = ready.withMeritAndApproval(merit = 1, approval = 70)
        assertFalse("政绩不足时不应晋升", PromotionEngine.isEligible(missingMerit, 90))
        assertFalse(PromotionEngine.evaluate(missingMerit, 90, GameRng(4L)).granted)
    }

    @Test
    fun allConditionsMetGrantEligibilityImmediately() {
        val ready = firstPromotionReady()
        assertTrue(PromotionEngine.isEligible(ready, 90))
        val status = PromotionEngine.statusOf(ready, 90)
        assertTrue(status.ready)
        assertTrue(status.eligible)
        assertTrue(status.blockers.isEmpty())
        val result = PromotionEngine.evaluate(ready, 90, GameRng(1L))
        assertTrue("全部条件满足即应晋升：${result.text}", result.granted)
        assertTrue("晋升说明应提到任职资格：${result.text}", result.text.contains("任职资格"))
    }

    @Test
    fun noVacancyStillPromotes() {
        val ready = firstPromotionReady()
        assertEquals("测试前提：没有任何岗位空缺数据", 0, ready.world.vacancies.size)
        assertTrue("没有空位也必须晋升", PromotionEngine.evaluate(ready, 90, GameRng(2L)).granted)
        assertEquals(
            "空缺查询应返回空，但晋升不受其影响",
            0,
            WorldSimulationEngine.promotionOpportunityBonus(ready)
        )
    }

    @Test
    fun manyNpcsDoNotAffectEligibility() {
        val ready = firstPromotionReady()
        assertTrue("世界里应当存在大量 NPC", NpcCatalog.npcs.size >= 20)
        for (seed in 1L..20L) {
            assertTrue(
                "多 NPC 环境下仍应正常晋升（seed=$seed）",
                PromotionEngine.evaluate(ready, 90, GameRng(seed)).granted
            )
        }
    }

    @Test
    fun emptyPositionDataFallsBackToDefault() {
        // 正国级可选职位要求政绩 60；此处政绩 50，硬条件已满足但没有具体职位匹配。
        val state = topRungReady(merit = 50)
        assertTrue("硬条件应当已满足", PromotionEngine.isEligible(state, 95))
        assertTrue(
            "职位池确实没有匹配项",
            PromotionPositionCatalog.optionsFor(12).filter { it.matches(50, 99, 100) }.isEmpty()
        )
        val options = PromotionEngine.eligibleOptions(state, 95)
        assertFalse("绝不能出现无职位可选", options.isEmpty())
        assertTrue("应回退到默认合法职位", options.first().isDefault)
        assertEquals(12, options.first().rankIndex)

        val promoted = GameEngine.promoteNow(state)
        assertEquals("默认职位也必须能完成晋升", com.shitu.app.model.nextPromotionIndex(state.player.positionIndex), promoted.player.positionIndex)
        assertTrue(promoted.player.appointmentTitle.isNotBlank())
    }

    @Test
    fun defaultPromotionPositionAlwaysUsable() {
        for (rank in 1 until com.shitu.app.model.POSITION_LADDER.size) {
            val fallback = PromotionPositionCatalog.defaultFor(rank)
            assertTrue("第 $rank 级应有默认职位", fallback.isDefault)
            assertEquals(rank, fallback.rankIndex)
            assertTrue("默认职位名称不能为空", fallback.title.isNotBlank())
            assertTrue("默认职位不得设置任何门槛", fallback.matches(0, 0, 0))
        }
        assertTrue(PromotionEngine.defaultPromotionPosition(firstPromotionReady()).isDefault)
    }

    @Test
    fun promotedStateSurvivesSaveAndReload() {
        val promoted = GameEngine.promoteNow(firstPromotionReady())
        assertEquals(3, promoted.player.positionIndex)
        assertTrue("默认晋升也必须落到具体职位", promoted.player.appointmentTitle.isNotBlank())
        assertTrue("晋升后必须显示「职级·具体职位」", promoted.displayPositionTitle.contains("·"))

        val restored = GameCodec.decode(GameCodec.encode(promoted))
        assertNotNull("晋升后的存档必须可读", restored)
        assertEquals(promoted.player.positionIndex, restored!!.player.positionIndex)
        assertEquals(promoted.player.appointmentTitle, restored.player.appointmentTitle)
        assertEquals(promoted.player.positionTitle, restored.player.positionTitle)
        assertFalse(PromotionEngine.isEligible(restored, 90))
    }

    @Test
    fun legacySaveWithoutAppointmentTitleStillLoads() {
        val promoted = GameEngine.promoteNow(firstPromotionReady())
        val lines = GameCodec.encode(promoted).lines()
        val legacy = lines.filterNot { it.startsWith("player.appointmentTitle") }.joinToString("\n")
        val restored = GameCodec.decode(legacy)
        assertNotNull("缺省新字段的旧存档仍应可读", restored)
        assertEquals(promoted.player.positionIndex, restored!!.player.positionIndex)
        assertEquals("", restored.player.appointmentTitle)
        assertTrue("旧存档仍能给出正式职位名称", restored.player.positionTitle.isNotBlank())
    }

    @Test
    fun promoteNowIsStableAcrossRepeatedCalls() {
        var state = firstPromotionReady()
        state = GameEngine.promoteNow(state)
        val once = state.player.positionIndex
        val again = GameEngine.promoteNow(state)
        assertEquals("条件不满足时二次调用不应改变职级", once, again.player.positionIndex)
    }

    /** 不变式：阻碍项必须与界面上列出的硬条件一一对应，不存在隐藏门槛。 */
    @Test
    fun blockersAlwaysMatchDisplayedRequirements() {
        val states = listOf(
            newGame(),
            firstPromotionReady(),
            topRungReady(merit = 30),
            topRungReady(merit = 50)
        )
        states.forEach { state ->
            val blockers = PromotionEngine.blockersOf(state, 90)
            val unmet = PromotionEngine.requirementList(state, 90).filter { !it.met }
            assertEquals(
                "隐藏门槛不得存在：${blockers} vs ${unmet.map { it.label }}",
                unmet.size,
                blockers.size
            )
            assertEquals("阻碍项与未达标条件必须同时为空", unmet.isEmpty(), blockers.isEmpty())
        }
    }

    /** 只要界面上列出的全部硬条件达标，就一定晋升。 */
    @Test
    fun allDisplayedRequirementsMetForcesPromotion() {
        val ready = firstPromotionReady()
        assertTrue(
            "测试前提：界面上所有硬条件都必须达标",
            PromotionEngine.requirementList(ready, 90).all { it.met }
        )
        assertTrue(PromotionEngine.blockersOf(ready, 90).isEmpty())
        assertTrue(PromotionEngine.isEligible(ready, 90))
        for (seed in 1L..30L) {
            assertTrue(
                "全部硬条件达标就必须晋升（seed=$seed）",
                PromotionEngine.evaluate(ready, 90, GameRng(seed)).granted
            )
        }
    }

    /** 历史低评价不再是隐藏门槛：只要当前数值重新达标，就应该晋升。 */
    @Test
    fun pastLowGradesDoNotBlockOnceNumbersAreMetAgain() {
        val base = firstPromotionReady()
        val earlierLow = base.copy(
            reviews = listOf(
                base.reviews.first().copy(year = base.year, score = 45, grade = "基本称职"),
                base.reviews.first().copy(year = base.year + 1, score = 92, grade = "优秀")
            )
        )
        assertEquals(92, PromotionEngine.reviewScoreOf(earlierLow))
        assertTrue("历史低评价不应再形成阻碍", PromotionEngine.blockersOf(earlierLow, 92).isEmpty())
        assertTrue(PromotionEngine.isEligible(earlierLow, 92))
        assertTrue(PromotionEngine.evaluate(earlierLow, 92, GameRng(9L)).granted)
    }

    /** 官场风险是界面可见的硬条件，不是隐藏门槛。 */
    @Test
    fun highRiskIsVisibleInsteadOfHidden() {
        val ready = firstPromotionReady()
        val risky = ready.copy(
            relationship = ready.relationship.withRisk(PromotionEngine.RISK_BLOCK_THRESHOLD)
        )
        val requirements = PromotionEngine.requirementList(risky, 90)
        assertTrue("官场风险必须显示给玩家", requirements.any { it.label == "官场风险" })
        assertFalse(PromotionEngine.isEligible(risky, 90))
        assertEquals(
            "风险阻碍同样必须出现在条件列表里",
            requirements.count { !it.met },
            PromotionEngine.blockersOf(risky, 90).size
        )
    }
}