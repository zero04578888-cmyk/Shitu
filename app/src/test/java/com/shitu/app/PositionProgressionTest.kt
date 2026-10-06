package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.FIRST_GRADE_INDEX
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.UnitType
import com.shitu.app.model.isLegacyRank
import com.shitu.app.model.nextPromotionIndex
import com.shitu.app.model.positionAt
import com.shitu.app.model.positionDisplay
import com.shitu.app.model.positionTitleOf
import com.shitu.app.model.previousPromotionIndex
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 晋升等级体系回归测试。
 *
 * 规则：
 * - 职级严格递增，不允许出现「正科级 → 副科级」这类观感倒退；
 * - 旧版残留职级（副主任科员 / 主任科员）不进入晋升路径；
 * - 科员的第一次晋升直接进入「副科级·部门副职」；
 * - 旧存档停在残留职级时自动迁移到副科级。
 */
class PositionProgressionTest {

    @Test
    fun effectiveLadderIsStrictlyIncreasingAndSkipsLegacy() {
        val effective = POSITION_LADDER.filterNot { it.legacy }
        assertEquals(
            listOf(
                "科员", "副科级", "正科级", "副处级", "正处级",
                "副厅级", "正厅级", "副部级", "正部级", "副国级", "正国级"
            ),
            effective.map { it.name }
        )
        // 有效档位连续递增：科员 = 0，副科级 = 1 … 正国级 = 10。
        assertEquals((0..10).toList(), effective.map { it.tier })
        effective.zipWithNext().forEach { (lower, higher) ->
            assertTrue("职级必须严格递增：${lower.name} -> ${higher.name}", higher.tier > lower.tier)
            assertTrue("数组下标必须递增", higher.index > lower.index)
            assertTrue("权力等级必须递增", higher.powerLevel > lower.powerLevel)
        }
        // 仅有的残留职级，且紧跟在科员之后、不参与晋升。
        assertTrue(isLegacyRank(1))
        assertTrue(isLegacyRank(2))
        assertFalse(isLegacyRank(0))
        assertFalse(isLegacyRank(FIRST_GRADE_INDEX))
    }

    @Test
    fun firstPromotionJumpsStraightToDeputySection() {
        assertEquals(FIRST_GRADE_INDEX, nextPromotionIndex(0))
        assertEquals("副科级", positionAt(nextPromotionIndex(0)).name)
        assertFalse(isLegacyRank(nextPromotionIndex(0)))
        // 下一次晋升才进入正科级，绝不会回落到主任科员。
        assertEquals("正科级", positionAt(nextPromotionIndex(FIRST_GRADE_INDEX)).name)
        assertEquals(0, previousPromotionIndex(FIRST_GRADE_INDEX))
    }

    @Test
    fun concretePositionNamesMatchTheRequestedRoute() {
        UnitType.entries.forEach { unit ->
            assertEquals("部门副职", positionTitleOf(unit, 3))
            assertTrue(positionTitleOf(unit, 4).contains("负责人"))
        }
        assertEquals("副科级·部门副职", positionDisplay("副科级", positionTitleOf(UnitType.TOWN, 3)))
        assertEquals("正科级·部门负责人", positionDisplay("正科级", positionTitleOf(UnitType.TOWN, 4)))
        assertEquals("副处级·副县长", positionDisplay("副处级", positionTitleOf(UnitType.TOWN, 5)))
        assertEquals("正处级·县长", positionDisplay("正处级", positionTitleOf(UnitType.TOWN, 6)))
        assertEquals("副厅级·副市长", positionDisplay("副厅级", positionTitleOf(UnitType.TOWN, 7)))
        assertEquals("正厅级·市长", positionDisplay("正厅级", positionTitleOf(UnitType.TOWN, 8)))
    }

    @Test
    fun newGameFirstPromotionShowsFullTitle() {
        val fresh = GameEngine.newGame("阶梯测试", "男", "本科", "乡镇公务员", 7L)
        val status = PromotionEngine.statusOf(fresh)
        assertEquals("科员", status.currentName)
        assertEquals("副科级", status.nextName)
        assertEquals("副科级·乡镇党委副书记", status.nextTitle)
        assertFalse(status.nextTitle!!.contains("主任科员"))
    }

    @Test
    fun legacyRankSaveMigratesToDeputySection() {
        for (legacyIndex in listOf(1, 2)) {
            val base = GameEngine.newGame("旧档", "男", "本科", "机关科员", 3L)
            val state = base.copy(player = base.player.copy(positionIndex = legacyIndex))
            val encoded = GameCodec.encode(state)
                .replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t7")
            val restored = GameCodec.decode(encoded)
            assertNotNull("旧存档必须可读", restored)
            assertEquals(
                "旧版残留职级应迁移到副科级",
                FIRST_GRADE_INDEX,
                restored!!.player.positionIndex
            )
            assertEquals("副科级·乡镇党委副书记", restored.displayPositionTitle)
        }
    }

    @Test
    fun playedCareersNeverUseLegacyRanksOrRegress() {
        var sawFirstPromotion = false
        for (seed in 1L..12L) {
            var state = GameEngine.newGame("履历", "男", "本科", "乡镇公务员", seed)
            var lastReachable = 0
            var step = 0
            while (!state.isFinished && step < 3000) {
                step += 1
                val index = state.player.positionIndex
                // 任何时刻都不应停在旧版残留职级上。
                assertFalse("不应停在残留职级：index=$index", isLegacyRank(index))
                when (state.phase) {
                    GamePhase.EVENT -> state = GameEngine.resolveChoice(state, step % 3)
                    GamePhase.OUTCOME -> state = GameEngine.advance(state)
                    GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                    GamePhase.ENDING -> break
                }
                val next = state.player.positionIndex
                if (next > lastReachable) {
                    if (lastReachable == 0 && next == FIRST_GRADE_INDEX) sawFirstPromotion = true
                    lastReachable = next
                }
            }
            state.log.forEach { entry ->
                assertFalse("晋升记录不应出现旧式职位：${entry.outcome}", entry.outcome.contains("主任科员"))
            }
        }
        assertTrue("长局模拟中应当至少出现一次首次晋升", sawFirstPromotion)
    }

    @Test
    fun reviewExplanationPointsToDeputySectionNotLegacyClerk() {
        val fresh = GameEngine.newGame("说明", "男", "本科", "机关科员", 9L)
        val review = com.shitu.app.model.AnnualReview(
            year = fresh.year, score = 55, grade = "基本称职", summary = "",
            items = emptyList(), promotionText = "", promotionGranted = false
        )
        val explanation = com.shitu.app.game.ReviewExplainer.of(fresh, review)
        assertTrue(
            "考核说明应指向副科级：${explanation.opportunityLine}",
            explanation.opportunityLine.contains("副科级")
        )
        assertFalse(
            "考核说明不得出现旧式职级：${explanation.opportunityLine}",
            explanation.opportunityLine.contains("主任科员")
        )
    }
    @Test
    fun legacyAppointmentNameIsIgnoredAndCleaned() {
        val base = GameEngine.newGame("旧档任命", "男", "本科", "乡镇公务员", 3L)
        val state = base.copy(
            player = base.player.copy(positionIndex = 2, appointmentTitle = "主任科员")
        )
        val encoded = GameCodec.encode(state)
            .replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t7")
        val restored = GameCodec.decode(encoded)
        assertNotNull("旧存档必须可读", restored)
        assertEquals(FIRST_GRADE_INDEX, restored!!.player.positionIndex)
        assertFalse("旧式任命名应视为无效", restored.player.hasExplicitPosition)
        assertEquals("副科级·乡镇党委副书记", restored.displayPositionTitle)
        assertEquals("副科级·乡镇党委副书记", restored.currentPositionNode.display)
        // 重新写出后不应再残留旧式职位名。
        assertFalse(GameCodec.encode(restored).contains("主任科员"))
    }

    @Test
    fun staleLegacyAppointmentTitleNeverDisplaysRegression() {
        val base = GameEngine.newGame("残留", "男", "本科", "机关科员", 4L)
        val state = base.copy(
            player = base.player.copy(positionIndex = 3, appointmentTitle = "乡镇主任科员")
        )
        assertFalse(state.player.hasExplicitPosition)
        assertEquals("副科级·乡镇党委副书记", state.displayPositionTitle)
        assertEquals("乡镇党委副书记", state.currentPositionNode.positionName)
    }
    @Test
    fun promotionRequirementUsesNewMeritBase() {
        // 第一次晋升（副科级）的综合政绩门槛应为 5，而不是旧的隐藏高门槛。
        val requirement = PromotionEngine.requirementList(
            GameEngine.newGame("门槛", "男", "本科", "机关科员", 1L)
        ).firstOrNull { it.label == "综合政绩" }
        assertNotNull(requirement)
        assertEquals(5, requirement!!.required)
    }

    @Test
    fun legacyCareerRecordTextMigratesToFullPosition() {
        val record = com.shitu.app.model.CareerRecord(
            year = 9, quarter = 4, age = 33,
            title = "晋升 主任科员",
            detail = "全部条件已满足，进入 主任科员。",
            kind = com.shitu.app.model.CareerRecordKind.PROMOTION,
            positionTitle = "主任科员",
            positionName = "副主任科员",
            fromPosition = "副主任科员",
            toPosition = "主任科员"
        )
        assertEquals("晋升 副科级·部门副职", record.displayTitle)
        assertEquals("全部条件已满足，进入 副科级·部门副职。", record.displayDetail)
        assertEquals("副科级·部门副职", record.displayPositionLabel)
        assertEquals("副科级·部门副职", record.displayFromPosition)
        assertEquals("副科级·部门副职", record.displayToPosition)
        // 原始数据保持不动，只影响展示。
        assertEquals("晋升 主任科员", record.title)
    }

    @Test
    fun legacyCityStintLabelMigratesForDisplay() {
        val stint = com.shitu.app.model.CityStint(
            cityId = "luoyang",
            cityName = "洛阳",
            province = "河南省",
            positionLabel = "主任科员",
            startAbsQuarter = 0
        )
        assertEquals("副科级·部门副职", stint.displayPositionLabel)
    }

    @Test
    fun migrateLegacyPositionTextLeavesOtherTextUntouched() {
        assertEquals(
            "正常履历文本",
            com.shitu.app.model.migrateLegacyPositionText("正常履历文本")
        )
        assertEquals(
            "晋升 正厅级·市长",
            com.shitu.app.model.migrateLegacyPositionText("晋升 正厅级·市长")
        )
    }

    @Test
    fun newPromotionRecordAvoidsRankOnlyTitle() {
        // 新产生的晋升履历标题必须带具体职位，不能只写职级。
        val base = GameEngine.newGame("履历标题", "男", "本科", "乡镇公务员", 5L)
        val state = base.copy(player = base.player.copy(positionIndex = 4))
        assertTrue(state.displayPositionTitle.contains("·"))
        assertFalse(state.displayPositionTitle.endsWith("级"))
    }
}
