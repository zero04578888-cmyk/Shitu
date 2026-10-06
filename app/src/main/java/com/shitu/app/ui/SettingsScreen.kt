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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shitu.app.BuildConfig
import com.shitu.app.ui.components.ShituBackground
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ThinDivider
import kotlin.math.roundToInt
import com.shitu.app.util.GameCodec

// 设置页（首页入口）：带标题与返回按钮。
@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    hasSave: Boolean,
    musicEnabled: Boolean = true,
    onMusicEnabledChange: (Boolean) -> Unit = {},
    musicVolume: Float = 0.30f,
    onMusicVolumeChange: (Float) -> Unit = {},
    sfxVolume: Float = 0.45f,
    onSfxVolumeChange: (Float) -> Unit = {},
    onTestSound: () -> Unit = {},
    hasManualSave: Boolean,
    onSaveManual: () -> Unit,
    onLoadManual: () -> Unit,
    onDeleteSave: () -> Unit,
    hapticsEnabled: Boolean = true,
    onHapticsEnabledChange: (Boolean) -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    onResetAllData: () -> Unit = {},
    saveInfo: String = "",
    onBack: () -> Unit,
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
                    text = "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onBack) { Text("返回") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SettingsContent(
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                soundEnabled = soundEnabled,
                onSoundEnabledChange = onSoundEnabledChange,
                hasSave = hasSave,
                musicEnabled = musicEnabled,
                onMusicEnabledChange = onMusicEnabledChange,
                musicVolume = musicVolume,
                onMusicVolumeChange = onMusicVolumeChange,
                sfxVolume = sfxVolume,
                onSfxVolumeChange = onSfxVolumeChange,
                onTestSound = onTestSound,
                hasManualSave = hasManualSave,
                onSaveManual = onSaveManual,
                onLoadManual = onLoadManual,
                onDeleteSave = onDeleteSave,
                hapticsEnabled = hapticsEnabled,
                onHapticsEnabledChange = onHapticsEnabledChange,
                onReplayTutorial = onReplayTutorial,
                onResetAllData = onResetAllData,
                saveInfo = saveInfo,
                inGame = false
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 设置内容：首页与游戏内共用。
@Composable
fun SettingsContent(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    hasSave: Boolean,
    musicEnabled: Boolean = true,
    onMusicEnabledChange: (Boolean) -> Unit = {},
    musicVolume: Float = 0.30f,
    onMusicVolumeChange: (Float) -> Unit = {},
    sfxVolume: Float = 0.45f,
    onSfxVolumeChange: (Float) -> Unit = {},
    onTestSound: () -> Unit = {},
    hasManualSave: Boolean,
    onSaveManual: () -> Unit,
    onLoadManual: () -> Unit,
    onDeleteSave: () -> Unit,
    hapticsEnabled: Boolean = true,
    onHapticsEnabledChange: (Boolean) -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    onResetAllData: () -> Unit = {},
    saveInfo: String = "",
    inGame: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    var showConfirmDelete by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showConfirmDelete) {
            AlertDialog(
                onDismissRequest = { showConfirmDelete = false },
                title = { Text("删除全部存档？") },
                text = {
                    Text(
                        "自动存档与手动存档都会被删除，且无法恢复。" +
                            "结局图鉴、成就与已解锁内容会保留。"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDelete = false
                        onDeleteSave()
                    }) { Text("确认删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDelete = false }) { Text("取消") }
                }
            )
        }
        if (showPrivacy) {
            AlertDialog(
                onDismissRequest = { showPrivacy = false },
                title = { Text("隐私说明") },
                text = {
                    Text(
                        "《仕途》完全离线运行，不联网、不登录、不收集或上传任何个人数据。" +
                            "所有进度只保存在本机。正式版将提供完整的隐私说明。"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacy = false }) { Text("知道了") }
                }
            )
        }
        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("重置全部数据？") },
                text = {
                    Text(
                        "存档、结局图鉴、成就与引导状态都会被清空，并且无法恢复。" +
                            "如果你正在游戏中，请重置后返回主菜单重新开始。"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showResetConfirm = false
                        onResetAllData()
                    }) { Text("确认重置") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) { Text("取消") }
                }
            )
        }
        SectionCard(title = "显示") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDarkModeChange(!darkMode) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "深色模式",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    Text(
                        text = if (darkMode) "当前使用深色配色" else "当前使用浅色配色",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
            }

            Spacer(modifier = Modifier.height(12.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHapticsEnabledChange(!hapticsEnabled) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "轻量震动",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "晋升、成就与重大事件时轻微震动一下",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Switch(checked = hapticsEnabled, onCheckedChange = onHapticsEnabledChange)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "声音") {
            Text(
                text = "背景音乐与界面音效都是自制的柔和音色，安静、耐听、不刺耳。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onMusicEnabledChange(!musicEnabled)
                        onTestSound()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "背景音乐",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    Text(
                        text = if (musicEnabled) "关闭即为静音音乐" else "背景音乐已静音",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Switch(checked = musicEnabled, onCheckedChange = onMusicEnabledChange)
            }
            Spacer(modifier = Modifier.height(8.dp))
            VolumeSlider(
                label = "音乐音量",
                value = musicVolume,
                enabled = musicEnabled,
                onValueChange = onMusicVolumeChange,
                onFinished = onTestSound
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSoundEnabledChange(!soundEnabled)
                        onTestSound()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "界面音效",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "点击、选择、晋升、成就与重大事件时的短提示音",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Switch(checked = soundEnabled, onCheckedChange = onSoundEnabledChange)
            }
            Spacer(modifier = Modifier.height(8.dp))
            VolumeSlider(
                label = "音效音量",
                value = sfxVolume,
                enabled = soundEnabled,
                onValueChange = onSfxVolumeChange,
                onFinished = onTestSound
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "存档") {
            Text(
                text = if (hasSave) {
                    "游戏进行中会自动保存。你也可以随时手动保存到独立存档位。"
                } else {
                    "当前没有存档。开始新人生后会自动保存到本机。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "存档格式 v${GameCodec.FORMAT_VERSION} · 自动与手动存档各保留一份最近备份" +
                    " · 主存档损坏时会尝试自动恢复。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            if (saveInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = saveInfo,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            if (inGame) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onSaveManual,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    ) {
                        Text("手动保存")
                    }
                    OutlinedButton(
                        onClick = onLoadManual,
                        enabled = hasManualSave,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                    ) {
                        Text("读取手动存档")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showConfirmDelete = true },
                enabled = hasSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("删除全部存档（保留人生图鉴）")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "数据与引导") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReplayTutorial() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "重新查看新手引导",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "下次进入游戏时会再次显示引导",
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
            Spacer(modifier = Modifier.height(12.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重置全部数据（含结局图鉴）")
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "重置会清空存档、结局图鉴与已解锁成就，无法恢复。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "游戏机制说明") {
            Text(
                text = "《仕途》是虚构的职业生涯模拟游戏。\n" +
                    "所有晋升条件、属性权重、事件概率均为游戏模拟参数，只服务于玩法平衡，" +
                    "不代表任何现实制度或政策。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "事件中的很多选择只改变隐性状态（压力、健康隐患、廉政压力、社会影响、" +
                    "人际网络）。它们不会立刻见效，而是在之后的季度里逐渐产生后果。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "任职到副科级前后，你会做一次发展方向的选择。" +
                    "方向会改变之后遇到的任务、年度考核的侧重点和调岗去向。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "「关系」页可以拜访、请教、送礼、求助、汇报、结交、拒绝合作和观察态度。" +
                    "互动要花精力，送礼会积累官场风险；靠拢一方势力，另一方会有看法。" +
                    "风险过高会带来谈话、检查与晋升受阻，但都可以通过后续选择挽回。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "「政务」页管理你任职的辖区：每季度有固定行动点，做每件事都要花财政与精力，" +
                    "财政不足就只能先放下。政绩由经济、民生、治安、财政、创新五项加权，" +
                    "每年自然衰减；民望过低会招来投诉与舆情。年度考核与晋升都要看治理成绩。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "年度考核权重（游戏模拟参数）",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "工作实绩 30% · 工作能力 20% · 政治素养 15% · " +
                    "群众基础 10% · 领导信任 15% · 同事关系 10%",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "关于") {
            Text(
                text = "《仕途》V${BuildConfig.VERSION_NAME}（Build ${BuildConfig.VERSION_CODE}）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "完全离线运行，不联网、不登录、不收集任何数据。",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "存档格式 v${GameCodec.FORMAT_VERSION} · 晋升规则与所有数值都是虚构的游戏模拟参数。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacy = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "隐私说明",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 音量滑条：数值取整到 5% 档，避免过度精细的无效拖拽。
@Composable
private fun VolumeSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val percent = (value * 100).toInt()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onValueChange((it * 20f).roundToInt() / 20f) },
            onValueChangeFinished = onFinished,
            enabled = enabled,
            valueRange = 0f..1f,
            steps = 19
        )
    }
}