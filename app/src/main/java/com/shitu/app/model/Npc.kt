package com.shitu.app.model

/** 人物层级，决定他能帮上什么忙、在什么场合出现。 */
enum class NpcTier(val label: String) {
    COUNTY_LEADER("县领导"),
    CITY_LEADER("市领导"),
    ORGANIZATION("组织部门"),
    COLLEAGUE("同僚"),
    SUBORDINATE("下属"),
    BUSINESS("企业"),
    MENTOR("老领导")
}

/**
 * 人物的静态设定。姓名、官职、势力这些不会变的信息放在这里，
 * 会随游戏变化的关系值与信任度存在 [RelationshipState] 中。
 */
data class NpcProfile(
    val id: String,
    val name: String,
    val title: String,
    val age: Int,
    val faction: Faction,
    val tier: NpcTier,
    val influence: Int,
    val personality: List<String>,
    val description: String
) {
    val personalityLabel: String get() = personality.joinToString("、")
}

/** 人物对玩家的态度，由关系值区间决定。 */
enum class NpcAttitude(val label: String) {
    HOSTILE("敌对"),
    COLD("不和"),
    NEUTRAL("普通"),
    FRIENDLY("友好"),
    CLOSE("亲近");

    companion object {
        fun of(relation: Int): NpcAttitude = when {
            relation <= -60 -> HOSTILE
            relation <= -20 -> COLD
            relation < 20 -> NEUTRAL
            relation < 60 -> FRIENDLY
            else -> CLOSE
        }
    }
}

/**
 * 人物的动态状态。会随着剧情变化，并直接影响这个人说话的份量
 * （[influenceDelta] 会加到静态影响力上）。
 */
enum class NpcStatus(val label: String, val influenceDelta: Int) {
    NORMAL("正常", 0),
    TRUSTED("受重用", 10),
    DISTANT("被冷落", -10),
    INVESTIGATED("被调查", -20),
    PROMOTED("已高升", 15),
    TRANSFERRED("已调离", -25);

    /** 已经说不上话的状态，帮不了忙。 */
    val outOfReach: Boolean get() = this == TRANSFERRED || this == INVESTIGATED

    companion object {
        fun byName(value: String?): NpcStatus? = entries.firstOrNull { it.name == value }

        fun of(value: String?): NpcStatus = byName(value) ?: NORMAL
    }
}
