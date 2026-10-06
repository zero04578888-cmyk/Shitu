package com.shitu.app.model

/**
 * 官场势力。玩家与每个势力各有一条关系值（-100 ~ +100）。
 *
 * 势力之间是有矛盾的：靠拢一方，另一方自然会有看法。
 * [rivals] 用于表达这种抵消关系，具体数值在 RelationshipEngine 里计算。
 */
enum class Faction(
    val label: String,
    val description: String
) {
    NEUTRAL("中立官员", "不轻易站队的人，谁也不得罪，谁也不深交。"),
    LOCAL("地方势力", "在本地经营多年，讲人情、讲关系、讲资历。"),
    PRAGMATIC("实务派", "看重能不能把事办成，讨厌空话和形式。"),
    REFORM("改革派", "想做事，也愿意为做事担一点风险。"),
    CONSERVATIVE("保守派", "按规矩来，最少出错，也最不容易被追责。"),
    CORE("上级核心圈", "决定干部去向的那一小圈人，离得远，但说话管用。");

    /** 靠拢本势力时，会同步产生反感的势力。 */
    val rivals: Set<Faction>
        get() = when (this) {
            NEUTRAL -> emptySet()
            LOCAL -> setOf(REFORM)
            PRAGMATIC -> setOf(CONSERVATIVE)
            REFORM -> setOf(CONSERVATIVE, LOCAL)
            CONSERVATIVE -> setOf(REFORM, PRAGMATIC)
            CORE -> emptySet()
        }

    companion object {
        fun byName(value: String): Faction? = entries.firstOrNull { it.name == value }
    }
}
