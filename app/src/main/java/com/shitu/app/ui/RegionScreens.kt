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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.shitu.app.game.GameState
import com.shitu.app.game.ProjectEngine
import com.shitu.app.game.RegionEngine
import com.shitu.app.model.Indicator
import com.shitu.app.model.LifeStage
import com.shitu.app.model.ProjectCatalog
import com.shitu.app.model.ProjectStatus
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.WorkPlan
import com.shitu.app.model.WorkSlot
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

/**
 * 第十二阶段：辖区与地区页面。
 * 展示当前任职地区的面貌、发展阶段与所在地的产业特点。
 */
@Composable
fun RegionScreen(state: GameState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val region = RegionEngine.regionOf(state)
    val stage = RegionEngine.stageOf(state)
    val condition = RegionEngine.conditionOf(state)
    val jurisdiction = state.governance.jurisdiction
    val lifeStage = LifeStage.of(state.player.positionIndex, state.player.serviceYears, state.player.age)
    val power = com.shitu.app.game.PositionPowerCalculator.profileOf(state)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Text(region.name, style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${region.province} · ${region.cityTypeLabel} · ${region.traitLabel} · 人口 ${region.population} 万",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "城市含权量 ${region.powerWeight} / 100",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(region.summary, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("发展阶段", stage.label, modifier = Modifier.weight(1f))
                InfoPair("治理形势", condition.label, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("治理压力", "${jurisdiction.pressure}", modifier = Modifier.weight(1f))
                InfoPair("治理难度", "${region.challenge}", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("发展潜力", "${region.potential}", modifier = Modifier.weight(1f))
                InfoPair("人生阶段", lifeStage.label, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(stage.description, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "辖区指标") {
            Indicator.entries.forEach { indicator ->
                AttributeBar(
                    label = indicator.label,
                    value = jurisdiction.valueOf(indicator),
                    barColor = scheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "城市产业与机遇") {
            val city = RegionCatalog.cityOf(region)
            Text(
                text = city.industryText,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.tertiary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "这座城市的产业决定了你能遇到什么样的事件与项目。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "产业结构") {
            AttributeBar("工业", region.industry, scheme.secondary)
            Spacer(modifier = Modifier.height(6.dp))
            AttributeBar("农业", region.agriculture, scheme.secondary)
            Spacer(modifier = Modifier.height(6.dp))
            AttributeBar("服务业", region.service, scheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            val projects = state.projects.filter { it.status == ProjectStatus.ONGOING }
            Text(
                "在建项目 ${projects.size} 个 · 已完工 ${state.world.builtProjects.size} 个",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "我的实际权力", trailing = "综合 ${power.overall} / 100") {
            Text(
                text = power.displayTitle + " · " + power.cityName,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            AttributeBar("政治影响力", power.politicalInfluence, scheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            AttributeBar("经济影响力", power.economicInfluence, scheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            AttributeBar("组织影响力", power.organizationalInfluence, scheme.secondary)
            Spacer(modifier = Modifier.height(6.dp))
            AttributeBar("资源调度能力", power.resourceControl, scheme.secondary)
            Spacer(modifier = Modifier.height(6.dp))
            AttributeBar("事件权限", power.eventAuthority, scheme.tertiary)
            Spacer(modifier = Modifier.height(6.dp))
            AttributeBar("晋升机会", power.promotionOpportunity, scheme.tertiary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "实际权力 = 职位基础 × 城市含权量 × 个人影响力 × 政治地位。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "其他城市", trailing = "共 ${RegionCatalog.regions.size} 个") {
            RegionCatalog.regions.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (item.id == region.id) "${item.name}（当前任职）" else item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item.id == region.id) scheme.primary else scheme.onSurface
                        )
                        Text(
                            "${item.province} · ${item.cityTypeLabel} · ${item.traitLabel} · 含权 ${item.powerWeight}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 第十二阶段：重大项目页面。
 */
@Composable
fun ProjectScreen(
    state: GameState,
    message: String,
    onStart: (String) -> Unit,
    onResume: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val active = state.projects.filter { it.status == ProjectStatus.ONGOING }
    val stalled = state.projects.filter { it.status == ProjectStatus.STALLED }
    val done = state.projects.filter { it.status == ProjectStatus.COMPLETED }
    val available = ProjectEngine.available(state)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        if (message.isNotEmpty()) {
            ShituCard {
                Text("操作结果", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        ShituCard {
            Text("重大项目", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "项目按季度投入，财政断档会停工；完工后才结算政绩与民望。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "财政结余 ${state.governance.finance}",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "在建项目", trailing = "${active.size} 个") {
            if (active.isEmpty() && stalled.isEmpty()) {
                EmptyHint("当前没有在建项目。")
            } else {
                active.forEach { project ->
                    val profile = ProjectCatalog.find(project.id)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(profile?.label ?: project.id, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        Text(
                            "进度 ${ProjectEngine.progressText(project)} · 已投入 ${project.invested}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                stalled.forEach { project ->
                    val profile = ProjectCatalog.find(project.id)
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${profile?.label ?: project.id}（停工）", style = MaterialTheme.typography.bodyLarge, color = scheme.error)
                        Text(
                            "原因：${project.note.ifEmpty { "财政不足" }} · 进度 ${ProjectEngine.progressText(project)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(onClick = { onResume(project.id) }) { Text("复工") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "可上项目", trailing = "${available.size} 个") {
            if (available.isEmpty()) {
                EmptyHint("暂时没有可以开工的项目。")
            } else {
                available.forEach { profile ->
                    val cost = profile.quarterlyCost
                    val boosted = ProjectEngine.isBoosted(state, profile)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(profile.label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        Text(
                            profile.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "预算 ${profile.budget} · 周期 ${profile.quarters} 季 · 每季 ${cost}" +
                                if (boosted) " · 本地区特色加成" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (boosted) scheme.tertiary else scheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = { onStart(profile.id) }) { Text("立项开工") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (done.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            SectionCard(title = "已完工", trailing = "${state.world.builtProjects.size} 个") {
                done.forEach { project ->
                    val profile = ProjectCatalog.find(project.id)
                    Text(
                        "· ${profile?.label ?: project.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 第十二阶段：调任页面。
 */
@Composable
fun TransferScreen(
    state: GameState,
    message: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val current = RegionEngine.regionOf(state)
    val offer = state.transferOffer
    val transfers = state.career.records.count { it.kind == com.shitu.app.model.CareerRecordKind.TRANSFER }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        if (message.isNotEmpty()) {
            ShituCard {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        ShituCard {
            Text("当前任职", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(current.name, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
            Text(
                "${current.province} · ${current.cityTypeLabel} · 历任调整 $transfers 次",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (offer != null) {
            val target = RegionCatalog.findOrDefault(offer.regionId)
            val expired = offer.expiresQuarter <= state.absoluteQuarter
            SectionCard(title = "调任机会") {
                Text(target.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Text(
                    "${target.province} · ${target.cityTypeLabel} · 含权 ${target.powerWeight} · 难度 ${target.challenge}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(offer.reason + "。", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (expired) "这次机会已经过期。" else "机会保留至第 ${offer.expiresQuarter / 4 + 1} 年。",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expired) scheme.error else scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("调任会改变你的辖区、收入与人际关系，也可能影响家庭。", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("接受调任") }
                    Spacer(modifier = Modifier.height(0.dp))
                    OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) { Text("婉拒") }
                }
            }
        } else {
            SectionCard(title = "调任机会") {
                EmptyHint("目前没有新的调任安排。年度考核表现较好时，组织上可能找你谈话。")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "可调任城市", trailing = "共 ${RegionCatalog.regions.size - 1} 座") {
            RegionCatalog.regions.filter { it.id != current.id }
                .sortedByDescending { it.powerWeight }
                .forEach { item ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                    Text(
                        "${item.province} · ${item.cityTypeLabel} · 含权 ${item.powerWeight} · 潜力 ${item.potential}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Text(item.summary, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 第十二阶段：工作安排页面。
 */
@Composable
fun WorkPlanScreen(
    state: GameState,
    message: String,
    onSubmit: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf(listOf<WorkSlot>()) }
    val plan = state.workPlan

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        if (message.isNotEmpty()) {
            ShituCard { Text(message, style = MaterialTheme.typography.bodyMedium, color = scheme.primary) }
            Spacer(modifier = Modifier.height(12.dp))
        }

        ShituCard {
            Text("季度工作安排", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (plan.isPlanned) {
                    "本季度已安排：" + plan.slots.joinToString("、") { it.label }
                } else {
                    "每个季度可以安排 ${WorkPlan.SLOT_COUNT} 个时段，不同安排影响精力、属性、关系与调研进度。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "已选 ${selected.size} / ${WorkPlan.SLOT_COUNT}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "可选安排") {
            WorkSlot.entries.forEach { slot ->
                val chosen = selected.count { it == slot }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (selected.size < WorkPlan.SLOT_COUNT) {
                                selected + slot
                            } else {
                                selected
                            }
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (chosen > 0) "${slot.label} ×$chosen" else slot.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (chosen > 0) scheme.primary else scheme.onSurface
                        )
                        Text(slot.description, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { selected = emptyList() },
                modifier = Modifier.weight(1f)
            ) { Text("清空") }
            Spacer(modifier = Modifier.height(0.dp))
            Button(
                onClick = { onSubmit(selected.map { it.name }); selected = emptyList() },
                enabled = selected.size == WorkPlan.SLOT_COUNT,
                modifier = Modifier.weight(1f)
            ) { Text("确定安排") }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
