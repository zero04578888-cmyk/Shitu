package com.shitu.app.util

import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.game.PendingChain
import com.shitu.app.game.GameRng
import com.shitu.app.model.AnnualReview
import com.shitu.app.model.ActiveProject
import com.shitu.app.model.Alliance
import com.shitu.app.model.Attr
import com.shitu.app.model.DecisionRecord
import com.shitu.app.model.DistrictState
import com.shitu.app.model.EconomicPhase
import com.shitu.app.model.EraState
import com.shitu.app.model.GlobalEconomy
import com.shitu.app.model.NewsCategory
import com.shitu.app.model.NewsLevel
import com.shitu.app.model.NpcRuntime
import com.shitu.app.model.PolicyRecord
import com.shitu.app.model.PolicyTag
import com.shitu.app.model.Vacancy
import com.shitu.app.model.WorldNews
import com.shitu.app.model.CrisisState
import com.shitu.app.model.Attributes
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.CareerMemory
import com.shitu.app.model.CareerProfile
import com.shitu.app.model.CareerRecord
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.CityStint
import com.shitu.app.model.PersonalAssets
import com.shitu.app.model.Ending
import com.shitu.app.model.FamilyState
import com.shitu.app.model.Faction
import com.shitu.app.model.HiddenKey
import com.shitu.app.model.HiddenStats
import com.shitu.app.model.HistoryEntry
import com.shitu.app.model.GovernanceState
import com.shitu.app.model.Indicator
import com.shitu.app.model.Jurisdiction
import com.shitu.app.model.JurisdictionLevel
import com.shitu.app.model.LogEntry
import com.shitu.app.model.LogKind
import com.shitu.app.model.OutcomeRecord
import com.shitu.app.model.Merit
import com.shitu.app.model.MeritKind
import com.shitu.app.model.IntelItem
import com.shitu.app.model.IntelLevel
import com.shitu.app.model.NpcStatus
import com.shitu.app.model.Player
import com.shitu.app.model.PoliticalState
import com.shitu.app.model.PoliticalTendency
import com.shitu.app.model.ProjectStatus
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.ResearchNote
import com.shitu.app.model.ReviewItem
import com.shitu.app.model.RelationshipState
import com.shitu.app.model.TransferOffer
import com.shitu.app.model.UnitType
import com.shitu.app.model.WorkPlan
import com.shitu.app.model.WorkSlot
import com.shitu.app.model.WorldState
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.migrateLegacyPositionIndex
import com.shitu.app.model.positionAt
import com.shitu.app.game.GovernanceEngine
import com.shitu.app.game.JurisdictionFactory
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.game.CareerEngine

// 存档编解码。使用纯 Kotlin 实现，不依赖第三方库，也不依赖 Android 框架，
// 因此可以在单元测试中完整验证。
object GameCodec {

    const val FORMAT_VERSION = 9
    private const val SEP = '\t'

    fun encode(state: GameState): String {
        val sb = StringBuilder()
        fun put(key: String, value: String) {
            sb.append(key).append(SEP).append(escape(value)).append('\n')
        }
        fun putInt(key: String, value: Int) = put(key, value.toString())
        fun putLong(key: String, value: Long) = put(key, value.toString())
        fun putBool(key: String, value: Boolean) = put(key, if (value) "1" else "0")

        putInt("version", FORMAT_VERSION)

        val player = state.player
        put("player.name", player.name)
        put("player.gender", player.gender)
        put("player.education", player.education)
        put("player.startTrack", player.startTrack)
        putInt("player.trait.count", player.traits.size)
        player.traits.forEachIndexed { index, trait -> put("player.trait.$index", trait) }
        put("player.unit", player.unit.name)
        putInt("player.positionIndex", player.positionIndex)
        putInt("player.ageAtStart", player.ageAtStart)
        putInt("player.serviceYears", player.serviceYears)
        putInt("player.yearsInPosition", player.yearsInPosition)
        putInt("player.positionStartAbs", player.positionStartAbs)
        // 旧存档若残留「主任科员」等已废弃职位名，写出时自动清理，避免污染后续存档。
        put("player.appointmentTitle", player.effectiveAppointmentTitle)
        put("player.appointmentId", player.appointmentId)
        put("player.appointmentCityId", player.appointmentCityId)
        // 第二十阶段：出生年份与退休信息。旧版本存档没有这些字段，
        // 读取时用开局年龄回推出生年份，不会闪退。
        putInt("player.birthYear", player.effectiveBirthYear)
        putBool("player.retired", player.retired)
        put("player.retirementType", player.retirementType)
        putInt("player.retirementAge", player.retirementAge)
        putInt("player.retirementYear", player.retirementYear)

        Attr.entries.forEach { attr ->
            putInt("attr.${attr.name}", player.attributes.valueOf(attr))
        }
        HiddenKey.entries.forEach { key ->
            putInt("hidden.${key.name}", player.hidden.valueOf(key))
        }
        putBool("family.hasParents", player.family.hasParents)
        putBool("family.hasPartner", player.family.hasPartner)
        putInt("family.children", player.family.children)
        putInt("family.marriedYear", player.family.marriedYear)

        putInt("year", state.year)
        putInt("quarter", state.quarter)
        putInt("reviewCount", state.reviewCount)
        putInt("totalWorkGained", state.totalWorkGained)
        putLong("rngState", state.rngState)
        putInt("promotionCount", state.promotionCount)
        putInt("warningCount", state.warningCount)
        put("route", state.route?.name.orEmpty())
        putInt("reachedTopYear", state.reachedTopYear ?: -1)
        putInt("demotionCount", state.demotionCount)
        put("currentEventId", state.currentEventId)
        put("phase", state.phase.name)
        // 第十八阶段：年度考核已通过晋升、等待玩家选择具体职务的挂起状态。
        putBool("pendingPromotion", state.pendingPromotion)

        // 结算结果也要存档：否则在「结果」阶段退出后再进入会卡住。
        val outcome = state.outcome
        putBool("outcome.present", outcome != null)
        if (outcome != null) {
            put("outcome.eventId", outcome.eventId)
            put("outcome.eventTitle", outcome.eventTitle)
            put("outcome.choiceText", outcome.choiceText)
            put("outcome.text", outcome.outcome)
            writeDeltas(sb, "outcome", outcome.deltas)
            writeDeltas(sb, "outcome.hidden", outcome.hiddenDeltas)
            writeDeltas(sb, "outcome.relation", outcome.relationDeltas)
            writeDeltas(sb, "outcome.governance", outcome.governanceDeltas)
        }

        putInt("log.count", state.log.size)
        state.log.forEachIndexed { index, entry ->
            put("log.$index.year", entry.year.toString())
            put("log.$index.quarter", entry.quarter.toString())
            put("log.$index.title", entry.title)
            put("log.$index.choice", entry.choiceText)
            put("log.$index.outcome", entry.outcome)
            put("log.$index.kind", entry.kind.name)
            putInt("log.$index.delta.count", entry.deltas.size)
            entry.deltas.forEachIndexed { deltaIndex, pair ->
                put("log.$index.delta.$deltaIndex.key", pair.first)
                put("log.$index.delta.$deltaIndex.value", pair.second.toString())
            }
        }

        putInt("review.count", state.reviews.size)
        state.reviews.forEachIndexed { index, review ->
            put("review.$index.year", review.year.toString())
            put("review.$index.score", review.score.toString())
            put("review.$index.grade", review.grade)
            put("review.$index.summary", review.summary)
            put("review.$index.promotionText", review.promotionText)
            put("review.$index.promotionGranted", if (review.promotionGranted) "1" else "0")
            putInt("review.$index.item.count", review.items.size)
            review.items.forEachIndexed { itemIndex, item ->
                put("review.$index.item.$itemIndex.label", item.label)
                put("review.$index.item.$itemIndex.comment", item.comment)
                put("review.$index.item.$itemIndex.score", item.score.toString())
            }
        }

        val relationship = state.relationship
        putInt("rel.risk", relationship.risk)
        putInt("rel.npc.count", relationship.relations.size)
        relationship.relations.entries.toList().forEachIndexed { index, entry ->
            put("rel.npc.$index.id", entry.key)
            put("rel.npc.$index.relation", entry.value.toString())
        }
        putInt("rel.trust.count", relationship.trust.size)
        relationship.trust.entries.toList().forEachIndexed { index, entry ->
            put("rel.trust.$index.id", entry.key)
            put("rel.trust.$index.value", entry.value.toString())
        }
        putInt("rel.faction.count", relationship.factions.size)
        relationship.factions.entries.toList().forEachIndexed { index, entry ->
            put("rel.faction.$index.id", entry.key.name)
            put("rel.faction.$index.value", entry.value.toString())
        }
        putInt("rel.cooldown.count", relationship.cooldown.size)
        relationship.cooldown.entries.toList().forEachIndexed { index, entry ->
            put("rel.cooldown.$index.key", entry.key)
            put("rel.cooldown.$index.value", entry.value.toString())
        }
        putInt("rel.secret.count", relationship.secrets.size)
        relationship.secrets.toList().forEachIndexed { index, id -> put("rel.secret.$index", id) }
        putInt("rel.scouted.count", relationship.scouted.size)
        relationship.scouted.toList().forEachIndexed { index, id -> put("rel.scouted.$index", id) }
        putInt("rel.status.count", relationship.statuses.size)
        relationship.statuses.entries.toList().forEachIndexed { index, entry ->
            put("rel.status.$index.id", entry.key)
            put("rel.status.$index.value", entry.value.name)
        }

        val governance = state.governance
        val jurisdiction = governance.jurisdiction
        put("gov.name", jurisdiction.name)
        put("gov.level", jurisdiction.level.name)
        putInt("gov.population", jurisdiction.population)
        Indicator.entries.forEach { indicator ->
            putInt("gov.ind.${indicator.name}", jurisdiction.valueOf(indicator))
        }
        MeritKind.entries.forEach { kind ->
            putInt("gov.merit.${kind.name}", governance.merit.valueOf(kind))
        }
        putInt("gov.finance", governance.finance)
        putInt("gov.actionsUsed", governance.actionsUsed)
        putInt("gov.savings", governance.savings)
        putInt("gov.salary", governance.salaryPerQuarter)
        putInt("gov.completed.count", governance.completed.size)
        governance.completed.toList().forEachIndexed { index, flag ->
            put("gov.completed.$index", flag)
        }
        putInt("gov.cooldown.count", governance.cooldowns.size)
        governance.cooldowns.entries.toList().forEachIndexed { index, entry ->
            put("gov.cooldown.$index.key", entry.key)
            put("gov.cooldown.$index.value", entry.value.toString())
        }

        // ===== 第十二阶段：地区、项目、世界与工作安排（全部可选，旧存档用默认值） =====
        put("regionId", state.regionId)
        // 第十六阶段：真实城市体系。当前城市派生字段与全国仕途履历。
        put("city.id", state.currentCityId)
        put("city.name", state.currentCityName)
        put("city.province", state.currentProvince)
        put("city.type", state.currentCityType)
        putInt("city.power", state.currentCityPower)
        put("city.positionId", state.currentPositionId)
        put("city.positionRank", state.currentPositionRank)
        putInt("city.stint.count", state.careerCities.size)
        state.careerCities.forEachIndexed { index, stint ->
            put("city.stint.$index.id", stint.cityId)
            put("city.stint.$index.name", stint.cityName)
            put("city.stint.$index.province", stint.province)
            put("city.stint.$index.position", stint.positionLabel)
            putInt("city.stint.$index.start", stint.startAbsQuarter)
            putInt("city.stint.$index.end", stint.endAbsQuarter)
        }
        putInt("proj.count", state.projects.size)
        state.projects.forEachIndexed { index, project ->
            put("proj.$index.id", project.id)
            putInt("proj.$index.started", project.startedQuarter)
            putInt("proj.$index.invested", project.invested)
            putInt("proj.$index.progress", project.progress)
            put("proj.$index.status", project.status.name)
            put("proj.$index.note", project.note)
            putInt("proj.$index.opLeft", project.operationLeft)
            putInt("proj.$index.incidents", project.incidents)
        }
        putInt("world.economy", state.world.economyClimate)
        putInt("world.environment", state.world.environmentClimate)
        putInt("world.npc.count", state.world.npcStatuses.size)
        state.world.npcStatuses.entries.toList().forEachIndexed { index, entry ->
            put("world.npc.$index.id", entry.key)
            put("world.npc.$index.status", entry.value.name)
        }
        putInt("world.built.count", state.world.builtProjects.size)
        state.world.builtProjects.forEachIndexed { index, id -> put("world.built.$index", id) }
        putInt("world.flag.count", state.world.majorFlags.size)
        state.world.majorFlags.toList().forEachIndexed { index, flag -> put("world.flag.$index", flag) }
        putInt("world.research.count", state.world.research.size)
        state.world.research.forEachIndexed { index, note ->
            put("world.research.$index.id", note.id)
            put("world.research.$index.label", note.label)
            put("world.research.$index.detail", note.detail)
            putInt("world.research.$index.year", note.year)
            putInt("world.research.$index.quarter", note.quarter)
        }
        // ===== 第十三阶段：世界模拟（经济、地区、政策、决策、动态、NPC、岗位） =====
        putInt("world.month", state.world.month)
        put("world.econ.phase", state.world.economy.phase.name)
        putInt("world.econ.index", state.world.economy.index)
        putInt("world.econ.momentum", state.world.economy.momentum)
        putInt("world.econ.months", state.world.economy.monthsInPhase)
        putInt("world.econ.hist.count", state.world.economy.history.size)
        state.world.economy.history.forEachIndexed { index, phase -> put("world.econ.hist.$index", phase.name) }
        put("world.era", state.world.era.name)
        putInt("world.district.count", state.world.districts.size)
        state.world.districts.values.toList().forEachIndexed { index, d ->
            put("world.district.$index.id", d.id)
            putInt("world.district.$index.population", d.population)
            putInt("world.district.$index.economy", d.economy)
            putInt("world.district.$index.industry", d.industry)
            putInt("world.district.$index.agriculture", d.agriculture)
            putInt("world.district.$index.service", d.service)
            putInt("world.district.$index.technology", d.technology)
            putInt("world.district.$index.tourism", d.tourism)
            putInt("world.district.$index.logistics", d.logistics)
            putInt("world.district.$index.employment", d.employment)
            putInt("world.district.$index.education", d.education)
            putInt("world.district.$index.medical", d.medical)
            putInt("world.district.$index.security", d.security)
            putInt("world.district.$index.infrastructure", d.infrastructure)
            putInt("world.district.$index.environment", d.environment)
            putInt("world.district.$index.attractiveness", d.attractiveness)
            putInt("world.district.$index.finance", d.finance)
        }
        putInt("world.policy.count", state.world.policies.size)
        state.world.policies.forEachIndexed { index, record ->
            put("world.policy.$index.id", record.id)
            putInt("world.policy.$index.year", record.year)
            putInt("world.policy.$index.month", record.month)
            put("world.policy.$index.title", record.title)
            put("world.policy.$index.direction", record.direction)
            put("world.policy.$index.region", record.regionId)
            putInt("world.policy.$index.magnitude", record.magnitude)
            put("world.policy.$index.summary", record.summary)
            putInt("world.policy.$index.tag.count", record.tags.size)
            record.tags.toList().forEachIndexed { t, tag -> put("world.policy.$index.tag.$t", tag.name) }
        }
        putInt("world.decision.count", state.world.decisions.size)
        state.world.decisions.forEachIndexed { index, record ->
            put("world.decision.$index.id", record.id)
            putInt("world.decision.$index.year", record.year)
            putInt("world.decision.$index.month", record.month)
            put("world.decision.$index.title", record.title)
            put("world.decision.$index.choice", record.choice)
            put("world.decision.$index.consequence", record.consequence)
            put("world.decision.$index.region", record.regionId)
            putInt("world.decision.$index.known.count", record.knownBy.size)
            record.knownBy.forEachIndexed { k, id -> put("world.decision.$index.known.$k", id) }
            putInt("world.decision.$index.tag.count", record.tags.size)
            record.tags.toList().forEachIndexed { t, tag -> put("world.decision.$index.tag.$t", tag.name) }
        }
        putInt("world.news.count", state.world.news.size)
        state.world.news.forEachIndexed { index, item ->
            put("world.news.$index.id", item.id)
            putInt("world.news.$index.year", item.year)
            putInt("world.news.$index.month", item.month)
            put("world.news.$index.category", item.category.name)
            put("world.news.$index.title", item.title)
            put("world.news.$index.detail", item.detail)
            put("world.news.$index.level", item.level.name)
            put("world.news.$index.region", item.regionId)
        }
        putInt("world.npcruntime.count", state.world.npcs.size)
        state.world.npcs.values.toList().forEachIndexed { index, runtime ->
            put("world.npcruntime.$index.id", runtime.npcId)
            putInt("world.npcruntime.$index.ability", runtime.ability)
            putInt("world.npcruntime.$index.merit", runtime.merit)
            putInt("world.npcruntime.$index.influence", runtime.influence)
            putInt("world.npcruntime.$index.position", runtime.positionIndex)
            putBool("world.npcruntime.$index.active", runtime.active)
            putInt("world.npcruntime.$index.last", runtime.lastActionMonth)
            put("world.npcruntime.$index.agenda", runtime.agenda)
        }
        putInt("world.npcrel.count", state.world.npcRelations.size)
        state.world.npcRelations.entries.toList().forEachIndexed { index, entry ->
            put("world.npcrel.$index.key", entry.key)
            putInt("world.npcrel.$index.value", entry.value)
        }
        putInt("world.vacancy.count", state.world.vacancies.size)
        state.world.vacancies.forEachIndexed { index, vacancy ->
            putInt("world.vacancy.$index.position", vacancy.positionIndex)
            put("world.vacancy.$index.region", vacancy.regionId)
            put("world.vacancy.$index.reason", vacancy.reason)
            putInt("world.vacancy.$index.created", vacancy.createdMonth)
            putBool("world.vacancy.$index.filled", vacancy.filled)
        }
        putInt("plan.quarter", state.workPlan.plannedQuarter)
        putInt("plan.count", state.workPlan.slots.size)
        state.workPlan.slots.forEachIndexed { index, slot -> put("plan.$index", slot.name) }
        val offer = state.transferOffer
        putBool("offer.present", offer != null)
        if (offer != null) {
            put("offer.regionId", offer.regionId)
            put("offer.reason", offer.reason)
            putInt("offer.expires", offer.expiresQuarter)
            putInt("offer.tier", offer.tier)
            putInt("offer.position", offer.positionIndex)
        }

        // ===== 第九阶段：政治 =====
        val politics = state.politics
        putInt("pol.capital", politics.capital)
        PoliticalTendency.entries.forEach { tendency ->
            putInt("pol.tendency.${tendency.name}", politics.tendencyOf(tendency))
        }
        Faction.entries.forEach { faction ->
            putInt("pol.influence.${faction.name}", politics.influenceOf(faction))
        }
        putInt("pol.intel.count", politics.intel.size)
        politics.intel.forEachIndexed { index, item ->
            put("pol.intel.$index.id", item.id)
            put("pol.intel.$index.title", item.title)
            put("pol.intel.$index.detail", item.detail)
            put("pol.intel.$index.level", item.level.name)
            putInt("pol.intel.$index.year", item.year)
            putInt("pol.intel.$index.quarter", item.quarter)
        }
        putInt("pol.alliance.count", politics.alliances.size)
        politics.alliances.forEachIndexed { index, alliance ->
            put("pol.alliance.$index.id", alliance.id)
            put("pol.alliance.$index.label", alliance.label)
            put("pol.alliance.$index.npc", alliance.npcId.orEmpty())
            put("pol.alliance.$index.faction", alliance.faction?.name.orEmpty())
            putInt("pol.alliance.$index.until", alliance.untilQuarter)
            put("pol.alliance.$index.note", alliance.note)
        }
        val crisis = politics.crisis
        putBool("pol.crisis.present", crisis != null)
        if (crisis != null) {
            put("pol.crisis.id", crisis.id)
            put("pol.crisis.title", crisis.title)
            put("pol.crisis.detail", crisis.detail)
            putInt("pol.crisis.severity", crisis.severity)
            putInt("pol.crisis.year", crisis.year)
        }
        putInt("pol.history.count", politics.history.size)
        politics.history.forEachIndexed { index, entry ->
            putInt("pol.history.$index.year", entry.year)
            putInt("pol.history.$index.quarter", entry.quarter)
            put("pol.history.$index.title", entry.title)
            put("pol.history.$index.choice", entry.choice)
            put("pol.history.$index.consequence", entry.consequence)
        }
        putInt("pol.major.count", politics.majorChoices.size)
        politics.majorChoices.toList().forEachIndexed { index, id ->
            put("pol.major.$index", id)
        }

        // ===== 第十阶段：仕途人生 =====
        val career = state.career
        putInt("career.experience", career.experience)
        putInt("career.leadership", career.leadership)
        putInt("career.decision", career.decision)
        putInt("career.communication", career.communication)
        putInt("career.execution", career.execution)
        putInt("career.specialty", career.specialty)
        putInt("career.positions", career.positionsHeld)
        putInt("career.highest", career.highestPositionIndex)
        putInt("career.personal", career.personalActionsUsed)
        putInt("career.assets.housing", career.assets.housing)
        putInt("career.assets.vehicle", career.assets.vehicle)
        putInt("career.priorLives", career.priorLives)
        putInt("career.positionHistory.count", career.positionHistory.size)
        career.positionHistory.forEachIndexed { index, id ->
            put("career.positionHistory.$index", id)
        }
        putInt("career.record.count", career.records.size)
        career.records.forEachIndexed { index, record ->
            put("career.record.$index.year", record.year.toString())
            put("career.record.$index.quarter", record.quarter.toString())
            put("career.record.$index.age", record.age.toString())
            put("career.record.$index.title", record.title)
            put("career.record.$index.detail", record.detail)
            put("career.record.$index.kind", record.kind.name)
            put("career.record.$index.position", record.positionName)
            put("career.record.$index.positionTitle", record.positionTitle)
            put("career.record.$index.region", record.regionName)
            put("career.record.$index.from", record.fromPosition)
            put("career.record.$index.to", record.toPosition)
        }
        putInt("career.memory.count", career.memories.size)
        career.memories.forEachIndexed { index, memory ->
            put("career.memory.$index.npc", memory.npcId)
            put("career.memory.$index.year", memory.year.toString())
            put("career.memory.$index.summary", memory.summary)
            put("career.memory.$index.relation", memory.relationBias.toString())
            put("career.memory.$index.trust", memory.trustBias.toString())
        }
        putInt("career.ach.count", career.unlockedAchievements.size)
        career.unlockedAchievements.toList().forEachIndexed { index, id ->
            put("career.ach.$index", id)
        }

        putInt("triggered.count", state.triggeredEvents.size)
        state.triggeredEvents.toList().forEachIndexed { index, id ->
            put("triggered.$index", id)
        }

        putInt("cooldown.count", state.eventCooldown.size)
        state.eventCooldown.entries.toList().forEachIndexed { index, entry ->
            put("cooldown.$index.key", entry.key)
            put("cooldown.$index.value", entry.value.toString())
        }

        putInt("flags.count", state.flags.size)
        state.flags.toList().forEachIndexed { index, flag ->
            put("flags.$index", flag)
        }

        putInt("chain.count", state.pendingChains.size)
        state.pendingChains.forEachIndexed { index, chain ->
            put("chain.$index.id", chain.chainId)
            put("chain.$index.step", chain.nextStep.toString())
            put("chain.$index.due", chain.dueQuarter.toString())
        }

        val ending = state.ending
        putBool("ending.present", ending != null)
        if (ending != null) {
            put("ending.id", ending.id)
            put("ending.title", ending.title)
            put("ending.category", ending.category)
            put("ending.description", ending.description)
            putInt("ending.highlight.count", ending.highlights.size)
            ending.highlights.forEachIndexed { index, pair ->
                put("ending.highlight.$index.key", pair.first)
                put("ending.highlight.$index.value", pair.second)
            }
            put("ending.tier", ending.tier)
            putInt("ending.reason.count", ending.reasons.size)
            ending.reasons.forEachIndexed { index, value -> put("ending.reason.$index", value) }
            putInt("ending.node.count", ending.keyNodes.size)
            ending.keyNodes.forEachIndexed { index, value -> put("ending.node.$index", value) }
            putInt("ending.choice.count", ending.keyChoices.size)
            ending.keyChoices.forEachIndexed { index, value -> put("ending.choice.$index", value) }
        }
        // 结尾标记：存档被截断时可以立刻发现，而不是读出一半数据。
        put("end", "1")
        return sb.toString()
    }

    fun decode(text: String): GameState? {
        return try {
            val map = parse(text)
            val version = map.int("version") ?: return null
            if (version > FORMAT_VERSION) return null
            // 第二版起要求结尾标记，缺少说明存档不完整。
            if (version >= 2 && map["end"] != "1") return null

            // 旧版职级布局（<=7）中存在已废弃的「副主任科员 / 主任科员」，
            // 读取时统一归一到副科级；新版存档按原值读取，保证编码-解码往返一致。
            val legacyRankLayout = version <= 7
            fun normalizedRank(value: Int): Int =
                if (legacyRankLayout) com.shitu.app.model.normalizePositionIndex(value)
                else value.coerceIn(0, POSITION_LADDER.size - 1)

            val attributes = Attributes(
                work = map.requiredInt("attr.${Attr.WORK.name}"),
                ability = map.requiredInt("attr.${Attr.ABILITY.name}"),
                politics = map.requiredInt("attr.${Attr.POLITICS.name}"),
                writing = map.requiredInt("attr.${Attr.WRITING.name}"),
                coordination = map.requiredInt("attr.${Attr.COORDINATION.name}"),
                masses = map.requiredInt("attr.${Attr.MASSES.name}"),
                leaderTrust = map.requiredInt("attr.${Attr.LEADER_TRUST.name}"),
                colleagues = map.requiredInt("attr.${Attr.COLLEAGUES.name}"),
                reputation = map.requiredInt("attr.${Attr.REPUTATION.name}"),
                integrityRisk = map.requiredInt("attr.${Attr.INTEGRITY_RISK.name}"),
                health = map.requiredInt("attr.${Attr.HEALTH.name}"),
                energy = map.requiredInt("attr.${Attr.ENERGY.name}"),
                family = map.requiredInt("attr.${Attr.FAMILY.name}"),
                wealth = map.requiredInt("attr.${Attr.WEALTH.name}")
            ).normalized()

            val hidden = HiddenStats(
                opportunity = map.requiredInt("hidden.${HiddenKey.OPPORTUNITY.name}"),
                mentor = map.requiredInt("hidden.${HiddenKey.MENTOR.name}"),
                riskPressure = map.requiredInt("hidden.${HiddenKey.RISK_PRESSURE.name}"),
                socialInfluence = map.requiredInt("hidden.${HiddenKey.SOCIAL_INFLUENCE.name}"),
                stress = map.requiredInt("hidden.${HiddenKey.STRESS.name}"),
                healthRisk = map.requiredInt("hidden.${HiddenKey.HEALTH_RISK.name}"),
                network = map.requiredInt("hidden.${HiddenKey.NETWORK.name}")
            ).normalized()

            val log = ArrayList<LogEntry>()
            val logCount = map.int("log.count") ?: 0
            for (index in 0 until logCount) {
                log.add(
                    LogEntry(
                        year = map.requiredInt("log.$index.year"),
                        quarter = map.requiredInt("log.$index.quarter"),
                        title = map.required("log.$index.title"),
                        choiceText = map.required("log.$index.choice"),
                        outcome = map.required("log.$index.outcome"),
                        deltas = readDeltas(map, "log.$index"),
                        kind = LogKind.entries.firstOrNull { it.name == map.required("log.$index.kind") }
                            ?: LogKind.EVENT
                    )
                )
            }

            val reviews = ArrayList<AnnualReview>()
            val reviewCount = map.int("review.count") ?: 0
            for (index in 0 until reviewCount) {
                val items = ArrayList<ReviewItem>()
                val itemCount = map.int("review.$index.item.count") ?: 0
                for (itemIndex in 0 until itemCount) {
                    items.add(
                        ReviewItem(
                            label = map.required("review.$index.item.$itemIndex.label"),
                            comment = map.required("review.$index.item.$itemIndex.comment"),
                            score = map.requiredInt("review.$index.item.$itemIndex.score")
                        )
                    )
                }
                reviews.add(
                    AnnualReview(
                        year = map.requiredInt("review.$index.year"),
                        score = map.requiredInt("review.$index.score"),
                        grade = map.required("review.$index.grade"),
                        summary = map.required("review.$index.summary"),
                        items = items,
                        promotionText = map["review.$index.promotionText"].orEmpty(),
                        promotionGranted = map["review.$index.promotionGranted"] == "1"
                    )
                )
            }

            val triggered = LinkedHashSet<String>()
            val triggeredCount = map.int("triggered.count") ?: 0
            for (index in 0 until triggeredCount) {
                map["triggered.$index"]?.let { triggered.add(it) }
            }

            val cooldown = LinkedHashMap<String, Int>()
            val cooldownCount = map.int("cooldown.count") ?: 0
            for (index in 0 until cooldownCount) {
                val key = map["cooldown.$index.key"] ?: continue
                val value = map.int("cooldown.$index.value") ?: continue
                cooldown[key] = value
            }

            val flags = LinkedHashSet<String>()
            val flagCount = map.int("flags.count") ?: 0
            for (index in 0 until flagCount) {
                map["flags.$index"]?.let { flags.add(it) }
            }

            val chains = ArrayList<PendingChain>()
            val chainCount = map.int("chain.count") ?: 0
            for (index in 0 until chainCount) {
                chains.add(
                    PendingChain(
                        chainId = map.required("chain.$index.id"),
                        nextStep = map.requiredInt("chain.$index.step"),
                        dueQuarter = map.requiredInt("chain.$index.due")
                    )
                )
            }

            val traits = ArrayList<String>()
            val traitCount = map.int("player.trait.count") ?: 0
            for (index in 0 until traitCount) {
                map["player.trait.$index"]?.let { traits.add(it) }
            }

            val relRelations = LinkedHashMap<String, Int>()
            val relNpcCount = map.int("rel.npc.count") ?: 0
            for (index in 0 until relNpcCount) {
                val id = map["rel.npc.$index.id"] ?: continue
                val value = map.int("rel.npc.$index.relation") ?: continue
                relRelations[id] = value
            }

            val relTrust = LinkedHashMap<String, Int>()
            val relTrustCount = map.int("rel.trust.count") ?: 0
            for (index in 0 until relTrustCount) {
                val id = map["rel.trust.$index.id"] ?: continue
                val value = map.int("rel.trust.$index.value") ?: continue
                relTrust[id] = value
            }

            val relFactions = LinkedHashMap<Faction, Int>()
            val relFactionCount = map.int("rel.faction.count") ?: 0
            for (index in 0 until relFactionCount) {
                val faction = Faction.byName(map["rel.faction.$index.id"].orEmpty()) ?: continue
                val value = map.int("rel.faction.$index.value") ?: continue
                relFactions[faction] = value
            }

            val relCooldown = LinkedHashMap<String, Int>()
            val relCooldownCount = map.int("rel.cooldown.count") ?: 0
            for (index in 0 until relCooldownCount) {
                val key = map["rel.cooldown.$index.key"] ?: continue
                val value = map.int("rel.cooldown.$index.value") ?: continue
                relCooldown[key] = value
            }

            val relSecrets = LinkedHashSet<String>()
            val relSecretCount = map.int("rel.secret.count") ?: 0
            for (index in 0 until relSecretCount) {
                map["rel.secret.$index"]?.let { relSecrets.add(it) }
            }

            val relScouted = LinkedHashSet<String>()
            val relScoutedCount = map.int("rel.scouted.count") ?: 0
            for (index in 0 until relScoutedCount) {
                map["rel.scouted.$index"]?.let { relScouted.add(it) }
            }

            val relStatuses = LinkedHashMap<String, NpcStatus>()
            val relStatusCount = map.int("rel.status.count") ?: 0
            for (index in 0 until relStatusCount) {
                val id = map["rel.status.$index.id"] ?: continue
                val status = NpcStatus.byName(map["rel.status.$index.value"]) ?: continue
                relStatuses[id] = status
            }

            val relationship = RelationshipState(
                relations = relRelations,
                trust = relTrust,
                factions = relFactions,
                risk = (map.int("rel.risk") ?: 0).coerceIn(0, 100),
                cooldown = relCooldown,
                secrets = relSecrets,
                scouted = relScouted,
                statuses = relStatuses
            )

            val decodedGovernance = map["gov.name"]?.let { govName ->
                val level = JurisdictionLevel.entries
                    .firstOrNull { it.name == map["gov.level"] }
                    ?: JurisdictionLevel.TOWNSHIP
                var jurisdiction = JurisdictionFactory.profileOf(level).copy(
                    name = govName,
                    population = map.int("gov.population") ?: level.population
                )
                Indicator.entries.forEach { indicator ->
                    map.int("gov.ind.${indicator.name}")?.let { value ->
                        jurisdiction = jurisdiction.withValue(indicator, value)
                    }
                }
                var merit = Merit()
                MeritKind.entries.forEach { kind ->
                    map.int("gov.merit.${kind.name}")?.let { value ->
                        merit = merit.withValue(kind, value)
                    }
                }
                val completed = LinkedHashSet<String>()
                val completedCount = map.int("gov.completed.count") ?: 0
                for (index in 0 until completedCount) {
                    map["gov.completed.$index"]?.let { completed.add(it) }
                }
                val govCooldown = LinkedHashMap<String, Int>()
                val govCooldownCount = map.int("gov.cooldown.count") ?: 0
                for (index in 0 until govCooldownCount) {
                    val key = map["gov.cooldown.$index.key"] ?: continue
                    val value = map.int("gov.cooldown.$index.value") ?: continue
                    govCooldown[key] = value
                }
                GovernanceState(
                    jurisdiction = jurisdiction,
                    merit = merit,
                    finance = (map.int("gov.finance") ?: level.baseFinance).coerceAtLeast(0),
                    actionsUsed = (map.int("gov.actionsUsed") ?: 0).coerceAtLeast(0),
                    completed = completed,
                    cooldowns = govCooldown,
                    savings = (map.int("gov.savings") ?: 0).coerceAtLeast(0),
                    salaryPerQuarter = map.int("gov.salary") ?: 0
                )
            }

            val ending: Ending? = if (map["ending.present"] == "1") {
                val highlights = ArrayList<Pair<String, String>>()
                val highlightCount = map.int("ending.highlight.count") ?: 0
                for (index in 0 until highlightCount) {
                    highlights.add(
                        map.required("ending.highlight.$index.key") to
                            map.required("ending.highlight.$index.value")
                    )
                }
                Ending(
                    id = map.required("ending.id"),
                    title = map.required("ending.title"),
                    category = map.required("ending.category"),
                    description = map.required("ending.description"),
                    highlights = highlights,
                    tier = map["ending.tier"].orEmpty(),
                    reasons = readStringList(map, "ending.reason"),
                    keyNodes = readStringList(map, "ending.node"),
                    keyChoices = readStringList(map, "ending.choice")
                )
            } else {
                null
            }

            val decodedCareer: CareerProfile = if (map["career.experience"] == null) {
                CareerEngine.initial()
            } else {
                val recordCount = (map.int("career.record.count") ?: 0)
                    .coerceIn(0, CareerProfile.MAX_RECORDS)
                val records = ArrayList<CareerRecord>(recordCount)
                for (index in 0 until recordCount) {
                    val title = map["career.record.$index.title"] ?: continue
                    records.add(
                        CareerRecord(
                            year = map.int("career.record.$index.year") ?: 1,
                            quarter = map.int("career.record.$index.quarter") ?: 1,
                            age = map.int("career.record.$index.age") ?: 24,
                            title = title,
                            detail = map["career.record.$index.detail"].orEmpty(),
                            kind = CareerRecordKind.entries.firstOrNull {
                                it.name == map["career.record.$index.kind"]
                            } ?: CareerRecordKind.START,
                            positionName = map["career.record.$index.position"].orEmpty(),
                            positionTitle = map["career.record.$index.positionTitle"].orEmpty(),
                            regionName = map["career.record.$index.region"].orEmpty(),
                            fromPosition = map["career.record.$index.from"].orEmpty(),
                            toPosition = map["career.record.$index.to"].orEmpty()
                        )
                    )
                }
                val memoryCount = (map.int("career.memory.count") ?: 0)
                    .coerceIn(0, CareerProfile.MAX_MEMORIES)
                val memories = ArrayList<CareerMemory>(memoryCount)
                for (index in 0 until memoryCount) {
                    val npcId = map["career.memory.$index.npc"] ?: continue
                    memories.add(
                        CareerMemory(
                            npcId = npcId,
                            year = map.int("career.memory.$index.year") ?: 1,
                            summary = map["career.memory.$index.summary"].orEmpty(),
                            relationBias = map.int("career.memory.$index.relation") ?: 0,
                            trustBias = map.int("career.memory.$index.trust") ?: 0
                        )
                    )
                }
                val achievements = LinkedHashSet<String>()
                val achCount = (map.int("career.ach.count") ?: 0).coerceIn(0, 64)
                for (index in 0 until achCount) {
                    map["career.ach.$index"]?.let { achievements.add(it) }
                }
                val positionHistory = mutableListOf<String>()
                val positionHistoryCount = (map.int("career.positionHistory.count") ?: 0).coerceIn(0, 128)
                for (index in 0 until positionHistoryCount) {
                    map["career.positionHistory.$index"]?.let { positionHistory.add(it) }
                }
                CareerProfile(
                    experience = map.int("career.experience") ?: 0,
                    leadership = map.int("career.leadership") ?: 30,
                    decision = map.int("career.decision") ?: 30,
                    communication = map.int("career.communication") ?: 30,
                    execution = map.int("career.execution") ?: 30,
                    specialty = map.int("career.specialty") ?: 30,
                    positionsHeld = map.int("career.positions") ?: 1,
                    highestPositionIndex = map.int("career.highest") ?: 0,
                    personalActionsUsed = map.int("career.personal") ?: 0,
                    records = records,
                    memories = memories,
                    assets = PersonalAssets(
                        housing = map.int("career.assets.housing") ?: 0,
                        vehicle = map.int("career.assets.vehicle") ?: 0
                    ),
                    unlockedAchievements = achievements,
                    priorLives = map.int("career.priorLives") ?: 0,
                    positionHistory = positionHistory
                )
            }

            val decodedPolitics: PoliticalState = if (map["pol.capital"] == null) {
                PoliticsEngine.initial()
            } else {
                run {
                    val tendencies = LinkedHashMap<PoliticalTendency, Int>()
                    PoliticalTendency.entries.forEach { tendency ->
                        tendencies[tendency] = map.int("pol.tendency.${tendency.name}") ?: 0
                    }
                    val influence = LinkedHashMap<Faction, Int>()
                    Faction.entries.forEach { faction ->
                        influence[faction] = map.int("pol.influence.${faction.name}") ?: 0
                    }
                    val intel = ArrayList<IntelItem>()
                    val intelCount = map.int("pol.intel.count") ?: 0
                    for (index in 0 until intelCount) {
                        val id = map["pol.intel.$index.id"] ?: continue
                        intel.add(
                            IntelItem(
                                id = id,
                                title = map["pol.intel.$index.title"].orEmpty(),
                                detail = map["pol.intel.$index.detail"].orEmpty(),
                                level = IntelLevel.byName(map["pol.intel.$index.level"]),
                                year = map.int("pol.intel.$index.year") ?: 1,
                                quarter = map.int("pol.intel.$index.quarter") ?: 1
                            )
                        )
                    }
                    val alliances = ArrayList<Alliance>()
                    val allianceCount = map.int("pol.alliance.count") ?: 0
                    for (index in 0 until allianceCount) {
                        val id = map["pol.alliance.$index.id"] ?: continue
                        alliances.add(
                            Alliance(
                                id = id,
                                label = map["pol.alliance.$index.label"].orEmpty(),
                                npcId = map["pol.alliance.$index.npc"]?.takeIf { it.isNotEmpty() },
                                faction = Faction.byName(map["pol.alliance.$index.faction"].orEmpty()),
                                untilQuarter = map.int("pol.alliance.$index.until") ?: 0,
                                note = map["pol.alliance.$index.note"].orEmpty()
                            )
                        )
                    }
                    val crisis = if (map["pol.crisis.present"] == "1") {
                        CrisisState(
                            id = map["pol.crisis.id"].orEmpty(),
                            title = map["pol.crisis.title"].orEmpty(),
                            detail = map["pol.crisis.detail"].orEmpty(),
                            severity = map.int("pol.crisis.severity") ?: 1,
                            year = map.int("pol.crisis.year") ?: 1
                        )
                    } else {
                        null
                    }
                    val history = ArrayList<HistoryEntry>()
                    val historyCount = map.int("pol.history.count") ?: 0
                    for (index in 0 until historyCount) {
                        history.add(
                            HistoryEntry(
                                year = map.int("pol.history.$index.year") ?: 1,
                                quarter = map.int("pol.history.$index.quarter") ?: 1,
                                title = map["pol.history.$index.title"].orEmpty(),
                                choice = map["pol.history.$index.choice"].orEmpty(),
                                consequence = map["pol.history.$index.consequence"].orEmpty()
                            )
                        )
                    }
                    val major = LinkedHashSet<String>()
                    val majorCount = map.int("pol.major.count") ?: 0
                    for (index in 0 until majorCount) {
                        map["pol.major.$index"]?.let { major.add(it) }
                    }
                    PoliticalState(
                        capital = map.int("pol.capital") ?: 0,
                        tendencies = tendencies,
                        factionInfluence = influence,
                        intel = intel,
                        alliances = alliances,
                        crisis = crisis,
                        history = history,
                        majorChoices = major
                    )
                }
            }

            val player = Player(
                name = map.required("player.name"),
                gender = map.required("player.gender"),
                education = map.required("player.education"),
                startTrack = map.required("player.startTrack"),
                traits = traits,
                unit = UnitType.byName(map.required("player.unit")),
                positionIndex = map.requiredInt("player.positionIndex").let { raw ->
                    val mapped = if (version <= 5) migrateLegacyPositionIndex(raw) else raw
                    // 停在旧版残留职级的存档归一到副科级，避免职位观感倒退。
                    normalizedRank(mapped)
                },
                attributes = attributes,
                hidden = hidden,
                family = FamilyState(
                    hasParents = map["family.hasParents"] == "1",
                    hasPartner = map["family.hasPartner"] == "1",
                    children = map.int("family.children") ?: 0,
                    marriedYear = map.int("family.marriedYear") ?: 0
                ),
                ageAtStart = map.requiredInt("player.ageAtStart"),
                serviceYears = map.requiredInt("player.serviceYears"),
                yearsInPosition = map.requiredInt("player.yearsInPosition"),
                positionStartAbs = map.int("player.positionStartAbs") ?: 0,
                appointmentTitle = map["player.appointmentTitle"].orEmpty(),
                appointmentId = map["player.appointmentId"].orEmpty(),
                appointmentCityId = map["player.appointmentCityId"].orEmpty(),
                birthYear = map.int("player.birthYear") ?: 0,
                retired = map["player.retired"] == "1",
                retirementType = map["player.retirementType"].orEmpty(),
                retirementAge = map.int("player.retirementAge") ?: 0,
                retirementYear = map.int("player.retirementYear") ?: 0
            )

            val phase = GamePhase.entries.firstOrNull { it.name == map["phase"] } ?: GamePhase.EVENT
            val regionId = map["regionId"].orEmpty().ifBlank { RegionCatalog.default.id }
                .let { if (RegionCatalog.find(it) != null) it else RegionCatalog.default.id }
            val cityStints = ArrayList<CityStint>()
            val stintCount = map.int("city.stint.count") ?: 0
            for (index in 0 until stintCount) {
                val id = map["city.stint.$index.id"] ?: continue
                cityStints.add(
                    CityStint(
                        cityId = id,
                        cityName = map["city.stint.$index.name"].orEmpty(),
                        province = map["city.stint.$index.province"].orEmpty(),
                        positionLabel = map["city.stint.$index.position"].orEmpty(),
                        startAbsQuarter = map.int("city.stint.$index.start") ?: 0,
                        endAbsQuarter = map.int("city.stint.$index.end") ?: -1
                    )
                )
            }
            // 旧存档没有城市履历：按当前城市补一段，保证履历非空、可展示。
            if (cityStints.isEmpty()) {
                val city = RegionCatalog.cityOf(regionId)
                cityStints.add(
                    CityStint(
                        cityId = city.id,
                        cityName = city.name,
                        province = city.province,
                        positionLabel = player.positionName,
                        startAbsQuarter = 0
                    )
                )
            }
            val projects = ArrayList<ActiveProject>()
            val projectCount = map.int("proj.count") ?: 0
            for (index in 0 until projectCount) {
                val id = map["proj.$index.id"] ?: continue
                projects.add(
                    ActiveProject(
                        id = id,
                        startedQuarter = map.int("proj.$index.started") ?: 0,
                        invested = map.int("proj.$index.invested") ?: 0,
                        progress = map.int("proj.$index.progress") ?: 0,
                        status = ProjectStatus.entries.firstOrNull { it.name == map["proj.$index.status"] }
                            ?: ProjectStatus.ONGOING,
                        note = map["proj.$index.note"].orEmpty(),
                        operationLeft = map.int("proj.$index.opLeft") ?: 0,
                        incidents = map.int("proj.$index.incidents") ?: 0
                    )
                )
            }
            val npcStatuses = LinkedHashMap<String, NpcStatus>()
            val npcCount = map.int("world.npc.count") ?: 0
            for (index in 0 until npcCount) {
                val id = map["world.npc.$index.id"] ?: continue
                npcStatuses[id] = NpcStatus.of(map["world.npc.$index.status"])
            }
            val built = ArrayList<String>()
            val builtCount = map.int("world.built.count") ?: 0
            for (index in 0 until builtCount) map["world.built.$index"]?.let { built.add(it) }
            val worldFlags = LinkedHashSet<String>()
            val worldFlagCount = map.int("world.flag.count") ?: 0
            for (index in 0 until worldFlagCount) map["world.flag.$index"]?.let { worldFlags.add(it) }
            val research = ArrayList<ResearchNote>()
            val researchCount = map.int("world.research.count") ?: 0
            for (index in 0 until researchCount) {
                val id = map["world.research.$index.id"] ?: continue
                research.add(
                    ResearchNote(
                        id = id,
                        label = map["world.research.$index.label"].orEmpty(),
                        detail = map["world.research.$index.detail"].orEmpty(),
                        year = map.int("world.research.$index.year") ?: 1,
                        quarter = map.int("world.research.$index.quarter") ?: 1
                    )
                )
            }
            // 第十三阶段：世界模拟数据。旧存档没有这些字段时全部使用默认值，保证可以继续游戏。
            val economyHistory = ArrayList<EconomicPhase>()
            val economyHistoryCount = map.int("world.econ.hist.count") ?: 0
            for (index in 0 until economyHistoryCount) {
                EconomicPhase.byName(map["world.econ.hist.$index"])?.let { economyHistory.add(it) }
            }
            val economy = GlobalEconomy(
                phase = EconomicPhase.byName(map["world.econ.phase"]) ?: EconomicPhase.STABLE,
                index = map.int("world.econ.index") ?: 50,
                momentum = map.int("world.econ.momentum") ?: 0,
                monthsInPhase = map.int("world.econ.months") ?: 0,
                history = economyHistory
            )
            val districts = LinkedHashMap<String, DistrictState>()
            val districtCount = map.int("world.district.count") ?: 0
            for (index in 0 until districtCount) {
                val id = map["world.district.$index.id"] ?: continue
                districts[id] = DistrictState(
                    id = id,
                    population = map.int("world.district.$index.population") ?: 50,
                    economy = map.int("world.district.$index.economy") ?: 50,
                    industry = map.int("world.district.$index.industry") ?: 40,
                    agriculture = map.int("world.district.$index.agriculture") ?: 40,
                    service = map.int("world.district.$index.service") ?: 40,
                    technology = map.int("world.district.$index.technology") ?: 30,
                    tourism = map.int("world.district.$index.tourism") ?: 20,
                    logistics = map.int("world.district.$index.logistics") ?: 30,
                    employment = map.int("world.district.$index.employment") ?: 50,
                    education = map.int("world.district.$index.education") ?: 50,
                    medical = map.int("world.district.$index.medical") ?: 50,
                    security = map.int("world.district.$index.security") ?: 55,
                    infrastructure = map.int("world.district.$index.infrastructure") ?: 50,
                    environment = map.int("world.district.$index.environment") ?: 55,
                    attractiveness = map.int("world.district.$index.attractiveness") ?: 50,
                    finance = map.int("world.district.$index.finance") ?: 60
                )
            }
            val policies = ArrayList<PolicyRecord>()
            val policyCount = map.int("world.policy.count") ?: 0
            for (index in 0 until policyCount) {
                val id = map["world.policy.$index.id"] ?: continue
                val tags = LinkedHashSet<PolicyTag>()
                val tagCount = map.int("world.policy.$index.tag.count") ?: 0
                for (t in 0 until tagCount) {
                    PolicyTag.byName(map["world.policy.$index.tag.$t"])?.let { tags.add(it) }
                }
                policies.add(
                    PolicyRecord(
                        id = id,
                        year = map.int("world.policy.$index.year") ?: 1,
                        month = map.int("world.policy.$index.month") ?: 0,
                        title = map["world.policy.$index.title"].orEmpty(),
                        direction = map["world.policy.$index.direction"].orEmpty(),
                        regionId = map["world.policy.$index.region"].orEmpty(),
                        tags = tags,
                        magnitude = map.int("world.policy.$index.magnitude") ?: 1,
                        summary = map["world.policy.$index.summary"].orEmpty()
                    )
                )
            }
            val decisions = ArrayList<DecisionRecord>()
            val decisionCount = map.int("world.decision.count") ?: 0
            for (index in 0 until decisionCount) {
                val id = map["world.decision.$index.id"] ?: continue
                val known = ArrayList<String>()
                val knownCount = map.int("world.decision.$index.known.count") ?: 0
                for (k in 0 until knownCount) map["world.decision.$index.known.$k"]?.let { known.add(it) }
                val tags = LinkedHashSet<PolicyTag>()
                val tagCount = map.int("world.decision.$index.tag.count") ?: 0
                for (t in 0 until tagCount) {
                    PolicyTag.byName(map["world.decision.$index.tag.$t"])?.let { tags.add(it) }
                }
                decisions.add(
                    DecisionRecord(
                        id = id,
                        year = map.int("world.decision.$index.year") ?: 1,
                        month = map.int("world.decision.$index.month") ?: 0,
                        title = map["world.decision.$index.title"].orEmpty(),
                        choice = map["world.decision.$index.choice"].orEmpty(),
                        consequence = map["world.decision.$index.consequence"].orEmpty(),
                        knownBy = known,
                        regionId = map["world.decision.$index.region"].orEmpty(),
                        tags = tags
                    )
                )
            }
            val news = ArrayList<WorldNews>()
            val newsCount = map.int("world.news.count") ?: 0
            for (index in 0 until newsCount) {
                val id = map["world.news.$index.id"] ?: continue
                news.add(
                    WorldNews(
                        id = id,
                        year = map.int("world.news.$index.year") ?: 1,
                        month = map.int("world.news.$index.month") ?: 0,
                        category = NewsCategory.byName(map["world.news.$index.category"]) ?: NewsCategory.WORLD,
                        title = map["world.news.$index.title"].orEmpty(),
                        detail = map["world.news.$index.detail"].orEmpty(),
                        level = NewsLevel.byName(map["world.news.$index.level"]),
                        regionId = map["world.news.$index.region"].orEmpty()
                    )
                )
            }
            val npcRuntimes = LinkedHashMap<String, NpcRuntime>()
            val runtimeCount = map.int("world.npcruntime.count") ?: 0
            for (index in 0 until runtimeCount) {
                val id = map["world.npcruntime.$index.id"] ?: continue
                npcRuntimes[id] = NpcRuntime(
                    npcId = id,
                    ability = map.int("world.npcruntime.$index.ability") ?: 50,
                    merit = map.int("world.npcruntime.$index.merit") ?: 40,
                    influence = map.int("world.npcruntime.$index.influence") ?: 50,
                    positionIndex = normalizedRank(map.int("world.npcruntime.$index.position") ?: 0),
                    active = map["world.npcruntime.$index.active"] != "0",
                    lastActionMonth = map.int("world.npcruntime.$index.last") ?: -1,
                    agenda = map["world.npcruntime.$index.agenda"].orEmpty()
                )
            }
            val npcRelations = LinkedHashMap<String, Int>()
            val relCount = map.int("world.npcrel.count") ?: 0
            for (index in 0 until relCount) {
                val key = map["world.npcrel.$index.key"] ?: continue
                npcRelations[key] = map.int("world.npcrel.$index.value") ?: 0
            }
            val vacancies = ArrayList<Vacancy>()
            val vacancyCount = map.int("world.vacancy.count") ?: 0
            for (index in 0 until vacancyCount) {
                vacancies.add(
                    Vacancy(
                        positionIndex = normalizedRank(map.int("world.vacancy.$index.position") ?: 0),
                        regionId = map["world.vacancy.$index.region"].orEmpty(),
                        reason = map["world.vacancy.$index.reason"].orEmpty(),
                        createdMonth = map.int("world.vacancy.$index.created") ?: 0,
                        filled = map["world.vacancy.$index.filled"] == "1"
                    )
                )
            }
            val world = WorldState(
                economyClimate = map.int("world.economy") ?: 50,
                environmentClimate = map.int("world.environment") ?: 50,
                npcStatuses = npcStatuses,
                builtProjects = built,
                majorFlags = worldFlags,
                research = research,
                month = map.int("world.month") ?: 0,
                economy = economy,
                era = EraState.entries.firstOrNull { it.name == map["world.era"] } ?: EraState.DEVELOPMENT,
                districts = districts,
                policies = policies,
                decisions = decisions,
                news = news,
                npcs = npcRuntimes,
                npcRelations = npcRelations,
                vacancies = vacancies
            )
            val planSlots = ArrayList<WorkSlot>()
            val planCount = map.int("plan.count") ?: 0
            for (index in 0 until planCount) {
                WorkSlot.entries.firstOrNull { it.name == map["plan.$index"] }?.let { planSlots.add(it) }
            }
            val workPlan = WorkPlan(
                slots = planSlots,
                plannedQuarter = map.int("plan.quarter") ?: -1
            )
            val transferOffer = if (map["offer.present"] == "1") {
                TransferOffer(
                    regionId = map["offer.regionId"].orEmpty(),
                    reason = map["offer.reason"].orEmpty(),
                    expiresQuarter = map.int("offer.expires") ?: 0,
                    tier = map.int("offer.tier") ?: 1,
                    positionIndex = normalizedRank(map.int("offer.position") ?: 0)
                )
            } else {
                null
            }
            val outcome = if (map["outcome.present"] == "1") {
                OutcomeRecord(
                    eventId = map["outcome.eventId"].orEmpty(),
                    eventTitle = map["outcome.eventTitle"].orEmpty(),
                    choiceText = map["outcome.choiceText"].orEmpty(),
                    outcome = map["outcome.text"].orEmpty(),
                    deltas = readDeltas(map, "outcome"),
                    hiddenDeltas = readDeltas(map, "outcome.hidden"),
                    relationDeltas = readDeltas(map, "outcome.relation"),
                    governanceDeltas = readDeltas(map, "outcome.governance")
                )
            } else {
                null
            }
            GameState(
                player = player,
                year = map.requiredInt("year").coerceAtLeast(1),
                quarter = map.requiredInt("quarter").coerceIn(1, 4),
                reviewCount = map.int("reviewCount") ?: 0,
                totalWorkGained = map.int("totalWorkGained") ?: 0,
                log = log,
                reviews = reviews,
                currentEventId = map["currentEventId"].orEmpty(),
                outcome = outcome,
                triggeredEvents = triggered,
                eventCooldown = cooldown,
                flags = flags,
                pendingChains = chains,
                rngState = map["rngState"]?.toLongOrNull() ?: GameRng.DEFAULT_SEED,
                promotionCount = map.int("promotionCount") ?: 0,
                warningCount = map.int("warningCount") ?: 0,
                route = CareerRoute.byName(map["route"].orEmpty()),
                relationship = relationship,
                politics = decodedPolitics,
                career = decodedCareer,
                governance = decodedGovernance
                    ?: GovernanceEngine.initial(player.unit, player.positionIndex),
                regionId = regionId,
                projects = projects,
                world = world,
                workPlan = workPlan,
                transferOffer = transferOffer,
                reachedTopYear = (map.int("reachedTopYear") ?: -1).takeIf { it > 0 },
                demotionCount = map.int("demotionCount") ?: 0,
                careerCities = cityStints,
                ending = ending,
                // 旧存档没有该字段时默认为 false。
                pendingPromotion = map["pendingPromotion"] == "1",
                phase = when {
                    ending != null -> GamePhase.ENDING
                    // 旧存档可能缺少结算结果，回退到事件阶段，避免卡住无法操作。
                    phase == GamePhase.OUTCOME && outcome == null -> GamePhase.EVENT
                    else -> phase
                }
            )
        } catch (error: Exception) {
            null
        }
    }

    private fun readDeltas(map: Map<String, String>, prefix: String): List<Pair<String, Int>> {
        val count = intOf(map["$prefix.delta.count"]) ?: 0
        val result = ArrayList<Pair<String, Int>>(count)
        for (index in 0 until count) {
            val key = map["$prefix.delta.$index.key"] ?: continue
            val value = intOf(map["$prefix.delta.$index.value"]) ?: continue
            result.add(key to value)
        }
        return result
    }

    // 写入一组「名称 -> 数值」变化，供结算结果等结构复用。
    private fun writeDeltas(sb: StringBuilder, prefix: String, deltas: List<Pair<String, Int>>) {
        sb.append(prefix).append(".delta.count").append(SEP).append(deltas.size).append('\n')
        deltas.forEachIndexed { index, pair ->
            sb.append(prefix).append(".delta.").append(index).append(".key")
                .append(SEP).append(escape(pair.first)).append('\n')
            sb.append(prefix).append(".delta.").append(index).append(".value")
                .append(SEP).append(pair.second).append('\n')
        }
    }

    private fun readStringList(map: Map<String, String>, prefix: String): List<String> {
        val count = intOf(map["$prefix.count"]) ?: 0
        val result = ArrayList<String>(count)
        for (index in 0 until count) {
            map["$prefix.$index"]?.let { result.add(it) }
        }
        return result
    }

    private fun parse(text: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        text.split('\n').forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            if (line.isEmpty()) return@forEach
            val index = line.indexOf(SEP)
            if (index <= 0) return@forEach
            val key = line.substring(0, index)
            val value = unescape(line.substring(index + 1))
            map[key] = value
        }
        return map
    }

    private fun Map<String, String>.required(key: String): String =
        this[key] ?: throw IllegalArgumentException("缺少字段 $key")

    private fun Map<String, String>.requiredInt(key: String): Int =
        intOf(this[key]) ?: throw IllegalArgumentException("字段 $key 不是整数")

    private fun Map<String, String>.int(key: String): Int? = intOf(this[key])

    private fun intOf(value: String?): Int? = value?.toIntOrNull()

    private fun escape(value: String): String {
        val sb = StringBuilder(value.length + 8)
        value.forEach { ch ->
            when (ch) {
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                SEP -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun unescape(value: String): String {
        val sb = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val ch = value[index]
            if (ch == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append(SEP)
                    else -> sb.append(next)
                }
                index += 2
            } else {
                sb.append(ch)
                index += 1
            }
        }
        return sb.toString()
    }
}



