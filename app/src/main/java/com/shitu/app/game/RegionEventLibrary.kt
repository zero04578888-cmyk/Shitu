package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.EventTag
import com.shitu.app.model.Faction
import com.shitu.app.model.GovernanceEffect
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.model.PoliticalEffect
import com.shitu.app.model.PoliticalImpact
import com.shitu.app.model.PoliticalTendency

/**
 * 第十二阶段：地区、调任、调研、会议与项目相关的事件。
 *
 * 这些事件都读取玩家当前的地区特色与治理状态，
 * 不同地区的同一次抉择可能带来完全不同的后果。全部为虚构设定。
 */
private fun g(
    ind: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    savings: Int = 0,
    risk: Int = 0
) = GovernanceEffect(
    indicators = ind, merit = merit, finance = finance, savings = savings, risk = risk
)

internal val regionEventLibrary: List<com.shitu.app.model.GameEvent> = listOf(
    ev(
        id = "r12_arrival_survey", title = "到任摸底",
        background = "刚到新岗位，情况还不熟",
        description = "办公室送来厚厚一摞材料，底下的人也在等你表态。你打算先从哪里入手？",
        tags = tags(EventTag.RESEARCH, EventTag.MEETING),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c(
                "先下基层跑一圈，把真实情况摸清楚", Attitude.STEADY,
                "你用了半个月走遍主要村和社区。情况比材料上写的复杂，但心里有了底。",
                AttrDelta(masses = 3, work = 2, ability = 2, energy = -4),
                HiddenDelta(opportunity = 3, network = 2),
                governance = g(ind = mapOf(Indicator.APPROVAL to 2), merit = mapOf(MeritKind.LIVELIHOOD to 2)),
                hint = "基层与实情"
            ),
            c(
                "先开干部会，把队伍拢起来", Attitude.BOLD,
                "你把中层干部叫到一起，当面把任务压了下去。有人服气，也有人心里打鼓。",
                AttrDelta(coordination = 3, colleagues = 2, leaderTrust = 1),
                HiddenDelta(stress = 3),
                hint = "队伍与执行"
            ),
            c(
                "先研究材料，等看清了再动", Attitude.CAUTIOUS,
                "你把近年来的报表、信访、审计材料都翻了一遍，方案还没有落地。",
                AttrDelta(writing = 3, politics = 2, ability = 2, masses = -1),
                HiddenDelta(opportunity = -1),
                hint = "稳妥与谨慎"
            )
        )
    ),
    ev(
        id = "r12_leading_firm", title = "走访龙头企业",
        background = "本地最大的企业递话想见你",
        description = "企业负责人热情得很，谈完经营，话锋一转，提到了几个具体的「困难」。",
        tags = tags(EventTag.PROJECT, EventTag.INTEGRITY, EventTag.LEADER_TASK),
        weight = 11, repeatable = true, cooldown = 14,
        choices = listOf(
            c(
                "当场表态支持，但把规矩讲在前面", Attitude.STEADY,
                "你答应在政策范围内帮企业解决问题，也明确说了底线。对方笑了笑，没再提别的。",
                AttrDelta(work = 2, coordination = 2, politics = 2, integrityRisk = -2),
                governance = g(ind = mapOf(Indicator.ECONOMY to 2), merit = mapOf(MeritKind.ECONOMIC to 3)),
                hint = "亲清有度"
            ),
            c(
                "先解决问题，别的以后再说", Attitude.RISKY,
                "你现场拍板协调了几个部门，企业很满意，效率也高，只是程序上留了点尾巴。",
                AttrDelta(work = 2, ability = 2, integrityRisk = 3),
                governance = g(ind = mapOf(Indicator.ECONOMY to 3), merit = mapOf(MeritKind.ECONOMIC to 4), risk = 4),
                hint = "效率与风险"
            ),
            c(
                "只听不说，把问题记下来", Attitude.CAUTIOUS,
                "你始终没有表态，对方也摸不准你的意思。事情暂时搁下了。",
                AttrDelta(politics = 2, writing = 2, work = 1),
                hint = "留有余地"
            )
        )
    ),
    ev(
        id = "r12_town_cadres", title = "乡镇干部座谈",
        background = "老乡镇们有一肚子话",
        description = "座谈谈到一半，一个干了二十年的老书记直接说：上面的任务，下面真的接不住了。",
        tags = tags(EventTag.MEETING, EventTag.COLLEAGUE, EventTag.COORDINATION),
        weight = 11, repeatable = true, cooldown = 14,
        choices = listOf(
            c(
                "认真听，回去就减负", Attitude.BOLD,
                "你把能减的任务清单当场列了出来。基层叫好，但个别部门觉得你在给他们添麻烦。",
                AttrDelta(masses = 3, colleagues = 3, coordination = 2, leaderTrust = -2),
                HiddenDelta(network = 2),
                governance = g(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 4))
            ),
            c(
                "鼓励大家再顶一顶", Attitude.STEADY,
                "你肯定了大家的付出，也把上级的压力原原本本讲了。会场安静了下来。",
                AttrDelta(leaderTrust = 3, coordination = 1, masses = -1),
                governance = g(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to -1))
            ),
            c(
                "把问题整理成报告往上报", Attitude.STEADY,
                "你连夜写了份调研报告。上级批了「阅」，事情有没有下文，谁也说不准。",
                AttrDelta(writing = 3, leaderTrust = 1, ability = 1),
                HiddenDelta(opportunity = 2)
            )
        )
    ),
    ev(
        id = "r12_field_water", title = "田间地头的水",
        background = "两个村为灌溉用水起了争执",
        description = "上游村把水渠截了，下游村的秧苗开始打蔫，两边的人都堵在渠边。",
        tags = tags(EventTag.RURAL, EventTag.MASS_APPEAL, EventTag.COMPLAINT),
        weight = 13, repeatable = true, cooldown = 12,
        units = setOf(com.shitu.app.model.UnitType.TOWN, com.shitu.app.model.UnitType.STREET),
        choices = listOf(
            c(
                "现场协调，按亩分水", Attitude.BOLD,
                "你顶着太阳在渠边待了一下午，定下了轮灌的办法。两边都不太满意，但水通到了地里。",
                AttrDelta(masses = 3, work = 2, coordination = 2, energy = -3),
                governance = g(ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 2))
            ),
            c(
                "紧急申请资金，把渠修一遍", Attitude.STEADY,
                "你打了报告要钱修渠。短期内水还是不够用，但根子上的问题有了着落。",
                AttrDelta(work = 2, politics = 2),
                governance = g(ind = mapOf(Indicator.INFRASTRUCTURE to 3), merit = mapOf(MeritKind.LIVELIHOOD to 2), finance = -8)
            ),
            c(
                "交给村里自己协商", Attitude.CAUTIOUS,
                "你说这是村民自治的事。水最后还是分不均，投诉信寄到了县里。",
                AttrDelta(masses = -2, leaderTrust = -1),
                governance = g(ind = mapOf(Indicator.APPROVAL to -3))
            )
        )
    ),
    ev(
        id = "r12_scenic_order", title = "景区秩序",
        background = "旅游旺季，投诉也跟着来了",
        description = "游客多了，宰客、乱收费、停车难的投诉一条接一条，文旅局和市场监管局互相推。",
        tags = tags(EventTag.COMPLAINT, EventTag.PUBLIC_OPINION, EventTag.MEETING),
        weight = 11, repeatable = true, cooldown = 14,
        choices = listOf(
            c(
                "联合执法，先治乱象", Attitude.BOLD,
                "你牵头组织了一次联合整治，几家商户被处理。游客叫好，本地个别人有怨气。",
                AttrDelta(coordination = 3, masses = 2, work = 2),
                governance = g(ind = mapOf(Indicator.APPROVAL to 3, Indicator.ECONOMY to 1), merit = mapOf(MeritKind.LIVELIHOOD to 2))
            ),
            c(
                "先把停车和引导做起来", Attitude.STEADY,
                "你协调了几个临时停车场和志愿者队伍，第二天投诉就少了一半。",
                AttrDelta(coordination = 2, ability = 2),
                governance = g(ind = mapOf(Indicator.APPROVAL to 2, Indicator.INFRASTRUCTURE to 2), finance = -5)
            ),
            c(
                "要求各单位各负其责", Attitude.CAUTIOUS,
                "你发了文、开了会。表面上各自都动了，实际问题还是没解决。",
                AttrDelta(politics = 2, coordination = -1),
                governance = g(ind = mapOf(Indicator.APPROVAL to -1))
            )
        )
    ),
    ev(
        id = "r12_resource_glut", title = "资源价格下行",
        background = "本地支柱产业遇到了周期",
        description = "大宗商品价格下来了，矿区几家企业的效益明显缩水，工人的收入也跟着降。",
        tags = tags(EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.RESEARCH),
        weight = 11, repeatable = true, cooldown = 16,
        choices = listOf(
            c(
                "借机推动转型，上新的产业", Attitude.BOLD,
                "你提出把资源收益的一部分投到新产业上。有人叫好，也有人觉得不切实际。",
                AttrDelta(ability = 3, coordination = 2, work = 2),
                politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.REFORMIST to 4)),
                governance = g(ind = mapOf(Indicator.ECONOMY to 2), merit = mapOf(MeritKind.INNOVATION to 4), finance = -10),
                hint = "改革与转型"
            ),
            c(
                "先稳住企业，保住就业", Attitude.STEADY,
                "你协调金融机构给企业续贷，工人没有大规模下岗，但财政也担了压力。",
                AttrDelta(work = 2, masses = 2, integrityRisk = 2),
                governance = g(ind = mapOf(Indicator.LIVELIHOOD to 3), merit = mapOf(MeritKind.LIVELIHOOD to 3), finance = -12)
            ),
            c(
                "让市场自己调整", Attitude.CAUTIOUS,
                "你没有出手。企业收缩，短期财政数据难看，长期倒也少了些包袱。",
                AttrDelta(politics = 2, ability = 1),
                governance = g(ind = mapOf(Indicator.ECONOMY to -2, Indicator.LIVELIHOOD to -3))
            )
        )
    ),
    ev(
        id = "r12_hard_region_debt", title = "困难地区的欠账",
        background = "移交清单上一串历史遗留问题",
        description = "上任交接时，前任留给你的是几个停工的工程、一批拖欠的款项和几件没解决的信访。",
        tags = tags(EventTag.INSPECTION, EventTag.COMPLAINT, EventTag.PROJECT),
        weight = 12, repeatable = false,
        choices = listOf(
            c(
                "一件一件啃，先把最急的解决", Attitude.BOLD,
                "你排了张时间表，最急的民生欠账先还上。进展慢，但群众开始信你。",
                AttrDelta(work = 3, masses = 3, ability = 2, energy = -4),
                governance = g(ind = mapOf(Indicator.APPROVAL to 4, Indicator.LIVELIHOOD to 2), finance = -8, merit = mapOf(MeritKind.LIVELIHOOD to 3))
            ),
            c(
                "向上级如实报告，争取支持", Attitude.STEADY,
                "你把家底写成报告报了上去，争取到一笔专项资金和缓期政策。",
                AttrDelta(writing = 3, leaderTrust = 2, politics = 2),
                governance = g(finance = 15, ind = mapOf(Indicator.APPROVAL to 1))
            ),
            c(
                "能拖的先拖一拖", Attitude.CAUTIOUS,
                "你把不紧急的先放一边。短期账面好看了，但几个信访件成了隐患。",
                AttrDelta(politics = 2, masses = -2, integrityRisk = 2),
                governance = g(ind = mapOf(Indicator.APPROVAL to -3), risk = 3)
            )
        )
    ),
    ev(
        id = "r12_strong_region_race", title = "强区里的节奏",
        background = "能人多，标准高",
        description = "这里的经济数据漂亮，工作标准也高。一个副职出现空缺，组织上会看谁平时把事做得扎实。",
        tags = tags(EventTag.ASSESSMENT, EventTag.CAREER, EventTag.LEADER_TALK),
        weight = 11, repeatable = true, cooldown = 20,
        minPos = 3,
        choices = listOf(
            c(
                "用成绩说话，把手里的事做到最好", Attitude.STEADY,
                "你没有去活动，而是把手头的工作做出了亮点。组织部门来考察时，材料很硬。",
                AttrDelta(work = 3, ability = 2, reputation = 2),
                politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.PRAGMATIC to 4)),
                hint = "实干路线"
            ),
            c(
                "主动向领导汇报工作，争取了解", Attitude.BOLD,
                "你找机会汇报了几次。领导对你有了印象，但也有人觉得你太急。",
                AttrDelta(leaderTrust = 3, politics = 2, colleagues = -2),
                politics = PoliticalImpact(capital = 4),
                hint = "争取与分寸"
            ),
            c(
                "静观其变，等组织安排", Attitude.CAUTIOUS,
                "你按自己的节奏做事，没有多去活动。",
                AttrDelta(politics = 2, ability = 1),
                politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.BALANCED to 3))
            )
        )
    ),
    ev(
        id = "r12_transfer_decision", title = "一纸调任通知",
        background = "组织上找你谈话",
        description = "谈话很客气，意思很清楚：另一个地区需要人，问你个人有没有困难。",
        tags = tags(EventTag.CAREER, EventTag.LEADER_TALK, EventTag.PROJECT),
        weight = 12, repeatable = true, cooldown = 24,
        minPos = 2,
        choices = listOf(
            c(
                "服从安排，到新地方去", Attitude.BOLD,
                "你表态服从。新地方条件差一些，但你能感觉到组织上记住了你这份担当。",
                AttrDelta(leaderTrust = 4, work = 2, reputation = 2, family = -3),
                politics = PoliticalImpact(capital = 5, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3)),
                flag = "accepted_transfer",
                hint = "服从与担当"
            ),
            c(
                "如实说明家庭困难，请求考虑", Attitude.STEADY,
                "你把家里的实际情况讲了。领导没有勉强，但这件事多少留下了点印象。",
                AttrDelta(politics = 2, family = 2, leaderTrust = -1),
                hint = "家庭与前途"
            ),
            c(
                "主动提出去最困难的地方锻炼", Attitude.RISKY,
                "你主动请缨去了最难的地方。有人觉得你傻，也有人开始重新看你。",
                AttrDelta(leaderTrust = 3, work = 3, reputation = 3, health = -3, energy = -5),
                politics = PoliticalImpact(capital = 6, tendencies = mapOf(PoliticalTendency.REFORMIST to 4)),
                governance = g(finance = -5),
                flag = "chose_hard_region",
                hint = "自我加压"
            )
        )
    ),
    ev(
        id = "r12_research_finding", title = "调研发现",
        background = "一份数据对不上",
        description = "你在调研时发现，某项上报的数据和实际情况明显对不上。",
        tags = tags(EventTag.RESEARCH, EventTag.INSPECTION, EventTag.INTEGRITY),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c(
                "如实上报，要求整改", Attitude.BOLD,
                "你把问题原原本本报了上去。上级认可你的认真，基层有人觉得你不讲情面。",
                AttrDelta(integrityRisk = -3, leaderTrust = 3, work = 2),
                governance = g(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to -2), merit = mapOf(MeritKind.FINANCE to 3))
            ),
            c(
                "先给基层机会自行纠正", Attitude.STEADY,
                "你把负责人叫来谈了一次，限期整改。对方感激，也真的改了。",
                AttrDelta(politics = 2, colleagues = 2, coordination = 1),
                governance = g(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 2))
            ),
            c(
                "暂时按下不表，避免影响全局", Attitude.CAUTIOUS,
                "你选择了睁一只眼闭一只眼。数据好看了，但你心里清楚这是个雷。",
                AttrDelta(politics = 3, integrityRisk = 4),
                governance = g(risk = 4)
            )
        )
    ),
    ev(
        id = "r12_old_debt_hidden", title = "挖出的旧账",
        background = "项目档案里少了一份批文",
        description = "你翻旧档案时发现，一个正在推进的项目，当年有一道手续并不齐全。",
        tags = tags(EventTag.INSPECTION, EventTag.PROJECT, EventTag.INTEGRITY),
        weight = 11, repeatable = true, cooldown = 20,
        choices = listOf(
            c(
                "主动补办手续，把窟窿堵上", Attitude.BOLD,
                "你顶住压力补办了手续，项目得以继续，也没有留下把柄。",
                AttrDelta(work = 2, integrityRisk = -4, coordination = 2),
                governance = g(finance = -6, risk = -3)
            ),
            c(
                "向上级报告，听候处理", Attitude.STEADY,
                "你如实报告。上级肯定了你的态度，也要求限期整改。",
                AttrDelta(leaderTrust = 3, integrityRisk = -2, writing = 2)
            ),
            c(
                "装作没看见，继续推进", Attitude.RISKY,
                "项目照常推进。这件事成了你档案里的一个隐患。",
                AttrDelta(integrityRisk = 6),
                governance = g(risk = 5, merit = mapOf(MeritKind.ECONOMIC to 2))
            )
        )
    ),
    ev(
        id = "r12_budget_meeting", title = "预算会上的刀",
        background = "各部门都来要钱",
        description = "预算会开了三个小时，教育、医疗、住建、交通都把自己的项目排在前面。",
        tags = tags(EventTag.MEETING, EventTag.COORDINATION, EventTag.PROJECT),
        weight = 12, repeatable = true, cooldown = 12,
        choices = listOf(
            c(
                "保民生，压一般性支出", Attitude.BOLD,
                "你把民生项目排在前面，砍掉了一批可有可无的会议和接待。有人不高兴。",
                AttrDelta(masses = 3, coordination = 2, colleagues = -2),
                governance = g(ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.EDUCATION to 2), merit = mapOf(MeritKind.LIVELIHOOD to 4), finance = 8)
            ),
            c(
                "按项目效益排，谁产出高给谁", Attitude.STEADY,
                "你按投入产出比重新排了序。经济部门满意，民生部门有意见。",
                AttrDelta(ability = 3, work = 2, masses = -1),
                governance = g(ind = mapOf(Indicator.ECONOMY to 3), merit = mapOf(MeritKind.ECONOMIC to 4), finance = 6)
            ),
            c(
                "先保障运转，项目往后放", Attitude.CAUTIOUS,
                "你把「三保」放在最前面。项目停了几个，但工资和运转没有出问题。",
                AttrDelta(politics = 3, colleagues = 2),
                governance = g(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 3), merit = mapOf(MeritKind.FINANCE to 3), finance = 10)
            )
        )
    ),
    ev(
        id = "r12_meeting_clash", title = "会上的分歧",
        background = "两位分管领导当着众人争了起来",
        description = "一个要坚持上项目，一个说财政承受不住。所有人都看向主持会议的你。",
        tags = tags(EventTag.MEETING, EventTag.COORDINATION, EventTag.COLLEAGUE),
        weight = 12, repeatable = true, cooldown = 12,
        choices = listOf(
            c(
                "当场拍板，支持上项目", Attitude.BOLD,
                "你拍了板。项目启动，但财政压力和政治风险都压在了你身上。",
                AttrDelta(coordination = 2, work = 2, integrityRisk = 2),
                politics = PoliticalImpact(factionInfluence = mapOf(Faction.PRAGMATIC to 3), tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3)),
                governance = g(ind = mapOf(Indicator.ECONOMY to 3), finance = -12, risk = 3)
            ),
            c(
                "折中：分期实施，先做一期", Attitude.STEADY,
                "你提出了分期的办法，两边都下了台阶。事情推进了，也没有谁完全满意。",
                AttrDelta(coordination = 3, politics = 2),
                politics = PoliticalImpact(factionInfluence = mapOf(Faction.NEUTRAL to 2)),
                governance = g(ind = mapOf(Indicator.ECONOMY to 2), finance = -6, merit = mapOf(MeritKind.INNOVATION to 2))
            ),
            c(
                "暂缓决定，让部门先做方案", Attitude.CAUTIOUS,
                "你让两边回去各做一版方案。会开完了，问题还在。",
                AttrDelta(politics = 3, coordination = -1),
                governance = g(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to -2))
            )
        )
    ),
    ev(
        id = "r12_safety_meeting", title = "安全生产的弦",
        background = "外地出了事故，上级连夜开会",
        description = "邻市发生一起安全生产事故，要求各地立即排查隐患。你辖区也有几处老问题。",
        tags = tags(EventTag.EMERGENCY, EventTag.INSPECTION, EventTag.ACCIDENT),
        weight = 12, repeatable = true, cooldown = 14,
        choices = listOf(
            c(
                "连夜排查，该停的停", Attitude.BOLD,
                "你带队查了几家企业，关停了两处隐患点。企业主不高兴，但事故没有发生。",
                AttrDelta(work = 3, coordination = 2, energy = -4, integrityRisk = -1),
                governance = g(ind = mapOf(Indicator.SECURITY to 5), merit = mapOf(MeritKind.SECURITY to 5), finance = -6)
            ),
            c(
                "开会部署，逐级压实责任", Attitude.STEADY,
                "你开了会、签了责任书。排查在推进，但力度不如亲自去。",
                AttrDelta(coordination = 2, writing = 2),
                governance = g(ind = mapOf(Indicator.SECURITY to 3), merit = mapOf(MeritKind.SECURITY to 3))
            ),
            c(
                "先抓重点，不搞运动式排查", Attitude.CAUTIOUS,
                "你把有限的精力放在最危险的两处。其他地方只能等下一轮。",
                AttrDelta(ability = 3, politics = 2),
                governance = g(ind = mapOf(Indicator.SECURITY to 2), merit = mapOf(MeritKind.SECURITY to 2))
            )
        )
    ),
    ev(
        id = "r12_project_land", title = "项目征地",
        background = "补偿标准谈不拢",
        description = "园区要扩建，涉及一个组的土地。补偿标准谈了几轮，还有几户不肯签。",
        tags = tags(EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.COMPLAINT),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c(
                "逐户上门，把政策讲透", Attitude.BOLD,
                "你带着工作组一户一户谈。最后几户看到政策是统一的，也签了。",
                AttrDelta(masses = 3, work = 3, energy = -4),
                governance = g(ind = mapOf(Indicator.APPROVAL to 2, Indicator.ECONOMY to 2), merit = mapOf(MeritKind.ECONOMIC to 3))
            ),
            c(
                "依法依规，走法律程序", Attitude.STEADY,
                "你坚持按程序办事，进度慢了下来，但没人能说你不合规。",
                AttrDelta(politics = 3, integrityRisk = -2, work = 1),
                governance = g(ind = mapOf(Indicator.ECONOMY to 1), merit = mapOf(MeritKind.FINANCE to 2))
            ),
            c(
                "个别户提高补偿，先把地拿到", Attitude.RISKY,
                "项目如期开工，但「会闹的有糖吃」的风声传了出去，后面更难谈了。",
                AttrDelta(work = 2, integrityRisk = 4),
                governance = g(ind = mapOf(Indicator.ECONOMY to 3, Indicator.APPROVAL to -2), finance = -12, risk = 4)
            )
        )
    ),
    ev(
        id = "r12_project_gap", title = "项目资金缺口",
        background = "工程干到一半，钱不够了",
        description = "施工方找上门，说再不到位，工地就要停。财政的盘子也很紧。",
        tags = tags(EventTag.PROJECT, EventTag.INSPECTION, EventTag.MEETING),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c(
                "压缩其他开支，保工程", Attitude.BOLD,
                "你停了几项一般性支出，把资金挪了过来。工程保住了，别的事往后放。",
                AttrDelta(coordination = 2, work = 2),
                governance = g(ind = mapOf(Indicator.INFRASTRUCTURE to 3), merit = mapOf(MeritKind.ECONOMIC to 3), finance = -14)
            ),
            c(
                "向上级申请专项支持", Attitude.STEADY,
                "你带着材料跑了几个部门，争取到了一部分资金。",
                AttrDelta(writing = 2, leaderTrust = 2, politics = 2),
                governance = g(finance = 12, ind = mapOf(Indicator.INFRASTRUCTURE to 1))
            ),
            c(
                "暂缓施工，等资金到位", Attitude.CAUTIOUS,
                "工地停了下来。省下了钱，但工期和口碑都受了影响。",
                AttrDelta(politics = 2, work = -1),
                governance = g(ind = mapOf(Indicator.APPROVAL to -2), merit = mapOf(MeritKind.ECONOMIC to -2))
            )
        )
    ),
    ev(
        id = "r12_project_quality", title = "工程质量投诉",
        background = "有群众反映新修的路鼓包了",
        description = "刚完工没多久的路面出现了鼓包，群众拍了照片发到网上，也有人怀疑偷工减料。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.PROJECT, EventTag.INSPECTION),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c(
                "立即停工彻查，公开结果", Attitude.BOLD,
                "你请了第三方检测，发现问题后要求施工方返工。过程不体面，但处理得干净。",
                AttrDelta(integrityRisk = -3, leaderTrust = 2, coordination = 2),
                governance = g(ind = mapOf(Indicator.INFRASTRUCTURE to -1), merit = mapOf(MeritKind.FINANCE to 3), finance = -6)
            ),
            c(
                "要求施工方限期整改", Attitude.STEADY,
                "你把施工方叫来，限期整改。路补好了，网上的热度也下去了。",
                AttrDelta(coordination = 2, work = 1),
                governance = g(ind = mapOf(Indicator.APPROVAL to 1))
            ),
            c(
                "先冷处理，等舆论过去", Attitude.CAUTIOUS,
                "你没有正面回应。事情慢慢淡了，但当地的信任也淡了一些。",
                AttrDelta(politics = 2, masses = -2),
                governance = g(ind = mapOf(Indicator.APPROVAL to -3), risk = 3)
            )
        )
    ),
    ev(
        id = "r12_project_done", title = "项目验收",
        background = "第一个大项目要验收了",
        description = "该项目是你到任后主抓的第一个大工程，验收结果直接关系到后面的评价。",
        tags = tags(EventTag.ACCEPTANCE, EventTag.PROJECT, EventTag.LEADER_TASK),
        weight = 12, repeatable = true, cooldown = 20,
        choices = listOf(
            c(
                "请上级和群众一起看，把成绩亮出来", Attitude.BOLD,
                "你把验收做成了现场会，上级给了肯定，群众也点了头。",
                AttrDelta(leaderTrust = 3, reputation = 3, coordination = 2),
                governance = g(merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.LIVELIHOOD to 2))
            ),
            c(
                "低调验收，把问题留着自己改", Attitude.STEADY,
                "你没有大张旗鼓，验收中发现的几个小问题当场就安排了整改。",
                AttrDelta(ability = 3, work = 2, integrityRisk = -2)
            ),
            c(
                "赶在节点前完工，先把进度报上去", Attitude.RISKY,
                "验收赶在节点前完成了。数据漂亮，但个别细节经不起细看。",
                AttrDelta(work = 2, integrityRisk = 3),
                governance = g(merit = mapOf(MeritKind.ECONOMIC to 4), risk = 3)
            )
        )
    ),
    ev(
        id = "r12_economy_headwind", title = "经济下行压力",
        background = "半年数据不好看",
        description = "几家规上企业产值下滑，税收增速放缓，上级已经开始点名。",
        tags = tags(EventTag.PROJECT, EventTag.LEADER_TASK, EventTag.RESEARCH),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c(
                "带队外出招商，抢项目", Attitude.BOLD,
                "你带着队伍跑了几座城市，谈成两个意向项目。短期花钱，长期可期。",
                AttrDelta(work = 3, coordination = 2, energy = -4),
                governance = g(ind = mapOf(Indicator.ECONOMY to 3), merit = mapOf(MeritKind.ECONOMIC to 4), finance = -10)
            ),
            c(
                "帮现有企业稳产扩产", Attitude.STEADY,
                "你把重点放在现有企业上，帮他们解决了几个实际困难。",
                AttrDelta(work = 2, masses = 2),
                governance = g(ind = mapOf(Indicator.ECONOMY to 2, Indicator.LIVELIHOOD to 2), merit = mapOf(MeritKind.ECONOMIC to 3))
            ),
            c(
                "如实报告困难，争取政策", Attitude.CAUTIOUS,
                "你把困难原原本本报了上去，争取到一点政策空间。",
                AttrDelta(writing = 3, leaderTrust = 1, politics = 2),
                governance = g(finance = 8)
            )
        )
    ),
    ev(
        id = "r12_env_inspection", title = "环保督察",
        background = "督察组来了",
        description = "上级环保督察组进驻，交办了一批问题，其中两个涉及本地的重点企业。",
        tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY, EventTag.PROJECT),
        weight = 12, repeatable = true, cooldown = 18,
        choices = listOf(
            c(
                "照单全收，该罚的罚、该停的停", Attitude.BOLD,
                "你顶住说情的压力，关停整改了几家企业。环境数据好转，经济数据短期受影响。",
                AttrDelta(integrityRisk = -4, leaderTrust = 3, work = 3),
                governance = g(ind = mapOf(Indicator.ENVIRONMENT to 6, Indicator.ECONOMY to -2), merit = mapOf(MeritKind.SECURITY to 3), finance = -8)
            ),
            c(
                "分类处置，给出整改时间表", Attitude.STEADY,
                "你没有一刀切，而是分类给了整改方案。督察组认为处置稳妥。",
                AttrDelta(coordination = 3, ability = 2),
                governance = g(ind = mapOf(Indicator.ENVIRONMENT to 4), merit = mapOf(MeritKind.SECURITY to 2))
            ),
            c(
                "先保企业，能拖就拖", Attitude.RISKY,
                "你替企业说了几句话。企业保住了，但督察组记下了这一笔。",
                AttrDelta(integrityRisk = 5, politics = 2),
                governance = g(ind = mapOf(Indicator.ENVIRONMENT to -3), risk = 5)
            )
        )
    )
)


