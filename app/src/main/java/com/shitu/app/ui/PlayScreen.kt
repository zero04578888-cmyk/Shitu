package com.shitu.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameSave
import com.shitu.app.game.GameState
import com.shitu.app.game.Interaction
import com.shitu.app.game.PoliticsEngine
import com.shitu.app.model.LifeArchive
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.ShituBackground
import com.shitu.app.ui.components.ThinDivider

// 游戏主容器：顶部状态栏、内容区、底部主导航。
@Composable
fun PlayScreen(
    state: GameState?,
    tab: PlayTab,
    onTabChange: (PlayTab) -> Unit,
    secondary: PlayTab?,
    onSecondaryChange: (PlayTab?) -> Unit,
    onChoose: (Int) -> Unit,
    onContinue: () -> Unit,
    onReviewContinue: () -> Unit,
    onSaveManual: () -> Unit,
    onLoadManual: () -> Unit,
    hasManualSave: Boolean,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onDeleteSave: () -> Unit,
    selectedNpcId: String?,
    onSelectNpc: (String?) -> Unit,
    onInteract: (String, Interaction) -> Unit,
    relationMessage: String,
    governanceMessage: String,
    onGovernanceAction: (String) -> Unit,
    onPromote: (String) -> Unit = { },
    /** 65 岁人生节点的选择：RETIREMENT（退休）/ SECOND_LINE（退居二线）。 */
    onRetirement: (String) -> Unit = { },
    regionMessage: String,
    onStartProject: (String) -> Unit,
    onResumeProject: (String) -> Unit,
    onAcceptTransfer: () -> Unit,
    onDeclineTransfer: () -> Unit,
    onSetWorkPlan: (List<String>) -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    musicEnabled: Boolean = true,
    onMusicEnabledChange: (Boolean) -> Unit = {},
    musicVolume: Float = 0.30f,
    onMusicVolumeChange: (Float) -> Unit = {},
    sfxVolume: Float = 0.45f,
    onSfxVolumeChange: (Float) -> Unit = {},
    onTestSound: () -> Unit = {},
    hapticsEnabled: Boolean = true,
    onHapticsEnabledChange: (Boolean) -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    onResetAllData: () -> Unit = {},
    saveInfo: String = "",
    archive: LifeArchive = LifeArchive.EMPTY,
    galleryProgress: String = "",
    /** 第十五阶段：当前存档信封，用于「我的」页面的存档概览。 */
    save: GameSave? = null,
    onReloadSave: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (state == null) {
        ShituBackground(modifier = modifier) {
            Column(modifier = Modifier.fillMaxSize()) {
                EmptyHint("还没有开始人生。请先在首页选择「开始新人生」。")
            }
        }
        return
    }

    // 第十八阶段：【任职选择】窗口。年度考核通过后由玩家决定具体职务。
    var showPositionPicker by remember(state.pendingPromotion) {
        mutableStateOf(false)
    }
    if (showPositionPicker && state.pendingPromotion && !state.retirementDue) {
        val pickerOptions = remember(state.pendingPromotion, state.player.positionIndex) {
            com.shitu.app.game.GameEngine.pendingOptions(state)
        }
        PositionPickerDialog(
            subtitle = "年度考核已经通过，你已具备${state.currentRank}任职资格。",
            cityLabel = "${state.currentProvince} · ${state.currentCityName}",
            options = pickerOptions,
            confirmLabel = "接受任命",
            onConfirm = { positionId ->
                showPositionPicker = false
                onPromote(positionId)
            }
        )
    }

    val ending = state.ending
    if (state.phase == GamePhase.ENDING && ending != null) {
        EndingScreen(
            ending = ending,
            onRestart = onRestart,
            onHome = onExit,
            galleryProgress = galleryProgress,
            modifier = modifier
        )
        return
    }

    // 65 岁人生节点：优先级高于年度报告、任职选择与一切普通内容。
    // 点击外部不会关闭，必须选择「退休」或「退居二线」。
    if (state.retirementDue) {
        RetirementDialog(
            positionTitle = state.displayPositionTitle,
            age = state.playerAge,
            onChoice = onRetirement
        )
    }

    ShituBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusHeader(state = state, onSaveManual = onSaveManual, onExit = onExit)
            ThinDivider()

            Column(modifier = Modifier.weight(1f)) {
                val currentSecondary = secondary
                if (state.phase == GamePhase.REVIEW && state.reviews.lastOrNull() != null) {
                    // 年度考核必须展示，优先于二级页面，避免玩家错过时间推进。
                    // 第十八阶段：晋升资格已通过时，先让玩家在【任职选择】里挑具体职务。
                    ReviewScreen(
                        state = state,
                        review = state.reviews.last(),
                        pendingPromotion = state.pendingPromotion,
                        onContinue = {
                            if (state.pendingPromotion) showPositionPicker = true else onReviewContinue()
                        }
                    )
                } else if (currentSecondary != null) {
                    SecondaryHeader(
                        title = tabLabel(currentSecondary),
                        onBack = { onSecondaryChange(null) }
                    )
                    SecondaryContent(
                        tab = currentSecondary,
                        state = state,
                        archive = archive,
                        selectedNpcId = selectedNpcId,
                        onSelectNpc = onSelectNpc,
                        onInteract = onInteract,
                        relationMessage = relationMessage,
                        onReviewContinue = onReviewContinue,
                        darkMode = darkMode,
                        onDarkModeChange = onDarkModeChange,
                        hasManualSave = hasManualSave,
                        onSaveManual = onSaveManual,
                        onLoadManual = onLoadManual,
                        onDeleteSave = onDeleteSave,
                        soundEnabled = soundEnabled,
                        onSoundEnabledChange = onSoundEnabledChange,
                        musicEnabled = musicEnabled,
                        onMusicEnabledChange = onMusicEnabledChange,
                        musicVolume = musicVolume,
                        onMusicVolumeChange = onMusicVolumeChange,
                        sfxVolume = sfxVolume,
                        onSfxVolumeChange = onSfxVolumeChange,
                        onTestSound = onTestSound,
                        hapticsEnabled = hapticsEnabled,
                        onHapticsEnabledChange = onHapticsEnabledChange,
                        onReplayTutorial = onReplayTutorial,
                        onResetAllData = onResetAllData,
                        saveInfo = saveInfo,
                        regionMessage = regionMessage,
                        onStartProject = onStartProject,
                        onResumeProject = onResumeProject,
                        onAcceptTransfer = onAcceptTransfer,
                        onDeclineTransfer = onDeclineTransfer,
                        onSetWorkPlan = onSetWorkPlan,
                        onPromote = onPromote
                    )
                } else {
                    PrimaryContent(
                        tab = tab,
                        state = state,
                        archive = archive,
                        save = save,
                        onTabChange = onTabChange,
                        onSecondaryChange = onSecondaryChange,
                        onChoose = onChoose,
                        onContinue = onContinue,
                        onGovernanceAction = onGovernanceAction,
                        governanceMessage = governanceMessage,
                        onPromote = onPromote,
                        onReloadSave = onReloadSave,
                        onRestart = onRestart,
                        onDeleteSave = onDeleteSave,
                        onExit = onExit
                    )
                }
            }

            BottomBar(
                tab = tab,
                secondary = secondary,
                onTabChange = {
                    onSecondaryChange(null)
                    onTabChange(it)
                }
            )
        }
    }
}

@Composable
private fun PrimaryContent(
    tab: PlayTab,
    state: GameState,
    archive: LifeArchive,
    save: GameSave?,
    onTabChange: (PlayTab) -> Unit,
    onSecondaryChange: (PlayTab?) -> Unit,
    onChoose: (Int) -> Unit,
    onContinue: () -> Unit,
    onGovernanceAction: (String) -> Unit,
    onPromote: (String) -> Unit = { },
    governanceMessage: String,
    onReloadSave: () -> Unit,
    onRestart: () -> Unit,
    onDeleteSave: () -> Unit,
    onExit: () -> Unit
) {
    when (tab) {
        PlayTab.HOME -> HomeScreen(
            state = state,
            onNavigate = onTabChange,
            onOpenSecondary = onSecondaryChange
        )

        PlayTab.GOVERNANCE -> GovernanceScreen(
            state = state,
            message = governanceMessage,
            onAction = onGovernanceAction
        )

        PlayTab.EVENT -> GameScreen(state = state, onChoose = onChoose, onContinue = onContinue)

        PlayTab.CAREER -> CareerScreen(state = state, archive = archive)

        PlayTab.MINE -> MineScreen(
            state = state,
            archive = archive,
            save = save,
            onOpen = onSecondaryChange,
            onReloadSave = onReloadSave,
            onRestart = onRestart,
            onDeleteSave = onDeleteSave,
            onExit = onExit,
            onPromote = onPromote
        )

        else -> HomeScreen(
            state = state,
            onNavigate = onTabChange,
            onOpenSecondary = onSecondaryChange
        )
    }
}

@Composable
private fun SecondaryContent(
    tab: PlayTab,
    state: GameState,
    archive: LifeArchive,
    selectedNpcId: String?,
    onSelectNpc: (String?) -> Unit,
    onInteract: (String, Interaction) -> Unit,
    relationMessage: String,
    onReviewContinue: () -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    hasManualSave: Boolean,
    onSaveManual: () -> Unit,
    onLoadManual: () -> Unit,
    onDeleteSave: () -> Unit,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    musicEnabled: Boolean = true,
    onMusicEnabledChange: (Boolean) -> Unit = {},
    musicVolume: Float = 0.30f,
    onMusicVolumeChange: (Float) -> Unit = {},
    sfxVolume: Float = 0.45f,
    onSfxVolumeChange: (Float) -> Unit = {},
    onTestSound: () -> Unit = {},
    hapticsEnabled: Boolean = true,
    onHapticsEnabledChange: (Boolean) -> Unit = {},
    onReplayTutorial: () -> Unit = {},
    onResetAllData: () -> Unit = {},
    saveInfo: String = "",
    regionMessage: String,
    onStartProject: (String) -> Unit,
    onResumeProject: (String) -> Unit,
    onAcceptTransfer: () -> Unit,
    onDeclineTransfer: () -> Unit,
    onSetWorkPlan: (List<String>) -> Unit,
    onPromote: (String) -> Unit = {}
) {
    when (tab) {
        PlayTab.REGION -> RegionScreen(state = state)

        PlayTab.PROJECT -> ProjectScreen(
            state = state,
            message = regionMessage,
            onStart = onStartProject,
            onResume = onResumeProject
        )

        PlayTab.TRANSFER -> TransferScreen(
            state = state,
            message = regionMessage,
            onAccept = onAcceptTransfer,
            onDecline = onDeclineTransfer
        )

        PlayTab.WORKPLAN -> WorkPlanScreen(
            state = state,
            message = regionMessage,
            onSubmit = onSetWorkPlan
        )

        PlayTab.RELATION -> RelationshipScreen(
            state = state,
            selectedNpcId = selectedNpcId,
            onSelectNpc = onSelectNpc,
            onInteract = onInteract,
            message = relationMessage
        )

        PlayTab.POLITICS -> PoliticsScreen(state = state, onPromote = onPromote)

        PlayTab.WORLD -> WorldScreen(state = state)

        PlayTab.ATTRIBUTES -> AttributeScreen(state = state)

        PlayTab.LOG -> LogScreen(state = state, modifier = Modifier.padding(horizontal = 20.dp))

        PlayTab.REVIEW -> {
            val review = state.reviews.lastOrNull()
            if (review != null) {
                ReviewScreen(state = state, review = review, onContinue = onReviewContinue)
            } else {
                EmptyHint("还没有进行过年度考核。每四个季度会考核一次。")
            }
        }

        PlayTab.SETTINGS -> Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            SettingsContent(
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                hasSave = true,
                hasManualSave = hasManualSave,
                onSaveManual = onSaveManual,
                onLoadManual = onLoadManual,
                onDeleteSave = onDeleteSave,
                soundEnabled = soundEnabled,
                onSoundEnabledChange = onSoundEnabledChange,
                musicEnabled = musicEnabled,
                onMusicEnabledChange = onMusicEnabledChange,
                musicVolume = musicVolume,
                onMusicVolumeChange = onMusicVolumeChange,
                sfxVolume = sfxVolume,
                onSfxVolumeChange = onSfxVolumeChange,
                onTestSound = onTestSound,
                hapticsEnabled = hapticsEnabled,
                onHapticsEnabledChange = onHapticsEnabledChange,
                onReplayTutorial = onReplayTutorial,
                onResetAllData = onResetAllData,
                saveInfo = saveInfo,
                inGame = true
            )
        }

        else -> EmptyHint("这个页面暂不可用。")
    }
}

@Composable
private fun SecondaryHeader(title: String, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("‹ 返回", color = scheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface
        )
    }
    ThinDivider()
}

@Composable
private fun StatusHeader(
    state: GameState,
    onSaveManual: () -> Unit,
    onExit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "第 ${state.year} 年 · 第 ${state.quarter} 季度",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.primary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onSaveManual) { Text("保存") }
            TextButton(onClick = onExit) { Text("主菜单") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(text = state.displayPositionTitle)
            Spacer(modifier = Modifier.width(6.dp))
            Tag(text = state.player.unit.label)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${state.player.name} · 任职 ${state.player.yearsInPosition} 年",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "辖区 ${state.governance.jurisdiction.name}" +
                " · 政绩 ${state.governance.merit.overall}" +
                " · 民望 ${state.governance.jurisdiction.approval}" +
                " · 财政 ${state.governance.finance}" +
                " · 威望 ${PoliticsEngine.reputation(state)}" +
                " · 风险 ${state.relationship.risk}",
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BottomBar(
    tab: PlayTab,
    secondary: PlayTab?,
    onTabChange: (PlayTab) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        ThinDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            PRIMARY_TABS.forEach { item ->
                val selected = secondary == null && item == tab
                TextButton(
                    onClick = { onTabChange(item) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tabLabel(item),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) scheme.primary else scheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun tabLabel(tab: PlayTab): String = when (tab) {
    PlayTab.HOME -> "首页"
    PlayTab.GOVERNANCE -> "政务"
    PlayTab.EVENT -> "事件"
    PlayTab.CAREER -> "生涯"
    PlayTab.MINE -> "我的"
    PlayTab.RELATION -> "人物关系"
    PlayTab.POLITICS -> "势力与机会"
    PlayTab.REGION -> "辖区与地区"
    PlayTab.PROJECT -> "重大项目"
    PlayTab.TRANSFER -> "调任机会"
    PlayTab.WORKPLAN -> "工作安排"
    PlayTab.WORLD -> "世界动态"
    PlayTab.ATTRIBUTES -> "属性"
    PlayTab.LOG -> "人生日志"
    PlayTab.REVIEW -> "年度报告"
    PlayTab.SETTINGS -> "设置与存档"
}
