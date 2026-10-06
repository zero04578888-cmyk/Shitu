package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.AnnualReview
import com.shitu.app.model.Attr
import com.shitu.app.model.FIRST_GRADE_INDEX
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.PositionRepository
import com.shitu.app.model.PositionRoute
import com.shitu.app.model.UnitType
import com.shitu.app.model.nextPromotionIndex
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 第十九阶段：完整职位路线与晋升分流系统。
 *
 * 规则：
 * - 职级 != 具体职位；每个职级都给出多个带有路线标签的具体职位；
 * - 当前具体职位影响下一次职位池（nextPositions 优先）；
 * - 不同城市给出不同的对口职位；
 * - 只要硬条件满足就一定能晋升，职位池绝不可能卡住玩家。
 */
class PhaseNineteenTest {

    private fun promotionReady(seed: Long = 11L): GameState {
        val base = GameEngine.newGame("路线测试", "男", "本科", "机关科员", seed)
        var merit = base.governance.merit
        MeritKind.entries.forEach { merit = merit.withValue(it, 30) }
        val attrs = base.player.attributes
            .withValue(Attr.WORK, 75)
            .withValue(Attr.ABILITY, 75)
            .withValue(Attr.POLITICS, 75)
            .withValue(Attr.LEADER_TRUST, 75)
            .withValue(Attr.MASSES, 75)
            .withValue(Attr.INTEGRITY_RISK, 0)
        return base.copy(
            player = base.player.copy(attributes = attrs, yearsInPosition = 3),
            governance = base.governance.copy(
                merit = merit,
                jurisdiction = base.governance.jurisdiction.withValue(Indicator.APPROVAL, 75)
            ),
            reviews = base.reviews + AnnualReview(
                year = base.year,
                score = 90,
                grade = "优秀",
                summary = "测试用年度评价",
                items = emptyList(),
                promotionText = "",
                promotionGranted = false
            )
        )
    }

    /** 已经处在副科级、准备晋升正科级的状态；挂起标记跳过重复资格判定。 */
    private fun readyForNextRank(): GameState {
        val base = promotionReady()
        return base.copy(
            player = base.player.copy(
                positionIndex = FIRST_GRADE_INDEX,
                appointmentId = "pos_3_town_deputy_secretary",
                appointmentTitle = "乡镇党委副书记"
            ),
            pendingPromotion = true
        )
    }

    // ===== 职位池 =====

    @Test
    fun everyRankOffersConcretePositionsWithinBounds() {
        for (rank in FIRST_GRADE_INDEX until POSITION_LADDER.size) {
            val options = PositionRepository.optionsFor(rank, "luoyang", UnitType.CITY)
            assertTrue("第 $rank 级至少 3 个具体职位：${options.size}", options.size >= PositionRepository.MIN_OPTIONS)
            assertTrue("第 $rank 级最多 6 个具体职位", options.size <= PositionRepository.MAX_OPTIONS)
            assertEquals("同一职级职务名不得重复", options.size, options.map { it.title }.toSet().size)
            options.forEach { option ->
                assertTrue("职务必须有具体名称", option.title.isNotBlank())
                assertTrue("职务不得只写职级", option.title != option.rankName)
                assertNotNull("职务必须有路线标签", option.route)
            }
        }
    }

    @Test
    fun firstThreeOptionsHaveNoGateForEveryRank() {
        for (rank in FIRST_GRADE_INDEX..11) {
            val head = PositionRepository.genericOptions(rank, UnitType.GENERAL).take(3)
            assertEquals("第 $rank 级应有至少 3 个通用职务", 3, head.size)
            head.forEach { option ->
                assertTrue(
                    "第 $rank 级前三个职务不应设置门槛：${option.title}",
                    option.matches(0, 0, 0)
                )
            }
        }
    }

    @Test
    fun optionsCarryRouteAndCareerValue() {
        val finance = PositionRepository.optionById("pos_4_finance_head")
        assertNotNull("财政局局长应存在", finance)
        assertEquals(PositionRoute.ECONOMY, finance!!.route)
        assertEquals("经济管理", finance.routeLabel)
        assertTrue("重点岗位应有履历价值", finance.careerValue >= 8)
        assertEquals(PositionRoute.ECONOMY, PositionRepository.routeOf("pos_4_finance_head"))
        assertEquals(4, PositionRepository.rankOfId("pos_4_finance_head"))
        assertTrue(PositionRepository.careerValueOf("pos_4_finance_head") > 0)
        assertTrue(finance.directionText.contains("经济管理"))
    }

    @Test
    fun nextPositionsFormRouteRelationships() {
        val secretary = PositionRepository.optionById("pos_4_town_secretary")!!
        assertTrue(
            "乡党委书记的后续方向应包含副县长或县委副书记",
            secretary.nextPositions.contains("pos_5_county_deputy_head") ||
                secretary.nextPositions.contains("pos_5_deputy_secretary")
        )
        secretary.nextPositions.forEach { id ->
            assertEquals(
                "后续职位应恰好高一级：$id",
                PositionRepository.rankOfId("pos_4_town_secretary") + 1,
                PositionRepository.rankOfId(id)
            )
        }
        val deputySecretary = PositionRepository.optionById("pos_5_deputy_secretary")!!
        assertTrue(
            "县委副书记应能从乡党委书记转入",
            deputySecretary.previousPositions.contains("pos_4_town_secretary")
        )
    }

    // ===== 当前职位影响下一次职位池 =====

    @Test
    fun chosenLocalGovernancePositionShiftsNextPool() {
        val fromSecretary = PositionRepository.optionById("pos_4_town_secretary")!!
        val fromFinance = PositionRepository.optionById("pos_4_finance_head")!!
        val poolA = PositionRepository.optionsFor(5, "luoyang", UnitType.TOWN, from = fromSecretary, seed = 7L)
        val poolB = PositionRepository.optionsFor(5, "luoyang", UnitType.TOWN, from = fromFinance, seed = 7L)

        assertNotEquals("不同路线应给出不同的职位池顺序", poolA.map { it.id }, poolB.map { it.id })

        fun rankOf(pool: List<com.shitu.app.model.PromotionPosition>, id: String): Int =
            pool.indexOfFirst { it.id == id }.let { if (it < 0) Int.MAX_VALUE else it }

        assertTrue(
            "走地方治理路线时县委副书记应更靠前：${rankOf(poolA, "pos_5_deputy_secretary")} vs ${rankOf(poolB, "pos_5_deputy_secretary")}",
            rankOf(poolA, "pos_5_deputy_secretary") < rankOf(poolB, "pos_5_deputy_secretary")
        )
        assertTrue(
            "走经济路线时市直部门副职应更靠前：${rankOf(poolB, "pos_5_city_bureau_deputy")} vs ${rankOf(poolA, "pos_5_city_bureau_deputy")}",
            rankOf(poolB, "pos_5_city_bureau_deputy") < rankOf(poolA, "pos_5_city_bureau_deputy")
        )
    }

    @Test
    fun chosenFinancePositionBiasesEconomyRoute() {
        val fromFinance = PositionRepository.optionById("pos_4_finance_head")!!
        val pool = PositionRepository.optionsFor(5, "luoyang", UnitType.TOWN, from = fromFinance, seed = 3L)
        assertEquals(PositionRoute.ECONOMY, fromFinance.route)
        val first = pool.first()
        assertTrue(
            "财政路线的首选应落在其后续方向内：${first.id}",
            fromFinance.nextPositions.contains(first.id)
        )
    }

    @Test
    fun differentCitiesGiveDifferentTraitPositions() {
        val tourism = PositionRepository.optionsFor(5, "luoyang", UnitType.TOWN).map { it.title }
        val economic = PositionRepository.optionsFor(5, "shanghai", UnitType.TOWN).map { it.title }
        assertTrue("旅游城市应有文旅对口职位：$tourism", tourism.any { it.contains("文化") })
        assertTrue("经济城市应有财政金融对口职位：$economic", economic.any { it.contains("财政") })
        assertFalse("经济城市不应出现文旅对口职位", economic.any { it.contains("文化旅游") })
    }

    // ===== 高级职级同样有多职位 =====

    @Test
    fun provincialRankOffersMultipleConcretePositions() {
        val options = PositionRepository.optionsFor(8, "suzhou", UnitType.CITY)
        assertTrue("正厅级至少 4 个具体职位：${options.map { it.title }}", options.size >= 4)
        assertTrue(options.any { it.title == "市长" })
        assertTrue(options.any { it.title == "市委书记" })
        assertTrue(options.any { it.title == "省政府部门厅长" })
        assertEquals(options.size, options.map { it.title }.toSet().size)
    }

    // ===== 不卡关 =====

    @Test
    fun eligiblePlayerAlwaysGetsAtLeastThreeOptions() {
        val state = promotionReady()
        assertTrue("硬条件应当满足", PromotionEngine.canApply(state))
        val options = PromotionEngine.eligibleOptions(state, 90)
        assertTrue("至少 3 个可选职位：${options.size}", options.size >= PositionRepository.MIN_OPTIONS)
        val rank = nextPromotionIndex(state.player.positionIndex)
        assertTrue("全部属于下一职级", options.all { it.rankIndex == rank })
        assertTrue("职务名不能只是职级", options.all { it.title != it.rankName })
    }

    @Test
    fun noVacancyDataDoesNotBlockPromotion() {
        val state = promotionReady()
        assertEquals("测试前提：没有任何岗位空缺数据", 0, state.world.vacancies.size)
        assertTrue("没有空位也必须能晋升", PromotionEngine.isEligible(state, 90))
        val promoted = GameEngine.promoteNow(state)
        assertEquals(nextPromotionIndex(state.player.positionIndex), promoted.player.positionIndex)
    }

    @Test
    fun promotionCompletesThroughPositionPickerFlow() {
        val ready = promotionReady().copy(quarter = 4, phase = GamePhase.OUTCOME)
        val reviewed = GameEngine.advance(ready)
        assertEquals(GamePhase.REVIEW, reviewed.phase)
        assertTrue("应挂起晋升等待玩家选择", reviewed.pendingPromotion)
        val options = GameEngine.pendingOptions(reviewed)
        assertTrue("挂起时必须提供至少 3 个具体职位", options.size >= 3)
        val chosen = options.first { it.title == "乡镇党委副书记" }
        val promoted = GameEngine.completePendingPromotion(reviewed, chosen.id)
        assertFalse("完成后不应再挂起", promoted.pendingPromotion)
        assertEquals("副科级·乡镇党委副书记", promoted.displayPositionTitle)
    }

    // ===== 履历与存档 =====

    @Test
    fun promotionRecordsConcretePositionHistory() {
        val state = readyForNextRank()
        val chosen = PromotionEngine.eligibleOptions(state, 90).first { it.title == "县财政局局长" }
        val promoted = GameEngine.promoteNow(state, chosen.id)
        assertTrue(
            "晋升后必须记录走过的具体职务 id：${promoted.career.positionHistory}",
            promoted.career.positionHistory.contains(chosen.id)
        )
    }

    @Test
    fun positionHistoryAndRouteSurviveSaveAndReload() {
        val state = readyForNextRank()
        val chosen = PromotionEngine.eligibleOptions(state, 90).first { it.title == "乡党委书记" }
        val promoted = GameEngine.promoteNow(state, chosen.id)
        val restored = GameCodec.decode(GameCodec.encode(promoted))
        assertNotNull("存档必须可读", restored)
        assertEquals("具体职务 id 必须保留", chosen.id, restored!!.player.appointmentId)
        assertEquals("正科级·乡党委书记", restored.displayPositionTitle)
        assertEquals(
            "走过的具体职务必须完整保留",
            promoted.career.positionHistory,
            restored.career.positionHistory
        )
        assertEquals(PositionRoute.LOCAL_GOVERNANCE, restored.currentAppointment?.route)
    }

    @Test
    fun legacySaveWithoutPositionFallsBackToConcretePosition() {
        val base = GameEngine.newGame("旧档路线", "男", "本科", "机关科员", 5L)
        val state = base.copy(
            player = base.player.copy(
                positionIndex = FIRST_GRADE_INDEX,
                appointmentTitle = "",
                appointmentId = ""
            )
        )
        val text = GameCodec.encode(state)
            .replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t7")
        val restored = GameCodec.decode(text)
        assertNotNull("旧存档必须可读", restored)
        assertNotNull("旧存档也要解析出具体职位", restored!!.currentAppointment)
        assertTrue("旧存档必须显示「职级·具体职位」", restored.displayPositionTitle.contains("·"))
        assertFalse("旧存档不得显示笼统职级名", restored.displayPositionTitle.endsWith("级"))
    }
}