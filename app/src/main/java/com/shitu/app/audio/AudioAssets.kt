package com.shitu.app.audio

/**
 * 第十七阶段：音频资源的统一清单。
 *
 * 引擎与测试共用同一份清单，避免代码与 assets 目录脱节。
 * 资源查找顺序为 ogg → mp3 → m4a → aac → wav：
 * 正式音乐替换进来后，占位 wav 可以直接删掉，不需要改代码。
 */
object AudioAssets {

    /** 支持的扩展名，靠前优先级更高。 */
    val AUDIO_EXTS = listOf("ogg", "mp3", "m4a", "aac", "wav")

    /** 扫描的 assets 子目录。 */
    val ASSET_DIRS = listOf("audio/bgm", "audio/ui", "audio/sfx")

    /** 音效 → assets 基名（不含扩展名）。 */
    val SFX_BASES: Map<GameSound, String> = mapOf(
        GameSound.CLICK to "audio/ui/click",
        GameSound.PAGE to "audio/ui/page",
        GameSound.BACK to "audio/ui/back",
        GameSound.CLOSE to "audio/ui/close",
        GameSound.CONFIRM to "audio/ui/confirm",
        GameSound.TOGGLE to "audio/ui/toggle",
        GameSound.WARNING to "audio/sfx/warning",
        GameSound.SUCCESS to "audio/sfx/success",
        GameSound.FAILURE to "audio/sfx/failure",
        GameSound.EVENT_MAJOR to "audio/sfx/event_major",
        GameSound.PROMOTION to "audio/sfx/promotion",
        GameSound.ACHIEVEMENT to "audio/sfx/achievement",
        GameSound.ENDING to "audio/sfx/ending"
    )

    /** 扩展名优先级，数字越小越优先。 */
    fun rankOf(extension: String): Int {
        val index = AUDIO_EXTS.indexOf(extension.lowercase())
        return if (index < 0) AUDIO_EXTS.size else index
    }
}