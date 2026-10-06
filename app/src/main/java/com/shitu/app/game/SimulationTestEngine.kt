package com.shitu.app.game

/**
 * 第十三阶段：自动模拟引擎（开发与测试用）。
 *
 * 让开发者 / 测试可以快速跑 1 / 5 / 10 / 20 / 30 年，检查世界不会出现
 * 数值爆炸、归零、人口负数、财政无限增长、派系失衡、NPC 无限晋升等问题。
 * 也用于平衡性审查，不参与正常游戏流程。
 */
object SimulationTestEngine {

    /** 模拟过程中记录的关键数值范围，用于断言「没有异常」。 */
    data class Report(
        val requestedYears: Int,
        val reachedYear: Int,
        val steps: Int,
        val finished: Boolean,
        val endingTitle: String,
        val promotions: Int,
        val reviews: Int,
        val minFinance: Int,
        val maxFinance: Int,
        val minRisk: Int,
        val maxRisk: Int,
        val minApproval: Int,
        val maxApproval: Int,
        val minPopulation: Int,
        val maxPopulation: Int,
        val maxEconomyIndex: Int,
        val minEconomyIndex: Int,
        val maxFactionInfluence: Int,
        val newsCount: Int,
        val policyCount: Int,
        val decisionCount: Int,
        val vacancyCount: Int
    )

    /**
     * 跑一局模拟：每个事件随机挑一个选项，直到到达目标年份或产生结局。
     */
    fun simulateYears(start: GameState, years: Int, seed: Long = GameRng.DEFAULT_SEED): GameState =
        simulateWithReport(start, years, seed).first

    /** 与 [simulateYears] 相同，但额外返回一份数值审查报告。 */
    fun simulateWithReport(
        start: GameState,
        years: Int,
        seed: Long = GameRng.DEFAULT_SEED
    ): Pair<GameState, Report> {
        val rng = GameRng(seed)
        var state = start
        var steps = 0
        val maxSteps = (years.coerceAtLeast(1) + 2) * 60

        var minFinance = Int.MAX_VALUE
        var maxFinance = Int.MIN_VALUE
        var minRisk = Int.MAX_VALUE
        var maxRisk = Int.MIN_VALUE
        var minApproval = Int.MAX_VALUE
        var maxApproval = Int.MIN_VALUE
        var minPopulation = Int.MAX_VALUE
        var maxPopulation = Int.MIN_VALUE
        var minEconomy = Int.MAX_VALUE
        var maxEconomy = Int.MIN_VALUE
        var maxFaction = 0

        fun sample(current: GameState) {
            val finance = current.governance.finance
            if (finance < minFinance) minFinance = finance
            if (finance > maxFinance) maxFinance = finance
            val risk = current.relationship.risk
            if (risk < minRisk) minRisk = risk
            if (risk > maxRisk) maxRisk = risk
            val approval = current.governance.jurisdiction.approval
            if (approval < minApproval) minApproval = approval
            if (approval > maxApproval) maxApproval = approval
            val population = current.world.districtOf(current.regionId).population
            if (population < minPopulation) minPopulation = population
            if (population > maxPopulation) maxPopulation = population
            val economyIndex = current.world.economy.index
            if (economyIndex < minEconomy) minEconomy = economyIndex
            if (economyIndex > maxEconomy) maxEconomy = economyIndex
            current.politics.factionInfluence.values.maxOrNull()?.let { if (it > maxFaction) maxFaction = it }
        }

        sample(state)
        while (!state.isFinished && state.year <= years && steps < maxSteps) {
            steps += 1
            state = when (state.phase) {
                GamePhase.EVENT -> {
                    val event = GameEngine.currentEvent(state)
                    if (event == null || event.choices.isEmpty()) {
                        GameEngine.proceedFromReview(state)
                    } else {
                        GameEngine.resolveChoice(state, rng.nextInt(event.choices.size))
                    }
                }
                GamePhase.OUTCOME -> GameEngine.advance(state)
                GamePhase.REVIEW -> GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> break
            }
            sample(state)
        }

        val report = Report(
            requestedYears = years,
            reachedYear = state.year,
            steps = steps,
            finished = state.isFinished,
            endingTitle = state.ending?.title.orEmpty(),
            promotions = state.promotionCount,
            reviews = state.reviewCount,
            minFinance = if (minFinance == Int.MAX_VALUE) 0 else minFinance,
            maxFinance = if (maxFinance == Int.MIN_VALUE) 0 else maxFinance,
            minRisk = if (minRisk == Int.MAX_VALUE) 0 else minRisk,
            maxRisk = if (maxRisk == Int.MIN_VALUE) 0 else maxRisk,
            minApproval = if (minApproval == Int.MAX_VALUE) 0 else minApproval,
            maxApproval = if (maxApproval == Int.MIN_VALUE) 0 else maxApproval,
            minPopulation = if (minPopulation == Int.MAX_VALUE) 0 else minPopulation,
            maxPopulation = if (maxPopulation == Int.MIN_VALUE) 0 else maxPopulation,
            maxEconomyIndex = if (maxEconomy == Int.MIN_VALUE) 0 else maxEconomy,
            minEconomyIndex = if (minEconomy == Int.MAX_VALUE) 0 else minEconomy,
            maxFactionInfluence = maxFaction,
            newsCount = state.world.news.size,
            policyCount = state.world.policies.size,
            decisionCount = state.world.decisions.size,
            vacancyCount = state.world.vacancies.size
        )
        return state to report
    }
}