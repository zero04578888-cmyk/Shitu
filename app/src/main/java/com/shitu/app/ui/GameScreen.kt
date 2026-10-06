package com.shitu.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.EventPresentation
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GameState
import com.shitu.app.model.Choice
import com.shitu.app.model.EventTier
import com.shitu.app.model.GameEvent
import com.shitu.app.model.OutcomeRecord
import com.shitu.app.ui.components.AppearBox
import com.shitu.app.ui.components.DeltaChip
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.HighlightBanner
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.StaggerAppear
import com.shitu.app.ui.components.ThinDivider

// 事件页：显示当前事件、选项，选择后显示结果与属性变化。
@Composable
fun GameScreen(
    state: GameState,
    onChoose: (Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = GameEngine.currentEvent(state)
    val outcome = state.outcome

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        when {
            outcome != null -> OutcomePanel(state = state, outcome = outcome, onContinue = onContinue)
            event != null -> EventPanel(event = event, state = state, onChoose = onChoose)
            else -> SectionCard(title = "当前事件") {
                EmptyHint("这一阶段没有需要你决定的事务。")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EventPanel(event: GameEvent, state: GameState, onChoose: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tier = EventPresentation.tierOf(event)
    AppearBox(key = event.id, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ShituCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierBadge(tier)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = scheme.surfaceVariant) {
                        Text(
                            text = event.background,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface
                )

                if (tier.isHighlighted) {
                    val npcs = EventPresentation.involvedNpcs(event)
                    if (npcs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        ThinDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "涉及人物",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            npcs.forEach { name ->
                                Surface(shape = RoundedCornerShape(8.dp), color = scheme.secondaryContainer) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                    val situation = EventPresentation.situationHints(event, state)
                    if (situation.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "当前局势",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        situation.forEach { line ->
                            Text(
                                text = "· $line",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "你的选择",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            event.choices.forEachIndexed { index, choice ->
                ChoiceCard(index = index, choice = choice, onChoose = onChoose)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun TierBadge(tier: EventTier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tier) {
        EventTier.NORMAL -> scheme.surfaceVariant to scheme.onSurfaceVariant
        EventTier.IMPORTANT -> scheme.primaryContainer to scheme.onPrimaryContainer
        EventTier.MAJOR -> scheme.secondaryContainer to scheme.onSecondaryContainer
        EventTier.CRISIS -> scheme.errorContainer to scheme.onErrorContainer
    }
    Surface(shape = RoundedCornerShape(8.dp), color = container) {
        Text(
            text = tier.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ChoiceCard(index: Int, choice: Choice, onChoose: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // 防止快速连点造成同一次选择被结算两次。
    var clicked by remember(choice) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !clicked) {
                clicked = true
                onChoose(index)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = scheme.secondaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = ('A' + index).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                // 第十五阶段之后的调整：选项只显示文字本身。
                // 策略倾向（果断/稳健/保守）、预期影响、后续连锁等提示一律不出现，
                // 具体后果由后台逻辑计算，玩家在做选择之后才会看到。
                Text(
                    text = choice.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun OutcomePanel(
    state: GameState,
    outcome: OutcomeRecord,
    onContinue: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val verdict = verdictOf(outcome)
    AppearBox(key = outcome.eventTitle + outcome.choiceText, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HighlightBanner(text = verdict, key = outcome.eventTitle)
            Spacer(modifier = Modifier.height(10.dp))
            ShituCard {
                Text(
                    text = "选择结果",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = outcome.eventTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "你的选择：${outcome.choiceText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                ThinDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = outcome.outcome,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface
                )

                DeltaSection(title = "属性变化", deltas = outcome.deltas, startIndex = 0)
                DeltaSection(title = "官场影响", deltas = outcome.relationDeltas, startIndex = outcome.deltas.size)

                if (outcome.hiddenDeltas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "有些变化不会立刻显现：" + outcome.hiddenDeltas.joinToString("、") { it.first },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                DeltaSection(
                    title = "辖区与政绩",
                    deltas = outcome.governanceDeltas,
                    startIndex = outcome.deltas.size + outcome.relationDeltas.size
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (state.quarter >= 4) "进入年度考核" else "继续下一季度",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "第 ${state.year} 年 · 第 ${state.quarter} 季度（结算中）",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun DeltaSection(title: String, deltas: List<Pair<String, Int>>, startIndex: Int) {
    if (deltas.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = scheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    deltas.chunked(2).forEachIndexed { rowIndex, row ->
        StaggerAppear(index = startIndex + rowIndex, key = "$title-$rowIndex") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, delta) ->
                    DeltaChip(label = label, delta = delta, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 结果提示：只给整体感受，不暴露具体公式。
private fun verdictOf(outcome: OutcomeRecord): String {
    val gain = outcome.deltas.filter { it.second > 0 }.sumOf { it.second }
    val loss = outcome.deltas.filter { it.second < 0 }.sumOf { it.second }
    return when {
        gain - loss >= 8 -> "进展顺利"
        loss <= -8 -> "代价不小"
        else -> "结果已产生"
    }
}
