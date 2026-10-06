package com.shitu.app.game

import com.shitu.app.model.ActiveProject
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.EconomicPhase
import com.shitu.app.model.GovernanceEffect
import com.shitu.app.model.GovernanceState
import com.shitu.app.model.NewsCategory
import com.shitu.app.model.NewsLevel
import com.shitu.app.model.ProjectCatalog
import com.shitu.app.model.ProjectProfile
import com.shitu.app.model.ProjectReward
import com.shitu.app.model.ProjectStatus
import com.shitu.app.model.WorldNews

/** 项目过程中可能遇到的波折。全部为虚构的游戏设定。 */
enum class ProjectIncident(val label: String, val detail: String, val extraCost: Int) {
    FUNDING("资金问题", "一笔配套资金没有按时到位，工人等了两周。", 6),
    CONSTRUCTION("施工问题", "地质条件比预想的复杂，施工方案要重新调整。", 5),
    MASS_OPINION("群众意见", "附近群众对施工噪音和出行有意见，需要先做工作。", 3),
    TECHNOLOGY("技术问题", "关键设备的技术参数不达标，需要返工。", 5),
    INVESTMENT("招商变化", "原本谈好的合作方态度变了，要重新对接。", 4),
    INSPECTION("上级检查", "项目被列入检查名单，需要停下来准备材料。", 2);

    companion object {
        /** 按项目风险与当下经济环境挑一个波折。 */
        fun pick(rng: GameRng, profile: ProjectProfile, penalty: Int): ProjectIncident {
            val pool = entries.toMutableList()
            if (profile.category == com.shitu.app.model.ProjectCategory.INDUSTRY) pool += INVESTMENT
            if (profile.risk >= 3) pool += INSPECTION
            if (penalty >= 12) pool += FUNDING
            return pool[rng.nextInt(pool.size)]
        }
    }
}

/**
 * 第十二/十三阶段：重大项目引擎。
 *
 * 项目是跨季度、跨年度的持续投入：建设期扣财政、推进度，遇到波折会延期；
 * 建成后进入运营期，持续产生收益，运营期结束才算全面收官。
 * 财政断档会停工。所有数值都是虚构的游戏模拟参数。
 */
object ProjectEngine {

    fun activeOf(state: GameState): List<ActiveProject> = state.projects.filter { it.isActive }

    fun findActive(state: GameState, projectId: String): ActiveProject? =
        state.projects.firstOrNull { it.id == projectId && it.isActive }

    fun hasBuilt(state: GameState, projectId: String): Boolean = state.world.hasBuilt(projectId)

    /** 当前可以开工的项目：职位够、没在建也没停工、没建成。 */
    fun available(state: GameState): List<ProjectProfile> {
        val trait = RegionEngine.regionOf(state).trait
        return ProjectCatalog.availableFor(state.player.positionIndex, trait).filter { profile ->
            state.projects.none { it.id == profile.id } && !hasBuilt(state, profile.id)
        }
    }

    /** 某个项目对本地区是否有特色加成。 */
    fun isBoosted(state: GameState, profile: ProjectProfile): Boolean =
        RegionEngine.regionOf(state).trait in profile.traitBonus

    fun start(state: GameState, projectId: String): Pair<GameState, String> {
        val profile = ProjectCatalog.find(projectId)
            ?: return state to "没有这个项目。"
        if (profile.minPositionIndex > state.player.positionIndex) {
            return state to "以你现在的职务，还牵不动这个项目。"
        }
        if (state.projects.any { it.id == projectId }) return state to "这个项目已经在推进中。"
        if (hasBuilt(state, projectId)) return state to "这个项目已经完工了。"
        if (state.governance.finance < profile.quarterlyCost) {
            return state to "财政拿不出首期投入（需要 ${profile.quarterlyCost}）。"
        }
        val project = ActiveProject(
            id = projectId,
            startedQuarter = state.absoluteQuarter,
            invested = profile.quarterlyCost,
            progress = 1
        )
        var next = state.copy(
            projects = state.projects + project,
            governance = state.governance.copy(finance = state.governance.finance - profile.quarterlyCost)
        )
        next = CareerEngine.record(
            next, CareerRecordKind.PROJECT,
            "启动 ${profile.label}",
            "决定上马${profile.label}，预算 ${profile.budget}，建设周期 ${profile.quarters} 个季度，运营期 ${profile.operationQuarters} 个季度。"
        )
        // 第十三阶段：项目代表一种长期政策投入，记入政策历史。
        next = WorldSimulationEngine.recordPolicy(
            next,
            title = "上马${profile.label}",
            direction = profile.category.label,
            tags = profile.policyTags,
            magnitude = 3,
            summary = profile.description
        )
        next = next.copy(
            world = next.world.withNews(
                projectNews(
                    next, profile, NewsLevel.MEDIUM,
                    "「${profile.label}」开工建设", "项目进入建设阶段，预计 ${profile.quarters} 个季度建成。"
                )
            )
        )
        return next to "「${profile.label}」已开工，本季度投入 ${profile.quarterlyCost}。"
    }

    /** 停工的项目重新复工（财政允许时）。 */
    fun resume(state: GameState, projectId: String): Pair<GameState, String> {
        val project = state.projects.firstOrNull { it.id == projectId }
            ?: return state to "没有这个项目。"
        if (project.status != ProjectStatus.STALLED) return state to "这个项目不需要复工。"
        val profile = ProjectCatalog.find(projectId) ?: return state to "没有这个项目。"
        if (state.governance.finance < profile.quarterlyCost) {
            return state to "财政仍然紧张，暂时无法复工。"
        }
        val updated = state.projects.map {
            if (it.id == projectId) it.copy(status = ProjectStatus.ONGOING, note = "") else it
        }
        return state.copy(projects = updated) to "「${profile.label}」已复工。"
    }

    /** 项目遇到的波折概率：受风险、经济周期与环境共同影响。 */
    fun incidentChance(state: GameState, profile: ProjectProfile): Int {
        val economyPenalty = when (state.world.economy.phase) {
            EconomicPhase.RECESSION -> 18
            EconomicPhase.SLOWDOWN -> 10
            EconomicPhase.RECOVERY -> 4
            EconomicPhase.BOOM -> -4
            else -> 0
        }
        val environmentPenalty = if (state.governance.jurisdiction.environment < 40) 6 else 0
        return (6 + economyPenalty + profile.risk * 3 + environmentPenalty).coerceIn(0, 45)
    }

    /**
     * 季度推进。传入 rng 时会掷出项目波折；不传则按确定性推进（便于测试）。
     */
    fun advance(state: GameState, rng: GameRng? = null): Pair<GameState, List<String>> {
        if (state.projects.none { it.isActive }) return state to emptyList()
        val notes = mutableListOf<String>()
        var gov = state.governance
        var world = state.world
        var risk = state.relationship.risk
        val operatingNow = mutableListOf<ProjectProfile>()
        val closedNow = mutableListOf<ProjectProfile>()

        val updated = state.projects.map { project ->
            val profile = ProjectCatalog.find(project.id) ?: return@map project
            when (project.status) {
                ProjectStatus.OPERATING -> {
                    val left = project.operationLeft - 1
                    gov = gov.withFinance(profile.revenuePerQuarter)
                    if (left <= 0) {
                        closedNow += profile
                        notes += "「${profile.label}」运营期结束，项目全面收官。"
                        world = world.withNews(
                            projectNews(
                                state, profile, NewsLevel.LOW,
                                "「${profile.label}」运营收官",
                                "项目走完整个运营周期，成为本地一项稳定的长期资产。"
                            )
                        )
                        project.copy(operationLeft = 0, status = ProjectStatus.COMPLETED)
                    } else {
                        project.copy(operationLeft = left)
                    }
                }

                ProjectStatus.ONGOING -> {
                    if (gov.finance < profile.quarterlyCost) {
                        notes += "「${profile.label}」因财政不足停工，需要复工才能继续。"
                        world = world.withNews(
                            projectNews(
                                state, profile, NewsLevel.MEDIUM,
                                "「${profile.label}」停工", "财政没有跟上，工程暂时停了下来。"
                            )
                        )
                        project.copy(status = ProjectStatus.STALLED, note = "财政不足")
                    } else {
                        val incident = rollIncident(state, profile, rng)
                        if (incident != null) {
                            gov = gov.withFinance(-incident.extraCost)
                            notes += "「${profile.label}」遇到${incident.label}：${incident.detail}"
                            world = world.withNews(
                                projectNews(
                                    state, profile, NewsLevel.LOW,
                                    "「${profile.label}」${incident.label}",
                                    incident.detail
                                )
                            )
                            project.copy(
                                invested = project.invested + incident.extraCost,
                                incidents = project.incidents + 1,
                                note = incident.label
                            )
                        } else {
                            gov = gov.withFinance(-profile.quarterlyCost)
                            gov = applyEffect(gov, profile.perQuarter)
                            val progress = project.progress + 1
                            if (progress >= profile.quarters) {
                                val boost = if (RegionEngine.regionOf(state).trait in profile.traitBonus) 2 else 0
                                gov = applyEffect(gov, profile.reward.toEffect(boost))
                                risk += profile.reward.risk + profile.risk
                                world = world.withBuilt(profile.id).withFlag("built_${profile.id}")
                                    .withNews(
                                        projectNews(
                                            state, profile, NewsLevel.HIGH,
                                            "「${profile.label}」建成投用",
                                            "项目进入运营期，开始持续产生效益。"
                                        )
                                    )
                                operatingNow += profile
                                notes += "「${profile.label}」建成投用，进入运营期。"
                                project.copy(
                                    invested = project.invested + profile.quarterlyCost,
                                    progress = progress,
                                    status = ProjectStatus.OPERATING,
                                    operationLeft = profile.operationQuarters,
                                    note = "运营中"
                                )
                            } else {
                                project.copy(
                                    invested = project.invested + profile.quarterlyCost,
                                    progress = progress,
                                    note = ""
                                )
                            }
                        }
                    }
                }

                else -> project
            }
        }

        val kept = updated.filter { it.status != ProjectStatus.COMPLETED }.takeLast(10)
        val done = updated.filter { it.status == ProjectStatus.COMPLETED }.takeLast(6)
        var next = state.copy(
            projects = kept + done,
            governance = gov,
            world = world,
            relationship = state.relationship.withRisk(risk)
        )
        operatingNow.forEach { profile ->
            next = CareerEngine.record(
                next, CareerRecordKind.PROJECT,
                "完工 ${profile.label}",
                profile.reward.record.ifEmpty { "${profile.label}建成投用。" }
            )
            next = CareerEngine.gainExperience(next, 12)
        }
        if (closedNow.isNotEmpty()) {
            next = CareerEngine.gainExperience(next, 4 * closedNow.size)
        }
        if (notes.isNotEmpty()) {
            next = next.copy(
                log = next.log + com.shitu.app.model.LogEntry(
                    year = next.year,
                    quarter = next.quarter,
                    title = "项目推进",
                    choiceText = "季度结算",
                    outcome = notes.joinToString("\n"),
                    deltas = emptyList(),
                    kind = com.shitu.app.model.LogKind.PROJECT
                )
            )
        }
        return next to notes
    }

    private fun rollIncident(state: GameState, profile: ProjectProfile, rng: GameRng?): ProjectIncident? {
        if (rng == null) return null
        val chance = incidentChance(state, profile)
        if (chance <= 0 || rng.nextInt(100) >= chance) return null
        return ProjectIncident.pick(rng, profile, chance)
    }

    private fun projectNews(
        state: GameState,
        profile: ProjectProfile,
        level: NewsLevel,
        title: String,
        detail: String
    ): WorldNews = WorldNews(
        id = "proj_${profile.id}_${state.world.month}_${title}",
        year = state.year,
        month = state.world.month,
        category = NewsCategory.PROJECT,
        title = title,
        detail = detail,
        level = level,
        regionId = state.regionId
    )

    /** 项目回报转成通用治理效果。 */
    private fun ProjectReward.toEffect(boost: Int): GovernanceEffect = GovernanceEffect(
        indicators = indicators.mapValues { it.value + boost },
        merit = merit,
        finance = financeGain,
        risk = risk
    )

    private fun applyEffect(gov: GovernanceState, effect: GovernanceEffect): GovernanceState {
        var result = gov
        effect.indicators.forEach { (indicator, delta) -> result = result.withIndicator(indicator, delta) }
        effect.merit.forEach { (kind, delta) -> result = result.withMerit(kind, delta) }
        if (effect.finance != 0) result = result.withFinance(effect.finance)
        if (effect.savings != 0) result = result.copy(savings = (result.savings + effect.savings).coerceAtLeast(0))
        return result
    }

    /** 项目进度文本。 */
    fun progressText(project: ActiveProject): String {
        val profile = ProjectCatalog.find(project.id) ?: return project.id
        return when (project.status) {
            ProjectStatus.OPERATING -> "运营 ${project.operationLeft}/${profile.operationQuarters}"
            ProjectStatus.STALLED -> "停工 ${project.progress}/${profile.quarters}"
            ProjectStatus.COMPLETED -> "已完成"
            else -> "${project.progress}/${profile.quarters}"
        }
    }

    fun labelOf(projectId: String): String = ProjectCatalog.find(projectId)?.label ?: projectId
}