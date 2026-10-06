package com.shitu.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shitu.app.game.GameState
import com.shitu.app.model.Attr
import com.shitu.app.model.AttrGroup
import com.shitu.app.model.HiddenKey
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituBackground
import com.shitu.app.ui.components.ThinDivider

// 属性页：十四项属性的进度条 + 隐藏变量的模糊提示。
@Composable
fun AttributeScreen(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val attrs = state.player.attributes

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        SectionCard(title = "个人属性", trailing = "范围 0 - 100") {
            var first = true
            AttrGroup.entries.forEach { group ->
                if (!first) Spacer(modifier = Modifier.height(14.dp))
                first = false
                Text(
                    text = groupLabel(group),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Attr.entries.filter { it.group == group }.forEach { attr ->
                    AttributeBar(
                        label = attr.label,
                        value = attrs.valueOf(attr),
                        barColor = colorOf(attr)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "隐性状态", trailing = "部分信息不公开") {
            HiddenKey.entries.forEach { key ->
                val value = state.player.hidden.valueOf(key)
                if (key.visible) {
                    AttributeBar(
                        label = key.label,
                        value = value,
                        barColor = scheme.secondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Text(
                        text = "· ${hiddenHint(key, value)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "当前处境") {
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("姓名", state.player.name, modifier = Modifier.weight(1f))
                InfoPair("年龄", "${state.player.age} 岁", modifier = Modifier.weight(1f))
            }
            if (state.retirementStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                InfoPair(
                    "状态",
                    state.retirementStatus,
                    modifier = Modifier.fillMaxWidth(),
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("学历", state.player.education, modifier = Modifier.weight(1f))
                InfoPair("性别", state.player.gender, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair(
                    "起点",
                    state.player.startTrack,
                    modifier = Modifier.weight(1f)
                )
                InfoPair(
                    "天赋",
                    state.player.traits.joinToString("、").ifEmpty { "无" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("职位", state.displayPositionTitle, modifier = Modifier.weight(1f))
                InfoPair("任职年限", "${state.player.yearsInPosition} 年", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("单位", state.player.unit.label, modifier = Modifier.weight(1f))
                InfoPair("服务年限", "${state.player.serviceYears} 年", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair(
                    "发展方向",
                    state.route?.label ?: "尚未确定",
                    modifier = Modifier.weight(1f)
                )
                InfoPair(
                    "方向侧重",
                    state.route?.focusLabel ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = state.player.unit.feature,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            state.route?.let { route ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "方向：${route.description}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "家庭：${state.player.family.summary}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "累计工作实绩 ${state.totalWorkGained} · 年度考核 ${state.reviewCount} 次",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun groupLabel(group: AttrGroup): String = when (group) {
    AttrGroup.PERFORMANCE -> "工作与能力"
    AttrGroup.RELATION -> "关系与声望"
    AttrGroup.RISK -> "风险"
    AttrGroup.SELF -> "个人状态"
}

@Composable
private fun colorOf(attr: Attr): Color {
    val scheme = MaterialTheme.colorScheme
    return when (attr.group) {
        AttrGroup.PERFORMANCE -> scheme.primary
        AttrGroup.RELATION -> scheme.tertiary
        AttrGroup.RISK -> scheme.error
        AttrGroup.SELF -> scheme.secondary
    }
}

// 隐藏变量只给出模糊描述，不暴露具体数值。
private fun hiddenHint(key: HiddenKey, value: Int): String = when (key) {
    HiddenKey.OPPORTUNITY -> when {
        value >= 60 -> "近来常有人提起你的名字，机会正在靠近。"
        value >= 30 -> "机会不多不少，需要自己争取。"
        else -> "很少有新的机会主动找到你。"
    }
    HiddenKey.MENTOR -> when {
        value >= 60 -> "关键时候，有人愿意替你说一句话。"
        value >= 30 -> "关键的人脉还算说得上话。"
        else -> "多数时候，你只能靠自己。"
    }
    HiddenKey.RISK_PRESSURE -> when {
        value >= 60 -> "你总觉得，有些事早晚会被翻出来。"
        value >= 30 -> "有些旧事需要留意。"
        else -> "暂时没有让人不安的东西。"
    }
    HiddenKey.HEALTH_RISK -> when {
        value >= 60 -> "身体已经在提醒你了。"
        value >= 30 -> "偶尔会觉得力不从心。"
        else -> "身体状态还算稳定。"
    }
    else -> ""
}
