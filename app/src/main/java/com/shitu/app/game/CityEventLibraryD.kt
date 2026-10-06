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
 * 第十六阶段：城市专属事件池（四）。全部为虚构事件设定。
 */
private fun cityGovD(
    ind: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    risk: Int = 0
) = GovernanceEffect(indicators = ind, merit = merit, finance = finance, risk = risk)

internal val cityEventLibraryD: List<GameEvent> = listOf(

    ev(
        id = "city16_water_source", title = "水源地保护与产业发展",
        background = "水源地保护与工业发展产生矛盾",
        description = "水源保护区内的企业面临搬迁，涉及财政、就业与供水安全的多重权衡。",
        tags = tags(EventTag.EMERGENCY, EventTag.PROJECT, EventTag.MASS_APPEAL),
        weight = 11, repeatable = true, cooldown = 20, minPos = 4,
        cityTags = setOf("ocean", "tourism", "agriculture", "energy", "chemicals"),
        choices = listOf(
            c(
                "依法搬迁，同步做好职工安置", Attitude.STEADY,
                "搬迁按计划完成，供水安全得到保障，职工也基本安置妥当。",
                AttrDelta(work = 5, masses = 3, coordination = 3),
                governance = cityGovD(
                    ind = mapOf(Indicator.ENVIRONMENT to 4, Indicator.SECURITY to 2),
                    merit = mapOf(MeritKind.SECURITY to 3, MeritKind.LIVELIHOOD to 2), finance = -6
                )
            ),
            c(
                "先关停高风险环节，分步搬迁", Attitude.CAUTIOUS,
                "风险环节先停，整体搬迁往后排，供水安全有了基本保障。",
                AttrDelta(politics = 3, work = 3),
                governance = cityGovD(ind = mapOf(Indicator.ENVIRONMENT to 2), merit = mapOf(MeritKind.SECURITY to 2))
            ),
            c(
                "争取上级资金，暂缓搬迁", Attitude.RISKY,
                "你向上争取资金，但项目迟迟没有落地，环境隐患依然存在。",
                AttrDelta(politics = 2, coordination = 2),
                HiddenDelta(riskPressure = 5),
                governance = cityGovD(ind = mapOf(Indicator.ENVIRONMENT to -2), risk = 5)
            )
        )
    ),

    ev(
        id = "city16_logistics_hub", title = "物流枢纽与通道建设",
        background = "区域物流通道需要提级",
        description = "本地是区域交通节点，但多式联运衔接不畅，物流成本偏高。",
        tags = tags(EventTag.PROJECT, EventTag.COORDINATION, EventTag.MEETING),
        weight = 11, repeatable = true, cooldown = 18, minPos = 4,
        cityTags = setOf("logistics", "port", "trade", "transportation", "resources"),
        choices = listOf(
            c(
                "推动多式联运一体化", Attitude.STEADY,
                "铁路、公路、港口衔接顺畅了，物流成本下降，企业得实惠。",
                AttrDelta(work = 5, coordination = 4, ability = 3),
                governance = cityGovD(
                    ind = mapOf(Indicator.INFRASTRUCTURE to 3, Indicator.ECONOMY to 2),
                    merit = mapOf(MeritKind.ECONOMIC to 4), finance = -4
                )
            ),
            c(
                "大干快上，先建几个大项目", Attitude.BOLD,
                "项目开工很快，但部分设施与既有线路衔接还需磨合。",
                AttrDelta(work = 4, ability = 2, leaderTrust = 3),
                governance = cityGovD(ind = mapOf(Indicator.INFRASTRUCTURE to 2), merit = mapOf(MeritKind.ECONOMIC to 3), finance = -7)
            ),
            c(
                "等待上级统一规划", Attitude.CAUTIOUS,
                "你没有主动推进，物流成本依旧偏高。",
                AttrDelta(politics = 2),
                governance = cityGovD(merit = mapOf(MeritKind.ECONOMIC to -1))
            )
        )
    ),

    ev(
        id = "city16_culture_ip", title = "城市文化IP打造",
        background = "城市文化资源有待激活",
        description = "本地文化资源丰富，但缺乏统一的城市IP与传播，文旅转化率不高。",
        tags = tags(EventTag.PROJECT, EventTag.MEETING, EventTag.PUBLIC_OPINION),
        weight = 11, repeatable = true, cooldown = 18, minPos = 3,
        cityTags = setOf("culture", "history", "media", "tourism"),
        choices = listOf(
            c(
                "系统梳理资源，打造统一城市IP", Attitude.STEADY,
                "城市IP立了起来，文旅与文创产品同步跟进，传播效果逐步显现。",
                AttrDelta(work = 4, coordination = 3, reputation = 3),
                governance = cityGovD(
                    ind = mapOf(Indicator.ECONOMY to 2, Indicator.APPROVAL to 2),
                    merit = mapOf(MeritKind.INNOVATION to 3, MeritKind.ECONOMIC to 2), finance = -3
                )
            ),
            c(
                "制造话题，先冲一波流量", Attitude.RISKY,
                "话题热度很高，游客暴增，但服务与承载没能同步跟上。",
                AttrDelta(work = 4, reputation = 3, masses = -2),
                HiddenDelta(riskPressure = 4, socialInfluence = 5),
                governance = cityGovD(
                    ind = mapOf(Indicator.ECONOMY to 3),
                    merit = mapOf(MeritKind.ECONOMIC to 4), risk = 3
                )
            ),
            c(
                "以保护为主，谨慎开发", Attitude.CAUTIOUS,
                "文化资源保护得好，但转化利用程度不高。",
                AttrDelta(politics = 3, writing = 2),
                governance = cityGovD(merit = mapOf(MeritKind.INNOVATION to 1))
            )
        )
    )
)