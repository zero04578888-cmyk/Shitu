package com.shitu.app.model

/**
 * 结局图鉴条目。
 *
 * [id] 必须与 EndingEngine 产出的结局 id 一致，
 * [hint] 在未解锁时替代描述展示，避免提前剧透。
 */
data class EndingCatalogEntry(
    val id: String,
    val title: String,
    val category: String,
    val hint: String
)

/** 图鉴中的一个条目加上玩家的收集进度。 */
data class EndingGalleryItem(
    val entry: EndingCatalogEntry,
    val count: Int
) {
    val unlocked: Boolean get() = count > 0
}

/**
 * 全部可收集结局。顺序即图鉴中的展示顺序：先职业发展，再类型与口碑，
 * 最后是风险与人生事故。
 */
object EndingCatalog {

    val entries: List<EndingCatalogEntry> = listOf(
        EndingCatalogEntry("fast_rise", "快速晋升", "晋升", "很早就走到了高位"),
        EndingCatalogEntry("steady_rise", "稳步晋升", "晋升", "没有跳跃，也没有明显失误"),
        EndingCatalogEntry("specialist", "专业型干部", "类型", "把一件事做到别人难以替代"),
        EndingCatalogEntry("manager", "管理型干部", "类型", "擅长把人和事组织起来"),
        EndingCatalogEntry("behind_scenes", "幕后智囊", "类型", "习惯站在灯光之外"),
        EndingCatalogEntry("balanced_life", "平衡人生", "类型", "工作之外，生活还在"),
        EndingCatalogEntry("people_first", "一心为民", "口碑", "很多普通人记得你的名字"),
        EndingCatalogEntry("mass_recognition", "群众认可", "口碑", "走到哪里都有人愿意和你说心里话"),
        EndingCatalogEntry("high_reputation", "声望极高", "声望", "在本系统内被反复提起"),
        EndingCatalogEntry("grassroots", "基层干部", "职业", "一辈子留在一个地方"),
        EndingCatalogEntry("stagnation", "职业停滞", "职业", "很多年，职级几乎没有变化"),
        EndingCatalogEntry("frustration", "事业受挫", "职业", "被安排到不再重要的岗位"),
        EndingCatalogEntry("normal_retire", "正常退休", "职业", "平稳的三十年"),
        EndingCatalogEntry("business_turn", "商海转身", "转折", "中途离开，去走另一条路"),
        EndingCatalogEntry("integrity_fail", "廉政风险失败", "纪律", "当年看起来「没什么」的选择"),
        EndingCatalogEntry("health_ruin", "健康透支", "身体", "身体比履历更早撑不住"),
        EndingCatalogEntry("family_break", "家庭破裂", "家庭", "回过头时，家里已经没有人等你"),
        EndingCatalogEntry("total_fail", "最终失败", "职业", "职业生涯被提前结束"),
        EndingCatalogEntry("pragmatic_achiever", "实干型干部", "路线", "不太说话，但事情都办成了"),
        EndingCatalogEntry("reform_pioneer", "改革型干部", "路线", "一直在试新办法"),
        EndingCatalogEntry("steady_statesman", "稳健型干部", "路线", "很多年都没有出过事"),
        EndingCatalogEntry("grassroots_governor", "基层治理型", "职业", "把一个地方从里到外摸透了"),
        EndingCatalogEntry("regretful_success", "遗憾的成功", "人生", "履历很好看，代价只有自己知道"),
        EndingCatalogEntry("midway_turn", "转折人生", "人生", "中途换过方向"),
        EndingCatalogEntry("pinnacle", "高峰型仕途", "晋升", "走到很高，而且站得住"),
        EndingCatalogEntry("ordinary_life", "平凡人生", "人生", "普通，但完整"),
        EndingCatalogEntry("hidden_legacy", "传奇人生", "隐藏", "条件和结局都未公开")
    )

    private val byId: Map<String, EndingCatalogEntry> = entries.associateBy { it.id }

    val total: Int get() = entries.size

    fun find(id: String): EndingCatalogEntry? = byId[id]

    /** 把收集记录整理成图鉴展示列表。 */
    fun galleryOf(archive: LifeArchive): List<EndingGalleryItem> = entries.map { entry ->
        EndingGalleryItem(entry = entry, count = archive.countOf(entry.id))
    }
}
