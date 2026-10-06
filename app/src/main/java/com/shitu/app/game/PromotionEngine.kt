package com.shitu.app.game

import com.shitu.app.model.AttrDelta
import com.shitu.app.model.PositionLevel
import com.shitu.app.model.PromotionPosition
import com.shitu.app.model.PositionCatalog
import com.shitu.app.model.UnitType
import com.shitu.app.model.isTopPosition
import com.shitu.app.model.positionAt
import com.shitu.app.model.nextPromotionIndex

// 晋升判定结果。
data class PromotionResult(
    val applicable: Boolean,
    val granted: Boolean,
    val text: String
)

/** 晋升界面里的一项条件。 */
data class PromotionRequirement(
    val label: String,
    val current: Int,
    val required: Int,
    val met: Boolean,
    /** 是否越高越好；廉政风险这类指标相反。 */
    val higherIsBetter: Boolean = true,
    val display: String = "$current / $required"
)

/** 「我的 - 当前职位」卡的完整数据。 */
data class PositionStatus(
    val currentIndex: Int,
    val currentName: String,
    val currentTitle: String,
    val powerLevel: Int,
    val scopeLabel: String,
    val yearsInPosition: Int,
    val monthsInPosition: Int,
    val top: Boolean,
    val nextName: String?,
    val nextTitle: String?,
    val progressPercent: Int,
    val ready: Boolean,
    val requirements: List<PromotionRequirement>,
    val blockers: List<String>
) {
    /** 全部硬条件满足即可晋升，与 [ready] 同义。满足就一定能晋升，不看空位。 */
    val eligible: Boolean get() = ready
}

// 晋升需要综合判断：任职年限、年度表现、实绩、能力、政治素养、领导信任、
// 群众基础、廉政风险、威望、财富与履历。
// 判定采用「条件制」：硬条件全部达标即通过，不存在与他人比较、排名或争夺；
// 关系与岗位机会只影响过程的说明文字，不改变结果。
object PromotionEngine {

    fun evaluate(
        state: GameState,
        reviewScore: Int,
        rng: GameRng
    ): PromotionResult {
        val current = state.player.position
        if (state.postCareer) {
            return PromotionResult(
                applicable = false,
                granted = false,
                text = "已退出正常仕途，不再参与晋升。"
            )
        }
        if (isTopPosition(current.index)) {
            return PromotionResult(
                applicable = false,
                granted = false,
                text = "已是当前梯队的最高职级。"
            )
        }
        if (RetirementEngine.isDue(state)) {
            // 65 岁人生节点优先级最高：未做出「退休 / 退居二线」选择前不参与晋升。
            return PromotionResult(
                applicable = false,
                granted = false,
                text = "已到退休年龄，请先处理人生节点。"
            )
        }
        val target = positionAt(nextPromotionIndex(current.index))
        val blockers = blockersOf(state, reviewScore)

        if (blockers.isNotEmpty()) {
            // 只说清楚还差哪些具体条件，不写任何解释性长句。
            val unmet = requirementList(state, reviewScore)
                .filterNot { it.met }
                .joinToString("；") { "${it.label} ${it.display}" }
            return PromotionResult(
                applicable = true,
                granted = false,
                text = unmet
            )
        }

        // 硬条件全部达标即获得晋升资格：立即进入晋升流程，
        // 不存在空位、名额、概率或竞争者等任何额外门槛。
        // 关系与派系只影响过程的说明文字，不改变结果。
        val support = PoliticsEngine.relationshipSupport(state)
        val helpText = if (support > 0) "会上有人替你说了一句公道话。" else ""
        return PromotionResult(
            applicable = true,
            granted = true,
            text = "经研究与考察，你已具备${target.name}任职资格。$helpText"
        )
    }

    /**
     * 是否已经具备晋升资格：下一职级存在，且全部硬条件满足。
     * 满足即可立即晋升，不再检查任何「空位 / 名额 / 概率」。
     */
    fun isEligible(state: GameState, reviewScore: Int = reviewScoreOf(state)): Boolean =
        !state.pendingPromotion && canApply(state, reviewScore)

    /**
     * 是否已经满足晋升的全部硬条件。
     *
     * 与 [isEligible] 的区别：不考虑「年度考核已通过、等待玩家选择具体职务」
     * 的挂起状态。任命逻辑用它，界面用它来判断能不能点晋升。
     */
    fun canApply(state: GameState, reviewScore: Int = reviewScoreOf(state)): Boolean =
        state.player.inActiveCareer &&
            !RetirementEngine.isDue(state) &&
            !isTopPosition(state.player.positionIndex) &&
            blockersOf(state, reviewScore).isEmpty()

    /**
     * 可选的下一职级具体职位。
     *
     * 只要有合法职位就允许晋升；若全部具体职位都不满足额外门槛，
     * 或职位数据为空，则返回一个默认合法职位兜底，绝不出现「无职位可选」。
     */
    fun eligibleOptions(
        state: GameState,
        reviewScore: Int = reviewScoreOf(state)
    ): List<PromotionPosition> {
        if (isTopPosition(state.player.positionIndex)) return emptyList()
        val rank = nextPromotionIndex(state.player.positionIndex)
        val merit = state.governance.merit.overall
        val approval = state.governance.jurisdiction.approval
        val trust = state.player.attributes.leaderTrust
        // 第十八阶段：具体职务由 PositionRepository 按
        // 「城市行政层级 + 单位 + 履历条线 + 城市属性」生成，
        // 同一职级至少 3 个、最多 6 个，且前三项不设门槛。
        // 城市主官职务例如直辖市 rank 11 -> 市委书记，副省级城市 rank 9 -> 市委书记 / 市长。
        // 第十九阶段：把当前具体职务与稳定种子传入职位池，
        // 让「当前职位 → 下一次职位池」形成路线倾向，但仍不设任何晋升门槛。
        val from = state.currentAppointment
        val seed = state.absoluteQuarter.toLong() * 31L + state.player.positionIndex
        val pool = PositionCatalog.promotionOptions(
            rank,
            state.currentCityId,
            state.player.unit,
            from = from,
            seed = seed
        )
        val matched = pool.filter { it.matches(merit, approval, trust) }.toMutableList()
        // 保证至少 3 个可选职务：不足时用不设门槛的职位补齐，绝不出现「无职位可选」。
        if (matched.size < com.shitu.app.model.PositionRepository.MIN_OPTIONS) {
            pool.filter { it.matches(0, 0, 0) }.forEach { candidate ->
                if (matched.size < com.shitu.app.model.PositionRepository.MIN_OPTIONS &&
                    matched.none { it.id == candidate.id }
                ) {
                    matched.add(candidate)
                }
            }
        }
        return matched.ifEmpty { listOf(defaultPromotionPosition(state)) }
    }

    /** 默认合法职位：任何情况下都可用，保证满足条件的玩家一定能晋升。 */
    fun defaultPromotionPosition(state: GameState): PromotionPosition {
        val rank = nextPromotionIndex(state.player.positionIndex)
        val node = PositionCatalog.defaultNode(rank, state.currentCityId, state.player.unit)
        return PromotionPosition(
            id = "default_$rank",
            rankIndex = rank,
            title = node.positionName,
            description = "由组织按程序安排的具体职位，条件达标即可任职。",
            scope = node.scope,
            cityId = node.cityId
        )
    }
    private fun requiredReviewScore(target: PositionLevel): Int = 58 + target.tier * 3

    /**
     * 只列出当前阻碍晋升的条件，不消耗随机数、不改变状态。
     * 首页与考核页用它告诉玩家「为什么还没晋升」，避免玩家觉得全靠运气。
     *
     * 不变式：[blockersOf] 与 [requirementList] 必须一一对应，
     * 也就是所有当前硬性数值条件都已列出、不存在任何隐藏门槛。
     * 只要 [requirementList] 中的条件全部达标（即 blockers 为空），[evaluate] 必然晋升。
     */
    fun blockersOf(state: GameState, reviewScore: Int): List<String> {
        val current = state.player.position
        if (isTopPosition(current.index)) return emptyList()
        val target = positionAt(nextPromotionIndex(current.index))
        val attrs = state.player.attributes
        val player = state.player
        val blockers = mutableListOf<String>()
        if (player.yearsInPosition < target.minYearsInPosition) {
            blockers.add("任职年限不足（需 ${target.minYearsInPosition} 年，现 ${player.yearsInPosition} 年）")
        }
        if (attrs.work < target.requiredWork) blockers.add("工作实绩未达线")
        if (attrs.ability < target.requiredAbility) blockers.add("工作能力未达线")
        if (attrs.politics < target.requiredPolitics) blockers.add("政治素养未达线")
        if (attrs.leaderTrust < target.requiredLeaderTrust) blockers.add("领导信任未达线")
        if (attrs.masses < target.requiredMasses) blockers.add("群众基础未达线")
        if (attrs.integrityRisk > target.maxIntegrityRisk) blockers.add("廉政风险偏高")
        if (reviewScore < requiredReviewScore(target)) {
            blockers.add("年度表现未达到 ${requiredReviewScore(target)} 分")
        }
        if (state.relationship.risk >= RISK_BLOCK_THRESHOLD) {
            blockers.add("官场风险偏高，这个节点上不宜动议")
        }
        val merit = state.governance.merit.overall
        if (merit < requiredMerit(target)) {
            blockers.add("政绩不足（需 ${requiredMerit(target)} 分，现 $merit 分）")
        }
        val approval = state.governance.jurisdiction.approval
        if (approval < requiredApproval(target)) {
            blockers.add("民望偏低（需 ${requiredApproval(target)}，现 $approval）")
        }
        if (target.requiredReputation > 0) {
            val reputation = PoliticsEngine.reputation(state)
            if (reputation < target.requiredReputation) {
                blockers.add("威望不足（需 ${target.requiredReputation}，现 $reputation）")
            }
        }
        if (target.requiredWealth > 0 && attrs.wealth < target.requiredWealth) {
            blockers.add("个人积累不足（需 ${target.requiredWealth}，现 ${attrs.wealth}）")
        }
        if (target.requiredCompletedEvents > 0 && state.log.size < target.requiredCompletedEvents) {
            blockers.add("履历还不够厚（需完成 ${target.requiredCompletedEvents} 件事，现 ${state.log.size} 件）")
        }
        return blockers
    }

    /**
     * 综合政绩门槛。
     *
     * 基层起步只要 [MERIT_BASE]（5）分即可，随后平缓上升；
     * 厅级以上才明显加码，避免玩家长期卡在同一个职位。
     * 这是真正参与晋升判定的数值，界面与判定共用同一个函数。
     */
    fun requiredMerit(target: PositionLevel): Int = when (target.tier) {
        0, 1 -> MERIT_BASE
        2 -> 7
        3 -> 9
        4 -> 12
        5 -> 15
        6 -> 18
        7 -> 22
        8 -> 26
        9 -> 31
        10 -> 36
        11 -> 41
        else -> 47
    }

    /** 民望门槛：太低说明群众意见大，不宜提拔。 */
    fun requiredApproval(target: PositionLevel): Int = 38 + target.tier * 2

    /** 年度表现门槛。 */
    fun requiredReview(target: PositionLevel): Int = requiredReviewScore(target)

    /** 用最近一次考核估算当前表现；没有考核记录时给一个中性值。 */
    fun reviewScoreOf(state: GameState): Int = state.reviews.lastOrNull()?.score ?: 60

    /**
     * 晋升进度百分比：把每一项硬条件的完成度取平均。
     * 只用于「我的」页展示，不影响判定逻辑。
     */
    fun promotionProgress(state: GameState, reviewScore: Int = reviewScoreOf(state)): Int {
        val current = state.player.position
        if (isTopPosition(current.index)) return 100
        // 年度考核已经通过、等待选择具体职务时视为已就绪。
        if (state.pendingPromotion) return 100
        val attrs = state.player.attributes
        val target = positionAt(nextPromotionIndex(current.index))
        val ratios = mutableListOf<Double>()
        fun ratio(value: Int, required: Int) {
            if (required <= 0) return
            ratios.add((value.toDouble() / required).coerceIn(0.0, 1.0))
        }
        ratio(state.player.yearsInPosition, target.minYearsInPosition)
        ratio(attrs.work, target.requiredWork)
        ratio(attrs.ability, target.requiredAbility)
        ratio(attrs.politics, target.requiredPolitics)
        ratio(attrs.leaderTrust, target.requiredLeaderTrust)
        ratio(attrs.masses, target.requiredMasses)
        ratio(state.governance.merit.overall, requiredMerit(target))
        ratio(state.governance.jurisdiction.approval, requiredApproval(target))
        ratio(reviewScore, requiredReviewScore(target))
        ratio(PoliticsEngine.reputation(state), target.requiredReputation)
        ratio(attrs.wealth, target.requiredWealth)
        ratio(state.log.size, target.requiredCompletedEvents)
        ratios.add(
            ((target.maxIntegrityRisk - attrs.integrityRisk).toDouble() /
                (target.maxIntegrityRisk + 1)).coerceIn(0.0, 1.0)
        )
        if (ratios.isEmpty()) return 0
        return (ratios.average() * 100).toInt().coerceIn(0, 99)
    }

    /** 晋升条件逐项对比，供「我的 - 当前职位」卡展示。 */
    fun requirementList(
        state: GameState,
        reviewScore: Int = reviewScoreOf(state)
    ): List<PromotionRequirement> {
        val current = state.player.position
        if (isTopPosition(current.index)) return emptyList()
        val target = positionAt(nextPromotionIndex(current.index))
        val attrs = state.player.attributes
        val merit = state.governance.merit.overall
        val approval = state.governance.jurisdiction.approval
        val reputation = PoliticsEngine.reputation(state)
        val list = mutableListOf<PromotionRequirement>()
        list.add(
            PromotionRequirement(
                "任职年限", state.player.yearsInPosition, target.minYearsInPosition,
                state.player.yearsInPosition >= target.minYearsInPosition,
                display = "${state.player.yearsInPosition} / ${target.minYearsInPosition} 年"
            )
        )
        list.add(PromotionRequirement("工作实绩", attrs.work, target.requiredWork, attrs.work >= target.requiredWork))
        list.add(PromotionRequirement("工作能力", attrs.ability, target.requiredAbility, attrs.ability >= target.requiredAbility))
        list.add(PromotionRequirement("政治素养", attrs.politics, target.requiredPolitics, attrs.politics >= target.requiredPolitics))
        list.add(PromotionRequirement("领导信任", attrs.leaderTrust, target.requiredLeaderTrust, attrs.leaderTrust >= target.requiredLeaderTrust))
        list.add(PromotionRequirement("群众基础", attrs.masses, target.requiredMasses, attrs.masses >= target.requiredMasses))
        list.add(PromotionRequirement("综合政绩", merit, requiredMerit(target), merit >= requiredMerit(target)))
        list.add(PromotionRequirement("民望", approval, requiredApproval(target), approval >= requiredApproval(target)))
        list.add(
            PromotionRequirement(
                "年度表现", reviewScore, requiredReviewScore(target),
                reviewScore >= requiredReviewScore(target)
            )
        )
        if (target.requiredReputation > 0) {
            list.add(
                PromotionRequirement(
                    "威望", reputation, target.requiredReputation,
                    reputation >= target.requiredReputation
                )
            )
        }
        if (target.requiredWealth > 0) {
            list.add(PromotionRequirement("个人积累", attrs.wealth, target.requiredWealth, attrs.wealth >= target.requiredWealth))
        }
        if (target.requiredCompletedEvents > 0) {
            list.add(
                PromotionRequirement(
                    "履历事件", state.log.size, target.requiredCompletedEvents,
                    state.log.size >= target.requiredCompletedEvents,
                    display = "${state.log.size} / ${target.requiredCompletedEvents} 件"
                )
            )
        }
        list.add(
            PromotionRequirement(
                "廉政风险", attrs.integrityRisk, target.maxIntegrityRisk,
                attrs.integrityRisk <= target.maxIntegrityRisk,
                higherIsBetter = false,
                display = "${attrs.integrityRisk} / 上限 ${target.maxIntegrityRisk}"
            )
        )
        list.add(
            PromotionRequirement(
                "官场风险", state.relationship.risk, RISK_BLOCK_THRESHOLD,
                state.relationship.risk < RISK_BLOCK_THRESHOLD,
                higherIsBetter = false,
                display = "${state.relationship.risk} / 上限 ${RISK_BLOCK_THRESHOLD}"
            )
        )
        return list
    }

    /** 组装「我的 - 当前职位」卡所需的全部数据。 */
    fun statusOf(state: GameState, reviewScore: Int = reviewScoreOf(state)): PositionStatus {
        val current = state.player.position
        val top = isTopPosition(current.index)
        val target = if (top) null else positionAt(nextPromotionIndex(current.index))
        val blockers = blockersOf(state, reviewScore)
        return PositionStatus(
            currentIndex = current.index,
            currentName = current.name,
            currentTitle = state.player.positionTitle,
            powerLevel = current.powerLevel,
            scopeLabel = current.scope.label,
            yearsInPosition = state.player.yearsInPosition,
            monthsInPosition = monthsInPosition(state),
            top = top,
            nextName = target?.name,
            nextTitle = target?.let {
                com.shitu.app.model.positionDisplay(
                    it.name,
                    PositionCatalog.defaultNode(it.index, state.currentCityId, state.player.unit).positionName
                )
            },
            progressPercent = promotionProgress(state, reviewScore),
            ready = !state.pendingPromotion &&
                blockers.isEmpty() &&
                state.player.inActiveCareer &&
                !RetirementEngine.isDue(state),
            requirements = requirementList(state, reviewScore),
            blockers = blockers
        )
    }

    /** 任职月数，按绝对季度推算，旧存档回退到任职年限。 */
    private fun monthsInPosition(state: GameState): Int {
        val start = state.player.positionStartAbs
        val months = if (start > 0) {
            ((state.absoluteQuarter - start).coerceAtLeast(0)) * 3
        } else {
            state.player.yearsInPosition * 12
        }
        return months.coerceAtLeast(0)
    }

    // 晋升带来的属性变化。
    fun promotionEffects(): AttrDelta = AttrDelta(
        reputation = 6,
        leaderTrust = 4,
        politics = 3,
        wealth = 4,
        energy = -4
    )

    // 晋升到关键台阶时可能调整单位。
    fun nextUnit(state: GameState): UnitType? {
        val reached = nextPromotionIndex(state.player.positionIndex)
        if (reached != 5 && reached != 7) return null
        val current = state.player.unit
        // 选定了发展方向，就按方向安排平台。
        state.route?.let { route -> return RouteEngine.preferredUnit(route, current) }
        val hidden = state.player.hidden
        return when {
            hidden.mentor >= 55 && current != UnitType.CITY -> UnitType.CITY
            hidden.opportunity >= 50 && current != UnitType.GENERAL -> UnitType.GENERAL
            reached >= 7 && current == UnitType.TOWN -> UnitType.COUNTY
            else -> null
        }
    }

    /** 第一次晋升（科员 → 副科级）的综合政绩门槛。 */
    const val MERIT_BASE = 5

    const val RISK_BLOCK_THRESHOLD = 70

}