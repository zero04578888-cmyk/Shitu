package com.shitu.app.audio

/**
 * 第十七阶段：场景音乐。
 *
 * 每首曲子的资源基名（不含扩展名）与播放参数。
 * 引擎会按 ogg → mp3 → wav 的顺序查找同名资源，
 * 因此后续把占位 wav 换成正式 ogg/mp3 时不需要改动任何代码。
 *
 * @param assetBase assets 目录下的资源基名
 * @param loop 是否循环（晋升曲是一次性的，播完由界面层淡回场景曲）
 * @param gain 该曲的固有增益，用于平衡不同曲目的响度
 */
enum class MusicScene(
    val assetBase: String,
    val loop: Boolean,
    val gain: Float
) {
    /** 主菜单 / 创建角色 / 生涯档案 */
    MENU("audio/bgm/menu", true, 0.85f),

    /** 首页与日常界面：舒适、平静 */
    HOME("audio/bgm/home", true, 1.00f),

    /** 政务处理：轻松、专注 */
    GOVERNANCE("audio/bgm/governance", true, 0.95f),

    /** 普通事件：平静 */
    EVENT("audio/bgm/event", true, 1.00f),

    /** 重大事件与重大决策：更厚的弦乐，克制但有层次 */
    MAJOR("audio/bgm/major", true, 1.00f),

    /** 政治与人事：深沉、克制 */
    POLITICS("audio/bgm/politics", true, 0.95f),

    /** 危机：低沉，但不恐怖 */
    CRISIS("audio/bgm/crisis", true, 1.00f),

    /** 晋升：温暖、开阔，播完即回落到场景曲 */
    PROMOTION("audio/bgm/promotion", false, 1.05f),

    /** 结局 */
    ENDING("audio/bgm/ending", true, 1.00f)
}

/**
 * 界面「音乐线索」：把导航状态翻译成音乐场景的中间层。
 * 与 Compose / 导航实现完全解耦，方便单元测试与后续扩展。
 */
enum class MusicCue {
    MENU,
    HOME,
    GOVERNANCE,
    EVENT,
    EVENT_MAJOR,
    EVENT_CRISIS,
    POLITICS,
    PROMOTION,
    ENDING
}

object MusicSceneResolver {

    /** 线索 → 场景：普通事件平静、重大事件更厚、危机低沉、政治庄重。 */
    fun sceneFor(cue: MusicCue): MusicScene = when (cue) {
        MusicCue.MENU -> MusicScene.MENU
        MusicCue.HOME -> MusicScene.HOME
        MusicCue.GOVERNANCE -> MusicScene.GOVERNANCE
        MusicCue.EVENT -> MusicScene.EVENT
        MusicCue.EVENT_MAJOR -> MusicScene.MAJOR
        MusicCue.EVENT_CRISIS -> MusicScene.CRISIS
        MusicCue.POLITICS -> MusicScene.POLITICS
        MusicCue.PROMOTION -> MusicScene.PROMOTION
        MusicCue.ENDING -> MusicScene.ENDING
    }
}

/**
 * 音频参数的默认值与边界保护。
 *
 * 听感目标：安静、耐听、沉稳。因此默认音量刻意偏低，
 * 背景音乐 30%、界面音效 45%，并且永远不允许超过 100%。
 */
object AudioSettings {

    /** 背景音乐默认音量（25%~35% 是推荐区间）。 */
    const val DEFAULT_MUSIC_VOLUME = 0.30f

    /** 界面与事件音效默认音量（40%~50% 是推荐区间）。 */
    const val DEFAULT_SFX_VOLUME = 0.45f

    /** 交叉淡入淡出时长：1200ms，落在建议的 800~1500ms 区间内。 */
    const val CROSS_FADE_MS = 1200

    /** 暂停 / 恢复时的短渐变，避免硬切。 */
    const val PAUSE_FADE_MS = 320

    fun clampVolume(value: Float): Float = when {
        value.isNaN() -> 0f
        value < 0f -> 0f
        value > 1f -> 1f
        else -> value
    }

    /** 读取存档或旧设置时使用：缺失或非法值一律回落到默认值。 */
    fun sanitizeVolume(value: Float, fallback: Float): Float =
        if (value.isNaN() || value < 0f || value > 1f) clampVolume(fallback) else value
}