package com.shitu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.GameState
import com.shitu.app.game.Interaction
import com.shitu.app.game.NpcCatalog
import com.shitu.app.game.PoliticalEngine
import com.shitu.app.game.RelationshipEngine
import com.shitu.app.model.Faction
import com.shitu.app.model.NpcAttitude
import com.shitu.app.model.NpcProfile
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

// 关系页：势力概览 + 主要人物列表，点击人物进入详情与互动。
@Composable
fun RelationshipScreen(
    state: GameState,
    selectedNpcId: String?,
    onSelectNpc: (String?) -> Unit,
    onInteract: (String, Interaction) -> Unit,
    message: String,
    modifier: Modifier = Modifier
) {
    val npc = selectedNpcId?.let { NpcCatalog.find(it) }
    if (npc != null) {
        NpcDetail(
            state = state,
            npc = npc,
            message = message,
            onBack = { onSelectNpc(null) },
            onInteract = { action -> onInteract(npc.id, action) },
            modifier = modifier
        )
        return
    }

    val scheme = MaterialTheme.colorScheme
    val relationship = state.relationship
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoPair(
                    "当前职务",
                    "${state.displayPositionTitle} · ${state.player.unit.label}",
                    modifier = Modifier.weight(1f)
                )
                Column {
                    Text(
                        text = "官场风险",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = "${relationship.risk} · ${RelationshipEngine.riskLabel(relationship.risk)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = riskColor(relationship.risk)
                    )
                }
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

        SectionCard(title = "势力关系", trailing = "靠拢一方会得罪另一方") {
            Faction.entries.forEach { faction ->
                val value = relationship.factionOf(faction)
                SignedBar(
                    label = faction.label,
                    value = value,
                    color = factionColor(value)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = faction.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "主要人物", trailing = "${NpcCatalog.npcs.size} 人") {
            NpcCatalog.npcs.forEach { profile ->
                NpcRow(
                    state = state,
                    profile = profile,
                    onClick = { onSelectNpc(profile.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NpcRow(
    state: GameState,
    profile: NpcProfile,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val relationship = state.relationship
    val relation = relationship.relationOf(profile.id)
    val attitude = relationship.attitudeOf(profile.id)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Tag(text = attitude.label, color = attitudeColor(attitude))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${profile.title} · ${profile.faction.label}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (relationship.isScouted(profile.id)) {
                    "关系 $relation · 信任 ${relationship.trustOf(profile.id)}" +
                        if (relationship.knowsSecret(profile.id)) " · 捏着你的旧事" else ""
                } else {
                    "态度：${attitude.label}（想看清底细可以用「观察态度」）"
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NpcDetail(
    state: GameState,
    npc: NpcProfile,
    message: String,
    onBack: () -> Unit,
    onInteract: (Interaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val relationship = state.relationship
    val attitude = relationship.attitudeOf(npc.id)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = npc.name,
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.primary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("返回列表") }
        }
        Spacer(modifier = Modifier.height(10.dp))

        ShituCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(text = attitude.label, color = attitudeColor(attitude))
                Spacer(modifier = Modifier.weight(1f))
                Tag(text = npc.tier.label, color = scheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = npc.description,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("官职", npc.title, modifier = Modifier.weight(1f))
                InfoPair("年龄", "${npc.age} 岁", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("所属势力", npc.faction.label, modifier = Modifier.weight(1f))
                InfoPair("影响力", npc.influence.toString(), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("性格", npc.personalityLabel, modifier = Modifier.weight(1f))
                InfoPair(
                    "掌握秘密",
                    if (relationship.knowsSecret(npc.id)) "有" else "无",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair(
                    "当前处境",
                    relationship.statusOf(npc.id).label,
                    modifier = Modifier.weight(1f)
                )
                InfoPair(
                    "世界动向",
                    state.world.statusOf(npc.id).label,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(10.dp))
            if (relationship.isScouted(npc.id)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoPair(
                        "关系值",
                        signed(relationship.relationOf(npc.id)),
                        modifier = Modifier.weight(1f)
                    )
                    InfoPair(
                        "信任度",
                        relationship.trustOf(npc.id).toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "态度：${attitude.label}。你还不清楚对方的真实数值，" +
                        "用「观察态度」可以摸清底细。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = buildString {
                    append(
                        if (RelationshipEngine.canHelp(relationship, npc)) {
                            "这个人愿意帮你。"
                        } else {
                            "现在还谈不上帮你。"
                        }
                    )
                    if (RelationshipEngine.canHinder(relationship, npc)) {
                        append("对方对你有些看法，打交道时多留意分寸。")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
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

        val memories = state.career.memoryOf(npc.id).reversed()
        val agenda = state.world.runtimeOf(npc.id).agenda
        SectionCard(title = "关系变化记录", trailing = "${memories.size} 条") {
            if (agenda.isNotEmpty()) {
                Text(
                    text = "最近动态：$agenda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                ThinDivider()
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (memories.isEmpty()) {
                Text(
                    text = "还没有留下长期记忆。关键场合的取舍会被这个人记住。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            } else {
                memories.take(8).forEach { memory ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(
                            text = "第 ${memory.year} 年",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                            modifier = Modifier.width(72.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = memory.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurface
                            )
                            val bias = buildString {
                                if (memory.relationBias != 0) append("关系 ${signed(memory.relationBias)}")
                                if (memory.trustBias != 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append("信任 ${signed(memory.trustBias)}")
                                }
                            }
                            if (bias.isNotEmpty()) {
                                Text(
                                    text = bias,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "互动", trailing = "每次都要花精力") {
            Interaction.entries.forEach { action ->
                val reason = PoliticalEngine.unavailabilityReason(state, npc, action)
                ActionRow(
                    action = action,
                    reason = reason,
                    onClick = { onInteract(action) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ActionRow(
    action: Interaction,
    reason: String?,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val enabled = reason == null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) scheme.surface else scheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reason ?: action.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) scheme.onSurfaceVariant else scheme.error
                )
            }
            Text(
                text = if (action.energyCost > 0) "精力 -${action.energyCost}" else "不耗精力",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SignedBar(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val fraction = ((value + 100).coerceIn(0, 200)) / 200f
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = signed(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surface, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

@Composable
private fun attitudeColor(attitude: NpcAttitude): Color {
    val scheme = MaterialTheme.colorScheme
    return when (attitude) {
        NpcAttitude.HOSTILE -> scheme.error
        NpcAttitude.COLD -> scheme.error
        NpcAttitude.NEUTRAL -> scheme.onSurfaceVariant
        NpcAttitude.FRIENDLY -> scheme.tertiary
        NpcAttitude.CLOSE -> scheme.primary
    }
}

@Composable
private fun factionColor(value: Int): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        value >= 40 -> scheme.tertiary
        value >= 10 -> scheme.primary
        value <= -30 -> scheme.error
        else -> scheme.onSurfaceVariant
    }
}

@Composable
private fun riskColor(risk: Int): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        risk >= 70 -> scheme.error
        risk >= 40 -> scheme.tertiary
        else -> scheme.onSurface
    }
}
