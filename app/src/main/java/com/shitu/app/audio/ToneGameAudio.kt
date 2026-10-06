package com.shitu.app.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * 基于系统提示音的极简实现，仅作为音频资源不可用时的兜底。
 * [enabled] 由设置项控制；任何异常都不会影响游戏流程。
 */
class ToneGameAudio(private val enabled: () -> Boolean) : GameAudio {

    private var tone: ToneGenerator? = null

    override fun play(sound: GameSound) {
        if (!enabled()) return
        val (type, duration) = toneOf(sound)
        try {
            val generator = tone ?: ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME).also { tone = it }
            generator.startTone(type, duration)
        } catch (error: Exception) {
            // 部分设备没有提示音通道，静默失败即可。
            Log.w(TAG, "播放音效失败：${sound.name}", error)
        }
    }

    override fun release() {
        try {
            tone?.release()
        } catch (error: Exception) {
            Log.w(TAG, "释放音效通道失败", error)
        } finally {
            tone = null
        }
    }

    private fun toneOf(sound: GameSound): Pair<Int, Int> = when (sound) {
        GameSound.CLICK -> ToneGenerator.TONE_PROP_BEEP to 40
        GameSound.TOGGLE -> ToneGenerator.TONE_PROP_BEEP to 35
        GameSound.PAGE -> ToneGenerator.TONE_PROP_BEEP2 to 50
        GameSound.BACK -> ToneGenerator.TONE_PROP_BEEP2 to 45
        GameSound.CLOSE -> ToneGenerator.TONE_PROP_BEEP2 to 45
        GameSound.CONFIRM -> ToneGenerator.TONE_PROP_ACK to 90
        GameSound.EVENT_MAJOR -> ToneGenerator.TONE_PROP_BEEP2 to 90
        GameSound.WARNING -> ToneGenerator.TONE_PROP_NACK to 120
        GameSound.SUCCESS -> ToneGenerator.TONE_PROP_ACK to 120
        GameSound.FAILURE -> ToneGenerator.TONE_PROP_NACK to 150
        GameSound.PROMOTION -> ToneGenerator.TONE_PROP_ACK to 160
        GameSound.ACHIEVEMENT -> ToneGenerator.TONE_PROP_ACK to 120
        GameSound.ENDING -> ToneGenerator.TONE_PROP_BEEP2 to 200
    }

    private companion object {
        const val TAG = "GameAudio"
        const val VOLUME = 45
    }
}