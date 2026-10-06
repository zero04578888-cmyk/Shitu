package com.shitu.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shitu.app.model.PromotionPosition

/**
 * 第十八阶段：【任职选择】窗口。
 *
 * 玩家达到任职资格后，从这里挑选具体职务。
 *
 * 只展示客观信息：职务名称、所属城市与部门、职责简介、管辖范围、
 * 权力等级与影响力分布。不做任何「推荐 / 最佳 / 最容易晋升」的暗示，
 * 选择权完全交给玩家。
 *
 * 只要玩家已经获得任职资格，这里一定至少有一个可选职务；
 * 数据层由 [com.shitu.app.model.PositionRepository] 保证。
 */
@Composable
fun PositionPickerDialog(
    subtitle: String,
    cityLabel: String,
    options: List<PromotionPosition>,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "确认任命"
) {
    if (options.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    var selectedId by remember(options) { mutableStateOf(options.first().id) }

    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = scheme.surface,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "任职选择",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "请选择新的具体职务",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = cityLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    options.forEach { option ->
                        PositionOptionCard(
                            option = option,
                            selected = option.id == selectedId,
                            onClick = { selectedId = option.id }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onConfirm(selectedId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(confirmLabel)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "全部条件已经满足，选择职务只是决定去向，不会影响晋升结果。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 单个可选职务卡：客观信息，不做推荐排序。 */
@Composable
private fun PositionOptionCard(
    option: PromotionPosition,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (selected) "已选择" else "选择",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) scheme.primary else scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            val location = listOf(option.departmentType, option.jurisdiction)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (location.isNotEmpty()) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            if (option.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface
                )
            }
            if (option.focus.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "重点：${option.focus}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            if (option.routeLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "主要方向：${option.directionText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = "履历价值：${option.careerValueLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "权力 Lv.${option.powerLevel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                val influences = option.influenceSummary
                if (influences.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = influences.joinToString("  ") { (label, value) -> "$label $value" },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}