package com.shitu.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.GameState
import com.shitu.app.game.ReviewExplainer
import com.shitu.app.model.AnnualReview
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.ui.components.AnimatedNumber
import com.shitu.app.ui.components.AppearBox
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.HighlightBanner
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider
import com.shitu.app.ui.theme.Amber
import com.shitu.app.ui.theme.Crimson
import com.shitu.app.ui.theme.Jade

/**
 * 年度报告：综合评分、主要贡献与风险、晋升资格、各项明细。
 * 第十四阶段：让玩家看得懂「为什么是这个结果」，同时不暴露具体公式。
 */
@Composable
fun ReviewScreen(
    state: GameState,
    review: AnnualReview,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    /** 第十八阶段：年度考核已通过晋升，等待玩家选择具体职务。 */
    pendingPromotion: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    var clicked by remember(review.year) { mutableStateOf(false) }
    val explanation = remember(state.year, review.score) { ReviewExplainer.of(state, review) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Text(
                text = "第 ${review.year} 年 年度考核",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedNumber(
                    value = review.score,
                    style = MaterialTheme.typography.displayLarge,
                    color = scoreColor(review.score)
                )
                Spacer(modifier = Modifier.fillMaxWidth(0.02f))
                Text(
                    text = review.grade,
                    style = MaterialTheme.typography.headlineMedium,
                    color = scoreColor(review.score),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = review.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface
            )
        }

        if (explanation.contributions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            SectionCard(title = "主要贡献") {
                explanation.contributions.forEach { reason ->
                    ReasonRow(reason.label, reason.detail, Jade)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        if (explanation.risks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            SectionCard(title = "主要风险") {
                explanation.risks.forEach { reason ->
                    ReasonRow(reason.label, reason.detail, Crimson)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        SectionCard(
            title = "晋升资格",
            trailing = explanation.opportunityLevel
        ) {
            Text(
                text = explanation.opportunityLine,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (explanation.readyForPromotion) {
                Text(
                    text = "✓ 已具备",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Jade
                )
            } else {
                // 只列真实条件，不做任何解释。
                explanation.promotionConditions.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        if (review.promotionGranted) {
            Spacer(modifier = Modifier.height(14.dp))
            ShituCard {
                if (review.promotionGranted) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = scheme.secondaryContainer
                    ) {
                        Text(
                            text = if (pendingPromotion) "晋升资格" else "晋升",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val lastPromotion = state.career.records.lastOrNull {
                        it.kind == CareerRecordKind.PROMOTION
                    }
                    if (!pendingPromotion && lastPromotion != null && lastPromotion.fromPosition.isNotBlank()) {
                        AppearBox(key = "promotion-${review.year}") {
                            Column {
                                Text(
                                    text = lastPromotion.fromPosition,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurfaceVariant
                                )
                                Text(
                                    text = "↓",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = scheme.primary
                                )
                                Text(
                                    text = lastPromotion.toPosition,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = scheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "新职位已解锁，可以处理更高级别的事务。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        HighlightBanner(
                            text = if (pendingPromotion) "已具备晋升资格" else "职级发生变化",
                            key = "promotion-${review.year}"
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (!pendingPromotion) {
                    Text(
                        text = explanation.resultLine,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        SectionCard(title = "评价明细") {
            review.items.forEach { item ->
                AttributeBar(
                    label = "${item.label} · ${item.comment}",
                    value = item.score,
                    barColor = scoreColor(item.score)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = "综合评分由游戏内部模拟权重计算，仅用于玩法平衡。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        ThinDivider()
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = {
                if (!clicked) {
                    clicked = true
                    onContinue()
                }
            },
            enabled = !clicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (pendingPromotion) "选择新的具体职务" else "进入下一年度",
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "新的一年在等着你",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ReasonRow(label: String, detail: String, color: Color) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun scoreColor(score: Int) = when {
    score >= 80 -> Jade
    score >= 65 -> MaterialTheme.colorScheme.primary
    score >= 55 -> Amber
    else -> Crimson
}
