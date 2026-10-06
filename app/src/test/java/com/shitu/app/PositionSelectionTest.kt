package com.shitu.app

import com.shitu.app.game.EventEngine
import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.AnnualReview
import com.shitu.app.model.Attr
import com.shitu.app.model.EventTag
import com.shitu.app.model.FIRST_GRADE_INDEX
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.model.PositionRepository
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
 * 第十八阶段：具体职务选择系统。
 *
 * 规则：
 * - 职级 != 具体职务；同一个职级至少给出 3 个具体职务；
 * - 职务按城市行政层级、任职单位与城市属性生成，不同城市不同；
 * - 满足晋升条件后一定选得到职务，绝不会因为职务池而卡住；
 * - 选择结果进入存档，重新进入游戏后仍然是同一个具体职务。
 */
class PositionSelectionTest {

    // ===== 职务池 =====

    @Test
    fun everyRankOffersAtLeastThreeConcretePositions() {
        for (rank in FIRST_GRADE_INDEX until com.shitu.app.model.POSITION_LADDER.size) {
            val options = PositionRepository.genericOptions(rank, UnitType.GENERAL)
            assertTrue("第 $rank 级至少应有 3 个具体职务，实际 ${options.size}", options.size >= 3)
            assertTrue("具体职务数量不应超过 6 个", options.size <= 6)
            assertEquals("同一职级内职务名不得重复", options.size, options.map { it.title }.toSet().size)
            options.forEach { option ->
                assertEquals(rank, option.rankIndex)
                assertTrue("职务名不能为空", option.title.isNotBlank())
                assertTrue("职务必须有职责简介", option.description.isNotBlank())
            }
        }
    }

    @Test
    fun firstThreeOptionsHaveNoGateSoChoiceIsNeverBlocked() {
        for (rank in FIRST_GRADE_INDEX..11) {
            val head = PositionRepository.genericOptions(rank, UnitType.GENERAL).take(3)
            assertEquals(3, head.size)
            head.forEach { option ->
                assertTrue(
                    "第 $rank 级前三个职务不应设置门槛：${option.title}",
                    option.matches(0, 0, 0)
                )
            }
        }
    }

    @Test
    fun sectionRankOffersConcreteNamedPositions() {
        val titles = PositionRepository.optionsFor(4, "luoyang", UnitType.TOWN).map { it.title }
        assertTrue("正科级应有乡党委书记：$titles", titles.contains("乡党委书记"))
        assertTrue("正科级应有乡镇长：$titles", titles.contains("乡镇长"))
        assertTrue("正科级应有县直部门局长：$titles", titles.contains("县直部门局长"))
    }

    @Test
    fun concretePositionsAreDifferentiated() {
        val options = PositionRepository.genericOptions(4, UnitType.GENERAL).associateBy { it.title }
        val secretary = options.getValue("乡党委书记")
        val head = options.getValue("乡镇长")
        assertNotEquals("不同职务条线必须不同", secretary.routeType, head.routeType)
        assertNotEquals("不同职务影响力必须不同", secretary.organizationalInfluence, head.economicInfluence)
        assertTrue("党委书记应有组织类标签", secretary.eventTags.contains(EventTag.LEADER_TALK))
        assertTrue("乡镇长应有项目类标签", head.eventTags.contains(EventTag.PROJECT))
        assertTrue("每个职务都要有后续方向", options.values.all { it.nextRoutes.isNotEmpty() })
    }

    @Test
    fun differentCitiesGiveDifferentPositionPools() {
        val shanghai = PositionRepository.optionsFor(10, "shanghai", UnitType.CITY).map { it.title }
        val luoyang = PositionRepository.optionsFor(10, "luoyang", UnitType.CITY).map { it.title }
        assertTrue("直辖市应有市级主官职务：$shanghai", shanghai.contains("市长"))
        assertFalse("普通地级市不应出现直辖市层级的主官职务：$luoyang", luoyang.contains("市长"))
        assertTrue("普通地级市仍应有省级部门职务：$luoyang", luoyang.isNotEmpty())
    }

    @Test
    fun cityTraitAddsMatchingDepartment() {
        // 洛阳是旅游名城，应出现文旅对口的正科级职务。
        val luoyang = PositionRepository.optionsFor(5, "luoyang", UnitType.TOWN).map { it.title }
        assertTrue("文旅城市应有对口部门职务：$luoyang", luoyang.any { it.contains("文化") })
    }

    @Test
    fun topRankKeepsFallbackWhenGatesNotMet() {
        val base = GameEngine.newGame("顶层", "男", "本科", "机关科员", 5L)
        val state = base.copy(player = base.player.copy(positionIndex = 11))
        var merit = state.governance.merit
        // 政绩只有 50：硬条件可能满足，但正国级职务池的门槛都高于 50。
        MeritKind.entries.forEach { merit = merit.withValue(it, 50) }
        val withMerit = state.copy(governance = state.governance.copy(merit = merit))
        val options = PromotionEngine.eligibleOptions(withMerit, 95)
        assertFalse("绝不能出现无职务可选", options.isEmpty())
        assertTrue("应回退到默认合法职务", options.first().isDefault)
    }

    // ===== 晋升流程 =====

    private fun readyForPromotion(seed: Long = 11L): GameState {
        val base = GameEngine.newGame("选择测试", "男", "本科", "机关科员", seed)
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

    @Test
    fun eligiblePlayerAlwaysGetsAChoice() {
        val state = readyForPromotion()
        assertTrue("硬条件应当满足", PromotionEngine.canApply(state))
        val options = PromotionEngine.eligibleOptions(state, 90)
        assertTrue("至少应有 3 个可选职务：${options.size}", options.size >= 3)
        assertTrue("所有职务都应属于下一职级", options.all { it.rankIndex == FIRST_GRADE_INDEX })
        assertFalse("职务名不能只是职级", options.all { it.title == "副科级" })
    }

    @Test
    fun annualReviewDefersAppointmentToThePlayer() {
        val ready = readyForPromotion().copy(quarter = 4, phase = GamePhase.OUTCOME)
        val reviewed = GameEngine.advance(ready)
        assertEquals("年度考核后应停在考核页", GamePhase.REVIEW, reviewed.phase)
        assertTrue("应当挂起晋升，等玩家选择具体职务", reviewed.pendingPromotion)
        assertEquals("选择职务之前不应自动升职", 0, reviewed.player.positionIndex)
        assertTrue(
            "挂起期间不应再提供「立即晋升」",
            !PromotionEngine.isEligible(reviewed, 90)
        )
        val options = GameEngine.pendingOptions(reviewed)
        assertTrue("挂起晋升必须提供具体职务：${options.size}", options.size >= 3)
    }

    @Test
    fun completingPendingPromotionUsesTheChosenPosition() {
        val ready = readyForPromotion().copy(quarter = 4, phase = GamePhase.OUTCOME)
        val reviewed = GameEngine.advance(ready)
        val options = GameEngine.pendingOptions(reviewed)
        val chosen = options.firstOrNull { it.title == "乡镇党委副书记" }
        assertNotNull("挂起晋升应包含乡镇党委副书记：${options.map { it.title }}", chosen)
        val promoted = GameEngine.completePendingPromotion(reviewed, chosen!!.id)

        assertFalse("完成后必须清掉挂起标记", promoted.pendingPromotion)
        assertEquals(FIRST_GRADE_INDEX, promoted.player.positionIndex)
        assertEquals(chosen!!.id, promoted.player.appointmentId)
        assertEquals("乡镇党委副书记", promoted.player.appointmentTitle)
        assertEquals("副科级·乡镇党委副书记", promoted.displayPositionTitle)
        assertTrue(
            "履历里应记录这次任命",
            promoted.career.records.any { it.title.contains("乡镇党委副书记") }
        )
    }

    @Test
    fun proceedFromReviewNeverLeavesThePlayerStuck() {
        val ready = readyForPromotion().copy(quarter = 4, phase = GamePhase.OUTCOME)
        val reviewed = GameEngine.advance(ready)
        assertTrue(reviewed.pendingPromotion)
        val next = GameEngine.proceedFromReview(reviewed)
        assertFalse("继续之后不应再挂起", next.pendingPromotion)
        assertEquals("应当已经完成晋升", FIRST_GRADE_INDEX, next.player.positionIndex)
        assertEquals(GamePhase.EVENT, next.phase)
    }

    @Test
    fun manualPromotionRecordsCareerHistory() {
        val state = readyForPromotion()
        val options = PromotionEngine.eligibleOptions(state, 90)
        val chosen = options[1]
        val promoted = GameEngine.promoteNow(state, chosen.id)
        assertEquals(FIRST_GRADE_INDEX, promoted.player.positionIndex)
        assertEquals(chosen.title, promoted.player.appointmentTitle)
        assertTrue(
            "手动晋升也要写进履历",
            promoted.career.records.any { it.kind == com.shitu.app.model.CareerRecordKind.PROMOTION }
        )
    }

    // ===== 存档 =====

    @Test
    fun chosenPositionSurvivesSaveAndReload() {
        val state = readyForPromotion()
        val options = PromotionEngine.eligibleOptions(state, 90)
        val chosen = options.first { it.title == "县政府办公室副主任" }
        val promoted = GameEngine.promoteNow(state, chosen.id)
        val title = promoted.displayPositionTitle

        val restored = GameCodec.decode(GameCodec.encode(promoted))
        assertNotNull("晋升后的存档必须可读", restored)
        assertEquals(chosen.id, restored!!.player.appointmentId)
        assertEquals(chosen.title, restored.player.appointmentTitle)
        assertEquals("重新进入游戏后具体职务不能丢", title, restored.displayPositionTitle)
        assertEquals(
            "重新进入游戏后仍是同一个具体职务",
            chosen.id,
            restored.currentAppointment?.id
        )
    }

    @Test
    fun pendingPromotionSurvivesSaveAndReload() {
        val ready = readyForPromotion().copy(quarter = 4, phase = GamePhase.OUTCOME)
        val reviewed = GameEngine.advance(ready)
        assertTrue(reviewed.pendingPromotion)
        val restored = GameCodec.decode(GameCodec.encode(reviewed))
        assertNotNull(restored)
        assertTrue("挂起状态必须能存档", restored!!.pendingPromotion)
        assertTrue("重新进入后仍要能选职务", GameEngine.pendingOptions(restored).size >= 3)
    }

    @Test
    fun legacySaveWithoutPositionFieldsFallsBackToDefault() {
        val base = GameEngine.newGame("旧档", "男", "本科", "乡镇公务员", 3L)
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
        assertEquals(FIRST_GRADE_INDEX, restored!!.player.positionIndex)
        assertNotNull("旧存档也要解析出具体职务", restored.currentAppointment)
        assertTrue(restored.displayPositionTitle.contains("·"))
        assertFalse(restored.pendingPromotion)
    }

    // ===== 与事件系统联动 =====

    @Test
    fun chosenPositionBiasesMatchingEvents() {
        val base = readyForPromotion()
        // 乡镇党委副书记：基层 / 群众 / 领导条线，不含 PROJECT；
        // 该职级的默认职务「部门副职」含 PROJECT，对比即可看出职务对事件池的影响。
        val promoted = base.copy(
            player = base.player.copy(
                positionIndex = FIRST_GRADE_INDEX,
                appointmentId = "pos_3_town_deputy_secretary",
                appointmentTitle = "乡镇党委副书记"
            )
        )
        assertTrue(promoted.currentPositionTags.contains(EventTag.LEADER_TALK))
        assertFalse(promoted.currentPositionTags.contains(EventTag.PROJECT))

        val projectEvent = EventLibrary.events.first { EventTag.PROJECT in it.tags }
        val withPosition = EventEngine.weightOf(promoted, projectEvent)
        val withoutPosition = EventEngine.weightOf(
            promoted.copy(player = promoted.player.copy(appointmentId = "", appointmentTitle = "")),
            projectEvent
        )
        assertTrue(
            "不对口的职务不应享受事件权重加成：$withPosition vs $withoutPosition",
            withoutPosition > withPosition
        )
    }

    @Test
    fun unknownAppointmentDoesNotCrash() {
        val base = GameEngine.newGame("异常职务", "男", "本科", "乡镇公务员", 4L)
        val state = base.copy(
            player = base.player.copy(
                positionIndex = FIRST_GRADE_INDEX,
                appointmentTitle = "不存在的职务"
            )
        )
        assertNotNull("无法匹配时应安全回退到默认职务", state.currentAppointment)
        assertTrue("无法匹配时不应抛异常", state.currentPositionTags.isEmpty())
        assertTrue(state.displayPositionTitle.isNotBlank())

        // 科员阶段还没有具体职务，派生字段必须安全回退而不是崩溃。
        val rookie = base.copy(player = base.player.copy(appointmentTitle = "不存在的职务"))
        assertEquals(null, rookie.currentAppointment)
        assertTrue(rookie.currentPositionTags.isEmpty())
        assertTrue(rookie.displayPositionTitle.isNotBlank())
    }

    @Test
    fun promotionTargetMatchesNextPromotionIndex() {
        val state = readyForPromotion()
        val options = PromotionEngine.eligibleOptions(state, 90)
        val expected = nextPromotionIndex(state.player.positionIndex)
        assertTrue(options.all { it.rankIndex == expected })
        assertTrue(options.all { it.displayTitle.startsWith(com.shitu.app.model.positionAt(expected).name) })
    }

    // ===== 不同职级的具体职务在存档中保持 =====

    @Test
    fun deputySectionAppointmentSurvivesReload() {
        val option = PositionRepository.optionsFor(5, "luoyang", UnitType.TOWN)
            .first { it.title == "副县长" }
        val base = GameEngine.newGame("副处级任职", "男", "本科", "机关科员", 21L)
        val state = base.copy(
            player = base.player.copy(
                positionIndex = 5,
                appointmentId = option.id,
                appointmentTitle = option.title,
                appointmentCityId = "luoyang"
            )
        )
        assertEquals("副处级·副县长", state.displayPositionTitle)

        val restored = GameCodec.decode(GameCodec.encode(state))
        assertNotNull("副处级存档必须可读", restored)
        assertEquals("副处级·副县长", restored!!.displayPositionTitle)
        assertEquals(option.id, restored.currentAppointment?.id)
    }

    @Test
    fun departmentRankAppointmentSurvivesReload() {
        val option = PositionRepository.optionsFor(8, "luoyang", UnitType.CITY).first()
        val base = GameEngine.newGame("正厅级任职", "男", "本科", "机关科员", 22L)
        val state = base.copy(
            player = base.player.copy(
                positionIndex = 8,
                appointmentId = option.id,
                appointmentTitle = option.title,
                appointmentCityId = "luoyang"
            )
        )
        assertEquals(8, option.rankIndex)
        assertTrue("应显示「正厅级·具体职务」", state.displayPositionTitle.startsWith("正厅级·"))

        val restored = GameCodec.decode(GameCodec.encode(state))
        assertNotNull("正厅级存档必须可读", restored)
        assertEquals(state.displayPositionTitle, restored!!.displayPositionTitle)
        assertEquals(option.title, restored.player.appointmentTitle)
    }
}