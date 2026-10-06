package com.shitu.app

import com.shitu.app.game.DemotionEngine
import com.shitu.app.game.EventEngine
import com.shitu.app.game.EventLibrary
import com.shitu.app.util.GameCodec
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GameState
import com.shitu.app.game.GovernanceEngine
import com.shitu.app.game.PositionEngine
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.Attributes
import com.shitu.app.model.CareerRecord
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.Merit
import com.shitu.app.model.MeritKind
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.UnitType
import com.shitu.app.model.isTopPosition
import com.shitu.app.model.migrateLegacyPositionIndex
import com.shitu.app.model.nextPromotionIndex
import com.shitu.app.model.previousPromotionIndex
import com.shitu.app.model.positionAt
import com.shitu.app.model.positionTitleOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 第十五阶段：完整职位体系。
 *
 * 覆盖十三级阶梯、正式职位名称、职位属性进入游戏逻辑、
 * 晋升条件（含威望 / 财富 / 履历）、晋升进度、降职、事件门控与存档兼容。
 */
class PhaseSixteenTest {

    private fun game(seed: Long = 11L): GameState =
        GameEngine.newGame("职位测试者", "男", "硕士", "机关科员", seed)

    private fun GameState.at(index: Int): GameState =
        copy(player = player.copy(positionIndex = index))

    private fun GameState.wealth(value: Int): GameState =
        copy(player = player.copy(attributes = player.attributes.withValue(Attr.WEALTH, value)))

    private fun GameState.reputation(value: Int): GameState =
        copy(player = player.copy(attributes = player.attributes.withValue(Attr.REPUTATION, value)))

    private fun GameState.fullMerit(value: Int): GameState {
        var merit: Merit = governance.merit
        MeritKind.entries.forEach { merit = merit.withValue(it, value) }
        return copy(governance = governance.copy(merit = merit))
    }

    private fun strongAttributes(): Attributes = Attributes(
        work = 96, ability = 94, politics = 94, writing = 88,
        coordination = 90, masses = 94, leaderTrust = 96, colleagues = 90,
        reputation = 90, integrityRisk = 4, health = 88, energy = 88,
        family = 80, wealth = 88
    )

    // ===== 阶梯与职位名称 =====

    @Test
    fun ladderKeepsLegacyLevelsButSkipsThemForPromotion() {
        assertEquals(13, POSITION_LADDER.size)
        val expected = listOf(
            "科员", "副主任科员", "主任科员", "副科级", "正科级",
            "副处级", "正处级", "副厅级", "正厅级", "副部级",
            "正部级", "副国级", "正国级"
        )
        assertEquals(expected, POSITION_LADDER.map { it.name })
        POSITION_LADDER.forEachIndexed { index, level ->
            assertEquals(index, level.index)
            assertEquals(index + 1, level.powerLevel)
            assertTrue("职位名称不能为空：$index", level.title.isNotBlank())
        }
        // 旧版残留职级只为兼容存档保留，不进入晋升路径。
        assertTrue(POSITION_LADDER[1].legacy)
        assertTrue(POSITION_LADDER[2].legacy)
        val effective = POSITION_LADDER.filterNot { it.legacy }
        assertEquals(
            listOf(0, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
            effective.map { it.index }
        )
        // 有效档位连续递增，杜绝「职级倒退 / 重复」。
        val tiers = effective.map { it.tier }
        assertEquals(tiers.sorted(), tiers)
        assertEquals(tiers.size, tiers.toSet().size)
        assertEquals(3, nextPromotionIndex(0))
        assertEquals(12, nextPromotionIndex(11))
        assertEquals(12, nextPromotionIndex(12))
        assertEquals(0, previousPromotionIndex(3))
        assertEquals(11, previousPromotionIndex(12))
    }

    @Test
    fun powerAndIncomeGrowWithRank() {
        POSITION_LADDER.zipWithNext().forEach { (lower, higher) ->
            assertTrue("权力等级应当递增", higher.powerLevel > lower.powerLevel)
            assertTrue("收入应当递增", higher.monthlyIncome >= lower.monthlyIncome)
            assertTrue("权限域不应倒退", higher.scope.ordinal >= lower.scope.ordinal)
        }
        assertEquals(POSITION_LADDER.last().index, POSITION_LADDER.size - 1)
        assertTrue(isTopPosition(POSITION_LADDER.size - 1))
        assertFalse(isTopPosition(0))
    }

    @Test
    fun positionTitlesAreSpecificAndNotJustLevels() {
        UnitType.entries.forEach { unit ->
            assertTrue(positionTitleOf(unit, 0).contains("科员"))
            assertTrue(positionTitleOf(unit, 3).contains("副职"))
            assertTrue(positionTitleOf(unit, 4).contains("负责人"))
        }
        assertTrue(positionTitleOf(UnitType.COUNTY, 5).contains("副县长"))
        assertTrue(positionTitleOf(UnitType.COUNTY, 6).contains("县长"))
        assertTrue(positionTitleOf(UnitType.COUNTY, 7).contains("副市长"))
        assertTrue(positionTitleOf(UnitType.COUNTY, 8).contains("市长"))
        assertTrue(positionTitleOf(UnitType.COUNTY, 10).contains("省级"))
        assertTrue(positionTitleOf(UnitType.COUNTY, 12).contains("国家"))
    }

    // ===== 职位属性进入游戏逻辑 =====

    @Test
    fun salaryIsDrivenByLadderData() {
        POSITION_LADDER.forEach { level ->
            assertEquals(level.monthlyIncome, PositionEngine.salaryOf(level.index))
            assertEquals(level.monthlyIncome, GovernanceEngine.salaryOf(level.index))
        }
    }

    @Test
    fun tenureEffectsOnlyApplyFromHighRanks() {
        assertTrue(PositionEngine.tenureEffectOf(0).isEmpty)
        assertTrue(PositionEngine.tenureEffectOf(3).isEmpty)
        assertTrue(PositionEngine.tenureEffectOf(7).reputation > 0)
        assertTrue(PositionEngine.tenureEffectOf(9).merit > 0)
    }

    @Test
    fun tenureApplicationKeepsValuesBounded() {
        val state = game().at(9).fullMerit(98)
        val applied = PositionEngine.applyTenure(state)
        applied.governance.merit.let { merit ->
            MeritKind.entries.forEach { kind ->
                assertTrue("政绩越界：${merit.valueOf(kind)}", merit.valueOf(kind) in 0..100)
            }
        }
        assertTrue(applied.governance.jurisdiction.approval in 0..100)
    }

    // ===== 晋升条件 =====

    @Test
    fun newRequirementsBlockPromotionWhenMissing() {
        val base = game().at(7)
            .copy(player = game().player.copy(
                positionIndex = 7,
                yearsInPosition = 8,
                attributes = strongAttributes().copy(wealth = 0)
            ))
            .fullMerit(90)
            .reputation(90)
        val blockers = PromotionEngine.blockersOf(base, 95)
        assertTrue(
            "财富不足应当成为阻碍：$blockers",
            blockers.any { it.contains("个人积累") }
        )

        val poorReputation = game().at(7).copy(
            player = game().player.copy(
                positionIndex = 7,
                yearsInPosition = 8,
                attributes = Attributes(
                    work = 96, ability = 94, politics = 94, writing = 88,
                    coordination = 90, masses = 94, leaderTrust = 6, colleagues = 6,
                    reputation = 0, integrityRisk = 4, health = 88, energy = 88,
                    family = 80, wealth = 90
                )
            )
        )
        val repBlockers = PromotionEngine.blockersOf(poorReputation, 95)
        assertTrue(
            "威望不足应当成为阻碍：$repBlockers",
            repBlockers.any { it.contains("威望") }
        )
    }

    @Test
    fun topPositionCannotBePromotedFurther() {
        val top = game().at(POSITION_LADDER.size - 1)
        val result = PromotionEngine.evaluate(top, 95, com.shitu.app.game.GameRng(1L))
        assertFalse(result.applicable)
        val status = PromotionEngine.statusOf(top)
        assertTrue(status.top)
        assertEquals(100, status.progressPercent)
        assertTrue(status.requirements.isEmpty())
    }

    @Test
    fun statusReportsProgressNextTitleAndRequirements() {
        val status = PromotionEngine.statusOf(game())
        assertFalse(status.top)
        assertEquals("科员", status.currentName)
        assertEquals("副科级", status.nextName)
        assertTrue(status.nextTitle!!.isNotBlank())
        assertTrue(status.progressPercent in 0..99)
        assertTrue(status.requirements.isNotEmpty())
        assertTrue(status.requirements.all { it.current >= 0 && it.required >= 0 })
    }

    @Test
    fun requirementListExposesHighRankGates() {
        val state = game().at(6)
        val labels = PromotionEngine.requirementList(state).map { it.label }
        assertTrue("高职级应展示威望要求：$labels", labels.contains("威望"))
        assertTrue("高职级应展示履历要求：$labels", labels.contains("履历事件"))
    }

    @Test
    fun betterStateMakesMoreProgress() {
        val low = game()
        val high = game().copy(
            player = game().player.copy(
                positionIndex = 1,
                yearsInPosition = 8,
                attributes = strongAttributes()
            )
        ).fullMerit(80)
        assertTrue(
            "条件更好时晋升进度应当更高",
            PromotionEngine.promotionProgress(high) > PromotionEngine.promotionProgress(low)
        )
        PromotionEngine.statusOf(high).let { status ->
            assertTrue(status.progressPercent in 0..99)
        }
    }

    // ===== 降职 =====

    @Test
    fun demotionNeverHappensAtBaseOrOnNormalGrades() {
        val base = game()
        org.junit.Assert.assertNull(
            DemotionEngine.reasonFor(base, "不称职", 40, 3)
        )
        val mid = game().at(5)
        org.junit.Assert.assertNull(DemotionEngine.reasonFor(mid, "称职", 70, 0))
        org.junit.Assert.assertNull(DemotionEngine.reasonFor(mid, "良好", 80, 0))
    }

    @Test
    fun demotionTriggersOnSevereFailureOrRisk() {
        val risky = game().at(5).copy(
            player = game().player.copy(
                positionIndex = 5,
                attributes = game().player.attributes.copy(integrityRisk = 95)
            ),
            relationship = game().relationship.withRisk(85)
        )
        assertNotNull(
            "廉政与官场风险叠加应当触发处理",
            DemotionEngine.reasonFor(risky, "称职", 60, 0)
        )

        val failing = game().at(5)
        assertNotNull(
            "连续不称职应当触发降职",
            DemotionEngine.reasonFor(failing, "不称职", 40, 3)
        )
    }

    // ===== 事件权限门控 =====

    @Test
    fun highRankEventsAreLockedUntilPromotion() {
        val event = EventLibrary.find("p15_county_major_project")
        assertNotNull(event)
        assertEquals(6, event!!.minPositionIndex)
        assertTrue(event.requiredRank == event.minPositionIndex)

        val low = game()
        assertFalse(
            "低职级不应触发高职级事件",
            EventEngine.eligibleEvents(low).any { it.id == event.id }
        )

        val high = game().at(6)
        assertTrue(
            "晋升到对应职级后应当解锁该事件",
            EventEngine.eligibleEvents(high).any { it.id == event.id }
        )
        assertTrue(PositionEngine.canHandleEvent(high, event.minPositionIndex, event.requiredScope))
        assertFalse(PositionEngine.canHandleEvent(low, event.minPositionIndex, event.requiredScope))
    }

    @Test
    fun positionEventLibraryCoversEveryHighScope() {
        val high = EventLibrary.events.filter { it.minPositionIndex >= 6 }
        assertTrue("应当有高职级专属事件", high.size >= 8)
        assertTrue(high.any { it.requiredScope == com.shitu.app.model.PositionScope.NATIONAL })
        assertTrue(high.any { it.requiredScope == com.shitu.app.model.PositionScope.PROVINCE })
        assertEquals(high.size, high.map { it.id }.toSet().size)
    }

    // ===== 存档兼容 =====

    @Test
    fun codecRoundTripsPositionFields() {
        val record = CareerRecord(
            year = 3, quarter = 4, age = 27,
            title = "晋升 副科级", detail = "进入副科级。",
            kind = CareerRecordKind.PROMOTION,
            positionName = "副科级",
            regionName = "市辖区",
            fromPosition = "主任科员",
            toPosition = "副科级"
        )
        val base = game()
        val state = base.copy(
            player = base.player.copy(
                positionIndex = 3,
                positionStartAbs = 7,
                yearsInPosition = 2
            ),
            demotionCount = 1,
            career = base.career.withRecord(record)
        )
        val decoded = GameCodec.decode(GameCodec.encode(state))
        assertNotNull(decoded)
        val other = decoded!!
        assertEquals(3, other.player.positionIndex)
        assertEquals(7, other.player.positionStartAbs)
        assertEquals(1, other.demotionCount)
        val restoredRecord = other.career.records.last { it.kind == CareerRecordKind.PROMOTION }
        assertEquals("主任科员", restoredRecord.fromPosition)
        assertEquals("副科级", restoredRecord.toPosition)
        assertEquals("副科级", other.player.positionName)
        assertTrue(other.player.positionTitle.isNotBlank())
    }

    @Test
    fun legacySaveRemapsOldNineLevelLadder() {
        val text = GameCodec.encode(game())
            .replaceFirst("player.positionIndex\t0", "player.positionIndex\t5")
            .replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t5")
        val decoded = GameCodec.decode(text)
        assertNotNull("旧存档仍应可以读取", decoded)
        assertEquals("旧「副科级」应映射到新阶梯的副科级", 3, decoded!!.player.positionIndex)
    }

    @Test
    fun legacyMappingIsMonotonicAndBounded() {
        val mapped = (0..8).map { migrateLegacyPositionIndex(it) }
        assertEquals(mapped.sorted(), mapped)
        assertTrue(mapped.all { it in 0 until POSITION_LADDER.size })
        assertEquals(0, migrateLegacyPositionIndex(0))
        assertEquals(6, migrateLegacyPositionIndex(8))
        assertEquals(6, migrateLegacyPositionIndex(99))
        assertEquals(0, migrateLegacyPositionIndex(-5))
    }

    @Test
    fun promotionCeremonyRecordsFromAndToPositions() {
        val state = game()
        val withRecord = state.copy(
            player = state.player.copy(positionIndex = 3, positionStartAbs = 0),
            career = state.career
        )
        val updated = com.shitu.app.game.CareerEngine.record(
            withRecord,
            CareerRecordKind.PROMOTION,
            "晋升 副科级",
            "经研究与考察。",
            fromPosition = "科员",
            toPosition = "副科级"
        )
        val record = updated.career.records.last()
        assertEquals("科员", record.fromPosition)
        assertEquals("副科级", record.toPosition)
        assertEquals("副科级", updated.player.positionName)
        assertTrue(positionAt(3).title.isNotBlank())
    }
}