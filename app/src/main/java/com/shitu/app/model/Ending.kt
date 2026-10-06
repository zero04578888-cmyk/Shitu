package com.shitu.app.model

// 结局。结局由整局游戏的数据综合判定，不依赖最后一次选择。
data class Ending(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val highlights: List<Pair<String, String>>,
    /** 结局等级：优 / 良 / 中 / 憾 / 败 / 隐藏。 */
    val tier: String = "",
    /** 判定的关键原因。 */
    val reasons: List<String> = emptyList(),
    /** 影响一生的关键履历节点。 */
    val keyNodes: List<String> = emptyList(),
    /** 影响一生的几个选择。 */
    val keyChoices: List<String> = emptyList()
)
