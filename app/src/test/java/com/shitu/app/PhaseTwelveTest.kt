package com.shitu.app

import com.shitu.app.game.CareerEngine
import com.shitu.app.game.EventEngine
import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.NpcCatalog
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.game.ProjectEngine
import com.shitu.app.game.RegionEngine
import com.shitu.app.game.RouteEngine
import com.shitu.app.game.WorkPlanEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.CareerAbility
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.EventTag
import com.shitu.app.model.Faction
import com.shitu.app.model.HiddenKey
import com.shitu.app.model.Indicator
import com.shitu.app.model.JobTrack
import com.shitu.app.model.JurisdictionLevel
import com.shitu.app.model.LifeStage
import com.shitu.app.model.NpcStatus
import com.shitu.app.model.NpcTier
import com.shitu.app.model.ProjectCatalog
import com.shitu.app.model.ProjectStatus
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.RegionCondition
import com.shitu.app.model.RegionStage
import com.shitu.app.model.RegionTrait
import com.shitu.app.model.ResearchNote
import com.shitu.app.model.UnitType
import com.shitu.app.model.WorkPlan
import com.shitu.app.model.WorkSlot
import com.shitu.app.model.WorldState
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 第十二阶段：地区、条线、项目、工作安排、世界状态、事件内容、NPC 扩充与存档兼容。
class PhaseTwelveTest {

    private fun game(seed: Long = 7L, regionId: String? = null, start: String = "乡镇公务员"): GameState =
        GameEngine.newGame("测试者", "男", "本科", start, seed, regionId = regionId)

    private fun richGame(seed: Long = 11L, regionId: String = "suzhou"): GameState {
        val base = game(seed, regionId)
        return base.copy(governance = base.governance.copy(finance = 800))
    }

    private fun richState(): GameState {
        var state = richGame(seed = 21)
        state = ProjectEngine.start(state, "proj_road").first
        state = WorkPlanEngine.plan(state, listOf(WorkSlot.RESEARCH, WorkSlot.MEETING, WorkSlot.REST)).first
        state = state.copy(world = state.world.withFlag("test_flag").withResearch(ResearchNote("k1", "调研一", "内容")))
        val offer = RegionEngine.offerFor(state, GameRng(3))
        assertNotNull(offer)
        return state.copy(transferOffer = offer)
    }

    // ===== 地区系统 =====

    @Test
    fun regionCatalogCoversRealCities() {
        assertTrue("城市数量不足", RegionCatalog.regions.size >= 60)
        assertTrue(RegionCatalog.municipalitiesCount() >= 4)
        assertTrue(RegionCatalog.subProvincialCount() >= 15)
        assertEquals(RegionCatalog.regions.size, RegionCatalog.regions.map { it.id }.toSet().size)
        assertEquals(RegionCatalog.regions.size, RegionCatalog.regions.map { it.name }.toSet().size)
    }

    @Test
    fun regionCatalogLookupAndFallback() {
        assertNotNull(RegionCatalog.find("suzhou"))
        assertNull(RegionCatalog.find("does_not_exist"))
        assertEquals(RegionCatalog.default, RegionCatalog.findOrDefault(null))
        assertEquals(RegionCatalog.default, RegionCatalog.findOrDefault("does_not_exist"))
    }

    @Test
    fun regionRandomSeededIsStableAndWraps() {
        assertEquals(RegionCatalog.regions[0], RegionCatalog.randomSeeded(0L))
        assertEquals(RegionCatalog.regions[3], RegionCatalog.randomSeeded(3L))
        assertEquals(RegionCatalog.regions[0], RegionCatalog.randomSeeded(RegionCatalog.regions.size.toLong()))
        assertEquals(
            RegionCatalog.regions[RegionCatalog.regions.size - 1],
            RegionCatalog.randomSeeded(-1L)
        )
    }

    @Test
    fun regionTraitFilteringWorks() {
        RegionTrait.entries.forEach { trait ->
            assertTrue("特色 $trait 没有地区", RegionCatalog.withTrait(trait).isNotEmpty())
        }
        RegionCatalog.withTrait(RegionTrait.ECONOMIC_HUB).forEach {
            assertEquals(RegionTrait.ECONOMIC_HUB, it.trait)
        }
    }

    @Test
    fun regionProfilesDifferFromEachOther() {
        val economies = RegionCatalog.regions.map { it.economy }
        val finances = RegionCatalog.regions.map { it.financeBase }
        assertTrue("地区经济差异过小", economies.max() - economies.min() >= 25)
        assertTrue("地区财政差异过小", finances.max() - finances.min() >= 80)
        assertTrue(RegionCatalog.regions.all { it.summary.isNotBlank() && it.seatName.isNotBlank() })
    }

    @Test
    fun regionStageMappingFollowsDevelopment() {
        assertEquals(RegionStage.LAGGING, RegionStage.of(40))
        assertEquals(RegionStage.STARTING, RegionStage.of(45))
        assertEquals(RegionStage.DEVELOPING, RegionStage.of(55))
        assertEquals(RegionStage.IMPROVING, RegionStage.of(60))
        assertEquals(RegionStage.PROSPEROUS, RegionStage.of(70))
    }

    @Test
    fun regionConditionReflectsPressureRiskAndApproval() {
        assertEquals(RegionCondition.STABLE, RegionCondition.of(30, 20, 60))
        assertEquals(RegionCondition.RECOVERING, RegionCondition.of(50, 20, 60))
        assertEquals(RegionCondition.PRESSURE, RegionCondition.of(60, 20, 60))
        assertEquals(RegionCondition.PRESSURE, RegionCondition.of(30, 20, 35))
        assertEquals(RegionCondition.CRISIS, RegionCondition.of(80, 20, 60))
        assertEquals(RegionCondition.CRISIS, RegionCondition.of(30, 75, 60))
    }

    @Test
    fun regionJurisdictionMixesRegionFlavor() {
        val strong = RegionCatalog.find("shenzhen")!!
        val weak = RegionCatalog.find("zhoukou")!!
        val strongCounty = RegionEngine.jurisdictionOf(strong, JurisdictionLevel.COUNTY)
        val weakCounty = RegionEngine.jurisdictionOf(weak, JurisdictionLevel.COUNTY)
        assertTrue(strongCounty.economy > weakCounty.economy)
        assertTrue(strongCounty.infrastructure > weakCounty.infrastructure)
        assertEquals(strong.name, strongCounty.name)
    }

    @Test
    fun regionJurisdictionNameUsesSeatForLowLevel() {
        val region = RegionCatalog.find("zhoukou")!!
        val town = RegionEngine.jurisdictionOf(region, JurisdictionLevel.TOWNSHIP)
        assertEquals(region.seatName, town.name)
        assertEquals(region.population, RegionEngine.jurisdictionOf(region, JurisdictionLevel.COUNTY).population)
    }

    @Test
    fun regionInitialFinanceRisesWithFinanceBase() {
        val rich = RegionEngine.initialFinance(RegionCatalog.find("shenzhen")!!, JurisdictionLevel.COUNTY)
        val poor = RegionEngine.initialFinance(RegionCatalog.find("zhoukou")!!, JurisdictionLevel.COUNTY)
        assertTrue(rich > poor)
    }

    @Test
    fun newGameHonorsRequestedRegion() {
        val state = game(seed = 1L, regionId = "zhoukou")
        assertEquals("zhoukou", state.regionId)
        assertEquals("周口城区", state.governance.jurisdiction.name)
    }

    @Test
    fun newGameWithoutRegionVariesBySeedAndRecordsRegion() {
        val ids = (0L..9L).map { game(seed = it, regionId = null).regionId }.toSet()
        assertTrue("多周目没有地区变化", ids.size >= 5)
        val state = game(seed = 1L, regionId = "datong")
        assertEquals("datong", state.regionId)
        assertTrue(state.career.records.first().detail.contains("大同"))
    }

    @Test
    fun regionStageAndConditionReadFromState() {
        val state = game(seed = 4L, regionId = "luoyang")
        assertEquals(RegionStage.of(state.governance.jurisdiction.development), RegionEngine.stageOf(state))
        assertEquals(
            RegionCondition.of(
                state.governance.jurisdiction.pressure,
                state.relationship.risk,
                state.governance.jurisdiction.approval
            ),
            RegionEngine.conditionOf(state)
        )
        assertTrue(RegionCatalog.find("luoyang")!!.challenge in 0..100)
    }

    // ===== 岗位条线 =====

    @Test
    fun jobTrackHasTwelveTracks() {
        assertEquals(12, JobTrack.entries.size)
        assertTrue(JobTrack.entries.all { it.label.isNotBlank() && it.description.isNotBlank() })
        assertTrue(JobTrack.entries.all { it.tags.isNotEmpty() && it.focusIndicators.isNotEmpty() })
        assertTrue(JobTrack.entries.all { it.riskBias in 0..10 })
    }

    @Test
    fun jobTrackOfPrefersRouteThenUnit() {
        assertEquals(JobTrack.ECONOMY, JobTrack.of(UnitType.TOWN, CareerRoute.ECONOMY_ROUTE))
        assertEquals(JobTrack.ORGANIZATION, JobTrack.of(UnitType.TOWN, CareerRoute.ORGANIZATION_ROUTE))
        assertEquals(JobTrack.HOUSING, JobTrack.of(UnitType.TOWN, CareerRoute.REFORM_ROUTE))
        assertEquals(JobTrack.PROPAGANDA, JobTrack.of(UnitType.CITY, CareerRoute.REFORM_ROUTE))
    }

    @Test
    fun jobTrackOfMapsUnitWhenRouteMissing() {
        assertEquals(JobTrack.GRASSROOTS, JobTrack.of(UnitType.TOWN, null))
        assertEquals(JobTrack.GENERAL_ADMIN, JobTrack.of(UnitType.COUNTY, null))
        assertEquals(JobTrack.OFFICE, JobTrack.of(UnitType.CITY, null))
        assertEquals(JobTrack.EMERGENCY, JobTrack.of(UnitType.BUSINESS, null))
    }

    @Test
    fun jobTrackNamesRoundTrip() {
        JobTrack.entries.forEach { assertEquals(it, JobTrack.byName(it.name)) }
        assertNull(JobTrack.byName("no_such_track"))
    }

    // ===== 职业路线 =====

    @Test
    fun careerRouteHasSevenRoutes() {
        assertEquals(7, CareerRoute.entries.size)
        assertTrue(CareerRoute.entries.all { it.label.isNotBlank() && it.tags.isNotEmpty() })
    }

    @Test
    fun newRoutesDrivePrimaryAbility() {
        assertEquals(CareerAbility.SPECIALTY, CareerEngine.primaryAbility(CareerRoute.PROFESSIONAL))
        assertEquals(CareerAbility.DECISION, CareerEngine.primaryAbility(CareerRoute.ECONOMY_ROUTE))
        assertEquals(CareerAbility.COMMUNICATION, CareerEngine.primaryAbility(CareerRoute.ORGANIZATION_ROUTE))
        assertEquals(CareerAbility.DECISION, CareerEngine.primaryAbility(CareerRoute.REFORM_ROUTE))
        assertEquals(CareerAbility.DECISION, CareerEngine.primaryAbility(null))
    }

    @Test
    fun everyRouteHasReviewWeightsSummingToOneHundred() {
        (CareerRoute.entries + listOf(null)).forEach { route ->
            val total = RouteEngine.reviewWeights(route).sumOf { it.second }
            assertEquals("路线 $route 的考核权重不等于 100", 100, total)
        }
    }

    @Test
    fun newRoutesHavePreferredUnits() {
        assertEquals(UnitType.BUSINESS, RouteEngine.preferredUnit(CareerRoute.ECONOMY_ROUTE, UnitType.TOWN))
        assertNull(RouteEngine.preferredUnit(CareerRoute.ECONOMY_ROUTE, UnitType.BUSINESS))
        assertEquals(UnitType.GENERAL, RouteEngine.preferredUnit(CareerRoute.ORGANIZATION_ROUTE, UnitType.TOWN))
        assertEquals(UnitType.CITY, RouteEngine.preferredUnit(CareerRoute.REFORM_ROUTE, UnitType.TOWN))
        assertEquals(3, RouteEngine.promotionBonus(CareerRoute.REFORM_ROUTE))
        assertEquals(0, RouteEngine.promotionBonus(null))
    }

    // ===== 项目系统 =====

    @Test
    fun projectCatalogHasFourteenProjects() {
        assertEquals(14, ProjectCatalog.projects.size)
        assertEquals(ProjectCatalog.projects.size, ProjectCatalog.projects.map { it.id }.toSet().size)
        assertTrue(ProjectCatalog.projects.all { it.quarterlyCost >= 1 && it.quarters >= 1 })
    }

    @Test
    fun projectLookupAndAvailabilityRespectPosition() {
        assertNotNull(ProjectCatalog.find("proj_road"))
        assertNull(ProjectCatalog.find("no_such_project"))
        assertTrue(ProjectCatalog.availableFor(0, RegionTrait.BALANCED).all { it.minPositionIndex <= 0 })
        assertEquals(ProjectCatalog.projects.size, ProjectCatalog.availableFor(8, RegionTrait.BALANCED).size)
    }

    @Test
    fun projectTraitBonusRanksMatchingRegionsFirst() {
        val boosted = ProjectCatalog.forTrait(RegionTrait.INDUSTRIAL)
        assertTrue(boosted.isNotEmpty())
        assertTrue(boosted.all { RegionTrait.INDUSTRIAL in it.traitBonus })
        val park = ProjectCatalog.find("proj_industrial_park")!!
        assertTrue(ProjectEngine.isBoosted(richGame(regionId = "tangshan"), park))
        assertFalse(ProjectEngine.isBoosted(richGame(regionId = "guilin"), park))
    }

    @Test
    fun projectStartSpendsFinanceAndRecordsProgress() {
        val state = richGame()
        val road = ProjectCatalog.find("proj_road")!!
        val before = state.governance.finance
        val (next, message) = ProjectEngine.start(state, "proj_road")
        assertTrue(message.isNotBlank())
        assertEquals(before - road.quarterlyCost, next.governance.finance)
        assertEquals(1, next.projects.size)
        assertEquals(ProjectStatus.ONGOING, next.projects.first().status)
        assertTrue(next.career.records.any { it.kind == CareerRecordKind.PROJECT })
    }

    @Test
    fun projectStartRejectsLowPosition() {
        val state = richGame()
        val (next, message) = ProjectEngine.start(state, "proj_industrial_park")
        assertTrue(message.contains("牵不动"))
        assertTrue(next.projects.isEmpty())
    }

    @Test
    fun projectStartRejectsInsufficientFinance() {
        val base = game(regionId = "zhoukou")
        val poor = base.copy(governance = base.governance.copy(finance = 3))
        val (next, message) = ProjectEngine.start(poor, "proj_road")
        assertTrue(message.contains("财政"))
        assertTrue(next.projects.isEmpty())
    }

    @Test
    fun projectAdvanceCompletesAndUnlocksWorldFlag() {
        var state = richGame()
        state = ProjectEngine.start(state, "proj_road").first
        repeat(3) { state = ProjectEngine.advance(state).first }
        assertTrue(ProjectEngine.hasBuilt(state, "proj_road"))
        assertTrue(state.world.builtProjects.contains("proj_road"))
        assertTrue(state.world.hasFlag("built_proj_road"))
        // 第十三阶段：建成后先进入运营期，运营结束才算全面收官。
        assertEquals(ProjectStatus.OPERATING, state.projects.first { it.id == "proj_road" }.status)
        assertTrue(state.career.records.any { it.title.startsWith("完工") })
        repeat(2) { state = ProjectEngine.advance(state).first }
        assertEquals(ProjectStatus.COMPLETED, state.projects.first { it.id == "proj_road" }.status)
    }

    @Test
    fun projectStallsWithoutFinanceAndResumes() {
        var state = richGame()
        state = ProjectEngine.start(state, "proj_road").first
        val broke = state.copy(governance = state.governance.copy(finance = 0))
        val stalled = ProjectEngine.advance(broke)
        assertEquals(ProjectStatus.STALLED, stalled.first.projects.first().status)
        assertTrue(stalled.second.any { it.contains("停工") })
        val funded = stalled.first.copy(governance = stalled.first.governance.copy(finance = 200))
        val resumed = ProjectEngine.resume(funded, "proj_road")
        assertEquals(ProjectStatus.ONGOING, resumed.first.projects.first().status)
    }

    // ===== 工作安排 =====

    @Test
    fun workPlanRejectsWrongSlotCount() {
        val state = game()
        val (next, message) = WorkPlanEngine.plan(state, listOf(WorkSlot.REST, WorkSlot.REST))
        assertTrue(message.isNotBlank())
        assertFalse(next.workPlan.isPlanned)
        assertEquals(3, WorkPlan.SLOT_COUNT)
    }

    @Test
    fun workPlanResolveRestRestoresEnergyAndLowersStress() {
        val base = game()
        var state = base.copy(
            player = base.player.copy(
                attributes = base.player.attributes.withValue(Attr.ENERGY, 40),
                hidden = base.player.hidden.withValue(HiddenKey.STRESS, 60)
            )
        )
        state = WorkPlanEngine.plan(state, listOf(WorkSlot.REST, WorkSlot.REST, WorkSlot.REST)).first
        val resolved = WorkPlanEngine.resolve(state).first
        assertTrue(resolved.player.attributes.energy > 40)
        assertTrue(resolved.player.hidden.valueOf(HiddenKey.STRESS) < 60)
        assertFalse(resolved.workPlan.isPlanned)
    }

    @Test
    fun workPlanResolveMeetingRaisesCoordinationAndStress() {
        val base = game()
        val state0 = base.copy(
            player = base.player.copy(hidden = base.player.hidden.withValue(HiddenKey.STRESS, 20))
        )
        val planned = WorkPlanEngine.plan(state0, listOf(WorkSlot.MEETING, WorkSlot.MEETING, WorkSlot.MEETING)).first
        val resolved = WorkPlanEngine.resolve(planned).first
        assertTrue(resolved.player.attributes.coordination > state0.player.attributes.coordination)
        assertTrue(resolved.player.hidden.valueOf(HiddenKey.STRESS) > 20)
    }

    @Test
    fun workPlanResolveResearchCreatesNote() {
        val state = game(seed = 2L, regionId = "guilin")
        val planned = WorkPlanEngine.plan(state, listOf(WorkSlot.RESEARCH, WorkSlot.MATERIAL, WorkSlot.STUDY)).first
        val resolved = WorkPlanEngine.resolve(planned).first
        assertTrue(resolved.world.hasResearch("research_guilin"))
        assertTrue(resolved.world.research.first().detail.contains("桂林"))
        assertTrue(resolved.player.attributes.writing > state.player.attributes.writing)
    }

    @Test
    fun workPlanResolveReceptionRaisesApproval() {
        val state = game(seed = 3L)
        val before = state.governance.jurisdiction.approval
        val planned = WorkPlanEngine.plan(state, listOf(WorkSlot.RECEPTION, WorkSlot.RECEPTION, WorkSlot.FAMILY)).first
        val resolved = WorkPlanEngine.resolve(planned).first
        assertTrue(resolved.governance.jurisdiction.approval > before)
        assertTrue(resolved.player.attributes.family > state.player.attributes.family)
    }

    // ===== 世界状态 =====

    @Test
    fun worldStateBuiltProjectsAreIdempotent() {
        var world = WorldState()
        world = world.withBuilt("a").withBuilt("a")
        assertEquals(1, world.builtProjects.size)
        assertTrue(world.hasBuilt("a"))
        assertFalse(world.hasBuilt("b"))
    }

    @Test
    fun worldStateResearchDeduplicatesByKey() {
        var world = WorldState().withResearch(ResearchNote("k", "旧", "旧内容"))
        world = world.withResearch(ResearchNote("k", "新", "新内容"))
        assertEquals(1, world.research.size)
        assertEquals("新", world.research.first().label)
    }

    @Test
    fun worldStateResearchIsCapped() {
        var world = WorldState()
        repeat(40) { index -> world = world.withResearch(ResearchNote("k$index", "n$index", "d$index")) }
        assertEquals(WorldState.MAX_RESEARCH, world.research.size)
    }

    @Test
    fun worldStateFlagsAreIdempotentAndStatusDefaultsToNormal() {
        var world = WorldState().withFlag("f").withFlag("f")
        assertEquals(1, world.majorFlags.size)
        assertTrue(world.hasFlag("f"))
        assertEquals(NpcStatus.NORMAL, world.statusOf("nobody"))
        assertFalse(WorldState().statusOf("nobody").outOfReach)
    }

    // ===== NPC 与竞争对手扩充 =====

    @Test
    fun npcCatalogHasAtLeastSixtyUniquePeople() {
        assertTrue("NPC 数量不足：${NpcCatalog.npcs.size}", NpcCatalog.npcs.size >= 60)
        assertEquals(NpcCatalog.npcs.size, NpcCatalog.npcs.map { it.id }.toSet().size)
        assertEquals(NpcCatalog.npcs.size, NpcCatalog.npcs.map { it.name }.toSet().size)
    }

    @Test
    fun npcProfilesAreCompleteAndSearchable() {
        assertTrue(NpcCatalog.npcs.all { it.name.isNotBlank() && it.title.isNotBlank() && it.description.isNotBlank() })
        assertTrue(NpcCatalog.npcs.all { it.personality.isNotEmpty() && it.influence in 0..100 })
        assertNotNull(NpcCatalog.find(NpcCatalog.npcs.first().id))
        assertNull(NpcCatalog.find("no_such_npc"))
        assertTrue(NpcCatalog.byFaction(Faction.LOCAL).isNotEmpty())
    }

    @Test
    fun peerColleaguesExistAsNormalNpcs() {
        val peers = NpcCatalog.npcs.filter { it.tier == NpcTier.COLLEAGUE }
        assertTrue("同层同事数量不足：${peers.size}", peers.size >= 6)
        assertTrue(peers.all { it.name.isNotBlank() && it.description.isNotBlank() })
        assertTrue(peers.all { it.influence in 0..100 })
        assertEquals(peers.size, peers.map { it.id }.toSet().size)
    }

    @Test
    fun relationshipSupportIsBoundedAndOnlyHelps() {
        val state = game(seed = 8L)
        val support = PoliticsEngine.relationshipSupport(state)
        assertTrue("关系助力应当落在合理区间：$support", support in 0..PoliticsEngine.MAX_RELATIONSHIP_SUPPORT)
        val connected = state.copy(
            relationship = state.relationship
                .withFaction(Faction.CORE, 95)
                .withRelation("chen_jianguo", 95)
                .withTrust("chen_jianguo", 95)
        )
        assertTrue(
            "关系更好时助力只应持平或上升",
            PoliticsEngine.relationshipSupport(connected) >= support
        )
    }

    // ===== 调任系统 =====

    @Test
    fun transferOfferTargetsAnotherRegionAndExpires() {
        val state = game(seed = 5L, regionId = "suzhou")
        val offer = RegionEngine.offerFor(state, GameRng(5L))
        assertNotNull(offer)
        assertFalse(offer!!.regionId == "suzhou")
        assertTrue(offer.reason.isNotBlank())
        assertEquals(state.absoluteQuarter + 8, offer.expiresQuarter)
        assertNotNull(RegionCatalog.find(offer.regionId))
    }

    @Test
    fun transferAcceptMovesRegionAndKeepsHalfOfResults() {
        val state = game(seed = 6L, regionId = "suzhou")
        val offer = RegionEngine.offerFor(state, GameRng(6L))!!
        val moved = RegionEngine.accept(state, offer)
        assertEquals(offer.regionId, moved.regionId)
        assertNull(moved.transferOffer)
        assertTrue(moved.career.records.any { it.kind == CareerRecordKind.TRANSFER })
        val newApproval = moved.governance.jurisdiction.approval
        val oldApproval = state.governance.jurisdiction.approval
        assertTrue(newApproval <= (oldApproval + 100) / 2 + 55)
    }

    @Test
    fun transferDeclineClearsOfferAndAddsRisk() {
        val state = game(seed = 9L, regionId = "luoyang")
        val offer = RegionEngine.offerFor(state, GameRng(9L))!!
        val declined = RegionEngine.decline(state, offer)
        assertNull(declined.transferOffer)
        assertTrue(declined.relationship.risk >= state.relationship.risk)
        assertTrue(declined.career.records.any { it.kind == CareerRecordKind.SETBACK })
    }

    // ===== 事件内容扩充 =====

    @Test
    fun libraryContainsAllPhaseTwelveEventFamilies() {
        val ids = EventLibrary.events.map { it.id }
        assertTrue(ids.any { it.startsWith("r12_") })
        assertTrue(ids.any { it.startsWith("j12_") })
        assertTrue(ids.any { it.startsWith("c12_") })
        assertTrue(ids.any { it.startsWith("f12_") })
    }

    @Test
    fun libraryHasAtLeastTwoHundredEventsWithUniqueIds() {
        assertTrue("事件数量不足：${EventLibrary.events.size}", EventLibrary.events.size >= 200)
        val ids = EventLibrary.events.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun randomPoolExcludesChainedEventsOnly() {
        assertTrue(EventLibrary.randomPool.none { it.chainId != null })
        assertTrue(EventLibrary.randomPool.size >= 150)
    }

    @Test
    fun chainEventsHaveContinuousSteps() {
        val chainIds = EventLibrary.events.mapNotNull { it.chainId }.toSet()
        assertTrue("事件链数量不足：${chainIds.size}", chainIds.size >= 20)
        chainIds.forEach { chainId ->
            val steps = EventLibrary.stepsOf(chainId).map { it.chainStep }.sorted()
            if (chainId.startsWith("c_")) {
                assertTrue("第十二阶段事件链 $chainId 节点过少", steps.size >= 2)
            }
            assertEquals("事件链 $chainId 步骤不连续", (1..steps.size).toList(), steps)
        }
    }

    @Test
    fun everyEventKeepsThreeUsableChoices() {
        EventLibrary.events.forEach { event ->
            assertTrue("事件 ${event.id} 选项不足", event.choices.size >= 3)
            assertTrue(event.choices.all { it.text.isNotBlank() && it.outcome.isNotBlank() })
        }
    }

    // ===== 事件调度与人生阶段 =====

    @Test
    fun eligibleEventsRespectPositionAndUnitLimits() {
        val state = game(seed = 12L)
        EventEngine.eligibleEvents(state).forEach { event ->
            assertTrue(event.minPositionIndex <= state.player.positionIndex)
            assertTrue(event.maxPositionIndex >= state.player.positionIndex)
            assertTrue(event.units.isEmpty() || state.player.unit in event.units)
        }
    }

    @Test
    fun jobTrackAndRouteRaiseWeightOfAlignedEvents() {
        val base = game(seed = 3L)
        val economy = base.copy(route = CareerRoute.ECONOMY_ROUTE)
        val event = EventLibrary.find("r12_leading_firm")!!
        val plain = EventEngine.weightOf(base, event)
        val aligned = EventEngine.weightOf(economy, event)
        assertTrue("对口事件权重没有提高", aligned > plain)
    }

    @Test
    fun lifeStageTagsDifferByStage() {
        assertTrue(EventTag.FAMILY in EventEngine.lifeStageTags(LifeStage.LATE))
        assertTrue(EventTag.OVERTIME in EventEngine.lifeStageTags(LifeStage.EARLY))
        assertTrue(EventTag.ASSESSMENT in EventEngine.lifeStageTags(LifeStage.MID_CAREER))
        assertTrue(EventTag.CAREER in EventEngine.lifeStageTags(LifeStage.CORE))
    }

    @Test
    fun lifeStageMappingFollowsCareerClock() {
        assertEquals(LifeStage.EARLY, LifeStage.of(0, 0, 22))
        assertEquals(LifeStage.GRASSROOTS, LifeStage.of(1, 2, 25))
        assertEquals(LifeStage.GROWTH, LifeStage.of(2, 7, 30))
        assertEquals(LifeStage.MID_CAREER, LifeStage.of(4, 10, 35))
        assertEquals(LifeStage.CORE, LifeStage.of(6, 15, 45))
        assertEquals(LifeStage.PEAK, LifeStage.of(7, 18, 50))
        assertEquals(LifeStage.LATE, LifeStage.of(8, 28, 55))
    }

    @Test
    fun engineReportsCurrentLifeStage() {
        val state = game(seed = 13L)
        assertEquals(LifeStage.EARLY, EventEngine.lifeStageOf(state))
        val veteran = state.copy(player = state.player.copy(serviceYears = 30))
        assertEquals(LifeStage.LATE, EventEngine.lifeStageOf(veteran))
    }

    // ===== 存档 =====

    @Test
    fun codecRoundTripPreservesPhaseTwelveState() {
        val state = richState()
        val text = GameCodec.encode(state)
        val restored = GameCodec.decode(text)
        assertNotNull(restored)
        val other = restored!!
        assertEquals(state.regionId, other.regionId)
        assertEquals(state.projects, other.projects)
        assertEquals(state.world, other.world)
        assertEquals(state.workPlan, other.workPlan)
        assertEquals(state.transferOffer, other.transferOffer)
        assertEquals(text, GameCodec.encode(other))
    }

    @Test
    fun codecReadsOldSaveWithoutPhaseTwelveFields() {
        val state = richState()
        val text = GameCodec.encode(state)
        val legacy = text.split("\n").filter { line ->
            !line.startsWith("regionId\t") &&
                !line.startsWith("proj.") &&
                !line.startsWith("world.") &&
                !line.startsWith("plan.") &&
                !line.startsWith("offer.")
        }.joinToString("\n").replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t3")
        val restored = GameCodec.decode(legacy)
        assertNotNull("旧存档应当可以读取", restored)
        val other = restored!!
        assertEquals(RegionCatalog.default.id, other.regionId)
        assertTrue(other.projects.isEmpty())
        assertEquals(WorldState(), other.world)
        assertFalse(other.workPlan.isPlanned)
        assertNull(other.transferOffer)
    }

    @Test
    fun codecRejectsFutureFormatVersion() {
        val text = GameCodec.encode(richState()).replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t" + (GameCodec.FORMAT_VERSION + 1))
        assertNull(GameCodec.decode(text))
    }

    // ===== 完整循环 =====

    @Test
    fun fullPhaseTwelveLoopReachesEndingAndSaves() {
        var state = game(seed = 42L, regionId = "tangshan")
        var step = 0
        while (!state.isFinished && step < 6000) {
            step += 1
            state = when (state.phase) {
                GamePhase.EVENT -> GameEngine.resolveChoice(state, step % 3)
                GamePhase.OUTCOME -> GameEngine.advance(state)
                GamePhase.REVIEW -> GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> state
            }
        }
        assertTrue("游戏没有进入结局", state.isFinished)
        assertNotNull(state.ending)
        assertNotNull(GameCodec.decode(GameCodec.encode(state)))
    }

    @Test
    fun expansionDoesNotBlockPromotion() {
        var promoted = 0
        for (seed in 1L..8L) {
            var state = game(seed = seed, regionId = "zhoukou")
            var step = 0
            while (!state.isFinished && step < 6000) {
                step += 1
                when (state.phase) {
                    GamePhase.EVENT -> state = GameEngine.resolveChoice(state, step % 3)
                    GamePhase.OUTCOME -> state = GameEngine.advance(state)
                    GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                    GamePhase.ENDING -> break
                }
            }
            if (state.promotionCount >= 1) promoted += 1
        }
        assertTrue("内容扩充后几乎晋升不了：$promoted/8", promoted >= 1)
    }
}