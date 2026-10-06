package com.shitu.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.audio.AndroidGameHaptics
import com.shitu.app.audio.AudioSettings
import com.shitu.app.audio.GameSound
import com.shitu.app.audio.HapticKind
import com.shitu.app.audio.MediaGameMusic
import com.shitu.app.audio.MusicCue
import com.shitu.app.audio.MusicScene
import com.shitu.app.audio.MusicSceneResolver
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameRng
import com.shitu.app.game.GameSave
import com.shitu.app.game.GameState
import com.shitu.app.game.GovernanceEngine
import com.shitu.app.game.PoliticalEngine
import com.shitu.app.game.ProjectEngine
import com.shitu.app.game.RegionEngine
import com.shitu.app.game.WorkPlanEngine
import com.shitu.app.model.AchievementCatalog
import com.shitu.app.model.AchievementDef
import com.shitu.app.model.EventTier
import com.shitu.app.model.EndingCatalog
import com.shitu.app.model.WorkSlot
import com.shitu.app.ui.theme.ShituTheme
import com.shitu.app.util.LoadResult
import com.shitu.app.util.SaveManager
import kotlinx.coroutines.delay

// 顶层页面。
enum class RootScreen {
    HOME,
    CREATE,
    PLAY,
    ARCHIVE,
    SETTINGS
}

// 游戏内的标签页。前五个是底部主导航，其余通过「我的」进入二级页面。
enum class PlayTab {
    HOME,
    GOVERNANCE,
    EVENT,
    CAREER,
    MINE,
    RELATION,
    POLITICS,
    REGION,
    PROJECT,
    TRANSFER,
    WORKPLAN,
    WORLD,
    ATTRIBUTES,
    LOG,
    REVIEW,
    SETTINGS
}

// 底部导航只保留这五个入口。
val PRIMARY_TABS = listOf(
    PlayTab.HOME,
    PlayTab.GOVERNANCE,
    PlayTab.EVENT,
    PlayTab.CAREER,
    PlayTab.MINE
)

// 应用根组合函数：负责页面切换、存档与主题。
@Composable
fun ShituApp() {
    val context = LocalContext.current
    val saveManager = remember { SaveManager(context) }

    var root by remember { mutableStateOf(RootScreen.HOME) }
    var tab by remember { mutableStateOf(PlayTab.HOME) }
    var secondary by remember { mutableStateOf<PlayTab?>(null) }
    var state by remember { mutableStateOf<GameState?>(null) }
    var darkOverride by remember { mutableStateOf(saveManager.darkModeOverride()) }
    var refresh by remember { mutableIntStateOf(0) }
    var selectedNpcId by remember { mutableStateOf<String?>(null) }
    var relationMessage by remember { mutableStateOf("") }
    var governanceMessage by remember { mutableStateOf("") }
    var regionMessage by remember { mutableStateOf("") }
    var tutorialDone by remember { mutableStateOf(saveManager.tutorialDone()) }
    var achievementBanner by remember { mutableStateOf<AchievementDef?>(null) }
    val soundEnabled = remember { mutableStateOf(saveManager.soundEnabled()) }
    val hapticsEnabled = remember { mutableStateOf(saveManager.hapticsEnabled()) }
    val musicEnabled = remember { mutableStateOf(saveManager.musicEnabled()) }
    val musicVolume = remember { mutableStateOf(saveManager.musicVolume()) }
    val sfxVolume = remember { mutableStateOf(saveManager.sfxVolume()) }
    // 第十七阶段：统一的音乐 / 音效管理器。资源缺失时静默降级，不影响游戏流程。
    val audio = remember { MediaGameMusic(context) }
    val haptics = remember { AndroidGameHaptics(context) { hapticsEnabled.value } }

    DisposableEffect(Unit) {
        onDispose {
            audio.release()
            haptics.release()
        }
    }

    // 音量与开关实时生效：关闭音效 = 音效音量归零，静音音乐则整体淡出。
    LaunchedEffect(soundEnabled.value, sfxVolume.value) {
        audio.setSfxVolume(if (soundEnabled.value) sfxVolume.value else 0f)
    }
    LaunchedEffect(musicEnabled.value) {
        audio.setMusicEnabled(musicEnabled.value)
    }
    LaunchedEffect(musicVolume.value) {
        audio.setMusicVolume(musicVolume.value)
    }

    // 第十五阶段：进入后台（ON_STOP）时自动保存一次，进程被回收也不会丢进度。
    val hostActivity = remember(context) { context.findComponentActivity() }
    DisposableEffect(hostActivity) {
        val lifecycle = hostActivity?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // 进入后台：自动保存一次并淡出音乐，避免打扰其他应用。
                    state?.let { saveManager.saveAuto(it) }
                    audio.pauseMusic()
                }
                Lifecycle.Event.ON_START -> audio.resumeMusic()
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    val useDark = darkOverride ?: isSystemInDarkTheme()
    val latest: LoadResult = remember(refresh) { saveManager.loadLatest() }
    val hasSave = latest is LoadResult.Success
    val latestSave: GameSave? = remember(latest) { (latest as? LoadResult.Success)?.save }
    val hasManual = remember(refresh) { saveManager.hasManualSave() }
    val summary = remember(refresh) { saveSummaryOf(latest) }
    val archiveLines = remember(refresh) { archiveLinesOf(latest) }
    val archiveEnding = remember(refresh) { archiveEndingOf(latest) }
    val lifeArchive = remember(refresh) { saveManager.loadArchive() }
    val gallery = remember(lifeArchive) { EndingCatalog.galleryOf(lifeArchive) }
    val galleryProgress = if (lifeArchive.unlockedCount > 0) {
        "结局图鉴 已解锁 ${lifeArchive.unlockedCount} / ${EndingCatalog.total}"
    } else {
        ""
    }
    val saveInfo = remember(refresh) { saveInfoOf(saveManager) }

    // 第十七阶段：按当前界面与事件等级切换场景音乐。
    var promotionMoment by remember { mutableStateOf(false) }
    val baseCue = musicCueOf(
        root = root,
        tab = tab,
        secondary = secondary,
        phase = state?.phase,
        hasEnding = state?.ending != null,
        tier = currentEventTierOf(state)
    )
    LaunchedEffect(promotionMoment, baseCue) {
        if (promotionMoment) {
            // 晋升音乐一次性播放：先铺垫、再弦乐展开，约十秒后淡回当前场景。
            audio.crossFadeTo(MusicScene.PROMOTION, AudioSettings.CROSS_FADE_MS)
            delay(PROMOTION_MOMENT_MS)
            promotionMoment = false
        } else {
            // 稍微延迟一下再切歌：玩家快速翻页时不会来回打断音乐。
            delay(SCENE_DEBOUNCE_MS)
            audio.crossFadeTo(MusicSceneResolver.sceneFor(baseCue), AudioSettings.CROSS_FADE_MS)
        }
    }

    // 每次状态变化都写入自动存档，并根据变化播放极短的提示音、弹出成就横幅。
    fun persist(newState: GameState) {
        try {
            val previous = state
            val gained = if (previous == null) {
                emptySet()
            } else {
                newState.career.unlockedAchievements - previous.career.unlockedAchievements
            }
            val promoted = previous != null && newState.promotionCount > previous.promotionCount
            state = newState
            saveManager.saveAuto(newState)
            if (newState.ending != null) saveManager.recordEnding(newState)
            refresh += 1
            when {
                newState.ending != null && previous?.ending == null -> {
                    audio.play(GameSound.ENDING)
                    haptics.play(HapticKind.ENDING)
                }
                promoted -> {
                    audio.play(GameSound.PROMOTION)
                    haptics.play(HapticKind.PROMOTION)
                    promotionMoment = true
                }
                gained.isNotEmpty() -> {
                    audio.play(GameSound.ACHIEVEMENT)
                    haptics.play(HapticKind.ACHIEVEMENT)
                }
            }
            if (gained.isNotEmpty()) {
                // 横幅显示成就名称与说明，避免只弹一句文本。
                achievementBanner = AchievementCatalog.find(gained.first())
            }
        } catch (error: Exception) {
            Log.e("ShituApp", "结算或存档失败", error)
            toast(context, "操作失败，请稍后重试。")
        }
    }

    ShituTheme(darkTheme = useDark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 第十四阶段：返回键优先回到主菜单与首页，避免误触直接退出游戏。
                BackHandler(
                    enabled = root != RootScreen.HOME || secondary != null ||
                        (root == RootScreen.PLAY && tab != PlayTab.HOME)
                ) {
                    audio.play(GameSound.BACK)
                    when {
                        root == RootScreen.PLAY && secondary != null -> secondary = null
                        root == RootScreen.PLAY && tab != PlayTab.HOME -> tab = PlayTab.HOME
                        else -> {
                            refresh += 1
                            root = RootScreen.HOME
                        }
                    }
                }
                when (root) {
                    RootScreen.HOME -> StartScreen(
                        hasSave = hasSave,
                        saveSummary = summary,
                        galleryProgress = galleryProgress,
                        onNewGame = { root = RootScreen.CREATE },
                        onContinue = {
                            when (val result = saveManager.loadLatest()) {
                                is LoadResult.Success -> {
                                    state = result.state
                                    if (result.recovered) toast(context, "主存档异常，已从备份恢复。")
                                    if (result.state.ending != null) saveManager.recordEnding(result.state)
                                    refresh += 1
                                    secondary = null
                                    tab = PlayTab.HOME
                                    root = RootScreen.PLAY
                                }
                                LoadResult.Corrupt -> {
                                    // 存档损坏时自动恢复为默认状态，不让应用崩溃。
                                    saveManager.clearAll()
                                    refresh += 1
                                    toast(context, "存档读取失败，已创建新的游戏数据。")
                                    root = RootScreen.CREATE
                                }
                                LoadResult.Empty -> toast(context, "没有可以继续的存档。")
                            }
                        },
                        onRestart = { root = RootScreen.CREATE },
                        onArchive = { root = RootScreen.ARCHIVE },
                        onSettings = { root = RootScreen.SETTINGS }
                    )

                    RootScreen.CREATE -> CreateCharacterScreen(
                        onBack = { root = RootScreen.HOME },
                        onStart = { draft ->
                            // 第十二阶段：多周目变化。每个周目使用不同的随机种子，
                            // 让初始世界、事件顺序与人物状态都有所不同，同时仍可复现。
                            val playthroughSeed = GameRng.DEFAULT_SEED +
                                lifeArchive.records.size * 104729L +
                                draft.name.hashCode().toLong() * 31L +
                                draft.start.hashCode().toLong() +
                                draft.regionId.hashCode().toLong()
                            val newState = GameEngine.newGame(
                                name = draft.name,
                                gender = draft.gender,
                                education = draft.education,
                                start = draft.start,
                                seed = playthroughSeed,
                                traits = draft.traits,
                                priorLives = lifeArchive.records.size,
                                regionId = draft.regionId
                            )
                            state = newState
                            // 新的一局：重置本局开始时间后再写入存档。
                            saveManager.beginRun()
                            saveManager.saveAuto(newState)
                            refresh += 1
                            secondary = null
                            tab = PlayTab.HOME
                            root = RootScreen.PLAY
                        }
                    )

                    RootScreen.PLAY -> PlayScreen(
                        state = state,
                        tab = tab,
                        onTabChange = {
                            if (it != tab) audio.play(GameSound.PAGE)
                            tab = it
                        },
                        secondary = secondary,
                        onSecondaryChange = {
                            if (it != secondary) audio.play(if (it == null) GameSound.BACK else GameSound.PAGE)
                            secondary = it
                        },
                        onChoose = { index ->
                            state?.let { current ->
                                val event = GameEngine.currentEvent(current)
                                if (event != null && event.effectiveTier.isHighlighted) {
                                    audio.play(GameSound.EVENT_MAJOR)
                                } else {
                                    audio.play(GameSound.CLICK)
                                }
                                persist(GameEngine.resolveChoice(current, index))
                            }
                        },
                        onContinue = {
                            audio.play(GameSound.PAGE)
                            state?.let { current -> persist(GameEngine.advance(current)) }
                        },
                        onPromote = { positionId ->
                            state?.let { current ->
                                val promoted = GameEngine.promoteNow(current, positionId)
                                if (promoted.promotionCount > current.promotionCount) {
                                    // 第十八阶段：年度考核挂起的晋升在选定职务后
                                    // 直接进入下一年度，不再多按一次「继续」。
                                    val next = if (current.pendingPromotion) {
                                        GameEngine.proceedFromReview(promoted)
                                    } else {
                                        promoted
                                    }
                                    persist(next)
                                    toast(
                                        context,
                                        "任命完成：${current.displayPositionTitle} → " +
                                            promoted.displayPositionTitle
                                    )
                                }
                            }
                        },
                        onReviewContinue = {
                            state?.let { current -> persist(GameEngine.proceedFromReview(current)) }
                        },
                        onRetirement = { type ->
                            state?.let { current ->
                                val next = GameEngine.retirementChoice(current, type)
                                persist(next)
                                if (next.ending != null) {
                                    toast(context, "你办理了退休，仕途告一段落。")
                                } else {
                                    toast(context, "你退居二线，不再参与职务晋升。")
                                }
                            }
                        },
                        onSaveManual = {
                            state?.let { current ->
                                saveManager.saveManual(current)
                                refresh += 1
                                toast(context, "已保存到手动存档位。")
                            }
                        },
                        onLoadManual = {
                            when (val result = saveManager.loadManual()) {
                                is LoadResult.Success -> {
                                    state = result.state
                                    if (result.recovered) toast(context, "主存档异常，已从备份恢复。")
                                    if (result.state.ending != null) saveManager.recordEnding(result.state)
                                    refresh += 1
                                    secondary = null
                                    tab = PlayTab.HOME
                                    toast(context, "已读取手动存档。")
                                }
                                LoadResult.Corrupt -> toast(context, "手动存档已损坏，且未能恢复备份。")
                                LoadResult.Empty -> toast(context, "还没有手动存档。")
                            }
                        },
                        hasManualSave = hasManual,
                        save = latestSave,
                        onReloadSave = {
                            when (val result = saveManager.loadLatest()) {
                                is LoadResult.Success -> {
                                    state = result.state
                                    if (result.recovered) toast(context, "存档异常，已从备份恢复。")
                                    if (result.state.ending != null) saveManager.recordEnding(result.state)
                                    refresh += 1
                                    secondary = null
                                    tab = PlayTab.HOME
                                    toast(context, "已读取最近一次存档。")
                                }
                                LoadResult.Corrupt -> toast(context, "存档已损坏，无法读取。")
                                LoadResult.Empty -> toast(context, "还没有存档。")
                            }
                        },
                        onExit = {
                            // 退出游戏前显式保存一次。
                            state?.let { saveManager.saveAuto(it) }
                            refresh += 1
                            root = RootScreen.HOME
                        },
                        onRestart = {
                            state = null
                            root = RootScreen.CREATE
                        },
                        darkMode = useDark,
                        onDarkModeChange = { value ->
                            darkOverride = value
                            saveManager.setDarkModeOverride(value)
                        },
                        onDeleteSave = {
                            saveManager.clearAll()
                            state = null
                            refresh += 1
                            toast(context, "存档已删除。")
                            root = RootScreen.HOME
                        },
                        selectedNpcId = selectedNpcId,
                        onSelectNpc = { id ->
                            selectedNpcId = id
                            relationMessage = ""
                        },
                        onInteract = { npcId, action ->
                            state?.let { current ->
                                val result = PoliticalEngine.interact(current, npcId, action)
                                relationMessage = result.message
                                persist(result.state)
                            }
                        },
                        relationMessage = relationMessage,
                        governanceMessage = governanceMessage,
                        onGovernanceAction = { actionId ->
                            state?.let { current ->
                                val outcome = GovernanceEngine.perform(current, actionId)
                                governanceMessage = outcome.message
                                persist(outcome.state)
                            }
                        },
                        regionMessage = regionMessage,
                        onStartProject = { projectId ->
                            state?.let { current ->
                                val (updated, msg) = ProjectEngine.start(current, projectId)
                                regionMessage = msg
                                if (updated !== current) audio.play(GameSound.PAGE)
                                persist(updated)
                            }
                        },
                        onResumeProject = { projectId ->
                            state?.let { current ->
                                val (updated, msg) = ProjectEngine.resume(current, projectId)
                                regionMessage = msg
                                persist(updated)
                            }
                        },
                        onAcceptTransfer = {
                            state?.let { current ->
                                val offer = current.transferOffer
                                if (offer == null) {
                                    regionMessage = "现在没有可接受的调任安排。"
                                } else {
                                    audio.play(GameSound.PAGE)
                                    val target = com.shitu.app.model.RegionCatalog.findOrDefault(offer.regionId)
                                    regionMessage = "已接受调任，赴${target.name}任职。"
                                    persist(RegionEngine.accept(current, offer))
                                }
                            }
                        },
                        onDeclineTransfer = {
                            state?.let { current ->
                                val offer = current.transferOffer
                                if (offer == null) {
                                    regionMessage = "现在没有需要回应的调任安排。"
                                } else {
                                    regionMessage = "你婉拒了这次调任。"
                                    persist(RegionEngine.decline(current, offer))
                                }
                            }
                        },
                        onSetWorkPlan = { slotNames ->
                            state?.let { current ->
                                val slots = slotNames.mapNotNull { name ->
                                    WorkSlot.entries.firstOrNull { it.name == name }
                                }
                                val (updated, msg) = WorkPlanEngine.plan(current, slots)
                                regionMessage = msg
                                persist(updated)
                            }
                        },
                        soundEnabled = soundEnabled.value,
                        onSoundEnabledChange = { value ->
                            soundEnabled.value = value
                            saveManager.setSoundEnabled(value)
                        },
                        hapticsEnabled = hapticsEnabled.value,
                        onHapticsEnabledChange = { value ->
                            hapticsEnabled.value = value
                            saveManager.setHapticsEnabled(value)
                        },
                        musicEnabled = musicEnabled.value,
                        onMusicEnabledChange = { value ->
                            musicEnabled.value = value
                            saveManager.setMusicEnabled(value)
                        },
                        musicVolume = musicVolume.value,
                        onMusicVolumeChange = { value ->
                            musicVolume.value = value
                            saveManager.setMusicVolume(value)
                        },
                        sfxVolume = sfxVolume.value,
                        onSfxVolumeChange = { value ->
                            sfxVolume.value = value
                            saveManager.setSfxVolume(value)
                        },
                        onTestSound = { audio.playSfx(GameSound.CLICK) },
                        onReplayTutorial = {
                            tutorialDone = false
                            saveManager.setTutorialDone(false)
                            refresh += 1
                            toast(context, "已重新开启新手引导。")
                        },
                        onResetAllData = {
                            saveManager.clearAll()
                            saveManager.clearArchive()
                            saveManager.setTutorialDone(false)
                            tutorialDone = false
                            state = null
                            secondary = null
                            tab = PlayTab.HOME
                            refresh += 1
                            root = RootScreen.HOME
                            toast(context, "全部数据已重置。")
                        },
                        saveInfo = saveInfo,
                        archive = lifeArchive,
                        galleryProgress = galleryProgress
                    )

                    RootScreen.ARCHIVE -> ArchiveScreen(
                        hasArchive = archiveLines.isNotEmpty(),
                        archiveLines = archiveLines,
                        endingSummary = archiveEnding,
                        gallery = gallery,
                        recentLives = lifeArchive.records,
                        unlockedCount = lifeArchive.unlockedCount,
                        totalCount = EndingCatalog.total,
                        onClearArchive = {
                            saveManager.clearArchive()
                            refresh += 1
                            toast(context, "人生图鉴已清空。")
                        },
                        onBack = { root = RootScreen.HOME }
                    )

                    RootScreen.SETTINGS -> SettingsScreen(
                        darkMode = useDark,
                        onDarkModeChange = { value ->
                            darkOverride = value
                            saveManager.setDarkModeOverride(value)
                        },
                        soundEnabled = soundEnabled.value,
                        onSoundEnabledChange = { value ->
                            soundEnabled.value = value
                            saveManager.setSoundEnabled(value)
                        },
                        hapticsEnabled = hapticsEnabled.value,
                        onHapticsEnabledChange = { value ->
                            hapticsEnabled.value = value
                            saveManager.setHapticsEnabled(value)
                        },
                        musicEnabled = musicEnabled.value,
                        onMusicEnabledChange = { value ->
                            musicEnabled.value = value
                            saveManager.setMusicEnabled(value)
                        },
                        musicVolume = musicVolume.value,
                        onMusicVolumeChange = { value ->
                            musicVolume.value = value
                            saveManager.setMusicVolume(value)
                        },
                        sfxVolume = sfxVolume.value,
                        onSfxVolumeChange = { value ->
                            sfxVolume.value = value
                            saveManager.setSfxVolume(value)
                        },
                        onTestSound = { audio.playSfx(GameSound.CLICK) },
                        onReplayTutorial = {
                            tutorialDone = false
                            saveManager.setTutorialDone(false)
                            refresh += 1
                            toast(context, "已重新开启新手引导。")
                        },
                        onResetAllData = {
                            saveManager.clearAll()
                            saveManager.clearArchive()
                            saveManager.setTutorialDone(false)
                            tutorialDone = false
                            state = null
                            secondary = null
                            tab = PlayTab.HOME
                            refresh += 1
                            root = RootScreen.HOME
                            toast(context, "全部数据已重置。")
                        },
                        saveInfo = saveInfo,
                        hasSave = hasSave,
                        hasManualSave = hasManual,
                        onSaveManual = {
                            state?.let { current ->
                                saveManager.saveManual(current)
                                refresh += 1
                                toast(context, "已保存到手动存档位。")
                            }
                        },
                        onLoadManual = {
                            when (val result = saveManager.loadManual()) {
                                is LoadResult.Success -> {
                                    state = result.state
                                    refresh += 1
                                    toast(context, "已读取手动存档。")
                                }
                                LoadResult.Corrupt -> toast(context, "手动存档已损坏，已忽略。")
                                LoadResult.Empty -> toast(context, "还没有手动存档。")
                            }
                        },
                        onDeleteSave = {
                            saveManager.clearAll()
                            state = null
                            refresh += 1
                            toast(context, "存档已删除。")
                        },
                        onBack = { root = RootScreen.HOME }
                    )
                }

                if (root == RootScreen.PLAY && !tutorialDone && state?.isFinished == false) {
                    TutorialOverlay(
                        onFinish = {
                            tutorialDone = true
                            saveManager.setTutorialDone(true)
                            audio.play(GameSound.PAGE)
                        }
                    )
                }

                achievementBanner?.let { banner ->
                    LaunchedEffect(banner.id) {
                        delay(2600)
                        achievementBanner = null
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Surface(
                            modifier = Modifier.padding(top = 72.dp, start = 24.dp, end = 24.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shadowElevation = 6.dp
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                                Text(
                                    text = "成就解锁",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = banner.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = banner.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

// 从 Compose 的 Context 逐层向上找到宿主 Activity，用于监听生命周期。
private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

// 设置页展示的存档信息。
private fun saveInfoOf(manager: SaveManager): String {
    if (!manager.hasAnySave()) return "当前没有存档，开始新人生后会自动保存。"
    val time = manager.savedAt()
    val stamp = if (time > 0L) {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
            .format(java.util.Date(time))
    } else {
        "未知"
    }
    val backup = if (manager.backupAvailable()) "已保留备份" else "暂无备份"
    return "最近保存 $stamp · 存档格式 v${manager.storedVersion()} · $backup"
}

// 首页的存档摘要。
private fun saveSummaryOf(result: LoadResult): String = when (result) {
    is LoadResult.Success -> {
        val state = result.state
        "${state.player.name} · ${state.displayPositionTitle} · ${state.player.unit.label}\n" +
            "${state.currentProvince} · ${state.currentCityName}（${state.currentCityType}）\n" +
            "第 ${state.year} 年 · 第 ${state.quarter} 季度 · 服务 ${state.player.serviceYears} 年" +
            if (state.ending != null) "\n已结束：${state.ending.title}" else ""
    }
    LoadResult.Corrupt -> "存档读取失败，可在设置中清理。"
    LoadResult.Empty -> ""
}

// 人生档案的履历条目。
private fun archiveLinesOf(result: LoadResult): List<Pair<String, String>> {
    val state = (result as? LoadResult.Success)?.state ?: return emptyList()
    val player = state.player
    return listOf(
        "姓名" to player.name,
        "性别" to player.gender,
        "学历" to player.education,
        "天赋" to player.traits.joinToString("、").ifEmpty { "无" },
        "起点" to player.startTrack,
        "发展路线" to (state.route?.label ?: "未确定"),
        "当前职位" to state.displayPositionTitle,
        "任职城市" to (state.currentProvince + " · " + state.currentCityName),
        "城市层级" to state.currentCityType,
        "城市含权量" to state.currentCityPower.toString(),
        "全国任职城市数" to state.careerCities.size.toString(),
        "工作单位" to player.unit.label,
        "服务年限" to "${player.serviceYears} 年",
        "当前年龄" to "${player.age} 岁",
        "岗位任职" to "${player.yearsInPosition} 年",
        "年度考核" to "${state.reviewCount} 次",
        "晋升次数" to "${state.promotionCount} 次",
        "累计工作实绩" to state.totalWorkGained.toString(),
        "工作实绩" to player.attributes.work.toString(),
        "群众基础" to player.attributes.masses.toString(),
        "个人声望" to player.attributes.reputation.toString(),
        "廉政风险" to player.attributes.integrityRisk.toString(),
        "健康" to player.attributes.health.toString(),
        "家庭关系" to player.attributes.family.toString(),
        "家庭" to player.family.summary,
        "最近事件" to (state.log.lastOrNull()?.title ?: "尚无记录")
    )
}

private fun archiveEndingOf(result: LoadResult): String {
    val ending = (result as? LoadResult.Success)?.state?.ending ?: return ""
    return "${ending.title}（${ending.category}）\n${ending.description}"
}

/** 晋升音乐的持续时长：铺垫—展开—开阔，十秒后自然淡回场景曲。 */
private const val PROMOTION_MOMENT_MS = 10_500L

/** 场景切换去抖：快速翻页时只保留最后一次切换。 */
private const val SCENE_DEBOUNCE_MS = 300L

/** 当前事件等级，仅用于选择音乐场景。 */
private fun currentEventTierOf(state: GameState?): EventTier? =
    state?.let { GameEngine.currentEvent(it)?.effectiveTier }

/** 事件等级 → 音乐线索：危机低沉、重大有层次、普通保持平静。 */
private fun eventCue(tier: EventTier?): MusicCue = when (tier) {
    EventTier.CRISIS -> MusicCue.EVENT_CRISIS
    EventTier.MAJOR, EventTier.IMPORTANT -> MusicCue.EVENT_MAJOR
    else -> MusicCue.EVENT
}

/**
 * 界面状态 → 音乐线索。纯函数，便于单元测试。
 * 二级页面统一使用政务 / 政治场景，避免频繁切换。
 */
internal fun musicCueOf(
    root: RootScreen,
    tab: PlayTab,
    secondary: PlayTab?,
    phase: GamePhase?,
    hasEnding: Boolean,
    tier: EventTier?
): MusicCue = when {
    hasEnding -> MusicCue.ENDING
    root != RootScreen.PLAY -> MusicCue.MENU
    secondary == PlayTab.POLITICS || secondary == PlayTab.TRANSFER ||
        secondary == PlayTab.RELATION -> MusicCue.POLITICS
    secondary != null -> MusicCue.GOVERNANCE
    phase == GamePhase.EVENT || phase == GamePhase.OUTCOME -> eventCue(tier)
    tab == PlayTab.EVENT -> eventCue(tier)
    phase == GamePhase.REVIEW -> MusicCue.HOME
    tab == PlayTab.GOVERNANCE -> MusicCue.GOVERNANCE
    else -> MusicCue.HOME
}