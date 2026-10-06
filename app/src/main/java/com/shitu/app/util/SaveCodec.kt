package com.shitu.app.util

import com.shitu.app.game.GameSave
import com.shitu.app.game.GameState

/**
 * 第十五阶段：存档信封编解码。
 *
 * 在 [GameCodec] 的游戏状态正文之前，加入一小段元信息：
 * 信封版本、开局时间（startedAt）、最后保存时间（savedAt）。
 *
 * 兼容策略：
 * - 旧存档没有信封标记，直接按纯正文解析，元信息使用安全默认值（0 / 当前版本）。
 * - 信封只解析到正文第一行为止，正文格式仍由 [GameCodec] 负责，互不干扰。
 * - 任何解析异常都返回 null，由调用方回退到备份或安全默认值，绝不抛出。
 */
object SaveCodec {

    /** 当前信封版本。升级信封结构时递增。 */
    const val ENVELOPE_VERSION: Int = GameSave.DEFAULT_VERSION

    private const val MARK = "#SHITU_SAVE"
    private const val SEP = '\t'

    fun encode(save: GameSave): String {
        val sb = StringBuilder()
        sb.append(MARK).append('\n')
        sb.append("envelope").append(SEP).append(ENVELOPE_VERSION).append('\n')
        sb.append("startedAt").append(SEP).append(save.startedAt).append('\n')
        sb.append("savedAt").append(SEP).append(save.savedAt).append('\n')
        sb.append(GameCodec.encode(save.state))
        return sb.toString()
    }

    fun decode(text: String): GameSave? = try {
        if (!text.startsWith(MARK)) {
            // 旧存档：没有信封，正文即 GameCodec 输出。
            GameCodec.decode(text)?.let { GameSave(state = it) }
        } else {
            decodeEnvelope(text)
        }
    } catch (error: Exception) {
        null
    }

    private fun decodeEnvelope(text: String): GameSave? {
        val lines = text.split('\n')
        var index = 1
        var version = ENVELOPE_VERSION
        var startedAt = 0L
        var savedAt = 0L
        while (index < lines.size) {
            val line = lines[index]
            val value = line.substringAfter(SEP, "")
            when {
                line.startsWith("envelope") -> version = value.toIntOrNull() ?: ENVELOPE_VERSION
                line.startsWith("startedAt") -> startedAt = value.toLongOrNull() ?: 0L
                line.startsWith("savedAt") -> savedAt = value.toLongOrNull() ?: 0L
                else -> break
            }
            index++
        }
        val body = lines.drop(index).joinToString("\n")
        val state: GameState = GameCodec.decode(body) ?: return null
        return GameSave(version = version, startedAt = startedAt, savedAt = savedAt, state = state)
    }
}