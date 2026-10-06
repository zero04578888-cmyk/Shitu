package com.shitu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.CareerEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.game.PromotionEngine
import com.shitu.app.model.LifeStage
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

// 首页本季度的一条重点事项。
private data class FocusItem(
    val title: String,
    val detail: String,
    val action: String,
    val target: PlayTab,
    val secondary: Boolean = false,
    val urgent: Boolean = false
)

/**
 * 第十一阶段：游戏内首页。
 * 只呈现最重要的信息：身份、时间、核心状态和「本季度重点」，
 * 其余功能通过底部导航与二级页面进入，避免一屏堆满数据。
 */
@Composable
fun HomeScreen(
    state: GameState,
    onNavigate: (PlayTab) -> Unit,
    onOpenSecondary: (PlayTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val attrs = state.player.attributes
    val evaluation = CareerEngine.evaluate(state)
    val reviewScore = state.reviews.lastOrNull()?.score ?: 0
    val blockers = PromotionEngine.blockersOf(state, reviewScore)
    val focus = focusItemsOf(state, blockers)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Text(
                text = "第 ${state.year} 年 · ${dateLabel(state.quarter)}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.player.name,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.displayPositionTitle,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${state.currentCityName} · 任职 ${state.player.yearsInPosition} 年",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("当前年龄", "${state.player.age} 岁", modifier = Modifier.weight(1f))
                InfoPair(
                    "任职城市",
                    "${state.currentProvince} · ${state.currentCityName}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("辖区", state.governance.jurisdiction.name, modifier = Modifier.weight(1f))
                InfoPair("城市层级", state.currentCityType, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("服务年限", "${state.player.serviceYears} 年", modifier = Modifier.weight(1f))
                InfoPair("发展路线", state.route?.label ?: "未确定", modifier = Modifier.weight(1f))
            }
            if (state.retirementStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoPair(
                    "状态",
                    state.retirementStatus,
                    modifier = Modifier.fillMaxWidth(),
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                val stage = LifeStage.of(
                    state.player.positionIndex, state.player.serviceYears, state.player.age
                )
                InfoPair("当前阶段", stage.label, modifier = Modifier.weight(1f))
                InfoPair(
                    "本季度行动点",
                    "${state.governance.actionsLeft} / ${state.governance.actionPoints}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            InfoPair(
                "当前主要目标",
                focus.firstOrNull()?.title ?: "按部就班把手上的事做扎实"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "核心状态", trailing = "本季度") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("政绩", state.governance.merit.overall, modifier = Modifier.weight(1f))
                StatTile("民望", state.governance.jurisdiction.approval, modifier = Modifier.weight(1f))
                StatTile("威望", PoliticsEngine.reputation(state), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("能力", state.career.overallAbility, modifier = Modifier.weight(1f))
                StatTile("政治资源", state.politics.capital, modifier = Modifier.weight(1f))
                StatTile(
                    label = "风险",
                    value = state.relationship.risk,
                    modifier = Modifier.weight(1f),
                    lowerIsBetter = true
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("精力", attrs.energy, modifier = Modifier.weight(1f))
                StatTile("财政", state.governance.finance, modifier = Modifier.weight(1f), valueSuffix = "万")
                StatTile("家庭", attrs.family, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "人生评价 ${evaluation.overall} · ${evaluation.rating}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "本季度重点", trailing = if (focus.isEmpty()) "暂无" else "${focus.size} 项") {
            if (focus.isEmpty()) {
                EmptyHint("暂时没有必须处理的事项。你可以在「事件」里推进一个季度。")
            } else {
                focus.forEach { item ->
                    FocusRow(item = item) {
                        if (item.secondary) onOpenSecondary(item.target) else onNavigate(item.target)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "快捷入口") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickButton("处理政务", Modifier.weight(1f)) { onNavigate(PlayTab.GOVERNANCE) }
                QuickButton("查看事件", Modifier.weight(1f)) { onNavigate(PlayTab.EVENT) }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickButton("人物互动", Modifier.weight(1f)) { onOpenSecondary(PlayTab.RELATION) }
                QuickButton("晋升进度", Modifier.weight(1f)) { onNavigate(PlayTab.CAREER) }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickButton("世界动态", Modifier.weight(1f)) { onOpenSecondary(PlayTab.WORLD) }
                QuickButton("辖区地区", Modifier.weight(1f)) { onOpenSecondary(PlayTab.REGION) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun dateLabel(quarter: Int): String = "第 ${quarter} 季度"

private fun focusItemsOf(state: GameState, blockers: List<String>): List<FocusItem> {
    val items = mutableListOf<FocusItem>()
    val gov = state.governance
    val attrs = state.player.attributes

    state.politics.crisis?.let { crisis ->
        items += FocusItem(
            title = "政治危机：${crisis.title}",
            detail = "严重程度 ${crisis.severity}，需要尽快处理。",
            action = "去处理",
            target = PlayTab.POLITICS,
            secondary = true,
            urgent = true
        )
    }
    if (state.phase == GamePhase.EVENT && state.currentEventId.isNotEmpty()) {
        items += FocusItem(
            "有待处理的事件",
            "本季度还有一件事需要你拍板。",
            "去处理",
            PlayTab.EVENT,
            urgent = true
        )
    }
    if (state.phase == GamePhase.OUTCOME) {
        items += FocusItem(
            "上一件事的结果待确认",
            "结果已经结算，确认后进入下一季度。",
            "去看结果",
            PlayTab.EVENT,
            urgent = true
        )
    }
    if (gov.actionsLeft > 0) {
        items += FocusItem(
            "政务行动点未用完",
            "本季度还剩 ${gov.actionsLeft} 点行动点，可用于改善辖区。",
            "去政务",
            PlayTab.GOVERNANCE
        )
    }
    if (state.quarter == 4) {
        items += FocusItem(
            "本季度结束后年度考核",
            "考核结果会影响晋升，注意平衡政绩、民望与风险。",
            "看生涯",
            PlayTab.CAREER
        )
    }
    items += if (blockers.isEmpty()) {
        FocusItem(
            "晋升条件已具备",
            "各项硬条件已经达标，可以立即选择任职。",
            "看生涯",
            PlayTab.CAREER
        )
    } else {
        // 只列具体还差的条件，不写解释性长句。
        val gaps = PromotionEngine.requirementList(state)
            .filterNot { it.met }
            .take(2)
            .joinToString("；") { "${it.label} ${it.display}" }
            .ifBlank { "条件未满足" }
        FocusItem(
            "晋升条件未满足",
            "$gaps。",
            "看生涯",
            PlayTab.CAREER
        )
    }
    if (attrs.energy <= 25) {
        items += FocusItem(
            "精力偏低",
            "精力不足会拖慢工作。可以在政务的「个人」里安排休息。",
            "去调整",
            PlayTab.GOVERNANCE,
            urgent = true
        )
    }
    if (gov.jurisdiction.approval <= 45) {
        items += FocusItem(
            "民望偏低",
            "群众意见正在积累，处理民生与信访有助于挽回。",
            "去政务",
            PlayTab.GOVERNANCE
        )
    }
    return items.take(4)
}

@Composable
private fun StatTile(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    lowerIsBetter: Boolean = false,
    valueSuffix: String = ""
) {
    val scheme = MaterialTheme.colorScheme
    val warning = if (lowerIsBetter) value >= 60 else value <= 35
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$value$valueSuffix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (warning) scheme.error else scheme.onSurface
            )
        }
    }
}

@Composable
private fun FocusRow(item: FocusItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (item.urgent) scheme.errorContainer else scheme.primaryContainer
        ) {
            Text(
                text = if (item.urgent) "急" else "办",
                style = MaterialTheme.typography.labelSmall,
                color = if (item.urgent) scheme.onErrorContainer else scheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.action,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = item.detail,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
