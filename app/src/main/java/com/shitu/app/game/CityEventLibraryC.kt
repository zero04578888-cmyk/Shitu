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
 * 第十六阶段：城市专属事件池（三）。全部为虚构事件设定。
 */
private fun cityGovC(
    ind: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    risk: Int = 0
) = GovernanceEffect(indicators = ind, merit = merit, finance = finance, risk = risk)

internal val cityEventLibraryC: List<GameEvent> = listOf(

    ev(
        id = "city16_agri_brand", title = "农产品区域公用品牌",
        background = "本地农产品要打品牌",
        description = "本地农产品品质不错，但品牌散、渠道弱，卖不上价，农户增收有限。",
        tags = tags(EventTag.RURAL, EventTag.PROJECT, EventTag.RESEARCH),
        weight = 12, repeatable = true, cooldown = 16, minPos = 2,
        cityTags = setOf("agriculture", "food", "ecommerce", "tourism"),
        choices = listOf(
            c(
                "统一标准，打造区域公用品牌", Attitude.STEADY,
                "标准立了起来，品牌统一对外，收购价稳步走高，农户尝到了甜头。",
                AttrDelta(work = 4, masses = 4, coordination = 3, ability = 2),
                governance = cityGovC(
                    ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.LIVELIHOOD to 4, MeritKind.ECONOMIC to 2), finance = -3
                )
            ),
            c(
                "政府背书，先冲一波销量", Attitude.BOLD,
                "直播带货冲出了销量，但个别批次品控没跟上，出现退货。",
                AttrDelta(work = 4, reputation = 2, integrityRisk = 2, masses = 1),
                governance = cityGovC(
                    ind = mapOf(Indicator.ECONOMY to 3),
                    merit = mapOf(MeritKind.ECONOMIC to 4), risk = 2
                )
            ),
            c(
                "让龙头企业自己带品牌", Attitude.CAUTIOUS,
                "企业做得不错，但小农户受益有限，带动面不够广。",
                AttrDelta(politics = 2, work = 1),
                governance = cityGovC(merit = mapOf(MeritKind.ECONOMIC to 2))
            )
        )
    ),

    ev(
        id = "city16_grain_security", title = "耕地保护与粮食安全",
        background = "耕地保护责任必须压实",
        description = "上级核查发现部分地块存在非农化倾向，要求限期整改，同时要兼顾农民收益。",
        tags = tags(EventTag.RURAL, EventTag.INSPECTION, EventTag.MASS_APPEAL),
        weight = 12, repeatable = true, cooldown = 16, minPos = 2,
        cityTags = setOf("agriculture", "food"),
        choices = listOf(
            c(
                "坚决复耕，同步给农户找出路", Attitude.STEADY,
                "地块如期复耕，镇里帮农户对接了订单农业，阻力比预想小。",
                AttrDelta(work = 5, masses = 3, politics = 3),
                governance = cityGovC(
                    ind = mapOf(Indicator.LIVELIHOOD to 2),
                    merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.SECURITY to 2)
                )
            ),
            c(
                "严格执法，逾期不整改就处置", Attitude.BOLD,
                "整改速度最快，但个别农户的利益受损，反映到了上面。",
                AttrDelta(work = 5, politics = 2, masses = -5, leaderTrust = 2),
                HiddenDelta(riskPressure = 5),
                governance = cityGovC(merit = mapOf(MeritKind.SECURITY to 3), risk = 4)
            ),
            c(
                "先摸底，分年度逐步整改", Attitude.CAUTIOUS,
                "你选择了慢工出细活，核查组认为进度偏慢。",
                AttrDelta(writing = 3, politics = 2, leaderTrust = -2),
                governance = cityGovC(merit = mapOf(MeritKind.LIVELIHOOD to 1))
            )
        )
    ),

    ev(
        id = "city16_biomed", title = "生物医药产业培育",
        background = "生物医药企业需要临床与审批支持",
        description = "几家生物医药企业进入关键期，需要的不仅是土地，还有临床资源与审评通道。",
        tags = tags(EventTag.PROJECT, EventTag.RESEARCH, EventTag.LEADER_TASK),
        weight = 11, repeatable = true, cooldown = 20, minPos = 4,
        cityTags = setOf("medicine", "technology", "biology", "education"),
        choices = listOf(
            c(
                "搭平台、建通道，全链条支持", Attitude.BOLD,
                "临床与研究平台建了起来，企业进度明显加快，产业链开始聚集。",
                AttrDelta(work = 6, ability = 4, coordination = 3),
                HiddenDelta(opportunity = 5),
                governance = cityGovC(
                    ind = mapOf(Indicator.ECONOMY to 3, Indicator.MEDICAL to 2),
                    merit = mapOf(MeritKind.INNOVATION to 5), finance = -6
                )
            ),
            c(
                "先解决最急的厂房与资金", Attitude.STEADY,
                "你先把最紧迫的问题解决，企业稳住了阵脚，后续支持还在路上。",
                AttrDelta(work = 4, coordination = 3, politics = 2),
                governance = cityGovC(
                    ind = mapOf(Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.INNOVATION to 2)
                )
            ),
            c(
                "交由市场与园区自行运作", Attitude.CAUTIOUS,
                "企业自力更生，个别项目因资金链吃紧而放缓。",
                AttrDelta(politics = 2),
                governance = cityGovC(merit = mapOf(MeritKind.INNOVATION to -1))
            )
        )
    ),

    ev(
        id = "city16_platform_rules", title = "平台经济规范与发展",
        background = "平台企业快速发展带来新问题",
        description = "平台企业带动了就业与消费，但劳动者权益、数据合规等问题开始集中出现。",
        tags = tags(EventTag.COORDINATION, EventTag.PUBLIC_OPINION, EventTag.INSPECTION),
        weight = 12, repeatable = true, cooldown = 18, minPos = 5,
        cityTags = setOf("internet", "ecommerce", "digital", "commerce"),
        choices = listOf(
            c(
                "约谈平台，推动建立权益保障机制", Attitude.STEADY,
                "平台承诺改进算法与保障条款，监管与发展的关系处理得比较平衡。",
                AttrDelta(work = 5, politics = 4, coordination = 3, masses = 3),
                governance = cityGovC(
                    ind = mapOf(Indicator.APPROVAL to 2),
                    merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.FINANCE to 2)
                )
            ),
            c(
                "重拳整治，先规范再发展", Attitude.BOLD,
                "整治立竿见影，但部分业务收缩，短期就业与税收受影响。",
                AttrDelta(politics = 4, leaderTrust = 3, work = 3, reputation = 2),
                governance = cityGovC(
                    ind = mapOf(Indicator.ECONOMY to -2, Indicator.APPROVAL to 3),
                    merit = mapOf(MeritKind.SECURITY to 3, MeritKind.LIVELIHOOD to 2), risk = 3
                )
            ),
            c(
                "观察为主，避免误伤新业态", Attitude.CAUTIOUS,
                "你没有急于出手，问题逐渐积累，社会关注度上升。",
                AttrDelta(politics = 2, work = -1),
                governance = cityGovC(ind = mapOf(Indicator.APPROVAL to -3), risk = 4)
            )
        )
    ),

    ev(
        id = "city16_intl_expo", title = "国际会展与城市形象",
        background = "一场国际性会展即将举办",
        description = "会展规格高、来宾多，城市形象、安保、服务保障都面临一次大考。",
        tags = tags(EventTag.MEETING, EventTag.COORDINATION, EventTag.EMERGENCY),
        weight = 10, repeatable = true, cooldown = 24, minPos = 5,
        cityTags = setOf("international", "media", "consumption", "capital", "shipping"),
        choices = listOf(
            c(
                "全流程演练，把服务做到位", Attitude.STEADY,
                "会展平稳落幕，来宾评价很高，城市形象上了一个台阶。",
                AttrDelta(work = 5, coordination = 4, reputation = 4, energy = -7),
                HiddenDelta(socialInfluence = 4, network = 3),
                governance = cityGovC(
                    ind = mapOf(Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.SECURITY to 2), finance = -4
                )
            ),
            c(
                "集中力量办大事，追求高规格", Attitude.BOLD,
                "场面很足，效果亮眼，但投入也大，后续还要精打细算。",
                AttrDelta(work = 4, leaderTrust = 4, reputation = 3),
                HiddenDelta(opportunity = 4, stress = 5),
                governance = cityGovC(merit = mapOf(MeritKind.ECONOMIC to 3), finance = -8)
            ),
            c(
                "压缩规模，稳妥办好", Attitude.CAUTIOUS,
                "会展顺利但不够出彩，上级觉得你偏于保守。",
                AttrDelta(politics = 2, work = 1, leaderTrust = -1),
                governance = cityGovC(merit = mapOf(MeritKind.ECONOMIC to 1))
            )
        )
    ),

    ev(
        id = "city16_mega_city", title = "超大城市治理",
        background = "人口规模给城市治理带来压力",
        description = "人口持续流入，住房、通勤、学位、医疗都吃紧，治理难度明显上升。",
        tags = tags(EventTag.COORDINATION, EventTag.MEETING, EventTag.MASS_APPEAL),
        weight = 11, repeatable = true, cooldown = 20, minPos = 5,
        cityTags = setOf("capital", "politics", "international", "finance", "shipping"),
        choices = listOf(
            c(
                "补齐公共服务短板，疏解功能布局", Attitude.STEADY,
                "学位与床位增加，通勤压力有所缓解，治理更从容了。",
                AttrDelta(work = 5, coordination = 4, masses = 3),
                governance = cityGovC(
                    ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.INFRASTRUCTURE to 2, Indicator.APPROVAL to 2),
                    merit = mapOf(MeritKind.LIVELIHOOD to 4), finance = -6
                )
            ),
            c(
                "严格人口与功能管理，控制增量", Attitude.BOLD,
                "人口增速降了下来，但部分产业与服务业感到用工紧张。",
                AttrDelta(politics = 4, work = 3, masses = -3),
                governance = cityGovC(
                    ind = mapOf(Indicator.SECURITY to 2, Indicator.ECONOMY to -1),
                    merit = mapOf(MeritKind.SECURITY to 3), risk = 3
                )
            ),
            c(
                "先做规划研究，逐步推进", Attitude.CAUTIOUS,
                "规划研究做得很充分，但短期问题仍在积累。",
                AttrDelta(writing = 3, politics = 2, masses = -1),
                governance = cityGovC(ind = mapOf(Indicator.APPROVAL to -2))
            )
        )
    ),

    ev(
        id = "city16_industry_transfer", title = "产业转移承接",
        background = "沿海产业向内地转移",
        description = "一批制造企业寻求内迁，带来就业与税收，也带来环保与用地压力。",
        tags = tags(EventTag.PROJECT, EventTag.RURAL, EventTag.COORDINATION),
        weight = 11, repeatable = true, cooldown = 18, minPos = 3,
        cityTags = setOf("manufacturing", "industry", "agriculture", "logistics"),
        choices = listOf(
            c(
                "设门槛，只承接绿色高效的项目", Attitude.STEADY,
                "承接的项目质量不错，就业与税收稳步增加，环境压力可控。",
                AttrDelta(work = 5, coordination = 3, politics = 2),
                governance = cityGovC(
                    ind = mapOf(Indicator.ECONOMY to 3, Indicator.ENVIRONMENT to 1),
                    merit = mapOf(MeritKind.ECONOMIC to 4), finance = -3
                )
            ),
            c(
                "先把项目引进来，其他以后再说", Attitude.BOLD,
                "园区很快满员，用工需求旺盛，但个别企业的环保设施没跟上。",
                AttrDelta(work = 5, ability = 2, integrityRisk = 3),
                HiddenDelta(riskPressure = 5),
                governance = cityGovC(
                    ind = mapOf(Indicator.ECONOMY to 4, Indicator.ENVIRONMENT to -3),
                    merit = mapOf(MeritKind.ECONOMIC to 5), risk = 5
                )
            ),
            c(
                "谨慎承接，逐个评估", Attitude.CAUTIOUS,
                "你卡得很严，落地的企业不多，指标增长有限。",
                AttrDelta(writing = 3, politics = 2, work = -1),
                governance = cityGovC(merit = mapOf(MeritKind.ECONOMIC to 1))
            )
        )
    ),

    ev(
        id = "city16_talent", title = "人才引进与安居",
        background = "重点产业急需紧缺人才",
        description = "几家重点企业反映招不到人，尤其是高技能与研发人才，住房是最大障碍。",
        tags = tags(EventTag.PROJECT, EventTag.RESEARCH, EventTag.LEADER_TASK),
        weight = 11, repeatable = true, cooldown = 16, minPos = 3,
        cityTags = setOf("technology", "innovation", "education", "digital", "medicine"),
        choices = listOf(
            c(
                "建人才公寓，配套子女入学", Attitude.BOLD,
                "人才公寓交付使用，几家企业招人明显顺利了，财政也投入不小。",
                AttrDelta(work = 5, coordination = 3, reputation = 2),
                governance = cityGovC(
                    ind = mapOf(Indicator.EDUCATION to 2, Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.INNOVATION to 4, MeritKind.LIVELIHOOD to 2), finance = -6
                )
            ),
            c(
                "用补贴撬动，企业配套出资", Attitude.STEADY,
                "政府补一点、企业出一点，引才成本降了下来，效果比较实在。",
                AttrDelta(work = 4, coordination = 3, ability = 2),
                governance = cityGovC(
                    ind = mapOf(Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.INNOVATION to 3), finance = -3
                )
            ),
            c(
                "让企业自己解决住房问题", Attitude.CAUTIOUS,
                "企业抱怨解决不了后顾之忧，个别团队考虑外迁。",
                AttrDelta(politics = 2, leaderTrust = -1),
                governance = cityGovC(merit = mapOf(MeritKind.INNOVATION to -1))
            )
        )
    )
)