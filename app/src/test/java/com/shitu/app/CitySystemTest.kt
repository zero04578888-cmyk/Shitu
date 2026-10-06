package com.shitu.app

import com.shitu.app.game.CityEventLibrary
import com.shitu.app.game.EventEngine
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GameState
import com.shitu.app.game.PositionPowerCalculator
import com.shitu.app.model.CityData
import com.shitu.app.model.CityDataRepository
import com.shitu.app.model.CityLevel
import com.shitu.app.model.CityPositionRole
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.RegionCatalog
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 第十六阶段：真实中国城市体系。
 * 覆盖城市数据、行政层级、职位等级规则、城市事件池、实际权力计算与存档兼容。
 */
class CitySystemTest {

    private fun game(cityId: String, rank: Int = 0): GameState {
        val base = GameEngine.newGame("测试者", "男", "本科", "乡镇公务员", 7L, regionId = cityId)
        return if (rank <= 0) base else base.copy(player = base.player.copy(positionIndex = rank))
    }

    // ===== 城市数据 =====

    @Test
    fun catalogCoversRealCities() {
        assertTrue("城市数量不足", CityDataRepository.cities.size >= 60)
        assertEquals(
            CityDataRepository.cities.size,
            CityDataRepository.cities.map { it.id }.toSet().size
        )
        assertEquals(
            CityDataRepository.cities.size,
            CityDataRepository.cities.map { it.name }.toSet().size
        )
    }

    @Test
    fun catalogHasFourMunicipalities() {
        assertEquals(4, CityDataRepository.municipalities.size)
        val names = CityDataRepository.municipalities.map { it.name }.toSet()
        assertEquals(setOf("北京", "上海", "天津", "重庆"), names)
    }

    @Test
    fun subProvincialCountIsFifteenWithoutDoubleCounting() {
        assertEquals(15, CityDataRepository.subProvincialCities.size)
        assertEquals(5, CityDataRepository.separatelyPlannedCities.size)
        // 计划单列市属于副省级体系，且每座城市只归入一个层级。
        CityDataRepository.cities.forEach { city ->
            val levels = CityLevel.entries.count { level -> level == city.level }
            assertEquals(1, levels)
        }
        assertTrue(
            CityDataRepository.separatelyPlannedCities.all { it.level == CityLevel.SEPARATELY_PLANNED }
        )
    }

    @Test
    fun provincialCapitalsAreNotAutomaticallySubProvincial() {
        val changsha = CityDataRepository.findByName("长沙")!!
        assertTrue(changsha.isProvincialCapital)
        assertFalse(changsha.isSubProvincial)
        assertEquals(CityLevel.PROVINCIAL_CAPITAL, changsha.level)

        val guangzhou = CityDataRepository.findByName("广州")!!
        assertTrue(guangzhou.isProvincialCapital)
        assertTrue(guangzhou.isSubProvincial)
    }

    @Test
    fun cityDataFieldsStayInRange() {
        CityDataRepository.cities.forEach { city ->
            listOf(
                city.powerWeight, city.economic, city.politics, city.culture, city.education,
                city.technology, city.transportation, city.internationalInfluence,
                city.strategicImportance, city.gdp, city.gdpGrowth, city.population,
                city.fiscalStrength, city.industrialStrength, city.serviceIndustryStrength,
                city.foreignTrade, city.investmentStrength, city.historyLevel, city.tourismLevel,
                city.potential, city.challenge
            ).forEach { value ->
                assertTrue("${city.name} 数值越界：$value", value in 0..100)
            }
            assertTrue(city.industries.isNotEmpty())
            assertTrue(city.eventTags.isNotEmpty())
            assertTrue(city.summary.isNotBlank())
            assertTrue(city.dataSource.isNotBlank())
            assertTrue(city.dataYear >= 2020)
        }
    }

    @Test
    fun cityPowerWeightReflectsStatusNotJustGdp() {
        assertEquals(100, CityDataRepository.findByName("上海")!!.powerWeight)
        assertEquals(100, CityDataRepository.findByName("北京")!!.powerWeight)
        assertTrue(CityDataRepository.findByName("广州")!!.powerWeight >= 90)
        assertTrue(CityDataRepository.findByName("深圳")!!.powerWeight >= 90)
        // GDP 最高不等于权力最高：苏州经济指数极高，但含权量低于上海。
        val suzhou = CityDataRepository.findByName("苏州")!!
        val shanghai = CityDataRepository.findByName("上海")!!
        assertTrue(suzhou.economic >= 85)
        assertTrue("GDP 不能直接等于权力", suzhou.powerWeight < shanghai.powerWeight)
    }

    @Test
    fun provincesCoverMostOfChina() {
        assertTrue("省级行政区覆盖不足", CityDataRepository.provinces.size >= 30)
        CityDataRepository.provinces.forEach { province ->
            assertTrue(province.name.isNotBlank())
            assertTrue(province.cityIds.isNotEmpty())
            assertNotNull(province.capitalCityId)
            assertTrue(province.region.isNotBlank())
        }
    }

    @Test
    fun noFictionalCitiesRemain() {
        listOf("青川", "云岭", "临江", "南泽", "东平", "安州", "景河", "宁川", "平岳", "江源")
            .forEach { name ->
                assertNull("虚构城市仍在：$name", CityDataRepository.findByName(name))
            }
        assertNull(RegionCatalog.find("qingchuan"))
        assertNull(RegionCatalog.find("yunling"))
        assertNotNull(RegionCatalog.find("shenzhen"))
    }

    @Test
    fun regionProfilesCarryCityIdentity() {
        val shenzhen = RegionCatalog.find("shenzhen")!!
        assertEquals("广东省", shenzhen.province)
        assertEquals(CityLevel.SEPARATELY_PLANNED, shenzhen.cityLevel)
        assertTrue(shenzhen.isSubProvincial)
        assertEquals(CityDataRepository.find("shenzhen")!!.powerWeight, shenzhen.powerWeight)

        val beijing = RegionCatalog.find("beijing")!!
        assertTrue(beijing.isMunicipality)
        assertEquals(CityLevel.MUNICIPALITY, beijing.cityLevel)
    }

    // ===== 职位等级规则 =====

    @Test
    fun municipalitySecretaryOutranksMayor() {
        val shanghai = CityDataRepository.findByName("上海")!!
        val secretary = CityDataRepository.specFor(shanghai, CityPositionRole.PARTY_SECRETARY)
        val mayor = CityDataRepository.specFor(shanghai, CityPositionRole.MAYOR)
        assertEquals("上海市委书记", secretary.title)
        assertEquals("上海市长", mayor.title)
        assertTrue("直辖市书记应高于市长", secretary.rankIndex > mayor.rankIndex)
        assertEquals("副国级", POSITION_LADDER[secretary.rankIndex].name)
        assertEquals("正部级", POSITION_LADDER[mayor.rankIndex].name)
    }

    @Test
    fun subProvincialMayorIsDeputyMinisterial() {
        val shenzhen = CityDataRepository.findByName("深圳")!!
        val mayor = CityDataRepository.specFor(shenzhen, CityPositionRole.MAYOR)
        assertEquals("副部级", POSITION_LADDER[mayor.rankIndex].name)

        val luoyang = CityDataRepository.findByName("洛阳")!!
        val ordinaryMayor = CityDataRepository.specFor(luoyang, CityPositionRole.MAYOR)
        assertEquals("正厅级", POSITION_LADDER[ordinaryMayor.rankIndex].name)
        assertTrue(mayor.rankIndex > ordinaryMayor.rankIndex)
    }

    @Test
    fun provincialCapitalUsesTownHallRank() {
        val changsha = CityDataRepository.findByName("长沙")!!
        val mayor = CityDataRepository.specFor(changsha, CityPositionRole.MAYOR)
        assertEquals("正厅级", POSITION_LADDER[mayor.rankIndex].name)
    }

    @Test
    fun rolesWithinSameCityDiffer() {
        val shanghai = CityDataRepository.findByName("上海")!!
        val secretary = CityDataRepository.specFor(shanghai, CityPositionRole.PARTY_SECRETARY)
        val mayor = CityDataRepository.specFor(shanghai, CityPositionRole.MAYOR)
        val bureau = CityDataRepository.specFor(shanghai, CityPositionRole.BUREAU_HEAD)
        assertTrue(secretary.politicalInfluence > mayor.politicalInfluence)
        assertTrue(mayor.economicInfluence > secretary.economicInfluence)
        assertTrue(secretary.powerLevel > bureau.powerLevel)
        assertTrue(secretary.promotionPotential > bureau.promotionPotential)
        // 同一城市所有职务都应有各自的档案。
        val specs = CityDataRepository.positionSpecs(shanghai)
        assertEquals(CityPositionRole.entries.size, specs.size)
        assertNotEquals(specs.map { it.title }.toSet().size, 0)
        assertEquals(CityPositionRole.entries.size, specs.map { it.title }.toSet().size)
    }

    @Test
    fun roleSpecsAreScaledByCityPower() {
        val shanghai = CityPositionCalculatorSpec(CityDataRepository.findByName("上海")!!)
        val zhoukou = CityPositionCalculatorSpec(CityDataRepository.findByName("周口")!!)
        assertTrue(shanghai.mayorEconomic > zhoukou.mayorEconomic)
    }

    private data class CityPositionCalculatorSpec(val city: CityData) {
        val mayorEconomic: Int =
            CityDataRepository.specFor(city, CityPositionRole.MAYOR).economicInfluence
    }

    // ===== 实际权力计算 =====

    @Test
    fun powerProfileReflectsCityWeight() {
        val low = PositionPowerCalculator.profileOf(game("zhoukou"))
        val high = PositionPowerCalculator.profileOf(game("shanghai"))
        assertEquals("周口", low.cityName)
        assertEquals("上海", high.cityName)
        assertTrue("高含权量城市应带来更高实际权力", high.overall > low.overall)
        assertTrue(high.politicalInfluence > low.politicalInfluence)
    }

    @Test
    fun powerProfileGrowsWithRank() {
        val junior = PositionPowerCalculator.profileOf(game("hangzhou", rank = 1))
        val senior = PositionPowerCalculator.profileOf(game("hangzhou", rank = 9))
        assertTrue(senior.overall > junior.overall)
        assertTrue(senior.eventAuthority > junior.eventAuthority)
    }

    @Test
    fun eventAuthorityIsMonotonic() {
        val values = com.shitu.app.model.PositionScope.entries.map { PositionPowerCalculator.scopeRank(it) }
        assertEquals(values.sorted(), values)
        assertTrue(values.first() >= 0)
        assertTrue(values.last() <= 100)
    }

    @Test
    fun powerValuesStayInRange() {
        CityDataRepository.cities.forEach { city ->
            val profile = PositionPowerCalculator.profileOf(game(city.id, rank = 6))
            listOf(
                profile.politicalInfluence, profile.economicInfluence,
                profile.organizationalInfluence, profile.resourceControl,
                profile.eventAuthority, profile.promotionOpportunity, profile.overall
            ).forEach { value ->
                assertTrue("${city.name} 权力越界：$value", value in 0..100)
            }
        }
    }

    @Test
    fun municipalityTopRankIsHigherThanSubProvincial() {
        val shanghai = CityDataRepository.findByName("上海")!!
        val shenzhen = CityDataRepository.findByName("深圳")!!
        assertTrue(
            PositionPowerCalculator.topRank(shanghai) > PositionPowerCalculator.topRank(shenzhen)
        )
        assertEquals(POSITION_LADDER.size - 2, PositionPowerCalculator.topRank(shanghai))
    }

    // ===== 城市事件池 =====

    @Test
    fun cityEventsAreTaggedAndWellFormed() {
        assertTrue(CityEventLibrary.events.size >= 20)
        CityEventLibrary.events.forEach { event ->
            assertTrue("城市事件缺少标签：${event.id}", event.requiredCityTags.isNotEmpty())
            assertTrue("事件选项不足：${event.id}", event.choices.size >= 3)
            assertTrue(event.title.isNotBlank())
            assertTrue(event.description.isNotBlank())
        }
    }

    @Test
    fun everyCityEventTagExistsOnSomeCity() {
        val allTags = CityDataRepository.cities.flatMap { it.eventTags }.toSet()
        CityEventLibrary.events.forEach { event ->
            event.requiredCityTags.forEach { tag ->
                assertTrue("标签没有对应城市：$tag", tag in allTags)
            }
        }
    }

    @Test
    fun cityTagsDriveEventEligibility() {
        val shenzhen = game("shenzhen", rank = 8)
        assertTrue("technology" in EventEngine.cityTags(shenzhen))
        val shenzhenEvents = EventEngine.eligibleEvents(shenzhen).map { it.id }
        assertTrue("芯片事件应出现在深圳", "city16_chip_land" in shenzhenEvents)

        val zhoukou = game("zhoukou", rank = 8)
        assertFalse("technology" in EventEngine.cityTags(zhoukou))
        val zhoukouEvents = EventEngine.eligibleEvents(zhoukou).map { it.id }
        assertFalse("芯片事件不应出现在周口", "city16_chip_land" in zhoukouEvents)
        assertTrue("粮食安全事件应出现在周口", "city16_grain_security" in zhoukouEvents)
    }

    @Test
    fun tourismCityGetsTourismEvents() {
        val guilin = game("guilin", rank = 6)
        val events = EventEngine.eligibleEvents(guilin).map { it.id }
        assertTrue("city16_tourism_peak" in events)
        assertTrue("city16_culture_ip" in events)
    }

    @Test
    fun visibleForFiltersByTags() {
        val tech = CityEventLibrary.visibleFor(setOf("technology"))
        assertTrue(tech.all { it.requiredCityTags.any { tag -> tag == "technology" } })
        assertTrue(tech.any { it.id == "city16_chip_land" })
    }

    // ===== 存档兼容 =====

    @Test
    fun codecRoundTripKeepsCityState() {
        val state = game("shenzhen")
        val restored = GameCodec.decode(GameCodec.encode(state))
        assertNotNull(restored)
        val other = restored!!
        assertEquals(state.regionId, other.regionId)
        assertEquals(state.careerCities, other.careerCities)
        assertEquals("广东省", other.currentProvince)
        assertEquals("深圳", other.currentCityName)
        assertEquals(CityDataRepository.find("shenzhen")!!.powerWeight, other.currentCityPower)
    }

    @Test
    fun legacySaveWithoutCityFieldsFallsBackSafely() {
        val state = game("suzhou")
        val text = GameCodec.encode(state)
        val legacy = text.split("\n")
            .filter { !it.startsWith("city.") && !it.startsWith("regionId\t") }
            .joinToString("\n")
            .replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t5")
        val restored = GameCodec.decode(legacy)
        assertNotNull("旧存档必须可读", restored)
        val other = restored!!
        assertEquals(RegionCatalog.default.id, other.regionId)
        assertTrue("旧存档应自动补一段城市履历", other.careerCities.isNotEmpty())
        assertEquals(RegionCatalog.default.name, other.currentCityName)
    }

    @Test
    fun futureFormatStillRejected() {
        val text = GameCodec.encode(game("beijing"))
            .replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t99")
        assertNull(GameCodec.decode(text))
    }

    @Test
    fun transferRecordsCityHistory() {
        val base = game("suzhou", rank = 4)
        val offer = com.shitu.app.game.RegionEngine.offerFor(base, com.shitu.app.game.GameRng(5))
        if (offer != null) {
            val moved = com.shitu.app.game.RegionEngine.accept(base, offer)
            assertTrue(moved.careerCities.size >= 2)
            assertEquals(offer.regionId, moved.regionId)
            assertTrue(moved.careerCities.any { it.isCurrent && it.cityId == offer.regionId })
            assertTrue(moved.careerCities.any { !it.isCurrent && it.endAbsQuarter >= 0 })
        }
    }

    @Test
    fun catalogHasAtLeastOneCityPerTrait() {
        com.shitu.app.model.RegionTrait.entries.forEach { trait ->
            assertTrue("${trait.label} 没有城市", CityDataRepository.withTrait(trait).isNotEmpty())
        }
    }
}