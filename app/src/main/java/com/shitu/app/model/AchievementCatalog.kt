package com.shitu.app.model

/**
 * 成就图鉴。成就以「解锁 id 集合」的形式进入存档与人生档案。
 * 未解锁时只显示标题与提示，不提前透露触发条件。
 */
object AchievementCatalog {

    val entries: List<AchievementDef> = listOf(
        AchievementDef("first_promotion", "初次晋升", "第一次获得职务上的进步。", 1),
        AchievementDef("decade_service", "十年磨一剑", "完整走过十年仕途。", 1),
        AchievementDef("thirty_years", "三十年仕途", "把整段职业生涯走完。", 2),
        AchievementDef("top_position", "位极人臣", "走到本作最高的职级。", 3),
        AchievementDef("merit_master", "政绩卓著", "综合政绩达到很高水平。", 2),
        AchievementDef("approval_high", "深得民心", "民望达到很高水平。", 2),
        AchievementDef("clean_record", "两袖清风", "任职多年始终保持低风险。", 3),
        AchievementDef("stress_free", "张弛有度", "把工作压力长期控制在低位。", 2),
        AchievementDef("family_first", "家和人安", "把家庭关系经营得很好。", 1),
        AchievementDef("balanced_master", "平衡之道", "事业、家庭与健康都没有落下。", 2),
        AchievementDef("reform_pioneer", "改革先锋", "长期推动新办法、愿意担风险。", 2),
        AchievementDef("steady_hand", "稳健如山", "长期把不出事放在第一位。", 2),
        AchievementDef("pragmatist", "实干为本", "长期靠做成事说话。", 2),
        AchievementDef("network_weaver", "广结善缘", "与多位关键人物保持良好关系。", 2),
        AchievementDef("faction_backing", "派系支持", "获得某个势力的高度认可。", 2),
        AchievementDef("crisis_survivor", "转危为机", "成功化解一场政治危机。", 3),
        AchievementDef("intel_master", "消息灵通", "手里同时掌握多条关键情报。", 1),
        AchievementDef("ally_builder", "盟友相助", "通过联盟获得支持。", 1),
        AchievementDef("decision_maker", "抉择者", "做出多次重大抉择。", 2),
        AchievementDef("record_keeper", "履历丰厚", "履历上留下大量重要经历。", 2),
        AchievementDef("ability_peak", "能力登顶", "把个人能力提升到最高等级。", 3),
        AchievementDef("veteran_posts", "转战多岗", "在多个岗位上留下履历。", 2),
        AchievementDef("restart_life", "重开人生", "在第一段人生之后再走一遍仕途。", 2),
        AchievementDef("all_rounder", "多面手", "履历覆盖多种不同类型的经历。", 2),
        AchievementDef("ending_collector", "结局收藏家", "在人生档案中解锁多个不同结局。", 3)
    )

    private val byId: Map<String, AchievementDef> = entries.associateBy { it.id }

    val total: Int get() = entries.size

    fun find(id: String): AchievementDef? = byId[id]

    /** 按解锁状态生成展示列表：未解锁的排在后面。 */
    fun itemsOf(unlocked: Set<String>): List<AchievementItem> = entries
        .map { AchievementItem(it, it.id in unlocked) }
        .sortedWith(compareByDescending<AchievementItem> { it.unlocked }.thenBy { it.def.tier })

    val META_COLLECTOR = "ending_collector"
}
