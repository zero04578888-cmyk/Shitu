package com.shitu.app.game

import com.shitu.app.model.AnnualReview
import com.shitu.app.model.Attr
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.Ending
import com.shitu.app.model.HiddenKey
import com.shitu.app.model.HistoryEntry
import com.shitu.app.model.LogEntry
import com.shitu.app.model.LogKind
import com.shitu.app.model.OutcomeRecord
import com.shitu.app.model.PromotionPosition
import com.shitu.app.model.GovernanceState
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.WorldState

// 游戏主流程：创建人物、结算选择、推进时间、年度考核、晋升与结局。
object GameEngine {

    // 第二十阶段：强制退休改由「65 岁人生节点」决定（副国级及以上不受限）。
    // 这里保留一个总年限上限，主要用于给副国级以上的长局收口，保证游戏一定会结束。
    const val MAX_SERVICE_YEARS = 45

    fun newGame(
        name: String,
        gender: String,
        education: String,
        start: String,
        seed: Long = GameRng.DEFAULT_SEED,
        traits: List<String> = emptyList(),
        priorLives: Int = 0,
        regionId: String? = null
    ): GameState {
        val player = CharacterFactory.create(name, gender, education, start, traits)
        val rng = GameRng(seed)
        // 多周目变化：不指定地区时，按种子随机落到一个虚构地区。
        // 指定且存在的城市直接使用；未指定时按种子随机；旧存档里的未知城市回退到默认城市。
        val region = when {
            regionId.isNullOrBlank() -> RegionCatalog.randomSeeded(seed)
            else -> RegionCatalog.find(regionId) ?: RegionCatalog.default
        }
        val level = JurisdictionFactory.levelFor(player.unit, player.positionIndex)
        val jurisdiction = RegionEngine.jurisdictionFor(region, player.unit, player.positionIndex)
        val world = WorldState(
            economyClimate = (region.economy + rng.nextInt(21) - 10).coerceIn(20, 90),
            environmentClimate = (region.environment + rng.nextInt(21) - 10).coerceIn(20, 90)
        )
        var base = GameState(
            player = player,
            rngState = seed,
            relationship = PoliticalEngine.initialRelationship(player.unit),
            governance = GovernanceState(
                jurisdiction = jurisdiction,
                finance = RegionEngine.initialFinance(region, level),
                salaryPerQuarter = GovernanceEngine.salaryOf(player.positionIndex)
            ),
            politics = PoliticsEngine.initial(),
            career = CareerEngine.initial(priorLives),
            regionId = region.id,
            world = world,
            careerCities = listOf(
                com.shitu.app.model.CityStint(
                    cityId = region.id,
                    cityName = region.name,
                    province = region.province,
                    positionLabel = player.positionName,
                    startAbsQuarter = 0
                )
            )
        )
        base = CareerEngine.record(
            base,
            CareerRecordKind.START,
            "参加工作",
            "进入${region.province}${region.name}（${region.cityTypeLabel}），在${player.unit.label}任${player.positionName}。"
        )
        base = CareerEngine.syncPosition(base)
        // 第十三阶段：补齐世界模拟所需的地区、NPC、经济与时代状态。
        base = WorldSimulationEngine.ensureWorld(base)
        val event = EventEngine.pickNext(base, rng) ?: EventLibrary.find("overtime_task")
        return base.copy(
            currentEventId = event?.id.orEmpty(),
            rngState = rng.state,
            phase = GamePhase.EVENT
        )
    }

    fun currentEvent(state: GameState) = EventLibrary.find(state.currentEventId)

    // 结算玩家的选择：属性、隐藏变量、标记、事件链、日志。
    fun resolveChoice(state: GameState, choiceIndex: Int): GameState {
        if (state.phase != GamePhase.EVENT) return state
        // 退休节点未决时，任何事件选择都不能推进，避免用事件拖延退休。
        if (RetirementEngine.isDue(state)) return state.copy(phase = GamePhase.REVIEW)
        val event = currentEvent(state) ?: return state
        val choice = event.choices.getOrNull(choiceIndex) ?: return state

        var attributes = state.player.attributes.apply(choice.effects)
        var hidden = state.player.hidden.apply(choice.hiddenEffects)
        val deltaList = choice.effects.entries().toMutableList()
        val hiddenList = choice.hiddenEffects.entries().toMutableList()
        val outcomeText = StringBuilder(choice.outcome)

        // 事件链的条件加成：前期的选择会改变后期的结果。
        choice.conditionalBonus.forEach { bonus ->
            if (state.flags.contains(bonus.requiredFlag)) {
                attributes = attributes.apply(bonus.effects)
                hidden = hidden.apply(bonus.hiddenEffects)
                deltaList += bonus.effects.entries()
                hiddenList += bonus.hiddenEffects.entries()
                if (bonus.extraOutcome.isNotEmpty()) {
                    outcomeText.append("\n\n").append(bonus.extraOutcome)
                }
            }
        }

        // 重大抉择会留下长期标记，供后续事件用条件加成引用，形成长期后果。
        val flags = buildSet {
            addAll(state.flags)
            choice.setsFlag?.let { add(it) }
            if (event.major) add("major_${event.id}")
            if (choice.politics.resolveCrisis) add("resolved_crisis")
        }
        val cooldown = state.eventCooldown.toMutableMap()
        if (event.repeatable) {
            cooldown[event.id] = state.absoluteQuarter + event.cooldownQuarters
        }
        val pending = scheduleChains(state, event, choice.startsChain)

        val record = OutcomeRecord(
            eventId = event.id,
            eventTitle = event.title,
            choiceText = choice.text,
            outcome = outcomeText.toString(),
            deltas = deltaList.map { it.first.label to it.second },
            hiddenDeltas = hiddenList.map { it.first.label to it.second },
            relationDeltas = relationDeltasOf(choice),
            governanceDeltas = governanceDeltasOf(choice)
        )

        val logEntry = LogEntry(
            year = state.year,
            quarter = state.quarter,
            title = event.title,
            choiceText = choice.text,
            outcome = outcomeText.toString(),
            deltas = record.deltas,
            kind = LogKind.EVENT
        )

        // 发展方向一旦选定便长期生效，单独留一条记录。
        val routeLog = choice.setsRoute?.let { picked ->
            LogEntry(
                year = state.year,
                quarter = state.quarter,
                title = "发展方向",
                choiceText = picked.label,
                outcome = "组织上认可了你的选择。此后会按${picked.label}方向考察和使用。" +
                    picked.description,
                deltas = emptyList(),
                kind = LogKind.ROUTE
            )
        }

        var politics = PoliticsEngine.applyChoice(state, choice.politics)
        if (event.major) politics = politics.withMajorChoice(event.id)
        choice.politics.history?.let { note ->
            politics = politics.withHistory(
                HistoryEntry(
                    year = state.year,
                    quarter = state.quarter,
                    title = event.title,
                    choice = choice.text,
                    consequence = note
                )
            )
        }

        // 第十阶段：经验成长、履历与人物记忆。
        var careerState = CareerEngine.gainExperience(
            state,
            CareerEngine.experienceForChoice(choice, event.major)
        )
        if (event.major) {
            careerState = CareerEngine.record(careerState, CareerRecordKind.CHOICE, event.title, choice.text)
        } else if (com.shitu.app.model.EventTag.PROJECT in event.tags && !event.repeatable) {
            careerState = CareerEngine.record(careerState, CareerRecordKind.PROJECT, event.title, choice.outcome)
        } else if (com.shitu.app.model.EventTag.FAMILY in event.tags && !event.repeatable) {
            careerState = CareerEngine.record(careerState, CareerRecordKind.FAMILY, event.title, choice.outcome)
        }
        careerState = CareerEngine.rememberFromChoice(careerState, choice, event.title)
        careerState = CareerEngine.syncPosition(careerState)

        // 第十三阶段：重大抉择写入决策记忆，未来事件与长期因果链会读取它。
        var withMemory = careerState
        if (event.major) {
            withMemory = WorldSimulationEngine.recordDecision(
                withMemory,
                id = "dec_${event.id}",
                title = event.title,
                choice = choice.text,
                consequence = outcomeText.toString(),
                tags = WorldSimulationEngine.policyTagsOf(event.tags)
            )
        }
        val resolved = state.copy(
            player = state.player.copy(attributes = attributes, hidden = hidden),
            world = withMemory.world,
            outcome = record,
            log = state.log + logEntry + listOfNotNull(routeLog),
            flags = flags,
            route = choice.setsRoute ?: state.route,
            relationship = appliedRelationship(state, choice),
            politics = politics,
            career = careerState.career,
            governance = GovernanceEngine.applyToJurisdiction(state.governance, choice.governance),
            pendingChains = pending,
            triggeredEvents = state.triggeredEvents + event.id,
            eventCooldown = cooldown,
            totalWorkGained = state.totalWorkGained + choice.effects.work.coerceAtLeast(0),
            phase = GamePhase.OUTCOME
        )
        return CareerEngine.checkAchievements(resolved)
    }

    // 把一次选择带来的官场关系变化整理成界面可读的列表。
    private fun relationDeltasOf(choice: com.shitu.app.model.Choice): List<Pair<String, Int>> {
        val political = choice.political
        if (political.isEmpty) return emptyList()
        val result = mutableListOf<Pair<String, Int>>()
        political.relations.forEach { (npcId, delta) ->
            result += (NpcCatalog.find(npcId)?.name ?: npcId) to delta
        }
        political.trust.forEach { (npcId, delta) ->
            result += "${NpcCatalog.find(npcId)?.name ?: npcId}·信任" to delta
        }
        political.factions.forEach { (faction, delta) ->
            result += faction.label to delta
        }
        if (political.risk != 0) result += "官场风险" to political.risk
        return result
    }

    private fun appliedRelationship(
        state: GameState,
        choice: com.shitu.app.model.Choice
    ): com.shitu.app.model.RelationshipState {
        val applied = RelationshipEngine.applyEffect(state.relationship, choice.political)
        val riskFromGovernance = choice.governance.risk
        return if (riskFromGovernance == 0) {
            applied
        } else {
            applied.withRisk(applied.risk + riskFromGovernance)
        }
    }

    // 辖区与政绩变化，用于结算面板展示。
    private fun governanceDeltasOf(choice: com.shitu.app.model.Choice): List<Pair<String, Int>> {
        val effect = choice.governance
        if (effect.isEmpty) return emptyList()
        val result = mutableListOf<Pair<String, Int>>()
        effect.indicators.forEach { (indicator, delta) -> result += indicator.label to delta }
        effect.merit.forEach { (kind, delta) -> result += kind.label to delta }
        if (effect.finance != 0) result += "财政" to effect.finance
        if (effect.savings != 0) result += "个人存款" to effect.savings
        effect.attributes.forEach { (attr, delta) -> result += attr.label to delta }
        return result
    }

    // 事件链调度：把下一个节点排进日程。
    private fun scheduleChains(
        state: GameState,
        event: com.shitu.app.model.GameEvent,
        startsChain: String?
    ): List<PendingChain> {
        var pending = state.pendingChains
        if (event.chainId != null) {
            pending = pending.filterNot { it.chainId == event.chainId && it.nextStep == event.chainStep }
        }
        if (startsChain != null) {
            val next = EventLibrary.nextChainStep(startsChain, event.chainStep)
            if (next != null) {
                val due = state.absoluteQuarter + next.chainAfterQuarters
                pending = pending.filterNot { it.chainId == startsChain && it.nextStep == next.chainStep } +
                    PendingChain(chainId = startsChain, nextStep = next.chainStep, dueQuarter = due)
            }
        }
        return pending
    }

    // 选择之后推进一个季度；第四季度结束后进行年度考核。
    fun advance(state: GameState): GameState {
        if (state.phase != GamePhase.OUTCOME) return state
        // 退休节点未决时先处理人生节点，不推进季度。
        if (RetirementEngine.isDue(state)) return state.copy(phase = GamePhase.REVIEW)
        val rng = GameRng(state.rngState)
        val settled = ConsequenceEngine.settle(drift(state))
        val riskSettled = PoliticalEngine.settleRisk(settled.state)
        val (afterRisk, riskNotes) = PoliticalEngine.settleConsequences(riskSettled)
        val (afterGovernance, governanceNotes) = GovernanceEngine.settleQuarter(afterRisk)
        // 第十二阶段：先兑现本季度工作安排，再推进在建项目。
        val (afterPlan, planNotes) = WorkPlanEngine.resolve(afterGovernance)
        // 第十三阶段：世界先运转（经济、地区、人口、NPC、世界事件与长期因果）。
        val afterWorld = WorldSimulationEngine.advanceQuarter(afterPlan, rng)
        val (afterProjects, projectNotes) = ProjectEngine.advance(afterWorld, rng)
        val notes = settled.notes + riskNotes + governanceNotes + planNotes + projectNotes
        // 隐性状态造成实际影响时写一条提醒，避免玩家觉得数值变化来路不明。
        val drifted = if (notes.isEmpty()) {
            afterProjects
        } else {
            afterProjects.copy(
                log = afterProjects.log + LogEntry(
                    year = afterGovernance.year,
                    quarter = afterGovernance.quarter,
                    title = "隐性状态",
                    choiceText = "季度结算",
                    outcome = notes.joinToString("\n"),
                    deltas = emptyList(),
                    kind = LogKind.WARNING
                )
            )
        }
        // 第十阶段：每季度重置个人行动次数。
        val withCareer = CareerEngine.resetQuarter(drifted)
        val result = if (withCareer.quarter >= 4) {
            conductAnnualReview(withCareer, rng)
        } else {
            selectNextEvent(withCareer.copy(quarter = withCareer.quarter + 1), rng)
        }
        return result.copy(rngState = rng.state)
    }

    // 年度报告看完之后，进入下一年度的第一个事件。
    fun proceedFromReview(state: GameState): GameState {
        if (state.phase != GamePhase.REVIEW) return state
        // 65 岁人生节点未决时，默认按「退休」处理，保证流程不会停在等待状态。
        // 界面会先弹出【人生节点】让玩家在「退休 / 退居二线」之间选择。
        if (RetirementEngine.isDue(state)) {
            return retirementChoice(state, RetirementChoice.RETIREMENT)
        }
        // 第十八阶段：年度考核已通过晋升但还没有选具体职务时，
        // 自动按默认合法职务完成任命，保证流程不会停住。
        val resolved = if (state.pendingPromotion) completePendingPromotion(state) else state
        val rng = GameRng(resolved.rngState)
        val result = selectNextEvent(resolved, rng)
        return result.copy(rngState = rng.state)
    }

    /**
     * 65 岁人生节点的选择。
     *
     * - 退休：记录退休信息并进入人生总结（结局页）。
     * - 退居二线：记录退休信息，允许继续有限的退休后内容，但不再参与任何晋升。
     *
     * 只有真正处于退休节点（65 岁且低于副国级）时才会生效，
     * 重复调用或未到年龄时原样返回，保证存档与流程稳定。
     */
    fun retirementChoice(state: GameState, choice: RetirementChoice): GameState =
        retirementChoice(state, choice.type)

    fun retirementChoice(state: GameState, type: String): GameState {
        val selected = RetirementChoice.byType(type) ?: return state
        if (!RetirementEngine.isDue(state)) return state

        val age = state.player.age
        val year = state.year
        val retired = selected == RetirementChoice.RETIREMENT
        val updated = state.copy(
            player = state.player.copy(
                retired = retired,
                retirementType = selected.type,
                retirementAge = age,
                retirementYear = year
            ),
            transferOffer = null,
            pendingPromotion = false,
            log = state.log + LogEntry(
                year = year,
                quarter = state.quarter,
                title = "人生节点 · ${selected.label}",
                choiceText = selected.label,
                outcome = if (retired) {
                    "你办理了退休手续，结束了在一线的仕途。"
                } else {
                    "你退居二线，不再参与职务晋升，但仍关注着手上的事。"
                },
                deltas = emptyList(),
                kind = LogKind.WARNING
            )
        )
        val recorded = CareerEngine.record(
            updated,
            CareerRecordKind.RETIRE,
            "退休节点 · ${selected.label}",
            if (retired) "第 $year 年，$age 岁办理退休。" else "第 $year 年，$age 岁退居二线。",
            year = year,
            quarter = state.quarter
        )
        return if (retired) {
            finish(recorded, EndingEngine.finalEnding(recorded))
        } else {
            // 退居二线：先让玩家看完这一年的年度报告，再由 proceedFromReview
            // 进入有限的退休后内容。此时已不属于「退休节点」，不会再次弹出。
            recorded.copy(outcome = null, phase = GamePhase.REVIEW)
        }
    }

    // 每个季度的自然变化：精力恢复、压力缓解、健康随精力浮动。
    private fun drift(state: GameState): GameState {
        val attrs = state.player.attributes
        val hidden = state.player.hidden
        val newAttrs = attrs
            .withValue(Attr.ENERGY, attrs.energy + 6)
            .withValue(Attr.HEALTH, if (attrs.energy >= 55) attrs.health + 1 else attrs.health - 2)
            .withValue(Attr.INTEGRITY_RISK, attrs.integrityRisk - 1)
        val newHidden = hidden
            .withValue(HiddenKey.STRESS, hidden.stress - 3)
            .withValue(HiddenKey.HEALTH_RISK, hidden.healthRisk - 1)
        return state.copy(player = state.player.copy(attributes = newAttrs, hidden = newHidden))
    }

    private fun selectNextEvent(state: GameState, rng: GameRng): GameState {
        if (state.isFinished) return state

        // 退休节点未决时，不再进入普通事件，直接回到人生节点。
        if (RetirementEngine.isDue(state)) return state.copy(phase = GamePhase.REVIEW)

        // 清理已经没有后续节点的事件链记录，避免存档里留下无效数据。
        val cleaned = state.copy(
            pendingChains = state.pendingChains.filter { chain ->
                EventLibrary.stepsOf(chain.chainId).any { it.chainStep == chain.nextStep }
            }
        )

        // 达到服务年限上限则进入退休结局。
        if (cleaned.player.serviceYears >= MAX_SERVICE_YEARS) {
            return finish(cleaned, EndingEngine.finalEnding(cleaned))
        }
        EndingEngine.terminalEnding(cleaned)?.let { ending ->
            return finish(cleaned, ending)
        }
        val event = EventEngine.pickNext(cleaned, rng)
        return cleaned.copy(
            currentEventId = event?.id ?: cleaned.currentEventId,
            outcome = null,
            phase = GamePhase.EVENT
        )
    }

    /**
     * 立即晋升：玩家满足当前职位的全部晋升条件时，直接完成晋升。
     *
     * 这里不再检查任何「空位 / 名额 / 下一轮机会 / 竞争者」，
     * 只要 PromotionEngine.isEligible 为真就必须成功。
     * [positionId] 为玩家在可选具体职位里选择的编号；为空或非法时取第一个合法职位。
     */
    fun promoteNow(state: GameState, positionId: String? = null): GameState {
        // 已退休 / 退居二线后不再参与任何晋升。
        if (state.postCareer) return state
        // 年度考核已经研究通过（pendingPromotion）时不再重复判定门槛，
        // 否则要求当前确实满足全部硬条件。
        if (!state.pendingPromotion && !PromotionEngine.canApply(state)) return state
        val options = PromotionEngine.eligibleOptions(state)
        val chosen = options.firstOrNull { it.id == positionId } ?: options.first()
        val from = state.displayPositionTitle
        val toIndex = com.shitu.app.model.nextPromotionIndex(state.player.positionIndex)
        val toRank = com.shitu.app.model.positionAt(toIndex).name
        val to = com.shitu.app.model.positionDisplay(toRank, chosen.title)

        var unit = state.player.unit
        var governance = state.governance
        val logs = mutableListOf<LogEntry>()

        PromotionEngine.nextUnit(state)?.let { newUnit ->
            unit = newUnit
            logs.add(
                LogEntry(
                    year = state.year,
                    quarter = state.quarter,
                    title = "岗位调整",
                    choiceText = "服从组织安排",
                    outcome = "组织决定把你调整到${newUnit.label}工作。",
                    deltas = emptyList(),
                    kind = LogKind.PROMOTION
                )
            )
        }
        val newLevel = JurisdictionFactory.levelFor(unit, toIndex)
        if (newLevel != governance.jurisdiction.level) {
            governance = governance.copy(
                jurisdiction = JurisdictionFactory.upgraded(governance.jurisdiction, newLevel)
            )
            logs.add(
                LogEntry(
                    year = state.year,
                    quarter = state.quarter,
                    title = "辖区调整",
                    choiceText = "组织安排",
                    outcome = "你被安排到${newLevel.defaultName}（${newLevel.label}）工作，管的盘子更大了。",
                    deltas = emptyList(),
                    kind = LogKind.PROMOTION
                )
            )
        }
        logs.add(
            LogEntry(
                year = state.year,
                quarter = state.quarter,
                title = "职级晋升",
                choiceText = "接受组织任命",
                outcome = "全部条件已满足，由${from}晋升为${to}。",
                deltas = PromotionEngine.promotionEffects().entries().map { it.first.label to it.second },
                kind = LogKind.PROMOTION
            )
        )

        val promoted = state.copy(
            player = state.player.copy(
                positionIndex = toIndex,
                yearsInPosition = 0,
                attributes = state.player.attributes.apply(PromotionEngine.promotionEffects()),
                unit = unit,
                appointmentTitle = chosen.title,
                appointmentId = chosen.id,
                appointmentCityId = chosen.cityId.ifBlank { state.currentCityId },
                positionStartAbs = state.absoluteQuarter
            ),
            governance = governance,
            promotionCount = state.promotionCount + 1,
            reachedTopYear = if (toIndex >= com.shitu.app.model.POSITION_LADDER.size - 1 &&
                state.reachedTopYear == null
            ) {
                state.year
            } else {
                state.reachedTopYear
            },
            pendingPromotion = false,
            log = state.log + logs
        )
        // 履历：晋升与调任都要留痕，重新进入游戏后仍能看到。
        var result = promoted.copy(
            career = promoted.career.copy(positionsHeld = promoted.career.positionsHeld + 1)
        )
        // 第十九阶段：记录走过的具体职务 id，用于仕途路线展示与履历价值估算。
        result = result.copy(
            career = result.career.copy(
                positionHistory = result.career.positionHistory + chosen.id
            )
        )
        result = CareerEngine.record(
            result,
            CareerRecordKind.PROMOTION,
            "晋升 ${result.displayPositionTitle}",
            "经研究与考察，进入 ${result.displayPositionTitle}。",
            fromPosition = from,
            toPosition = to
        )
        if (unit != state.player.unit) {
            result = CareerEngine.record(
                result,
                CareerRecordKind.TRANSFER,
                "调任 ${result.player.unit.label}",
                "根据工作需要，调整到${result.player.unit.label}工作。"
            )
        }
        return result
    }

    /**
     * 第十八阶段：完成「年度考核已通过、等待选择具体职务」的晋升。
     *
     * 玩家在【任职选择】里选定具体职务后调用；没有选择（例如自动推进）时
     * 使用默认合法职务，保证满足条件的玩家一定完成晋升、绝不会卡住。
     */
    fun completePendingPromotion(state: GameState, positionId: String? = null): GameState {
        if (!state.pendingPromotion) return state
        val promoted = promoteNow(state, positionId)
        // 无论走哪条分支，都必须清掉挂起标记，避免流程停在等待状态。
        return if (promoted.pendingPromotion) promoted.copy(pendingPromotion = false) else promoted
    }

    /**
     * 当前挂起晋升的可选具体职务；没有挂起时返回空列表。
     * 界面用它渲染【任职选择】窗口。
     */
    fun pendingOptions(state: GameState): List<PromotionPosition> =
        if (!state.pendingPromotion) emptyList() else PromotionEngine.eligibleOptions(state)
    // 年度考核 + 晋升判定。
    private fun conductAnnualReview(state: GameState, rng: GameRng): GameState {
        val review = AnnualReviewEngine.conduct(state)
        // 晋升只看玩家自己的硬条件：全部满足即通过，不存在空位、名额、
        // 概率或竞争者等任何额外门槛。
        val promotion = PromotionEngine.evaluate(state, review.score, rng)

        var attributes = state.player.attributes
        var hidden = state.player.hidden
        var warningCount = state.warningCount

        when (review.grade) {
            "优秀" -> {
                attributes = attributes
                    .withValue(Attr.LEADER_TRUST, attributes.leaderTrust + 5)
                    .withValue(Attr.REPUTATION, attributes.reputation + 4)
                    .withValue(Attr.WORK, attributes.work + 2)
                hidden = hidden.withValue(HiddenKey.MENTOR, hidden.mentor + 3)
            }
            "良好" -> {
                attributes = attributes
                    .withValue(Attr.LEADER_TRUST, attributes.leaderTrust + 3)
                    .withValue(Attr.REPUTATION, attributes.reputation + 2)
            }
            "称职" -> {
                attributes = attributes
                    .withValue(Attr.LEADER_TRUST, attributes.leaderTrust + 1)
                    .withValue(Attr.REPUTATION, attributes.reputation + 1)
            }
            "基本称职" -> {
                attributes = attributes
                    .withValue(Attr.LEADER_TRUST, attributes.leaderTrust - 3)
                    .withValue(Attr.REPUTATION, attributes.reputation - 2)
                hidden = hidden.withValue(HiddenKey.STRESS, hidden.stress + 4)
            }
            "不称职" -> {
                attributes = attributes
                    .withValue(Attr.LEADER_TRUST, attributes.leaderTrust - 8)
                    .withValue(Attr.REPUTATION, attributes.reputation - 6)
                    .withValue(Attr.WORK, attributes.work - 3)
                hidden = hidden.withValue(HiddenKey.STRESS, hidden.stress + 8)
                warningCount += 1
            }
        }

        // 廉政风险偏高时在年度考核中提出提醒。
        val riskWarning = attributes.integrityRisk >= 70
        if (riskWarning) {
            attributes = attributes
                .withValue(Attr.REPUTATION, attributes.reputation - 5)
                .withValue(Attr.LEADER_TRUST, attributes.leaderTrust - 4)
            warningCount += 1
        }

        var positionIndex = state.player.positionIndex
        var yearsInPosition = state.player.yearsInPosition + 1
        var promotionCount = state.promotionCount
        var reachedTopYear = state.reachedTopYear
        var unit = state.player.unit
        var governance = state.governance
        var demotionCount = state.demotionCount
        // 第十八阶段：年度考核通过后先挂起，等玩家选择具体职务。
        var pendingPromotion = false
        var appointmentTitle = state.player.appointmentTitle
        var appointmentId = state.player.appointmentId
        var appointmentCityId = state.player.appointmentCityId
        val extraLogs = mutableListOf<LogEntry>()

        // 第十五阶段：连续失败或重大风险可能触发降职，与晋升互斥。
        val demotion = if (promotion.granted) {
            null
        } else {
            DemotionEngine.reasonFor(state, review.grade, review.score, warningCount)
        }

        if (promotion.granted) {
            // 第十八阶段：年度考核通过后不直接任命，
            // 由玩家在【任职选择】里决定具体职务，再由
            // GameEngine.completePendingPromotion 正式任命。
            pendingPromotion = true
            extraLogs.add(
                LogEntry(
                    year = state.year,
                    quarter = 4,
                    title = "晋升资格",
                    choiceText = "接受组织考察",
                    outcome = promotion.text + " 请选择新的具体职务。",
                    deltas = emptyList(),
                    kind = LogKind.PROMOTION
                )
            )
        } else if (promotion.applicable) {
            extraLogs.add(
                LogEntry(
                    year = state.year,
                    quarter = 4,
                    title = "晋升考察",
                    choiceText = "接受组织考察",
                    outcome = promotion.text,
                    deltas = emptyList(),
                    kind = LogKind.WARNING
                )
            )
        }

        if (demotion != null) {
            positionIndex = com.shitu.app.model.previousPromotionIndex(positionIndex)
            yearsInPosition = 0
            demotionCount += 1
            appointmentTitle = ""
            appointmentId = ""
            attributes = attributes
                .withValue(Attr.REPUTATION, attributes.reputation - 8)
                .withValue(Attr.LEADER_TRUST, attributes.leaderTrust - 10)
                .withValue(Attr.WORK, attributes.work - 3)
            extraLogs.add(
                LogEntry(
                    year = state.year,
                    quarter = 4,
                    title = "职务调整",
                    choiceText = "接受组织处理",
                    outcome = demotion.text,
                    deltas = listOf("个人声望" to -8, "领导信任" to -10, "工作实绩" to -3),
                    kind = LogKind.WARNING
                )
            )
        }

        extraLogs.add(
            LogEntry(
                year = state.year,
                quarter = 4,
                title = "年度考核",
                choiceText = "第 ${state.reviewCount + 1} 次年度考核",
                outcome = review.summary,
                deltas = emptyList(),
                kind = LogKind.REVIEW
            )
        )

        if (riskWarning) {
            extraLogs.add(
                LogEntry(
                    year = state.year,
                    quarter = 4,
                    title = "廉政提醒",
                    choiceText = "接受提醒谈话",
                    outcome = "年度考核中，组织就廉政风险问题对你进行了提醒谈话。",
                    deltas = emptyList(),
                    kind = LogKind.WARNING
                )
            )
        }

        val finalReview = AnnualReview(
            year = review.year,
            score = review.score,
            grade = review.grade,
            summary = review.summary,
            items = review.items,
            promotionText = promotion.text,
            promotionGranted = promotion.granted
        )

        val serviceYears = state.player.serviceYears + 1

        // 第九阶段：政治危机与年度政治结算。
        val (tickedPolitics, politicsNotes) = PoliticsEngine.applyYearTick(state, rng)
        var politics = tickedPolitics
        if (politics.crisis == null) {
            PoliticsEngine.crisisAfterYear(state)?.let { crisisId ->
                CrisisCatalog.find(crisisId)?.let { crisis ->
                    politics = politics.copy(crisis = crisis.copy(year = state.year))
                }
            }
        }
        val politicsLogs = politicsNotes.map { note ->
            LogEntry(
                year = state.year,
                quarter = 4,
                title = "政治格局",
                choiceText = "年度结算",
                outcome = note,
                deltas = emptyList(),
                kind = LogKind.WARNING
            )
        }

        val newPositionStart = when {
            demotion != null -> state.absoluteQuarter
            state.player.positionStartAbs > 0 -> state.player.positionStartAbs
            else -> (state.absoluteQuarter - yearsInPosition * 4).coerceAtLeast(0)
        }

        var newState = state.copy(
            player = state.player.copy(
                attributes = attributes,
                hidden = hidden,
                positionIndex = positionIndex,
                yearsInPosition = yearsInPosition,
                serviceYears = serviceYears,
                unit = unit,
                appointmentTitle = appointmentTitle,
                appointmentId = appointmentId,
                appointmentCityId = appointmentCityId.ifBlank { state.currentCityId },
                positionStartAbs = newPositionStart,
                family = advanceFamily(state)
            ),
            year = state.year + 1,
            quarter = 1,
            reviewCount = state.reviewCount + 1,
            reviews = state.reviews + finalReview,
            log = state.log + extraLogs + politicsLogs,
            warningCount = warningCount,
            promotionCount = promotionCount,
            demotionCount = demotionCount,
            reachedTopYear = reachedTopYear,
            politics = politics,
            governance = GovernanceEngine.settleYear(governance),
            outcome = null,
            pendingPromotion = pendingPromotion,
            phase = GamePhase.REVIEW
        )

        // 第十阶段：年度经验、履历与成就。
        newState = CareerEngine.gainExperience(newState, 5 + review.score / 10)
        when {
            review.score >= 80 -> newState = CareerEngine.record(
                newState, CareerRecordKind.HONOR, "年度考核优秀",
                "第 ${state.year} 年考核 ${review.score} 分（${review.grade}）。",
                year = state.year, quarter = 4
            )
            review.score < 55 -> newState = CareerEngine.record(
                newState, CareerRecordKind.SETBACK, "年度考核受挫",
                "第 ${state.year} 年考核 ${review.score} 分（${review.grade}）。",
                year = state.year, quarter = 4
            )
        }
        // 第十八阶段：晋升的正式任命与履历记录，统一由
        // GameEngine.completePendingPromotion（玩家选择职务后）完成。
        // 见上方 pendingPromotion 分支。
        if (demotion != null) {
            newState = newState.copy(
                career = newState.career.copy(
                    positionsHeld = newState.career.positionsHeld + 1
                )
            )
            newState = CareerEngine.record(
                newState, CareerRecordKind.SETBACK,
                "职务调整 ${newState.displayPositionTitle}",
                "因工作失误，职务作出调整。",
                year = state.year, quarter = 4,
                fromPosition = state.displayPositionTitle,
                toPosition = newState.displayPositionTitle
            )
        } else {
            // 第十五阶段：职位本身的在岗影响，按年结算一次。
            newState = PositionEngine.applyTenure(newState, positionIndex)
        }
        newState = CareerEngine.syncPosition(newState)
        // 第十二阶段：调任机会。考核过得去、又不是刚提拔时，可能出现新的安排。
        val expired = newState.transferOffer?.takeIf { it.expiresQuarter <= newState.absoluteQuarter }
        if (expired != null) newState = newState.copy(transferOffer = null)
        if (newState.transferOffer == null && review.score >= 58 && rng.nextInt(100) < 35) {
            RegionEngine.offerFor(newState, rng)?.let { offer ->
                newState = newState.copy(
                    transferOffer = offer,
                    log = newState.log + LogEntry(
                        year = newState.year,
                        quarter = 1,
                        title = "调任机会",
                        choiceText = RegionCatalog.findOrDefault(offer.regionId).name,
                        outcome = "${offer.reason}。是否接受，由你决定。",
                        deltas = emptyList(),
                        kind = LogKind.PROMOTION
                    )
                )
            }
        }
        // 世界大环境每年小幅变化，影响后续事件的倾向。
        newState = newState.copy(
            world = newState.world.copy(
                economyClimate = (newState.world.economyClimate + rng.nextInt(7) - 3).coerceIn(15, 95),
                environmentClimate = (newState.world.environmentClimate + rng.nextInt(7) - 3).coerceIn(15, 95)
            )
        )
        // 第十三阶段：年度世界结算（地区、人口、NPC、派系、世界事件与时代）。
        newState = WorldSimulationEngine.advanceYear(newState, rng)
        newState = CareerEngine.checkAchievements(newState)

        // 65 岁人生节点优先于普通晋升与后续事件：低于副国级的玩家必须做出选择。
        // 达到副国级及以上者不受年龄限制，可以继续任职。
        // 若本次考核已经通过晋升资格，则按「晋升后的职级」判断，避免把即将进入
        // 副国级的玩家在任命生效之前提前拦下。
        val prospectiveRankIndex = if (pendingPromotion) {
            com.shitu.app.model.nextPromotionIndex(newState.player.positionIndex)
        } else {
            newState.player.positionIndex
        }
        if (RetirementEngine.isDue(
                age = newState.player.age,
                rankIndex = prospectiveRankIndex,
                inActiveCareer = newState.player.inActiveCareer
            )
        ) {
            return newState.copy(
                transferOffer = null,
                pendingPromotion = false,
                phase = GamePhase.REVIEW
            )
        }

        // 退休或终局条件在年度考核后立即生效。
        if (serviceYears >= MAX_SERVICE_YEARS) {
            return finish(newState, EndingEngine.finalEnding(newState))
        }
        EndingEngine.terminalEnding(newState)?.let { ending ->
            return finish(newState, ending)
        }
        return newState
    }

    // 结束一局时把结局写进日志，让「日志」页能完整回顾一生。
    private fun finish(state: GameState, ending: Ending): GameState = state.copy(
        ending = ending,
        phase = GamePhase.ENDING,
        outcome = null,
        log = state.log + LogEntry(
            year = state.year,
            quarter = state.quarter,
            title = "结局 · ${ending.title}",
            choiceText = "人生落幕",
            outcome = ending.description,
            deltas = emptyList(),
            kind = LogKind.ENDING
        )
    )

    // 家庭成员随时间自然变化。
    private fun advanceFamily(state: GameState): com.shitu.app.model.FamilyState {
        val family = state.player.family
        val year = state.year
        var updated = family
        if (!family.hasPartner && state.player.age >= 30 && state.player.attributes.family >= 45) {
            updated = updated.copy(hasPartner = true, marriedYear = year)
        } else if (updated.hasPartner && updated.children == 0 && state.player.age in 28..42 &&
            state.player.attributes.family >= 60
        ) {
            updated = updated.copy(children = 1)
        }
        return updated
    }
}
