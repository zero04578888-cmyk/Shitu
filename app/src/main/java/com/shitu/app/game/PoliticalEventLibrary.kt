package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.EventTag
import com.shitu.app.model.Faction
import com.shitu.app.model.GameEvent
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.PoliticalEffect
import com.shitu.app.model.PoliticalGate

/**
 * 官场事件库。
 *
 * 与普通事件使用同一套机制（选项、属性影响、事件链、前置条件），
 * 区别只在于它们的选项会改动人物关系、势力关系与官场风险。
 *
 * 数值都是游戏模拟参数。
 */
object PoliticalEventLibrary {

    val events: List<GameEvent> = listOf(
        ev(
            id = "pol_policy_question", title = "领导问你怎么看",
            background = "会后的走廊",
            description = "散会时领导叫住你，随口问了一句：「刚才那个政策，你怎么看？」" +
                "走廊里还有别人，脚步都慢了下来。",
            tags = tags(EventTag.LEADER_TALK, EventTag.CAREER),
            weight = 9,
            repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "照实说，把问题讲到点子上", Attitude.BOLD,
                    "你说得太直，领导没接话，只点了点头。旁边有人替你捏了把汗。",
                    AttrDelta(ability = 3, reputation = 3, leaderTrust = -2),
                    HiddenDelta(stress = 4),
                    political = PoliticalEffect(
                        relations = mapOf("gao_zhiqiang" to 6, "chen_jianguo" to -3),
                        factions = mapOf(Faction.REFORM to 4, Faction.CONSERVATIVE to -2)
                    ),
                    hint = "改革派会记住你"
                ),
                c(
                    "顺着领导的思路往下说", Attitude.STEADY,
                    "领导听完又补了两句，气氛很顺。你心里清楚自己刚才省掉了什么。",
                    AttrDelta(leaderTrust = 4, politics = 2),
                    political = PoliticalEffect(
                        relations = mapOf("chen_jianguo" to 5),
                        factions = mapOf(Faction.LOCAL to 3)
                    )
                ),
                c(
                    "说得很稳，不明确表态", Attitude.CAUTIOUS,
                    "你把两边的道理都讲了一遍，谁也没得罪，谁也没记住你的观点。",
                    AttrDelta(politics = 2, integrityRisk = -1),
                    political = PoliticalEffect(
                        relations = mapOf("sun_dehai" to 3),
                        factions = mapOf(Faction.CONSERVATIVE to 3),
                        risk = -2
                    )
                )
            )
        ),
        ev(
            id = "pol_dinner", title = "一场饭局",
            background = "周末的私人聚会",
            description = "一位老同学组了局，来的都是本系统的人。座位怎么坐，酒怎么喝，" +
                "在别人眼里都是话。",
            tags = tags(EventTag.MEETING, EventTag.CAREER),
            weight = 8,
            repeatable = true, cooldown = 10,
            choices = listOf(
                c(
                    "坐到主位那一桌去", Attitude.BOLD,
                    "你端着杯子在主桌坐下了。散场时有人说，这人心里有数。",
                    AttrDelta(health = -4, energy = -3),
                    HiddenDelta(network = 8, stress = 6),
                    political = PoliticalEffect(
                        relations = mapOf("chen_jianguo" to 6),
                        factions = mapOf(Faction.LOCAL to 5),
                        risk = 4
                    )
                ),
                c(
                    "找个理由提前走", Attitude.CAUTIOUS,
                    "你说家里有事，九点就走了。第二天有人开玩笑说你不好请。",
                    AttrDelta(energy = 2, family = 3),
                    political = PoliticalEffect(
                        relations = mapOf("chen_jianguo" to -4, "sun_dehai" to 2),
                        risk = -3
                    )
                ),
                c(
                    "挨桌敬酒，谁也不冷落", Attitude.STEADY,
                    "你一圈下来喝了十几杯，谁都觉得你给足了面子。",
                    AttrDelta(coordination = 3, health = -6, energy = -6),
                    HiddenDelta(network = 10, healthRisk = 5),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.LOCAL to 3, Faction.NEUTRAL to 3),
                        risk = 5
                    )
                )
            )
        ),
        ev(
            id = "pol_colleague_credit", title = "署名之争",
            background = "材料署名的争议",
            description = "一份你主笔的材料，在汇报时被马文斌说成了「我们科室一起弄的」。" +
                "坐在这里的人都听得出区别。",
            tags = tags(EventTag.COLLEAGUE),
            weight = 9,
            repeatable = true, cooldown = 12,
            minPos = 1,
            choices = listOf(
                c(
                    "当场把过程说清楚", Attitude.BOLD,
                    "你把改稿记录摆了出来。会议室安静了几秒，事情算是清楚了。",
                    AttrDelta(leaderTrust = 3, reputation = 2, colleagues = -4),
                    HiddenDelta(stress = 5),
                    political = PoliticalEffect(
                        relations = mapOf("ma_wenbin" to -10),
                        factions = mapOf(Faction.PRAGMATIC to -2),
                        risk = 2
                    ),
                    hint = "得罪一个同僚"
                ),
                c(
                    "私下提醒他一次", Attitude.STEADY,
                    "你在楼梯间把话说开了。他愣了一下，说下次注意。",
                    AttrDelta(colleagues = 4),
                    political = PoliticalEffect(
                        relations = mapOf("ma_wenbin" to 4),
                        trust = mapOf("ma_wenbin" to 5)
                    )
                ),
                c(
                    "算了，不纠结这一次", Attitude.CAUTIOUS,
                    "你什么也没说。回到座位上，你盯着屏幕看了很久。",
                    AttrDelta(work = -2),
                    HiddenDelta(stress = 6),
                    political = PoliticalEffect(relations = mapOf("ma_wenbin" to 2))
                )
            )
        ),
        ev(
            id = "pol_gift_from_business", title = "老板的心意",
            background = "项目结束之后",
            description = "郑海涛把一个纸袋放在你办公室的柜子里，说「就是点土特产，别多想」，" +
                "转身就走了。",
            tags = tags(EventTag.GIFT, EventTag.INTEGRITY),
            weight = 8,
            repeatable = true, cooldown = 16,
            choices = listOf(
                c(
                    "收下，什么也不说", Attitude.RISKY,
                    "袋子比看上去沉。你把它锁进了柜子，也把一件事锁进了心里。",
                    AttrDelta(wealth = 10),
                    HiddenDelta(riskPressure = 12),
                    political = PoliticalEffect(
                        relations = mapOf("zheng_haitao" to 8),
                        risk = 12,
                        secrets = setOf("zheng_haitao")
                    ),
                    hint = "有人从此捏着你的把柄"
                ),
                c(
                    "追出去退回去，把话说清楚", Attitude.BOLD,
                    "你在楼下把袋子塞回他手里。他笑了笑，说你想多了。",
                    AttrDelta(reputation = 2, integrityRisk = -3),
                    political = PoliticalEffect(
                        relations = mapOf("zheng_haitao" to -4),
                        risk = -3
                    )
                ),
                c(
                    "收下，交到单位登记", Attitude.STEADY,
                    "你把东西原封不动交到了办公室，还留了一张登记单。",
                    AttrDelta(politics = 3, leaderTrust = 2),
                    political = PoliticalEffect(
                        relations = mapOf("zheng_haitao" to -2),
                        factions = mapOf(Faction.CONSERVATIVE to 3)
                    )
                )
            )
        ),
        ev(
            id = "pol_subordinate_help", title = "下属的难题",
            background = "交办任务卡住了",
            description = "林晓芸站在你工位旁边，小声说她那部分材料出了点问题，" +
                "明天就要报上去。",
            tags = tags(EventTag.COLLEAGUE, EventTag.MATERIAL),
            weight = 8,
            repeatable = true, cooldown = 10,
            choices = listOf(
                c(
                    "把她的活接过来自己扛", Attitude.BOLD,
                    "你重新写了一遍，凌晨两点发出去。她第二天来得很早，什么也没说，" +
                        "给你泡了杯茶。",
                    AttrDelta(work = 2, energy = -5),
                    HiddenDelta(stress = 5),
                    political = PoliticalEffect(
                        relations = mapOf("lin_xiaoyun" to 8),
                        trust = mapOf("lin_xiaoyun" to 6)
                    )
                ),
                c(
                    "让她自己想清楚再来找我", Attitude.CAUTIOUS,
                    "她红着脸走了，一小时后带着改好的版本回来。",
                    AttrDelta(work = 1),
                    political = PoliticalEffect(
                        relations = mapOf("lin_xiaoyun" to -5),
                        trust = mapOf("lin_xiaoyun" to -3)
                    )
                ),
                c(
                    "坐下来教她怎么改", Attitude.STEADY,
                    "你讲了半个小时思路。她记了满满一页，后来这份材料被当作范文。",
                    AttrDelta(ability = 1, colleagues = 2),
                    political = PoliticalEffect(
                        relations = mapOf("lin_xiaoyun" to 5),
                        trust = mapOf("lin_xiaoyun" to 4)
                    )
                )
            )
        ),
        ev(
            id = "pol_rumor", title = "风言风语",
            background = "有人在背后说起你",
            description = "你听到一些说法，关于你某次办事的程序，版本不止一个。",
            tags = tags(EventTag.PUBLIC_OPINION, EventTag.INTEGRITY),
            weight = 10,
            repeatable = true, cooldown = 8,
            gate = PoliticalGate(minRisk = 30),
            choices = listOf(
                c(
                    "主动找领导说明情况", Attitude.BOLD,
                    "你把经过原原本本讲了一遍。领导说知道了，你先回去干活。",
                    AttrDelta(leaderTrust = 2, reputation = -1),
                    political = PoliticalEffect(risk = -10)
                ),
                c(
                    "不理会，该干什么干什么", Attitude.STEADY,
                    "你照常上班。传言没有消失，只是暂时没有人当面提起。",
                    AttrDelta(politics = 1),
                    HiddenDelta(stress = 5),
                    political = PoliticalEffect(risk = 4)
                ),
                c(
                    "找人打听是谁在传", Attitude.RISKY,
                    "你托了两个人去问。消息传回来了，也传出去了。",
                    AttrDelta(coordination = 2, reputation = -2),
                    HiddenDelta(network = 4, socialInfluence = -2),
                    political = PoliticalEffect(risk = 6)
                )
            )
        ),
        ev(
            id = "pol_talk", title = "谈话提醒",
            background = "被叫到一间小会议室",
            description = "分管领导和一个你不认识的人坐在对面，桌上放着一份材料。" +
                "「今天找你聊聊，不用紧张。」",
            tags = tags(EventTag.INTEGRITY, EventTag.LEADER_TALK),
            weight = 12,
            repeatable = true, cooldown = 8,
            gate = PoliticalGate(minRisk = 60),
            choices = listOf(
                c(
                    "如实说明，主动把问题讲清楚", Attitude.BOLD,
                    "你讲了两个小时，把能想起来的都说了。走的时候，对方说了句「态度是好的」。",
                    AttrDelta(integrityRisk = -4, reputation = -2),
                    HiddenDelta(riskPressure = -15),
                    political = PoliticalEffect(risk = -18)
                ),
                c(
                    "承认一部分，其余含糊过去", Attitude.CAUTIOUS,
                    "你把小的说了，大的绕开了。对方没有再追问。",
                    AttrDelta(politics = 2, energy = -4),
                    HiddenDelta(stress = 8),
                    political = PoliticalEffect(risk = -6)
                ),
                c(
                    "一概否认", Attitude.RISKY,
                    "你说这些都不是事实。对方合上本子，说那就先这样。",
                    AttrDelta(reputation = -4, integrityRisk = 3),
                    HiddenDelta(riskPressure = 10, stress = 10),
                    political = PoliticalEffect(risk = 8)
                )
            )
        ),
        ev(
            id = "pol_investigation", title = "专项检查",
            background = "检查组进驻",
            description = "一桩多年前的项目被列入专项检查范围，你当时是经办人之一。",
            tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY),
            weight = 14,
            repeatable = true, cooldown = 12,
            gate = PoliticalGate(minRisk = 80),
            choices = listOf(
                c(
                    "全面配合，主动自查", Attitude.BOLD,
                    "你把所有经手的材料整理成册交了上去，连瑕疵也标了出来。",
                    AttrDelta(work = -3, politics = 4),
                    HiddenDelta(riskPressure = -20),
                    political = PoliticalEffect(risk = -25)
                ),
                c(
                    "托人打招呼，想把范围缩小", Attitude.RISKY,
                    "有人答应试试。几天后你听说，检查组把范围扩大了。",
                    AttrDelta(reputation = -3, colleagues = -3),
                    HiddenDelta(riskPressure = 12),
                    political = PoliticalEffect(
                        relations = mapOf("chen_jianguo" to -6),
                        risk = 5
                    )
                ),
                c(
                    "沉默应对，问什么答什么", Attitude.CAUTIOUS,
                    "你只回答被问到的问题，一句多余的都没有。",
                    AttrDelta(leaderTrust = -4, energy = -6),
                    HiddenDelta(stress = 8),
                    political = PoliticalEffect(risk = -8)
                )
            )
        ),
        ev(
            id = "pol_transfer_offer", title = "借调机会",
            background = "上面要借一个人",
            description = "市里一个专班要借调干部，为期一年。那边活重，但能见到的人不一样。",
            tags = tags(EventTag.CAREER, EventTag.LEADER_TASK),
            weight = 10,
            repeatable = true, cooldown = 20,
            gate = PoliticalGate(
                minFaction = mapOf(Faction.REFORM to 25)
            ),
            choices = listOf(
                c(
                    "去，机会难得", Attitude.BOLD,
                    "你收拾了行李。这一年很累，但你见到了原来见不到的人和场面。",
                    AttrDelta(work = 4, ability = 4, family = -6),
                    HiddenDelta(opportunity = 8, stress = 8),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.REFORM to 4, Faction.CORE to 2)
                    )
                ),
                c(
                    "不去，手上还有没做完的事", Attitude.STEADY,
                    "你说手头的项目走不开。领导没有再劝。",
                    AttrDelta(leaderTrust = -2, family = 4),
                    HiddenDelta(opportunity = -4)
                ),
                c(
                    "推荐更合适的同事去", Attitude.CAUTIOUS,
                    "你把马文斌推了上去。他走之前请你吃了顿饭。",
                    AttrDelta(colleagues = 5),
                    political = PoliticalEffect(
                        relations = mapOf("ma_wenbin" to 8),
                        factions = mapOf(Faction.REFORM to -3)
                    )
                )
            )
        ),
        ev(
            id = "pol_org_talk", title = "组织部谈话",
            background = "考察组约你谈话",
            description = "周慧兰约你在组织部谈了一个小时，问得很细，包括你对现在岗位的想法。",
            tags = tags(EventTag.CAREER, EventTag.LEADER_TALK),
            weight = 10,
            repeatable = true, cooldown = 16,
            gate = PoliticalGate(minFaction = mapOf(Faction.CORE to 25)),
            choices = listOf(
                c(
                    "把自己的成绩讲清楚", Attitude.BOLD,
                    "你讲了三件事，每一件都有具体数字。她一直在记。",
                    AttrDelta(leaderTrust = 4, reputation = 2),
                    political = PoliticalEffect(
                        relations = mapOf("zhou_huilan" to 8),
                        trust = mapOf("zhou_huilan" to 6),
                        factions = mapOf(Faction.CORE to 5)
                    )
                ),
                c(
                    "多谈不足和想法", Attitude.STEADY,
                    "你谈了自己的短板，也谈了下一步想怎么做。她抬头看了你一眼。",
                    AttrDelta(politics = 4, work = -2),
                    political = PoliticalEffect(
                        relations = mapOf("zhou_huilan" to 5),
                        trust = mapOf("zhou_huilan" to 8),
                        factions = mapOf(Faction.CORE to 3)
                    )
                ),
                c(
                    "少说多听", Attitude.CAUTIOUS,
                    "大部分时间你在听。她最后说了一句「你先回去吧」。",
                    AttrDelta(politics = 2),
                    political = PoliticalEffect(
                        relations = mapOf("zhou_huilan" to 2),
                        factions = mapOf(Faction.CORE to 3)
                    )
                )
            )
        ),
        ev(
            id = "pol_old_leader_favor", title = "老领导的托付",
            background = "一通电话",
            description = "孙德海打来电话，说他一个晚辈的事卡住了，问你方不方便过问一下。",
            tags = tags(EventTag.CAREER, EventTag.COLLEAGUE),
            weight = 9,
            repeatable = true, cooldown = 14,
            gate = PoliticalGate(minNpcRelation = mapOf("sun_dehai" to 25)),
            choices = listOf(
                c(
                    "答应下来，帮他跑一趟", Attitude.BOLD,
                    "你打了几个电话，事情有了眉目。他在电话那头连说了三个好。",
                    AttrDelta(work = -3, colleagues = 2),
                    HiddenDelta(network = 6),
                    political = PoliticalEffect(
                        relations = mapOf("sun_dehai" to 10),
                        trust = mapOf("sun_dehai" to 8),
                        risk = 3
                    )
                ),
                c(
                    "委婉说明现在不方便", Attitude.CAUTIOUS,
                    "他说理解，你忙你的。挂了电话，你坐了一会儿。",
                    AttrDelta(leaderTrust = -1, energy = 2),
                    political = PoliticalEffect(
                        relations = mapOf("sun_dehai" to -10),
                        trust = mapOf("sun_dehai" to -6),
                        risk = -2
                    )
                ),
                c(
                    "按规矩办，能帮的在范围内帮", Attitude.STEADY,
                    "你告诉他该走哪个程序，还帮他理了材料。",
                    AttrDelta(work = 1, integrityRisk = -2),
                    political = PoliticalEffect(
                        relations = mapOf("sun_dehai" to 6),
                        trust = mapOf("sun_dehai" to 5),
                        factions = mapOf(Faction.CONSERVATIVE to 2)
                    )
                )
            )
        ),
        ev(
            id = "pol_junior_offer", title = "牵个线",
            background = "郑海涛又来办公室",
            description = "郑海涛说他想认识一下发改局的人，问你「方便的话，一起吃个饭？」",
            tags = tags(EventTag.GIFT, EventTag.COORDINATION),
            weight = 8,
            repeatable = true, cooldown = 16,
            gate = PoliticalGate(minNpcRelation = mapOf("zheng_haitao" to 30)),
            choices = listOf(
                c(
                    "帮他约这个局", Attitude.RISKY,
                    "饭吃了，事也谈了。你知道自己从这天起，站在了一条线上。",
                    AttrDelta(wealth = 8, reputation = 2),
                    HiddenDelta(riskPressure = 8),
                    political = PoliticalEffect(
                        relations = mapOf("zheng_haitao" to 8, "gao_zhiqiang" to 4),
                        risk = 8,
                        secrets = setOf("zheng_haitao")
                    ),
                    hint = "从此脱不开关系"
                ),
                c(
                    "婉拒，说自己不方便出面", Attitude.CAUTIOUS,
                    "他说「那就算了」，语气听不出什么。",
                    AttrDelta(reputation = 1, integrityRisk = -2),
                    political = PoliticalEffect(
                        relations = mapOf("zheng_haitao" to -8),
                        risk = -2
                    )
                ),
                c(
                    "把他介绍给对口的部门", Attitude.STEADY,
                    "你按正常渠道做了引荐，全程有记录。",
                    AttrDelta(coordination = 3, work = 2),
                    political = PoliticalEffect(
                        relations = mapOf("zheng_haitao" to 3, "gao_zhiqiang" to 3),
                        risk = 2
                    )
                )
            )
        ),

        // ===== 事件链：帮一位干部 → 被邀请参加重要工作 → 结果影响领导评价 =====
        ev(
            id = "pol_favor_start", title = "帮一个干部说话",
            background = "会上有人被追责",
            description = "赵春生在会上被点名批评，问题不全在他。轮到你发言时，" +
                "所有人都在等你怎么说。",
            tags = tags(EventTag.COLLEAGUE, EventTag.MEETING),
            weight = 0,
            chain = "favor", step = 1, after = 3,
            choices = listOf(
                c(
                    "替他把话说清楚", Attitude.BOLD,
                    "你讲了三条理由，把责任分清楚了。赵春生散会后在楼梯口等了你十分钟。",
                    AttrDelta(work = -2, colleagues = 2),
                    HiddenDelta(network = 5),
                    chain = "favor", flag = "favor_help",
                    political = PoliticalEffect(
                        relations = mapOf("zhao_chunsheng" to 10),
                        trust = mapOf("zhao_chunsheng" to 8),
                        factions = mapOf(Faction.LOCAL to 3)
                    )
                ),
                c(
                    "先问清楚情况再说", Attitude.STEADY,
                    "你说这事需要再核实一下。会后你找两个人问了问。",
                    AttrDelta(politics = 2),
                    chain = "favor", flag = "favor_check",
                    political = PoliticalEffect(
                        relations = mapOf("zhao_chunsheng" to 3),
                        trust = mapOf("zhao_chunsheng" to 2)
                    )
                ),
                c(
                    "不接这个话", Attitude.CAUTIOUS,
                    "你说按程序办。赵春生没有再看你。",
                    AttrDelta(colleagues = -2),
                    political = PoliticalEffect(
                        relations = mapOf("zhao_chunsheng" to -6),
                        risk = -3
                    )
                )
            )
        ),
        ev(
            id = "pol_favor_invite", title = "被拉进一个重要的事",
            background = "几个月后的电话",
            description = "赵春生打电话来，说他牵头的一件跨部门的事缺个写材料的人，" +
                "问你愿不愿意来。",
            tags = tags(EventTag.LEADER_TASK, EventTag.MATERIAL),
            weight = 0,
            chain = "favor", step = 2, after = 3,
            choices = listOf(
                c(
                    "接下来，做主力", Attitude.BOLD,
                    "你成了这个专班的笔杆子。连着两个月，你几乎没在正常时间回过家。",
                    AttrDelta(work = 5, ability = 4, energy = -4),
                    HiddenDelta(stress = 8, opportunity = 6),
                    chain = "favor", flag = "favor_take",
                    political = PoliticalEffect(
                        relations = mapOf("zhao_chunsheng" to 6),
                        trust = mapOf("zhao_chunsheng" to 4)
                    )
                ),
                c(
                    "只做配合，不挑头", Attitude.STEADY,
                    "你负责其中一块，按时交卷，不多揽事。",
                    AttrDelta(coordination = 3, colleagues = 3),
                    chain = "favor", flag = "favor_support"
                ),
                c(
                    "推辞掉", Attitude.CAUTIOUS,
                    "你说手头实在排不开。他哦了一声就挂了。",
                    AttrDelta(leaderTrust = -3, energy = 2),
                    political = PoliticalEffect(
                        relations = mapOf("zhao_chunsheng" to -8)
                    )
                )
            )
        ),
        ev(
            id = "pol_favor_result", title = "事情办成之后",
            background = "专班总结会",
            description = "事情办成了。总结会上要有人汇报，也有人要担之前出的纰漏。",
            tags = tags(EventTag.LEADER_TASK, EventTag.CAREER),
            weight = 0,
            chain = "favor", step = 3, after = 4,
            choices = listOf(
                c(
                    "站上去汇报", Attitude.BOLD,
                    "你讲了二十分钟，把过程、数据和问题都讲清楚了。会后领导专门留下了你。",
                    AttrDelta(leaderTrust = 6, reputation = 4),
                    HiddenDelta(opportunity = 8, socialInfluence = 6),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.LOCAL to 4, Faction.CORE to 3)
                    )
                ),
                c(
                    "先担下之前出的纰漏", Attitude.STEADY,
                    "你把两处失误差在自己身上。会后有人拍了下你的肩。",
                    AttrDelta(work = -3, politics = 4, leaderTrust = -2),
                    HiddenDelta(riskPressure = 4),
                    political = PoliticalEffect(
                        relations = mapOf("zhao_chunsheng" to 8, "lin_xiaoyun" to 6),
                        trust = mapOf("zhao_chunsheng" to 6),
                        risk = 4
                    )
                ),
                c(
                    "把功劳让给具体经办的人", Attitude.CAUTIOUS,
                    "你退到后排。上台的那个人，后来成了你最好的搭档。",
                    AttrDelta(colleagues = 6, reputation = -1),
                    HiddenDelta(network = 8),
                    political = PoliticalEffect(
                        trust = mapOf("zhao_chunsheng" to 4, "lin_xiaoyun" to 6),
                        factions = mapOf(Faction.NEUTRAL to 3)
                    )
                )
            )
        )
    )
}