package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.EventTag
import com.shitu.app.model.Faction
import com.shitu.app.model.GameEvent
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.Indicator
import com.shitu.app.model.IndicatorGate
import com.shitu.app.model.GovernanceGate
import com.shitu.app.model.PoliticalEffect
import com.shitu.app.model.PoliticalGate
import com.shitu.app.model.PoliticalImpact
import com.shitu.app.model.PoliticalTendency

/**
 * 第十阶段：重大人生节点。
 *
 * 这些事件不随机堆积，而是读取职位、家庭、风险、派系与治理状态后才会出现。
 * 每个节点都会写进履历，并影响人物记忆与结局判定。
 * 数值都是游戏模拟参数。
 */
object LifeEventLibrary {

    val events: List<GameEvent> = listOf(
        ev(
            id = "life_first_step", title = "第一次独立挑担",
            background = "任务分工会",
            description = "领导把一件原来不归你管的事交到你手上，说得很客气：" +
                "「先试试，出了事我担着。」你听得出，这话不能全信。",
            tags = tags(EventTag.LEADER_TASK, EventTag.CAREER),
            weight = 9, minPos = 1, major = true,
            choices = listOf(
                c(
                    "接下来，按自己的想法做", Attitude.BOLD,
                    "你把担子接了下来，按自己的思路推进，过程磕磕绊绊，但事情成了。",
                    AttrDelta(work = 4, ability = 3, leaderTrust = 3, energy = -4),
                    HiddenDelta(riskPressure = 4, opportunity = 4)
                ),
                c(
                    "接下来，但先请示清楚边界", Attitude.STEADY,
                    "你把职责边界一条条问清楚，事情做得慢，但没有出岔子。",
                    AttrDelta(work = 2, ability = 2, politics = 2, leaderTrust = 2)
                ),
                c(
                    "先推给别人，等更有把握再上", Attitude.CAUTIOUS,
                    "你把机会让了出去。安全，但领导记住了你的犹豫。",
                    AttrDelta(leaderTrust = -3, ability = 1, politics = 1)
                )
            )
        ),
        ev(
            id = "life_first_mistake", title = "第一次重大失误",
            background = "深夜办公室",
            description = "一份你经手的材料出了错，已经报到上面去了。问题是，" +
                "现在承认，还是想办法圆过去。",
            tags = tags(EventTag.MATERIAL, EventTag.CAREER),
            weight = 10, minPos = 1, minRisk = 20, major = true,
            choices = listOf(
                c(
                    "第一时间上报，说明情况", Attitude.STEADY,
                    "你主动把问题摆到了桌面上。挨了批评，但保住了信任。",
                    AttrDelta(leaderTrust = -3, politics = 3, ability = 3, integrityRisk = -4),
                    HiddenDelta(riskPressure = -3)
                ),
                c(
                    "自己补救，尽量不惊动上面", Attitude.BOLD,
                    "你连夜把材料改了回来，事情压住了，但心里一直悬着。",
                    AttrDelta(work = 3, ability = 2, energy = -5, integrityRisk = 3),
                    HiddenDelta(riskPressure = 6, stress = 5)
                ),
                c(
                    "把责任推给流程和别的部门", Attitude.RISKY,
                    "你没有认下这件事。风头过去了，但有人记住了你。",
                    AttrDelta(politics = 3, integrityRisk = 8, reputation = -3, colleagues = -5),
                    HiddenDelta(riskPressure = 8)
                )
            )
        ),
        ev(
            id = "life_big_project", title = "第一次主持大项目",
            background = "项目指挥部",
            description = "一个投资不小的项目落到你头上。做好了是履历，做砸了是事故。" +
                "工期、资金、群众工作，哪一样都不轻松。",
            tags = tags(EventTag.PROJECT, EventTag.LEADER_TASK),
            weight = 9, minPos = 2, major = true,
            choices = listOf(
                c(
                    "挂图作战，亲自盯在一线", Attitude.BOLD,
                    "你把铺盖搬到了指挥部，项目按期推进，人也瘦了一圈。",
                    AttrDelta(work = 5, ability = 4, leaderTrust = 4, health = -4, energy = -6),
                    governance = com.shitu.app.model.GovernanceEffect(
                        indicators = mapOf(Indicator.INFRASTRUCTURE to 5, Indicator.ECONOMY to 3),
                        merit = mapOf(
                            com.shitu.app.model.MeritKind.ECONOMIC to 4,
                            com.shitu.app.model.MeritKind.INNOVATION to 3
                        )
                    )
                ),
                c(
                    "搭好班子，抓关键节点", Attitude.STEADY,
                    "你选了几个人各管一摊，只抓几个关键节点，项目稳步推进。",
                    AttrDelta(work = 3, coordination = 4, leaderTrust = 3),
                    governance = com.shitu.app.model.GovernanceEffect(
                        indicators = mapOf(Indicator.INFRASTRUCTURE to 3),
                        merit = mapOf(com.shitu.app.model.MeritKind.FINANCE to 3)
                    )
                ),
                c(
                    "把项目转给更有经验的部门牵头", Attitude.CAUTIOUS,
                    "你把项目让了出去，落得清闲，也少了一段可以拿出来的履历。",
                    AttrDelta(politics = 2, ability = -1, leaderTrust = -2)
                )
            )
        ),
        ev(
            id = "life_faction_squeeze", title = "第一次面对派系压力",
            background = "会后的小范围",
            description = "有人把你叫到一边，话说得很重：「这件事，你得有个态度。」" +
                "你知道，表态容易，但站上去就下不来了。",
            tags = tags(EventTag.CAREER, EventTag.COORDINATION),
            weight = 9, minPos = 2,
            gate = PoliticalGate(minFaction = mapOf(Faction.PRAGMATIC to 20)),
            major = true,
            choices = listOf(
                c(
                    "明确表态，靠过去", Attitude.BOLD,
                    "你给了对方一个明确的答复。关系一下子近了很多，也跟着背上了立场。",
                    AttrDelta(leaderTrust = 3, colleagues = 3, integrityRisk = 4),
                    political = PoliticalEffect(factions = mapOf(Faction.PRAGMATIC to 8, Faction.CONSERVATIVE to -5)),
                    politics = PoliticalImpact(
                        capital = 4,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3),
                        factionInfluence = mapOf(Faction.PRAGMATIC to 4)
                    )
                ),
                c(
                    "不表态，但把事情办好", Attitude.STEADY,
                    "你没有说站哪边，只是把该做的事做得无可挑剔。两边都不满意，也都没话说。",
                    AttrDelta(work = 3, politics = 3, ability = 2),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.BALANCED to 3))
                ),
                c(
                    "当面顶回去", Attitude.RISKY,
                    "你拒绝了。对方脸上挂不住，你也把自己放到了风口上。",
                    AttrDelta(politics = 4, reputation = 2, colleagues = -6),
                    political = PoliticalEffect(risk = 6),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.REFORMIST to 3))
                )
            )
        ),
        ev(
            id = "life_key_contest", title = "第一次重要任职",
            background = "组织谈话前",
            description = "一个你早就想去的岗位空了出来，组织上会听听符合条件的同志怎么想。" +
                "有人劝你多去汇报汇报，也有人劝你别太露锋芒。",
            tags = tags(EventTag.ASSESSMENT, EventTag.CAREER),
            weight = 9, minPos = 3, major = true,
            choices = listOf(
                c(
                    "主动向组织汇报自己的想法", Attitude.BOLD,
                    "你把这些年的工作系统地讲了一遍，态度诚恳，也表明了自己的意愿。",
                    AttrDelta(leaderTrust = 4, reputation = 3, politics = 3),
                    politics = PoliticalImpact(
                        capital = 5,
                        history = "在第一次重要任职中主动表达意愿"
                    )
                ),
                c(
                    "把材料准备好，等组织考察", Attitude.STEADY,
                    "你没有四处活动，只把该准备的准备扎实。",
                    AttrDelta(work = 3, writing = 3, leaderTrust = 2),
                    politics = PoliticalImpact(capital = 3, tendencies = mapOf(PoliticalTendency.STEADY to 2))
                ),
                c(
                    "先放一放，等下一轮再说", Attitude.CAUTIOUS,
                    "你把想法收了收。有人承你的情，也有人觉得你不够主动。",
                    AttrDelta(colleagues = 4, reputation = -2, leaderTrust = -2),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.BALANCED to 3),
                        history = "主动放慢了一次任职节奏"
                    )
                )
            )
        ),
        ev(
            id = "life_remote_post", title = "第一次异地调任",
            background = "电话通知",
            description = "组织上考虑让你去外地任职。机会是真的，代价也是真的：" +
                "离家更远，人生地不熟，家属的工作和孩子的学业都要重新安排。",
            tags = tags(EventTag.CAREER, EventTag.FAMILY),
            weight = 8, minPos = 4, major = true,
            choices = listOf(
                c(
                    "服从安排，去", Attitude.BOLD,
                    "你收拾好行李去了新地方。履历上多了一段重要经历，家里少了一个人。",
                    AttrDelta(ability = 4, work = 3, leaderTrust = 4, family = -8, energy = -4),
                    politics = PoliticalImpact(
                        capital = 5,
                        history = "接受异地调任，从头打拼"
                    )
                ),
                c(
                    "去，但把家里安排妥当再走", Attitude.STEADY,
                    "你先花了两个月安顿家里，才安心赴任。起步慢一点，但没有后顾之忧。",
                    AttrDelta(ability = 3, work = 2, family = -2, coordination = 3),
                    politics = PoliticalImpact(capital = 3, tendencies = mapOf(PoliticalTendency.STEADY to 2))
                ),
                c(
                    "以家庭原因婉拒", Attitude.CAUTIOUS,
                    "你把机会推了。家里松了口气，组织上也没有再说什么。",
                    AttrDelta(family = 5, leaderTrust = -4, reputation = -2)
                )
            )
        ),
        ev(
            id = "life_leader_praise", title = "上级的高度评价",
            background = "年度总结会上",
            description = "主要领导在会上点了你的名，评价很高。散会后，" +
                "有人来道贺，也有人开始和你保持距离。",
            tags = tags(EventTag.LEADER_TALK, EventTag.CAREER),
            weight = 8, minPos = 3,
            gate = PoliticalGate(minFaction = mapOf(Faction.CORE to 15)),
            choices = listOf(
                c(
                    "把功劳分给团队", Attitude.STEADY,
                    "你在会上把成绩归给了大家，团队更服你，领导也觉得你稳。",
                    AttrDelta(colleagues = 5, leaderTrust = 3, reputation = 2)
                ),
                c(
                    "趁热打铁，提出更大的想法", Attitude.BOLD,
                    "你借这个机会提出了一个更大胆的设想，领导让你先拿方案。",
                    AttrDelta(work = 3, ability = 3, leaderTrust = 3, integrityRisk = 2),
                    politics = PoliticalImpact(capital = 4, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3))
                ),
                c(
                    "低调处理，主动降温", Attitude.CAUTIOUS,
                    "你刻意压了压风头，少了很多应酬，也少了一些机会。",
                    AttrDelta(politics = 3, integrityRisk = -3, reputation = -1),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 2))
                )
            )
        ),
        ev(
            id = "life_family_choice", title = "家里的一件大事",
            background = "周末家里",
            description = "家里有一件需要你拿主意的事：父母身体、孩子升学或者伴侣的工作。" +
                "偏偏这段时间单位也正忙。",
            tags = tags(EventTag.FAMILY, EventTag.HEALTH),
            weight = 9, maxFamily = 78, major = true,
            choices = listOf(
                c(
                    "请假把家里的事办好", Attitude.STEADY,
                    "你把手头的事往后放了放。单位有点意见，家里却记住了你。",
                    AttrDelta(family = 8, health = 2, leaderTrust = -2, energy = 2),
                    HiddenDelta(stress = -6)
                ),
                c(
                    "工作家庭两头跑", Attitude.BOLD,
                    "你白天在单位，晚上回家，把自己拆成了两半。",
                    AttrDelta(family = 3, work = 2, health = -3, energy = -5),
                    HiddenDelta(stress = 5)
                ),
                c(
                    "先顾工作，家里以后再补", Attitude.CAUTIOUS,
                    "你选择了工作。家里没有再说什么，但气氛变了。",
                    AttrDelta(family = -8, work = 3, leaderTrust = 2),
                    HiddenDelta(stress = 3)
                )
            )
        ),
        ev(
            id = "life_work_family", title = "事业与家庭的冲突",
            background = "深夜的电话",
            description = "家里出了点状况，需要你回去。可你手上正压着一件走不开的事。",
            tags = tags(EventTag.FAMILY, EventTag.CAREER),
            weight = 9, minPos = 3, maxFamily = 62, major = true,
            choices = listOf(
                c(
                    "放下工作，先回家", Attitude.STEADY,
                    "你把事情交给同事，赶了回去。工作受了点影响，家庭保住了。",
                    AttrDelta(family = 7, leaderTrust = -2, colleagues = -2, health = 2),
                    HiddenDelta(stress = -6)
                ),
                c(
                    "两边都扛下来", Attitude.BOLD,
                    "你白天处理工作，晚上守在家里，硬是都扛了下来。",
                    AttrDelta(family = 4, work = 3, health = -4, energy = -6),
                    HiddenDelta(stress = 4)
                ),
                c(
                    "让家人先自己处理", Attitude.CAUTIOUS,
                    "你把家里的事交给了家人。工作没耽误，心里的账却记上了。",
                    AttrDelta(family = -9, work = 3),
                    HiddenDelta(stress = 4, riskPressure = 2)
                )
            )
        ),
        ev(
            id = "life_frontline", title = "还要不要继续待在一线",
            background = "人事酝酿时",
            description = "有人建议你调去机关，说那里更适合往上走；也有人说，" +
                "离开一线，你这辈子最值钱的那点东西就没了。",
            tags = tags(EventTag.CAREER),
            weight = 8, minPos = 4, major = true,
            choices = listOf(
                c(
                    "回机关，为更高的位置做准备", Attitude.STEADY,
                    "你调去了机关。视野更宽了，离群众却远了一些。",
                    AttrDelta(writing = 3, coordination = 3, leaderTrust = 3, masses = -5),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.BALANCED to 2))
                ),
                c(
                    "留在一线，继续把事做实", Attitude.BOLD,
                    "你留了下来。群众认你，考核也拿得出手，只是上升的路更窄。",
                    AttrDelta(masses = 6, work = 4, reputation = 3, leaderTrust = -1),
                    politics = PoliticalImpact(
                        capital = 3,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3),
                        history = "在关键阶段选择留在一线"
                    )
                ),
                c(
                    "申请去最难的地方", Attitude.RISKY,
                    "你主动请缨去了矛盾最集中的地方。风险很大，机会也在这里。",
                    AttrDelta(work = 5, ability = 4, masses = 4, health = -4, energy = -5),
                    HiddenDelta(riskPressure = 6),
                    politics = PoliticalImpact(
                        capital = 5,
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 3),
                        history = "主动申请到最困难的岗位"
                    )
                )
            )
        ),
        ev(
            id = "life_professional_choice", title = "要不要走专业路线",
            background = "一次进修机会",
            description = "有一个系统内的专业培训名额，学出来就是行家，但意味着" +
                "要离开综合岗位几年。",
            tags = tags(EventTag.RESEARCH, EventTag.CAREER),
            weight = 8, minPos = 3, major = true,
            choices = listOf(
                c(
                    "去，把专业做深", Attitude.STEADY,
                    "你去进修了一段时间。专业上了一个台阶，综合事务生疏了些。",
                    AttrDelta(ability = 4, writing = 2, work = -1, reputation = 2),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2))
                ),
                c(
                    "不去了，综合岗位更接近决策", Attitude.BOLD,
                    "你留在了综合岗位上。离决策近，离专业远。",
                    AttrDelta(coordination = 3, politics = 3, ability = 1)
                ),
                c(
                    "白天工作，晚上自学", Attitude.CAUTIOUS,
                    "你两头都不想放，硬是靠业余时间把课学完了。",
                    AttrDelta(ability = 3, writing = 2, health = -2, energy = -4),
                    HiddenDelta(stress = 3)
                )
            )
        ),
        ev(
            id = "life_higher_level", title = "要不要争取更高层级",
            background = "组织部门约谈",
            description = "组织上问你，有没有到更高层级去闯一闯的打算。" +
                "上去一步是更大的舞台，也可能是更难的局面。",
            tags = tags(EventTag.CAREER, EventTag.ASSESSMENT),
            weight = 8, minPos = 5, major = true,
            choices = listOf(
                c(
                    "明确表示愿意挑更重的担子", Attitude.BOLD,
                    "你把态度表明了。组织上开始把你放进更大的盘子考虑。",
                    AttrDelta(leaderTrust = 4, reputation = 3, ability = 3, energy = -4),
                    politics = PoliticalImpact(
                        capital = 5,
                        history = "明确表达承担更大责任的意愿"
                    )
                ),
                c(
                    "服从组织安排，不主动表态", Attitude.STEADY,
                    "你没有多说，表示服从安排。",
                    AttrDelta(politics = 3, leaderTrust = 2)
                ),
                c(
                    "觉得自己更适合现在的岗位", Attitude.CAUTIOUS,
                    "你婉拒了。组织上表示理解，机会留给了更想去的人。",
                    AttrDelta(reputation = -3, leaderTrust = -2, family = 3),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 3))
                )
            )
        ),
        ev(
            id = "life_give_up", title = "一次重要的取舍",
            background = "机会与代价",
            description = "这次机会很好，但接过来意味着更重的担子和更少的个人时间。" +
                "是全力去接，还是留在现在的位置上。",
            tags = tags(EventTag.ASSESSMENT, EventTag.CAREER),
            weight = 8, minPos = 4, major = true,
            choices = listOf(
                c(
                    "全力接下，不留遗憾", Attitude.BOLD,
                    "你把能用的资源都用上了，尽力去接这次机会。",
                    AttrDelta(work = 3, reputation = 2, energy = -5, colleagues = -3),
                    political = PoliticalEffect(risk = 4),
                    politics = PoliticalImpact(
                        spendCapital = 4,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3),
                        history = "全力承接了一次重要机会"
                    )
                ),
                c(
                    "量力而行，先稳住当下", Attitude.STEADY,
                    "你没有硬接，把精力留给了手上这摊事。",
                    AttrDelta(colleagues = 5, reputation = 2, leaderTrust = 1),
                    politics = PoliticalImpact(
                        capital = 3,
                        tendencies = mapOf(PoliticalTendency.BALANCED to 2),
                        history = "量力而行，把精力留给当下"
                    )
                ),
                c(
                    "提出折中方案，分步承接", Attitude.CAUTIOUS,
                    "你提出把任务分步承接，给自己留出了余地。",
                    AttrDelta(coordination = 4, politics = 3, colleagues = 3)
                )
            )
        ),
        ev(
            id = "life_mentor_legacy", title = "老领导的托付",
            background = "一次探望",
            description = "老领导退下来了，说话反而更直：" +
                "「有件事我一直没跟你讲透，现在讲给你听。」",
            tags = tags(EventTag.LEADER_TALK, EventTag.CAREER),
            weight = 7, minPos = 3,
            gate = PoliticalGate(minNpcRelation = mapOf("sun_dehai" to 40)),
            choices = listOf(
                c(
                    "认真听，把话记在心里", Attitude.STEADY,
                    "老领导把多年的人情账和雷区都讲了一遍，你少走了很多弯路。",
                    AttrDelta(politics = 4, coordination = 3, leaderTrust = 3),
                    politics = PoliticalImpact(
                        capital = 4,
                        intelId = "intel_mentor_hint",
                        tendencies = mapOf(PoliticalTendency.STEADY to 2)
                    )
                ),
                c(
                    "把老领导的人脉接过来", Attitude.BOLD,
                    "你顺势接下了他留下的一些关系，路好走了，也背上了人情。",
                    AttrDelta(colleagues = 4, reputation = 3, integrityRisk = 3),
                    political = PoliticalEffect(factions = mapOf(Faction.CONSERVATIVE to 6)),
                    politics = PoliticalImpact(
                        capital = 4,
                        startAlliance = "ally_mentor",
                        factionInfluence = mapOf(Faction.CONSERVATIVE to 3)
                    )
                ),
                c(
                    "只听，不表态，也不接手", Attitude.CAUTIOUS,
                    "你听完了，但没有接任何东西。干净，也孤单。",
                    AttrDelta(politics = 3, integrityRisk = -3, colleagues = -1)
                )
            )
        ),
        ev(
            id = "life_final_direction", title = "最后一程的方向",
            background = "班子调整前",
            description = "到了这个阶段，路基本能看清了。是再拼一次，" +
                "还是把剩下的时间用在最想做的事上。",
            tags = tags(EventTag.CAREER),
            weight = 8, minPos = 6, major = true,
            choices = listOf(
                c(
                    "再拼一次，冲更高的位置", Attitude.BOLD,
                    "你把剩下的精力压了上去，成与不成，都写在了这一段履历里。",
                    AttrDelta(work = 4, ability = 3, leaderTrust = 4, health = -4, family = -4, energy = -5),
                    politics = PoliticalImpact(
                        capital = 4,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3),
                        history = "在职业生涯后段再次全力一搏"
                    )
                ),
                c(
                    "稳下来，把手上的事交清楚", Attitude.STEADY,
                    "你不再折腾，把队伍和摊子一点点交出去。",
                    AttrDelta(politics = 4, colleagues = 3, health = 3, family = 3),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 3))
                ),
                c(
                    "回头补上家庭和身体欠下的账", Attitude.CAUTIOUS,
                    "你把节奏慢了下来，开始认真陪家人、看医生。",
                    AttrDelta(family = 8, health = 6, work = -3, energy = 4),
                    HiddenDelta(stress = -8)
                )
            )
        ),
        ev(
            id = "life_health_warning", title = "身体发出的信号",
            background = "体检报告",
            description = "体检报告上有几个数字不太好看。医生说得很客气：" +
                "「注意休息，别太拼。」",
            tags = tags(EventTag.HEALTH, EventTag.CAREER),
            weight = 10, minStress = 55, major = true,
            choices = listOf(
                c(
                    "调整节奏，该放的放一放", Attitude.STEADY,
                    "你把一些不紧要的事分了出去，作息规律了不少。",
                    AttrDelta(health = 6, energy = 6, work = -2, family = 3),
                    HiddenDelta(stress = -10, healthRisk = -6)
                ),
                c(
                    "开点药顶着，工作不能停", Attitude.BOLD,
                    "你带着药继续上班，项目没有停，身体也没有真正好起来。",
                    AttrDelta(health = -4, work = 3, energy = -3),
                    HiddenDelta(stress = 3, healthRisk = 6)
                ),
                c(
                    "请一段长假，彻底休整", Attitude.CAUTIOUS,
                    "你破天荒请了长假。回来后，位置还在，节奏慢了。",
                    AttrDelta(health = 9, energy = 9, family = 5, leaderTrust = -3, reputation = -2),
                    HiddenDelta(stress = -14, healthRisk = -10)
                )
            )
        ),
        ev(
            id = "life_second_spring", title = "重新被想起",
            background = "一次临时会议",
            description = "一桩难啃的事，几个年轻干部都没能推动。" +
                "有人提了一句：「要不，让那个熟情况的人来试试？」",
            tags = tags(EventTag.LEADER_TASK, EventTag.CAREER),
            weight = 8, minPos = 3,
            gate = PoliticalGate(maxRisk = 45),
            governanceGate = GovernanceGate(
                indicators = listOf(IndicatorGate(Indicator.APPROVAL, min = 50))
            ),
            choices = listOf(
                c(
                    "接过来，把难事做成", Attitude.BOLD,
                    "你把这块硬骨头啃了下来，重新回到了大家的视野里。",
                    AttrDelta(work = 4, reputation = 4, leaderTrust = 3, energy = -5),
                    governance = com.shitu.app.model.GovernanceEffect(
                        indicators = mapOf(Indicator.SECURITY to 4, Indicator.LIVELIHOOD to 3),
                        merit = mapOf(
                            com.shitu.app.model.MeritKind.LIVELIHOOD to 4,
                            com.shitu.app.model.MeritKind.SECURITY to 3
                        )
                    ),
                    politics = PoliticalImpact(capital = 4, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2))
                ),
                c(
                    "帮着出主意，让年轻人去干", Attitude.STEADY,
                    "你把经验讲给他们听，自己退到后面。事情成了，功劳是大家的。",
                    AttrDelta(coordination = 4, colleagues = 4, reputation = 2)
                ),
                c(
                    "只做顾问，不担责任", Attitude.CAUTIOUS,
                    "你只提供了建议，没有接手。安全，也再没有被想起。",
                    AttrDelta(politics = 2, work = -1)
                )
            )
        )
    )
}
