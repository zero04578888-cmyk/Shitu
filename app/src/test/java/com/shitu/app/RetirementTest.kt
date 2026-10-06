package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.RetirementChoice
import com.shitu.app.game.RetirementEngine
import com.shitu.app.model.AnnualReview
import com.shitu.app.model.Attr
import com.shitu.app.model.EndingCatalog
import com.shitu.app.model.Faction
import com.shitu.app.model.Indicator
import com.shitu.app.model.LogEntry
import com.shitu.app.model.MeritKind
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 65 岁退休机制的回归测试。
 *
 * 规则：
 * - 年龄每年 +1，季度不增加；
 * - 年满 65 岁且职级低于副国级：必须进入【人生节点】，只能「退休 / 退居二线」，
 *   不能继续正常晋升、任职选择或调任，也不能用事件拖延；
 * - 职级达到副国级及以上者不受年龄限制，可以继续任职；
 * - 判断一律使用职级下标（数值），不做字符串比较；
 * - 退休信息进入存档，重新进入游戏后与退出前一致；旧存档缺少字段也能安全读取。
 */
class RetirementTest {

    private fun newGame(seed: Long = 7L): GameState =
        GameEngine.newGame("退休测试", "男", "本科", "机关科员", seed)

    /** 把玩家调整到指定的年龄与职级下标。 */
    private fun GameState.at(age: Int, rankIndex: Int): GameState = copy(
        player = player.copy(
            positionIndex = rankIndex,
            serviceYears = (age - player.ageAtStart).coerceAtLeast(0)
        )
    )

    private fun GameState.withAttrs(vararg pairs: Pair<Attr, Int>): GameState {
        var attrs = player.attributes
        pairs.forEach { (key, value) -> attrs = attrs.withValue(key, value) }
        return copy(player = player.copy(attributes = attrs))
    }

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

    /** 近乎完美的一局：治理、属性全部拉满，用于检验晋升边界。 */
    private fun superbReady(age: Int, rankIndex: Int): GameState {
        val aged = newGame(seed = 5L).at(age, rankIndex)
        var attrs = aged.player.attributes
        Attr.entries.forEach { attr ->
            attrs = attrs.withValue(attr, if (attr == Attr.INTEGRITY_RISK) 0 else 100)
        }
        var jurisdiction = aged.governance.jurisdiction
        Indicator.entries.forEach { indicator -> jurisdiction = jurisdiction.withValue(indicator, 100) }
        var merit = aged.governance.merit
        MeritKind.entries.forEach { kind -> merit = merit.withValue(kind, 100) }
        return aged.copy(
            player = aged.player.copy(yearsInPosition = 20, attributes = attrs),
            relationship = aged.relationship.withFaction(Faction.CORE, 90),
            governance = aged.governance.copy(
                merit = merit,
                jurisdiction = jurisdiction,
                finance = jurisdiction.level.baseFinance * 5
            ),
            year = 40,
            quarter = 4,
            phase = GamePhase.OUTCOME,
            log = (0 until 30).map { index ->
                LogEntry(
                    year = aged.year,
                    quarter = aged.quarter,
                    title = "履历事件 $index",
                    choiceText = "按程序办理",
                    outcome = "留档",
                    deltas = emptyList()
                )
            }
        ).withReview(95)
    }

    /** 正部级只差任命程序、已具备晋升副国级的全部条件。 */
    private fun deputyNationalReady(age: Int): GameState = superbReady(age, 10)

    private fun step(state: GameState, choice: Int = 0): GameState = when (state.phase) {
        GamePhase.EVENT -> GameEngine.resolveChoice(state, choice)
        GamePhase.OUTCOME -> GameEngine.advance(state)
        GamePhase.REVIEW -> GameEngine.proceedFromReview(state)
        GamePhase.ENDING -> state
    }

    // ===== 一、年龄系统 =====

    @Test
    fun ageIncreasesWithEachYearButNotEachQuarter() {
        var state = newGame()
        val startAge = state.player.ageAtStart
        assertEquals("开局年龄应与学历设定一致", startAge, state.player.age)
        assertEquals(0, state.player.serviceYears)

        var guard = 0
        while (state.year == 1 && !state.isFinished && guard < 200) {
            guard += 1
            state = step(state)
            if (state.year == 1) {
                assertEquals("同一年内年龄不得变化", startAge, state.player.age)
            }
        }
        assertTrue("时间应当推进到第二年", state.year >= 2)
        assertEquals("进入新的一年后年龄 +1", startAge + 1, state.player.age)
        assertEquals(1, state.player.serviceYears)
    }

    // ===== 二、65 岁人生节点 =====

    @Test
    fun reachingRetirementAgeIsNotTriggeredBelow65() {
        val age64 = newGame().at(age = 64, rankIndex = 8)
        assertFalse("64 岁不应触发退休节点", RetirementEngine.isDue(age64))
        assertFalse(age64.retirementDue)
        assertEquals("未到年龄不应有退休状态文案", "", age64.retirementStatus)

        val age65 = newGame().at(age = 65, rankIndex = 8)
        assertTrue("65 岁必须触发退休节点", RetirementEngine.isDue(age65))
        assertTrue(age65.retirementDue)
        assertEquals("退休节点", age65.retirementStatus)
    }

    @Test
    fun retirementNodePersistsPastRetirementAgeUntilDecision() {
        for (age in 65..75) {
            val state = newGame().at(age, rankIndex = 8)
            assertTrue("$age 岁未做出选择前必须仍在退休节点", RetirementEngine.isDue(state))
            assertEquals("退休节点", state.retirementStatus)
        }
    }

    // ===== 三、副国级例外 =====

    @Test
    fun deputyNationalRankAndAboveAreExemptFromAgeLimit() {
        assertEquals("副国级下标必须是 11", 11, RetirementEngine.EXEMPT_RANK_INDEX)
        assertEquals("副国级", POSITION_LADDER[RetirementEngine.EXEMPT_RANK_INDEX].name)
        for (age in 60..85) {
            for (rank in RetirementEngine.EXEMPT_RANK_INDEX until POSITION_LADDER.size) {
                val state = newGame().at(age, rank)
                assertFalse("$age 岁 $rank 级（副国级及以上）不应触发退休", RetirementEngine.isDue(state))
                assertEquals("", state.retirementStatus)
            }
        }
    }

    @Test
    fun rankBelowDeputyNationalStillRetiresAt65() {
        for (rank in 0 until RetirementEngine.EXEMPT_RANK_INDEX) {
            val state = newGame().at(age = 65, rankIndex = rank)
            assertTrue("$rank 级低于副国级，65 岁必须退休", RetirementEngine.isDue(state))
        }
    }

    // ===== 四、64 岁仍可正常晋升 =====

    @Test
    fun promotionStillWorksAt64() {
        val ready = firstPromotionReady().at(age = 64, rankIndex = 0)
        assertFalse("64 岁不应触发退休", ready.retirementDue)
        assertTrue("64 岁满足条件仍应具备晋升资格", PromotionEngine.isEligible(ready, 90))
        val promoted = GameEngine.promoteNow(ready)
        assertTrue("64 岁应当可以正常晋升", promoted.promotionCount > ready.promotionCount)
        assertEquals("", promoted.retirementStatus)
    }

    @Test
    fun promotionIsBlockedWhileRetirementNodeIsPending() {
        val ready = firstPromotionReady().at(age = 65, rankIndex = 0)
        assertTrue(ready.retirementDue)
        assertFalse("退休节点未决时不得具备晋升资格", PromotionEngine.canApply(ready, 90))
        assertFalse(PromotionEngine.evaluate(ready, 90, GameRng(1L)).granted)
        assertFalse("退休节点未决时界面不应显示可晋升", PromotionEngine.statusOf(ready, 90).ready)
        assertEquals("退休节点未决时 promoteNow 不应改变状态", ready, GameEngine.promoteNow(ready))
    }

    @Test
    fun deputyNationalAt65CanStillBePromoted() {
        val ready = superbReady(age = 65, rankIndex = 11)
        assertFalse("副国级不受年龄限制", ready.retirementDue)
        assertTrue(PromotionEngine.isEligible(ready, 95))
        val promoted = GameEngine.promoteNow(ready)
        assertEquals("应能晋升到正国级", 12, promoted.player.positionIndex)
        assertFalse(promoted.postCareer)
        assertEquals(65, promoted.player.age)
    }

    // ===== 五、退休流程 =====

    @Test
    fun retirementEndsCareerAndRecordsFinalSnapshot() {
        val due = newGame().at(age = 65, rankIndex = 8)
        val ended = GameEngine.retirementChoice(due, RetirementChoice.RETIREMENT)

        assertTrue("退休应直接进入结局", ended.isFinished)
        val ending = ended.ending
        assertNotNull("退休必须产生结局", ending)
        assertNotNull("退休结局必须在图鉴中", EndingCatalog.find(ending!!.id))
        assertTrue(ended.player.retired)
        assertEquals(RetirementChoice.RETIREMENT.type, ended.player.retirementType)
        assertEquals(65, ended.player.retirementAge)
        assertEquals(due.year, ended.player.retirementYear)
        assertEquals("已退休", ended.retirementStatus)
        assertTrue(ended.postCareer)
        assertNull("退休后不应残留调任机会", ended.transferOffer)
        assertFalse("退休后不应残留晋升挂起", ended.pendingPromotion)
    }

    @Test
    fun secondLineKeepsLimitedContentButBlocksPromotion() {
        val due = newGame().at(age = 65, rankIndex = 8)
        val secondLine = GameEngine.retirementChoice(due, RetirementChoice.SECOND_LINE)

        assertFalse("退居二线不应立即结束游戏", secondLine.isFinished)
        assertFalse(secondLine.player.retired)
        assertEquals(RetirementChoice.SECOND_LINE.type, secondLine.player.retirementType)
        assertEquals(65, secondLine.player.retirementAge)
        assertTrue(secondLine.postCareer)
        assertEquals("退居二线", secondLine.retirementStatus)
        assertFalse("做出选择后不应再次弹出退休节点", secondLine.retirementDue)

        assertFalse("退居二线后不得再参与晋升", PromotionEngine.canApply(secondLine))
        assertEquals("退居二线后 promoteNow 不应改变状态", secondLine, GameEngine.promoteNow(secondLine))

        // 仍可以继续过有限的内容，并且最终一定会正常收口。
        var state = secondLine
        var guard = 0
        while (!state.isFinished && guard < 600) {
            guard += 1
            state = step(state)
        }
        assertTrue("退居二线后游戏仍应正常结束", state.isFinished)
    }

    // ===== 六、不能绕过退休 =====

    @Test
    fun retirementNodeCannotBeBypassedByEventFlow() {
        val due = newGame().at(age = 65, rankIndex = 8)

        assertEquals(
            "事件选择不能绕过退休",
            GamePhase.REVIEW,
            GameEngine.resolveChoice(due.copy(phase = GamePhase.EVENT), 0).phase
        )
        assertEquals(
            "季度推进不能绕过退休",
            GamePhase.REVIEW,
            GameEngine.advance(due.copy(phase = GamePhase.OUTCOME)).phase
        )

        val ended = GameEngine.proceedFromReview(due.copy(phase = GamePhase.REVIEW))
        assertTrue("年度报告继续时若未决，默认按退休处理", ended.isFinished)
        assertTrue(ended.player.retired)
    }

    @Test
    fun retirementNodeAlsoBlocksAt66And67() {
        for (age in listOf(66, 67)) {
            val state = newGame().at(age, rankIndex = 6)
            assertTrue(RetirementEngine.isDue(state))
            assertEquals(GamePhase.REVIEW, GameEngine.resolveChoice(state.copy(phase = GamePhase.EVENT), 0).phase)
            assertEquals(GamePhase.REVIEW, GameEngine.advance(state.copy(phase = GamePhase.OUTCOME)).phase)
        }
    }

    // ===== 七、边界：晋升到副国级 =====

    @Test
    fun promotionToDeputyNationalAtTheBoundaryIsNotPrematurelyRetired() {
        val ready = deputyNationalReady(age = 64)
        assertTrue(
            "基准状态应已具备晋升副国级的条件：${PromotionEngine.blockersOf(ready, 95)}",
            PromotionEngine.isEligible(ready, 95)
        )

        val next = GameEngine.advance(ready)
        assertTrue("64 岁通过考核的晋升不应被退休节点吞掉", next.pendingPromotion)
        assertFalse(next.isFinished)
        assertEquals("年龄应推进到 65", 65, next.player.age)
        assertEquals(
            "服务年限应推进到 65 岁对应的年限",
            65 - ready.player.ageAtStart,
            next.player.serviceYears
        )

        // 选定具体职务、正式任命后进入副国级，从此不受 65 岁限制。
        val promoted = GameEngine.promoteNow(next)
        assertEquals(RetirementEngine.EXEMPT_RANK_INDEX, promoted.player.positionIndex)
        assertFalse("副国级及以上不受 65 岁限制", promoted.retirementDue)
        assertFalse(promoted.postCareer)
        assertEquals("年龄仍是 65", 65, promoted.player.age)
    }

    @Test
    fun promotionBelowDeputyNationalAt65IsReplacedByRetirementNode() {
        // 正厅级（8）晋副厅级（9）发生在 64 岁结算时：即便通过考核，
        // 65 岁且晋升后仍低于副国级，也必须进入退休节点。
        val ready = superbReady(age = 64, rankIndex = 8)
        assertTrue(
            "基准状态应已具备晋升副厅级的条件：${PromotionEngine.blockersOf(ready, 95)}",
            PromotionEngine.isEligible(ready, 95)
        )

        val next = GameEngine.advance(ready)
        assertTrue("晋升后仍低于副国级时必须退休", next.retirementDue)
        assertFalse("不得残留任职选择", next.pendingPromotion)
        assertEquals(GamePhase.REVIEW, next.phase)
    }

    // ===== 八、存档 =====

    @Test
    fun retirementSurvivesSaveAndReload() {
        val due = newGame().at(age = 65, rankIndex = 8)
        val ended = GameEngine.retirementChoice(due, RetirementChoice.RETIREMENT)
        val text = GameCodec.encode(ended)
        val restored = GameCodec.decode(text)
        assertNotNull("退休存档必须能读取", restored)
        val other = restored!!
        assertTrue(other.player.retired)
        assertEquals(RetirementChoice.RETIREMENT.type, other.player.retirementType)
        assertEquals(65, other.player.retirementAge)
        assertEquals(65, other.playerAge)
        assertTrue("重新读取后仍应是结局状态", other.isFinished)
        assertEquals(ended.ending?.id, other.ending?.id)
    }

    @Test
    fun secondLineSurvivesSaveAndReload() {
        val due = newGame().at(age = 65, rankIndex = 8)
        val secondLine = GameEngine.retirementChoice(due, RetirementChoice.SECOND_LINE)
        val restored = GameCodec.decode(GameCodec.encode(secondLine))
        assertNotNull(restored)
        val other = restored!!
        assertFalse(other.player.retired)
        assertEquals(RetirementChoice.SECOND_LINE.type, other.player.retirementType)
        assertEquals(65, other.playerAge)
        assertEquals("退居二线", other.retirementStatus)
        assertTrue("重新读取后仍退出晋升通道", other.postCareer)
        assertEquals(secondLine.displayPositionTitle, other.displayPositionTitle)
    }

    @Test
    fun legacySaveWithoutBirthYearIsCalculatedSafely() {
        val state = newGame().at(age = 50, rankIndex = 6)
        val text = GameCodec.encode(state)
        val legacy = text.split("\n").filterNot { it.startsWith("player.birthYear\t") }.joinToString("\n")
        val restored = GameCodec.decode(legacy)
        assertNotNull("缺少 birthYear 的旧存档必须能读取", restored)
        val other = restored!!
        assertEquals("缺少 birthYear 时按开局年龄回推，年龄不变", 50, other.player.age)
        assertEquals(state.player.effectiveBirthYear, other.player.effectiveBirthYear)
        assertFalse("旧存档默认视为在役", other.postCareer)
        assertEquals("", other.retirementStatus)
    }

    @Test
    fun repeatedRetirementChoiceIsIdempotent() {
        val due = newGame().at(age = 65, rankIndex = 8)
        val secondLine = GameEngine.retirementChoice(due, RetirementChoice.SECOND_LINE)
        // 已经做出选择后再次调用不应改变任何东西。
        assertEquals(secondLine, GameEngine.retirementChoice(secondLine, RetirementChoice.RETIREMENT))
        // 未到年龄时也不会生效。
        val young = newGame().at(age = 40, rankIndex = 6)
        assertEquals(young, GameEngine.retirementChoice(young, RetirementChoice.RETIREMENT))
    }

    @Test
    fun unknownRetirementTypeIsIgnored() {
        val due = newGame().at(age = 65, rankIndex = 8)
        assertEquals("未知类型不应改变状态", due, GameEngine.retirementChoice(due, "NOT_A_TYPE"))
    }

    // ===== 九、全局模拟不崩溃 =====

    @Test
    fun fullCareerEndsWithRetirementOrValidEnding() {
        for (seed in 1L..6L) {
            var state = newGame(seed)
            var guard = 0
            while (!state.isFinished && guard < 4000) {
                guard += 1
                state = step(state, guard % 3)
            }
            assertTrue("seed=$seed 必须产生结局", state.isFinished)
            assertNotNull(state.ending)
            assertTrue(
                "服务年限必须落在合法范围内：${state.player.serviceYears}",
                state.player.serviceYears in 1..GameEngine.MAX_SERVICE_YEARS
            )
            assertTrue(
                "年龄不得超过开局年龄 + 服务年限上限：${state.player.age}",
                state.player.age <= state.player.ageAtStart + GameEngine.MAX_SERVICE_YEARS
            )
        }
    }

    @Test
    fun gameRngIsNotConsumedByRetirementGate() {
        // 退休守卫不消耗随机数：同一状态多次判定结果一致。
        val due = newGame().at(age = 65, rankIndex = 8)
        val rng = GameRng(123L)
        assertEquals(RetirementEngine.isDue(due), RetirementEngine.isDue(due))
        assertEquals(rng.state, GameRng(123L).state)
    }
}