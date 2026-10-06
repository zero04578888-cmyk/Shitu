package com.shitu.app.audio

/**
 * 第十七阶段：统一的音乐 / 音效管理器接口。
 *
 * 实现必须满足：
 * - 资源缺失时静默降级，绝不抛异常影响游戏流程；
 * - 切场景用交叉淡入淡出，不做硬切；
 * - 进入后台暂停或降音，回到前台平滑恢复。
 */
interface GameMusic : GameAudio {

    /** 立即（带短渐变）切到某个场景，等价于 [crossFadeTo]。 */
    fun playBgm(scene: MusicScene)

    /** 淡出并停止背景音乐。 */
    fun stopBgm()

    /** 交叉淡入淡出到目标场景。 */
    fun crossFadeTo(scene: MusicScene, durationMs: Int = AudioSettings.CROSS_FADE_MS)

    /** 播放一个界面 / 事件音效。 */
    fun playSfx(sound: GameSound)

    /** 背景音乐总开关（关闭即静音音乐，不影响音效开关）。 */
    fun setMusicEnabled(enabled: Boolean)

    /** 背景音乐音量，自动夹取到 0f..1f。 */
    fun setMusicVolume(volume: Float)

    /** 音效音量，自动夹取到 0f..1f。 */
    fun setSfxVolume(volume: Float)

    /** 进入后台：平滑降音后暂停。 */
    fun pauseMusic()

    /** 回到前台：平滑恢复到当前场景。 */
    fun resumeMusic()

    override fun play(sound: GameSound) = playSfx(sound)

    companion object {
        val NONE: GameMusic = object : GameMusic {
            override fun playBgm(scene: MusicScene) = Unit
            override fun stopBgm() = Unit
            override fun crossFadeTo(scene: MusicScene, durationMs: Int) = Unit
            override fun playSfx(sound: GameSound) = Unit
            override fun setMusicEnabled(enabled: Boolean) = Unit
            override fun setMusicVolume(volume: Float) = Unit
            override fun setSfxVolume(volume: Float) = Unit
            override fun pauseMusic() = Unit
            override fun resumeMusic() = Unit
            override fun release() = Unit
        }
    }
}