package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.ConditionalEffect
import com.shitu.app.model.EventTag
import com.shitu.app.model.Faction
import com.shitu.app.model.GameEvent
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.PoliticalEffect
import com.shitu.app.model.PoliticalImpact
import com.shitu.app.model.PoliticalTendency

/**
 * 第九阶段：派系博弈、情报与日常协作的事件。
 *
 * 与普通事件共用一套机制，只是选项会影响政治资源、派系格局、
 * 情报与联盟。数值都是游戏模拟参数。
 */
object FactionEventLibrary {

    val events: List<GameEvent> = listOf(
        ev(
            id = "fac_intel_gossip", title = "一句闲话",
            background = "走廊尽头",
            description = "散会后有人凑过来，随口说了两句。你听得出来，这不是随便说说。",
            tags = tags(EventTag.CAREER, EventTag.COLLEAGUE),
            weight = 8, minPos = 1, repeatable = true, cooldown = 8,
            choices = listOf(
                c(
                    "认真听，把话记下来", Attitude.STEADY,
                    "你听出了话里的意思：有个位子可能要动。",
                    AttrDelta(ability = 1),
                    politics = PoliticalImpact(intelId = "intel_vacancy", tendencies = mapOf(PoliticalTendency.BALANCED to 1))
                ),
                c(
                    "顺着问一句上面最近关心什么", Attitude.BOLD,
                    "对方说漏了嘴：上面最近盯着经济指标。",
                    AttrDelta(coordination = 2),
                    politics = PoliticalImpact(intelId = "intel_leader_focus")
                ),
                c(
                    "笑笑走开，不接这个话", Attitude.CAUTIOUS,
                    "你没接话。少听一句，少一件事。",
                    AttrDelta(politics = 1, integrityRisk = -1),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 1))
                )
            )
        ),
        ev(
            id = "fac_alliance_offer", title = "有人想搭伙",
            background = "饭桌上",
            description = "有人提出：这件事两个人一起做，资源凑一凑，比单干强。",
            tags = tags(EventTag.COORDINATION, EventTag.CAREER),
            weight = 7, minPos = 1,
            choices = listOf(
                c(
                    "答应，先把资源凑起来", Attitude.BOLD,
                    "你们达成了一个短期的默契：利益共享，风险共担。",
                    AttrDelta(coordination = 3, colleagues = 2),
                    political = PoliticalEffect(factions = mapOf(Faction.PRAGMATIC to 5, Faction.CONSERVATIVE to -3)),
                    politics = PoliticalImpact(
                        startAlliance = "ally_pragmatic",
                        factionInfluence = mapOf(Faction.PRAGMATIC to 3),
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2)
                    ),
                    hint = "联盟会到期，也可能带来连带风险"
                ),
                c(
                    "先看对方能拿出什么", Attitude.CAUTIOUS,
                    "你把话往回收了收，让对方先出牌。",
                    AttrDelta(ability = 2, politics = 2),
                    politics = PoliticalImpact(capital = 2, tendencies = mapOf(PoliticalTendency.BALANCED to 2))
                ),
                c(
                    "不结盟，各干各的", Attitude.STEADY,
                    "你婉拒了。关系还在，但没有捆绑。",
                    AttrDelta(politics = 2, colleagues = -1),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 2))
                )
            )
        ),
        ev(
            id = "fac_rival_moves", title = "同层近况",
            background = "材料堆里",
            description = "你注意到同科室的几位同志最近都在加班补材料，办公室的灯常常亮到很晚。",
            tags = tags(EventTag.ASSESSMENT, EventTag.CAREER),
            weight = 8, minPos = 1, repeatable = true, cooldown = 10,
            choices = listOf(
                c(
                    "把手上工作做出亮点来回击", Attitude.BOLD,
                    "你没有去争，而是把事做出一个亮点。这比说什么都有用。",
                    AttrDelta(work = 3, ability = 2),
                    politics = PoliticalImpact(capital = 4, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3))
                ),
                c(
                    "侧面了解大家在忙什么", Attitude.STEADY,
                    "你侧面打听了一圈，大致摸清了最近工作的重点在哪里。",
                    AttrDelta(ability = 2, coordination = 2),
                    politics = PoliticalImpact(
                        intelId = "intel_peer_move",
                        tendencies = mapOf(PoliticalTendency.BALANCED to 2)
                    )
                ),
                c(
                    "向领导汇报自己这一段的成绩", Attitude.CAUTIOUS,
                    "你把近一段的成绩整理清楚递了上去，这一段的付出有了记录。",
                    AttrDelta(leaderTrust = 3, writing = 2),
                    politics = PoliticalImpact(spendCapital = 3, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2))
                )
            )
        ),
        ev(
            id = "fac_leader_probe", title = "领导问你站哪边",
            background = "办公室",
            description = "领导没有绕弯子：最近的一些事，你怎么看，站在哪一边。",
            tags = tags(EventTag.LEADER_TALK, EventTag.CAREER),
            weight = 8, minPos = 2,
            choices = listOf(
                c(
                    "说清楚自己的判断", Attitude.BOLD,
                    "你把立场摆了出来。领导听完没说话，但点了点头。",
                    AttrDelta(ability = 2, reputation = 2),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.REFORM to 4, Faction.CONSERVATIVE to -3),
                        risk = 3
                    ),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.REFORMIST to 3))
                ),
                c(
                    "说两边都有道理", Attitude.CAUTIOUS,
                    "你把话讲得很圆。领导笑了笑，说「你很稳」。",
                    AttrDelta(politics = 3),
                    political = PoliticalEffect(factions = mapOf(Faction.NEUTRAL to 4)),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.BALANCED to 3), capital = 3)
                ),
                c(
                    "把问题拉回工作本身", Attitude.STEADY,
                    "你说自己想先把事做成，别的没多想。领导没有再追。",
                    AttrDelta(work = 2, leaderTrust = 2),
                    political = PoliticalEffect(factions = mapOf(Faction.PRAGMATIC to 4)),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3))
                )
            )
        ),
        ev(
            id = "fac_mentor_intro", title = "老领导牵的线",
            background = "一次私人聚会",
            description = "退休的老领导把你介绍给几个人，说「这是我看着长起来的」。",
            tags = tags(EventTag.MEETING, EventTag.CAREER),
            weight = 6, minPos = 1,
            choices = listOf(
                c(
                    "接住这份人情", Attitude.STEADY,
                    "你敬了酒，认了这份人情。从此有人替你说话。",
                    AttrDelta(colleagues = 2, reputation = 2),
                    political = PoliticalEffect(
                        relations = mapOf("sun_dehai" to 6),
                        trust = mapOf("sun_dehai" to 6),
                        factions = mapOf(Faction.CONSERVATIVE to 5)
                    ),
                    politics = PoliticalImpact(startAlliance = "ally_mentor", capital = 3)
                ),
                c(
                    "把话题引到工作上", Attitude.BOLD,
                    "你把聊天引到了具体项目上，几位都对你有印象。",
                    AttrDelta(coordination = 3, ability = 2),
                    politics = PoliticalImpact(intelId = "intel_project", capital = 2)
                ),
                c(
                    "客气，但不攀附", Attitude.CAUTIOUS,
                    "你礼貌地应酬，没有多留。老领导看了你一眼，没说什么。",
                    AttrDelta(politics = 2, integrityRisk = -1),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 1))
                )
            )
        ),
        ev(
            id = "fac_network_chain", title = "关系链",
            background = "一个电话",
            description = "你托的人又托了别人。事情绕了一圈，最后从一个你没想到的方向有了回音。",
            tags = tags(EventTag.CAREER, EventTag.COORDINATION),
            weight = 7, minPos = 2, repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "沿着这条线继续维护", Attitude.STEADY,
                    "你把这层关系记在了心里，也回了一份人情。",
                    AttrDelta(colleagues = 3, coordination = 2),
                    HiddenDelta(network = 6),
                    politics = PoliticalImpact(capital = 4, tendencies = mapOf(PoliticalTendency.BALANCED to 2))
                ),
                c(
                    "借这个机会打听关键信息", Attitude.BOLD,
                    "你顺势问到了那件旧事的一些来龙去脉。",
                    AttrDelta(ability = 2),
                    politics = PoliticalImpact(intelId = "intel_hidden_issue")
                ),
                c(
                    "适可而止，不欠太多人情", Attitude.CAUTIOUS,
                    "你把事情收住了，人情没欠太深。",
                    AttrDelta(politics = 2, integrityRisk = -2),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 2))
                )
            )
        ),
        ev(
            id = "fac_power_shift", title = "格局在松动",
            background = "一段时间以来的观察",
            description = "你察觉到，本地几股力量的分量正在悄悄变化。",
            tags = tags(EventTag.CAREER, EventTag.MEETING),
            weight = 6, minPos = 3, repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "顺势靠向正在上升的一方", Attitude.BOLD,
                    "你把资源往上升的一方压了一点。",
                    AttrDelta(leaderTrust = 2),
                    political = PoliticalEffect(factions = mapOf(Faction.REFORM to 5, Faction.CONSERVATIVE to -3), risk = 3),
                    politics = PoliticalImpact(
                        factionInfluence = mapOf(Faction.REFORM to 5, Faction.CONSERVATIVE to -4),
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 2)
                    )
                ),
                c(
                    "谁都不靠，闷头做事", Attitude.STEADY,
                    "你谁的账都没买，把精力放回了工作。",
                    AttrDelta(work = 3),
                    politics = PoliticalImpact(capital = 4, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3))
                ),
                c(
                    "把水搅得再清一点", Attitude.CAUTIOUS,
                    "你暗中帮了较弱的一方一把，格局没定得太死。",
                    AttrDelta(coordination = 3, politics = 1),
                    political = PoliticalEffect(factions = mapOf(Faction.NEUTRAL to 3)),
                    politics = PoliticalImpact(capital = 2, tendencies = mapOf(PoliticalTendency.BALANCED to 3))
                )
            )
        ),
        ev(
            id = "fac_rally_support", title = "盟友助阵",
            background = "关键的一次会",
            description = "在要紧的场合，你的盟友替你说了话。",
            tags = tags(EventTag.COORDINATION, EventTag.CAREER),
            weight = 6, minPos = 2, repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "接住支持，把事往前推", Attitude.BOLD,
                    "有了这句话，事情顺畅了很多。",
                    AttrDelta(leaderTrust = 3, colleagues = 2),
                    politics = PoliticalImpact(capital = 4, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2))
                ),
                c(
                    "把功劳记在盟友身上", Attitude.CAUTIOUS,
                    "你在会上把功劳分了出去，关系更近了一层。",
                    AttrDelta(colleagues = 4, reputation = -1),
                    politics = PoliticalImpact(capital = 3, tendencies = mapOf(PoliticalTendency.BALANCED to 3))
                ),
                c(
                    "保持距离，别显得太绑定", Attitude.STEADY,
                    "你礼貌地谢过，但没有表现得太近。",
                    AttrDelta(politics = 2, integrityRisk = -1),
                    political = PoliticalEffect(factions = mapOf(Faction.NEUTRAL to 2)),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 2))
                )
            )
        ),
        ev(
            id = "fac_quiet_observer", title = "有人在看着你",
            background = "汇报结束之后",
            description = "你注意到，最近有人在不动声色地观察你的做法与态度。",
            tags = tags(EventTag.CAREER, EventTag.INSPECTION),
            weight = 6, minPos = 1, repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "照常做事，不刻意表现", Attitude.STEADY,
                    "你没改变节奏。观察你的人最终只看到你在做事。",
                    AttrDelta(work = 2, integrityRisk = -2),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2))
                ),
                c(
                    "把成绩整理得漂亮一点", Attitude.CAUTIOUS,
                    "你花了几个晚上把材料做得无可挑剔。",
                    AttrDelta(writing = 3, leaderTrust = 2),
                    politics = PoliticalImpact(spendCapital = 2, tendencies = mapOf(PoliticalTendency.PRAGMATIC to 1), capital = 2)
                ),
                c(
                    "主动汇报自己的想法", Attitude.BOLD,
                    "你把思路完整地讲了一遍，对方听得很认真。",
                    AttrDelta(ability = 2, leaderTrust = 3),
                    politics = PoliticalImpact(intelId = "intel_leader_focus", capital = 2)
                )
            )
        ),
        ev(
            id = "fac_crisis_response", title = "危机应对",
            background = "事情闹大了",
            description = "一场政治危机摆在面前：处理不好会伤筋动骨，处理好了反而是转机。",
            tags = tags(EventTag.CAREER, EventTag.PUBLIC_OPINION),
            weight = 0, minPos = 0, priority = true, crisisOnly = true,
            choices = listOf(
                c(
                    "坦诚说明，主动担责", Attitude.STEADY,
                    "你把责任担了下来，也把整改的步骤说清楚了。风波慢慢平息。",
                    AttrDelta(politics = 3, reputation = 2),
                    political = PoliticalEffect(risk = -8),
                    politics = PoliticalImpact(
                        resolveCrisis = true,
                        tendencies = mapOf(PoliticalTendency.STEADY to 3),
                        history = "在政治危机中主动担责"
                    )
                ),
                c(
                    "动用资源，先稳住局面", Attitude.BOLD,
                    "你把积累的政治资源用了出去，局面稳住了。",
                    AttrDelta(leaderTrust = 3, colleagues = 2),
                    politics = PoliticalImpact(
                        spendCapital = 8,
                        resolveCrisis = true,
                        tendencies = mapOf(PoliticalTendency.BALANCED to 3),
                        history = "用政治资源化解了一场危机"
                    )
                ),
                c(
                    "反击，把水搅浑", Attitude.RISKY,
                    "你把矛头引向了别处。危机暂时过去，但留下了新的隐患。",
                    AttrDelta(integrityRisk = 5, reputation = 2),
                    political = PoliticalEffect(risk = 6),
                    politics = PoliticalImpact(
                        resolveCrisis = true,
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 3),
                        history = "用强硬手段渡过了一场危机"
                    )
                )
            )
        ),
        ev(
            id = "fac_reform_aftermath", title = "试点之后",
            background = "一年以后",
            description = "当初那项试点到了看结果的时候。有人等着看笑话，" +
                "也有人希望你把它做成可以推广的经验。",
            tags = tags(EventTag.CAREER, EventTag.INSPECTION),
            weight = 6, minPos = 2,
            choices = listOf(
                c(
                    "如实汇报，问题不藏着", Attitude.STEADY,
                    "你把成绩和问题一起摆了出来，态度反而得到认可。",
                    AttrDelta(leaderTrust = 3, reputation = 2, writing = 2),
                    politics = PoliticalImpact(
                        capital = 3,
                        tendencies = mapOf(PoliticalTendency.STEADY to 2)
                    ),
                    bonus = listOf(
                        ConditionalEffect(
                            requiredFlag = "major_maj_reform_pilot",
                            effects = AttrDelta(leaderTrust = 3, work = 2),
                            extraOutcome = "你是这项试点的发起人，汇报时更有分量，当初的坚持开始有回报。"
                        )
                    )
                ),
                c(
                    "把经验包装成一套做法推广", Attitude.BOLD,
                    "你把试点总结成了一套可复制的做法，会上反响不错。",
                    AttrDelta(writing = 4, work = 2, leaderTrust = 2),
                    political = PoliticalEffect(factions = mapOf(Faction.REFORM to 5, Faction.CONSERVATIVE to -3)),
                    politics = PoliticalImpact(
                        capital = 4,
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 2),
                        factionInfluence = mapOf(Faction.REFORM to 3)
                    ),
                    bonus = listOf(
                        ConditionalEffect(
                            requiredFlag = "major_maj_reform_pilot",
                            effects = AttrDelta(work = 3, reputation = 2),
                            extraOutcome = "因为试点是你力推的，这套经验天然带着你的印记。"
                        )
                    )
                ),
                c(
                    "主动降温，把话题引开", Attitude.CAUTIOUS,
                    "你没有借机放大声量，试点平稳收尾，没有激起更大争议。",
                    AttrDelta(politics = 2, integrityRisk = -2),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.BALANCED to 2))
                )
            )
        ),
        ev(
            id = "fac_project_aftermath", title = "项目余波",
            background = "工地边上",
            description = "之前争议过的那个项目，如今又出了新的情况，" +
                "当初投过票的人都被重新提起。",
            tags = tags(EventTag.PROJECT, EventTag.CAREER),
            weight = 6, minPos = 2,
            choices = listOf(
                c(
                    "把项目拉回正轨，先解决问题", Attitude.BOLD,
                    "你没有回避，直接去现场把问题一项项过了一遍。",
                    AttrDelta(work = 3, coordination = 2),
                    politics = PoliticalImpact(
                        capital = 3,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2)
                    ),
                    bonus = listOf(
                        ConditionalEffect(
                            requiredFlag = "major_maj_project_dispute",
                            effects = AttrDelta(leaderTrust = 3, reputation = 2),
                            extraOutcome = "这个项目当初就是你顶着质疑推下去的，现在你把它救了回来。"
                        ),
                        ConditionalEffect(
                            requiredFlag = "major_maj_land_use",
                            effects = AttrDelta(ability = 2, colleagues = 2),
                            extraOutcome = "这块地当初怎么用是你拍的板，如今你需要把话圆回来。"
                        )
                    )
                ),
                c(
                    "请上级出面协调", Attitude.STEADY,
                    "你把难题往上交，上级出面把各方叫到了一起。",
                    AttrDelta(leaderTrust = 2, politics = 2),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.BALANCED to 2))
                ),
                c(
                    "先按合同办，把责任划清", Attitude.CAUTIOUS,
                    "你先守住程序，把责任边界划清了再谈下一步。",
                    AttrDelta(writing = 3, integrityRisk = -2),
                    political = PoliticalEffect(risk = -3),
                    politics = PoliticalImpact(tendencies = mapOf(PoliticalTendency.STEADY to 2))
                )
            )
        )
    )
}
