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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.GameSave
import com.shitu.app.game.PositionStatus
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.GameState
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.LifeArchive
import com.shitu.app.model.PositionRepository
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

private data class MineEntry(val tab: PlayTab, val title: String, val detail: String)

private val MINE_ENTRIES = listOf(
    MineEntry(PlayTab.REGION, "辖区与地区", "当前任职地区的面貌、发展阶段与产业特点"),
    MineEntry(PlayTab.PROJECT, "重大项目", "立项开工、推进建设、查看完工成果"),
    MineEntry(PlayTab.WORKPLAN, "工作安排", "安排本季度时段，平衡精力、成长与家庭"),
    MineEntry(PlayTab.WORLD, "世界动态", "经济周期、地区变化、人物动向与长期政策后果"),
    MineEntry(PlayTab.TRANSFER, "调任机会", "查看与接受调任安排，改变任职地区"),
    MineEntry(PlayTab.RELATION, "人物关系", "拜访、请教、汇报，维持领导信任与派系评价"),
    MineEntry(PlayTab.POLITICS, "势力与机会", "派系格局、任职机会、情报与临时联盟"),
    MineEntry(PlayTab.ATTRIBUTES, "属性面板", "查看全部可见属性与潜在压力"),
    MineEntry(PlayTab.LOG, "人生日志", "完整的事件、选择与结果记录"),
    MineEntry(PlayTab.REVIEW, "年度报告", "最近一次年度考核的明细"),
    MineEntry(PlayTab.SETTINGS, "设置与存档", "深色模式、音效开关、手动存档")
)

/**
 * 第十一阶段：二级功能入口页。
 * 底部导航只保留五个主要入口，其余功能集中到这里，避免导航栏过度拥挤。
 *
 * 第十五阶段：新增「存档与进度」卡片，展示当前存档概览，并提供
 * 继续游戏 / 重新开始 / 删除存档（二次确认）三个操作。
 */
@Composable
fun MineScreen(
    state: GameState,
    archive: LifeArchive,
    save: GameSave?,
    onOpen: (PlayTab) -> Unit,
    onReloadSave: () -> Unit,
    onRestart: () -> Unit,
    onDeleteSave: () -> Unit,
    onExit: () -> Unit,
    onPromote: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var showRestartConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { showRestartConfirm = false },
            title = { Text("重新开始一局？") },
            text = { Text("当前进度会被新的一局取代。结局图鉴与成就会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    showRestartConfirm = false
                    onRestart()
                }) { Text("重新开始") }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除存档？") },
            text = { Text("自动与手动存档都会被删除，且无法恢复。结局图鉴与成就仍会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteSave()
                }) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Text(
                text = state.player.name,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${state.displayPositionTitle} · ${state.player.unit.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("年龄", "${state.playerAge} 岁", modifier = Modifier.weight(1f))
                InfoPair("服务年限", "${state.player.serviceYears} 年", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("晋升次数", "${state.promotionCount} 次", modifier = Modifier.weight(1f))
                InfoPair("解锁成就", "${state.career.unlockedAchievements.size} 项", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("履历", "${state.career.records.size} 条", modifier = Modifier.weight(1f))
                InfoPair(
                    "状态",
                    state.retirementStatus.ifBlank { "在任" },
                    modifier = Modifier.weight(1f),
                    valueColor = if (state.retirementStatus.isNotBlank()) scheme.primary else scheme.onSurface
                )
            }
            if (archive.unlockedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "累计解锁结局 ${archive.unlockedCount} / ${com.shitu.app.model.EndingCatalog.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        PositionSection(state, onPromote)

        Spacer(modifier = Modifier.height(14.dp))

        SaveSection(
            save = save,
            onReloadSave = onReloadSave,
            onRestart = { showRestartConfirm = true },
            onDeleteSave = { showDeleteConfirm = true }
        )

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "全部功能") {
            MINE_ENTRIES.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(entry.tab) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface
                        )
                        Text(
                            text = entry.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("回到主菜单")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 存档与进度卡片：概览当前存档并提供保存相关操作。 */
@Composable
private fun SaveSection(
    save: GameSave?,
    onReloadSave: () -> Unit,
    onRestart: () -> Unit,
    onDeleteSave: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    SectionCard(title = "存档与进度") {
        if (save == null) {
            EmptyHint("还没有存档。开始新的一局后会自动保存。")
            return@SectionCard
        }
        Text(
            text = "最后保存：${formatSaveTime(save.savedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("当前官职", save.displayPositionTitle, modifier = Modifier.weight(1f))
            InfoPair("当前等级", save.levelLabel, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("当前年份", "第 ${save.year} 年 第 ${save.quarter} 季度", modifier = Modifier.weight(1f))
            InfoPair("任职地区", save.regionName, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("政绩", save.merit.toString(), modifier = Modifier.weight(1f))
            InfoPair("民望", save.approval.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("路线", save.routeLabel, modifier = Modifier.weight(1f))
            InfoPair("存档格式", "v${save.version}", modifier = Modifier.weight(1f))
        }
        if (save.startedAt > 0L) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本局开局：${formatSaveTime(save.startedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onReloadSave,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("继续游戏")
            }
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("重新开始")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "「继续游戏」会读取最近一次存档；「重新开始」进入新的角色创建。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDeleteSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text("删除存档", color = scheme.error)
        }
    }
}

private fun formatSaveTime(millis: Long): String =
    if (millis <= 0L) {
        "未知"
    } else {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
            .format(java.util.Date(millis))
    }

/** 「当前职位」卡：职级、正式职位名称、任职时间、晋升进度与条件。 */
@Composable
private fun PositionSection(state: GameState, onPromote: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val status = PromotionEngine.statusOf(state)


    val options = remember(status.ready) {
        if (status.ready) PromotionEngine.eligibleOptions(state) else emptyList()
    }
    // 第十八阶段：具体职务在【任职选择】窗口里挑选。
    var showPositionPicker by remember(options) { mutableStateOf(false) }

    SectionCard(title = "当前职位") {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.displayPositionTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.primary
                )
                Text(
                    text = "${state.currentCityName} · ${status.currentTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "权力 Lv.${status.powerLevel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = status.scopeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair(
                "任职城市",
                state.currentProvince + " · " + state.currentCityName,
                modifier = Modifier.weight(1f)
            )
            InfoPair("城市层级", state.currentCityType, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("城市含权量", state.currentCityPower.toString(), modifier = Modifier.weight(1f))
            InfoPair(
                "实际权力",
                com.shitu.app.game.PositionPowerCalculator.profileOf(state).overall.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("职位影响力", state.currentPositionNode.influence.toString(), modifier = Modifier.weight(1f))
            InfoPair("职位威望", state.currentPositionNode.prestige.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("条线", state.currentPositionNode.routeType.ifBlank { "综合行政" }, modifier = Modifier.weight(1f))
            InfoPair("权限", status.scopeLabel, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("任职时间", tenureLabel(status), modifier = Modifier.weight(1f))
            InfoPair(
                "下一职位",
                if (state.postCareer) "已退出晋升通道" else (status.nextTitle ?: "已到顶"),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.postCareer) {
            Text(
                text = if (state.player.retired) {
                    "你已办理退休，不再参与职务晋升。"
                } else {
                    "你已退居二线，不再参与职务晋升。"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary
            )
        } else if (status.top) {
            Text(
                text = "你已到达当前梯队的最高职级。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
        } else {
            Text(
                text = "晋升进度 ${status.progressPercent}%",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { status.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = scheme.primary,
                trackColor = scheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "晋升要求",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            status.requirements.forEach { requirement ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (requirement.met) "✓" else "○",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (requirement.met) scheme.primary else scheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = requirement.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = requirement.display,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (status.ready) {
                Text(
                    text = "✓ 已具备",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showPositionPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("选择任职")
                }
                if (showPositionPicker) {
                    PositionPickerDialog(
                        subtitle = "你已具备${state.currentRank}任职资格。",
                        cityLabel = state.currentProvince + " · " + state.currentCityName,
                        options = options,
                        onConfirm = { positionId ->
                            showPositionPicker = false
                            onPromote(positionId)
                        }
                    )
                }
            }
        }

        val history = state.career.records
            .filter {
                it.kind == CareerRecordKind.PROMOTION ||
                    it.kind == CareerRecordKind.TRANSFER ||
                    it.kind == CareerRecordKind.SETBACK
            }
            .takeLast(5)
            .reversed()
        if (history.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "职务变动",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            history.forEach { record ->
                val change = if (record.fromPosition.isNotBlank() && record.toPosition.isNotBlank()) {
                    "${record.displayFromPosition} → ${record.displayToPosition}"
                } else {
                    record.displayTitle
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "第 ${record.year} 年",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.width(64.dp)
                    )
                    Text(
                        text = change,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        ThinDivider()
        Spacer(modifier = Modifier.height(10.dp))
        CareerRouteSection(state)
    }
}

/** 第十九阶段：「我的仕途路线」：当前路线 + 走过的具体职务，未来显示「待选择」。 */
@Composable
private fun CareerRouteSection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val route = state.currentAppointment?.route
    val walked = state.career.positionHistory.mapNotNull { PositionRepository.optionById(it) }

    Text(
        text = "我的仕途路线",
        style = MaterialTheme.typography.labelLarge,
        color = scheme.onSurface
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "当前路线：" + (route?.label ?: "综合行政"),
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.primary
    )
    if (route != null) {
        Text(
            text = route.focus,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (walked.isEmpty()) {
        Text(
            text = "还没有具体任职记录，完成第一次晋升后即可看到自己的路线。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    } else {
        walked.forEachIndexed { index, option ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}. ${option.displayTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = option.routeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
        }
        Text(
            text = "下一步：待选择",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
    state.currentAppointment?.let { appointment ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "履历价值：" + appointment.careerValueLabel,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
}

private fun tenureLabel(status: PositionStatus): String {
    val years = status.yearsInPosition
    val months = status.monthsInPosition % 12
    return when {
        years <= 0 && months <= 0 -> "刚上任"
        years <= 0 -> "$months 个月"
        months <= 0 -> "$years 年"
        else -> "$years 年 $months 个月"
    }
}