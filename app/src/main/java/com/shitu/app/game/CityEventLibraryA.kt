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
 * 第十六阶段：城市专属事件池（一）。
 * 事件通过 [GameEvent.requiredCityTags] 与任职城市的标签绑定，全部为虚构事件设定。
 */
private fun cityGovA(
    ind: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    risk: Int = 0
) = GovernanceEffect(indicators = ind, merit = merit, finance = finance, risk = risk)

internal val cityEventLibraryA: List<GameEvent> = listOf(

    ev(
        id = "city16_digital_invest", title = "数字经济园区招商",
        background = "数字经济企业洽谈落地",
        description = "几家数字经济企业看中了本地的产业配套与人才，希望在园区拿地建总部，但提出的政策条件不低。",
        tags = tags(EventTag.PROJECT, EventTag.LEADER_TASK),
        weight = 12, repeatable = true, cooldown = 16, minPos = 3,
        cityTags = setOf("digital", "technology", "internet", "ecommerce"),
        choices = listOf(
            c(
                "按政策上限给足支持，尽快签约", Attitude.BOLD,
                "企业很快落笔，园区一期的地块随即挂牌。账面数字好看，但财政要先垫一笔配套。",
                AttrDelta(work = 5, ability = 3, coordination = 3, leaderTrust = 2),
                HiddenDelta(opportunity = 4, riskPressure = 3),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 3),
                    merit = mapOf(MeritKind.ECONOMIC to 5, MeritKind.INNOVATION to 3),
                    finance = -6, risk = 3
                )
            ),
            c(
                "分期供地，把配套和考核写进协议", Attitude.STEADY,
                "你把落地节奏与产值、就业指标绑定，企业接受了分期方案。推进慢一些，但更稳。",
                AttrDelta(work = 4, coordination = 4, politics = 2, writing = 2),
                HiddenDelta(opportunity = 3),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.INNOVATION to 2),
                    finance = -3
                )
            ),
            c(
                "先做产业评估，暂缓表态", Attitude.CAUTIOUS,
                "你请第三方做了尽调。企业转去邻近城市考察，项目悬而未决。",
                AttrDelta(politics = 2, writing = 3, work = -1),
                HiddenDelta(opportunity = -2)
            )
        )
    ),

    ev(
        id = "city16_chip_land", title = "芯片项目用地",
        background = "半导体项目急需连片工业用地",
        description = "一家集成电路企业要连片用地，涉及几处低效厂房的搬迁，牵动不少人的利益。",
        tags = tags(EventTag.PROJECT, EventTag.COORDINATION, EventTag.PETITION),
        weight = 11, repeatable = true, cooldown = 18, minPos = 4,
        cityTags = setOf("technology", "innovation"),
        choices = listOf(
            c(
                "依法依规推进搬迁，同步安置群众", Attitude.STEADY,
                "搬迁按程序推进，安置方案做了两轮公示。进度不快，但没有出乱子。",
                AttrDelta(work = 4, masses = 3, coordination = 3, politics = 2),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 2, Indicator.APPROVAL to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.LIVELIHOOD to 2),
                    finance = -4
                )
            ),
            c(
                "集中攻坚，限时完成腾地", Attitude.BOLD,
                "你下了死命令，腾地在期限内完成，项目如期开工，但有几户群众一直在反映问题。",
                AttrDelta(work = 6, ability = 3, masses = -6, integrityRisk = 2),
                HiddenDelta(riskPressure = 6),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 4, Indicator.APPROVAL to -3),
                    merit = mapOf(MeritKind.ECONOMIC to 6), finance = -3, risk = 5
                )
            ),
            c(
                "先调整选址，避开矛盾集中区", Attitude.CAUTIOUS,
                "你让规划部门重新比选，用地面积缩了水，企业对方案不太满意。",
                AttrDelta(coordination = 3, politics = 2, work = 2),
                governance = cityGovA(merit = mapOf(MeritKind.ECONOMIC to 1))
            )
        )
    ),

    ev(
        id = "city16_ev_plant", title = "新能源汽车整车项目",
        background = "整车厂希望扩建二期",
        description = "本地新能源整车厂经营向好，提出扩建二期，条件是配套的土地、能源与人才政策。",
        tags = tags(EventTag.PROJECT, EventTag.LEADER_TASK, EventTag.COORDINATION),
        weight = 12, repeatable = true, cooldown = 18, minPos = 4,
        cityTags = setOf("automotive", "technology", "innovation"),
        choices = listOf(
            c(
                "全力保障，把产业链一起招进来", Attitude.BOLD,
                "二期顺利启动，配套的电池与零部件企业也陆续签约，产业集聚起来了。",
                AttrDelta(work = 6, coordination = 4, leaderTrust = 3, energy = -6),
                HiddenDelta(opportunity = 5, stress = 4),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 4),
                    merit = mapOf(MeritKind.ECONOMIC to 6), finance = -6
                )
            ),
            c(
                "先解决能源与环保指标，再谈扩建", Attitude.STEADY,
                "你要求先落实用能与排放指标。项目慢了一步，但没有留下环保欠账。",
                AttrDelta(work = 4, politics = 3, coordination = 3),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 2, Indicator.ENVIRONMENT to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.INNOVATION to 2)
                )
            ),
            c(
                "让企业自己解决配套，政府只做服务", Attitude.CAUTIOUS,
                "企业自己去谈能源与土地，进度明显放缓，二期最终只上了一半。",
                AttrDelta(politics = 2, work = -2, leaderTrust = -2),
                governance = cityGovA(merit = mapOf(MeritKind.ECONOMIC to -1))
            )
        )
    ),

    ev(
        id = "city16_cross_border", title = "跨境电商综试区",
        background = "跨境电商业态快速聚集",
        description = "一批跨境电商企业聚集，仓库、通关、结汇都出现了新情况，也带来监管上的新问题。",
        tags = tags(EventTag.PROJECT, EventTag.COORDINATION, EventTag.INSPECTION),
        weight = 11, repeatable = true, cooldown = 16, minPos = 3,
        cityTags = setOf("trade", "ecommerce", "port", "commerce"),
        choices = listOf(
            c(
                "建一站式服务平台，边发展边规范", Attitude.STEADY,
                "你把海关、税务、物流拉到一起办公，通关效率明显提升，企业反映不错。",
                AttrDelta(work = 5, coordination = 4, ability = 2),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 3),
                    merit = mapOf(MeritKind.ECONOMIC to 4), finance = -3
                )
            ),
            c(
                "先放宽准入门槛，把规模做起来", Attitude.BOLD,
                "数据增长很快，但个别企业出现了申报不规范的问题，留下隐患。",
                AttrDelta(work = 4, ability = 2, integrityRisk = 3),
                HiddenDelta(riskPressure = 5),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 4),
                    merit = mapOf(MeritKind.ECONOMIC to 5), risk = 4
                )
            ),
            c(
                "抬高门槛，只留资质过硬的企业", Attitude.CAUTIOUS,
                "平台秩序好了，但一部分中小卖家选择离开，规模增速放缓。",
                AttrDelta(politics = 3, writing = 2, work = -1),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to -1),
                    merit = mapOf(MeritKind.ECONOMIC to -1)
                )
            )
        )
    ),

    ev(
        id = "city16_free_trade", title = "自贸片区制度创新",
        background = "自贸片区要推出新一批制度创新",
        description = "上级要求自贸片区拿出一批可复制的制度创新成果，时间紧、标准高，还要防住风险。",
        tags = tags(EventTag.PROJECT, EventTag.MEETING, EventTag.LEADER_TASK),
        weight = 10, repeatable = true, cooldown = 20, minPos = 5,
        cityTags = setOf("trade", "international", "finance", "shipping"),
        choices = listOf(
            c(
                "聚焦企业痛点，推几项真管用的改革", Attitude.STEADY,
                "你挑了几个企业反映最集中的环节动刀，成果不算多，但落地效果实打实。",
                AttrDelta(work = 5, ability = 4, writing = 3),
                HiddenDelta(opportunity = 4),
                governance = cityGovA(merit = mapOf(MeritKind.INNOVATION to 5, MeritKind.ECONOMIC to 2))
            ),
            c(
                "打包一批创新点，争取上级推广", Attitude.BOLD,
                "你把创新点打包上报，获得了通报表扬，但个别条款落地时还需要再打补丁。",
                AttrDelta(work = 3, leaderTrust = 4, reputation = 3, integrityRisk = 2),
                HiddenDelta(opportunity = 5, riskPressure = 4),
                governance = cityGovA(merit = mapOf(MeritKind.INNOVATION to 5, MeritKind.FINANCE to 2), risk = 3)
            ),
            c(
                "稳妥推进，先小范围试点", Attitude.CAUTIOUS,
                "你选择了风险最小的路径，试点平稳，但亮点不够突出。",
                AttrDelta(politics = 3, writing = 2),
                governance = cityGovA(merit = mapOf(MeritKind.INNOVATION to 2), risk = -2)
            )
        )
    ),

    ev(
        id = "city16_finance_risk", title = "金融风险排查",
        background = "区域金融风险需要专项排查",
        description = "上级通报了外地的一起风险事件，要求各地排查同类隐患。你辖区里正好有几家机构关联密切。",
        tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY, EventTag.EMERGENCY),
        weight = 12, repeatable = true, cooldown = 18, minPos = 5,
        cityTags = setOf("finance", "international", "commerce"),
        choices = listOf(
            c(
                "全面排查，问题机构先控增量", Attitude.STEADY,
                "你组织专班逐户排查，及时处置了两处隐患，虽然处置成本不低，但守住了底线。",
                AttrDelta(work = 5, politics = 3, ability = 3, integrityRisk = -2),
                governance = cityGovA(
                    ind = mapOf(Indicator.SECURITY to 3),
                    merit = mapOf(MeritKind.FINANCE to 4, MeritKind.SECURITY to 2), finance = -3
                )
            ),
            c(
                "先保稳定，暂缓对外披露细节", Attitude.RISKY,
                "风险暂时压住了，市场情绪平稳，但一旦后续发酵，责任不小。",
                AttrDelta(politics = 3, coordination = 2, integrityRisk = 4),
                HiddenDelta(riskPressure = 6, stress = 5),
                governance = cityGovA(merit = mapOf(MeritKind.FINANCE to 2), risk = 6)
            ),
            c(
                "问题机构限期自查自纠", Attitude.CAUTIOUS,
                "你给了机构自查的窗口期，排查进度慢，但程序上更稳妥。",
                AttrDelta(writing = 3, politics = 2, work = 1),
                governance = cityGovA(merit = mapOf(MeritKind.FINANCE to 2), risk = 2)
            )
        )
    ),

    ev(
        id = "city16_port_channel", title = "港口航道扩容",
        background = "主航道通航能力接近饱和",
        description = "港口吞吐量持续增长，主航道在高峰期出现压船，航运企业反映强烈。",
        tags = tags(EventTag.PROJECT, EventTag.COORDINATION, EventTag.LEADER_TASK),
        weight = 11, repeatable = true, cooldown = 20, minPos = 4,
        cityTags = setOf("port", "shipping", "logistics", "trade"),
        choices = listOf(
            c(
                "上报立项，争取上级资金支持扩容", Attitude.STEADY,
                "项目进入上级盘子，前期工作有序推进，短期内压船问题还没有完全缓解。",
                AttrDelta(work = 4, politics = 3, writing = 3, coordination = 2),
                HiddenDelta(opportunity = 3),
                governance = cityGovA(ind = mapOf(Indicator.INFRASTRUCTURE to 2), merit = mapOf(MeritKind.ECONOMIC to 3))
            ),
            c(
                "先优化调度，把现有能力榨出来", Attitude.BOLD,
                "你推动港航联动调度，压船缓解得很快，企业叫好，但长期仍要靠扩容。",
                AttrDelta(work = 5, ability = 3, coordination = 4),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.INNOVATION to 2)
                )
            ),
            c(
                "按部就班等待统一规划", Attitude.CAUTIOUS,
                "你没有额外用力，一切按既定规划走，压船问题继续存在。",
                AttrDelta(politics = 2),
                governance = cityGovA(ind = mapOf(Indicator.APPROVAL to -2))
            )
        )
    ),

    ev(
        id = "city16_aerospace", title = "航空航天配套基地",
        background = "航空航天企业寻求本地配套",
        description = "一家航空航天配套企业希望在本地布局，涉及高技能人才引进与专门政策支持。",
        tags = tags(EventTag.PROJECT, EventTag.RESEARCH, EventTag.LEADER_TASK),
        weight = 11, repeatable = true, cooldown = 20, minPos = 5,
        cityTags = setOf("aerospace", "defense", "technology", "education"),
        choices = listOf(
            c(
                "配套人才政策，把研发团队一起引进", Attitude.BOLD,
                "团队顺利落地，配套的科研平台也搭了起来，产业与科教形成了联动。",
                AttrDelta(work = 6, ability = 4, leaderTrust = 3),
                HiddenDelta(opportunity = 5),
                governance = cityGovA(
                    ind = mapOf(Indicator.ECONOMY to 3, Indicator.EDUCATION to 2),
                    merit = mapOf(MeritKind.INNOVATION to 5, MeritKind.ECONOMIC to 3), finance = -5
                )
            ),
            c(
                "先解决厂房与用地，人才政策再议", Attitude.STEADY,
                "项目先落了地，人才配套慢了一拍，企业表示理解但也在观望。",
                AttrDelta(work = 4, coordination = 3, politics = 2),
                governance = cityGovA(ind = mapOf(Indicator.ECONOMY to 2), merit = mapOf(MeritKind.ECONOMIC to 3))
            ),
            c(
                "评估产业风险后再决定", Attitude.CAUTIOUS,
                "你要求做完整评估，项目周期被拉长，企业开始考虑周边城市。",
                AttrDelta(writing = 3, politics = 2, work = -1),
                HiddenDelta(opportunity = -3)
            )
        )
    )
)