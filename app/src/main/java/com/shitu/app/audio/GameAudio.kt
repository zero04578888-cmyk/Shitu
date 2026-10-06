package com.shitu.app.audio

/**
 * 第十一阶段建立、第十七阶段扩展的统一音效接口。
 *
 * 本作不引入任何第三方音频资源：界面音效为自制的柔和提示音
 * （见 assets/audio），音量很低，只服务于关键节点的反馈。
 * 如果之后要换成正式音效资源，只需要替换实现，界面层不用改。
 */
enum class GameSound {
    /** 普通按钮点击：极短的木质触感。 */
    CLICK,

    /** 页面切换。 */
    PAGE,

    /** 返回。 */
    BACK,

    /** 关闭弹窗。 */
    CLOSE,

    /** 确认选择。 */
    CONFIRM,

    /** 开关切换。 */
    TOGGLE,

    /** 重要 / 重大事件。 */
    EVENT_MAJOR,

    /** 提醒（风险、警告），克制不刺耳。 */
    WARNING,

    /** 一般成功。 */
    SUCCESS,

    /** 一般失败。 */
    FAILURE,

    /** 晋升。 */
    PROMOTION,

    /** 成就解锁。 */
    ACHIEVEMENT,

    /** 结局。 */
    ENDING
}

interface GameAudio {
    fun play(sound: GameSound)
    fun release()

    companion object {
        val NONE: GameAudio = object : GameAudio {
            override fun play(sound: GameSound) = Unit
            override fun release() = Unit
        }
    }
}