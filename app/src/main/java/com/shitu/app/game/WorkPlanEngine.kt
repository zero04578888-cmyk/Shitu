package com.shitu.app.game

import com.shitu.app.model.Attr
import com.shitu.app.model.HiddenKey
import com.shitu.app.model.ResearchNote
import com.shitu.app.model.WorkPlan
import com.shitu.app.model.WorkSlot

/**
 * 第十二阶段：工作安排引擎。
 *
 * 每个季度有三个时段可以安排。不同安排影响精力、属性、关系、
 * 压力与调研记录，形成「时间也是一种资源」的取舍。
 * 不安排不会崩，但会白白错失成长机会。所有数值都是游戏模拟参数。
 */
object WorkPlanEngine {

    fun plan(state: GameState, slots: List<WorkSlot>): Pair<GameState, String> {
        if (slots.size != WorkPlan.SLOT_COUNT) {
            return state to "一个季度要安排 ${WorkPlan.SLOT_COUNT} 个时段。"
        }
        val plan = WorkPlan(slots = slots, plannedQuarter = state.absoluteQuarter)
        return state.copy(workPlan = plan) to "本季度的工作安排已定下。"
    }

    /** 季度结算时兑现安排。 */
    fun resolve(state: GameState): Pair<GameState, List<String>> {
        val plan = state.workPlan
        if (!plan.isPlanned) return state.copy(workPlan = WorkPlan()) to emptyList()
        val notes = mutableListOf<String>()
        var attrs = state.player.attributes
        var hidden = state.player.hidden
        var gov = state.governance
        var world = state.world

        plan.slots.forEach { slot ->
            when (slot) {
                WorkSlot.MEETING -> {
                    attrs = attrs.withValue(Attr.COORDINATION, attrs.coordination + 2)
                    attrs = attrs.withValue(Attr.COLLEAGUES, attrs.colleagues + 2)
                    hidden = hidden.withValue(HiddenKey.STRESS, hidden.stress + 2)
                    hidden = hidden.withValue(HiddenKey.NETWORK, hidden.network + 1)
                    attrs = attrs.withValue(Attr.ENERGY, attrs.energy - 2)
                }
                WorkSlot.RESEARCH -> {
                    attrs = attrs.withValue(Attr.ABILITY, attrs.ability + 1)
                    hidden = hidden.withValue(HiddenKey.OPPORTUNITY, hidden.opportunity + 2)
                    val region = RegionEngine.regionOf(state)
                    val note = ResearchNote(
                        id = "research_${region.id}",
                        label = "${region.name}基层情况",
                        detail = "通过调研摸清了${region.name}的产业结构、民生短板与风险点。",
                        year = state.year,
                        quarter = state.quarter
                    )
                    world = world.withResearch(note)
                }
                WorkSlot.MATERIAL -> {
                    attrs = attrs.withValue(Attr.WRITING, attrs.writing + 2)
                    attrs = attrs.withValue(Attr.WORK, attrs.work + 1)
                    attrs = attrs.withValue(Attr.LEADER_TRUST, attrs.leaderTrust + 1)
                }
                WorkSlot.RECEPTION -> {
                    attrs = attrs.withValue(Attr.MASSES, attrs.masses + 2)
                    gov = gov.withIndicator(com.shitu.app.model.Indicator.APPROVAL, 2)
                    attrs = attrs.withValue(Attr.ENERGY, attrs.energy - 1)
                }
                WorkSlot.STUDY -> {
                    attrs = attrs.withValue(Attr.ABILITY, attrs.ability + 2)
                    hidden = hidden.withValue(HiddenKey.OPPORTUNITY, hidden.opportunity + 1)
                }
                WorkSlot.FAMILY -> {
                    attrs = attrs.withValue(Attr.FAMILY, attrs.family + 3)
                    hidden = hidden.withValue(HiddenKey.STRESS, hidden.stress - 4)
                }
                WorkSlot.REST -> {
                    attrs = attrs.withValue(Attr.ENERGY, attrs.energy + 6)
                    attrs = attrs.withValue(Attr.HEALTH, attrs.health + 1)
                    hidden = hidden.withValue(HiddenKey.STRESS, hidden.stress - 3)
                }
            }
        }
        notes += "本季度工作安排：" + plan.slots.joinToString("、") { it.label } + "。"
        val next = state.copy(
            player = state.player.copy(attributes = attrs.normalized(), hidden = hidden.normalized()),
            governance = gov,
            world = world,
            workPlan = WorkPlan()
        )
        return next to notes
    }
}
