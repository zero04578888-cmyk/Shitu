package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameState
import com.shitu.app.game.NpcCatalog
import com.shitu.app.game.ProjectEngine
import com.shitu.app.game.SimulationTestEngine
import com.shitu.app.game.WorldEventLibrary
import com.shitu.app.game.WorldEventScope
import com.shitu.app.game.WorldEventTemplate
import com.shitu.app.game.WorldSimulationEngine
import com.shitu.app.model.DistrictDelta
import com.shitu.app.model.DistrictState
import com.shitu.app.model.EconomicPhase
import com.shitu.app.model.EraState
import com.shitu.app.model.GlobalEconomy
import com.shitu.app.model.Industry
import com.shitu.app.model.NpcRuntime
import com.shitu.app.model.PolicyTag
import com.shitu.app.model.ProjectStatus
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.Vacancy
import com.shitu.app.model.WorldState
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 第十三阶段：动态世界与长期因果。经济周期、地区动态、NPC 自主行动、
// 政策长期后果、岗位空缺、世界事件与 30 年模拟的数值保护。
class PhaseThirteenTest {

    private fun game(seed: Long = 13L, regionId: String = "suzhou"): GameState =
        GameEngine.newGame("世界", "男", "本科", "乡镇公务员", seed, regionId = regionId)

    private fun withPolicies(tag: PolicyTag, times: Int, magnitude: Int = 3, year: Int = 4): GameState {
        var state = game()
        state = state.copy(year = year)
        repeat(times) { index ->
            state = WorldSimulationEngine.recordPolicy(
                state,
                title = "测试政策$index",
                direction = "测试",
                tags = setOf(tag),
                magnitude = magnitude
            )
        }
        return state
    }

    // ===== 内容库 =====

    @Test
    fun worldEventLibraryHasAtLeastThirtyEvents() {
        assertTrue("世界事件不足 30 个：${WorldEventLibrary.events.size}", WorldEventLibrary.events.size >= 30)
    }

    @Test
    fun worldEventIdsAreUnique() {
        val ids = WorldEventLibrary.events.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun causalChainLibraryHasAtLeastTenChains() {
        assertTrue("长期因果链不足 10 条：${WorldEventLibrary.chains.size}", WorldEventLibrary.chains.size >= 10)
    }

    @Test
    fun causalChainIdsAreUnique() {
        val ids = WorldEventLibrary.chains.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun worldEventCandidatesRespectEconomyBounds() {
        val state = game().copy(world = game().world.withEconomy(GlobalEconomy(index = 20, phase = EconomicPhase.RECESSION)))
        val candidates = WorldEventLibrary.candidates(state)
        candidates.forEach { event ->
            assertTrue("事件 ${event.id} 不应在低景气下出现", 20 >= event.minEconomy)
            assertTrue("事件 ${event.id} 不应在低景气下出现", 20 <= event.maxEconomy)
        }
    }

    // ===== 数据模型 =====

    @Test
    fun districtFromRegionMatchesProfile() {
        val region = RegionCatalog.find("datong")!!
        val district = DistrictState.fromRegion(region)
        assertEquals(region.id, district.id)
        assertEquals(region.industry, district.industry)
        assertEquals(region.environment, district.environment)
    }

    @Test
    fun districtApplyClampsValues() {
        val rich = DistrictState(id = "x", economy = 95, population = 160)
        val high = rich.apply(DistrictDelta(economy = 20, population = 20))
        assertEquals(100, high.economy)
        assertEquals(DistrictState.MAX_POPULATION, high.population)

        val poor = DistrictState(id = "x", population = 1, finance = 0)
        val low = poor.apply(DistrictDelta(population = -5, finance = -30))
        assertEquals(DistrictState.MIN_POPULATION, low.population)
        assertEquals(0, low.finance)
    }

    @Test
    fun districtDeltaSumAndScale() {
        val a = DistrictDelta(economy = 2, environment = -1)
        val b = DistrictDelta(economy = 3, employment = 4)
        val sum = a + b
        assertEquals(5, sum.economy)
        assertEquals(-1, sum.environment)
        assertEquals(4, sum.employment)
        assertEquals(6, b.scaled(2).economy)
        assertTrue(DistrictDelta().isEmpty)
        assertFalse(a.isEmpty)
    }

    @Test
    fun districtDevelopmentReflectsIndicators() {
        val high = DistrictState(id = "x", economy = 90, employment = 90, infrastructure = 90, education = 90, medical = 90, environment = 90, security = 90)
        val low = DistrictState(id = "x", economy = 10, employment = 10, infrastructure = 10, education = 10, medical = 10, environment = 10, security = 10)
        assertTrue(high.development > low.development)
    }

    @Test
    fun npcRuntimeNormalizesAndComputesPower() {
        val runtime = NpcRuntime(npcId = "x", ability = 200, merit = -20, influence = 60, positionIndex = 99)
        val normalized = runtime.normalized()
        assertEquals(100, normalized.ability)
        assertEquals(0, normalized.merit)
        assertTrue(normalized.positionIndex < com.shitu.app.model.POSITION_LADDER.size)
        assertTrue(normalized.power in 0..100)
    }

    @Test
    fun globalEconomyWithPhaseResetsMonths() {
        val economy = GlobalEconomy(phase = EconomicPhase.STABLE, index = 50, monthsInPhase = 9)
        val next = economy.withPhase(EconomicPhase.BOOM)
        assertEquals(0, next.monthsInPhase)
        assertTrue(next.history.contains(EconomicPhase.BOOM))
    }

    // ===== 初始化 =====

    @Test
    fun ensureWorldInitializesDistrictsAndNpcs() {
        val state = WorldSimulationEngine.ensureWorld(game())
        assertEquals(RegionCatalog.regions.size, state.world.districts.size)
        assertEquals(NpcCatalog.npcs.size, state.world.npcs.size)
    }

    @Test
    fun newGameStartsWithInitializedWorld() {
        val state = game()
        assertEquals(RegionCatalog.regions.size, state.world.districts.size)
        assertTrue(state.world.economy.history.isNotEmpty())
    }

    @Test
    fun ensureWorldDerivesEconomyFromClimate() {
        val base = game()
        val crafted = base.copy(world = WorldState(economyClimate = 80, environmentClimate = 70))
        val state = WorldSimulationEngine.ensureWorld(crafted)
        assertEquals(80, state.world.economy.index)
        assertEquals(EconomicPhase.BOOM, state.world.economy.phase)
    }

    // ===== 经济周期 =====

    @Test
    fun phaseForIndexMapsRanges() {
        assertEquals(EconomicPhase.BOOM, WorldSimulationEngine.phaseForIndex(80))
        assertEquals(EconomicPhase.GROWTH, WorldSimulationEngine.phaseForIndex(60))
        assertEquals(EconomicPhase.STABLE, WorldSimulationEngine.phaseForIndex(50))
        assertEquals(EconomicPhase.SLOWDOWN, WorldSimulationEngine.phaseForIndex(35))
        assertEquals(EconomicPhase.RECESSION, WorldSimulationEngine.phaseForIndex(26))
        assertEquals(EconomicPhase.RECOVERY, WorldSimulationEngine.phaseForIndex(10))
    }

    @Test
    fun economyIndexStaysInBoundsOverManyTicks() {
        var world = WorldSimulationEngine.ensureWorld(game()).world
        val rng = GameRng(5L)
        repeat(200) {
            world = WorldSimulationEngine.tickEconomy(world, rng)
            assertTrue(world.economy.index in WorldSimulationEngine.ECONOMY_MIN..WorldSimulationEngine.ECONOMY_MAX)
        }
    }

    @Test
    fun economyIndexVariesInsteadOfFreezing(){
        var world = WorldSimulationEngine.ensureWorld(game()).world
        val rng = GameRng(77L)
        var min = world.economy.index
        var max = world.economy.index
        repeat(160) {
            world = WorldSimulationEngine.tickEconomy(world, rng)
            if (world.economy.index < min) min = world.economy.index
            if (world.economy.index > max) max = world.economy.index
        }
        assertTrue("经济指数长期不动：$min..$max", max - min >= 5)
        assertTrue("经济不应长期停在极端：$max", max < 95 || min < 90)
    }
    @Test
    fun economicPhaseDoesNotFlipEveryQuarter() {
        var world = WorldSimulationEngine.ensureWorld(game()).world
        val rng = GameRng(9L)
        var changes = 0
        var previous = world.economy.phase
        repeat(60) {
            world = WorldSimulationEngine.tickEconomy(world, rng)
            if (world.economy.phase != previous) changes += 1
            previous = world.economy.phase
        }
        // 至少持续两个季度才可能换阶段，所以 60 次里换阶段不可能超过 30 次。
        assertTrue("经济周期变化过于频繁：$changes", changes <= 30)
    }

    @Test
    fun incomeModifierFollowsPhase() {
        val boom = game().copy(world = game().world.withEconomy(GlobalEconomy(phase = EconomicPhase.BOOM)))
        val recession = game().copy(world = game().world.withEconomy(GlobalEconomy(phase = EconomicPhase.RECESSION)))
        assertTrue(WorldSimulationEngine.incomeModifier(boom) > 0)
        assertTrue(WorldSimulationEngine.incomeModifier(recession) < 0)
    }

    // ===== 地区与 NPC =====

    @Test
    fun industrialDistrictEnvironmentDeclines() {
        val world = WorldSimulationEngine.ensureWorld(game()).world
            .withDistrict(DistrictState(id = "suzhou", industry = 92, environment = 60))
        val after = WorldSimulationEngine.tickDistricts(world, GameRng(3L))
        assertTrue(after.districtOf("suzhou").environment <= 60)
    }

    @Test
    fun npcGrowthIsCapped() {
        var world = WorldSimulationEngine.ensureWorld(game()).world
        val rng = GameRng(11L)
        repeat(200) { world = WorldSimulationEngine.tickNpcs(world, rng) }
        world.npcs.values.forEach { runtime ->
            assertTrue(runtime.ability in 0..100)
            assertTrue(runtime.merit in 0..100)
            assertTrue(runtime.influence in 0..100)
        }
    }

    @Test
    fun factionDriftPerQuarterIsAtMostOne() {
        var state = game()
        val rng = GameRng(17L)
        var maxDelta = 0
        repeat(30) {
            val before = state.politics.factionInfluence.toMap()
            val after = WorldSimulationEngine.tickFactions(state, rng).politics.factionInfluence
            before.keys.forEach { faction ->
                val delta = kotlin.math.abs((after[faction] ?: 0) - (before[faction] ?: 0))
                if (delta > maxDelta) maxDelta = delta
            }
            state = state.copy(politics = WorldSimulationEngine.tickFactions(state, rng).politics)
        }
        assertTrue("派系影响力变化过猛：$maxDelta", maxDelta <= WorldSimulationEngine.FACTION_DRIFT_LIMIT)
    }

    @Test
    fun advanceQuarterMovesWorldClock() {
        val state = WorldSimulationEngine.advanceQuarter(game(), GameRng(1L))
        assertEquals(WorldSimulationEngine.MONTHS_PER_QUARTER, state.world.month)
    }

    @Test
    fun advanceQuarterKeepsValuesInRange() {
        var state = game(seed = 21L)
        val rng = GameRng(21L)
        repeat(120) { state = WorldSimulationEngine.advanceQuarter(state, rng) }
        assertTrue(state.governance.finance >= 0)
        state.world.districts.values.forEach { district ->
            assertTrue(district.population >= DistrictState.MIN_POPULATION)
            assertTrue(district.population <= DistrictState.MAX_POPULATION)
            assertTrue(district.economy in 0..100)
            assertTrue(district.environment in 0..100)
            assertTrue(district.finance >= 0)
        }
        assertTrue(state.world.economy.index in WorldSimulationEngine.ECONOMY_MIN..WorldSimulationEngine.ECONOMY_MAX)
        assertTrue(state.relationship.risk in 0..100)
    }

    @Test
    fun recordPolicyAccumulatesPressure() {
        val state = withPolicies(PolicyTag.INDUSTRY, times = 3, magnitude = 3)
        assertEquals(9, WorldSimulationEngine.effectivePressure(state, PolicyTag.INDUSTRY))
    }

    @Test
    fun recordDecisionAddsToPressure() {
        val state = WorldSimulationEngine.recordDecision(
            withPolicies(PolicyTag.INDUSTRY, times = 1, magnitude = 3),
            id = "dec_test",
            title = "测试抉择",
            choice = "A",
            consequence = "结果",
            tags = setOf(PolicyTag.INDUSTRY)
        )
        assertEquals(5, WorldSimulationEngine.effectivePressure(state, PolicyTag.INDUSTRY))
    }

    @Test
    fun causalChainFiresOnceWhenPressureIsHigh() {
        val state = withPolicies(PolicyTag.INDUSTRY, times = 4, magnitude = 3, year = 4)
        val settled = WorldSimulationEngine.settleCausalChains(state, GameRng(3L))
        assertTrue(settled.world.hasFlag("causal_causal_industry_pollution"))
        assertTrue(settled.world.news.any { it.title == "工业扩张带来环境压力" })
        val again = WorldSimulationEngine.settleCausalChains(settled, GameRng(3L))
        assertEquals(settled.world.news.size, again.world.news.size)
    }

    @Test
    fun causalChainRequiresThreshold() {
        val state = withPolicies(PolicyTag.INDUSTRY, times = 1, magnitude = 1, year = 4)
        val settled = WorldSimulationEngine.settleCausalChains(state, GameRng(3L))
        assertFalse(settled.world.hasFlag("causal_causal_industry_pollution"))
    }

    @Test
    fun causalChainRespectsMinimumYear() {
        val state = withPolicies(PolicyTag.INDUSTRY, times = 4, magnitude = 3, year = 1)
        val settled = WorldSimulationEngine.settleCausalChains(state, GameRng(3L))
        assertFalse(settled.world.hasFlag("causal_causal_industry_pollution"))
    }

    @Test
    fun eraReflectsWorldPressure() {
        val world = WorldState(
            economy = GlobalEconomy(index = 20, phase = EconomicPhase.RECESSION),
            environmentClimate = 60
        )
        assertEquals(EraState.PRESSURE, WorldSimulationEngine.eraOf(world, 3))
    }

    // ===== 世界事件 =====

    private val testEvent = WorldEventTemplate(
        id = "test_event",
        title = "测试世界事件",
        detail = "用于测试的世界事件。",
        scope = WorldEventScope.GLOBAL,
        effect = com.shitu.app.game.WorldEventEffect(district = DistrictDelta(economy = 5))
    )

    @Test
    fun worldEventApplicationWritesNews() {
        val state = WorldSimulationEngine.applyWorldEvent(game(), testEvent, GameRng(2L))
        assertTrue(state.world.news.any { it.title == "测试世界事件" })
    }

    @Test
    fun globalWorldEventAffectsAllDistricts() {
        val base = WorldSimulationEngine.ensureWorld(game())
        val before = RegionCatalog.regions.associate { it.id to base.world.districtOf(it.id).economy }
        val after = WorldSimulationEngine.applyWorldEvent(base, testEvent, GameRng(2L))
        RegionCatalog.regions.forEach { region ->
            assertEquals(before[region.id]!! + 5, after.world.districtOf(region.id).economy)
        }
    }

    @Test
    fun playerRegionEventOnlyAffectsPlayerRegion() {
        val base = WorldSimulationEngine.ensureWorld(game())
        val localEvent = testEvent.copy(id = "test_local", scope = WorldEventScope.PLAYER_REGION)
        val otherBefore = base.world.districtOf("guilin").economy
        val after = WorldSimulationEngine.applyWorldEvent(base, localEvent, GameRng(2L))
        assertEquals(otherBefore, after.world.districtOf("guilin").economy)
        assertTrue(after.world.districtOf("suzhou").economy >= base.world.districtOf("suzhou").economy)
    }

    // ===== 岗位空缺 =====

    @Test
    fun vacanciesEventuallyAppear() {
        var state = game(seed = 33L)
        val rng = GameRng(33L)
        var found = false
        repeat(120) {
            state = WorldSimulationEngine.advanceQuarter(state, rng)
            if (state.world.vacancies.any { !it.filled }) found = true
        }
        assertTrue("长时间运行后应当出现岗位空缺", found)
    }

    @Test
    fun promotionOpportunityBonusMatchesVacancy() {
        val base = game()
        val target = com.shitu.app.model.nextPromotionIndex(base.player.positionIndex)
        assertEquals(0, WorldSimulationEngine.promotionOpportunityBonus(base))
        val withVacancy = base.copy(
            world = base.world.withVacancy(
                Vacancy(positionIndex = target, regionId = base.regionId, reason = "测试", createdMonth = 0)
            )
        )
        assertTrue(WorldSimulationEngine.promotionOpportunityBonus(withVacancy) > 0)
    }

    @Test
    fun pruneVacanciesRemovesExpired() {
        val world = WorldState().withVacancy(
            Vacancy(positionIndex = 3, regionId = "suzhou", reason = "测试", createdMonth = 0)
        )
        val pruned = world.pruneVacancies(WorldState.VACANCY_TTL_MONTHS + 5)
        assertTrue(pruned.vacancies.isEmpty())
    }

    @Test
    fun worldNewsIsCapped() {
        var world = WorldState()
        repeat(120) { index ->
            world = world.withNews(
                com.shitu.app.model.WorldNews(
                    id = "n$index", year = 1, month = index, category = com.shitu.app.model.NewsCategory.WORLD,
                    title = "动态$index", detail = "内容"
                )
            )
        }
        assertTrue(world.news.size <= WorldState.MAX_NEWS)
    }

    // ===== 项目生命周期 =====

    @Test
    fun projectLifecycleGoesThroughOperatingToCompleted() {
        var state = game()
        state = state.copy(governance = state.governance.copy(finance = 500))
        state = ProjectEngine.start(state, "proj_road").first
        repeat(3) { state = ProjectEngine.advance(state).first }
        assertEquals(ProjectStatus.OPERATING, state.projects.first { it.id == "proj_road" }.status)
        repeat(2) { state = ProjectEngine.advance(state).first }
        assertEquals(ProjectStatus.COMPLETED, state.projects.first { it.id == "proj_road" }.status)
    }

    @Test
    fun operatingProjectProducesRevenue() {
        var state = game()
        state = state.copy(governance = state.governance.copy(finance = 500))
        state = ProjectEngine.start(state, "proj_road").first
        repeat(3) { state = ProjectEngine.advance(state).first }
        val before = state.governance.finance
        val after = ProjectEngine.advance(state).first
        assertTrue("运营期应当带来财政收益", after.governance.finance >= before)
    }

    // ===== 存档 =====

    @Test
    fun codecRoundTripPreservesWorldSimulation() {
        var state = game(seed = 55L)
        val rng = GameRng(55L)
        repeat(12) { state = WorldSimulationEngine.advanceQuarter(state, rng) }
        state = WorldSimulationEngine.recordPolicy(state, "测试", "测试", setOf(PolicyTag.INDUSTRY), 3)
        val text = GameCodec.encode(state)
        val restored = GameCodec.decode(text)
        assertNotNull(restored)
        assertEquals(state.world, restored!!.world)
        assertEquals(text, GameCodec.encode(restored))
    }

    @Test
    fun codecReadsVer4SaveWithoutPhaseThirteenFields() {
        var state = game(seed = 66L)
        repeat(6) { state = WorldSimulationEngine.advanceQuarter(state, GameRng(66L + it)) }
        val text = GameCodec.encode(state)
        val legacy = text.split("\n").filter { line ->
            !line.startsWith("world.month\t") &&
                !line.startsWith("world.econ.") &&
                !line.startsWith("world.era\t") &&
                !line.startsWith("world.district.") &&
                !line.startsWith("world.policy.") &&
                !line.startsWith("world.decision.") &&
                !line.startsWith("world.news.") &&
                !line.startsWith("world.npcruntime.") &&
                !line.startsWith("world.npcrel.") &&
                !line.startsWith("world.vacancy.")
        }.joinToString("\n").replaceFirst("version\t" + GameCodec.FORMAT_VERSION, "version\t4")
        val restored = GameCodec.decode(legacy)
        assertNotNull("旧存档应当可以读取", restored)
        val other = restored!!
        assertEquals(state.player.positionIndex, other.player.positionIndex)
        assertEquals(state.year, other.year)
        assertTrue(other.world.districts.isEmpty())
        // 读取后自动补齐世界，可以继续游戏。
        val upgraded = WorldSimulationEngine.ensureWorld(other)
        assertEquals(RegionCatalog.regions.size, upgraded.world.districts.size)
        assertEquals(NpcCatalog.npcs.size, upgraded.world.npcs.size)
    }

    // ===== 自动模拟 =====

    @Test
    fun simulateTenYearsKeepsWorldConsistent() {
        val (_, report) = SimulationTestEngine.simulateWithReport(game(seed = 101L), years = 10, seed = 101L)
        assertTrue(report.minPopulation >= DistrictState.MIN_POPULATION)
        assertTrue(report.maxPopulation <= DistrictState.MAX_POPULATION)
        assertTrue(report.minFinance >= 0)
        assertTrue(report.maxRisk <= 100)
        assertTrue(report.minApproval >= 0)
        assertTrue(report.maxApproval <= 100)
        assertTrue(report.maxEconomyIndex <= WorldSimulationEngine.ECONOMY_MAX)
        assertTrue(report.minEconomyIndex >= WorldSimulationEngine.ECONOMY_MIN)
    }

    @Test
    fun simulateThirtyYearsDoesNotCrash() {
        val state = SimulationTestEngine.simulateYears(game(seed = 202L), years = 30, seed = 202L)
        assertTrue(state.world.month > 0)
        assertEquals(RegionCatalog.regions.size, state.world.districts.size)
        assertTrue(state.world.economy.index in WorldSimulationEngine.ECONOMY_MIN..WorldSimulationEngine.ECONOMY_MAX)
    }

    @Test
    fun simulateThirtyYearsKeepsPopulationPositive() {
        val (_, report) = SimulationTestEngine.simulateWithReport(game(seed = 303L), years = 30, seed = 303L)
        assertTrue("人口不应为负", report.minPopulation >= DistrictState.MIN_POPULATION)
        assertTrue("财政不应为负", report.minFinance >= 0)
        assertTrue("派系影响力不应爆表", report.maxFactionInfluence <= 100)
    }

    @Test
    fun simulationProducesWorldNewsAndPolicies() {
        val (state, report) = SimulationTestEngine.simulateWithReport(game(seed = 404L), years = 6, seed = 404L)
        assertTrue(report.newsCount > 0)
        assertTrue("长时间运行应当留下世界动态", state.world.news.isNotEmpty())
    }

    @Test
    fun districtDominantIndustryIsConsistent() {
        val district = DistrictState.fromRegion(RegionCatalog.find("weifang")!!)
        assertEquals(Industry.AGRICULTURE, district.dominantIndustry)
    }
}