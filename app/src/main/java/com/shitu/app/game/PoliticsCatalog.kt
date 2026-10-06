package com.shitu.app.game

import com.shitu.app.model.Alliance
import com.shitu.app.model.CrisisState
import com.shitu.app.model.Faction
import com.shitu.app.model.IntelItem
import com.shitu.app.model.IntelLevel

/** 派系的静态设定：理念、关注领域、当前目标、核心人物。 */
data class FactionInfo(
    val faction: Faction,
    val ideology: String,
    val focus: List<String>,
    val goal: String,
    val coreNpcId: String?
)

object FactionCatalog {
    val infos: List<FactionInfo> = listOf(
        FactionInfo(
            Faction.LOCAL, "讲人情、讲资历，先稳住基本盘。",
            listOf("民生", "基层", "稳定"), "守住本地话语权", "chen_jianguo"
        ),
        FactionInfo(
            Faction.PRAGMATIC, "看结果，谁能把事办成谁就上。",
            listOf("经济", "项目", "效率"), "把经济指标做上去", "ma_wenbin"
        ),
        FactionInfo(
            Faction.REFORM, "愿意为做事担风险，推新办法。",
            listOf("改革", "创新", "环保"), "推动一项有争议的改革", "gao_zhiqiang"
        ),
        FactionInfo(
            Faction.CONSERVATIVE, "按规矩来，先保证不出事。",
            listOf("稳定", "合规", "风险控制"), "避免出现重大失误", "sun_dehai"
        ),
        FactionInfo(
            Faction.CORE, "上级的意志决定干部去向。",
            listOf("上级评价", "考察", "人事"), "维持上级对本地班子的认可", "zhou_huilan"
        ),
        FactionInfo(
            Faction.NEUTRAL, "不轻易站队，谁也不深交。",
            listOf("平衡"), "在各方之间保持回旋", null
        )
    )

    fun infoOf(faction: Faction): FactionInfo = infos.first { it.faction == faction }
}

/** 可获取的情报。 */
object IntelCatalog {
    val items: List<IntelItem> = listOf(
        IntelItem(
            "intel_vacancy", "岗位酝酿调整",
            "组织部门正在酝酿一次干部调整，一个高一级的岗位可能出现空缺。",
            IntelLevel.KEY
        ),
        IntelItem(
            "intel_leader_focus", "上级关注重点",
            "上级近期重点看经济指标和项目落地，汇报时要拿得出数据。",
            IntelLevel.IMPORTANT
        ),
        IntelItem(
            "intel_project", "重点项目将启动",
            "一个重点项目即将启动，牵头单位还没有最终定下来。",
            IntelLevel.COMMON
        ),
        IntelItem(
            "intel_faction_recruit", "有人在找人",
            "有势力正在物色可靠的人，愿意给机会，也需要你表个态。",
            IntelLevel.IMPORTANT
        ),
        IntelItem(
            "intel_peer_move", "同层动向",
            "同层的几位同事最近汇报得比平时勤，节奏有些不一样。",
            IntelLevel.IMPORTANT
        ),
        IntelItem(
            "intel_inspection", "专项检查计划",
            "上级专项检查的时间大致定了，主题是民生资金使用。",
            IntelLevel.KEY
        ),
        IntelItem(
            "intel_mentor_hint", "老领导的提醒",
            "老领导提醒你，风头正盛的时候要收着点，别把人都得罪了。",
            IntelLevel.COMMON
        ),
        IntelItem(
            "intel_hidden_issue", "一件旧事",
            "有人手里握着一件旧事，平时不提，关键时候可能被翻出来。",
            IntelLevel.KEY
        )
    )

    private val byId: Map<String, IntelItem> = items.associateBy { it.id }

    fun find(id: String): IntelItem? = byId[id]
}

/** 联盟模板。durationQuarters 是持续季度数。 */
data class AllianceTemplate(
    val id: String,
    val label: String,
    val npcId: String?,
    val faction: Faction?,
    val durationQuarters: Int,
    val note: String
)

object AllianceCatalog {
    val templates: List<AllianceTemplate> = listOf(
        AllianceTemplate(
            "ally_pragmatic", "与实务派合作", null, Faction.PRAGMATIC, 8,
            "一起把项目做出来，收益共享，风险也共享。"
        ),
        AllianceTemplate(
            "ally_reform", "与改革派联手", null, Faction.REFORM, 8,
            "共同推动一项有争议的改革，成败都记在两个人头上。"
        ),
        AllianceTemplate(
            "ally_local", "与地方势力结盟", null, Faction.LOCAL, 8,
            "交换人情与资源，短期内好办事，长期要还。"
        ),
        AllianceTemplate(
            "ally_mentor", "老领导旧部", "sun_dehai", Faction.CONSERVATIVE, 6,
            "借老领导的人脉过渡一段时间，但会被贴上标签。"
        )
    )

    private val byId: Map<String, AllianceTemplate> = templates.associateBy { it.id }

    fun find(id: String): AllianceTemplate? = byId[id]

    fun toAlliance(template: AllianceTemplate, absoluteQuarter: Int): Alliance = Alliance(
        id = template.id,
        label = template.label,
        npcId = template.npcId,
        faction = template.faction,
        untilQuarter = absoluteQuarter + template.durationQuarters,
        note = template.note
    )
}

/** 政治危机模板。 */
object CrisisCatalog {
    val templates: Map<String, CrisisState> = listOf(
        CrisisState("crisis_accident", "重大工作失误", "一项重点工作出了纰漏，上面开始追问责任。", 3, 0),
        CrisisState("crisis_opinion", "舆论危机", "一段现场视频被传到网上，评论越来越难听。", 2, 0),
        CrisisState("crisis_doubt", "上级质疑", "主要领导在会上点了你的名，语气不太客气。", 2, 0),
        CrisisState("crisis_faction", "派系冲突", "两个势力因为一个人事安排顶上了，你被夹在中间。", 3, 0),
        CrisisState("crisis_integrity", "廉政问询", "有人递了一份材料上去，提到你在项目审批里的处置，需要说清楚。", 3, 0),
        CrisisState("crisis_project", "项目争议", "你主推的项目被质疑，支持与反对的声音都很大。", 2, 0)
    ).associateBy { it.id }

    fun find(id: String): CrisisState? = templates[id]
}


