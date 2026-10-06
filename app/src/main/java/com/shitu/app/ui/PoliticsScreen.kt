package com.shitu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.FactionCatalog
import com.shitu.app.game.GameState
import com.shitu.app.game.NpcCatalog
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.RelationshipEngine
import com.shitu.app.model.Alliance
import com.shitu.app.model.Faction
import com.shitu.app.model.IntelItem
import com.shitu.app.model.IntelLevel
import com.shitu.app.model.PoliticalTendency
import com.shitu.app.model.isTopPosition
import com.shitu.app.model.positionAt
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

// 政治页内的分区。
private enum class PoliticsSection(val label: String) {
    FACTION("势力"),
    OPPORTUNITY("机会"),
    INTEL("情报"),
    HISTORY("履历"),
    ROUTE("路线")
}

/**
 * 第九阶段：派系格局与政治资源页面。
 *
 * 页面只展示长期积累出来的结果，真正的取舍发生在事件里：
 * 政治资源、声望、路线、情报、联盟都不是可以直接购买的货币。
 * 所有数值都是游戏模拟参数。
 */
@Composable
fun PoliticsScreen(
    state: GameState,
    onPromote: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(PoliticsSection.FACTION) }
    val scheme = MaterialTheme.colorScheme
    val politics = state.politics
    val reputation = PoliticsEngine.reputation(state)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoPair(
                    "政治资源",
                    "${politics.capital} / 100",
                    modifier = Modifier.weight(1f)
                )
                InfoPair(
                    "官场声望",
                    "$reputation · ${PoliticsEngine.reputationLabel(reputation)}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair(
                    "主导路线",
                    politics.dominantTendency.label,
                    modifier = Modifier.weight(1f)
                )
                InfoPair(
                    "官场风险",
                    "${state.relationship.risk} · ${RelationshipEngine.riskLabel(state.relationship.risk)}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "政治资源靠治理成绩、人物关系与联盟逐季积累，用于争取岗位机会与应对危机，" +
                    "不能直接买卖官职。声望由政绩、民望、人物与派系评价综合而成。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            PoliticsSection.entries.forEach { item ->
                val selected = item == section
                TextButton(
                    onClick = { section = item },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (section) {
            PoliticsSection.FACTION -> FactionSection(state)
            PoliticsSection.OPPORTUNITY -> OpportunitySection(state, onPromote)
            PoliticsSection.INTEL -> IntelSection(state)
            PoliticsSection.HISTORY -> HistorySection(state)
            PoliticsSection.ROUTE -> RouteSection(state)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FactionSection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val politics = state.politics

    SectionCard(title = "地方政治格局", trailing = "影响力占比") {
        val ordered = Faction.entries
            .map { it to politics.influenceOf(it) }
            .sortedByDescending { it.second }
        val total = ordered.sumOf { it.second }.coerceAtLeast(1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(scheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                ordered.forEach { (faction, value) ->
                    if (value <= 0) return@forEach
                    Box(
                        modifier = Modifier
                            .weight(value.toFloat())
                            .fillMaxSize()
                            .background(factionColor(faction))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        ordered.forEach { (faction, value) ->
            val percent = value * 100 / total
            Text(
                text = "${faction.label} $percent%  ·  ${FactionCatalog.infoOf(faction).ideology}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        ThinDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "格局会随着事件与你的选择缓慢变化，个人影响力不会突然压过整个局面。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "派系详情", trailing = "${Faction.entries.size} 个") {
        Faction.entries.forEach { faction ->
            val info = FactionCatalog.infoOf(faction)
            val influence = politics.influenceOf(faction)
            val relation = state.relationship.factionOf(faction)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = faction.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "关系 ${signed(relation)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = factionColor(faction)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                AttributeBar(
                    label = "影响力 ${influence}",
                    value = influence,
                    barColor = factionColor(faction)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "理念：${info.ideology}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = "关注：${info.focus.joinToString("、")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = "目标：${info.goal}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                val core = info.coreNpcId?.let { NpcCatalog.find(it) }
                if (core != null) {
                    val trust = state.relationship.trustOf(core.id)
                    Text(
                        text = "核心人物：${core.name}（${state.relationship.attitudeOf(core.id).label}·信任 $trust）",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (faction != Faction.entries.last()) ThinDivider()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "当前联盟", trailing = "${politics.activeAlliances(state.absoluteQuarter).size} 个") {
        val active = politics.activeAlliances(state.absoluteQuarter)
        if (active.isEmpty()) {
            EmptyHint("目前没有生效中的联盟。结盟会带来支持，也会带来连带风险，并且会自动到期。")
        } else {
            active.forEach { alliance ->
                AllianceRow(alliance, state)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AllianceRow(alliance: Alliance, state: GameState) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(12.dp), color = scheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = alliance.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface
            )
            val partner = alliance.npcId?.let { NpcCatalog.find(it)?.name }
            if (partner != null) {
                Text(
                    text = "合作对象：$partner",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Text(
                text = alliance.note,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            val left = (alliance.untilQuarter - state.absoluteQuarter).coerceAtLeast(0)
            Text(
                text = "剩余约 $left 个季度到期",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary
            )
        }
    }
}

/** 任职机会：只看自己的硬条件；条件满足即可立即晋升。 */
@Composable
private fun OpportunitySection(state: GameState, onPromote: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ladderIndex = state.player.positionIndex
    val top = isTopPosition(ladderIndex)
    val targetIndex = com.shitu.app.model.nextPromotionIndex(ladderIndex)
    val target = positionAt(targetIndex)
    val status = PromotionEngine.statusOf(state)
    val eligible = status.eligible
    val options = if (eligible) PromotionEngine.eligibleOptions(state) else emptyList()
    // 第十八阶段：具体职务在【任职选择】窗口里挑选。
    var showPositionPicker by remember(options) { mutableStateOf(false) }

    SectionCard(title = "下一个职级", trailing = if (top) "已到顶" else (status.nextTitle ?: target.name)) {
        if (top) {
            Text(
                text = "你已在当前梯队的最高职级。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
        } else {
            Text(
                text = "当前：${state.displayPositionTitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            AttributeBar(
                label = "晋升准备度 ${status.progressPercent}%",
                value = status.progressPercent,
                barColor = scheme.primary
            )
        }
    }

    if (!top) {
        Spacer(modifier = Modifier.height(14.dp))
        SectionCard(title = "晋升资格", trailing = if (eligible) "已具备" else "条件未满足") {
            if (eligible) {
                Text(
                    text = "✓ 已具备",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { showPositionPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("选择任职")
                }
                if (showPositionPicker) {
                    PositionPickerDialog(
                        subtitle = "你已具备${state.currentRank}任职资格。",
                        cityLabel = "${state.currentProvince} · ${state.currentCityName}",
                        options = options,
                        onConfirm = { positionId ->
                            showPositionPicker = false
                            onPromote(positionId)
                        }
                    )
                }
            } else {
                // 只列具体条件，不写任何解释性文字。
                status.requirements.forEach { requirement ->
                    val mark = if (requirement.met) "✓" else "○"
                    val color = if (requirement.met) scheme.primary else scheme.onSurfaceVariant
                    Text(
                        text = "$mark  ${requirement.label}  ${requirement.display}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "政治资源", trailing = "${state.politics.capital} / 100") {
        AttributeBar(
            label = "当前资源",
            value = state.politics.capital,
            barColor = scheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "政治资源来自治理成绩、人物关系与联盟的逐季积累，用于应对危机与把握机会，" +
                "不能买卖官职。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
}
@Composable
private fun IntelSection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val intel = state.politics.intel
        .sortedWith(compareByDescending<IntelItem> { it.level.ordinal }.thenByDescending { it.year })

    SectionCard(title = "掌握的情报", trailing = "${intel.size} 条") {
        if (intel.isEmpty()) {
            EmptyHint("手里还没有情报。留意走廊里的闲话、领导的态度和检查前的暗示——它们会给你更多选择。")
        } else {
            intel.forEach { item ->
                IntelRow(item)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "情报说明") {
        Text(
            text = "情报分为普通、重要、关键三级。关键情报往往提前透出岗位调整与专项检查，" +
                "能让你在重大抉择里多一条退路。情报只来自游戏内合法的政务与人际场景。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IntelRow(item: IntelItem) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(12.dp), color = scheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = item.level.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = intelColor(item.level)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.detail,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = "第 ${item.year} 年第 ${item.quarter} 季度获得",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistorySection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val history = state.politics.history.reversed()

    SectionCard(title = "重大经历", trailing = "${state.politics.majorChoices.size} 次重大抉择") {
        if (history.isEmpty()) {
            EmptyHint("还没有留下值得记下的经历。重大抉择会写进履历，并在以后的事件里显出分量。")
        } else {
            history.forEach { entry ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "第 ${entry.year} 年第 ${entry.quarter} 季度 · ${entry.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.choice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = entry.consequence,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RouteSection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val politics = state.politics

    SectionCard(title = "政治路线倾向", trailing = "由长期行为形成") {
        PoliticalTendency.entries.forEach { tendency ->
            AttributeBar(
                label = tendency.label,
                value = politics.tendencyOf(tendency),
                barColor = tendencyColor(tendency)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        ThinDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前倾向：${politics.dominantTendency.label}。" +
                politics.dominantTendency.description,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "声望与资源来源") {
        val faction = RelationshipEngine.bestFactionRelation(state.relationship).coerceAtLeast(0)
        val attributes = state.player.attributes
        Text(
            text = "上级信任 ${attributes.leaderTrust} · 同僚支持 ${attributes.colleagues} · " +
                "个人声望 ${attributes.reputation} · 最强派系关系 $faction · " +
                "综合政绩 ${state.governance.merit.overall} · 民望 ${state.governance.jurisdiction.approval}",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "每季度积累约 ${PoliticsEngine.capitalGain(state)} 点政治资源；" +
                "路线倾向与派系影响力也会在年度结算时自然漂移。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()

private fun factionColor(faction: Faction): Color = when (faction) {
    Faction.LOCAL -> Color(0xFF8D6E63)
    Faction.PRAGMATIC -> Color(0xFF1E88E5)
    Faction.REFORM -> Color(0xFF00897B)
    Faction.CONSERVATIVE -> Color(0xFF6D4C41)
    Faction.CORE -> Color(0xFFC62828)
    Faction.NEUTRAL -> Color(0xFF757575)
}

private fun tendencyColor(tendency: PoliticalTendency): Color = when (tendency) {
    PoliticalTendency.PRAGMATIC -> Color(0xFF1E88E5)
    PoliticalTendency.BALANCED -> Color(0xFF00897B)
    PoliticalTendency.REFORMIST -> Color(0xFFEF6C00)
    PoliticalTendency.STEADY -> Color(0xFF6D4C41)
}

private fun intelColor(level: IntelLevel): Color = when (level) {
    IntelLevel.COMMON -> Color(0xFF757575)
    IntelLevel.IMPORTANT -> Color(0xFF1E88E5)
    IntelLevel.KEY -> Color(0xFFC62828)
}

