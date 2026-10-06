package com.shitu.app.audio

/**
 * 第十四阶段：统一的震动反馈接口。
 *
 * 与音效一样，先在逻辑层建立干净接口，具体实现可替换。
 * 只在关键节点轻震一下，不做持续或强烈的震动。
 */
enum class HapticKind {
    TAP,
    CONFIRM,
    WARN,
    PROMOTION,
    ACHIEVEMENT,
    ENDING
}

interface GameHaptics {
    fun play(kind: HapticKind)
    fun release()

    companion object {
        val NONE: GameHaptics = object : GameHaptics {
            override fun play(kind: HapticKind) = Unit
            override fun release() = Unit
        }
    }
}