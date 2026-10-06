package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.ConditionalEffect
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
 * 第十二阶段：跨季度事件链。共 20 条，每条 3～4 个阶段。
 *
 * 链式事件的第一阶段是随机事件，选择后续阶段会按季度依次到期；
 * 前期选择留下的标记（flag）会通过 conditionalBonus 改变后期结果，
 * 形成「早期保守 → 后期风险低但慢、早期激进 → 收益高风险大」的长期因果。
 * 全部为虚构设定。
 */
private fun cg(
    ind: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    risk: Int = 0
) = GovernanceEffect(indicators = ind, merit = merit, finance = finance, risk = risk)

internal val chainEventLibrary: List<com.shitu.app.model.GameEvent> = listOf(
    // ===== 链 1：企业落户 =====
    ev(
        id = "c12_firm_start", title = "企业准备投资",
        background = "一家制造企业想在本地落户",
        description = "对方看中了本地的交通和人力，打算先谈一个投资意向。",
        tags = tags(EventTag.PROJECT, EventTag.LEADER_TASK, EventTag.RESEARCH),
        weight = 12,
        choices = listOf(
            c("积极对接，尽快把意向定下来", Attitude.BOLD, "你带队对接，双方签了投资意向。故事才刚刚开始。",
                AttrDelta(work = 2, coordination = 2), chain = "c_industrial", flag = "ind_welcome",
                governance = cg(ind = mapOf(Indicator.ECONOMY to 2), merit = mapOf(MeritKind.ECONOMIC to 2))),
            c("先考察背景，稳妥推进", Attitude.STEADY, "你派人做了背景核查，发现对方资金链一般。",
                AttrDelta(ability = 2, politics = 2), chain = "c_industrial", flag = "ind_cautious"),
            c("先听听群众的意见", Attitude.CAUTIOUS, "你没有急着表态，先让相关部门收集了周边居民的看法。", AttrDelta(politics = 2, masses = 1))
        )
    ),
    ev(
        id = "c12_firm_oppose", title = "群众的顾虑",
        background = "拟选址附近有居民反对",
        description = "居民担心工厂的噪音和排污，已经联名写了意见。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.COMPLAINT, EventTag.PROJECT),
        chain = "c_industrial", step = 1, after = 3, weight = 12,
        choices = listOf(
            c("开听证会，把环保承诺写进协议", Attitude.STEADY, "你把居民代表和企业请到一起，企业当场承诺了环保标准。",
                AttrDelta(coordination = 3, masses = 3, work = 1), chain = "c_industrial", flag = "ind_hearing",
                governance = cg(ind = mapOf(Indicator.APPROVAL to 3, Indicator.ENVIRONMENT to 1))),
            c("坚持项目优先，做好解释工作", Attitude.BOLD, "项目照常推进，解释也做了，但居民的情绪没有完全平复。",
                AttrDelta(work = 3, masses = -2), chain = "c_industrial", flag = "ind_push",
                governance = cg(ind = mapOf(Indicator.ECONOMY to 2, Indicator.APPROVAL to -2), risk = 2)),
            c("缓一缓，另行选址", Attitude.CAUTIOUS, "你决定另找地方。项目慢了下来，居民满意了。",
                AttrDelta(politics = 2, masses = 2), chain = "c_industrial", flag = "ind_relocate",
                governance = cg(ind = mapOf(Indicator.ECONOMY to -1)))
        )
    ),
    ev(
        id = "c12_firm_money", title = "项目资金出问题",
        background = "企业那边资金紧张",
        description = "开工在即，企业却提出希望本地先垫一部分配套资金。",
        tags = tags(EventTag.PROJECT, EventTag.INSPECTION, EventTag.INTEGRITY),
        chain = "c_industrial", step = 2, after = 3, weight = 12,
        choices = listOf(
            c("按政策给配套，程序走全", Attitude.STEADY, "你按规定拨付了配套资金，手续齐全。项目继续推进。",
                AttrDelta(work = 2, integrityRisk = -2), chain = "c_industrial", flag = "ind_clean",
                governance = cg(finance = -12), bonus = listOf(ConditionalEffect("ind_hearing", AttrDelta(work = 1), extraOutcome = "此前的听证承诺让这笔投入更容易被接受。"))),
            c("额外给一笔周转，别让项目停", Attitude.RISKY, "资金很快到位，项目没停。但这笔钱的性质，以后要说得清。",
                AttrDelta(work = 2, integrityRisk = 4), chain = "c_industrial", flag = "ind_extra",
                governance = cg(finance = -18, risk = 4), bonus = listOf(ConditionalEffect("ind_push", AttrDelta(coordination = 1), extraOutcome = "你此前强推项目，如今更不愿意让它停下来。"))),
            c("不垫资，让企业自己想办法", Attitude.CAUTIOUS, "你没有松口。项目进度慢了下来。",
                AttrDelta(politics = 2), chain = "c_industrial", flag = "ind_hold",
                governance = cg(ind = mapOf(Indicator.ECONOMY to -1)))
        )
    ),
    ev(
        id = "c12_firm_end", title = "项目的最终结果",
        background = "一年之后，企业终于要投产了",
        description = "这一年里，这个项目牵动了太多东西。结果就要见分晓。",
        tags = tags(EventTag.PROJECT, EventTag.ACCEPTANCE, EventTag.LEADER_TASK),
        chain = "c_industrial", step = 3, after = 4, weight = 12,
        choices = listOf(
            c("把投产做成现场会，兑现当初的承诺", Attitude.BOLD, "项目投产，就业和税收都上来了。当初反对的居民看到环保设施，也放了心。",
                AttrDelta(reputation = 3, leaderTrust = 3, masses = 3),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 4, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.ECONOMIC to 5), finance = 12),
                bonus = listOf(ConditionalEffect("ind_hearing", AttrDelta(masses = 2), extraOutcome = "因为当初的听证，群众这次给了你信任票。"))),
            c("低调投产，把精力放在下一步", Attitude.STEADY, "项目顺利投产。你没有大张旗鼓，而是开始谋划产业链。",
                AttrDelta(ability = 3, work = 2),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 3), merit = mapOf(MeritKind.ECONOMIC to 3))),
            c("先总结经验，再谈推广", Attitude.CAUTIOUS, "项目收尾后你先做了复盘，经验留了下来。", AttrDelta(writing = 2, ability = 1))
        )
    ),

    // ===== 链 2：城中村改造 =====
    ev(
        id = "c12_village_start", title = "改造正式启动",
        background = "片区改造方案批下来了",
        description = "方案批下来的那天，村里就炸开了锅，有人欢喜有人愁。",
        tags = tags(EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.RESEARCH),
        weight = 12,
        choices = listOf(
            c("先摸底，把每一户的情况搞清楚", Attitude.STEADY, "你安排工作组逐户摸底，掌握了第一手情况。",
                AttrDelta(work = 2, ability = 2), chain = "c_village", flag = "vil_survey"),
            c("按方案推进，不搞特殊", Attitude.BOLD, "你要求严格按方案执行。效率高，矛盾也来得快。",
                AttrDelta(work = 3, coordination = 1), chain = "c_village", flag = "vil_strict"),
            c("先开一次村民代表大会", Attitude.STEADY, "你把方案摆到会上讨论了一轮，意见收集得更全。", AttrDelta(coordination = 2, masses = 1))
        )
    ),
    ev(
        id = "c12_village_comp", title = "补偿标准的争议",
        background = "有几户坚决不签",
        description = "争议集中在两件事上：面积怎么算，过渡期住哪里。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.COMPLAINT, EventTag.PROJECT),
        chain = "c_village", step = 1, after = 3, weight = 12,
        choices = listOf(
            c("先把过渡安置解决好", Attitude.STEADY, "你协调了一批过渡房，最现实的顾虑解决了，签约率一下子上来了。",
                AttrDelta(coordination = 3, masses = 3), chain = "c_village", flag = "vil_housed",
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 3), finance = -10)),
            c("坚持标准，绝不开口子", Attitude.BOLD, "你顶住了压力。标准守住了，但几户成了「钉子」。",
                AttrDelta(politics = 3, integrityRisk = -3, masses = -2), chain = "c_village", flag = "vil_firm",
                governance = cg(ind = mapOf(Indicator.APPROVAL to -2), merit = mapOf(MeritKind.FINANCE to 3))),
            c("个别情况个别处理", Attitude.RISKY, "你给几户特殊照顾，签了字。但其他户的风声很快传开了。",
                AttrDelta(work = 2, integrityRisk = 4), chain = "c_village", flag = "vil_special",
                governance = cg(finance = -12, risk = 4))
        )
    ),
    ev(
        id = "c12_village_end", title = "改造的收尾",
        background = "回迁的日子定了",
        description = "新楼盖好了，回迁户马上要拿到钥匙，这段时间的账，也该算一算。",
        tags = tags(EventTag.ACCEPTANCE, EventTag.PROJECT, EventTag.MASS_APPEAL),
        chain = "c_village", step = 2, after = 4, weight = 12,
        choices = listOf(
            c("组织公开摇号分房，全程监督", Attitude.BOLD, "分房全程公开，有群众代表监督。没有人说不公。",
                AttrDelta(integrityRisk = -4, masses = 3, reputation = 2),
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 4))),
            c("按签约顺序安置，简单直接", Attitude.STEADY, "按顺序安置，少了争议，也有个别户觉得不够公平。",
                AttrDelta(coordination = 2, work = 2),
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 3))),
            c("安置之后再回访一遍", Attitude.STEADY, "安置完成后你安排了回访，遗留问题逐一登记。", AttrDelta(masses = 2, work = 1))
        )
    ),

    // ===== 链 3：水源地保护 =====
    ev(
        id = "c12_water_start", title = "水源地边的工厂",
        background = "上游有家老厂排污",
        description = "群众反映上游一家老厂的排水有问题，而这里正是本地的饮用水源地。",
        tags = tags(EventTag.RESEARCH, EventTag.DISASTER, EventTag.INSPECTION),
        weight = 12,
        choices = listOf(
            c("立即取样检测，先掌握证据", Attitude.STEADY, "你安排环保部门连夜取样，结果确实超标。",
                AttrDelta(work = 2, ability = 2), chain = "c_water", flag = "wat_evidence"),
            c("先上门做工作，让企业自行整改", Attitude.CAUTIOUS, "企业口头答应整改，但迟迟没有动作。",
                AttrDelta(politics = 2), chain = "c_water", flag = "wat_talk"),
            c("先请第三方机构来检测", Attitude.CAUTIOUS, "你请了第三方机构，数据更有说服力。", AttrDelta(ability = 2, integrityRisk = -1))
        )
    ),
    ev(
        id = "c12_water_firm", title = "说情的人来了",
        background = "有人替企业传话",
        description = "企业是当地的纳税大户，为它说话的人不少，语气里带着点提醒。",
        tags = tags(EventTag.INTEGRITY, EventTag.INSPECTION, EventTag.RESEARCH),
        chain = "c_water", step = 1, after = 2, weight = 12,
        choices = listOf(
            c("依法处理，谁来说情都没用", Attitude.BOLD, "你顶住压力依法处罚并限期整改。水源安全了，也得罪了人。",
                AttrDelta(integrityRisk = -4, reputation = 3, colleagues = -2), chain = "c_water", flag = "wat_law",
                politics = PoliticalImpact(capital = 3, factionInfluence = mapOf(Faction.REFORM to 3))),
            c("给整改期，同时向上报告", Attitude.STEADY, "你给了企业整改期，并把情况如实报告。程序上站得住。",
                AttrDelta(politics = 3, integrityRisk = -2, writing = 2), chain = "c_water", flag = "wat_deadline"),
            c("先保企业，缓一缓", Attitude.RISKY, "你替企业缓了一缓。钱是留下了，风险也留下了。",
                AttrDelta(integrityRisk = 5, politics = 2), chain = "c_water", flag = "wat_shelter",
                governance = cg(ind = mapOf(Indicator.ECONOMY to 2, Indicator.ENVIRONMENT to -2), risk = 5))
        )
    ),
    ev(
        id = "c12_water_end", title = "水源地的后来",
        background = "一年之后",
        description = "水源地的水质稳定了下来，但这件事留下的东西比数据更多。",
        tags = tags(EventTag.RESEARCH, EventTag.ACCEPTANCE, EventTag.RESEARCH),
        chain = "c_water", step = 2, after = 4, weight = 12,
        choices = listOf(
            c("建立水源地长期保护机制", Attitude.BOLD, "你把水源保护写进了长效机制，还争取了一笔治理资金。",
                AttrDelta(ability = 3, reputation = 3, work = 2),
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to 5, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.INNOVATION to 4), finance = 8),
                bonus = listOf(ConditionalEffect("wat_law", AttrDelta(leaderTrust = 2), extraOutcome = "当初依法办事的态度，让这次机制建设得到了上级支持。"))),
            c("重点是恢复生产秩序", Attitude.STEADY, "企业恢复生产，环保设施也上了。事情平稳收场。",
                AttrDelta(work = 2, coordination = 2),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 2, Indicator.ENVIRONMENT to 2))),
            c("把整改情况向社会公开", Attitude.STEADY, "你把整改结果公开，群众看到了变化。", AttrDelta(masses = 2, integrityRisk = -1))
        )
    ),

    // ===== 链 4：人才引进 =====
    ev(
        id = "c12_talent_start", title = "人才引进计划",
        background = "本地想引进一批专业人才",
        description = "方案里写着给房、给补贴、给编制，但盘子里没有这么多钱。",
        tags = tags(EventTag.RESEARCH, EventTag.PROJECT, EventTag.CAREER),
        weight = 11,
        choices = listOf(
            c("聚焦急需的少数人，给足条件", Attitude.STEADY, "你决定集中资源，引进最缺的那几个人。",
                AttrDelta(ability = 2, coordination = 2), chain = "c_talent", flag = "tal_focus"),
            c("广撒网，先把声势做起来", Attitude.BOLD, "招聘公告铺得很广，报名的人不少。",
                AttrDelta(work = 2, politics = 2), chain = "c_talent", flag = "tal_broad"),
            c("先摸清到底缺什么样的人", Attitude.CAUTIOUS, "你先做了一轮需求摸底，避免盲目引才。", AttrDelta(ability = 2, work = 1))
        )
    ),
    ev(
        id = "c12_talent_dispute", title = "待遇的争议",
        background = "本地干部有意见了",
        description = "「引进的给套房，我们干了一辈子还没有」——这话在单位里流传开了。",
        tags = tags(EventTag.COLLEAGUE, EventTag.CAREER, EventTag.MEETING),
        chain = "c_talent", step = 1, after = 3, weight = 11,
        choices = listOf(
            c("同步提高本地骨干待遇", Attitude.BOLD, "你调整了本地骨干的激励政策。花销大了，但队伍的情绪稳住了。",
                AttrDelta(colleagues = 4, coordination = 2), chain = "c_talent", flag = "tal_balance",
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 4), finance = -12)),
            c("把政策解释清楚，讲明道理", Attitude.STEADY, "你开了会，把「引才」和「留才」的关系讲了一遍。多数人理解了。",
                AttrDelta(writing = 2, colleagues = 2), chain = "c_talent", flag = "tal_explain",
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 1))),
            c("先推下去，情绪以后再消化", Attitude.RISKY, "政策照常执行。人才来了，队伍里的怨气也积着。",
                AttrDelta(politics = 2, colleagues = -3), chain = "c_talent", flag = "tal_push",
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to -3)))
        )
    ),
    ev(
        id = "c12_talent_end", title = "人才留下了吗",
        background = "两年之后",
        description = "引进的人才有留下的，也有走的。这一轮的账，慢慢能看清了。",
        tags = tags(EventTag.RESEARCH, EventTag.CAREER, EventTag.ACCEPTANCE),
        chain = "c_talent", step = 2, after = 4, weight = 11,
        choices = listOf(
            c("把人才用在关键岗位上", Attitude.BOLD, "你把留下的人才放到了一线关键岗位，很快做出了成果。",
                AttrDelta(ability = 3, reputation = 2),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 2, Indicator.EDUCATION to 2), merit = mapOf(MeritKind.INNOVATION to 4))),
            c("先稳住队伍，不再折腾", Attitude.STEADY, "你不再扩大规模，把现有的人用好。事情平稳。",
                AttrDelta(coordination = 2, colleagues = 1),
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 2))),
            c("建立人才跟踪服务机制", Attitude.STEADY, "你安排专人跟踪服务，人才的稳定性提高了。", AttrDelta(coordination = 2, ability = 1))
        )
    ),

    // ===== 链 5：学校合并 =====
    ev(
        id = "c12_school_start", title = "小规模学校要不要并",
        background = "生源少了，几所村小难以为继",
        description = "并了能集中资源，不并孩子要跑远路。两难摆在面前。",
        tags = tags(EventTag.RESEARCH, EventTag.FAMILY, EventTag.RURAL),
        weight = 11,
        choices = listOf(
            c("先听家长和老师的意见", Attitude.STEADY, "你开了几场院坝会，听到了很多具体的困难。",
                AttrDelta(masses = 2, ability = 2), chain = "c_school", flag = "sch_listen"),
            c("按教育规律办，该并就并", Attitude.BOLD, "你从教育质量出发，决定推进合并。",
                AttrDelta(work = 2, ability = 2), chain = "c_school", flag = "sch_merge"),
            c("先做一轮生源和路况调查", Attitude.CAUTIOUS, "你把生源、路况、家长意愿都摸了一遍。", AttrDelta(ability = 2, writing = 1))
        )
    ),
    ev(
        id = "c12_school_protest", title = "家长的担心",
        background = "合并的消息传开了",
        description = "家长最担心的是低龄孩子上学远、路不安全。",
        tags = tags(EventTag.FAMILY, EventTag.COMPLAINT, EventTag.EMERGENCY),
        chain = "c_school", step = 1, after = 3, weight = 11,
        choices = listOf(
            c("先配校车和安全护送，再谈合并", Attitude.BOLD, "你把校车和护送安排先落实了，家长的态度软了下来。",
                AttrDelta(coordination = 3, masses = 3), chain = "c_school", flag = "sch_bus",
                governance = cg(ind = mapOf(Indicator.EDUCATION to 3, Indicator.APPROVAL to 3, Indicator.SECURITY to 2), finance = -10)),
            c("保留低年级教学点", Attitude.STEADY, "你决定低年级就近保留。资源分散了，但家长的顾虑小了。",
                AttrDelta(coordination = 2, masses = 2), chain = "c_school", flag = "sch_keep",
                governance = cg(ind = mapOf(Indicator.EDUCATION to 1, Indicator.APPROVAL to 2))),
            c("按计划合并，边走边解决", Attitude.RISKY, "合并照常推进，问题一边出现一边解决。家长的抱怨持续了很久。",
                AttrDelta(work = 2, masses = -3), chain = "c_school", flag = "sch_force",
                governance = cg(ind = mapOf(Indicator.EDUCATION to 3, Indicator.APPROVAL to -3)))
        )
    ),
    ev(
        id = "c12_school_end", title = "孩子们的去处",
        background = "一学年过去了",
        description = "合并之后的教学质量和孩子的上学路，都有了答案。",
        tags = tags(EventTag.RESEARCH, EventTag.FAMILY, EventTag.ACCEPTANCE),
        chain = "c_school", step = 2, after = 4, weight = 11,
        choices = listOf(
            c("把集中后的资源用在提升质量上", Attitude.BOLD, "你给合并后的学校配强了师资和设施，教学质量明显提升。",
                AttrDelta(ability = 2, reputation = 2),
                governance = cg(ind = mapOf(Indicator.EDUCATION to 5), merit = mapOf(MeritKind.LIVELIHOOD to 4))),
            c("重点保安全、保运转", Attitude.STEADY, "你把重心放在校车安全和日常运转上。家长放心了。",
                AttrDelta(coordination = 2, masses = 2),
                governance = cg(ind = mapOf(Indicator.EDUCATION to 2, Indicator.SECURITY to 2))),
            c("持续跟踪孩子的适应情况", Attitude.STEADY, "你要求学校定期反馈，问题发现得早。", AttrDelta(coordination = 1, masses = 2))
        )
    )
,
    // ===== 链 6：食品安全 =====
    ev(
        id = "c12_food_start", title = "一批抽检不合格",
        background = "市场监管抽检发现本地产品不合格",
        description = "抽检报告摆在桌上：本地一家食品厂的批次不合格，产品已经流出去了。",
        tags = tags(EventTag.HEALTH, EventTag.INSPECTION, EventTag.PUBLIC_OPINION),
        weight = 12,
        choices = listOf(
            c("立即召回，公开通报", Attitude.BOLD, "你要求连夜召回并公开信息。短期难看，但把风险挡在了前面。",
                AttrDelta(work = 2, integrityRisk = -3, leaderTrust = 1), chain = "c_food", flag = "food_recall"),
            c("先查清流向，避免恐慌", Attitude.STEADY, "你先摸清了产品的流向，再决定怎么处置。",
                AttrDelta(ability = 2, politics = 2), chain = "c_food", flag = "food_trace"),
            c("先控制流通渠道，防止扩大", Attitude.CAUTIOUS, "你先通知主要渠道暂缓销售，风险缩小了。", AttrDelta(coordination = 2, work = 1))
        )
    ),
    ev(
        id = "c12_food_firm", title = "企业求情",
        background = "厂长带着一摞材料来了",
        description = "厂长说厂里养着几百号人，一旦停产，工人生计怎么办。",
        tags = tags(EventTag.INTEGRITY, EventTag.HEALTH, EventTag.MASS_APPEAL),
        chain = "c_food", step = 1, after = 2, weight = 12,
        choices = listOf(
            c("依法停业整改，同时帮工人找出路", Attitude.BOLD, "你依法处理，也协调了工人的临时安置。企业有怨气，群众有安全感。",
                AttrDelta(integrityRisk = -4, masses = 3, work = 2), chain = "c_food", flag = "food_stop",
                governance = cg(ind = mapOf(Indicator.SECURITY to 3, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.SECURITY to 3))),
            c("限期整改，边改边生产", Attitude.STEADY, "企业边整改边生产，风险控制住了，但没有完全停下。",
                AttrDelta(coordination = 2, integrityRisk = -1), chain = "c_food", flag = "food_deadline"),
            c("罚一点，别把企业整垮", Attitude.RISKY, "企业交了罚款继续生产。隐患还在，群众也不买账。",
                AttrDelta(integrityRisk = 4, masses = -2), chain = "c_food", flag = "food_soft",
                governance = cg(risk = 4, ind = mapOf(Indicator.APPROVAL to -2)))
        )
    ),
    ev(
        id = "c12_food_end", title = "餐桌上的信任",
        background = "半年之后",
        description = "本地食品的口碑，在这半年里经历了一次考验。",
        tags = tags(EventTag.HEALTH, EventTag.ACCEPTANCE, EventTag.INSPECTION),
        chain = "c_food", step = 2, after = 3, weight = 12,
        choices = listOf(
            c("建立从田间到餐桌的追溯体系", Attitude.BOLD, "你推动了全链条追溯，本地食品重新赢回了口碑。",
                AttrDelta(ability = 2, reputation = 3),
                governance = cg(ind = mapOf(Indicator.SECURITY to 4, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.INNOVATION to 3))),
            c("加强日常抽检，常态化管理", Attitude.STEADY, "你把抽检常态化，风险基本可控。",
                AttrDelta(work = 2, coordination = 1),
                governance = cg(ind = mapOf(Indicator.SECURITY to 3), merit = mapOf(MeritKind.SECURITY to 2))),
            c("公开抽检结果，重建信任", Attitude.STEADY, "你把抽检结果定期公开，消费者慢慢放心了。", AttrDelta(masses = 2, integrityRisk = -2))
        )
    ),

    // ===== 链 7：企业欠薪 =====
    ev(
        id = "c12_wage_start", title = "企业欠薪",
        background = "工人到政府门口讨说法",
        description = "一家企业拖欠了三个月工资，几十名工人堵在门口，情绪激动。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.COMPLAINT, EventTag.PETITION),
        weight = 13,
        choices = listOf(
            c("先接访安抚，承诺限期解决", Attitude.STEADY, "你把工人代表请进会议室，认真记下了诉求。",
                AttrDelta(masses = 2, coordination = 2), chain = "c_wage", flag = "wage_talk"),
            c("立即约谈企业负责人", Attitude.BOLD, "你当天就把企业负责人约来，要求限期解决。",
                AttrDelta(work = 2, coordination = 2), chain = "c_wage", flag = "wage_press"),
            c("先登记欠薪金额和人数", Attitude.CAUTIOUS, "你把欠薪的底数摸清了，处置有了依据。", AttrDelta(ability = 2, work = 1))
        )
    ),
    ev(
        id = "c12_wage_firm", title = "企业拿不出钱",
        background = "企业确实遇到了困难",
        description = "企业账上确实没钱，上游回款也断了。工人的工资还是没有着落。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.PROJECT, EventTag.INSPECTION),
        chain = "c_wage", step = 1, after = 2, weight = 13,
        choices = listOf(
            c("先垫付基本工资，再向企业追偿", Attitude.BOLD, "你动用了一笔应急资金先垫付了基本工资，工人拿到了钱。",
                AttrDelta(masses = 4, work = 2, integrityRisk = 1), chain = "c_wage", flag = "wage_paid",
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 4), finance = -12, merit = mapOf(MeritKind.LIVELIHOOD to 4))),
            c("协调银行给企业续贷", Attitude.STEADY, "续贷到位，企业补发了工资。风险还是留在了银行和企业。",
                AttrDelta(coordination = 3, work = 2), chain = "c_wage", flag = "wage_loan",
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 2), finance = -4)),
            c("引导工人走法律途径", Attitude.CAUTIOUS, "你建议工人申请仲裁。程序合法，但工人的钱要等更久。",
                AttrDelta(politics = 3, masses = -2), chain = "c_wage", flag = "wage_law",
                governance = cg(ind = mapOf(Indicator.APPROVAL to -3)))
        )
    ),
    ev(
        id = "c12_wage_end", title = "工人的钱袋子",
        background = "事情有了结果",
        description = "工人最终拿到了多少，这件事在本地传开之后，人心自有评判。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.ACCEPTANCE, EventTag.PROJECT),
        chain = "c_wage", step = 2, after = 3, weight = 13,
        choices = listOf(
            c("建立欠薪预警机制", Attitude.BOLD, "你把欠薪预警做成了制度，同类问题明显减少。",
                AttrDelta(ability = 2, reputation = 2),
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.INNOVATION to 3))),
            c("加强日常排查", Attitude.STEADY, "你把排查频率提高了，问题发现得更早。",
                AttrDelta(work = 2, coordination = 1),
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 2))),
            c("定期回访，防止问题反复", Attitude.STEADY, "你安排了定期回访，工人心里更踏实。", AttrDelta(masses = 2, coordination = 1))
        )
    ),

    // ===== 链 8：房地产项目停工 =====
    ev(
        id = "c12_stall_start", title = "楼盘停了工",
        background = "一个在建楼盘停工了",
        description = "开发商资金链断了，工地停了，业主开始聚集。",
        tags = tags(EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.COMPLAINT),
        weight = 13,
        choices = listOf(
            c("立即成立专班，摸清资产和债务", Attitude.BOLD, "专班连夜进场，把项目的情况摸了个大概。",
                AttrDelta(work = 3, coordination = 2), chain = "c_stall", flag = "stall_team"),
            c("先安抚业主，再想办法", Attitude.STEADY, "你先和业主代表见了面，稳定了情绪。",
                AttrDelta(masses = 2, coordination = 2), chain = "c_stall", flag = "stall_calm"),
            c("先摸清预售资金和债权关系", Attitude.CAUTIOUS, "你把资金和债权关系理清楚了，后续处置有了基础。", AttrDelta(ability = 2, writing = 1))
        )
    ),
    ev(
        id = "c12_stall_owner", title = "业主的压力",
        background = "业主开始集体上访",
        description = "几十户业主联名上访，要求政府给个说法，有人情绪很激动。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.PETITION, EventTag.PUBLIC_OPINION),
        chain = "c_stall", step = 1, after = 2, weight = 13,
        choices = listOf(
            c("牵头引入新的投资方续建", Attitude.BOLD, "你出面协调，引入了一家愿意接盘的企业。项目有望复工。",
                AttrDelta(work = 3, coordination = 3, integrityRisk = 1), chain = "c_stall", flag = "c_stall_resume",
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 3), finance = -8, merit = mapOf(MeritKind.LIVELIHOOD to 3)),
                bonus = listOf(ConditionalEffect("stall_team", AttrDelta(coordination = 2), extraOutcome = "前期的摸底让这次协调有的放矢。"))),
            c("走司法程序，依法处置", Attitude.STEADY, "项目进入司法程序。业主的情绪稳定了一些，但时间要等。",
                AttrDelta(politics = 3, integrityRisk = -2), chain = "c_stall", flag = "stall_law",
                governance = cg(ind = mapOf(Indicator.APPROVAL to 1))),
            c("先稳住，等上面定政策", Attitude.CAUTIOUS, "你选择了等待。业主的耐心一天天在消耗。",
                AttrDelta(politics = 2, masses = -3), chain = "c_stall", flag = "stall_wait",
                governance = cg(ind = mapOf(Indicator.APPROVAL to -4), risk = 3))
        )
    ),
    ev(
        id = "c12_stall_end", title = "楼盘交房了吗",
        background = "一年半之后",
        description = "业主终于等到了结果，这件事在本地留下很深的印记。",
        tags = tags(EventTag.ACCEPTANCE, EventTag.PROJECT, EventTag.MASS_APPEAL),
        chain = "c_stall", step = 2, after = 5, weight = 13,
        choices = listOf(
            c("把保交楼的经验总结下来", Attitude.BOLD, "项目顺利交付，你总结的处置经验还被上级推广。",
                AttrDelta(reputation = 3, leaderTrust = 2),
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 4), merit = mapOf(MeritKind.LIVELIHOOD to 5))),
            c("稳妥收尾，不扩大影响", Attitude.STEADY, "项目平稳收尾。你没有声张，问题解决了就好。",
                AttrDelta(work = 2, ability = 2),
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 2, Indicator.APPROVAL to 2))),
            c("把处置过程向业主公开", Attitude.STEADY, "你把节点和进展定期告知业主，情绪稳定了。", AttrDelta(masses = 2, integrityRisk = -1))
        )
    ),

    // ===== 链 9：财政资金不足 =====
    ev(
        id = "c12_fiscal_start", title = "财政资金不足",
        background = "年底的账对不上",
        description = "几笔刚性支出同时在年底到期，财政的盘子明显不够。",
        tags = tags(EventTag.PROJECT, EventTag.INSPECTION, EventTag.LEADER_TASK),
        weight = 12,
        choices = listOf(
            c("全面梳理支出，能压就压", Attitude.STEADY, "你把所有支出过了一遍，压掉了一批非刚性项目。",
                AttrDelta(coordination = 2, ability = 2), chain = "c_fiscal", flag = "fis_cut"),
            c("先保重点，其余的往后排", Attitude.BOLD, "你定了优先序，重点支出先保，其余暂缓。",
                AttrDelta(politics = 2, work = 2), chain = "c_fiscal", flag = "fis_priority"),
            c("先把家底摸清再定方案", Attitude.CAUTIOUS, "你把收支和债务底数摸清了，方案更实。", AttrDelta(ability = 2, writing = 1))
        )
    ),
    ev(
        id = "c12_fiscal_pressure", title = "有人建议挪一挪",
        background = "专项资金还没到使用节点",
        description = "有人提出，先把一时用不上的专项资金挪出来周转一下。",
        tags = tags(EventTag.INTEGRITY, EventTag.INSPECTION, EventTag.PROJECT),
        chain = "c_fiscal", step = 1, after = 2, weight = 12,
        choices = listOf(
            c("不行，专款必须专用", Attitude.BOLD, "你在会上把话说死了。资金紧张的局面还得靠自己解决。",
                AttrDelta(integrityRisk = -4, politics = 3, colleagues = -1), chain = "c_fiscal", flag = "fis_clean",
                governance = cg(merit = mapOf(MeritKind.FINANCE to 4))),
            c("向上级申请临时调度", Attitude.STEADY, "你按规定申请了临时调度，手续合规。",
                AttrDelta(politics = 2, writing = 2, leaderTrust = 1), chain = "c_fiscal", flag = "fis_apply",
                governance = cg(finance = 10)),
            c("先挪一下，事后再补", Attitude.RISKY, "难关暂时过了，但这件事被记在了账上。",
                AttrDelta(integrityRisk = 6), chain = "c_fiscal", flag = "fis_borrow",
                governance = cg(finance = 12, risk = 6))
        )
    ),
    ev(
        id = "c12_fiscal_end", title = "账目最终对上了",
        background = "审计年度账",
        description = "这一年的财政最终经受住了审计的检验吗？",
        tags = tags(EventTag.INSPECTION, EventTag.ACCEPTANCE, EventTag.PROJECT),
        chain = "c_fiscal", step = 2, after = 3, weight = 12,
        choices = listOf(
            c("主动公开收支，接受监督", Attitude.BOLD, "你把主要收支向社会公开，审计和群众都给了正面评价。",
                AttrDelta(integrityRisk = -3, reputation = 3),
                governance = cg(ind = mapOf(Indicator.APPROVAL to 3), merit = mapOf(MeritKind.FINANCE to 4))),
            c("把制度补齐，防止再犯", Attitude.STEADY, "你完善了资金管理制度，同类风险下降。",
                AttrDelta(ability = 2, coordination = 1),
                governance = cg(merit = mapOf(MeritKind.FINANCE to 3))),
            c("把预算绩效评价做起来", Attitude.STEADY, "你把绩效评价引入了预算管理。", AttrDelta(ability = 2, writing = 2))
        )
    ),

    // ===== 链 10：网络舆情 =====
    ev(
        id = "c12_online_start", title = "网络舆情",
        background = "一条帖子火了",
        description = "一条关于本地某件事的帖子被大量转发，评论区情绪激动。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.COMPLAINT, EventTag.MATERIAL),
        weight = 13,
        choices = listOf(
            c("第一时间核实并回应", Attitude.BOLD, "你连夜核实，第二天一早就发了回应。",
                AttrDelta(writing = 2, work = 2), chain = "c_online", flag = "on_time"),
            c("先观察舆情走向", Attitude.CAUTIOUS, "你选择了观察。热度没有立刻降下来。",
                AttrDelta(politics = 2), chain = "c_online", flag = "on_wait"),
            c("先把事实核清楚再表态", Attitude.CAUTIOUS, "你把事实核清后才回应，避免说错话。", AttrDelta(writing = 2, ability = 1))
        )
    ),
    ev(
        id = "c12_online_second", title = "二次发酵",
        background = "有人翻出了旧账",
        description = "舆情没有按预期平息，反而有人翻出了几年前的旧事。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.INSPECTION, EventTag.INTEGRITY),
        chain = "c_online", step = 1, after = 1, weight = 13,
        choices = listOf(
            c("召开新闻发布会，一次说清楚", Attitude.BOLD, "你在发布会上把新旧问题一次讲透，舆情迅速降温。",
                AttrDelta(writing = 3, integrityRisk = -3, reputation = 2), chain = "c_online", flag = "on_press",
                governance = cg(ind = mapOf(Indicator.APPROVAL to 2))),
            c("逐条回应，限时办结", Attitude.STEADY, "你把网友反映的问题逐条落实，用行动回应。",
                AttrDelta(work = 3, coordination = 2), chain = "c_online", flag = "on_resolve",
                governance = cg(ind = mapOf(Indicator.APPROVAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 2))),
            c("找平台降温", Attitude.RISKY, "热度暂时下去了，但「捂盖子」的印象留下了。",
                AttrDelta(integrityRisk = 4, masses = -3), chain = "c_online", flag = "on_cover",
                governance = cg(ind = mapOf(Indicator.APPROVAL to -3), risk = 4))
        )
    ),
    ev(
        id = "c12_online_end", title = "舆情的余波",
        background = "事情平息之后",
        description = "一场舆情过去，留下的是经验，还是教训？",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.ACCEPTANCE, EventTag.MATERIAL),
        chain = "c_online", step = 2, after = 3, weight = 13,
        choices = listOf(
            c("建立舆情快速响应机制", Attitude.BOLD, "你把快速响应变成了制度，此后几次舆情都处置得当。",
                AttrDelta(ability = 2, reputation = 2),
                governance = cg(merit = mapOf(MeritKind.INNOVATION to 3), ind = mapOf(Indicator.APPROVAL to 2))),
            c("加强日常沟通，少让问题上网", Attitude.STEADY, "你把功夫下在平时，群众诉求解决得更早了。",
                AttrDelta(masses = 2, work = 1),
                governance = cg(ind = mapOf(Indicator.APPROVAL to 2), merit = mapOf(MeritKind.LIVELIHOOD to 2))),
            c("把群众反映的问题逐一销号", Attitude.STEADY, "你把问题列成清单逐一销号，群众看得见。", AttrDelta(work = 2, masses = 1))
        )
    )
,
    // ===== 链 11：重大招商机会 =====
    ev(
        id = "c12_invest_start", title = "一个重大招商机会",
        background = "一家龙头企业正在选址",
        description = "对方在几个城市之间比较，条件开得很实，机会难得。",
        tags = tags(EventTag.PROJECT, EventTag.LEADER_TASK),
        weight = 12,
        choices = listOf(
            c("亮出优势，全力争取", Attitude.BOLD, "你把本地的条件做成了完整的方案，对方表现出了兴趣。",
                AttrDelta(work = 3, coordination = 2), chain = "c_invest", flag = "inv_push"),
            c("先摸清对方的真实需求", Attitude.STEADY, "你没有急着报价，先摸清了对方最看重什么。",
                AttrDelta(ability = 2, politics = 2), chain = "c_invest", flag = "inv_probe"),
            c("先请专业机构做评估", Attitude.CAUTIOUS, "你请机构评估了项目的真实价值。", AttrDelta(ability = 2, integrityRisk = -1))
        )
    ),
    ev(
        id = "c12_invest_terms", title = "条件的博弈",
        background = "对方提出了超出常规的条件",
        description = "土地、税收、配套，对方的要价明显高于本地惯例。",
        tags = tags(EventTag.PROJECT, EventTag.INTEGRITY, EventTag.RESEARCH),
        chain = "c_invest", step = 1, after = 2, weight = 12,
        choices = listOf(
            c("守住底线，用服务和发展前景谈", Attitude.STEADY, "你守住了政策底线，用产业配套和效率打动了对方。",
                AttrDelta(ability = 3, integrityRisk = -3), chain = "c_invest", flag = "inv_clean",
                governance = cg(ind = mapOf(Indicator.ECONOMY to 3), merit = mapOf(MeritKind.ECONOMIC to 4))),
            c("超常规给政策，先把项目拿下", Attitude.RISKY, "项目落地的可能性大了，但非常规的承诺埋下了隐患。",
                AttrDelta(work = 2, integrityRisk = 5), chain = "c_invest", flag = "inv_big",
                governance = cg(ind = mapOf(Indicator.ECONOMY to 4), finance = -18, risk = 5)),
            c("放弃这次机会", Attitude.CAUTIOUS, "你评估后决定不跟。项目去了别的城市。",
                AttrDelta(politics = 2), chain = "c_invest", flag = "inv_give_up",
                governance = cg(ind = mapOf(Indicator.ECONOMY to -1)))
        )
    ),
    ev(
        id = "c12_invest_end", title = "项目落地之后",
        background = "两年过去",
        description = "当初争来的项目，如今是什么样子？",
        tags = tags(EventTag.PROJECT, EventTag.ACCEPTANCE, EventTag.RESEARCH),
        chain = "c_invest", step = 2, after = 4, weight = 12,
        choices = listOf(
            c("推动上下游配套，把产业链做起来", Attitude.BOLD, "项目带动了一批配套企业，本地产业初具规模。",
                AttrDelta(ability = 2, reputation = 3),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 6), merit = mapOf(MeritKind.ECONOMIC to 6), finance = 15),
                bonus = listOf(ConditionalEffect("inv_clean", AttrDelta(leaderTrust = 2), extraOutcome = "当初守住底线的做法，让后续合作更受尊重。"))),
            c("做好服务，稳住现有产能", Attitude.STEADY, "项目稳定运行，你成了企业的「服务员」。",
                AttrDelta(work = 2, coordination = 2),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 3), merit = mapOf(MeritKind.ECONOMIC to 3))),
            c("做好跟踪服务，防止水土不服", Attitude.STEADY, "你安排了专班跟踪服务，项目落地更顺。", AttrDelta(coordination = 2, work = 1))
        )
    ),

    // ===== 链 12：群众集体投诉 =====
    ev(
        id = "c12_collective_start", title = "群众集体投诉",
        background = "一个小区的问题积了很久",
        description = "小区居民联名反映物业、停车、环境等一串问题，情绪比较集中。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.COMPLAINT, EventTag.PETITION),
        weight = 12,
        choices = listOf(
            c("带相关部门到现场办公", Attitude.BOLD, "你带着几个部门到小区开了现场会，当场认领了任务。",
                AttrDelta(coordination = 3, masses = 3, work = 2), chain = "c_collective", flag = "col_onsite"),
            c("先分类登记，再逐项交办", Attitude.STEADY, "你把问题分了类，逐项交办给了对口部门。",
                AttrDelta(work = 2, coordination = 2), chain = "c_collective", flag = "col_list"),
            c("先请居民代表一起参与", Attitude.STEADY, "你请居民代表一起梳理问题，诉求更清楚。", AttrDelta(masses = 2, coordination = 1))
        )
    ),
    ev(
        id = "c12_collective_core", title = "核心诉求",
        background = "问题集中到两件事上",
        description = "居民最在意的其实是两件事：停车位和环境整治，都需要真金白银。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.PROJECT, EventTag.PROJECT),
        chain = "c_collective", step = 1, after = 2, weight = 12,
        choices = listOf(
            c("挤出资金，先解决停车和环境", Attitude.BOLD, "你协调资金启动了停车位改造和环境整治，居民看在眼里。",
                AttrDelta(work = 2, masses = 3), chain = "c_collective", flag = "col_fund",
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 4), finance = -12, merit = mapOf(MeritKind.LIVELIHOOD to 4))),
            c("引入物业和居民共治", Attitude.STEADY, "你推动成立了业主委员会，由居民和物业共同治理。",
                AttrDelta(coordination = 3, masses = 2), chain = "c_collective", flag = "col_selfgov",
                governance = cg(ind = mapOf(Indicator.APPROVAL to 2), merit = mapOf(MeritKind.INNOVATION to 3))),
            c("先解决最急的，其余慢慢来", Attitude.CAUTIOUS, "你只处理了最急的一件。居民觉得只是应付了一下。",
                AttrDelta(politics = 2, masses = -2), chain = "c_collective", flag = "col_partial",
                governance = cg(ind = mapOf(Indicator.APPROVAL to -2)))
        )
    ),
    ev(
        id = "c12_collective_end", title = "小区的变化",
        background = "一年后回访",
        description = "当初联名的那个小区，如今变了多少？",
        tags = tags(EventTag.MASS_APPEAL, EventTag.ACCEPTANCE, EventTag.RESEARCH),
        chain = "c_collective", step = 2, after = 3, weight = 12,
        choices = listOf(
            c("推广到其他老旧小区", Attitude.BOLD, "你把经验推广开，一批小区都跟着变好了。",
                AttrDelta(reputation = 3, work = 2),
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 5))),
            c("巩固成果，防止反弹", Attitude.STEADY, "你建立了长效管理机制，成果稳住了。",
                AttrDelta(ability = 2, coordination = 1),
                governance = cg(ind = mapOf(Indicator.LIVELIHOOD to 2), merit = mapOf(MeritKind.LIVELIHOOD to 2))),
            c("把居民满意度作为检验标准", Attitude.STEADY, "你用满意度回访来检验成效，改进了不足。", AttrDelta(masses = 2, ability = 1))
        )
    ),

    // ===== 链 13：上级专项检查 =====
    ev(
        id = "c12_special_start", title = "上级专项检查",
        background = "检查组下周进驻",
        description = "上级确定对本地的某项工作进行一次专项检查。",
        tags = tags(EventTag.INSPECTION, EventTag.LEADER_TASK, EventTag.MATERIAL),
        weight = 12,
        choices = listOf(
            c("提前自查，把问题先解决", Attitude.BOLD, "你组织了自查，发现并整改了一批问题。",
                AttrDelta(work = 3, integrityRisk = -2), chain = "c_special", flag = "spe_selfcheck"),
            c("准备好材料，如实汇报", Attitude.STEADY, "你把材料准备得清清楚楚，等着检查组来。",
                AttrDelta(writing = 3, politics = 2), chain = "c_special", flag = "spe_report"),
            c("先请上级明确检查重点", Attitude.CAUTIOUS, "你主动对接，明确了检查重点，准备更有针对性。", AttrDelta(coordination = 2, writing = 1))
        )
    ),
    ev(
        id = "c12_special_issue", title = "查出了老问题",
        background = "检查组发现了几处历史遗留问题",
        description = "其中有几处是多年前留下的，责任并不完全在现任。",
        tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY, EventTag.MATERIAL),
        chain = "c_special", step = 1, after = 2, weight = 12,
        choices = listOf(
            c("不推不躲，认领并限期整改", Attitude.BOLD, "你主动认领了问题，还定下了整改时限。检查组给了正面评价。",
                AttrDelta(integrityRisk = -3, leaderTrust = 3, work = 2), chain = "c_special", flag = "spe_own",
                governance = cg(merit = mapOf(MeritKind.FINANCE to 3))),
            c("说明来龙去脉，争取客观评价", Attitude.STEADY, "你把历史原因讲清楚，同时承诺整改。检查组表示理解。",
                AttrDelta(writing = 3, politics = 2), chain = "c_special", flag = "spe_explain"),
            c("强调客观原因，弱化责任", Attitude.CAUTIOUS, "你多讲了几分成因。检查组没有多说，但记下了。",
                AttrDelta(politics = 3, integrityRisk = 2), chain = "c_special", flag = "spe_excuse",
                governance = cg(risk = 2))
        )
    ),
    ev(
        id = "c12_special_end", title = "检查结论",
        background = "检查结束了",
        description = "这次专项检查的结论，会进入你的档案。",
        tags = tags(EventTag.INSPECTION, EventTag.ACCEPTANCE, EventTag.LEADER_TASK),
        chain = "c_special", step = 2, after = 2, weight = 12,
        choices = listOf(
            c("把整改经验形成制度", Attitude.BOLD, "你把整改措施固化成制度，同类问题不再出现。",
                AttrDelta(ability = 3, reputation = 2),
                governance = cg(merit = mapOf(MeritKind.FINANCE to 4, MeritKind.INNOVATION to 2))),
            c("抓紧整改，尽快销号", Attitude.STEADY, "你按期完成了整改，问题逐一销号。",
                AttrDelta(work = 2, coordination = 1),
                governance = cg(merit = mapOf(MeritKind.FINANCE to 3))),
            c("把责任分解到人", Attitude.STEADY, "你把整改责任分解到了具体的人和时限。", AttrDelta(coordination = 2, work = 1))
        )
    ),

    // ===== 链 14：突击暗访 =====
    ev(
        id = "c12_sneak_start", title = "突击暗访",
        background = "上级没有打招呼就来了",
        description = "暗访组直接到了窗口单位和基层站所，拍下了不少画面。",
        tags = tags(EventTag.INSPECTION, EventTag.PUBLIC_OPINION, EventTag.COMPLAINT),
        weight = 12,
        choices = listOf(
            c("立即核查，该处理的处理", Attitude.BOLD, "你连夜核查，对责任人进行了处理。态度很硬。",
                AttrDelta(work = 3, integrityRisk = -3, colleagues = -2), chain = "c_sneak", flag = "sn_hard"),
            c("先整改，再向暗访组报告", Attitude.STEADY, "你先改了问题，再向暗访组如实报告。",
                AttrDelta(coordination = 2, writing = 2), chain = "c_sneak", flag = "sn_fix"),
            c("先查清问题出在哪个环节", Attitude.CAUTIOUS, "你顺着流程查了一遍，找到了症结。", AttrDelta(ability = 2, integrityRisk = -1))
        )
    ),
    ev(
        id = "c12_sneak_spread", title = "问题被通报",
        background = "暗访情况要在全市通报",
        description = "通报稿里点了本地的名，措辞不轻。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.INSPECTION, EventTag.MATERIAL),
        chain = "c_sneak", step = 1, after = 2, weight = 12,
        choices = listOf(
            c("公开整改情况，接受监督", Attitude.BOLD, "你把整改情况主动公开，把被动变成了主动。",
                AttrDelta(integrityRisk = -3, reputation = 2), chain = "c_sneak", flag = "sn_open",
                governance = cg(ind = mapOf(Indicator.APPROVAL to 2))),
            c("内部整改，低调处理", Attitude.STEADY, "你在内部完成了整改，通报的余波慢慢过去。",
                AttrDelta(politics = 2, work = 1), chain = "c_sneak", flag = "sn_quiet"),
            c("向上申诉，说明情况", Attitude.CAUTIOUS, "你提交了说明材料。上级没有改口，但了解了一些实情。",
                AttrDelta(writing = 2, politics = 2, leaderTrust = -1), chain = "c_sneak", flag = "sn_appeal")
        )
    ),
    ev(
        id = "c12_sneak_end", title = "窗口的作风",
        background = "几个月后",
        description = "窗口单位的作风，最终是要靠群众来评价的。",
        tags = tags(EventTag.INSPECTION, EventTag.ACCEPTANCE, EventTag.COMPLAINT),
        chain = "c_sneak", step = 2, after = 3, weight = 12,
        choices = listOf(
            c("开展作风整治，建立长效机制", Attitude.BOLD, "你开展了作风整治，窗口的服务明显好转。",
                AttrDelta(ability = 2, reputation = 2),
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 4, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.INNOVATION to 3))),
            c("加强日常监督", Attitude.STEADY, "你把监督检查常态化，问题发现得更早。",
                AttrDelta(coordination = 2),
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 2))),
            c("把群众评价纳入考核", Attitude.STEADY, "你把群众满意度纳入了窗口考核。", AttrDelta(masses = 2, coordination = 1))
        )
    ),

    // ===== 链 15：医共体改革 =====
    ev(
        id = "c12_medical_start", title = "医共体改革",
        background = "上级要求推进县域医共体",
        description = "改革方向是让县医院带动乡镇卫生院，但涉及人、财、物的重新划分。",
        tags = tags(EventTag.RESEARCH, EventTag.MEETING, EventTag.CAREER),
        weight = 11,
        choices = listOf(
            c("先试点，积累经验再推开", Attitude.STEADY, "你选了基础较好的两家卫生院先试。",
                AttrDelta(ability = 2, coordination = 2), chain = "c_medical", flag = "med_pilot"),
            c("全面推开，一次到位", Attitude.BOLD, "你决定整体推进。方案刚一落地，阻力就来了。",
                AttrDelta(work = 3, coordination = 1), chain = "c_medical", flag = "med_all"),
            c("先跟一线医生护士聊聊", Attitude.CAUTIOUS, "你听了基层医护的意见，方案更接地气。", AttrDelta(masses = 1, ability = 2))
        )
    ),
    ev(
        id = "c12_medical_resist", title = "改革遇到阻力",
        background = "既有的格局被打破",
        description = "人财物统一管理之后，原来的一些利益格局被打破，意见不小。",
        tags = tags(EventTag.MEETING, EventTag.COLLEAGUE, EventTag.RESEARCH),
        chain = "c_medical", step = 1, after = 3, weight = 11,
        choices = listOf(
            c("把利益调整讲清楚，做好思想工作", Attitude.STEADY, "你逐家谈话，把改革的逻辑讲透。多数人慢慢接受了。",
                AttrDelta(coordination = 3, colleagues = 3, energy = -3), chain = "c_medical", flag = "med_talk",
                governance = cg(ind = mapOf(Indicator.MEDICAL to 3), merit = mapOf(MeritKind.INNOVATION to 3))),
            c("用制度强推，限期到位", Attitude.BOLD, "改革按节点推进，阻力被压了下来，怨气也压了下来。",
                AttrDelta(work = 3, colleagues = -3), chain = "c_medical", flag = "med_force",
                governance = cg(ind = mapOf(Indicator.MEDICAL to 4), merit = mapOf(MeritKind.INNOVATION to 4))),
            c("放慢节奏，等大家想通", Attitude.CAUTIOUS, "改革慢了下来。阻力小了，进度也慢了。",
                AttrDelta(politics = 3), chain = "c_medical", flag = "med_slow",
                governance = cg(ind = mapOf(Indicator.MEDICAL to 1)))
        )
    ),
    ev(
        id = "c12_medical_end", title = "看病方便了吗",
        background = "一年多以后",
        description = "群众的就医体验，是检验这场改革最直接的标准。",
        tags = tags(EventTag.RESEARCH, EventTag.ACCEPTANCE, EventTag.HEALTH),
        chain = "c_medical", step = 2, after = 4, weight = 11,
        choices = listOf(
            c("继续深化，把基层能力提上去", Attitude.BOLD, "基层卫生院的能力明显提升，群众看病不用再往县里跑。",
                AttrDelta(ability = 2, reputation = 3),
                governance = cg(ind = mapOf(Indicator.MEDICAL to 6, Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 5))),
            c("巩固现有成果", Attitude.STEADY, "改革成果稳住了，没有大的反复。",
                AttrDelta(work = 2),
                governance = cg(ind = mapOf(Indicator.MEDICAL to 3), merit = mapOf(MeritKind.LIVELIHOOD to 2))),
            c("定期评估改革效果", Attitude.STEADY, "你建立了评估机制，问题能及时发现。", AttrDelta(ability = 2, coordination = 1))
        )
    ),

    // ===== 链 16：环保项目邻避 =====
    ev(
        id = "c12_nimby_start", title = "项目选址遇阻",
        background = "垃圾中转站选址引发反对",
        description = "选址一公布，周边居民就强烈反对，说「凭什么建在我们家门口」。",
        tags = tags(EventTag.RESEARCH, EventTag.MASS_APPEAL, EventTag.PROJECT),
        weight = 12,
        choices = listOf(
            c("先公开环评，用数据说话", Attitude.STEADY, "你把环评报告公开了，还请居民代表去看了同类设施。",
                AttrDelta(coordination = 2, masses = 1), chain = "c_nimby", flag = "nim_data"),
            c("重新选址，避开居民区", Attitude.CAUTIOUS, "你决定另找地方。项目慢了下来。",
                AttrDelta(politics = 2, masses = 2), chain = "c_nimby", flag = "nim_move"),
            c("先请第三方做环境评估", Attitude.CAUTIOUS, "第三方评估出来了，沟通有了依据。", AttrDelta(ability = 2, integrityRisk = -1))
        )
    ),
    ev(
        id = "c12_nimby_debate", title = "面对面的沟通",
        background = "居民要求对话",
        description = "居民代表要求政府出面，把这件事讲清楚。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.MEETING, EventTag.RESEARCH),
        chain = "c_nimby", step = 1, after = 2, weight = 12,
        choices = listOf(
            c("亲自到场，答问到满意为止", Attitude.BOLD, "你在现场回答了三个小时的问题。居民的态度缓和了很多。",
                AttrDelta(masses = 3, coordination = 3, energy = -4), chain = "c_nimby", flag = "nim_meet",
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to 3, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.INNOVATION to 3))),
            c("承诺补偿和共建，换取支持", Attitude.STEADY, "你承诺为周边改善环境和设施，多数居民点了头。",
                AttrDelta(coordination = 2, masses = 2), chain = "c_nimby", flag = "nim_deal",
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to 2, Indicator.LIVELIHOOD to 2), finance = -10)),
            c("按程序推进，不做过多的让步", Attitude.RISKY, "项目按程序继续。少数居民始终不服，成了持续的上访源。",
                AttrDelta(work = 2, masses = -3, integrityRisk = 2), chain = "c_nimby", flag = "nim_force",
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to 3, Indicator.APPROVAL to -3), risk = 3))
        )
    ),
    ev(
        id = "c12_nimby_end", title = "设施建起来了",
        background = "一年之后",
        description = "当初争议很大的这个设施，如今运行得怎么样？",
        tags = tags(EventTag.RESEARCH, EventTag.ACCEPTANCE, EventTag.PROJECT),
        chain = "c_nimby", step = 2, after = 3, weight = 12,
        choices = listOf(
            c("把周边环境一起做起来", Attitude.BOLD, "你顺带把周边绿化、道路也提升了，居民从反对变成了接受。",
                AttrDelta(reputation = 2, masses = 3),
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to 4, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.INNOVATION to 3))),
            c("确保规范运行，不出问题", Attitude.STEADY, "设施规范运行，没有出现异味和投诉。",
                AttrDelta(work = 2, ability = 1),
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to 3))),
            c("定期公开运行数据", Attitude.STEADY, "你把运行数据定期公开，居民放心了。", AttrDelta(masses = 2, integrityRisk = -2))
        )
    ),

    // ===== 链 17：农产品品牌 =====
    ev(
        id = "c12_brand_start", title = "打造农产品品牌",
        background = "本地的特色农产品想打出去",
        description = "品质不错，但没品牌、没渠道，卖不上价。",
        tags = tags(EventTag.RURAL, EventTag.PROJECT, EventTag.RESEARCH),
        weight = 11,
        choices = listOf(
            c("统一标准，先做品质", Attitude.STEADY, "你推动制定了统一的生产标准，品质先稳住了。",
                AttrDelta(ability = 2, work = 2), chain = "c_brand", flag = "bra_quality"),
            c("先投钱做营销，把名气打出去", Attitude.BOLD, "你投了一笔钱做宣传，短期内知名度上来了。",
                AttrDelta(work = 2, politics = 2), chain = "c_brand", flag = "bra_market"),
            c("先请老农和专家一起定标准", Attitude.CAUTIOUS, "你把老农和专家请到一起，标准更实用。", AttrDelta(ability = 2, coordination = 1))
        )
    ),
    ev(
        id = "c12_brand_crisis", title = "质量风波",
        background = "有客户反映产品有问题",
        description = "一批产品被反映质量不稳定，网上的评价开始变差。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.INSPECTION, EventTag.RURAL),
        chain = "c_brand", step = 1, after = 2, weight = 11,
        choices = listOf(
            c("召回问题批次，重建标准", Attitude.BOLD, "你召回了问题批次，还重新梳理了标准。品牌经受住了考验。",
                AttrDelta(integrityRisk = -3, work = 2, reputation = 2), chain = "c_brand", flag = "bra_recall",
                governance = cg(ind = mapOf(Indicator.ECONOMY to 2, Indicator.APPROVAL to 2), merit = mapOf(MeritKind.ECONOMIC to 3))),
            c("先解释，避免影响扩大", Attitude.STEADY, "你做了说明，也改了流程。风波慢慢过去了。",
                AttrDelta(writing = 2, coordination = 1), chain = "c_brand", flag = "bra_explain"),
            c("压热度，别让品牌受损", Attitude.RISKY, "热度压下去了，但消费者心里留下了疙瘩。",
                AttrDelta(integrityRisk = 3, politics = 2), chain = "c_brand", flag = "bra_cover",
                governance = cg(ind = mapOf(Indicator.ECONOMY to -1), risk = 3))
        )
    ),
    ev(
        id = "c12_brand_end", title = "品牌立住了吗",
        background = "两年之后",
        description = "本地的农产品，最终有没有卖上好价钱？",
        tags = tags(EventTag.RURAL, EventTag.ACCEPTANCE, EventTag.PROJECT),
        chain = "c_brand", step = 2, after = 4, weight = 11,
        choices = listOf(
            c("延伸产业链，做深加工", Attitude.BOLD, "你推动发展深加工，农产品的附加值上去了。",
                AttrDelta(ability = 2, reputation = 2),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 5, Indicator.LIVELIHOOD to 3), merit = mapOf(MeritKind.ECONOMIC to 4), finance = 10)),
            c("稳住品质，稳步推广", Attitude.STEADY, "品牌稳步成长，农民的收入也稳中有升。",
                AttrDelta(work = 2),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 3, Indicator.LIVELIHOOD to 2), merit = mapOf(MeritKind.ECONOMIC to 3))),
            c("把品牌收益返还给农户", Attitude.STEADY, "你把收益和农户挂钩，大家的积极性上来了。", AttrDelta(masses = 2, work = 1))
        )
    ),

    // ===== 链 18：数字政务 =====
    ev(
        id = "c12_digital_start", title = "数字政务怎么推",
        background = "群众办事还是跑断腿",
        description = "上级要求推进「最多跑一次」，但各部门的数据各自为政。",
        tags = tags(EventTag.RESEARCH, EventTag.PROJECT, EventTag.COORDINATION),
        weight = 11,
        choices = listOf(
            c("先打通数据，再谈应用", Attitude.STEADY, "你从数据共享入手，虽然慢，但基础牢。",
                AttrDelta(ability = 2, coordination = 2), chain = "c_digital", flag = "dig_data"),
            c("先上线一个高频事项，做出样板", Attitude.BOLD, "你选了群众最常办的一件事先做，很快见了效果。",
                AttrDelta(work = 2, coordination = 1), chain = "c_digital", flag = "dig_sample"),
            c("先选一个部门做试点", Attitude.CAUTIOUS, "你选了一个部门试点，风险可控。", AttrDelta(ability = 2, coordination = 1))
        )
    ),
    ev(
        id = "c12_digital_risk", title = "数据安全的顾虑",
        background = "部门担心数据外泄",
        description = "几个部门对数据共享顾虑很大，怕出安全问题担责任。",
        tags = tags(EventTag.MEETING, EventTag.COORDINATION, EventTag.INSPECTION),
        chain = "c_digital", step = 1, after = 2, weight = 11,
        choices = listOf(
            c("建立严格的安全制度再共享", Attitude.STEADY, "你先立了规矩，部门才放了心。进度慢了一点，但很稳。",
                AttrDelta(coordination = 3, ability = 2), chain = "c_digital", flag = "dig_safe",
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 3), merit = mapOf(MeritKind.INNOVATION to 3))),
            c("主要领导拍板，先共享后完善", Attitude.BOLD, "你拍了板，数据很快打通了。效率高，风险也在。",
                AttrDelta(work = 3, coordination = 2, integrityRisk = 2), chain = "c_digital", flag = "dig_force",
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 2), merit = mapOf(MeritKind.INNOVATION to 4), risk = 3)),
            c("请上级数据部门来指导", Attitude.CAUTIOUS, "上级给了指导，共享方案更规范。", AttrDelta(coordination = 2, leaderTrust = 1))
        )
    ),
    ev(
        id = "c12_digital_end", title = "群众少跑腿了吗",
        background = "一年之后",
        description = "「最多跑一次」到底跑成了没有？",
        tags = tags(EventTag.RESEARCH, EventTag.ACCEPTANCE, EventTag.COORDINATION),
        chain = "c_digital", step = 2, after = 3, weight = 11,
        choices = listOf(
            c("继续扩展，覆盖更多事项", Attitude.BOLD, "你把可线上办理的事项扩大了一倍，群众好评不断。",
                AttrDelta(reputation = 2, work = 2),
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 5, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.INNOVATION to 5))),
            c("把现有事项做扎实", Attitude.STEADY, "现有事项运行稳定，群众满意度稳步提升。",
                AttrDelta(work = 1, ability = 1),
                governance = cg(ind = mapOf(Indicator.OFFICIAL_SATISFACTION to 3), merit = mapOf(MeritKind.INNOVATION to 2))),
            c("请群众来评价好不好用", Attitude.STEADY, "你请群众来体验评价，功能越改越好用。", AttrDelta(masses = 2, ability = 1))
        )
    ),

    // ===== 链 19：班子调整 =====
    ev(
        id = "c12_transition_start", title = "班子要调整了",
        background = "组织部门来征求意见",
        description = "上级正在酝酿班子调整，找你了解情况，也顺便听听你的想法。",
        tags = tags(EventTag.CAREER, EventTag.ASSESSMENT, EventTag.LEADER_TALK),
        weight = 12, minPos = 3,
        choices = listOf(
            c("如实评价，不掺私心", Attitude.STEADY, "你客观地谈了几位同志的情况。组织部门记下了。",
                AttrDelta(politics = 3, integrityRisk = -2, reputation = 2), chain = "c_transition", flag = "tr_objective"),
            c("为关系近的同志说句话", Attitude.BOLD, "你替一位共事多年的同志美言了几句。",
                AttrDelta(colleagues = 2, politics = 2, integrityRisk = 1), chain = "c_transition", flag = "tr_favor"),
            c("先听一听班子成员的意见", Attitude.CAUTIOUS, "你把班子成员的意见听了一遍，心里更有数。", AttrDelta(politics = 2, coordination = 1))
        )
    ),
    ev(
        id = "c12_transition_choice", title = "你自己的位子",
        background = "有一个岗位向你敞开了",
        description = "这次调整中，一个更重要的岗位出现了空缺，你也在考虑范围之内。",
        tags = tags(EventTag.CAREER, EventTag.ASSESSMENT, EventTag.MEETING),
        chain = "c_transition", step = 1, after = 2, weight = 12, minPos = 3,
        choices = listOf(
            c("主动争取，把成绩摆出来", Attitude.BOLD, "你把近年来的工作梳理成材料递了上去，也说明了愿意挑更重担子的想法。",
                AttrDelta(leaderTrust = 2, reputation = 2, colleagues = -1), chain = "c_transition", flag = "tr_compete",
                politics = PoliticalImpact(capital = -4, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3))),
            c("服从组织安排，不跑不要", Attitude.STEADY, "你没有去争，只是把手头的事做好。",
                AttrDelta(politics = 3, integrityRisk = -2), chain = "c_transition", flag = "tr_wait",
                politics = PoliticalImpact(capital = 2, tendencies = mapOf(PoliticalTendency.BALANCED to 3))),
            c("争取，但不争", Attitude.CAUTIOUS, "你表达了意愿，也说明服从组织决定。分寸拿捏得不错。",
                AttrDelta(politics = 3, coordination = 2), chain = "c_transition", flag = "tr_soft")
        )
    ),
    ev(
        id = "c12_transition_end", title = "任命公布了",
        background = "调整结果下来了",
        description = "这次班子调整的结果，会影响你接下来几年的路。",
        tags = tags(EventTag.CAREER, EventTag.ACCEPTANCE, EventTag.ASSESSMENT),
        chain = "c_transition", step = 2, after = 2, weight = 12, minPos = 3,
        choices = listOf(
            c("无论结果如何，把手上的事做好", Attitude.STEADY, "你把注意力放回了工作本身。",
                AttrDelta(work = 2, reputation = 2), politics = PoliticalImpact(capital = 2)),
            c("总结这次的经验", Attitude.CAUTIOUS, "你把这次调整中看到的、听到的，记在了心里。",
                AttrDelta(politics = 3, ability = 2), politics = PoliticalImpact(capital = 1)),
            c("把心思放回工作上", Attitude.STEADY, "你没有多想，继续把手上的事做好。", AttrDelta(work = 2, reputation = 1))
        )
    ),

    // ===== 链 20：旅游公路 =====
    ev(
        id = "c12_tourroad_start", title = "一条旅游公路",
        background = "景区之间缺一条像样的路",
        description = "修路能带动旅游，但路线要穿过一片林地，环保上有争议。",
        tags = tags(EventTag.PROJECT, EventTag.RESEARCH, EventTag.RESEARCH),
        weight = 11,
        choices = listOf(
            c("先做方案比选，找对环境扰动最小的线", Attitude.STEADY, "你请设计单位做了多方案比选，尽量避开敏感区。",
                AttrDelta(ability = 2, coordination = 2), chain = "c_tourroad", flag = "road_plan"),
            c("尽快上马，抢占旅游旺季", Attitude.BOLD, "为了赶工期，你选了最短的线路。",
                AttrDelta(work = 3), chain = "c_tourroad", flag = "road_fast"),
            c("先请交通和环保一起论证", Attitude.CAUTIOUS, "两个部门一起论证，方案更稳。", AttrDelta(ability = 2, coordination = 1))
        )
    ),
    ev(
        id = "c12_tourroad_env", title = "环保的质疑",
        background = "有人向上反映毁林问题",
        description = "环保组织反映，公路施工对林地造成了破坏，要求停工核查。",
        tags = tags(EventTag.RESEARCH, EventTag.INSPECTION, EventTag.PROJECT),
        chain = "c_tourroad", step = 1, after = 2, weight = 11,
        choices = listOf(
            c("主动停工核查，补种修复", Attitude.BOLD, "你主动停工核查，还组织了补种。工期延后，但守住了底线。",
                AttrDelta(integrityRisk = -3, work = 2, reputation = 2), chain = "c_tourroad", flag = "road_restore",
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to 4), merit = mapOf(MeritKind.INNOVATION to 3), finance = -8)),
            c("优化方案，调整施工方式", Attitude.STEADY, "你调整了施工方式，把影响控制在了较小范围。",
                AttrDelta(coordination = 2, ability = 1), chain = "c_tourroad", flag = "road_adjust",
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to 2))),
            c("边施工边解释，先保工期", Attitude.RISKY, "工程没有停。舆论的压力和上级的关注都来了。",
                AttrDelta(work = 2, integrityRisk = 4), chain = "c_tourroad", flag = "road_push",
                governance = cg(ind = mapOf(Indicator.ENVIRONMENT to -3), risk = 4))
        )
    ),
    ev(
        id = "c12_tourroad_end", title = "路通了以后",
        background = "两年之后",
        description = "这条路最终给本地带来了什么？",
        tags = tags(EventTag.PROJECT, EventTag.ACCEPTANCE, EventTag.RESEARCH),
        chain = "c_tourroad", step = 2, after = 4, weight = 11,
        choices = listOf(
            c("把沿线村庄也带起来", Attitude.BOLD, "你把沿线的农家乐、农产品都串了起来，村民也跟着受益。",
                AttrDelta(reputation = 3, masses = 3),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 5, Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 3), merit = mapOf(MeritKind.ECONOMIC to 5))),
            c("重点做好养护和运营", Attitude.STEADY, "你把路养护好，景区客流稳步增长。",
                AttrDelta(work = 2, coordination = 1),
                governance = cg(ind = mapOf(Indicator.ECONOMY to 3, Indicator.INFRASTRUCTURE to 2), merit = mapOf(MeritKind.ECONOMIC to 3))),
            c("把沿线生态修复做扎实", Attitude.STEADY, "你安排了生态修复，补种和管护都落了地。", AttrDelta(ability = 1, integrityRisk = -1, masses = 1))
        )
    )
)




