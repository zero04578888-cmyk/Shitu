package com.shitu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shitu.app.game.GameState
import com.shitu.app.game.GovernanceCatalog
import com.shitu.app.game.GovernanceEngine
import com.shitu.app.model.GovernanceAction
import com.shitu.app.model.GovernanceCategory
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

// 政务页：辖区状态 + 政绩 + 政务大厅。
@Composable
fun GovernanceScreen(
    state: GameState,
    message: String,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val gov = state.governance
    val jurisdiction = gov.jurisdiction

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoPair(
                    "当前辖区",
                    "${jurisdiction.name} · ${jurisdiction.levelLabel}",
                    modifier = Modifier.weight(1f)
                )
                InfoPair(
                    "人口",
                    "${jurisdiction.population} 万",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair(
                    "财政结余",
                    "${gov.finance} 万",
                    modifier = Modifier.weight(1f)
                )
                InfoPair(
                    "行动点",
                    "${gov.actionsLeft} / ${gov.actionPoints}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair(
                    "季度收入",
                    "${GovernanceEngine.quarterlyIncome(state)} 万",
                    modifier = Modifier.weight(1f)
                )
                InfoPair(
                    "季度支出",
                    "${GovernanceEngine.quarterlyExpense(state)} 万",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("季度工资", "${gov.salaryPerQuarter} 万", modifier = Modifier.weight(1f))
                InfoPair("个人存款", "${gov.savings} 万", modifier = Modifier.weight(1f))
            }
            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                ThinDivider()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "施政成绩", trailing = "综合 ${gov.merit.overall} 分") {
            MeritKind.entries.forEach { kind ->
                AttributeBar(
                    label = kind.label,
                    value = gov.merit.valueOf(kind),
                    barColor = scheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            ThinDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "综合政绩由五项加权得出，任何一项落后都会拉低总分。" +
                    "当前最弱的一项：${gov.merit.weakest.label}。政绩每年自然衰减，需要持续做事。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(
            title = "辖区治理",
            trailing = "治理压力 ${jurisdiction.pressure}"
        ) {
            Indicator.entries.forEach { indicator ->
                AttributeBar(
                    label = indicator.label,
                    value = jurisdiction.valueOf(indicator),
                    barColor = indicatorColor(indicator)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            ThinDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "民望：${GovernanceEngine.approvalLabel(jurisdiction.approval)}。" +
                    "民望低会招来投诉与舆情，民望高则更容易做成事。发展水平 " +
                    "${jurisdiction.development}。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(
            title = "政务大厅",
            trailing = "本季度剩余 ${gov.actionsLeft} 次"
        ) {
            Text(
                text = "每做一件事都花财政、精力和一次行动点。" +
                    "财政不足时，很多事情只能先放下。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            GovernanceCategory.entries.forEach { category ->
                val actions = GovernanceCatalog.byCategory(category)
                if (actions.isEmpty()) return@forEach
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                actions.forEach { action ->
                    val reason = GovernanceEngine.unavailabilityReason(state, action)
                    GovernanceActionRow(
                        action = action,
                        reason = reason,
                        onClick = { onAction(action.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GovernanceActionRow(
    action: GovernanceAction,
    reason: String?,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val enabled = reason == null
    var clicked by remember(action.id, reason) { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !clicked) {
                clicked = true
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) scheme.surfaceVariant else scheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = costLabel(action),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (enabled) action.description else reason ?: action.description,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) scheme.onSurfaceVariant else scheme.error
            )
        }
    }
}

private fun costLabel(action: GovernanceAction): String {
    val parts = mutableListOf<String>()
    if (action.financeCost > 0) parts += "财政 -${action.financeCost}"
    if (action.savingsCost > 0) parts += "存款 -${action.savingsCost}"
    if (action.energyCost > 0) parts += "精力 -${action.energyCost}"
    if (parts.isEmpty()) parts += "行动点 -1"
    return parts.joinToString(" · ")
}

@Composable
private fun indicatorColor(indicator: Indicator): Color {
    val scheme = MaterialTheme.colorScheme
    return when (indicator) {
        Indicator.ECONOMY -> scheme.primary
        Indicator.LIVELIHOOD, Indicator.APPROVAL -> scheme.tertiary
        Indicator.SECURITY -> scheme.secondary
        Indicator.EDUCATION, Indicator.MEDICAL -> scheme.secondary
        Indicator.INFRASTRUCTURE -> scheme.primary
        Indicator.ENVIRONMENT -> scheme.tertiary
        Indicator.OFFICIAL_SATISFACTION -> scheme.onSurfaceVariant
    }
}
