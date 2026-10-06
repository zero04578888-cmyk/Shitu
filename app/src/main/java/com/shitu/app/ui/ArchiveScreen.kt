package com.shitu.app.ui

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.model.EndingGalleryItem
import com.shitu.app.model.LifeRecord
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.ShituBackground
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ThinDivider

/**
 * 人生档案：结局图鉴、最近一次人生的简历式摘要，以及最近走完的若干次人生。
 */
@Composable
fun ArchiveScreen(
    hasArchive: Boolean,
    archiveLines: List<Pair<String, String>>,
    endingSummary: String,
    gallery: List<EndingGalleryItem>,
    recentLives: List<LifeRecord>,
    unlockedCount: Int,
    totalCount: Int,
    onBack: () -> Unit,
    onClearArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    ShituBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "人生档案",
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onBack) { Text("返回") }
            }
            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(
                title = "结局图鉴",
                trailing = "已解锁 $unlockedCount / $totalCount"
            ) {
                AttributeBar(
                    label = "收集进度",
                    value = if (totalCount == 0) 0 else unlockedCount * 100 / totalCount,
                    barColor = scheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))
                gallery.forEachIndexed { index, item ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ThinDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    GalleryRow(item = item)
                }
            }

            if (hasArchive) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(title = "履历") {
                    archiveLines.forEach { (label, value) ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(title = "当前人生") {
                    EmptyHint("还没有进行中的人生。\n开始一段新的仕途后，这里会记录全部履历。")
                }
            }

            if (endingSummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(title = "结局") {
                    Text(
                        text = endingSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface
                    )
                }
            }

            if (recentLives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(title = "最近的人生", trailing = "${recentLives.size} 段") {
                    recentLives.asReversed().forEachIndexed { index, record ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ThinDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = record.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Tag(text = record.endingCategory)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${record.endingTitle} · 最高任职 ${record.displayPositionLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${record.unitLabel} · 服务 ${record.serviceYears} 年 · " +
                                    "晋升 ${record.promotionCount} 次 · 考核 ${record.reviewCount} 次",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (recentLives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedButton(
                    onClick = onClearArchive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("清空人生图鉴")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GalleryRow(item: EndingGalleryItem) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (item.unlocked) item.entry.title else "？？？",
                style = MaterialTheme.typography.titleMedium,
                color = if (item.unlocked) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Tag(text = item.entry.category)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (item.unlocked) {
                "已达成 ${item.count} 次"
            } else {
                item.entry.hint
            },
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Tag(text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
