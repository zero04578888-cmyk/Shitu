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
 * 第十二阶段：岗位条线相关事件。
 *
 * 玩家所在的条线会通过标签权重提高对口事件的出现概率：
 * 办公室遇到材料与会务，住建系统遇到工程与拆迁，应急系统遇到安全与灾害。
 * 全部为虚构设定。
 */
private fun jg(
    ind: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    risk: Int = 0
) = GovernanceEffect(indicators = ind, merit = merit, finance = finance, risk = risk)

internal val jobTrackEventLibrary: List<com.shitu.app.model.GameEvent> = listOf(
    ev(
        id = "j12_office_allnighter", title = "通宵改稿",
        background = "领导明天上午要讲话",
        description = "夜里十一点，办公室的灯还亮着。材料改了第七版，领导的意思还是「再顺一顺」。",
        tags = tags(EventTag.MATERIAL, EventTag.OVERTIME, EventTag.LEADER_TASK),
        weight = 12, repeatable = true, cooldown = 10,
        choices = listOf(
            c("自己顶上，逐字打磨", Attitude.BOLD, "你改到凌晨三点，稿子终于过了。领导在会上念得很顺。",
                AttrDelta(writing = 3, work = 2, leaderTrust = 2, energy = -6, health = -2), HiddenDelta(stress = 4)),
            c("叫上科室一起干", Attitude.STEADY, "你把大家叫回来分工。稿子赶出来了，有人嘴上不说，心里有数。",
                AttrDelta(coordination = 2, writing = 2, colleagues = -2, energy = -4)),
            c("按已有框架交一版，明天再说", Attitude.CAUTIOUS, "你交了一版中规中矩的稿子。领导没说什么，也没记住什么。",
                AttrDelta(writing = 1, leaderTrust = -1, energy = -1))
        )
    ),
    ev(
        id = "j12_office_document", title = "一份文件的下发",
        background = "文件在部门之间转了三圈",
        description = "一份本该上周下发的文件，因为两个部门的表述不一致，至今还压在你桌上。",
        tags = tags(EventTag.MATERIAL, EventTag.COORDINATION, EventTag.MEETING),
        weight = 11, repeatable = true, cooldown = 12,
        choices = listOf(
            c("上门去谈，当面把分歧解决", Attitude.BOLD, "你跑了两趟，把两边的意见捏到了一起。文件当天就发下去了。",
                AttrDelta(coordination = 3, work = 2, colleagues = 2)),
            c("按上级口径定，谁有意见谁提", Attitude.STEADY, "你按上级文件的口径定了稿。有人不满，但事情推下去了。",
                AttrDelta(politics = 3, leaderTrust = 1, colleagues = -1)),
            c("再等等，让两边自己协调", Attitude.CAUTIOUS, "文件又压了几天。基层催了几次，你自己也被动了。",
                AttrDelta(politics = 2, work = -1, leaderTrust = -1))
        )
    ),
    ev(
        id = "j12_org_cadre_review", title = "干部考察",
        background = "组织部来考察一名干部",
        description = "考察对象和你共事多年，工作能力不错，但有两件事一直有争议。",
        tags = tags(EventTag.CAREER, EventTag.ASSESSMENT, EventTag.MEETING),
        weight = 12, repeatable = true, cooldown = 16,
        minPos = 2,
        choices = listOf(
            c("实事求是，优缺点都讲", Attitude.STEADY, "你客观评价了这位干部。考察组记下了，对方也知道了。",
                AttrDelta(politics = 3, integrityRisk = -2, colleagues = 1)),
            c("多讲成绩，帮他一把", Attitude.BOLD, "你替他多说了几句好话。他后来知道你帮了忙。",
                AttrDelta(colleagues = 3, politics = 2, integrityRisk = 3),
                political = PoliticalEffect(relations = mapOf("ma_wenbin" to 4))),
            c("回避，不发表意见", Attitude.CAUTIOUS, "你以不熟悉情况为由回避了。没有人被得罪，也没有人被帮到。",
                AttrDelta(politics = 2))
        )
    ),
    ev(
        id = "j12_org_nomination", title = "推荐干部",
        background = "有一个推荐名额",
        description = "上级给了本地一个推荐名额，符合条件的有好几位，各有各的长处。",
        tags = tags(EventTag.CAREER, EventTag.ASSESSMENT, EventTag.LEADER_TALK),
        weight = 11, repeatable = true, cooldown = 20,
        minPos = 3,
        choices = listOf(
            c("推荐干得最实的那位", Attitude.STEADY, "你按实绩推荐了人。有人服气，也有人觉得你不懂人情。",
                AttrDelta(reputation = 3, colleagues = -1, integrityRisk = -2),
                politics = PoliticalImpact(capital = 3, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3))),
            c("推荐基层经验最丰富的那位", Attitude.BOLD, "你把名额给了在基层干了多年的老同志。基层反响不错。",
                AttrDelta(masses = 3, reputation = 2, leaderTrust = -1),
                politics = PoliticalImpact(factionInfluence = mapOf(Faction.LOCAL to 3))),
            c("按资历排序，谁都不得罪", Attitude.CAUTIOUS, "你选了资历最老的那位。没出错，也没有惊喜。",
                AttrDelta(politics = 3, reputation = -1))
        )
    ),
    ev(
        id = "j12_propaganda_hot", title = "舆情升温",
        background = "一条短视频正在发酵",
        description = "有人把一段与本地有关的视频发到了网上，评论区已经吵翻了，真假难辨。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.COMPLAINT, EventTag.MATERIAL),
        weight = 13, repeatable = true, cooldown = 10,
        choices = listOf(
            c("第一时间核实，如实回应", Attitude.BOLD, "你连夜核清事实，第二天就把情况说明了。舆论很快平息。",
                AttrDelta(writing = 3, integrityRisk = -3, leaderTrust = 2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 2))),
            c("先撤热度，再慢慢解释", Attitude.CAUTIOUS, "视频是压下去了，但「是不是有事瞒着」的猜测又起来了。",
                AttrDelta(politics = 2, masses = -2, integrityRisk = 3),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -2), risk = 3)),
            c("让涉事单位自己回应", Attitude.STEADY, "涉事单位出面回应了。口径不太统一，但事情没有失控。",
                AttrDelta(coordination = 2, writing = 1),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -1)))
        )
    ),
    ev(
        id = "j12_propaganda_report", title = "一篇报道的分寸",
        background = "记者想写一篇深度报道",
        description = "记者来采访，说想写写本地这几年的变化，也想写写存在的问题。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.MATERIAL, EventTag.INSPECTION),
        weight = 11, repeatable = true, cooldown = 16,
        choices = listOf(
            c("配合采访，成绩和问题都讲", Attitude.BOLD, "你坦诚地讲了成绩也讲了难处。报道发出来后，评价是「实在」。",
                AttrDelta(writing = 2, reputation = 3, integrityRisk = -2)),
            c("安排几个亮点给记者看", Attitude.STEADY, "记者看了安排好的点，稿子写得漂亮，但少了几分真。",
                AttrDelta(politics = 3, leaderTrust = 2, integrityRisk = 2)),
            c("谨慎接待，不提供额外信息", Attitude.CAUTIOUS, "你把接待控制在最小范围。记者没有多写，也没有多问。",
                AttrDelta(politics = 2, reputation = -1))
        )
    ),
    ev(
        id = "j12_economy_negotiation", title = "招商谈判",
        background = "一个项目到了关键一轮",
        description = "投资方开出的条件里，有几条明显超出了本地的政策空间。",
        tags = tags(EventTag.PROJECT, EventTag.RESEARCH, EventTag.LEADER_TASK),
        weight = 13, repeatable = true, cooldown = 14,
        choices = listOf(
            c("守住底线，能谈就谈", Attitude.STEADY, "你坚持了政策红线，最后双方各让一步，项目签了下来。",
                AttrDelta(ability = 3, work = 2, integrityRisk = -2),
                governance = jg(ind = mapOf(Indicator.ECONOMY to 3), merit = mapOf(MeritKind.ECONOMIC to 4))),
            c("先答应下来，细节以后再谈", Attitude.RISKY, "你把框架协议签了。项目落了地，但后面有些承诺很不好兑现。",
                AttrDelta(work = 2, integrityRisk = 4),
                governance = jg(ind = mapOf(Indicator.ECONOMY to 4), merit = mapOf(MeritKind.ECONOMIC to 4), risk = 4)),
            c("暂缓签约，再向上级请示", Attitude.CAUTIOUS, "你把情况报了上去。项目等了一阵，投资方有点着急。",
                AttrDelta(politics = 3, leaderTrust = 1),
                governance = jg(ind = mapOf(Indicator.ECONOMY to 1)))
        )
    ),
    ev(
        id = "j12_economy_data", title = "指标怎么报",
        background = "统计口径有弹性",
        description = "半年数据出来，真实增速比目标低了一截。有人暗示「口径上可以再研究研究」。",
        tags = tags(EventTag.MATERIAL, EventTag.INTEGRITY, EventTag.LEADER_TASK),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c("据实上报，宁可难看", Attitude.BOLD, "你把真实数据报了上去，也在报告里写清了原因。上级批了「务实」。",
                AttrDelta(integrityRisk = -4, leaderTrust = 2, writing = 2),
                politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3), capital = 3)),
            c("把口径讲清楚，不虚增但也不自曝", Attitude.STEADY, "你在合规范围内把数据说清楚了。上级了解了情况。",
                AttrDelta(politics = 3, writing = 2)),
            c("按暗示处理一下数据", Attitude.RISKY, "数字好看了，你在会上也过了关。但这件事成了把柄。",
                AttrDelta(integrityRisk = 7, leaderTrust = 1),
                governance = jg(risk = 6))
        )
    ),
    ev(
        id = "j12_finance_squeeze", title = "三保压力",
        background = "工资发放的日期快到了",
        description = "财政的盘子绷得很紧，这个月的基本支出还差一个口子。",
        tags = tags(EventTag.PROJECT, EventTag.INSPECTION, EventTag.LEADER_TASK),
        weight = 12, repeatable = true, cooldown = 12,
        choices = listOf(
            c("压减一般性支出，先保工资", Attitude.BOLD, "你砍掉了一批会议、培训和接待经费。运转保住了，怨气也有。",
                AttrDelta(coordination = 2, colleagues = -2),
                governance = jg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to -2), merit = mapOf(MeritKind.FINANCE to 4), finance = 12)),
            c("向金融机构短期周转", Attitude.STEADY, "你通过合规渠道周转了一笔资金，按时发了工资。",
                AttrDelta(politics = 2, work = 2), governance = jg(finance = 8, risk = 2)),
            c("争取上级转移支付", Attitude.CAUTIOUS, "你打了报告，等上级的钱下来。发薪日那天，大家心里都悬着。",
                AttrDelta(writing = 2, leaderTrust = 1), governance = jg(finance = 6, ind = mapOf(Indicator.OFFICIAL_SATISFACTION to -3)))
        )
    ),
    ev(
        id = "j12_finance_audit", title = "审计抽查",
        background = "审计组抽查了一笔专项资金",
        description = "审计发现一笔专项资金的拨付手续不够规范，虽然钱用在了正事上。",
        tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY, EventTag.MATERIAL),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c("主动说明，补全手续", Attitude.BOLD, "你把来龙去脉讲清楚，补齐了材料。审计组认可了整改。",
                AttrDelta(integrityRisk = -4, writing = 2, leaderTrust = 2)),
            c("解释清楚，争取从轻", Attitude.STEADY, "你说明是程序瑕疵，请审计组给了整改期。",
                AttrDelta(politics = 2, integrityRisk = -1)),
            c("把责任往下推", Attitude.RISKY, "你指出是具体经办人的问题。事情小了，但单位里人心凉了。",
                AttrDelta(colleagues = -4, integrityRisk = 3, reputation = -2))
        )
    ),
    ev(
        id = "j12_agri_harvest", title = "秋收遇上连阴雨",
        background = "天气预报说雨要下一周",
        description = "地里的庄稼熟了，连着几天阴雨，收割机和烘干设备都不够用。",
        tags = tags(EventTag.RURAL, EventTag.DISASTER, EventTag.MASS_APPEAL),
        weight = 13, repeatable = true, cooldown = 12,
        choices = listOf(
            c("连夜协调收割机和烘干点", Attitude.BOLD, "你调来了周边县市的机器，抢在雨前收了一大半。农民记着这份情。",
                AttrDelta(work = 3, masses = 3, coordination = 2, energy = -5),
                governance = jg(ind = mapOf(Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 4))),
            c("组织干部下乡帮抢收", Attitude.STEADY, "机关干部下了田。声势不小，实际抢收的量有限。",
                AttrDelta(masses = 2, coordination = 2, colleagues = -1),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 2))),
            c("发动群众自救，政府做好保障", Attitude.CAUTIOUS, "你把重点放在服务和保障上。有农户损失较大，意见不小。",
                AttrDelta(ability = 2, masses = -1),
                governance = jg(ind = mapOf(Indicator.LIVELIHOOD to -1)))
        )
    ),
    ev(
        id = "j12_agri_subsidy", title = "惠农补贴的名单",
        background = "补贴名单公示了",
        description = "有群众反映，公示名单里有几户并不符合条件，是村干部的亲戚。",
        tags = tags(EventTag.RURAL, EventTag.INTEGRITY, EventTag.COMPLAINT),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c("重新核查，不合规的剔除", Attitude.BOLD, "你要求重新核查，剔除了几户。群众叫好，村里有人记恨。",
                AttrDelta(integrityRisk = -3, masses = 3, colleagues = -2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 3), merit = mapOf(MeritKind.FINANCE to 3))),
            c("责令村委自查自纠", Attitude.STEADY, "村委自己改了一遍。问题解决了一部分，群众还是有议论。",
                AttrDelta(coordination = 2, masses = 1),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 1))),
            c("解释为「工作疏忽」，下不为例", Attitude.CAUTIOUS, "你替村里兜了一句。事情过去了，但群众的信任打了折。",
                AttrDelta(politics = 2, masses = -2, integrityRisk = 3),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -2)))
        )
    ),
    ev(
        id = "j12_housing_demolition", title = "拆迁谈判",
        background = "最后一户还没签",
        description = "改造片区的最后一户老人坚称补偿不合理，工程已经停在这里两周了。",
        tags = tags(EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.COMPLAINT),
        weight = 13, repeatable = true, cooldown = 16,
        choices = listOf(
            c("亲自上门，坐到谈拢为止", Attitude.BOLD, "你去了三趟，最后老人提的合理诉求被解决，签了字。",
                AttrDelta(masses = 3, work = 3, energy = -4, coordination = 2),
                governance = jg(ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 3))),
            c("按政策给足，超出的一分不给", Attitude.STEADY, "你坚持了政策口径。老人最后还是签了，过程不算愉快。",
                AttrDelta(politics = 3, integrityRisk = -2)),
            c("适当加一点，先把工程推进", Attitude.RISKY, "户主签了，工程复工。但口子一开，后面几户也想再谈谈。",
                AttrDelta(work = 2, integrityRisk = 4),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -1), finance = -10, risk = 4))
        )
    ),
    ev(
        id = "j12_housing_quality", title = "安置房的质量",
        background = "分房前有人来反映",
        description = "安置房还没交付，就有回迁户反映墙体开裂、防水不过关。",
        tags = tags(EventTag.PROJECT, EventTag.COMPLAINT, EventTag.INSPECTION),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c("暂停交付，全面复检", Attitude.BOLD, "你叫停了交付，组织全面复检。工期耽误了，但没有人住进问题房。",
                AttrDelta(work = 3, integrityRisk = -3, masses = 3),
                governance = jg(ind = mapOf(Indicator.INFRASTRUCTURE to -1, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.FINANCE to 3), finance = -6)),
            c("边整改边交付，不误工期", Attitude.STEADY, "你安排了整改和交付同步进行，问题基本解决。",
                AttrDelta(coordination = 3, work = 2),
                governance = jg(ind = mapOf(Indicator.LIVELIHOOD to 1))),
            c("先交付，有问题再修", Attitude.RISKY, "回迁户住了进去，问题陆续暴露，投诉一件接一件。",
                AttrDelta(masses = -3, integrityRisk = 4),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -4), risk = 4))
        )
    ),
    ev(
        id = "j12_edu_teacher", title = "乡村教师流失",
        background = "一个学期走了三位年轻老师",
        description = "条件艰苦的几所村小，年轻教师来了又走，课都快排不开了。",
        tags = tags(EventTag.RESEARCH, EventTag.COMPLAINT, EventTag.FAMILY),
        weight = 12, repeatable = true, cooldown = 18,
        choices = listOf(
            c("改善周转房和待遇，把人留住", Attitude.BOLD, "你挤出资金改善了教师周转房，还提高了乡村补贴。这学期没人再走。",
                AttrDelta(work = 2, masses = 2),
                governance = jg(ind = mapOf(Indicator.EDUCATION to 5), merit = mapOf(MeritKind.LIVELIHOOD to 4), finance = -10)),
            c("从城区学校派教师轮岗支教", Attitude.STEADY, "城区学校派了老师轮岗，课开齐了，但稳定性一般。",
                AttrDelta(coordination = 3),
                governance = jg(ind = mapOf(Indicator.EDUCATION to 3))),
            c("向上级申请编制和经费", Attitude.CAUTIOUS, "你打了报告。上级答复「研究研究」，这学期的课还是勉强排。",
                AttrDelta(writing = 2, leaderTrust = 1),
                governance = jg(ind = mapOf(Indicator.EDUCATION to -1)))
        )
    ),
    ev(
        id = "j12_edu_safety", title = "校门口的安全",
        background = "家长反映上下学太挤",
        description = "一所小学紧邻主干道，上下学时人车混行，家长意见很大。",
        tags = tags(EventTag.EMERGENCY, EventTag.COMPLAINT, EventTag.ACCIDENT),
        weight = 12, repeatable = true, cooldown = 14,
        choices = listOf(
            c("加装护栏、错峰放学、安排护学岗", Attitude.BOLD, "几项措施一起上，校门口的秩序明显好转。家长在群里点了赞。",
                AttrDelta(coordination = 3, masses = 2),
                governance = jg(ind = mapOf(Indicator.SECURITY to 4, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.SECURITY to 3), finance = -5)),
            c("协调交警在高峰时段值守", Attitude.STEADY, "交警来了，拥堵缓解了一些，但长期还要靠设施。",
                AttrDelta(coordination = 2),
                governance = jg(ind = mapOf(Indicator.SECURITY to 2))),
            c("让学校自己加强管理", Attitude.CAUTIOUS, "学校安排了值班老师。家长的担心并没有完全消除。",
                AttrDelta(politics = 2, masses = -1),
                governance = jg(ind = mapOf(Indicator.SECURITY to 1, Indicator.APPROVAL to -2)))
        )
    ),
    ev(
        id = "j12_health_queue", title = "排队的队伍",
        background = "卫生院里排起了长队",
        description = "流感高发，基层卫生院人满为患，有群众抱怨「看个病要排一上午」。",
        tags = tags(EventTag.HEALTH, EventTag.COMPLAINT, EventTag.EMERGENCY),
        weight = 12, repeatable = true, cooldown = 12,
        choices = listOf(
            c("开设分诊和延时门诊", Attitude.BOLD, "你协调卫生院开了分诊台和延时门诊，队伍短了一半。",
                AttrDelta(coordination = 3, masses = 2),
                governance = jg(ind = mapOf(Indicator.MEDICAL to 4, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.LIVELIHOOD to 3))),
            c("请上级医院派专家支援", Attitude.STEADY, "上级医院派人来支援了一周，压力缓解了。",
                AttrDelta(coordination = 2, leaderTrust = 1),
                governance = jg(ind = mapOf(Indicator.MEDICAL to 3))),
            c("加强宣传，引导轻症居家", Attitude.CAUTIOUS, "宣传起了一定作用，但重病人还是挤。",
                AttrDelta(writing = 2),
                governance = jg(ind = mapOf(Indicator.MEDICAL to 1, Indicator.APPROVAL to -1)))
        )
    ),
    ev(
        id = "j12_health_outbreak", title = "一起聚集性病例",
        background = "某村出现多例相似症状",
        description = "村里陆续有人发烧腹泻，初步怀疑与水源有关。",
        tags = tags(EventTag.HEALTH, EventTag.EMERGENCY, EventTag.DISASTER),
        weight = 13, repeatable = true, cooldown = 16,
        choices = listOf(
            c("立即封控水源，组织送医", Attitude.BOLD, "你连夜组织送医和送水，疫情没有扩散。上级肯定了处置速度。",
                AttrDelta(work = 3, coordination = 3, leaderTrust = 3, energy = -5),
                governance = jg(ind = mapOf(Indicator.MEDICAL to 3, Indicator.SECURITY to 2, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.SECURITY to 4), finance = -8)),
            c("边处置边向上报告", Attitude.STEADY, "你按程序报告并同步处置，信息透明，群众情绪稳定。",
                AttrDelta(writing = 2, leaderTrust = 2, coordination = 2),
                governance = jg(ind = mapOf(Indicator.MEDICAL to 2))),
            c("先内部处理，等情况明确再报", Attitude.CAUTIOUS, "你压了两天再报。疫情控制住了，但「瞒报」的风声让你很被动。",
                AttrDelta(integrityRisk = 4, leaderTrust = -2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -3), risk = 4))
        )
    ),
    ev(
        id = "j12_emergency_flood", title = "夜里的一级响应",
        background = "河水超了警戒线",
        description = "凌晨两点，河水超过了警戒水位，下游还有两个村没有转移完。",
        tags = tags(EventTag.FLOOD, EventTag.EMERGENCY, EventTag.DISASTER),
        weight = 14, repeatable = true, cooldown = 12,
        choices = listOf(
            c("亲自带队去最危险的地方", Attitude.BOLD, "你在堤上守到天亮，最后一个村也转移完了。有人受伤，没有人死亡。",
                AttrDelta(work = 4, coordination = 3, health = -3, energy = -6, reputation = 4),
                governance = jg(ind = mapOf(Indicator.SECURITY to 5, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.SECURITY to 5))),
            c("坐镇指挥中心，统一调度", Attitude.STEADY, "你在指挥中心调度各方力量，转移有序完成。",
                AttrDelta(coordination = 4, ability = 2),
                governance = jg(ind = mapOf(Indicator.SECURITY to 4), merit = mapOf(MeritKind.SECURITY to 4))),
            c("按预案执行，各负其责", Attitude.CAUTIOUS, "一切按预案走。转移完成了，但个别环节慢了半拍。",
                AttrDelta(politics = 3),
                governance = jg(ind = mapOf(Indicator.SECURITY to 2)))
        )
    ),
    ev(
        id = "j12_emergency_drill", title = "一场没有预告的演练",
        background = "上级突击拉练",
        description = "没有通知、没有脚本，上级直接拉响了应急演练的警报。",
        tags = tags(EventTag.EMERGENCY, EventTag.INSPECTION, EventTag.ACCIDENT),
        weight = 12, repeatable = true, cooldown = 18,
        choices = listOf(
            c("按实战标准组织响应", Attitude.BOLD, "你按真实情况调度，队伍在二十分钟内到位。上级评价「过硬」。",
                AttrDelta(coordination = 3, work = 2, leaderTrust = 3),
                governance = jg(merit = mapOf(MeritKind.SECURITY to 4))),
            c("先按流程报告，再组织力量", Attitude.STEADY, "流程走得很规范，响应速度中规中矩。",
                AttrDelta(politics = 2, coordination = 1)),
            c("临时通知各单位到场", Attitude.CAUTIOUS, "各单位陆续到了场。演练过去了，暴露的问题也记下了。",
                AttrDelta(politics = 2, ability = 1),
                governance = jg(merit = mapOf(MeritKind.SECURITY to 1)))
        )
    ),
    ev(
        id = "j12_grassroots_dispute", title = "邻里纠纷",
        background = "两户人家为一道墙吵了三年",
        description = "两家为宅基地的界墙闹了三年，村里调解多次都没用。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.PETITION, EventTag.COMPLAINT),
        weight = 13, repeatable = true, cooldown = 12,
        choices = listOf(
            c("现场丈量，一次把界划清", Attitude.BOLD, "你请来自然资源部门现场丈量，把界线定死了。两家虽然都不太情愿，但从此没再闹。",
                AttrDelta(work = 3, masses = 3, coordination = 2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 3))),
            c("请村里有威望的老人一起调", Attitude.STEADY, "老同志出面说和，两家各让一步，签了调解协议。",
                AttrDelta(masses = 2, coordination = 2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 2))),
            c("引导走法律途径", Attitude.CAUTIOUS, "你建议两家打官司。事情合法合规地拖下去了。",
                AttrDelta(politics = 2, masses = -1),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -1)))
        )
    ),
    ev(
        id = "j12_grassroots_petition", title = "老信访户",
        background = "一个跑了十几年的信访件",
        description = "一位老人为多年前的一件事反复上访，情绪激动，谁劝都不听。",
        tags = tags(EventTag.PETITION, EventTag.COMPLAINT, EventTag.MASS_APPEAL),
        weight = 12, repeatable = true, cooldown = 14,
        choices = listOf(
            c("耐心听完，把能解决的先解决", Attitude.STEADY, "你听了两个小时，先解决了他最急的一件事。老人情绪稳了下来。",
                AttrDelta(masses = 3, energy = -3, coordination = 2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 3))),
            c("组织专班重新核查诉求", Attitude.BOLD, "你成立了专班重新核查，把事情查了个明白。",
                AttrDelta(work = 3, coordination = 2, integrityRisk = -2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 2), merit = mapOf(MeritKind.FINANCE to 2))),
            c("按现有结论答复，做好稳控", Attitude.CAUTIOUS, "你按现有结论答复了。老人没有走，事情还在。",
                AttrDelta(politics = 3, masses = -2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -2), risk = 2))
        )
    ),
    ev(
        id = "j12_admin_errand", title = "谁都来交办的事",
        background = "一天之内来了五件交办",
        description = "五位领导分别交办了五件事，都说是「急事」。你只有一双手。",
        tags = tags(EventTag.COORDINATION, EventTag.LEADER_TASK, EventTag.MEETING),
        weight = 12, repeatable = true, cooldown = 12,
        choices = listOf(
            c("按轻重缓急排，先做最要紧的", Attitude.STEADY, "你把五件事排了序，最要紧的当天办结，其余给了期限。",
                AttrDelta(coordination = 3, ability = 2, work = 1)),
            c("分头布置，同时推进", Attitude.BOLD, "你把几件事同时铺开推进。事情都动了，质量参差不齐。",
                AttrDelta(coordination = 2, colleagues = -1, energy = -3)),
            c("逐一向领导说明，请求排序", Attitude.CAUTIOUS, "你如实说明精力有限。有位领导不太高兴，但事情清爽了。",
                AttrDelta(politics = 3, leaderTrust = -1))
        )
    ),
    ev(
        id = "j12_admin_reception", title = "一场接待",
        background = "上级来检查工作",
        description = "检查组明天到，接待的方案摆在你面前：按标准，还是「周到一点」。",
        tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY, EventTag.GIFT),
        weight = 12, repeatable = true, cooldown = 14,
        choices = listOf(
            c("严格按标准，工作说清楚就行", Attitude.BOLD, "你按规定安排，把汇报材料做得扎实。检查组认可了工作本身。",
                AttrDelta(integrityRisk = -4, writing = 2, leaderTrust = 1)),
            c("标准之内，尽量周到", Attitude.STEADY, "你在标准内把细节做足了。检查组感觉不错。",
                AttrDelta(coordination = 2, leaderTrust = 2)),
            c("超出标准安排，搞好关系", Attitude.RISKY, "检查组很满意。但超标准的开支成了隐患。",
                AttrDelta(leaderTrust = 2, integrityRisk = 5),
                governance = jg(risk = 4))
        )
    ),
    ev(
        id = "j12_general_letter", title = "一封群众来信",
        background = "信直接寄到了你的案头",
        description = "一封手写的信，字迹颤抖，反映的是村里一条路多年没修的问题。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.COMPLAINT, EventTag.RURAL),
        weight = 12, repeatable = true, cooldown = 14,
        choices = listOf(
            c("当天就批，安排人去实地看", Attitude.BOLD, "信当天就转了出去，第二天就有人去看了现场。写信的老人后来专门道了谢。",
                AttrDelta(masses = 3, work = 2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to 3, Indicator.LIVELIHOOD to 2), merit = mapOf(MeritKind.LIVELIHOOD to 3))),
            c("纳入下一年度的计划", Attitude.STEADY, "你把这件事列进了年度计划。事情有了着落，但要等。",
                AttrDelta(writing = 2, politics = 2),
                governance = jg(ind = mapOf(Indicator.LIVELIHOOD to 1))),
            c("转给属地按程序办理", Attitude.CAUTIOUS, "信按程序转了下去。几个月后，老人又写了一封。",
                AttrDelta(politics = 2, masses = -2),
                governance = jg(ind = mapOf(Indicator.APPROVAL to -2)))
        )
    ),
    ev(
        id = "j12_general_summary", title = "年终总结怎么写",
        background = "要交全年工作总结了",
        description = "这一年有成绩也有教训，总结的口径，也是给上级的印象。",
        tags = tags(EventTag.MATERIAL, EventTag.LEADER_TASK, EventTag.RESEARCH),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c("成绩讲够，问题也讲透", Attitude.BOLD, "你如实写了成绩与不足。上级评价「这份总结有分量」。",
                AttrDelta(writing = 3, integrityRisk = -3, leaderTrust = 3, reputation = 2)),
            c("突出亮点，问题点到为止", Attitude.STEADY, "总结写了几个亮点，问题一句带过。看着体面。",
                AttrDelta(writing = 2, leaderTrust = 1, integrityRisk = 1)),
            c("中规中矩，按往年格式来", Attitude.CAUTIOUS, "总结四平八稳。领导翻了几页，放了下来。",
                AttrDelta(writing = 1))
        )
    )
)



