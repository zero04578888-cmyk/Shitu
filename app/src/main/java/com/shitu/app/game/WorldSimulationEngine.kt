package com.shitu.app.game

import com.shitu.app.model.DecisionRecord
import com.shitu.app.model.DistrictDelta
import com.shitu.app.model.DistrictState
import com.shitu.app.model.EconomicPhase
import com.shitu.app.model.EraState
import com.shitu.app.model.Faction
import com.shitu.app.model.GlobalEconomy
import com.shitu.app.model.GovernanceAction
import com.shitu.app.model.GovernanceCategory
import com.shitu.app.model.Indicator
import com.shitu.app.model.LogEntry
import com.shitu.app.model.LogKind
import com.shitu.app.model.MeritKind
import com.shitu.app.model.NewsCategory
import com.shitu.app.model.NewsLevel
import com.shitu.app.model.NpcRuntime
import com.shitu.app.model.NpcTier
import com.shitu.app.model.PolicyRecord
import com.shitu.app.model.PolicyTag
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.Vacancy
import com.shitu.app.model.WorldNews
import com.shitu.app.model.WorldState

/**
 * 第十三阶段：世界模拟引擎。
 *
 * 这里是「世界自己会运转」的统一入口：经济周期、地区动态、人口与产业、
 * 财政、NPC 自主行动、派系漂移、长期政策后果、岗位空缺与世界事件。
 *
 * 设计原则：
 * - 时间只在玩家明确推进（结束今日 / 进入下季度 / 年度考核）时前进，不随打开页面推进；
 * - 所有数值都有上下限，避免 30 年模拟后出现爆炸、归零、NaN 等异常；
 * - 世界让玩家「什么都不做也在变」，但每次变化幅度都很小、可预期。
 * 所有数值都是虚构的游戏模拟参数。
 */
object WorldSimulationEngine {

    /** 一个季度等于世界时间 3 个月。 */
    const val MONTHS_PER_QUARTER = 3

    /** 每个季度最多有几位 NPC 自主行动，避免一次结算过重。 */
    const val MAX_NPC_ACTIONS_PER_QUARTER = 3

    /** 每季度发生一次世界事件的概率（百分比）。 */
    const val WORLD_EVENT_CHANCE = 22

    /** 每个季度最多结算一条长期因果链，避免同时爆发一堆后果。 */
    const val MAX_CHAINS_PER_QUARTER = 1

    /** 派系影响力每季度最多漂移 1 点，避免格局突变。 */
    const val FACTION_DRIFT_LIMIT = 1

    const val ECONOMY_MIN = 5
    const val ECONOMY_MAX = 95

    // ===== 初始化 =====

    /**
     * 补齐世界模拟所需的数据。旧存档（第 12 阶段以前）读入后没有这些字段，
     * 这里按默认值补上，保证可以继续游戏而不是崩溃。
     */
    fun ensureWorld(state: GameState): GameState {
        var world = state.world
        var changed = false

        // 地区动态：为每个虚构地区建立一份可变化的状态。
        if (world.districts.size < RegionCatalog.regions.size) {
            RegionCatalog.regions.forEach { region ->
                if (world.districts[region.id] == null) {
                    world = world.withDistrict(DistrictState.fromRegion(region))
                }
            }
            changed = true
        }

        // NPC 动态状态：即使是 NPC 也会自己成长、争岗位、建立关系。
        if (world.npcs.isEmpty()) {
            NpcCatalog.npcs.forEach { npc ->
                world = world.withRuntime(
                    NpcRuntime(
                        npcId = npc.id,
                        ability = (npc.influence + 10).coerceIn(20, 92),
                        merit = (npc.influence / 2 + 18).coerceIn(10, 82),
                        influence = npc.influence.coerceIn(0, 100),
                        positionIndex = tierPosition(npc.tier)
                    )
                )
            }
            changed = true
        }

        // 经济周期：旧存档没有经济状态时，由第 12 阶段的经济气候推导一个起点。
        if (world.economy == GlobalEconomy()) {
            val index = world.economyClimate.coerceIn(20, 85)
            val phase = phaseForIndex(index)
            world = world.withEconomy(
                GlobalEconomy(
                    phase = phase,
                    index = index,
                    momentum = momentumBase(phase),
                    monthsInPhase = 0,
                    history = listOf(phase)
                )
            )
            changed = true
        }

        val era = eraOf(world, state.year)
        if (world.era != era) {
            world = world.withEra(era)
            changed = true
        }
        return if (changed) state.copy(world = world) else state
    }

    /** 由一个经济指数推导周期阶段。 */
    fun phaseForIndex(index: Int): EconomicPhase = when {
        index >= 72 -> EconomicPhase.BOOM
        index >= 58 -> EconomicPhase.GROWTH
        index >= 42 -> EconomicPhase.STABLE
        index >= 32 -> EconomicPhase.SLOWDOWN
        index >= 24 -> EconomicPhase.RECESSION
        else -> EconomicPhase.RECOVERY
    }

    private fun momentumBase(phase: EconomicPhase): Int = when (phase) {
        EconomicPhase.BOOM -> 3
        EconomicPhase.GROWTH -> 2
        EconomicPhase.STABLE -> 0
        EconomicPhase.SLOWDOWN -> -2
        EconomicPhase.RECESSION -> -3
        EconomicPhase.RECOVERY -> 2
    }

    private fun tierPosition(tier: NpcTier): Int = when (tier) {
        NpcTier.CITY_LEADER -> 8
        NpcTier.COUNTY_LEADER -> 6
        NpcTier.MENTOR -> 6
        NpcTier.ORGANIZATION -> 5
        NpcTier.COLLEAGUE -> 3
        NpcTier.SUBORDINATE -> 1
        NpcTier.BUSINESS -> 1
    }

    // ===== 季度推进 =====

    /**
     * 推进一个季度的世界模拟：经济 -> 地区 -> 人口/产业 -> 财政 -> NPC ->
     * 世界事件 -> 长期因果 -> 派系 -> 岗位空缺。
     */
    fun advanceQuarter(state: GameState, rng: GameRng): GameState {
        var next = ensureWorld(state)
        var world = next.world.copy(month = next.world.month + MONTHS_PER_QUARTER)

        world = tickEconomy(world, rng)
        val beforeDistrict = world.districtOf(next.regionId)
        world = tickDistricts(world, rng)
        world = tickNpcs(world, rng)
        next = next.copy(world = world)
        next = syncPlayerJurisdiction(next, beforeDistrict)
        next = rollWorldEvent(next, rng)
        next = settleCausalChains(next, rng)
        next = tickFactions(next, rng)
        next = spawnVacancies(next, rng)
        world = next.world.pruneVacancies(next.world.month)
        world = world.withEra(eraOf(world, next.year))
        return next.copy(world = world)
    }

    /** 经济周期：带惯性，指数不会一月一变，周期之间平滑过渡。 */
    fun tickEconomy(world: WorldState, rng: GameRng): WorldState {
        val current = world.economy
        val noise = rng.nextInt(3) - 1
        val raw = current.index + momentumBase(current.phase) + noise
        // 均值回归：越接近极端，越容易回落，防止经济永远停在繁荣或永远起不来。
        val meanPull = when {
            raw >= 78 -> -1
            raw <= 24 -> 1
            else -> 0
        }
        val index = (raw + meanPull).coerceIn(ECONOMY_MIN, ECONOMY_MAX)
        var phase = current.phase
        var months = current.monthsInPhase + MONTHS_PER_QUARTER
        // 至少持续 6 个月才可能换阶段，避免周期抖动。
        if (months >= 6) {
            val nextPhase = transition(current.phase, index, rng)
            if (nextPhase != current.phase) {
                phase = nextPhase
                months = 0
            }
        }
        val momentum = momentumBase(phase) + (rng.nextInt(3) - 1)
        return world.withEconomy(
            GlobalEconomy(
                phase = phase,
                index = index,
                momentum = momentum.coerceIn(-4, 4),
                monthsInPhase = months,
                history = if (phase == current.phase) current.history else current.withPhase(phase).history
            )
        )
    }

    private fun transition(current: EconomicPhase, index: Int, rng: GameRng): EconomicPhase {
        val up = rng.nextInt(100) < 55
        return when (current) {
            EconomicPhase.BOOM -> if (index >= 76 && up) EconomicPhase.BOOM
            else if (up) EconomicPhase.GROWTH else EconomicPhase.STABLE
            EconomicPhase.GROWTH -> when {
                index >= 80 -> EconomicPhase.BOOM
                index <= 44 -> EconomicPhase.SLOWDOWN
                up -> EconomicPhase.GROWTH
                else -> EconomicPhase.STABLE
            }
            EconomicPhase.STABLE -> when {
                index >= 72 -> EconomicPhase.GROWTH
                index <= 34 -> EconomicPhase.RECESSION
                index <= 46 -> EconomicPhase.SLOWDOWN
                up -> EconomicPhase.GROWTH
                else -> EconomicPhase.SLOWDOWN
            }
            EconomicPhase.SLOWDOWN -> when {
                index <= 30 -> EconomicPhase.RECESSION
                index >= 58 -> EconomicPhase.STABLE
                up -> EconomicPhase.STABLE
                else -> EconomicPhase.SLOWDOWN
            }
            EconomicPhase.RECESSION -> if (index >= 40) EconomicPhase.RECOVERY else EconomicPhase.RECOVERY
            EconomicPhase.RECOVERY -> when {
                index >= 62 -> EconomicPhase.GROWTH
                index <= 26 -> EconomicPhase.RECESSION
                up -> EconomicPhase.GROWTH
                else -> EconomicPhase.STABLE
            }
        }
    }

    /** 所有地区的自然变化：经济、产业、就业、环境、吸引力、人口与财政。 */
    fun tickDistricts(world: WorldState, rng: GameRng): WorldState {
        val phaseTendency = momentumBase(world.economy.phase)
        var result = world
        RegionCatalog.regions.forEach { region ->
            val d = result.districtOf(region.id)
            var delta = DistrictDelta()
            delta += DistrictDelta(
                economy = (phaseTendency + (d.industry - 48) / 26 + (d.logistics - 48) / 40).coerceIn(-2, 2)
            )
            delta += DistrictDelta(
                industry = ((d.economy - 50) / 32 + phaseTendency / 2).coerceIn(-1, 1),
                agriculture = ((d.environment - 50) / 40).coerceIn(-1, 1),
                service = ((d.economy - 50) / 30 + (d.tourism - 40) / 60).coerceIn(-1, 1),
                logistics = ((d.infrastructure - 50) / 32).coerceIn(-1, 1)
            )
            delta += DistrictDelta(
                environment = when {
                    d.industry >= 66 -> -1
                    d.industry <= 34 -> 1
                    else -> 0
                },
                employment = ((d.economy - 50) / 26).coerceIn(-1, 1)
            )
            delta += DistrictDelta(
                attractiveness = when {
                    d.environment >= 64 && d.medical >= 54 -> 1
                    d.environment <= 40 -> -1
                    else -> 0
                },
                population = when {
                    d.attractiveness >= 62 && d.employment >= 54 -> 1
                    d.attractiveness <= 38 || d.environment <= 34 -> -1
                    else -> 0
                }
            )
            delta += DistrictDelta(
                finance = ((d.economy + d.industry) / 2 - 50).let { (it / 22).coerceIn(-2, 2) }
            )
            result = result.withDistrict(d.apply(delta))
        }
        return result
    }

    /**
     * NPC 自主行动：每季度有几位 NPC 在成长、做事、建立关系。
     * 关键人物会写进世界动态，普通人物只改数值，避免信息过载。
     */
    fun tickNpcs(world: WorldState, rng: GameRng): WorldState {
        var result = world
        val count = 1 + rng.nextInt(MAX_NPC_ACTIONS_PER_QUARTER)
        repeat(count) {
            val profile = NpcCatalog.npcs[rng.nextInt(NpcCatalog.npcs.size)]
            val runtime = result.runtimeOf(profile.id)
            // 同一个 NPC 至少间隔一个季度才再行动，避免刷屏。
            if (result.month - runtime.lastActionMonth < MONTHS_PER_QUARTER && runtime.lastActionMonth >= 0) {
                return@repeat
            }
            val abilityGain = rng.nextInt(3)
            val meritGain = rng.nextInt(4)
            val influenceGain = rng.nextInt(3) - 1
            var updated = runtime.copy(
                ability = (runtime.ability + abilityGain).coerceAtMost(100),
                merit = (runtime.merit + meritGain).coerceAtMost(100),
                influence = (runtime.influence + influenceGain).coerceIn(0, 100),
                lastActionMonth = result.month,
                agenda = npcAgenda(runtime)
            )
            // 小概率发生调离或退休，人物网络会自然更替。
            if (rng.nextInt(100) < 4 && updated.active && updated.positionIndex <= 2) {
                updated = updated.copy(active = false, agenda = "已调离")
                result = result.withNews(
                    WorldNews(
                        id = "npc_leave_${profile.id}_${result.month}",
                        year = currentYear(result),
                        month = result.month,
                        category = NewsCategory.NPC,
                        title = "${profile.name}调离本地",
                        detail = "因为工作需要，${profile.name}离开了现有岗位。",
                        level = NewsLevel.LOW
                    )
                )
            }
            result = result.withRuntime(updated)
            // 与另一位 NPC 的关系也会自然变化，形成动态的人脉网络。
            val other = NpcCatalog.npcs[rng.nextInt(NpcCatalog.npcs.size)].id
            if (other != profile.id) {
                result = result.withNpcRelation(profile.id, other, rng.nextInt(5) - 2)
            }
        }
        return result
    }

    private fun npcAgenda(runtime: NpcRuntime): String = when {
        runtime.merit >= 70 -> "正准备争取一个更重要的岗位"
        runtime.influence >= 70 -> "在经营自己的关系网"
        runtime.ability >= 70 -> "把手头的项目做出了起色"
        else -> "按部就班地做事"
    }

    private fun currentYear(world: WorldState): Int = (world.month / 12) + 1

    /**
     * 地区变化回灌到玩家辖区：让「我治理的地方真的在变」。
     * 幅度按 1/3 折算，且每季度最多 ±2，避免与玩家的治理操作互相打架。
     */
    private fun syncPlayerJurisdiction(state: GameState, before: DistrictState): GameState {
        val after = state.world.districtOf(state.regionId)
        var gov = state.governance
        fun nudge(indicator: Indicator, delta: Int) {
            val scaled = (delta / 3).coerceIn(-2, 2)
            if (scaled != 0) gov = gov.withIndicator(indicator, scaled)
        }
        nudge(Indicator.ECONOMY, after.economy - before.economy)
        nudge(Indicator.LIVELIHOOD, after.employment - before.employment)
        nudge(Indicator.ENVIRONMENT, after.environment - before.environment)
        nudge(Indicator.INFRASTRUCTURE, after.infrastructure - before.infrastructure)
        nudge(Indicator.EDUCATION, after.education - before.education)
        nudge(Indicator.MEDICAL, after.medical - before.medical)
        nudge(Indicator.SECURITY, after.security - before.security)
        return state.copy(governance = gov)
    }

    // ===== 世界事件 =====

    fun rollWorldEvent(state: GameState, rng: GameRng): GameState {
        if (rng.nextInt(100) >= WORLD_EVENT_CHANCE) return state
        val template = WorldEventLibrary.pick(state, rng) ?: return state
        return applyWorldEvent(state, template, rng)
    }

    /** 应用一个世界事件：影响若干地区、经济指数，并写入世界动态。 */
    fun applyWorldEvent(state: GameState, template: WorldEventTemplate, rng: GameRng): GameState {
        var world = applyEffectToWorld(state.world, template.scope, template.effect, state.regionId, rng)
        world = world.withNews(
            WorldNews(
                id = "we_${template.id}_${world.month}",
                year = state.year,
                month = world.month,
                category = template.category,
                title = template.title,
                detail = template.detail,
                level = template.level,
                regionId = if (template.scope == WorldEventScope.PLAYER_REGION) state.regionId else ""
            )
        )
        var next = applyPlayerEffect(state.copy(world = world), template.effect)
        if (template.level == NewsLevel.HIGH) {
            next = next.copy(
                log = next.log + LogEntry(
                    year = next.year,
                    quarter = next.quarter,
                    title = "世界动态 · ${template.title}",
                    choiceText = template.category.label,
                    outcome = template.detail,
                    deltas = emptyList(),
                    kind = LogKind.WARNING
                )
            )
        }
        return next
    }

    private fun applyEffectToWorld(
        world: WorldState,
        scope: WorldEventScope,
        effect: WorldEventEffect,
        playerRegionId: String,
        rng: GameRng
    ): WorldState {
        if (effect.district.isEmpty && effect.economyIndex == 0 && effect.momentum == 0) return world
        var result = world
        val targets = when (scope) {
            WorldEventScope.GLOBAL -> RegionCatalog.regions.map { it.id }
            WorldEventScope.PLAYER_REGION -> listOf(playerRegionId)
            WorldEventScope.RANDOM_REGION ->
                RegionCatalog.regions.map { it.id }.filter { it != playerRegionId }
                    .takeIf { it.isNotEmpty() }?.let { listOf(it[rng.nextInt(it.size)]) } ?: listOf(playerRegionId)
        }
        targets.forEach { id ->
            val district = result.districtOf(id)
            result = result.withDistrict(district.apply(effect.district))
        }
        if (effect.economyIndex != 0 || effect.momentum != 0) {
            val econ = result.economy
            result = result.withEconomy(
                econ.copy(
                    index = (econ.index + effect.economyIndex).coerceIn(ECONOMY_MIN, ECONOMY_MAX),
                    momentum = (econ.momentum + effect.momentum).coerceIn(-4, 4)
                )
            )
        }
        return result
    }

    private fun applyPlayerEffect(state: GameState, effect: WorldEventEffect): GameState {
        var next = state
        if (effect.playerFinance != 0) {
            next = next.copy(governance = next.governance.withFinance(effect.playerFinance))
        }
        if (effect.playerRisk != 0) {
            next = next.copy(
                relationship = next.relationship.withRisk(next.relationship.risk + effect.playerRisk)
            )
        }
        if (effect.playerApproval != 0) {
            next = next.copy(
                governance = next.governance.withIndicator(Indicator.APPROVAL, effect.playerApproval)
            )
        }
        if (effect.playerMerit.isNotEmpty()) {
            var gov = next.governance
            effect.playerMerit.forEach { (kind, delta) -> gov = gov.withMerit(kind, delta) }
            next = next.copy(governance = gov)
        }
        return next
    }

    // ===== 长期因果 =====

    /** 玩家某种政策取向的累计强度：政策记录 + 重大决策记忆。 */
    fun effectivePressure(state: GameState, tag: PolicyTag): Int =
        state.world.policyPressure(tag, state.year) + state.world.decisionsWith(tag).size * 2

    /** 结算长期因果链：过去的选择会在若干年后以结果的形式回来。 */
    fun settleCausalChains(state: GameState, rng: GameRng): GameState {
        var next = state
        var settled = 0
        for (chain in WorldEventLibrary.chains) {
            if (settled >= MAX_CHAINS_PER_QUARTER) break
            if (next.world.hasFlag("causal_${chain.id}")) continue
            if (next.year < chain.minYear) continue
            if (effectivePressure(next, chain.tag) < chain.threshold) continue
            val environment = next.world.districtOf(next.regionId).environment
            if (environment > chain.maxEnvironment || environment < chain.minEnvironment) continue
            next = applyCausalChain(next, chain, rng)
            settled += 1
        }
        return next
    }

    private fun applyCausalChain(state: GameState, chain: CausalChain, rng: GameRng): GameState {
        var world = applyEffectToWorld(state.world, chain.scope, chain.effect, state.regionId, rng)
        world = world.withFlag("causal_${chain.id}").withNews(
            WorldNews(
                id = "cc_${chain.id}_${state.year}",
                year = state.year,
                month = world.month,
                category = NewsCategory.POLICY,
                title = chain.title,
                detail = chain.detail,
                level = chain.level,
                regionId = if (chain.scope == WorldEventScope.PLAYER_REGION) state.regionId else ""
            )
        )
        var next = applyPlayerEffect(state.copy(world = world), chain.effect)
        // 写入履历，让玩家能回看「当初的选择带来了什么」。
        next = CareerEngine.record(
            next,
            com.shitu.app.model.CareerRecordKind.PROJECT,
            "长期影响 · ${chain.title}",
            chain.detail
        )
        return next
    }

    // ===== 派系与岗位 =====

    /** 派系影响力随时代缓慢漂移，每季度最多 1 点，绝不突变。 */
    fun tickFactions(state: GameState, rng: GameRng): GameState {
        if (rng.nextInt(100) >= 40) return state
        val favored = when (state.world.era) {
            EraState.PROSPERITY -> Faction.PRAGMATIC
            EraState.REFORM -> Faction.REFORM
            EraState.PRESSURE -> Faction.CONSERVATIVE
            EraState.ADJUSTMENT -> Faction.LOCAL
            EraState.DEVELOPMENT -> Faction.CORE
        }
        var politics = state.politics.withInfluence(favored, FACTION_DRIFT_LIMIT)
        favored.rivals.forEach { rival ->
            politics = politics.withInfluence(rival, -FACTION_DRIFT_LIMIT)
        }
        return state.copy(politics = politics)
    }

    /** 随机出现岗位空缺，让晋升环境是动态的。 */
    fun spawnVacancies(state: GameState, rng: GameRng): GameState {
        val target = com.shitu.app.model.nextPromotionIndex(state.player.positionIndex)
        if (state.world.openVacancy(target) != null) return state
        if (rng.nextInt(100) >= 15) return state
        val region = RegionCatalog.regions[rng.nextInt(RegionCatalog.regions.size)]
        val reason = listOf(
            "前任到龄退休", "干部交流调整", "组织部门统筹配备", "岗位职责调整"
        )[rng.nextInt(4)]
        val vacancy = Vacancy(
            positionIndex = target,
            regionId = region.id,
            reason = reason,
            createdMonth = state.world.month
        )
        val world = state.world.withVacancy(vacancy).withNews(
            WorldNews(
                id = "vac_${target}_${world2Month(state)}",
                year = state.year,
                month = state.world.month,
                category = NewsCategory.FACTION,
                title = "${com.shitu.app.model.positionAt(target).name}出现空缺",
                detail = "${region.name}因${reason}空出一个岗位，符合条件的人选都会进入视野。",
                level = NewsLevel.MEDIUM,
                regionId = region.id
            )
        )
        return state.copy(world = world)
    }

    private fun world2Month(state: GameState): Int = state.world.month

    /** 当前是否存在与玩家下一级岗位匹配的空缺。用于晋升机会的动态判断。 */
    fun promotionOpportunityBonus(state: GameState): Int {
        val target = com.shitu.app.model.nextPromotionIndex(state.player.positionIndex)
        val open = state.world.openVacancy(target) ?: return 0
        val regionBonus = if (open.regionId == state.regionId) 2 else 1
        return (2 + regionBonus).coerceAtMost(4)
    }

    // ===== 年度结算 =====

    /**
     * 年度世界结算：人口与产业的年变化、NPC 年度成长、派系格局、
     * 世界事件兜底与时代更新。比季度结算更重，但一年只跑一次。
     */
    fun advanceYear(state: GameState, rng: GameRng): GameState {
        var next = ensureWorld(state)
        var world = next.world

        // 年度人口与产业：把季度的小变化累积成年度趋势。
        RegionCatalog.regions.forEach { region ->
            val d = world.districtOf(region.id)
            val delta = DistrictDelta(
                population = when {
                    d.attractiveness >= 66 && d.employment >= 56 -> 1
                    d.attractiveness <= 36 -> -1
                    else -> 0
                },
                education = if (state.governance.merit.livelihood >= 40) 1 else 0,
                medical = if (state.governance.merit.livelihood >= 50) 1 else 0
            )
            world = world.withDistrict(d.apply(delta))
        }

        // NPC 年度成长：能力、政绩与影响力自然上涨，但没有无限上限。
        val profiles = NpcCatalog.npcs
        profiles.forEach { npc ->
            val runtime = world.runtimeOf(npc.id)
            val growth = 1 + rng.nextInt(3)
            world = world.withRuntime(
                runtime.copy(
                    ability = (runtime.ability + growth).coerceAtMost(100),
                    merit = (runtime.merit + growth).coerceAtMost(100),
                    influence = (runtime.influence + rng.nextInt(2)).coerceIn(0, 100)
                )
            )
        }

        next = next.copy(world = world)
        // 一年至少要发生一次世界事件，保证世界「有动静」。
        val hasThisYear = next.world.news.any { it.year == next.year }
        if (!hasThisYear) {
            rollWorldEvent(next, rng).let { next = it }
        }
        world = next.world.withEra(eraOf(next.world, next.year))
        return next.copy(world = world)
    }

    // ===== 政策与决策记录 =====

    /** 记录一条政策取向。措施、项目与重大决策都会写进这里，供未来读取。 */
    fun recordPolicy(
        state: GameState,
        title: String,
        direction: String,
        tags: Set<PolicyTag>,
        magnitude: Int = 1,
        summary: String = ""
    ): GameState {
        if (tags.isEmpty()) return state
        val id = "pol_${state.absoluteQuarter}_${title.take(10)}_${tags.first().name}"
        val record = PolicyRecord(
            id = id,
            year = state.year,
            month = state.world.month,
            title = title,
            direction = direction,
            regionId = state.regionId,
            tags = tags,
            magnitude = magnitude.coerceIn(1, 6),
            summary = summary
        )
        return state.copy(world = state.world.withPolicy(record))
    }

    /** 记录一条重大决策记忆。未来事件与因果链可以读取。 */
    fun recordDecision(
        state: GameState,
        id: String,
        title: String,
        choice: String,
        consequence: String,
        tags: Set<PolicyTag> = emptySet(),
        knownBy: List<String> = emptyList()
    ): GameState {
        val record = DecisionRecord(
            id = id,
            year = state.year,
            month = state.world.month,
            title = title,
            choice = choice,
            consequence = consequence,
            knownBy = knownBy,
            regionId = state.regionId,
            tags = tags
        )
        return state.copy(world = state.world.withDecision(record))
    }

    /** 政务措施对应的政策方向。 */
    fun policyTagsOf(action: GovernanceAction): Set<PolicyTag> {
        if (action.category == GovernanceCategory.PERSONAL) return emptySet()
        val tags = mutableSetOf<PolicyTag>()
        when (action.category) {
            GovernanceCategory.ECONOMY -> tags += PolicyTag.INDUSTRY
            GovernanceCategory.LIVELIHOOD -> tags += PolicyTag.WELFARE
            GovernanceCategory.INFRASTRUCTURE -> tags += PolicyTag.INFRASTRUCTURE
            GovernanceCategory.SECURITY -> tags += PolicyTag.SECURITY
            GovernanceCategory.ENVIRONMENT -> tags += PolicyTag.ENVIRONMENT
            GovernanceCategory.PERSONAL -> Unit
        }
        val id = action.id
        if (id.contains("invest") || id.contains("industry") || id.contains("subsidy")) {
            tags += PolicyTag.INDUSTRY
            tags += PolicyTag.FINANCE
        }
        if (id.contains("school")) tags += PolicyTag.EDUCATION
        if (id.contains("medical") || id.contains("elderly")) tags += PolicyTag.HEALTH
        if (id.contains("jobs") || id.contains("housing")) tags += PolicyTag.WELFARE
        if (id.contains("pollution") || id.contains("green") || id.contains("sanitation")) {
            tags += PolicyTag.ENVIRONMENT
        }
        if (id.contains("study")) tags += PolicyTag.TALENT
        if (id.contains("reform")) tags += PolicyTag.REFORM
        return tags
    }

    /** 事件标签对应的政策方向，用于把重大抉择写入政策记忆。 */
    fun policyTagsOf(eventTags: Set<com.shitu.app.model.EventTag>): Set<PolicyTag> {
        val tags = mutableSetOf<PolicyTag>()
        eventTags.forEach { tag ->
            when (tag) {
                com.shitu.app.model.EventTag.PROJECT -> tags += PolicyTag.INFRASTRUCTURE
                com.shitu.app.model.EventTag.RURAL -> tags += PolicyTag.AGRICULTURE
                com.shitu.app.model.EventTag.INSPECTION,
                com.shitu.app.model.EventTag.INTEGRITY,
                com.shitu.app.model.EventTag.GIFT -> tags += PolicyTag.INTEGRITY
                com.shitu.app.model.EventTag.PUBLIC_OPINION,
                com.shitu.app.model.EventTag.COMPLAINT,
                com.shitu.app.model.EventTag.MASS_APPEAL,
                com.shitu.app.model.EventTag.PETITION -> tags += PolicyTag.WELFARE
                com.shitu.app.model.EventTag.EMERGENCY,
                com.shitu.app.model.EventTag.DISASTER,
                com.shitu.app.model.EventTag.FLOOD,
                com.shitu.app.model.EventTag.ACCIDENT -> tags += PolicyTag.SECURITY
                com.shitu.app.model.EventTag.RESEARCH -> tags += PolicyTag.TECHNOLOGY
                else -> Unit
            }
        }
        return tags
    }

    // ===== 时代 =====

    /** 由经济、环境与改革压力推导时代。 */
    fun eraOf(world: WorldState, year: Int): EraState {
        val environment = playerEnvironment(world)
        val reform = world.policyPressure(PolicyTag.REFORM, year) + world.decisionsWith(PolicyTag.REFORM).size * 2
        return EraState.of(world.economy.index, environment, reform)
    }

    /** 世界整体的环境水平：取所有地区的平均，避免只看一个地区。 */
    fun playerEnvironment(world: WorldState): Int {
        if (world.districts.isEmpty()) return world.environmentClimate
        return world.districts.values.sumOf { it.environment } / world.districts.size
    }

    // ===== 界面辅助 =====

    /** 世界整体发展水平（0～100）。 */
    fun worldDevelopment(world: WorldState): Int {
        if (world.districts.isEmpty()) return 50
        return world.districts.values.sumOf { it.development } / world.districts.size
    }

    /** 经济周期对玩家财政收入的修正。 */
    fun incomeModifier(state: GameState): Int = when (state.world.economy.phase) {
        EconomicPhase.BOOM -> 6
        EconomicPhase.GROWTH -> 3
        EconomicPhase.STABLE -> 0
        EconomicPhase.SLOWDOWN -> -3
        EconomicPhase.RECESSION -> -6
        EconomicPhase.RECOVERY -> 1
    }
}