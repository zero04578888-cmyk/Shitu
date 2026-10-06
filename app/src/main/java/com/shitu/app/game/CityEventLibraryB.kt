package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.EventTag
import com.shitu.app.model.GameEvent
import com.shitu.app.model.GovernanceEffect
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind

/**
 * 第十六阶段：城市专属事件池（二）。全部为虚构事件设定。
 */
private fun cityGovB(
    ind: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    risk: Int = 0
) = GovernanceEffect(indicators = ind, merit = merit, finance = finance, risk = risk)

internal val cityEventLibraryB: List<GameEvent> = listOf(

    ev(
        id = "city16_university", title = "高校科研成果转化",
        background = "本地高校有成果待转化",
        description = "几所高校手里有一批可转化成果，缺的是中试平台与产业对接的通道。",
        tags = tags(EventTag.RESEARCH, EventTag.PROJECT, EventTag.MEETING),
        weight = 12, repeatable = true, cooldown = 16, minPos = 4,
        cityTags = setOf("education", "technology", "innovation", "defense"),
        choices = listOf(
            c(
                "共建中试基地，打通转化链条", Attitude.STEADY,
                "中试基地挂牌运行，第一批成果进入产业化通道，校地合作热了起来。",
                AttrDelta(work = 5, ability = 3, coordination = 3),
                HiddenDelta(opportunity = 4, network = 3),
                governance = cityGovB(
                    ind = mapOf(Indicator.EDUCATION to 3, Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.INNOVATION to 5), finance = -4
                )
            ),
            c(
                "政府牵头，直接把成果对接给企业", Attitude.BOLD,
                "对接会开得热闹，几项成果当场签约，但产业化还需要时间检验。",
                AttrDelta(work = 4, coordination = 4, leaderTrust = 2),
                governance = cityGovB(merit = mapOf(MeritKind.INNOVATION to 3, MeritKind.ECONOMIC to 2))
            ),
            c(
                "让市场自己对接，政府少干预", Attitude.CAUTIOUS,
                "你没有额外投入，转化进展缓慢，高校与企业各干各的。",
                AttrDelta(politics = 2, ability = 1),
                governance = cityGovB(merit = mapOf(MeritKind.INNOVATION to -1))
            )
        )
    ),

    ev(
        id = "city16_old_town", title = "历史街区保护与更新",
        background = "老城区既要保护又要改善民生",
        description = "历史街区里房屋老旧、管线老化，居民盼着改造，但一动就可能触碰保护红线。",
        tags = tags(EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.PETITION),
        weight = 12, repeatable = true, cooldown = 18, minPos = 2,
        cityTags = setOf("history", "culture", "tourism"),
        choices = listOf(
            c(
                "微改造，保留街巷肌理", Attitude.STEADY,
                "改造以修缮为主，居民住得舒服了，老街的味道也留住了。",
                AttrDelta(work = 4, masses = 4, coordination = 3),
                governance = cityGovB(
                    ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 3, Indicator.INFRASTRUCTURE to 2),
                    merit = mapOf(MeritKind.LIVELIHOOD to 4), finance = -4
                )
            ),
            c(
                "整体腾退，统一开发运营", Attitude.BOLD,
                "街区面貌焕然一新，文旅收入上来了，但一部分老住户搬走了，意见不小。",
                AttrDelta(work = 5, ability = 3, masses = -5, reputation = 2),
                HiddenDelta(riskPressure = 4),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 3, Indicator.APPROVAL to -2),
                    merit = mapOf(MeritKind.ECONOMIC to 5), finance = -6, risk = 3
                )
            ),
            c(
                "先做保护规划，暂缓动工", Attitude.CAUTIOUS,
                "规划做得很细，但居民对迟迟不动工有意见。",
                AttrDelta(writing = 3, politics = 2, masses = -2),
                governance = cityGovB(ind = mapOf(Indicator.APPROVAL to -2))
            )
        )
    ),

    ev(
        id = "city16_tourism_peak", title = "旅游旺季保障",
        background = "旺季客流远超承载能力",
        description = "假期客流创下新高，景区周边交通、住宿、食品安全压力同时上来。",
        tags = tags(EventTag.EMERGENCY, EventTag.PUBLIC_OPINION, EventTag.COORDINATION),
        weight = 13, repeatable = true, cooldown = 12, minPos = 2,
        cityTags = setOf("tourism", "culture", "consumption", "ocean"),
        choices = listOf(
            c(
                "限流预约，同步做好服务保障", Attitude.STEADY,
                "预约限流让体验平稳，游客满意度没有掉，但短期收入少了一块。",
                AttrDelta(work = 4, coordination = 4, masses = 3),
                governance = cityGovB(
                    ind = mapOf(Indicator.SECURITY to 3, Indicator.APPROVAL to 3),
                    merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.SECURITY to 2)
                )
            ),
            c(
                "全员上岗，把客流全接住", Attitude.BOLD,
                "收入创了纪录，但拥堵与投诉也随之而来，安全压力很大。",
                AttrDelta(work = 5, energy = -8, masses = -3),
                HiddenDelta(riskPressure = 6, stress = 6),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 4, Indicator.SECURITY to -3),
                    merit = mapOf(MeritKind.ECONOMIC to 4), risk = 5
                )
            ),
            c(
                "依靠市场调节，政府只做应急兜底", Attitude.CAUTIOUS,
                "你没有过多干预，问题不算大，但游客体验一般，口碑平平。",
                AttrDelta(politics = 2, work = 1),
                governance = cityGovB(merit = mapOf(MeritKind.ECONOMIC to 1))
            )
        )
    ),

    ev(
        id = "city16_duty_free", title = "免税消费扩容",
        background = "免税与首店经济持续升温",
        description = "一批品牌首店与免税业态希望扩容，涉及审批、监管与消费环境的整体提升。",
        tags = tags(EventTag.PROJECT, EventTag.COORDINATION, EventTag.LEADER_TASK),
        weight = 10, repeatable = true, cooldown = 20, minPos = 4,
        cityTags = setOf("consumption", "tourism", "international", "commerce"),
        choices = listOf(
            c(
                "优化审批，打造消费新场景", Attitude.BOLD,
                "首店与免税业态加速落地，消费数据明显回升，但监管压力同步上升。",
                AttrDelta(work = 5, coordination = 3, ability = 2),
                HiddenDelta(riskPressure = 3),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 3),
                    merit = mapOf(MeritKind.ECONOMIC to 5), finance = -3
                )
            ),
            c(
                "稳慎推进，先把监管规则立起来", Attitude.STEADY,
                "你先立规矩再放开，节奏平稳，企业也认可这种确定性。",
                AttrDelta(work = 4, politics = 3, writing = 2),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.FINANCE to 2)
                )
            ),
            c(
                "维持现状，不额外加码", Attitude.CAUTIOUS,
                "你没有推动扩容，消费热度慢慢被周边城市分流。",
                AttrDelta(politics = 2),
                governance = cityGovB(merit = mapOf(MeritKind.ECONOMIC to -1))
            )
        )
    ),

    ev(
        id = "city16_chem_safety", title = "化工园区安全整治",
        background = "化工园区需要安全专项整治",
        description = "上级部署化工园区安全整治，辖区内几家老企业设备老化，整改要投入真金白银。",
        tags = tags(EventTag.INSPECTION, EventTag.EMERGENCY, EventTag.ACCIDENT),
        weight = 12, repeatable = true, cooldown = 16, minPos = 4,
        cityTags = setOf("chemicals", "industry", "manufacturing", "energy"),
        choices = listOf(
            c(
                "限期整改，不达标的坚决停产", Attitude.BOLD,
                "两家企业被停产整顿，短期税收受影响，但安全底线守住了。",
                AttrDelta(work = 5, politics = 3, leaderTrust = 2, integrityRisk = -2),
                governance = cityGovB(
                    ind = mapOf(Indicator.SECURITY to 4, Indicator.ENVIRONMENT to 2),
                    merit = mapOf(MeritKind.SECURITY to 5), finance = -5
                )
            ),
            c(
                "分类处置，边生产边整改", Attitude.STEADY,
                "你按风险等级分类推进，企业分批整改，生产基本没停。",
                AttrDelta(work = 4, coordination = 3, ability = 2),
                governance = cityGovB(
                    ind = mapOf(Indicator.SECURITY to 2),
                    merit = mapOf(MeritKind.SECURITY to 3, MeritKind.ECONOMIC to 2)
                )
            ),
            c(
                "以帮扶为主，给企业留出时间", Attitude.CAUTIOUS,
                "企业松了口气，但整改进度偏慢，检查时被上级点了名。",
                AttrDelta(politics = 2, leaderTrust = -2, work = 1),
                HiddenDelta(riskPressure = 4),
                governance = cityGovB(risk = 4)
            )
        )
    ),

    ev(
        id = "city16_steel_swap", title = "钢铁产能置换",
        background = "钢铁产能置换与升级",
        description = "老钢铁基地迎来产能置换窗口，退与进都关系着就业与税收。",
        tags = tags(EventTag.PROJECT, EventTag.COORDINATION, EventTag.LEADER_TASK),
        weight = 11, repeatable = true, cooldown = 20, minPos = 4,
        cityTags = setOf("manufacturing", "industry", "resources", "energy"),
        choices = listOf(
            c(
                "压减落后产能，引进高端产线", Attitude.BOLD,
                "落后产能如期退出，新的高端产线开工，阵痛明显但方向对了。",
                AttrDelta(work = 6, ability = 4, leaderTrust = 3, masses = -3),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 2, Indicator.ENVIRONMENT to 3),
                    merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.INNOVATION to 3), finance = -5
                )
            ),
            c(
                "分步实施，先保就业再谈升级", Attitude.STEADY,
                "你把职工安置放在前面，产能置换稳妥推进，速度慢但阻力小。",
                AttrDelta(work = 4, masses = 3, coordination = 3),
                governance = cityGovB(
                    ind = mapOf(Indicator.LIVELIHOOD to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.LIVELIHOOD to 2)
                )
            ),
            c(
                "维持现有产能，等待政策明确", Attitude.CAUTIOUS,
                "你没有主动调整，企业继续按老路子生产，转型压力被往后拖。",
                AttrDelta(politics = 2, work = -2),
                governance = cityGovB(ind = mapOf(Indicator.ENVIRONMENT to -2), risk = 3)
            )
        )
    ),

    ev(
        id = "city16_resource_transition", title = "资源型城市转型",
        background = "资源产业见顶，转型迫在眉睫",
        description = "资源价格波动加大，单一产业的脆弱性暴露出来，必须考虑接续产业。",
        tags = tags(EventTag.PROJECT, EventTag.RESEARCH, EventTag.MEETING),
        weight = 12, repeatable = true, cooldown = 20, minPos = 3,
        cityTags = setOf("resources", "energy", "chemicals"),
        choices = listOf(
            c(
                "布局接续产业，培育新增长点", Attitude.BOLD,
                "你选定了两个接续产业方向，前期投入不小，但城市有了新的想象空间。",
                AttrDelta(work = 6, ability = 4, coordination = 3, energy = -6),
                HiddenDelta(opportunity = 5, stress = 4),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 3),
                    merit = mapOf(MeritKind.INNOVATION to 5, MeritKind.ECONOMIC to 3), finance = -6
                )
            ),
            c(
                "延伸资源产业链，提高附加值", Attitude.STEADY,
                "你推动本地深加工，附加值上来了，转型迈出了稳当的一步。",
                AttrDelta(work = 5, ability = 3, coordination = 3),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.INNOVATION to 2)
                )
            ),
            c(
                "维持现状，先保住财政收入", Attitude.CAUTIOUS,
                "短期财政稳住了，但转型窗口继续收窄。",
                AttrDelta(politics = 2),
                governance = cityGovB(merit = mapOf(MeritKind.FINANCE to 2), risk = 3)
            )
        )
    ),

    ev(
        id = "city16_clean_energy", title = "清洁能源基地建设",
        background = "清洁能源项目集中上马",
        description = "风光储项目集中布局，涉及征地、并网与群众收益分配。",
        tags = tags(EventTag.PROJECT, EventTag.RURAL, EventTag.MASS_APPEAL),
        weight = 11, repeatable = true, cooldown = 18, minPos = 4,
        cityTags = setOf("energy", "resources", "ocean", "technology"),
        choices = listOf(
            c(
                "连片开发，把收益与群众共享", Attitude.STEADY,
                "项目连片推进，村集体通过入股获得长期收益，群众支持度很高。",
                AttrDelta(work = 5, masses = 4, coordination = 3),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 3, Indicator.APPROVAL to 3, Indicator.ENVIRONMENT to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.LIVELIHOOD to 3), finance = -4
                )
            ),
            c(
                "加快并网，抢抓政策窗口", Attitude.BOLD,
                "项目上得很快，个别地块的补偿还没完全谈拢，留下了信访隐患。",
                AttrDelta(work = 5, ability = 3, masses = -4),
                HiddenDelta(riskPressure = 5),
                governance = cityGovB(
                    ind = mapOf(Indicator.ECONOMY to 4),
                    merit = mapOf(MeritKind.ECONOMIC to 5), risk = 4
                )
            ),
            c(
                "先做整体规划，稳步推进", Attitude.CAUTIOUS,
                "规划做得扎实，但项目进度慢，部分指标被周边地区抢先。",
                AttrDelta(writing = 3, politics = 2),
                governance = cityGovB(merit = mapOf(MeritKind.INNOVATION to 2))
            )
        )
    )
)