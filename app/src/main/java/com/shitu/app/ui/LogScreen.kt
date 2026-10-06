package com.shitu.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameState
import com.shitu.app.game.PendingChain
import com.shitu.app.model.LogEntry
import com.shitu.app.model.LogKind
import com.shitu.app.ui.components.DeltaChip
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

// 日志页：按时间倒序显示每一次选择与结果。
@Composable
fun LogScreen(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val pending = state.pendingChains
    if (state.log.isEmpty() && pending.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            EmptyHint("还没有记录。做出第一个选择之后，这里会写下你的每一步。")
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (pending.isNotEmpty()) {
            item {
                SectionCard(title = "进行中的事件链", trailing = "${pending.size} 条") {
                    pending.sortedBy { it.dueQuarter }.forEach { chain ->
                        ChainRow(chain)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text(
                        text = "事件链会在之后的季度继续推进，之前的选择会影响后面的结果。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(state.log.asReversed()) { entry ->
            LogCard(entry)
        }
    }
}

@Composable
private fun ChainRow(chain: PendingChain) {
    val scheme = MaterialTheme.colorScheme
    val total = EventLibrary.stepsOf(chain.chainId).size
    val finished = (chain.nextStep - 1).coerceAtLeast(0)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = EventLibrary.chainName(chain.chainId),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "第 ${finished + 1} / $total 环",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LogCard(entry: LogEntry) {
    val scheme = MaterialTheme.colorScheme
    ShituCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = kindLabel(entry.kind),
                style = MaterialTheme.typography.labelSmall,
                color = kindColor(entry.kind)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "第 ${entry.year} 年 · 第 ${entry.quarter} 季度",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.primary
        )
        if (entry.choiceText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.choiceText,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        ThinDivider()
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = entry.outcome,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface
        )
        if (entry.deltas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            entry.deltas.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (label, delta) ->
                        DeltaChip(label = label, delta = delta, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun kindLabel(kind: LogKind): String = when (kind) {
    LogKind.EVENT -> "事件"
    LogKind.REVIEW -> "年度考核"
    LogKind.PROMOTION -> "晋升"
    LogKind.ROUTE -> "方向"
    LogKind.PROJECT -> "项目"
    LogKind.WARNING -> "提醒"
    LogKind.ENDING -> "结局"
}

@Composable
private fun kindColor(kind: LogKind) = when (kind) {
    LogKind.EVENT -> MaterialTheme.colorScheme.onSurfaceVariant
    LogKind.REVIEW -> MaterialTheme.colorScheme.primary
    LogKind.PROMOTION -> MaterialTheme.colorScheme.secondary
    LogKind.ROUTE -> MaterialTheme.colorScheme.primary
    LogKind.PROJECT -> MaterialTheme.colorScheme.secondary
    LogKind.WARNING -> MaterialTheme.colorScheme.error
    LogKind.ENDING -> MaterialTheme.colorScheme.tertiary
}

