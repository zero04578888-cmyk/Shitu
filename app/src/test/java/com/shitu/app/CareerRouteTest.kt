package com.shitu.app

import com.shitu.app.game.AnnualReviewEngine
import com.shitu.app.game.EventEngine
import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.RouteEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.LogKind
import com.shitu.app.model.UnitType
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 职业发展路线：选择之后必须长期影响事件、考核、调岗与存档。
class CareerRouteTest {

    private fun newGame(seed: Long = 9L): GameState =
        GameEngine.newGame("路线测试", "男", "本科", "机关科员", seed)

    private fun GameState.atPosition(index: Int): GameState =
        copy(player = player.copy(positionIndex = index))

    private fun GameState.withRoute(route: CareerRoute?): GameState = copy(route = route)

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
    fun routeEventIsAMilestoneAvailableFromSecondPromotion() {
        val event = EventLibrary.find("route_choice")
        assertNotNull("事件库应当包含选择方向事件", event)
        assertTrue("选择方向应当是里程碑事件", event!!.priority)
        assertTrue("选择方向至少要有三个选项", event.choices.size >= 3)

        assertFalse(
            "刚入职场时不应出现方向选择",
            EventEngine.eligibleEvents(newGame().atPosition(1)).any { it.id == "route_choice" }
        )
        val ready = newGame().atPosition(2)
        assertTrue(
            "达到二级主任科员后应当出现方向选择",
            EventEngine.eligibleEvents(ready).any { it.id == "route_choice" }
        )
        assertEquals(
            "里程碑事件应当被优先选中",
            "route_choice",
            EventEngine.pickMilestoneEvent(ready)?.id
        )
    }

    @Test
    fun choosingADirectionSetsTheRouteAndWritesALog() {
        val state = newGame().atPosition(2).playing("route_choice")
        val chosen = GameEngine.resolveChoice(state, 0)
        assertEquals(CareerRoute.WRITING, chosen.route)
        val log = chosen.log.last()
        assertEquals(LogKind.ROUTE, log.kind)
        assertEquals("文字参谋", log.choiceText)
        assertTrue("选择方向后不应再次出现", "route_choice" !in
            EventEngine.eligibleEvents(chosen).map { it.id })
    }

    @Test
    fun everyDirectionCanBeChosen() {
        val expected = listOf(
            CareerRoute.WRITING,
            CareerRoute.PROFESSIONAL,
            CareerRoute.MASS_WORK,
            CareerRoute.MANAGEMENT
        )
        expected.forEachIndexed { index, route ->
            val state = newGame().atPosition(2).playing("route_choice")
            assertEquals(
                "第 $index 个选项应当是 $route",
                route,
                GameEngine.resolveChoice(state, index).route
            )
        }
    }

    @Test
    fun routeIsPersistedAndOldSavesRemainCompatible() {
        val state = newGame().withRoute(CareerRoute.MANAGEMENT)
        val encoded = GameCodec.encode(state)
        assertEquals(CareerRoute.MANAGEMENT, GameCodec.decode(encoded)?.route)

        // 旧存档没有 route 字段，应当解码为「未确定」而不是报错。
        val legacy = encoded.lines()
            .filterNot { it.startsWith("route\t") }
            .joinToString("\n")
        val restored = GameCodec.decode(legacy)
        assertNotNull("去掉 route 字段后仍应能读取旧存档", restored)
        assertNull(restored!!.route)
    }

    @Test
    fun reviewWeightsAreRebalancedButAlwaysSumToOneHundred() {
        assertEquals(100, RouteEngine.reviewWeights(null).sumOf { it.second })
        CareerRoute.entries.forEach { route ->
            val weights = RouteEngine.reviewWeights(route)
            assertEquals("$route 的考核权重之和应为 100", 100, weights.sumOf { it.second })
            assertEquals(
                "$route 的考核权重不应重复计算同一属性",
                weights.size,
                weights.map { it.first }.toSet().size
            )
        }
    }

    @Test
    fun directionChangesTheAnnualScoreInItsFavour() {
        val base = newGame()
        val writingHeavy = base.copy(
            player = base.player.copy(
                attributes = base.player.attributes
                    .withValue(Attr.WRITING, 92)
                    .withValue(Attr.MASSES, 40)
            )
        )
        val asGeneralist = AnnualReviewEngine.conduct(writingHeavy.withRoute(null)).score
        val asWriter = AnnualReviewEngine.conduct(
            writingHeavy.withRoute(CareerRoute.WRITING)
        ).score
        assertTrue(
            "文字能力突出时，走文字方向应当得到更高评价（$asWriter vs $asGeneralist）",
            asWriter > asGeneralist
        )

        val massHeavy = base.copy(
            player = base.player.copy(
                attributes = base.player.attributes
                    .withValue(Attr.MASSES, 92)
                    .withValue(Attr.WRITING, 40)
            )
        )
        assertTrue(
            "群众基础突出时，走群众方向应当得到更高评价",
            AnnualReviewEngine.conduct(massHeavy.withRoute(CareerRoute.MASS_WORK)).score >
                AnnualReviewEngine.conduct(massHeavy.withRoute(null)).score
        )
    }

    @Test
    fun directionRaisesTheWeightOfMatchingEvents() {
        val materialEvent = EventLibrary.randomPool.first {
            com.shitu.app.model.EventTag.MATERIAL in it.tags
        }
        val unrelated = EventLibrary.randomPool.first {
            com.shitu.app.model.EventTag.MATERIAL !in it.tags &&
                com.shitu.app.model.EventTag.LEADER_TASK !in it.tags
        }
        assertEquals("没有方向时不应有加成", 0, RouteEngine.eventAffinity(null, materialEvent))
        assertTrue(
            "文字方向应当对材料类事件加权",
            RouteEngine.eventAffinity(CareerRoute.WRITING, materialEvent) > 0
        )
        assertEquals(
            "不对口的事件不应有加成",
            0,
            RouteEngine.eventAffinity(CareerRoute.WRITING, unrelated)
        )
    }

    @Test
    fun directionDecidesWhereYouAreAssigned() {
        val state = newGame()
        val writing = state.withRoute(CareerRoute.WRITING)
            .copy(player = state.player.copy(unit = UnitType.TOWN, positionIndex = 4))
        assertEquals(UnitType.CITY, PromotionEngine.nextUnit(writing))

        val professional = state.copy(
            route = CareerRoute.PROFESSIONAL,
            player = state.player.copy(unit = UnitType.BUSINESS, positionIndex = 4)
        )
        assertNull("已经在业务部门时不再调整", PromotionEngine.nextUnit(professional))

        val massWork = state.copy(
            route = CareerRoute.MASS_WORK,
            player = state.player.copy(unit = UnitType.CITY, positionIndex = 6)
        )
        assertEquals(UnitType.TOWN, PromotionEngine.nextUnit(massWork))

        val withoutRoute = state.copy(
            player = state.player.copy(unit = UnitType.BUSINESS, positionIndex = 4)
        )
        assertNull("没有方向时不在关键台阶上强行调岗", PromotionEngine.nextUnit(withoutRoute))
    }

    @Test
    fun directionImprovesPromotionChance() {
        val base = newGame().atPosition(2)
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
            ),
            route = CareerRoute.MANAGEMENT
        )
        // 方向只提供加分与平台便利，不再改变「能不能晋升」这个结果。
        assertTrue(
            "方向明确时条件达标应正常晋升",
            PromotionEngine.evaluate(strong, 92, com.shitu.app.game.GameRng(1L)).granted
        )
        assertTrue(
            "没有选择方向时条件达标同样应正常晋升",
            PromotionEngine.evaluate(strong.withRoute(null), 92, com.shitu.app.game.GameRng(1L)).granted
        )
        assertTrue(
            "方向明确应当带来额外加分",
            RouteEngine.promotionBonus(CareerRoute.MANAGEMENT) > 0
        )
    }
}
