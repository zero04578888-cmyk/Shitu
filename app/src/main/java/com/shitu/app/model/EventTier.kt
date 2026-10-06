package com.shitu.app.model

/**
 * 事件等级：决定界面上的视觉强调程度。
 * 普通 < 重要 < 重大 < 危机，数字只用于排序。
 */
enum class EventTier(val label: String, val order: Int) {
    NORMAL("普通", 0),
    IMPORTANT("重要", 1),
    MAJOR("重大", 2),
    CRISIS("危机", 3);

    /** 重大及以上的事件使用独立的事件页布局。 */
    val isHighlighted: Boolean get() = order >= MAJOR.order
}
