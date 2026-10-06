package com.shitu.app.game

import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.CityStint
import com.shitu.app.model.Indicator
import com.shitu.app.model.Jurisdiction
import com.shitu.app.model.JurisdictionLevel
import com.shitu.app.model.RegionCondition
import com.shitu.app.model.RegionProfile
import com.shitu.app.model.RegionStage
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.RegionTrait
import com.shitu.app.model.TransferOffer
import com.shitu.app.model.UnitType

/**
 * 第十二阶段：地区引擎。
 *
 * 一个地区不是换了个名字：它的产业结构、财政底子、治理难度
 * 会真实地改变玩家的初始辖区、事件倾向、项目选项与晋升难度。
 * 所有数值都是虚构的游戏模拟参数。
 */
object RegionEngine {

    fun regionOf(state: GameState): RegionProfile = RegionCatalog.findOrDefault(state.regionId)

    /** 玩家辖区名称：到县区一级才用地区本名，低层级用驻地镇名。 */
    fun nameFor(region: RegionProfile, level: JurisdictionLevel): String =
        if (level == JurisdictionLevel.COUNTY) region.name else region.seatName

    /** 由地区与辖区层级生成初始辖区：层级越高，越接近地区的整体面貌。 */
    fun jurisdictionFor(region: RegionProfile, unit: UnitType, positionIndex: Int): Jurisdiction {
        val level = JurisdictionFactory.levelFor(unit, positionIndex)
        return jurisdictionOf(region, level)
    }

    fun jurisdictionOf(region: RegionProfile, level: JurisdictionLevel): Jurisdiction {
        val base = JurisdictionFactory.profileOf(level)
        val w = when (level) {
            JurisdictionLevel.TOWNSHIP -> 42
            JurisdictionLevel.STREET -> 42
            JurisdictionLevel.KEY_TOWN -> 52
            JurisdictionLevel.COUNTY -> 62
        }
        fun mix(regionValue: Int, baseValue: Int) = (regionValue * w + baseValue * (100 - w)) / 100
        return Jurisdiction(
            name = nameFor(region, level),
            level = level,
            population = if (level == JurisdictionLevel.COUNTY) region.population else level.population,
            economy = mix(region.economy, base.economy),
            livelihood = mix(region.livelihood, base.livelihood),
            security = mix(region.security, base.security),
            education = mix(region.education, base.education),
            medical = mix(region.medical, base.medical),
            infrastructure = mix(region.infrastructure, base.infrastructure),
            environment = mix(region.environment, base.environment),
            approval = mix(region.approval, base.approval),
            officialSatisfaction = mix(region.officialSatisfaction, base.officialSatisfaction)
        )
    }

    /** 初始财政结余：受地区财政基础影响。 */
    fun initialFinance(region: RegionProfile, level: JurisdictionLevel): Int =
        level.baseFinance + region.financeBase / 5

    fun stageOf(state: GameState): RegionStage =
        RegionStage.of(state.governance.jurisdiction.development)

    fun conditionOf(state: GameState): RegionCondition = RegionCondition.of(
        pressure = state.governance.jurisdiction.pressure,
        risk = state.relationship.risk,
        approval = state.governance.jurisdiction.approval
    )

    /**
     * 生成一次调任机会。地区特色与玩家的条线/路线决定去向，
     * 保证「经济地区练项目、基层地区练群众、困难地区练担当」。
     */
    fun offerFor(state: GameState, rng: GameRng): TransferOffer? {
        val current = regionOf(state)
        val candidates = RegionCatalog.regions.filter { it.id != current.id }
        if (candidates.isEmpty()) return null
        // 优先给出与当前地区不同类型、且发展空间更大的地方，让调任有意义。
        val pool = candidates.filter { it.trait != current.trait && it.potential >= current.potential }
            .ifEmpty { candidates }
        val picked = pool[rng.nextInt(pool.size)]
        val tier = when {
            picked.potential >= 78 -> 3
            picked.potential >= 60 -> 2
            else -> 1
        }
        val reason = when (picked.trait) {
            RegionTrait.UNDERDEVELOPED -> "组织上考虑让你去困难地区挑担子"
            RegionTrait.ECONOMIC_HUB -> "上级希望在重点地区配强力量"
            RegionTrait.INDUSTRIAL -> "当地正在推进产业转型，需要懂行的人"
            RegionTrait.AGRICULTURAL -> "当地农业农村任务重，需要熟悉基层的干部"
            RegionTrait.TOURISM -> "当地要打造文旅名片，需要综合协调能力"
            RegionTrait.RESOURCE -> "当地正处在转型关口，需要稳妥的干部"
            RegionTrait.BALANCED -> "组织上考虑让你去综合地区锻炼"
        }
        return TransferOffer(
            regionId = picked.id,
            reason = reason,
            expiresQuarter = state.absoluteQuarter + 8,
            tier = tier,
            positionIndex = state.player.positionIndex
        )
    }

    /** 接受调任：换地区、按新地区重建辖区（保留部分旧成果），并留下履历。 */
    fun accept(state: GameState, offer: TransferOffer): GameState {
        // 已退休 / 退居二线后不再接受新的职位调任。
        if (!state.player.inActiveCareer) return state
        val region = RegionCatalog.findOrDefault(offer.regionId)
        val level = state.governance.jurisdiction.level
        val fresh = jurisdictionOf(region, level)
        // 换地方保留一半成果，避免一调任就归零，也避免把旧成绩原样带过去。
        var blended = fresh
        Indicator.entries.forEach { indicator ->
            val mixed = (fresh.valueOf(indicator) + state.governance.jurisdiction.valueOf(indicator)) / 2
            blended = blended.withValue(indicator, mixed)
        }
        val finance = (state.governance.finance + initialFinance(region, level)) / 2
        var next = state.copy(
            regionId = region.id,
            governance = state.governance.copy(jurisdiction = blended, finance = finance),
            transferOffer = null,
            player = state.player.copy(unit = unitFor(region, state.player.unit))
        )
        next = CareerEngine.record(
            next, CareerRecordKind.TRANSFER,
            "调任 ${region.name}",
            "${offer.reason}，组织决定调整你的任职地区。"
        )
        next = CareerEngine.gainExperience(next, 6)
        // 第十六阶段：记录一段新的城市任职履历，形成全国仕途轨迹。
        val absQuarter = next.absoluteQuarter
        val history = next.careerCities.map { if (it.isCurrent) it.copy(endAbsQuarter = absQuarter) else it } +
            CityStint(
                cityId = region.id,
                cityName = region.name,
                province = region.province,
                positionLabel = next.displayPositionTitle,
                startAbsQuarter = absQuarter
            )
        return next.copy(careerCities = history)
    }

    /** 调任后落在哪一类平台。 */
    private fun unitFor(region: RegionProfile, current: UnitType): UnitType = when (region.trait) {
        RegionTrait.UNDERDEVELOPED, RegionTrait.AGRICULTURAL ->
            if (current == UnitType.TOWN || current == UnitType.STREET) current else UnitType.TOWN
        RegionTrait.ECONOMIC_HUB -> if (current == UnitType.CITY) current else UnitType.BUSINESS
        else -> current
    }

    /** 拒绝调任：可能让上级觉得你缺乏担当，留下一点风险。 */
    fun decline(state: GameState, offer: TransferOffer): GameState {
        val region = RegionCatalog.findOrDefault(offer.regionId)
        var next = state.copy(transferOffer = null)
        next = CareerEngine.record(
            next, CareerRecordKind.SETBACK,
            "婉拒调任",
            "组织曾考虑让你去${region.name}，你选择留在原岗位。"
        )
        return next.copy(relationship = state.relationship.withRisk(state.relationship.risk + 2))
    }

    /** 地区发展指数（0～100），由辖区指标综合。 */
    fun developmentOf(state: GameState): Int = state.governance.jurisdiction.development
}

