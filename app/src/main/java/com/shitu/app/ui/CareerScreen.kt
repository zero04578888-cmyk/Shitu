package com.shitu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.shitu.app.game.CareerEngine
import com.shitu.app.game.GovernanceEngine
import com.shitu.app.game.GameState
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.model.AchievementCatalog
import com.shitu.app.model.CareerAbility
import com.shitu.app.model.CareerRecord
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.EndingCatalog
import com.shitu.app.model.LifeArchive
import com.shitu.app.model.NpcAttitude
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

private enum class CareerSection(val label: String) {
    GROWTH("成长"),
    FAMILY("家庭"),
    ASSETS("资产"),
    RESUME("履历"),
    STATS("统计"),
    ACHIEVEMENTS("成就"),
    ENDINGS("结局")
}

/**
 * 第十阶段：仕途人生页面。个人成长、家庭、资产、履历、统计、成就与结局图鉴。
 * 数据全部来自存档与人生档案，未解锁的结局与成就不提前透露条件。
 */
@Composable
fun CareerScreen(
    state: GameState,
    archive: LifeArchive,
    modifier: Modifier = Modifier
) {
    var section by remember { mutableStateOf(CareerSection.GROWTH) }
    val scheme = MaterialTheme.colorScheme
    val career = state.career
    val evaluation = CareerEngine.evaluate(state)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoPair("当前年龄", "${state.player.age} 岁", modifier = Modifier.weight(1f))
                InfoPair("入仕年龄", "${state.player.ageAtStart} 岁", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("任职年限", "${state.player.yearsInPosition} 年", modifier = Modifier.weight(1f))
                InfoPair("总工作年限", "${state.player.serviceYears} 年", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("成长等级", "Lv.${career.level}", modifier = Modifier.weight(1f))
                InfoPair("历任岗位", "${career.positionsHeld} 个", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "人生评价 ${evaluation.overall} · ${evaluation.rating}，" +
                    "最强 ${evaluation.strongest.label}，最弱 ${evaluation.weakest.label}。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            CareerSection.entries.forEach { item ->
                val selected = item == section
                TextButton(onClick = { section = item }) {
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
            CareerSection.GROWTH -> GrowthSection(state)
            CareerSection.FAMILY -> FamilySection(state)
            CareerSection.ASSETS -> AssetsSection(state)
            CareerSection.RESUME -> ResumeSection(state)
            CareerSection.STATS -> StatsSection(state)
            CareerSection.ACHIEVEMENTS -> AchievementsSection(state, archive)
            CareerSection.ENDINGS -> EndingsSection(archive)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GrowthSection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val career = state.career

    SectionCard(title = "个人成长", trailing = "Lv.${career.level} / ${com.shitu.app.model.CareerProfile.MAX_LEVEL}") {
        val fraction = if (career.level >= com.shitu.app.model.CareerProfile.MAX_LEVEL) {
            com.shitu.app.model.CareerProfile.EXPERIENCE_PER_LEVEL
        } else {
            career.experienceInLevel
        }
        AttributeBar(
            label = "经验",
            value = fraction * 100 / com.shitu.app.model.CareerProfile.EXPERIENCE_PER_LEVEL,
            barColor = scheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (career.experienceToNext == 0) {
                "综合能力已经达到上限。"
            } else {
                "距离下一级还需 ${career.experienceToNext} 点经验。经验来自完成任务、治理辖区、处理突发事件与年度考核。"
            },
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        ThinDivider()
        Spacer(modifier = Modifier.height(12.dp))
        CareerAbility.entries.forEach { ability ->
            AttributeBar(
                label = ability.label,
                value = career.valueOf(ability),
                barColor = abilityColor(ability)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ability.description,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        ThinDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "综合能力 ${career.overallAbility}。能力成长有上限，主要靠长期完成任务而不是一次选择。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "最近成长", trailing = "${career.records.size} 条履历") {
        val recent = career.recentRecords(5)
        if (recent.isEmpty()) {
            EmptyHint("还没有成长记录。处理政务、完成任务后，履历会一点点厚起来。")
        } else {
            recent.forEach { record ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "第 ${record.year} 年",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary,
                        modifier = Modifier.width(64.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.displayTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface
                        )
                        Text(
                            text = record.kind.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FamilySection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val attrs = state.player.attributes
    val hidden = state.player.hidden
    val family = state.player.family

    SectionCard(title = "家庭", trailing = family.summary) {
        AttributeBar(label = "家庭关系", value = attrs.family, barColor = scheme.tertiary)
        Spacer(modifier = Modifier.height(10.dp))
        AttributeBar(label = "健康", value = attrs.health, barColor = scheme.secondary)
        Spacer(modifier = Modifier.height(10.dp))
        AttributeBar(label = "精力", value = attrs.energy, barColor = scheme.primary)
        Spacer(modifier = Modifier.height(10.dp))
        AttributeBar(label = "工作压力", value = hidden.stress, barColor = scheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "家庭关系影响压力恢复；长期高强度工作、异地任职与重大人生选择都会让它变化。" +
                "在「政务 · 个人」里可以安排家庭支出、休息与通勤改善。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "生活状态") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("婚姻", if (family.hasPartner) "已婚" else "未婚", modifier = Modifier.weight(1f))
            InfoPair("子女", "${family.children} 人", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("父母", if (family.hasParents) "健在" else "年迈", modifier = Modifier.weight(1f))
            InfoPair(
                "压力状态",
                stressLabel(hidden.stress),
                modifier = Modifier.weight(1f),
                valueColor = if (hidden.stress >= 70) scheme.error else scheme.onSurface
            )
        }
    }
}

@Composable
private fun AssetsSection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val gov = state.governance
    val assets = state.career.assets

    SectionCard(title = "个人资产", trailing = "全部来自合法收入") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("季度工资", "${gov.salaryPerQuarter} 万", modifier = Modifier.weight(1f))
            InfoPair("个人存款", "${gov.savings} 万", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("住房", assets.housingLabel, modifier = Modifier.weight(1f))
            InfoPair("出行", assets.vehicleLabel, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "资产只用于增强沉浸感：改善住房、通勤和家庭生活。工资与奖金是唯一来源，" +
                "不构成任何现实建议。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "收支概览") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("辖区财政", "${gov.finance} 万", modifier = Modifier.weight(1f))
            InfoPair("季度收入", "${GovernanceEngine.quarterlyIncome(state)} 万", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("季度支出", "${GovernanceEngine.quarterlyExpense(state)} 万", modifier = Modifier.weight(1f))
            InfoPair("行动点", "${gov.actionsLeft} / ${gov.actionPoints}", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResumeSection(state: GameState) {
    val scheme = MaterialTheme.colorScheme
    val records = state.career.records.reversed()

    SectionCard(title = "全国仕途履历", trailing = "共 ${state.careerCities.size} 地") {
        if (state.careerCities.isEmpty()) {
            EmptyHint("还没有任职城市记录。")
        } else {
            state.careerCities.forEachIndexed { index, stint ->
                val end = if (stint.isCurrent) state.absoluteQuarter else stint.endAbsQuarter
                val quarters = (end - stint.startAbsQuarter).coerceAtLeast(0)
                val years = quarters / 4
                val months = (quarters % 4) * 3
                val tenure = when {
                    years <= 0 && months <= 0 -> "刚任职"
                    years <= 0 -> "${months} 个月"
                    months <= 0 -> "${years} 年"
                    else -> "${years} 年 ${months} 个月"
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stint.province + " · " + stint.cityName + if (stint.isCurrent) "（现职）" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (stint.isCurrent) scheme.primary else scheme.onSurface
                        )
                        Text(
                            text = (if (stint.isCurrent) state.displayPositionTitle else stint.displayPositionLabel) + " · 任职 " + tenure,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                if (index < state.careerCities.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("↓", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "我的履历", trailing = "${records.size} 条") {
        if (records.isEmpty()) {
            EmptyHint("履历还是空白的。入职、晋升、重要任务与重大抉择都会记在这里。")
        } else {
            var lastYear = -1
            records.forEach { record ->
                if (record.year != lastYear) {
                    lastYear = record.year
                    Text(
                        text = "${record.year} 年",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                ResumeTimelineRow(record)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "人物记忆", trailing = "${state.career.memories.size} 条") {
        val memories = state.career.memories.reversed().take(10)
        if (memories.isEmpty()) {
            EmptyHint("还没有人把你的事记在心里。关键场合的取舍会留下长期印象。")
        } else {
            memories.forEach { memory ->
                val name = com.shitu.app.game.NpcCatalog.find(memory.npcId)?.name ?: memory.npcId
                Text(
                    text = "第 ${memory.year} 年 · $name",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary
                )
                Text(
                    text = memory.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ResumeTimelineRow(record: CareerRecord) {
    val scheme = MaterialTheme.colorScheme
    val important = record.kind in IMPORTANT_RECORD_KINDS
    val nodeColor = when (record.kind) {
        CareerRecordKind.HONOR -> scheme.tertiary
        CareerRecordKind.SETBACK -> scheme.error
        CareerRecordKind.PROMOTION, CareerRecordKind.TRANSFER -> scheme.primary
        CareerRecordKind.ACHIEVEMENT -> scheme.secondary
        else -> scheme.outline
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(scheme.outline.copy(alpha = 0.35f))
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(if (important) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (important) scheme.primaryContainer.copy(alpha = 0.35f) else scheme.surfaceVariant,
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.displayTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (important) FontWeight.SemiBold else FontWeight.Normal,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = record.kind.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (important) scheme.primary else scheme.onSurfaceVariant
                    )
                }
                if (record.detail.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = record.displayDetail,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                val meta = buildString {
                    append(record.age).append(" 岁")
                    val positionLabel = record.displayPositionLabel
                    if (positionLabel.isNotEmpty()) {
                        append(" · 时任 ").append(positionLabel)
                    }
                    if (record.regionName.isNotEmpty()) append(" · ").append(record.regionName)
                    append(" · 第 ").append(record.quarter).append(" 季度")
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

private val IMPORTANT_RECORD_KINDS = setOf(
    CareerRecordKind.PROMOTION,
    CareerRecordKind.TRANSFER,
    CareerRecordKind.PROJECT,
    CareerRecordKind.HONOR,
    CareerRecordKind.SETBACK,
    CareerRecordKind.CHOICE,
    CareerRecordKind.FAMILY
)

@Composable
private fun StatsSection(state: GameState) {
    val gov = state.governance
    val reviews = state.reviews
    val peakMerit = reviews.maxOfOrNull { r -> r.items.firstOrNull { it.label == "综合政绩" }?.score ?: 0 }
        ?: gov.merit.overall
    val peakApproval = reviews.maxOfOrNull { r -> r.items.firstOrNull { it.label == "民望" }?.score ?: 0 }
        ?: gov.jurisdiction.approval
    val transfers = state.career.records.count { it.kind == CareerRecordKind.TRANSFER }

    SectionCard(title = "人生统计") {
        StatRow("总工作年数", "${state.player.serviceYears} 年")
        StatRow("经历职位数", "${state.career.positionsHeld} 个")
        StatRow("管理地区数", "${transfers + 1} 个")
        StatRow("完成任务数", state.totalWorkGained.toString())
        StatRow("处理事件数", state.triggeredEvents.size.toString())
        StatRow("重大决策数", state.politics.majorChoices.size.toString())
        StatRow("优秀考核次数", reviews.count { it.score >= 85 }.toString())
        StatRow("最高政绩（考核记录）", peakMerit.toString())
        StatRow("最高民望（考核记录）", peakApproval.toString())
        StatRow("当前声望", PoliticsEngine.reputation(state).toString())
        StatRow("晋升次数", state.promotionCount.toString())
        StatRow("历年考核", "${state.reviewCount} 次")
        val ending = state.ending
        StatRow("最终结局", ending?.title ?: "尚未结束")
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun AchievementsSection(state: GameState, archive: LifeArchive) {
    val scheme = MaterialTheme.colorScheme
    val unlocked = archive.achievements + state.career.unlockedAchievements
    val items = AchievementCatalog.itemsOf(unlocked)
    // 解锁时间来自履历里的成就记录，旧存档没有记录时显示为空白。
    val unlockedAt = state.career.records
        .filter { it.kind == CareerRecordKind.ACHIEVEMENT }
        .associate { it.title to "第 ${it.year} 年 第 ${it.quarter} 季度" }

    SectionCard(
        title = "成就",
        trailing = "已解锁 ${items.count { it.unlocked }} / ${AchievementCatalog.total}"
    ) {
        AttributeBar(
            label = "完成度",
            value = if (AchievementCatalog.total == 0) 0 else items.count { it.unlocked } * 100 / AchievementCatalog.total,
            barColor = scheme.primary
        )
        Spacer(modifier = Modifier.height(14.dp))
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (item.unlocked) scheme.secondaryContainer else scheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (item.unlocked) item.def.title else "？？？",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (item.unlocked) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (item.unlocked) {
                                unlockedAt[item.def.title] ?: "已解锁"
                            } else {
                                "未解锁"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.unlocked) scheme.primary else scheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.unlocked) item.def.description else "满足条件后自动解锁。",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EndingsSection(archive: LifeArchive) {
    val scheme = MaterialTheme.colorScheme
    val gallery = EndingCatalog.galleryOf(archive)

    SectionCard(
        title = "结局图鉴",
        trailing = "已解锁 ${archive.unlockedCount} / ${EndingCatalog.total}"
    ) {
        Text(
            text = "你的选择已经改变了这个世界。未解锁的结局以「？？？」显示，达成条件不会提前公开。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        gallery.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                ThinDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (item.unlocked) item.entry.title else "？？？",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (item.unlocked) scheme.onSurface else scheme.onSurfaceVariant
                    )
                    Text(
                        text = if (item.unlocked) item.entry.category + " · 已达成 ${item.count} 次" else item.entry.hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                if (item.unlocked) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.primary
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    SectionCard(title = "已发现") {
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoPair("已认识人物", "${archive.discoveredNpcs.size} 人", modifier = Modifier.weight(1f))
            InfoPair("已走过路线", "${archive.discoveredRoutes.size} 条", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "多周目会保留结局图鉴、成就、已发现人物与路线。解锁 5 个不同结局可以再解锁一项隐藏成就。",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
    }
}

private fun abilityColor(ability: CareerAbility): Color = when (ability) {
    CareerAbility.LEADERSHIP -> Color(0xFFC62828)
    CareerAbility.DECISION -> Color(0xFF1E88E5)
    CareerAbility.COMMUNICATION -> Color(0xFF00897B)
    CareerAbility.EXECUTION -> Color(0xFF6D4C41)
    CareerAbility.SPECIALTY -> Color(0xFFEF6C00)
}

private fun stressLabel(stress: Int): String = when {
    stress >= 80 -> "接近极限"
    stress >= 60 -> "偏高"
    stress >= 35 -> "正常"
    else -> "轻松"
}
