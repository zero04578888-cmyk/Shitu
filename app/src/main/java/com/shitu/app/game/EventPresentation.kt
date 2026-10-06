package com.shitu.app.game

import com.shitu.app.model.EventTier
import com.shitu.app.model.GameEvent

/**
 * 事件演出所需的展示信息。
 *
 * 只保留不会泄露选择结果的内容：事件等级、涉及人物、当前局势。
 * 选项的策略倾向、预期影响与潜在风险已在展示层彻底移除，这里也不再提供
 * 任何用于「选择前预览后果」的辅助函数，避免被误用。
 */
object EventPresentation {

    fun tierOf(event: GameEvent): EventTier = event.effectiveTier

    /** 事件中可能出现的关键人物（只取事件里明确提到关系的对象）。 */
    fun involvedNpcs(event: GameEvent): List<String> {
        val ids = LinkedHashSet<String>()
        event.politicalGate.minNpcRelation.keys.forEach { ids += it }
        event.choices.forEach { choice ->
            ids += choice.political.relations.keys
            ids += choice.political.trust.keys
        }
        return ids.mapNotNull { NpcCatalog.find(it)?.name }.distinct()
    }

    /** 当前局势：结合辖区与自身状态，给玩家一个「为什么现在遇到这件事」的提示。 */
    fun situationHints(event: GameEvent, state: GameState): List<String> {
        val hints = mutableListOf<String>()
        val jurisdiction = state.governance.jurisdiction
        event.governanceGate.indicators.forEach { gate ->
            val current = jurisdiction.valueOf(gate.indicator)
            gate.min?.let { hints += "辖区${gate.indicator.label}当前 $current，需要不低于 $it 的条件" }
            gate.max?.let { hints += "辖区${gate.indicator.label}当前 $current，需要不高于 $it 的条件" }
        }
        event.governanceGate.minFinance?.let { hints += "辖区财政偏紧，需要至少 $it 万才能推进" }
        val attrs = state.player.attributes
        if (attrs.integrityRisk >= 45) hints += "你的廉政风险已经偏高，这类事容易被放大"
        if (attrs.family <= 40) hints += "家庭关系长期偏低，工作与生活正在互相牵扯"
        if (state.relationship.risk >= 45) hints += "官场风险不低，处理方式会被更多人盯着"
        if (jurisdiction.approval <= 45) hints += "辖区民望偏低，群众情绪需要安抚"
        if (jurisdiction.security <= 45) hints += "辖区治安基础偏弱，容易出突发情况"
        return hints.distinct().take(3)
    }
}