package com.shitu.app.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * 基于系统震动服务的轻量实现。
 * 只在关键节点做很短的一次震动；设备不支持或没有权限时静默跳过，绝不影响游戏流程。
 */
class AndroidGameHaptics(
    context: Context,
    private val enabled: () -> Boolean
) : GameHaptics {

    private val vibrator: Vibrator? = try {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (error: Exception) {
        Log.w(TAG, "无法获取震动服务", error)
        null
    }

    override fun play(kind: HapticKind) {
        if (!enabled()) return
        val device = vibrator ?: return
        try {
            if (!device.hasVibrator()) return
            device.vibrate(VibrationEffect.createOneShot(durationOf(kind), amplitudeOf(kind)))
        } catch (error: Exception) {
            Log.w(TAG, "震动失败：${kind.name}", error)
        }
    }

    override fun release() = Unit

    private fun durationOf(kind: HapticKind): Long = when (kind) {
        HapticKind.TAP -> 12L
        HapticKind.CONFIRM -> 20L
        HapticKind.WARN -> 30L
        HapticKind.PROMOTION -> 40L
        HapticKind.ACHIEVEMENT -> 35L
        HapticKind.ENDING -> 60L
    }

    private fun amplitudeOf(kind: HapticKind): Int = when (kind) {
        HapticKind.TAP -> 40
        HapticKind.CONFIRM -> 60
        HapticKind.WARN -> 90
        HapticKind.PROMOTION -> 110
        HapticKind.ACHIEVEMENT -> 100
        HapticKind.ENDING -> 120
    }

    private companion object {
        const val TAG = "GameHaptics"
    }
}