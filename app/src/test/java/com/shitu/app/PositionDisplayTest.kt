package com.shitu.app

import com.shitu.app.game.CareerEngine
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.CityDataRepository
import com.shitu.app.model.CityPositionRole
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.model.PositionCatalog
import com.shitu.app.model.UnitType
import com.shitu.app.model.positionDisplay
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 职位显示系统：所有界面统一显示「职级·具体职位」，例如「正厅级·市长」。
 *
 * 覆盖：显示拼接、城市行政层级联动、同职级多职位、晋升选择、履历记录与存档兼容。
 */
class PositionDisplayTest {

    private fun game(cityId: String, rank: Int = 0): GameState {
        val base = GameEngine.newGame("职位测试", "男", "本科", "乡镇公务员", 11L, regionId = cityId)
        return if (rank <= 0) base else base.copy(player = base.player.copy(positionIndex = rank))
    }

    // ===== 显示格式 =====

    @Test
    fun displayCombinesRankAndPositionName() {
        assertEquals("正厅级·市长", positionDisplay("正厅级", "市长"))
        assertEquals("副厅级·副市长", positionDisplay("副厅级", "副市长"))
        assertEquals("正处级·县长", positionDisplay("正处级", "县长"))
        assertEquals("副部级·市委书记", positionDisplay("副部级", "市委书记"))
    }

    @Test
    fun displayDoesNotDuplicateBaseLevels() {
        assertEquals("副主任科员", positionDisplay("副主任科员", "副主任科员"))
        assertEquals("主任科员", positionDisplay("主任科员", "主任科员"))
        assertEquals("正科级", positionDisplay("正科级", ""))
        assertEquals("正科级", positionDisplay("正科级", "   "))
    }

    // ===== 城市行政层级联动 =====

    @Test
    fun municipalitySecretaryOutranksMayor() {
        val shanghai = CityDataRepository.findOrDefault("shanghai")
        val secretary = PositionCatalog.cityNode(shanghai, CityPositionRole.PARTY_SECRETARY)
        val mayor = PositionCatalog.cityNode(shanghai, CityPositionRole.MAYOR)
        assertEquals("副国级·市委书记", secretary.display)
        assertEquals("正部级·市长", mayor.display)
        assertTrue("市委书记职级必须高于市长", secretary.rankIndex > mayor.rankIndex)
    }

    @Test
    fun subProvincialCitiesBothDeputyMinisterial() {
        val shenzhen = CityDataRepository.findOrDefault("shenzhen")
        val hangzhou = CityDataRepository.findOrDefault("hangzhou")
        val guangzhou = CityDataRepository.findOrDefault("guangzhou")
        listOf(shenzhen, hangzhou, guangzhou).forEach { city ->
            assertEquals("副部级·市委书记", PositionCatalog.cityNode(city, CityPositionRole.PARTY_SECRETARY).display)
            assertEquals("副部级·市长", PositionCatalog.cityNode(city, CityPositionRole.MAYOR).display)
        }
    }

    @Test
    fun ordinaryCitiesArePrefectureLevel() {
        val suzhou = CityDataRepository.findOrDefault("suzhou")
        assertEquals("正厅级·市委书记", PositionCatalog.cityNode(suzhou, CityPositionRole.PARTY_SECRETARY).display)
        assertEquals("正厅级·市长", PositionCatalog.cityNode(suzhou, CityPositionRole.MAYOR).display)
    }

    // ===== 同一职级多个职位 =====

    @Test
    fun oneRankAllowsMultipleConcretePositions() {
        val options = PositionCatalog.promotionOptions(8, "suzhou", UnitType.CITY)
        assertTrue("正厅级应提供多个具体职位", options.size >= 2)
        assertTrue(options.any { it.displayTitle == "正厅级·市长" })
        assertTrue(options.any { it.displayTitle == "正厅级·市委书记" })
        assertEquals(options.size, options.map { it.title }.toSet().size)
    }

    @Test
    fun promotionOptionsFollowCityLevel() {
        val shanghaiOptions = PromotionEngine.eligibleOptions(game("shanghai", 9), 90)
        assertTrue(
            "上海晋升正部级时应提供「正部级·市长」：${shanghaiOptions.map { it.displayTitle }}",
            shanghaiOptions.any { it.displayTitle == "正部级·市长" }
        )
        assertTrue(shanghaiOptions.all { it.rankIndex == 10 })

        val shenzhenOptions = PromotionEngine.eligibleOptions(game("shenzhen", 8), 90)
        assertTrue(shenzhenOptions.any { it.displayTitle == "副部级·市委书记" })
        assertTrue(shenzhenOptions.any { it.displayTitle == "副部级·市长" })
    }

    // ===== 当前职位显示 =====

    @Test
    fun currentPositionFallsBackToCityDefault() {
        val state = game("suzhou", 8)
        assertTrue("默认职位应带具体职位名", state.displayPositionTitle.contains("·"))
        assertEquals("正厅级·市委书记", state.displayPositionTitle)
    }

    @Test
    fun explicitAppointmentDrivesDisplayTitle() {
        val base = game("suzhou", 8)
        val mayor = base.copy(player = base.player.copy(appointmentTitle = "市长"))
        assertEquals("正厅级·市长", mayor.displayPositionTitle)
        assertEquals("正厅级", mayor.player.currentRank)
        assertEquals("市长", mayor.player.currentPositionName)
    }

    // ===== 履历 =====

    @Test
    fun careerRecordKeepsFullPositionTitle() {
        val state = game("suzhou", 8)
        val recorded = CareerEngine.record(state, CareerRecordKind.PROMOTION, "晋升", "任命")
        val record = recorded.career.records.last()
        assertEquals(state.displayPositionTitle, record.positionTitle)
        assertTrue(record.positionTitle.contains("·"))
    }

    // ===== 晋升后保存具体职位 =====

    private fun promotionReady(): GameState {
        val started = game("suzhou", 2)
        val base = started.copy(player = started.player.copy(yearsInPosition = 4))
        var merit = base.governance.merit
        MeritKind.entries.forEach { merit = merit.withValue(it, 70) }
        val governed = base.copy(
            governance = base.governance.copy(
                merit = merit,
                jurisdiction = base.governance.jurisdiction.withValue(Indicator.APPROVAL, 70)
            ),
            reviews = listOf(
                com.shitu.app.model.AnnualReview(
                    year = base.year, score = 90, grade = "优秀", summary = "测试",
                    items = emptyList(), promotionText = "", promotionGranted = false
                )
            )
        )
        var attrs = governed.player.attributes
        listOf(
            Attr.WORK to 95, Attr.ABILITY to 92, Attr.POLITICS to 92,
            Attr.LEADER_TRUST to 95, Attr.MASSES to 92, Attr.INTEGRITY_RISK to 5
        ).forEach { (key, value) -> attrs = attrs.withValue(key, value) }
        return governed.copy(player = governed.player.copy(attributes = attrs))
    }

    @Test
    fun promotionStoresConcretePositionAndFullTitle() {
        val ready = promotionReady()
        assertTrue(PromotionEngine.isEligible(ready))
        val promoted = GameEngine.promoteNow(ready)
        assertEquals(com.shitu.app.model.nextPromotionIndex(ready.player.positionIndex), promoted.player.positionIndex)
        assertTrue("必须记录具体职位 id", promoted.player.appointmentId.isNotBlank())
        assertTrue("必须记录具体职位名", promoted.player.appointmentTitle.isNotBlank())
        assertEquals(promoted.player.appointmentTitle, promoted.currentPositionName)
        assertTrue("晋升后必须显示「职级·具体职位」", promoted.displayPositionTitle.endsWith("·" + promoted.player.appointmentTitle))
        assertTrue(promoted.currentPositionNode.id.isNotBlank())
    }

    @Test
    fun promotionLogShowsFromAndToFullTitles() {
        val promoted = GameEngine.promoteNow(promotionReady())
        val log = promoted.log.last()
        assertTrue("晋升记录必须写清原职位", log.outcome.contains("主任科员"))
        assertTrue("晋升记录必须写清新职位", log.outcome.contains(promoted.displayPositionTitle))
        assertFalse("不应只写职级", log.outcome.contains("晋升为正科级。"))
    }

    @Test
    fun promotedPositionSurvivesSaveAndReload() {
        val promoted = GameEngine.promoteNow(promotionReady())
        val restored = GameCodec.decode(GameCodec.encode(promoted))
        assertNotNull(restored)
        assertEquals(promoted.player.appointmentId, restored!!.player.appointmentId)
        assertEquals(promoted.player.appointmentTitle, restored.player.appointmentTitle)
        assertEquals(promoted.displayPositionTitle, restored.displayPositionTitle)
    }

    @Test
    fun legacySaveWithoutConcretePositionStillShowsFullTitle() {
        val promoted = GameEngine.promoteNow(promotionReady())
        val legacy = GameCodec.encode(promoted).lines()
            .filterNot {
                it.startsWith("player.appointmentId") ||
                    it.startsWith("player.appointmentTitle") ||
                    it.startsWith("player.appointmentCityId")
            }
            .joinToString("\n")
        val restored = GameCodec.decode(legacy)
        assertNotNull("旧存档必须可读", restored)
        assertFalse("旧存档没有具体职位，应视为默认职位", restored!!.player.hasExplicitPosition)
        assertTrue("旧存档也要生成完整职位名", restored.displayPositionTitle.contains("·"))
        assertTrue(restored.displayPositionTitle.contains(restored.player.currentRank))
    }

    // ===== 统一命名 =====

    @Test
    fun namingIsConsistentAcrossStateAndSave() {
        val state = game("suzhou", 8)
        assertEquals(state.player.positionName, state.currentRank)
        assertEquals(state.player.positionName, state.player.currentRank)
        assertEquals(positionDisplay(state.currentRank, state.currentPositionName), state.displayPositionTitle)
        val save = com.shitu.app.game.GameSave(state = state)
        assertEquals(state.displayPositionTitle, save.displayPositionTitle)
        assertTrue(save.summaryLine().contains(state.displayPositionTitle))
    }

    @Test
    fun currentPositionIdAndNodeAreConsistent() {
        val state = game("hangzhou", 9).copy(player = game("hangzhou", 9).player.copy(appointmentTitle = "市长"))
        assertEquals("hangzhou#9", state.currentPositionId)
        assertEquals("副部级·市长", state.currentPositionNode.display)
        assertTrue(state.currentPositionId.startsWith(state.currentCityId))
    }

    // ===== 人生档案 =====

    @Test
    fun lifeArchiveKeepsFullPositionTitle() {
        val ended = game("suzhou", 8).copy(
            ending = com.shitu.app.model.Ending(
                id = "test_ending", title = "测试结局", category = "测试",
                description = "", highlights = emptyList(), tier = "T"
            )
        )
        val record = com.shitu.app.game.ArchiveFactory.lifeRecordOf(ended)
        assertNotNull(record)
        assertEquals("正厅级·市委书记", record!!.positionTitle)

        val archive = com.shitu.app.model.LifeArchive().appended(record)
        val restored = com.shitu.app.util.ArchiveCodec.decode(com.shitu.app.util.ArchiveCodec.encode(archive))
        assertNotNull(restored)
        assertEquals("正厅级·市委书记", restored!!.records.last().positionTitle)
    }
}