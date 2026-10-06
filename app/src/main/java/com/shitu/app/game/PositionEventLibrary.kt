package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.EventTag
import com.shitu.app.model.GovernanceEffect
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.model.PoliticalImpact
import com.shitu.app.model.PoliticalTendency
import com.shitu.app.model.PositionScope

/**
 * 第十五阶段：高职级专属事件。
 *
 * 这些事件只有在玩家晋升到对应职级后才会进入事件池，
 * 体现「职位越高，能处理的事越大」。全部为虚构设定。
 */
private fun pg(
    ind: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    risk: Int = 0
) = GovernanceEffect(indicators = ind, merit = merit, finance = finance, risk = risk)

internal val positionEventLibrary: List<com.shitu.app.model.GameEvent> = listOf(
    ev(
        id = "p15_county_major_project", title = "重大项目上马",
        background = "县里争取到一个重大项目", description = "项目投资大、周期长，能带动经济，也可能带来债务与环境压力。",
        tags = tags(EventTag.PROJECT, EventTag.RESEARCH, EventTag.CAREER),
        weight = 12, minPos = 6, repeatable = true, cooldown = 18,
        requiredScope = PositionScope.COUNTY,
        choices = listOf(
            c("全力推进，尽快开工", Attitude.BOLD, "项目当年开工，经济数据很快有了起色。", 
                AttrDelta(work = 4, ability = 2, reputation = 2, energy = -5),
                governance = pg(ind = mapOf(Indicator.ECONOMY to 4), merit = mapOf(MeritKind.ECONOMIC to 4), finance = -6, risk = 3)),
            c("先做充分论证再决策", Attitude.STEADY, "你把可行性报告看了三遍，项目稳了一步。", 
                AttrDelta(politics = 3, writing = 2, integrityRisk = -2),
                governance = pg(ind = mapOf(Indicator.ECONOMY to 2), merit = mapOf(MeritKind.INNOVATION to 2))),
            c("拆分实施，控制风险", Attitude.CAUTIOUS, "你把项目拆成了两期，压力小了，速度也慢了。", 
                AttrDelta(work = 2, coordination = 3, reputation = -1),
                governance = pg(ind = mapOf(Indicator.INFRASTRUCTURE to 2), finance = -3))
        )
    ),
    ev(
        id = "p15_county_cadre_selection", title = "科级干部调整",
        background = "一批科级岗位需要配齐", description = "人事盘子摆在桌上，用谁、怎么用，考验的是判断，也是关系。",
        tags = tags(EventTag.CAREER, EventTag.ASSESSMENT, EventTag.LEADER_TALK),
        weight = 12, minPos = 6, repeatable = true, cooldown = 20,
        requiredScope = PositionScope.COUNTY,
        choices = listOf(
            c("以实绩为先，能者上", Attitude.STEADY, "你把最能干的人放到了关键岗位。", 
                AttrDelta(politics = 3, reputation = 3, colleagues = -2)),
            c("兼顾资历与平衡", Attitude.CAUTIOUS, "你照顾了各方，队伍稳住了，冲劲弱了一点。", 
                AttrDelta(politics = 4, colleagues = 2, work = -1)),
            c("大胆启用年轻干部", Attitude.BOLD, "你提了几个年轻人，有人叫好，也有人不服。", 
                AttrDelta(work = 3, ability = 2, reputation = -1, colleagues = -3),
                politics = PoliticalImpact(capital = 3, tendencies = mapOf(PoliticalTendency.REFORMIST to 3)))
        )
    ),
    ev(
        id = "p15_city_region_plan", title = "区域协同规划",
        background = "几块地方的利益需要摆平", description = "规划一旦定下来，会影响未来很多年的走向，各方都在争取。",
        tags = tags(EventTag.COORDINATION, EventTag.MEETING, EventTag.PROJECT),
        weight = 12, minPos = 7, repeatable = true, cooldown = 22,
        requiredScope = PositionScope.CITY,
        choices = listOf(
            c("按整体利益定盘子", Attitude.BOLD, "你顶住压力按整体效益拍板，格局打开了。", 
                AttrDelta(work = 4, reputation = 3, colleagues = -3),
                governance = pg(ind = mapOf(Indicator.ECONOMY to 3), merit = mapOf(MeritKind.ECONOMIC to 3))),
            c("反复协商，求最大共识", Attitude.STEADY, "方案磨了很久，各方都能接受。", 
                AttrDelta(coordination = 4, politics = 2, energy = -4)),
            c("先放一放，等时机成熟", Attitude.CAUTIOUS, "规划暂缓，短期没出问题，长期机会在流失。", 
                AttrDelta(politics = 2, work = -2))
        )
    ),
    ev(
        id = "p15_city_cross_region", title = "跨区域产业带",
        background = "周边地区主动来谈合作", description = "产业带能做大蛋糕，但谁主导、谁受益，需要谈清楚。",
        tags = tags(EventTag.COORDINATION, EventTag.PROJECT, EventTag.RESEARCH),
        weight = 11, minPos = 7, repeatable = true, cooldown = 24,
        requiredScope = PositionScope.CITY,
        choices = listOf(
            c("主动牵头，做大平台", Attitude.BOLD, "你牵头搭起了合作框架，产业带初具雏形。", 
                AttrDelta(work = 4, coordination = 3, energy = -6),
                governance = pg(ind = mapOf(Indicator.ECONOMY to 4), merit = mapOf(MeritKind.ECONOMIC to 3), finance = -4)),
            c("平等协商，利益共享", Attitude.STEADY, "你坚持利益共享，合作谈成了。", 
                AttrDelta(coordination = 3, reputation = 2)),
            c("守住本地利益再说", Attitude.CAUTIOUS, "你优先保住本地盘子，合作规模小了一些。", 
                AttrDelta(politics = 3, work = 1, reputation = -1))
        )
    ),
    ev(
        id = "p15_city_policy_pilot", title = "改革试点申报",
        background = "上级给了一个试点名额", description = "试点做成了是政绩，做砸了要担责任。",
        tags = tags(EventTag.CAREER, EventTag.MEETING, EventTag.MATERIAL),
        weight = 11, minPos = 8, repeatable = true, cooldown = 26,
        requiredScope = PositionScope.CITY,
        choices = listOf(
            c("积极申报，争取先机", Attitude.BOLD, "你带队把方案做了出来，拿到了试点。", 
                AttrDelta(work = 4, ability = 3, leaderTrust = 3, energy = -5),
                politics = PoliticalImpact(capital = 4, tendencies = mapOf(PoliticalTendency.REFORMIST to 4))),
            c("量力而行，稳妥申报", Attitude.STEADY, "你选了一个把握较大的方向申报。", 
                AttrDelta(politics = 3, writing = 2)),
            c("不申报，先守住基本盘", Attitude.CAUTIOUS, "你放弃了这次机会，本地平稳如常。", 
                AttrDelta(politics = 2, leaderTrust = -2))
        )
    ),
    ev(
        id = "p15_province_policy", title = "全省性政策出台",
        background = "一项政策要覆盖全省", description = "政策一旦下发，影响的是很多地方、很多人。",
        tags = tags(EventTag.MEETING, EventTag.MATERIAL, EventTag.CAREER),
        weight = 12, minPos = 9, repeatable = true, cooldown = 26,
        requiredScope = PositionScope.PROVINCE,
        choices = listOf(
            c("先行先试，边推边改", Attitude.BOLD, "政策先落地，问题在实践中改。", 
                AttrDelta(work = 5, reputation = 3, integrityRisk = 3),
                politics = PoliticalImpact(capital = 4, tendencies = mapOf(PoliticalTendency.REFORMIST to 4))),
            c("充分征求意见再定稿", Attitude.STEADY, "政策反复打磨，出台后执行顺畅。", 
                AttrDelta(politics = 4, coordination = 3, energy = -4)),
            c("小范围试行，观察效果", Attitude.CAUTIOUS, "政策先在少数地方试行，稳妥但慢。", 
                AttrDelta(politics = 3, work = 1))
        )
    ),
    ev(
        id = "p15_province_reform", title = "重大改革方案",
        background = "改革进入深水区", description = "触动利益比触动灵魂还难，方案需要担当。",
        tags = tags(EventTag.ASSESSMENT, EventTag.MEETING, EventTag.RESEARCH),
        weight = 11, minPos = 9, repeatable = true, cooldown = 30,
        requiredScope = PositionScope.PROVINCE,
        choices = listOf(
            c("迎难而上，啃硬骨头", Attitude.RISKY, "你选择正面突破，争议和成效一起到来。", 
                AttrDelta(work = 5, ability = 3, reputation = 2, integrityRisk = 4, energy = -6),
                politics = PoliticalImpact(capital = 3, tendencies = mapOf(PoliticalTendency.REFORMIST to 5))),
            c("分步推进，稳中求进", Attitude.STEADY, "你按步骤推进，阻力被一点点化解。", 
                AttrDelta(politics = 4, coordination = 3, work = 2)),
            c("维持现状，避免风险", Attitude.CAUTIOUS, "你选择观望，短期内风平浪静。", 
                AttrDelta(politics = 3, work = -2, reputation = -2))
        )
    ),
    ev(
        id = "p15_province_budget", title = "全省财政统筹",
        background = "盘子大，缺口也大", description = "钱往哪里投，决定了未来几年哪里先发展。",
        tags = tags(EventTag.MATERIAL, EventTag.PROJECT, EventTag.MEETING),
        weight = 11, minPos = 10, repeatable = true, cooldown = 28,
        requiredScope = PositionScope.PROVINCE,
        choices = listOf(
            c("集中投重点区域", Attitude.BOLD, "资源集中，重点区域很快见了成效。", 
                AttrDelta(work = 4, ability = 2, colleagues = -3),
                governance = pg(ind = mapOf(Indicator.ECONOMY to 3), finance = 4)),
            c("均衡分配，兼顾公平", Attitude.STEADY, "各地都拿到了一部分，差距没有扩大。", 
                AttrDelta(politics = 4, reputation = 2)),
            c("压缩开支，过紧日子", Attitude.CAUTIOUS, "支出压下来了，日子紧了，抱怨也多了。", 
                AttrDelta(integrityRisk = -3, reputation = -2, work = -1))
        )
    ),
    ev(
        id = "p15_national_strategy", title = "国家层面战略布局",
        background = "一项长期战略需要拍板", description = "这一步走对了，影响很多年；走错了，代价同样深远。",
        tags = tags(EventTag.CAREER, EventTag.MEETING, EventTag.RESEARCH),
        weight = 12, minPos = 11, repeatable = true, cooldown = 32,
        requiredScope = PositionScope.NATIONAL,
        choices = listOf(
            c("着眼长远，果断布局", Attitude.BOLD, "你把目光放在了十年之后。", 
                AttrDelta(work = 5, ability = 4, reputation = 4, energy = -8),
                politics = PoliticalImpact(capital = 5, tendencies = mapOf(PoliticalTendency.REFORMIST to 4))),
            c("统筹兼顾，稳步推进", Attitude.STEADY, "你把节奏控制在一个能承受的范围。", 
                AttrDelta(politics = 5, coordination = 4)),
            c("审慎观望，控制风险", Attitude.CAUTIOUS, "你选择了更为保守的路径。", 
                AttrDelta(politics = 3, work = -1))
        )
    ),
    ev(
        id = "p15_national_legacy", title = "梯队建设与传承",
        background = "要为自己身后做安排", description = "培养谁、交给谁，关乎事业能否延续。",
        tags = tags(EventTag.CAREER, EventTag.LEADER_TALK, EventTag.ASSESSMENT),
        weight = 11, minPos = 11, repeatable = true, cooldown = 34,
        requiredScope = PositionScope.NATIONAL,
        choices = listOf(
            c("大胆起用新人，交担子", Attitude.BOLD, "你把重要工作交给了年轻人。", 
                AttrDelta(work = 3, reputation = 3, colleagues = -1)),
            c("按梯队逐级培养", Attitude.STEADY, "你按部就班地安排培养计划。", 
                AttrDelta(politics = 4, coordination = 3)),
            c("先稳住队伍，不急于调整", Attitude.CAUTIOUS, "你维持了现状，队伍稳定。", 
                AttrDelta(politics = 3, reputation = -1))
        )
    )
)