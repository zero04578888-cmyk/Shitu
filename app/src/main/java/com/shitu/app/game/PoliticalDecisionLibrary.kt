package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.EventTag
import com.shitu.app.model.Faction
import com.shitu.app.model.GameEvent
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.PoliticalEffect
import com.shitu.app.model.PoliticalImpact
import com.shitu.app.model.PoliticalTendency

/**
 * 第九阶段：重大政治抉择。
 *
 * 这些事件没有「明显正确」的选项，短期收益与长期代价并存；
 * 每个选择都会写入履历，并通过 flag 影响之后的事件。
 * 数值都是游戏模拟参数。
 */
object PoliticalDecisionLibrary {

    val events: List<GameEvent> = listOf(
        ev(
            id = "maj_reform_pilot", title = "一项有争议的试点",
            background = "上级交办",
            description = "上级要你在辖区推一项新做法。做成了是经验，做砸了是教训，" +
                "而且一定有人希望你做砸。",
            tags = tags(EventTag.LEADER_TASK, EventTag.CAREER),
            weight = 8, minPos = 1, major = true,
            choices = listOf(
                c(
                    "坚决执行，先把声势做起来", Attitude.BOLD,
                    "你把试点推了下去，动静很大。支持的人很振奋，反对的人记下了你。",
                    AttrDelta(leaderTrust = 5, work = 3, integrityRisk = 3),
                    HiddenDelta(opportunity = 6, riskPressure = 6),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.REFORM to 6, Faction.CONSERVATIVE to -4)
                    ),
                    politics = PoliticalImpact(
                        capital = 6,
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 3),
                        factionInfluence = mapOf(Faction.REFORM to 4, Faction.CONSERVATIVE to -2),
                        history = "力推一项有争议的改革试点"
                    ),
                    hint = "改革派会记住你"
                ),
                c(
                    "提出修改意见，边试边改", Attitude.STEADY,
                    "你接了下来，但把节奏放慢，留了退路。上级没有表态，也没有否定。",
                    AttrDelta(ability = 3, politics = 2),
                    political = PoliticalEffect(factions = mapOf(Faction.PRAGMATIC to 3)),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.BALANCED to 3),
                        history = "在争议试点中选择边试边改"
                    )
                ),
                c(
                    "先拖着，等风向明确", Attitude.CAUTIOUS,
                    "你以条件不成熟为由压了下来。事情没出错，但机会也过去了。",
                    AttrDelta(politics = 2, leaderTrust = -4),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.CONSERVATIVE to 5, Faction.REFORM to -2)
                    ),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.STEADY to 4),
                        history = "在争议试点中选择观望"
                    )
                )
            )
        ),
        ev(
            id = "maj_land_use", title = "一块地的用法",
            background = "项目选址会",
            description = "一块位置很好的地，有人想引进一个大项目，也有人提出这片地" +
                "本该留作公共绿地。两边都来找你表态。",
            tags = tags(EventTag.PROJECT, EventTag.CAREER),
            weight = 8, minPos = 1, major = true,
            choices = listOf(
                c(
                    "支持上项目，先把经济拉起来", Attitude.BOLD,
                    "项目落地，数字很好看。环保和居民的反对意见被压了下去。",
                    AttrDelta(work = 3, reputation = 3, integrityRisk = 4),
                    governance = com.shitu.app.model.GovernanceEffect(
                        indicators = mapOf(com.shitu.app.model.Indicator.ECONOMY to 5),
                        merit = mapOf(com.shitu.app.model.MeritKind.ECONOMIC to 4)
                    ),
                    political = PoliticalEffect(
                        relations = mapOf("zheng_haitao" to 6),
                        factions = mapOf(Faction.PRAGMATIC to 5, Faction.REFORM to -3)
                    ),
                    politics = PoliticalImpact(
                        capital = 5,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 4),
                        factionInfluence = mapOf(Faction.PRAGMATIC to 4),
                        history = "力主把地块用于招商项目"
                    )
                ),
                c(
                    "坚持先过环保评估", Attitude.STEADY,
                    "你顶住了压力，把评估放在了前面。项目慢了下来，但路走得更稳。",
                    AttrDelta(politics = 3, colleagues = 2),
                    governance = com.shitu.app.model.GovernanceEffect(
                        indicators = mapOf(com.shitu.app.model.Indicator.ENVIRONMENT to 5),
                        merit = mapOf(com.shitu.app.model.MeritKind.INNOVATION to 3)
                    ),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.REFORM to 5, Faction.LOCAL to -4)
                    ),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 3),
                        factionInfluence = mapOf(Faction.REFORM to 4, Faction.LOCAL to -2),
                        history = "在地块争议中坚持环保优先"
                    )
                ),
                c(
                    "分两期，先做一小块", Attitude.CAUTIOUS,
                    "你切了一刀：先动一小块试水，剩下的留给后面的人去争。",
                    AttrDelta(coordination = 3, ability = 2),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.BALANCED to 3),
                        capital = 3,
                        history = "在地块争议中提出折中方案"
                    )
                )
            )
        ),
        ev(
            id = "maj_personnel_slot", title = "一个空出来的位子",
            background = "组织部门吹风",
            description = "你听到消息：一个比你高一级的岗位可能空出来，符合条件的" +
                "不止你一个。要不要动，怎么动，都是问题。",
            tags = tags(EventTag.CAREER, EventTag.LEADER_TALK),
            weight = 9, minPos = 2, major = true,
            choices = listOf(
                c(
                    "主动找领导汇报，明确表达意愿", Attitude.BOLD,
                    "你把想法说得很清楚。领导没有承诺，但记住了你的态度。",
                    AttrDelta(leaderTrust = 4, reputation = 3),
                    HiddenDelta(opportunity = 6, stress = 5),
                    political = PoliticalEffect(risk = 3),
                    politics = PoliticalImpact(
                        spendCapital = 8,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3),
                        history = "主动争取过一次岗位"
                    ),
                    hint = "要花政治资源"
                ),
                c(
                    "先把成绩材料整理好，等组织来看", Attitude.STEADY,
                    "你没有四处活动，把一年的成绩做成了一本清楚的账。",
                    AttrDelta(work = 3, writing = 3, leaderTrust = 3),
                    politics = PoliticalImpact(
                        spendCapital = 4,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2),
                        history = "用实绩材料争取任职机会"
                    )
                ),
                c(
                    "按兵不动，先把手上事做好", Attitude.CAUTIOUS,
                    "你什么都没说。位子最后给了谁，你也是从别人嘴里听说的。",
                    AttrDelta(politics = 3, reputation = -2),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.STEADY to 3),
                        capital = 4,
                        history = "在岗位空缺时选择按兵不动"
                    )
                )
            )
        ),
        ev(
            id = "maj_faction_offer", title = "有人来递话",
            background = "下班后的车里",
            description = "一位熟人约你坐了坐，话不直说，但意思很清楚：愿意的话，" +
                "可以帮你，也期待你在关键时候站在这一边。",
            tags = tags(EventTag.CAREER, EventTag.MEETING),
            weight = 9, minPos = 1, major = true,
            choices = listOf(
                c(
                    "答应合作，先把关系做实", Attitude.BOLD,
                    "你给了对方明确的态度。从此有人替你说话，也有人开始防着你。",
                    AttrDelta(leaderTrust = 2, colleagues = 2),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.REFORM to 8, Faction.CONSERVATIVE to -5)
                    ),
                    politics = PoliticalImpact(
                        startAlliance = "ally_reform",
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 3),
                        factionInfluence = mapOf(Faction.REFORM to 5, Faction.CONSERVATIVE to -3),
                        history = "与改革派结成临时联盟"
                    ),
                    hint = "结盟有连带风险"
                ),
                c(
                    "不站队，但保持来往", Attitude.CAUTIOUS,
                    "你客客气气地绕开了表态，关系还在，但没有承诺。",
                    AttrDelta(politics = 3, coordination = 2),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.BALANCED to 4),
                        capital = 3,
                        history = "婉拒过一次派系结盟"
                    )
                ),
                c(
                    "直接拒绝，划清界限", Attitude.RISKY,
                    "你把话说明白了，对方笑了笑，说了句「理解」。",
                    AttrDelta(politics = 2),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.REFORM to -6, Faction.NEUTRAL to 3)
                    ),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.STEADY to 3),
                        history = "拒绝过一次派系拉拢"
                    )
                )
            )
        ),
        ev(
            id = "maj_project_dispute", title = "项目被质疑了",
            background = "意见反馈会",
            description = "你主推的项目被公开质疑：投入大、见效慢，还有人怀疑里面的" +
                "程序问题。会议室里所有人都在看你。",
            tags = tags(EventTag.PROJECT, EventTag.PUBLIC_OPINION),
            weight = 8, minPos = 3, major = true,
            choices = listOf(
                c(
                    "顶住压力，继续推进", Attitude.BOLD,
                    "你没有退。项目保住了，但质疑你的人也把事情记在了心里。",
                    AttrDelta(work = 3, reputation = 2, integrityRisk = 4),
                    political = PoliticalEffect(risk = 6),
                    politics = PoliticalImpact(
                        startCrisis = "crisis_project",
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 3),
                        history = "在质疑中坚持推进项目"
                    )
                ),
                c(
                    "主动暂停，逐条复查", Attitude.STEADY,
                    "你叫停了进度，把每一项都摊开来看。慢了一拍，但站住了脚。",
                    AttrDelta(politics = 3, work = -2),
                    political = PoliticalEffect(risk = -5),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.STEADY to 3),
                        history = "主动暂停项目接受复查"
                    )
                ),
                c(
                    "引入外部评估，让程序说话", Attitude.CAUTIOUS,
                    "你请了第三方来评。结论没有帮你太多，但所有人都挑不出毛病。",
                    AttrDelta(ability = 3, coordination = 3),
                    politics = PoliticalImpact(
                        spendCapital = 4,
                        tendencies = mapOf(PoliticalTendency.BALANCED to 3),
                        history = "用第三方评估回应项目质疑"
                    )
                )
            )
        ),
        ev(
            id = "maj_inspection_hint", title = "检查前的暗示",
            background = "一通电话",
            description = "有人告诉你检查组快来了，也顺口提了句「有些材料可以再整理整理」。",
            tags = tags(EventTag.INSPECTION, EventTag.CAREER),
            weight = 7, minPos = 2, major = true,
            choices = listOf(
                c(
                    "全面自查，把问题摆到桌面上", Attitude.STEADY,
                    "你把问题列成清单主动上报。检查组来的时候，反而没什么可说。",
                    AttrDelta(politics = 3, work = 2),
                    political = PoliticalEffect(risk = -6),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.STEADY to 3),
                        intelId = "intel_inspection",
                        history = "在专项检查前主动自查"
                    )
                ),
                c(
                    "该补的补，先把场面做过去", Attitude.BOLD,
                    "检查顺利通过。你心里清楚，有几笔账只是挪了个地方。",
                    AttrDelta(leaderTrust = 2, integrityRisk = 6),
                    HiddenDelta(opportunity = 4, riskPressure = 5),
                    political = PoliticalEffect(risk = 6),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 2),
                        history = "在检查中做了表面文章"
                    )
                ),
                c(
                    "借这个机会反映真实困难", Attitude.RISKY,
                    "你把基层的实际困难写进了汇报。检查组有人认真记了。",
                    AttrDelta(leaderTrust = 3, politics = -1),
                    political = PoliticalEffect(
                        relations = mapOf("zhou_huilan" to 5),
                        factions = mapOf(Faction.CORE to 3)
                    ),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 2),
                        intelId = "intel_inspection",
                        history = "借检查之机反映基层实情"
                    )
                )
            )
        ),
        ev(
            id = "maj_rival_cooperation", title = "同僚抛来的橄榄枝",
            background = "一次工作对接",
            description = "一直和你搭班子的那位同僚提出：这件事两个人一起做，" +
                "比你一个人扛要容易。你分不清这是合作还是别的什么。",
            tags = tags(EventTag.COORDINATION),
            weight = 7, minPos = 2, major = true,
            choices = listOf(
                c(
                    "接受合作，先把事做成", Attitude.BOLD,
                    "你们一起把事做成了。上级表扬的是「团队」，但你清楚其中的分寸。",
                    AttrDelta(coordination = 4, work = 2, colleagues = 3),
                    political = PoliticalEffect(
                        relations = mapOf("ma_wenbin" to 8, "qian_jinming" to 4),
                        trust = mapOf("ma_wenbin" to 6)
                    ),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.BALANCED to 3),
                        capital = 4,
                        history = "接受过同僚的合作"
                    )
                ),
                c(
                    "婉言拒绝，各干各的", Attitude.STEADY,
                    "你客气地推掉了。事情做完，也彻底分出了高下。",
                    AttrDelta(work = 3, ability = 2),
                    political = PoliticalEffect(
                        relations = mapOf("ma_wenbin" to -6),
                        factions = mapOf(Faction.PRAGMATIC to 2)
                    ),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3),
                        history = "拒绝过同僚的合作提议"
                    )
                ),
                c(
                    "表面合作，暗地里自己留一手", Attitude.RISKY,
                    "你答应得很快，也留了后手。事情成了，但关系从此微妙。",
                    AttrDelta(coordination = 3, integrityRisk = 5),
                    political = PoliticalEffect(risk = 5),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 1),
                        history = "对同僚的合作提议阳奉阴违"
                    )
                )
            )
        ),
        ev(
            id = "maj_opinion_crisis", title = "一段视频传开了",
            background = "手机响个不停",
            description = "辖区里一段冲突视频在网上传开，评论一边倒。上面还没表态，" +
                "但电话已经打到你这里。",
            tags = tags(EventTag.PUBLIC_OPINION, EventTag.COMPLAINT),
            weight = 7, minPos = 1, major = true,
            choices = listOf(
                c(
                    "第一时间回应，先道歉再整改", Attitude.STEADY,
                    "你出现在镜头前，把话说在前面。舆情慢慢压了下去。",
                    AttrDelta(reputation = 3, politics = 3),
                    governance = com.shitu.app.model.GovernanceEffect(
                        indicators = mapOf(com.shitu.app.model.Indicator.APPROVAL to 4)
                    ),
                    political = PoliticalEffect(risk = -3),
                    politics = PoliticalImpact(
                        startCrisis = "crisis_opinion",
                        tendencies = mapOf(PoliticalTendency.STEADY to 3),
                        history = "公开回应过一场舆论危机"
                    )
                ),
                c(
                    "先查清事实，再对外说明", Attitude.CAUTIOUS,
                    "你压了两天，等事实清楚才开口。有人嫌你慢，也有人说你稳。",
                    AttrDelta(work = 3, ability = 2),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.BALANCED to 3),
                        intelId = "intel_hidden_issue",
                        history = "先查后说处置舆论"
                    )
                ),
                c(
                    "强硬回应，不惯着闹事的人", Attitude.RISKY,
                    "你的态度很硬，支持的人叫好，反对的人更多了。",
                    AttrDelta(leaderTrust = -2, reputation = -3, integrityRisk = 3),
                    governance = com.shitu.app.model.GovernanceEffect(
                        indicators = mapOf(com.shitu.app.model.Indicator.APPROVAL to -5)
                    ),
                    political = PoliticalEffect(risk = 6),
                    politics = PoliticalImpact(
                        startCrisis = "crisis_opinion",
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 2),
                        history = "强硬处置过舆论事件"
                    )
                )
            )
        ),
        ev(
            id = "maj_route_shift", title = "换一条跑道",
            background = "一次组织谈话",
            description = "组织上问你：愿不愿意换一个方向、换一个平台，" +
                "从头开始也说不定。",
            tags = tags(EventTag.CAREER, EventTag.LEADER_TALK),
            weight = 6, minPos = 3, major = true,
            choices = listOf(
                c(
                    "服从安排，到更需要人的地方去", Attitude.STEADY,
                    "你答应得干脆。新地方条件一般，但缺口也是机会。",
                    AttrDelta(work = 3, politics = 3, leaderTrust = 3),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.BALANCED to 3),
                        capital = 4,
                        history = "服从组织安排换过一次方向"
                    )
                ),
                c(
                    "坚持本行，把专业做深", Attitude.CAUTIOUS,
                    "你说自己更适合留在原地。组织没有勉强。",
                    AttrDelta(ability = 4, work = 2),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 3),
                        history = "坚持留在原方向深耕"
                    )
                ),
                c(
                    "主动请缨去最难的岗位", Attitude.BOLD,
                    "你主动挑了最难的摊子。所有人都觉得你疯，也有人开始佩服你。",
                    AttrDelta(ability = 4, work = 3, reputation = 4, integrityRisk = 3),
                    HiddenDelta(opportunity = 8, stress = 6),
                    political = PoliticalEffect(
                        factions = mapOf(Faction.REFORM to 4, Faction.CORE to 3),
                        risk = 4
                    ),
                    politics = PoliticalImpact(
                        spendCapital = 6,
                        tendencies = mapOf(PoliticalTendency.REFORMIST to 4),
                        history = "主动请缨去最困难的岗位"
                    )
                )
            )
        ),
        ev(
            id = "maj_final_bet", title = "关键的一搏",
            background = "关键的节点",
            description = "到了一个不上不下的位置：再进一步很难，退一步又不甘心。" +
                "身边的人都在等你做决定。",
            tags = tags(EventTag.CAREER, EventTag.ASSESSMENT),
            weight = 7, minPos = 5, major = true,
            choices = listOf(
                c(
                    "把资源压上去，全力争取", Attitude.BOLD,
                    "你把攒了几年的资源一次性押了上去。赢了是台阶，输了要从头再来。",
                    AttrDelta(leaderTrust = 4, reputation = 4, integrityRisk = 3),
                    HiddenDelta(opportunity = 8, riskPressure = 8),
                    political = PoliticalEffect(risk = 5),
                    politics = PoliticalImpact(
                        spendCapital = 12,
                        tendencies = mapOf(PoliticalTendency.PRAGMATIC to 4),
                        history = "关键节点上全力一搏"
                    ),
                    hint = "消耗大量政治资源"
                ),
                c(
                    "联合盟友，把力量凑在一起", Attitude.STEADY,
                    "你找了几个信得过的人，把各自的资源凑到一起。成不成，看这一下。",
                    AttrDelta(colleagues = 4, coordination = 4),
                    politics = PoliticalImpact(
                        spendCapital = 6,
                        startAlliance = "ally_pragmatic",
                        tendencies = mapOf(PoliticalTendency.BALANCED to 4),
                        history = "在关键时刻联合盟友"
                    )
                ),
                c(
                    "稳住基本盘，不冒这个险", Attitude.CAUTIOUS,
                    "你没有动。位子擦肩而过，但你已经有的东西都还在。",
                    AttrDelta(politics = 4, reputation = -2),
                    politics = PoliticalImpact(
                        tendencies = mapOf(PoliticalTendency.STEADY to 4),
                        capital = 8,
                        history = "在关键节点选择稳住不冒险"
                    )
                )
            )
        )
    )
}
