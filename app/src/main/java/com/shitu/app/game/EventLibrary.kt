package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.Choice
import com.shitu.app.model.ConditionalEffect
import com.shitu.app.model.EventTag
import com.shitu.app.model.GameEvent
import com.shitu.app.model.GovernanceEffect
import com.shitu.app.model.GovernanceGate
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.PoliticalEffect
import com.shitu.app.model.PoliticalGate
import com.shitu.app.model.PositionScope
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.PoliticalImpact
import com.shitu.app.model.UnitType

// 事件库的构造辅助函数，只用于让事件定义保持可读。
internal fun tags(vararg t: EventTag): Set<EventTag> = t.toSet()

internal fun c(
    text: String,
    attitude: Attitude,
    outcome: String,
    effects: AttrDelta,
    hidden: HiddenDelta = HiddenDelta(),
    chain: String? = null,
    flag: String? = null,
    route: CareerRoute? = null,
    political: PoliticalEffect = PoliticalEffect(),
    politics: PoliticalImpact = PoliticalImpact(),
    governance: GovernanceEffect = GovernanceEffect(),
    hint: String = "",
    bonus: List<ConditionalEffect> = emptyList()
): Choice = Choice(
    text = text,
    attitude = attitude,
    outcome = outcome,
    effects = effects,
    hiddenEffects = hidden,
    startsChain = chain,
    setsFlag = flag,
    setsRoute = route,
    political = political,
    politics = politics,
    governance = governance,
    hint = hint,
    conditionalBonus = bonus
)

internal fun ev(
    id: String,
    title: String,
    background: String,
    description: String,
    tags: Set<EventTag>,
    choices: List<Choice>,
    weight: Int = 10,
    minPos: Int = 0,
    maxPos: Int = POSITION_LADDER.size - 1,
    units: Set<UnitType> = emptySet(),
    chain: String? = null,
    step: Int = 0,
    after: Int = 0,
    repeatable: Boolean = false,
    cooldown: Int = 12,
    minFamily: Int? = null,
    maxFamily: Int? = null,
    minRisk: Int? = null,
    minStress: Int? = null,
    minHealthRisk: Int? = null,
    minRiskPressure: Int? = null,
    minSocial: Int? = null,
    minNetwork: Int? = null,
    gate: PoliticalGate = PoliticalGate(),
    governanceGate: GovernanceGate = GovernanceGate(),
    priority: Boolean = false,
    major: Boolean = false,
    crisisOnly: Boolean = false,
    requiredScope: PositionScope? = null,
    cityTags: Set<String> = emptySet(),
    forbidCityTags: Set<String> = emptySet()
): GameEvent = GameEvent(
    id = id,
    title = title,
    background = background,
    description = description,
    tags = tags,
    choices = choices,
    weight = weight,
    minPositionIndex = minPos,
    maxPositionIndex = maxPos,
    units = units,
    chainId = chain,
    chainStep = step,
    chainAfterQuarters = after,
    repeatable = repeatable,
    cooldownQuarters = cooldown,
    minFamilyRelation = minFamily,
    maxFamilyRelation = maxFamily,
    minIntegrityRisk = minRisk,
    minStress = minStress,
    minHealthRisk = minHealthRisk,
    minRiskPressure = minRiskPressure,
    minSocialInfluence = minSocial,
    minNetwork = minNetwork,
    politicalGate = gate,
    governanceGate = governanceGate,
    priority = priority,
    major = major,
    crisisOnly = crisisOnly,
    requiredScope = requiredScope,
    requiredCityTags = cityTags,
    forbiddenCityTags = forbidCityTags
)

// 第一批：基础事件（灾情、群众、材料、协调、廉政等）。
private val eventsGroupOne: List<GameEvent> = listOf(
    ev(
        id = "rainstorm", title = "暴雨灾情",
        background = "连续暴雨，辖区多处道路积水",
        description = "凌晨的暴雨让低洼路段积水过膝，一名群众被困在屋内，电话打到了你的办公室。",
        tags = tags(EventTag.DISASTER, EventTag.FLOOD),
        weight = 12,
        choices = listOf(
            c(
                "立即组织人员赶往现场救援", Attitude.BOLD,
                "你带着几名同事赶到现场，把被困群众背了出来。视频被群众发到网上，点赞很多，但你在水里泡了三个小时。",
                AttrDelta(work = 8, ability = 4, masses = 8, reputation = 5, health = -7, energy = -9),
                HiddenDelta(riskPressure = 6, stress = 8, socialInfluence = 5)
            ),
            c(
                "先等上级指示，避免处置不当", Attitude.CAUTIOUS,
                "你守在电话旁等待指令，救援最终由另一支队伍完成。被困群众无碍，但有人记住了你当时不在现场。",
                AttrDelta(work = -6, leaderTrust = 2, masses = -8, reputation = -5, energy = 3),
                HiddenDelta(opportunity = -4, stress = 2)
            ),
            c(
                "联系相关部门并同步组织现场救援", Attitude.STEADY,
                "你一边通知应急与消防，一边组织身边同事先行排水救人。救援比预期更快，流程也说得清楚。",
                AttrDelta(work = 5, coordination = 7, masses = 5, leaderTrust = 3, energy = -4),
                HiddenDelta(network = 5, opportunity = 2)
            )
        )
    ),
    ev(
        id = "mass_help", title = "群众求助",
        background = "一位老人为孙子的学籍问题找到你",
        description = "老人跑了三个部门都没办成，听说你办事认真，直接找到你的办公室。这件事并不在你的职责清单里。",
        tags = tags(EventTag.MASS_APPEAL),
        weight = 12,
        choices = listOf(
            c(
                "亲自陪着老人跑完流程", Attitude.BOLD,
                "你花了一个下午陪老人补齐材料。事情办成了，老人逢人就说你的好，但你自己的工作只能晚上补。",
                AttrDelta(work = 3, masses = 8, reputation = 5, energy = -6, family = -2),
                HiddenDelta(network = 4, socialInfluence = 3)
            ),
            c(
                "把问题转交并持续跟进", Attitude.STEADY,
                "你按职责转交，并留下电话跟踪进度。事情解决了，你也守住了边界。",
                AttrDelta(work = 2, coordination = 5, masses = 3, colleagues = 2)
            ),
            c(
                "告知不属于本部门职责", Attitude.CAUTIOUS,
                "你解释了职责划分，老人失望地走了。程序上没有问题，但这件事在你心里留了一下。",
                AttrDelta(masses = -5, reputation = -3, politics = 2),
                HiddenDelta(riskPressure = -2, stress = 3)
            )
        )
    ),
    ev(
        id = "petition", title = "信访矛盾",
        background = "一起持续两年的邻里纠纷再次上访",
        description = "两户人家因为宅基地边界吵了两年，这次双方都带人到了接待大厅，情绪激动。",
        tags = tags(EventTag.PETITION, EventTag.MASS_APPEAL),
        weight = 11,
        choices = listOf(
            c(
                "亲自接访，当场承诺限期解决", Attitude.BOLD,
                "你当场定下调解时间，双方暂时散了。承诺说出去了，压力也落在了你身上。",
                AttrDelta(work = 4, masses = 6, leaderTrust = 2, energy = -5),
                HiddenDelta(stress = 8, riskPressure = 4),
                chain = "petition", flag = "petition_promise"
            ),
            c(
                "按程序转办并跟踪督办", Attitude.STEADY,
                "你按流程转办，建立台账。事情进入轨道，速度不快但没有漏洞。",
                AttrDelta(coordination = 5, work = 2, politics = 2)
            ),
            c(
                "先稳定情绪，暂缓处理", Attitude.CAUTIOUS,
                "你请双方先回去等消息，大厅恢复了安静。但问题还在原地，下次可能更难办。",
                AttrDelta(masses = -4, work = -2, politics = 1),
                HiddenDelta(stress = 4, riskPressure = 3)
            )
        )
    ),
    ev(
        id = "accident", title = "突发事故",
        background = "辖区内发生一起安全事故",
        description = "现场情况不明，有人员受伤，围观群众越来越多，各种说法已经开始传播。",
        tags = tags(EventTag.ACCIDENT, EventTag.EMERGENCY),
        weight = 11,
        choices = listOf(
            c(
                "第一时间赶到现场处置", Attitude.BOLD,
                "你赶到现场协调救治、疏散人群。处置及时，但你也在现场连续待了十几个小时。",
                AttrDelta(work = 8, ability = 4, leaderTrust = 4, health = -6, energy = -8),
                HiddenDelta(riskPressure = 7, stress = 9)
            ),
            c(
                "启动预案并按程序上报", Attitude.STEADY,
                "预案迅速启动，各部门按分工到位。处置过程规范，你在其中承担了协调角色。",
                AttrDelta(work = 4, coordination = 6, politics = 4, leaderTrust = 3),
                HiddenDelta(network = 4)
            ),
            c(
                "等待专业部门到场处理", Attitude.CAUTIOUS,
                "你留在后方等待消息。专业部门处理得不错，但现场群众对迟迟没见到负责人颇有微词。",
                AttrDelta(work = -4, masses = -6, reputation = -4, energy = 2),
                HiddenDelta(opportunity = -5, stress = 3)
            )
        )
    ),
    ev(
        id = "major_project", title = "重大项目",
        background = "区里要上一个重点项目，需要有人牵头",
        description = "项目周期长、难度大，做成了是实绩，做砸了要担责。领导正在物色牵头人。",
        tags = tags(EventTag.PROJECT),
        weight = 10,
        choices = listOf(
            c(
                "主动请缨牵头负责", Attitude.BOLD,
                "你把项目接了下来。从那天起，你的日程表被会议和现场填满。",
                AttrDelta(work = 9, ability = 5, leaderTrust = 5, energy = -9, health = -5),
                HiddenDelta(opportunity = 8, mentor = 5, stress = 10),
                chain = "project", flag = "project_lead"
            ),
            c(
                "参与其中，承担一部分工作", Attitude.STEADY,
                "你承担了其中一块具体任务，出力但不站在最前面。",
                AttrDelta(work = 5, coordination = 5, ability = 3, energy = -4)
            ),
            c(
                "保持距离，专注本职", Attitude.CAUTIOUS,
                "你婉拒了参与。本职工作没有受影响，但机会也从眼前过去了。",
                AttrDelta(work = -2, leaderTrust = -5, energy = 3),
                HiddenDelta(opportunity = -8, mentor = -3)
            )
        )
    ),
    ev(
        id = "rural_revitalization", title = "乡村振兴",
        background = "上级要求推进乡村振兴试点",
        description = "村里基础薄弱，群众意见不一。试点怎么推，方案需要你来拿。",
        tags = tags(EventTag.RURAL, EventTag.PROJECT),
        weight = 10,
        choices = listOf(
            c(
                "选择一个村先行试点", Attitude.BOLD,
                "你挑了一个基础较好的村先干起来，三个月后初见成效，也开始有人来参观。",
                AttrDelta(work = 7, masses = 6, ability = 4, energy = -7),
                HiddenDelta(opportunity = 6, socialInfluence = 4),
                chain = "rural", flag = "rural_pilot"
            ),
            c(
                "先做调研，再稳妥推进", Attitude.STEADY,
                "你花了一个月走遍所有村组，方案扎实，推进速度不快但阻力很小。",
                AttrDelta(work = 4, masses = 4, ability = 4, politics = 3, energy = -4)
            ),
            c(
                "按上级模板快速完成动作", Attitude.CAUTIOUS,
                "材料做得整齐，检查能过关。但村里人私下说，这只是又填了一遍表。",
                AttrDelta(work = 1, politics = 4, masses = -4, reputation = -4, writing = 3)
            )
        )
    ),
    ev(
        id = "flood_duty", title = "防汛抗旱",
        background = "汛期到来，需要连续值守",
        description = "上级要求 24 小时值班，堤段需要有人守，办公室也需要有人写材料。",
        tags = tags(EventTag.FLOOD, EventTag.OVERTIME),
        weight = 11,
        choices = listOf(
            c(
                "把自己排在堤段值班表第一班", Attitude.BOLD,
                "你在堤上守了两个通宵，鞋没干过。领导巡查时看见了你的名字。",
                AttrDelta(work = 7, leaderTrust = 5, masses = 4, energy = -10, health = -6),
                HiddenDelta(stress = 8, riskPressure = 3)
            ),
            c(
                "统筹排班，保证人人有责", Attitude.STEADY,
                "你把值班表排得清楚，还准备了轮换方案。大家嘴上抱怨，心里服气。",
                AttrDelta(coordination = 6, work = 4, colleagues = 4, energy = -4)
            ),
            c(
                "主动承担材料与信息报送", Attitude.CAUTIOUS,
                "你在办公室负责信息汇总与上报，材料写得漂亮，但一线的人没怎么见到你。",
                AttrDelta(writing = 6, politics = 3, work = -3, masses = -4)
            )
        )
    ),
    ev(
        id = "emergency", title = "应急处突",
        background = "群体性事件苗头出现",
        description = "几十名群众聚集在单位门口，要求对某项政策作出解释，情绪正在升级。",
        tags = tags(EventTag.EMERGENCY, EventTag.PUBLIC_OPINION),
        weight = 10,
        minPos = 1,
        choices = listOf(
            c(
                "走到人群前面直接对话", Attitude.BOLD,
                "你拿着扩音器站到最前面，一条条回应诉求。人群在两小时后散去，你的衬衫全湿了。",
                AttrDelta(work = 8, masses = 7, ability = 4, politics = 3, health = -5),
                HiddenDelta(riskPressure = 8, socialInfluence = 6)
            ),
            c(
                "按预案上报并组织多部门协同", Attitude.STEADY,
                "你按程序上报，同时协调多个部门到场接待。事情平稳解决，没有留下话柄。",
                AttrDelta(coordination = 6, politics = 5, work = 4, leaderTrust = 4)
            ),
            c(
                "回避现场，交由上级处理", Attitude.CAUTIOUS,
                "你没有出现在现场。问题最终由上级出面解决，但同事们的眼神有些微妙。",
                AttrDelta(leaderTrust = -6, work = -4, colleagues = -4),
                HiddenDelta(opportunity = -6, stress = -2)
            )
        )
    ),
    ev(
        id = "inspection", title = "检查迎检",
        background = "上级检查组下周到达",
        description = "检查组要看台账、看现场、看整改。单位里两种意见：把材料做到完美，还是把问题摊开讲清。",
        tags = tags(EventTag.INSPECTION, EventTag.MATERIAL),
        weight = 12,
        choices = listOf(
            c(
                "全力准备台账与汇报材料", Attitude.STEADY,
                "你们把三年的台账重新梳理了一遍，检查当天顺利过关。你的文字能力被看见了。",
                AttrDelta(writing = 8, work = 4, politics = 4, energy = -8)
            ),
            c(
                "突击补齐短板，现场硬碰硬", Attitude.BOLD,
                "你带着人连夜整改现场问题。检查组认可了整改速度，也指出基础工作不牢。",
                AttrDelta(work = 6, ability = 4, health = -5, energy = -8),
                HiddenDelta(riskPressure = 6, stress = 8)
            ),
            c(
                "如实反映困难与客观问题", Attitude.CAUTIOUS,
                "你把问题原原本本讲了出来。检查组记录了，单位里有人不高兴，但也有人说你实在。",
                AttrDelta(politics = 6, reputation = 7, masses = 4, leaderTrust = -4)
            )
        )
    ),
    ev(
        id = "urgent_material", title = "紧急材料",
        background = "今晚必须交一份重要汇报材料",
        description = "领导明天一早要用，材料还没成型，办公室的灯只剩下你这一间还亮着。",
        tags = tags(EventTag.MATERIAL, EventTag.OVERTIME),
        weight = 9,
        repeatable = true,
        cooldown = 8,
        choices = listOf(
            c(
                "一个人连夜写完", Attitude.BOLD,
                "你写到凌晨四点，材料交上去时领导很满意。第二天你靠在椅子上眯了一个小时。",
                AttrDelta(writing = 8, work = 5, leaderTrust = 5, health = -6, energy = -9, family = -5),
                HiddenDelta(stress = 9, healthRisk = 6)
            ),
            c(
                "召集相关同事分工协作", Attitude.STEADY,
                "你把任务拆成几块，十一点前全部收齐。材料质量不错，团队也没熬到太晚。",
                AttrDelta(coordination = 6, writing = 4, colleagues = 4, work = 4, energy = -4)
            ),
            c(
                "说明情况，申请明天下午交", Attitude.CAUTIOUS,
                "领导同意延后，但你明显感到对方语气里的失望。你把材料做得更细了一些。",
                AttrDelta(leaderTrust = -6, politics = -2, writing = 3, health = 3, energy = 3)
            )
        )
    ),
    ev(
        id = "leader_task", title = "领导临时交办",
        background = "领导把你叫到办公室",
        description = "一件不在计划内的事情需要有人接手，时间紧，要求高，责任也说不清。",
        tags = tags(EventTag.LEADER_TASK),
        weight = 12,
        repeatable = true,
        cooldown = 6,
        choices = listOf(
            c(
                "不推辞，立刻着手去办", Attitude.BOLD,
                "你当天就开始推进，遇到问题随时汇报。领导在多个场合提到了你的效率。",
                AttrDelta(work = 6, leaderTrust = 8, ability = 3, energy = -6),
                HiddenDelta(mentor = 6, opportunity = 5, stress = 5)
            ),
            c(
                "先问清目标与边界再动手", Attitude.STEADY,
                "你把要求问得很细，办出来的结果和领导预期基本一致，中间没有返工。",
                AttrDelta(ability = 5, leaderTrust = 4, politics = 3, work = 3)
            ),
            c(
                "把任务转给更合适的同事", Attitude.CAUTIOUS,
                "事情由别人办了。你省下时间处理本职，但领导之后很少再单独找你。",
                AttrDelta(colleagues = -5, leaderTrust = -6, reputation = -4, energy = 3),
                HiddenDelta(mentor = -6, opportunity = -6)
            )
        )
    ),
    ev(
        id = "colleague_mistake", title = "同事工作失误",
        background = "同事报送的数据出了错",
        description = "数据已经报上去，明天会被上级看到。同事还不知道，正在准备下班。",
        tags = tags(EventTag.COLLEAGUE),
        weight = 11,
        choices = listOf(
            c(
                "主动补位，连夜更正并说明", Attitude.BOLD,
                "你留下来把数据重新核了一遍，并向上级说明了更正原因。事情没有扩大。",
                AttrDelta(work = 6, colleagues = 8, leaderTrust = 5, energy = -6)
            ),
            c(
                "提醒同事，一起把问题改掉", Attitude.STEADY,
                "你把同事叫回来一起核对。对方很感激，你们的配合从此顺畅了很多。",
                AttrDelta(colleagues = 7, coordination = 4, work = 4, ability = 2)
            ),
            c(
                "按程序如实上报", Attitude.CAUTIOUS,
                "问题被如实上报，同事受到了批评。你说不出哪里不对，但办公室的气氛变了。",
                AttrDelta(politics = 3, colleagues = -8, reputation = -3),
                HiddenDelta(network = -5)
            )
        )
    ),
    ev(
        id = "dept_coordination", title = "部门协调",
        background = "一件跨部门的事推不动",
        description = "各部门都说不归自己管，事情卡在中间，群众已经等了两个月。",
        tags = tags(EventTag.COORDINATION),
        weight = 11,
        choices = listOf(
            c(
                "主动牵头把各方拉到一张桌上", Attitude.BOLD,
                "你一户一户打电话，把几个部门请到会议室。事情当天就有了分工表。",
                AttrDelta(coordination = 9, work = 5, colleagues = 4, energy = -6),
                HiddenDelta(network = 8)
            ),
            c(
                "请分管领导出面协调", Attitude.STEADY,
                "领导出面后事情很快推下去。你拿到了结果，但也少了一次自己协调的机会。",
                AttrDelta(work = 3, coordination = 3, leaderTrust = 2),
                HiddenDelta(network = -3, mentor = 2)
            ),
            c(
                "按流程发函，等待回复", Attitude.CAUTIOUS,
                "函发出去了，回复还没来。事情继续等，群众又来问了一次。",
                AttrDelta(politics = 3, work = -2, coordination = 1, reputation = -3, masses = -3)
            )
        )
    ),
    ev(
        id = "mass_complaint", title = "群众投诉",
        background = "有人投诉办事窗口态度差",
        description = "投诉件转到了你手上，被投诉的是你熟悉的同事，群众在电话里情绪仍然很激动。",
        tags = tags(EventTag.COMPLAINT, EventTag.MASS_APPEAL),
        weight = 11,
        choices = listOf(
            c(
                "上门回访，当面把问题讲透", Attitude.BOLD,
                "你到群众家里坐下来聊了两个小时。对方最后说，其实不是大事，就是没人听他说。",
                AttrDelta(masses = 8, reputation = 5, work = 4, energy = -5)
            ),
            c(
                "电话沟通并督促窗口整改", Attitude.STEADY,
                "你在电话里解释清楚，同时把窗口流程改了一处。投诉人接受了答复。",
                AttrDelta(coordination = 5, masses = 3, work = 3)
            ),
            c(
                "按规定书面答复，不再跟进", Attitude.CAUTIOUS,
                "答复在规定期限内寄出。程序上没有瑕疵，但投诉人把事情发到了网上。",
                AttrDelta(masses = -7, reputation = -6, politics = 2),
                HiddenDelta(riskPressure = 6, socialInfluence = -4)
            )
        )
    ),
    ev(
        id = "public_opinion", title = "舆情事件",
        background = "一段短视频在本地群里传开",
        description = "视频拍的是你单位辖区的一处问题，配上了情绪化的解说，转发量正在上升。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.EMERGENCY),
        weight = 10,
        minPos = 1,
        choices = listOf(
            c(
                "第一时间回应并同步整改", Attitude.BOLD,
                "当天下午你就发布了情况说明和整改措施。舆论很快转向，群众还提了建议。",
                AttrDelta(reputation = 9, masses = 6, politics = 5, work = 4),
                HiddenDelta(socialInfluence = 8, riskPressure = -4)
            ),
            c(
                "与宣传部门联动统一发布", Attitude.STEADY,
                "口径统一，发布节奏稳。舆情在两天内平息，没有出现次生问题。",
                AttrDelta(coordination = 7, politics = 4, reputation = 4),
                HiddenDelta(network = 5)
            ),
            c(
                "暂不回应，避免扩大影响", Attitude.CAUTIOUS,
                "热度确实下去了一些，但截屏留了下来，之后每次有类似事件都会被人翻出来。",
                AttrDelta(reputation = -7, masses = -5),
                HiddenDelta(stress = 8, riskPressure = 8, socialInfluence = -3)
            )
        )
    ),
    ev(
        id = "integrity_temptation", title = "廉政诱惑",
        background = "项目方在饭局后递来一个信封",
        description = "对方说只是「一点心意」，事情本身在流程内，签不签字还在你手上。",
        tags = tags(EventTag.INTEGRITY, EventTag.GIFT),
        weight = 10,
        minPos = 2,
        choices = listOf(
            c(
                "当场拒绝并说明纪律要求", Attitude.BOLD,
                "你把信封推了回去，并当面讲清了道理。对方有些尴尬，饭局很快散了。",
                AttrDelta(politics = 7, reputation = 5, leaderTrust = 3, integrityRisk = -8),
                HiddenDelta(riskPressure = -6, mentor = 2)
            ),
            c(
                "拒收并如实向组织报告", Attitude.STEADY,
                "你第二天就把情况写成了书面报告。组织上肯定了你的做法，项目方从此待你更加规矩。",
                AttrDelta(politics = 8, integrityRisk = -5, leaderTrust = 5, work = 2),
                HiddenDelta(riskPressure = -4, mentor = 5)
            ),
            c(
                "先收下，事后再处理", Attitude.RISKY,
                "你把信封放进了抽屉最里面。钱没有动，但每次有人敲门你都会心跳一下。",
                AttrDelta(integrityRisk = 20, wealth = 12),
                HiddenDelta(riskPressure = 20, stress = 26),
                chain = "integrity", flag = "integrity_stain",
                hint = "短期收益高，长期风险极高"
            )
        )
    ),
    ev(
        id = "gift_exchange", title = "人情往来",
        background = "老同学带着礼物上门",
        description = "多年不见的同学忽然来访，聊到最后提到，他亲戚的事情正好在你手上。",
        tags = tags(EventTag.GIFT, EventTag.INTEGRITY),
        weight = 10,
        choices = listOf(
            c(
                "依规登记，并按程序办理", Attitude.STEADY,
                "你把礼物登记上交，事情按程序走。同学不太高兴，但你心里踏实。",
                AttrDelta(politics = 6, integrityRisk = -7, reputation = 3, colleagues = 2),
                HiddenDelta(riskPressure = -5)
            ),
            c(
                "私下把礼物退回，事情照章办", Attitude.CAUTIOUS,
                "你悄悄把东西退了回去。事情办得干净，只是同学关系淡了一层。",
                AttrDelta(politics = 4, integrityRisk = -4, colleagues = -3, reputation = 2)
            ),
            c(
                "收下礼物，留一份人情", Attitude.RISKY,
                "你收下了那份人情。事情照着规矩办了，但你知道，人情是要还的。",
                AttrDelta(integrityRisk = 14, wealth = 7, colleagues = 5),
                HiddenDelta(riskPressure = 14, network = 4, stress = 8)
            )
        )
    ),
    ev(
        id = "overtime_task", title = "加班任务",
        background = "又一轮集中攻坚开始了",
        description = "任务表贴在墙上，接下来一个月，晚上十点前很难离开办公室。",
        tags = tags(EventTag.OVERTIME, EventTag.MATERIAL),
        weight = 8,
        repeatable = true,
        cooldown = 8,
        choices = listOf(
            c(
                "连续加班，把进度顶上去", Attitude.BOLD,
                "你把进度条往前拉了一大截，也把身体的账往后拖了一大笔。",
                AttrDelta(work = 9, leaderTrust = 5, health = -7, energy = -9, family = -7),
                HiddenDelta(stress = 10, healthRisk = 7)
            ),
            c(
                "合理分工，按节点推进", Attitude.STEADY,
                "你把任务拆解到人，按节点检查。进度正常，团队也没有失控。",
                AttrDelta(coordination = 5, work = 5, colleagues = 4, energy = -4)
            ),
            c(
                "按标准完成，不做额外投入", Attitude.CAUTIOUS,
                "你保住了生活节奏，任务也完成了，只是没有留下什么让人记住的东西。",
                AttrDelta(work = 2, health = 3, family = 4, leaderTrust = -4)
            )
        )
    ),
    ev(
        id = "family_conflict", title = "家庭矛盾",
        background = "家里已经等你很久了",
        description = "又一次因为临时任务错过了约定，家人的失望已经攒了很久。",
        tags = tags(EventTag.FAMILY),
        weight = 12,
        repeatable = true,
        cooldown = 8,
        maxFamily = 45,
        choices = listOf(
            c(
                "推掉应酬，回家把话说开", Attitude.STEADY,
                "你把事情解释清楚，也认真听了对方的委屈。关系缓和了，但需要时间慢慢修复。",
                AttrDelta(family = 9, energy = 3, work = -2, health = 2)
            ),
            c(
                "请几天假陪家人", Attitude.CAUTIOUS,
                "你第一次为了家里放下工作。家人明显轻松了，单位那边则留下了一点印象。",
                AttrDelta(family = 13, health = 4, energy = 5, leaderTrust = -5, work = -4)
            ),
            c(
                "解释情况后继续投入工作", Attitude.BOLD,
                "家人说「知道了」。这三个字之后，你们之间的对话变少了。",
                AttrDelta(family = -10, work = 5, leaderTrust = 3),
                HiddenDelta(stress = 6)
            )
        )
    ),
    ev(
        id = "health_issue", title = "健康问题",
        background = "体检报告上的箭头变多了",
        description = "医生建议你调整作息，少熬夜，多运动。报告放在桌上，电话又响了。",
        tags = tags(EventTag.HEALTH),
        weight = 11,
        repeatable = true,
        cooldown = 8,
        minStress = 40,
        choices = listOf(
            c(
                "调整作息，坚持锻炼", Attitude.STEADY,
                "你开始每天早起走路，晚上尽量不接无关电话。指标慢慢回来了，事情也没塌。",
                AttrDelta(health = 10, energy = 8, work = -2, leaderTrust = -1),
                HiddenDelta(healthRisk = -8, stress = -6)
            ),
            c(
                "先把手上的事做完再说", Attitude.BOLD,
                "你把报告收进抽屉。事情做完了，身体的账继续记着。",
                AttrDelta(work = 6, leaderTrust = 3, health = -8, energy = -6),
                HiddenDelta(healthRisk = 10, stress = 6)
            ),
            c(
                "申请一段时间的调整", Attitude.CAUTIOUS,
                "领导同意你减轻一部分任务。身体缓过来了，但重要工作开始绕开你。",
                AttrDelta(health = 12, energy = 9, family = 5, work = -5, leaderTrust = -5),
                HiddenDelta(opportunity = -6)
            )
        )
    )
)

// 第二批：机关日常、评价与机会类事件。
private val eventsGroupTwo: List<GameEvent> = listOf(
    ev(
        id = "leader_talk", title = "领导谈话",
        background = "分管领导让你去一趟办公室",
        description = "没有说明具体事由，只说随便聊聊。你大概能猜到，和最近的工作评价有关。",
        tags = tags(EventTag.LEADER_TALK),
        weight = 11,
        minPos = 1,
        choices = listOf(
            c(
                "如实汇报想法与困难", Attitude.BOLD,
                "你把真实情况讲了，包括自己的困惑。领导听完沉默了一会儿，说你比他想得清楚。",
                AttrDelta(leaderTrust = 7, politics = 4, ability = 3),
                HiddenDelta(mentor = 6)
            ),
            c(
                "先听领导的想法再回应", Attitude.STEADY,
                "你听得多说得少，最后顺着领导关注的几个点表了态。谈话气氛很好。",
                AttrDelta(leaderTrust = 5, politics = 5, coordination = 2)
            ),
            c(
                "只谈成绩，不谈问题", Attitude.CAUTIOUS,
                "你把这几年做成的几件事讲得很清楚。领导点了点头，但没有再多说什么。",
                AttrDelta(leaderTrust = 2, politics = -2, reputation = -2)
            )
        )
    ),
    ev(
        id = "colleague_competition", title = "评议季",
        background = "又到了评议的时候",
        description = "手上的事做得怎么样，这个月都要摆到桌面上。你把心思收回来，先把手里的收尾做好。",
        tags = tags(EventTag.ASSESSMENT),
        weight = 10,
        minPos = 1,
        choices = listOf(
            c(
                "把精力放在做出实际成果上", Attitude.STEADY,
                "你没有多说什么，只是把手上项目往前推了一大步。结果出来时，推荐名单上有你的名字。",
                AttrDelta(work = 8, ability = 4, reputation = 5, leaderTrust = 4, energy = -6),
                HiddenDelta(opportunity = 6)
            ),
            c(
                "主动沟通，争取支持", Attitude.BOLD,
                "你找了几位关键同事当面聊，把想法讲明白。支持者变多了，也有人觉得你太急。",
                AttrDelta(leaderTrust = 6, coordination = 5, colleagues = -3, reputation = 3),
                HiddenDelta(network = 7, opportunity = 5)
            ),
            c(
                "按部就班，不额外用力", Attitude.CAUTIOUS,
                "你按自己的节奏收尾。事情平平淡淡过去，没有惊喜，也没出岔子。",
                AttrDelta(colleagues = 4, reputation = -2, work = -2),
                HiddenDelta(opportunity = -6)
            )
        )
    ),
    ev(
        id = "important_meeting", title = "重要会议",
        background = "会议室里即将讨论你负责的议题",
        description = "主要领导的意见还不清楚，你的方案在会上可能会被大幅修改。",
        tags = tags(EventTag.MEETING),
        weight = 10,
        choices = listOf(
            c(
                "坚持专业判断，把利弊讲透", Attitude.BOLD,
                "你把数据和风险摆在了桌面上。方案被采纳了大半，但也有人的脸色不太好看。",
                AttrDelta(politics = 4, ability = 5, leaderTrust = 3, reputation = 5, colleagues = -3),
                HiddenDelta(riskPressure = 5)
            ),
            c(
                "先听意见，再灵活调整", Attitude.STEADY,
                "你根据会上反馈调整了方案，最终版本和主要领导的思路很接近。",
                AttrDelta(coordination = 5, politics = 5, leaderTrust = 4)
            ),
            c(
                "按领导倾向简化处理", Attitude.CAUTIOUS,
                "会议很快结束，方案顺利通过。只是你原本想坚持的那部分内容被删掉了。",
                AttrDelta(politics = 3, leaderTrust = 2, ability = -2, reputation = -2)
            )
        )
    ),
    ev(
        id = "project_acceptance", title = "项目验收",
        background = "你负责的项目进入验收阶段",
        description = "验收组已经到场，有几项指标并不好看，会议室里的空气有点紧。",
        tags = tags(EventTag.ACCEPTANCE, EventTag.PROJECT),
        weight = 10,
        minPos = 2,
        choices = listOf(
            c(
                "如实说明并给出整改时间表", Attitude.STEADY,
                "你把问题讲清楚，也拿出了可行的整改方案。验收组给了通过，并注明需要复查。",
                AttrDelta(politics = 5, work = 5, reputation = 5, ability = 3)
            ),
            c(
                "现场承诺限期整改到位", Attitude.BOLD,
                "你当场立下时间表，并连夜组织施工。整改在期限内完成，你累得说话都在发飘。",
                AttrDelta(work = 8, leaderTrust = 5, health = -6, energy = -8),
                HiddenDelta(stress = 8)
            ),
            c(
                "强调客观困难，争取延期", Attitude.CAUTIOUS,
                "验收组同意延期。压力缓了一下，但项目在你手上的评价打了折扣。",
                AttrDelta(work = -3, ability = -2, leaderTrust = -3, energy = 3),
                HiddenDelta(opportunity = -4)
            )
        )
    ),
    ev(
        id = "grassroots_research", title = "基层调研",
        background = "你要在一个村里蹲点一周",
        description = "不打招呼、不设路线，直接住进村里。这样做能看到真情况，也容易得罪人。",
        tags = tags(EventTag.RESEARCH, EventTag.MASS_APPEAL),
        weight = 11,
        choices = listOf(
            c(
                "不打招呼，随机走访", Attitude.BOLD,
                "你走进最偏的几户人家，听到了很多平时听不到的话。报告写出来，有人不舒服。",
                AttrDelta(masses = 8, ability = 5, work = 5, politics = 4, colleagues = -3),
                HiddenDelta(socialInfluence = 4)
            ),
            c(
                "按计划调研并补充随机走访", Attitude.STEADY,
                "你完成了规定动作，也挤出时间去了两个自然村。报告既有面子也有里子。",
                AttrDelta(work = 5, ability = 4, masses = 4, writing = 4)
            ),
            c(
                "按安排路线完成调研", Attitude.CAUTIOUS,
                "流程顺利，材料齐全。报告的结论和往年差不多。",
                AttrDelta(politics = 4, writing = 4, work = 2, masses = -2, ability = -2)
            )
        )
    ),
    ev(
        id = "paired_assistance", title = "结对帮扶",
        background = "单位安排你结对帮扶一户人家",
        description = "这户人家有重病人，也有正在读书的孩子，情况比表格上复杂得多。",
        tags = tags(EventTag.RURAL, EventTag.MASS_APPEAL),
        weight = 10,
        choices = listOf(
            c(
                "长期跟到底，帮着找资源", Attitude.BOLD,
                "你跑了医保、民政、学校，把能用的政策都用上了。年底他们家孩子的成绩单贴在了墙上。",
                AttrDelta(masses = 8, reputation = 6, work = 4, energy = -6, family = -2),
                HiddenDelta(socialInfluence = 6, network = 4)
            ),
            c(
                "按规定完成帮扶任务", Attitude.STEADY,
                "走访、记录、上报，每一步都做到了。情况有改善，但不算彻底。",
                AttrDelta(work = 4, masses = 3, politics = 3)
            ),
            c(
                "主要做好台账与总结", Attitude.CAUTIOUS,
                "台账做得很漂亮，检查时得到表扬。那户人家的实际问题解决得并不多。",
                AttrDelta(writing = 5, politics = 3, masses = -5, reputation = -4)
            )
        )
    ),
    ev(
        id = "window_service", title = "窗口服务",
        background = "你被安排到窗口值班",
        description = "队伍排到了门外，有人材料不齐，有人情绪不好，还有人赶时间。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.COMPLAINT),
        weight = 10,
        choices = listOf(
            c(
                "一次性告知，帮群众想办法", Attitude.STEADY,
                "你把缺的材料列成清单，能容缺的先办。当天办结率明显提高。",
                AttrDelta(masses = 6, work = 4, coordination = 4, reputation = 4)
            ),
            c(
                "坚持原则，材料不齐不予受理", Attitude.CAUTIOUS,
                "你严格按标准执行，没有办错一件。但窗口前的抱怨声也没停过。",
                AttrDelta(politics = 4, masses = -5, reputation = -4, work = 2)
            ),
            c(
                "延长服务时间，把当天的件办完", Attitude.BOLD,
                "你多干了两个小时，最后一个群众办完事时连声道谢。你回家时天已经黑了。",
                AttrDelta(masses = 7, work = 5, reputation = 5, energy = -7, family = -3, health = -3)
            )
        )
    ),
    ev(
        id = "data_report", title = "数据统计上报",
        background = "一张表要在今天下班前报出",
        description = "各口径数据互相不匹配，有的数字明显好看，有的数字明显难看。",
        tags = tags(EventTag.MATERIAL),
        weight = 10,
        repeatable = true,
        cooldown = 8,
        choices = listOf(
            c(
                "逐项核实，按实际情况上报", Attitude.BOLD,
                "你打了十几个电话核对。数据最终不算漂亮，但每一个都经得起问。",
                AttrDelta(politics = 5, work = 4, ability = 4, reputation = 4, leaderTrust = -3)
            ),
            c(
                "取折中口径，同时说明原因", Attitude.STEADY,
                "你在报表上加了说明，把差异的原因写清楚。上级没有追问。",
                AttrDelta(work = 3, writing = 4, politics = 3, coordination = 2)
            ),
            c(
                "按往年惯例填报", Attitude.CAUTIOUS,
                "表格按时报出，没人提出问题。这个数字会在明年继续被沿用。",
                AttrDelta(work = -3, politics = 2, ability = -3)
            )
        )
    ),
    ev(
        id = "receive_superior", title = "上级调研接待",
        background = "上级调研组本周到访",
        description = "接待规格、汇报内容、现场点位，每一项都有人盯着。",
        tags = tags(EventTag.INSPECTION, EventTag.MEETING),
        weight = 10,
        choices = listOf(
            c(
                "把真实情况讲清楚", Attitude.BOLD,
                "你没有回避问题，也讲了客观困难。调研组记录得很细，临走时说了一句「难得」。",
                AttrDelta(politics = 5, reputation = 7, ability = 4, leaderTrust = -3)
            ),
            c(
                "按汇报口径稳妥呈现", Attitude.STEADY,
                "接待周全，汇报得体。调研组满意，单位领导也满意。",
                AttrDelta(coordination = 5, politics = 5, leaderTrust = 5, writing = 3)
            ),
            c(
                "安排参观示范点位", Attitude.CAUTIOUS,
                "调研很顺利，点位效果很好。只是被问到几个具体数字时，你答得有点含糊。",
                AttrDelta(leaderTrust = 4, politics = 3, reputation = -3, work = -2)
            )
        )
    ),
    ev(
        id = "joint_enforcement", title = "联合执法",
        background = "多部门联合整治行动今晚开始",
        description = "被检查的对象里有熟面孔，也有情绪激动的商户。",
        tags = tags(EventTag.COORDINATION, EventTag.EMERGENCY),
        weight = 9,
        minPos = 1,
        choices = listOf(
            c(
                "全程带队，严格按标准执行", Attitude.BOLD,
                "行动干净利落，没有出意外。有人当场骂了你，第二天却主动来补办手续。",
                AttrDelta(work = 7, coordination = 5, ability = 4, reputation = 4, energy = -6),
                HiddenDelta(riskPressure = 5)
            ),
            c(
                "先宣传告知，再集中整治", Attitude.STEADY,
                "你们提前三天发了告知书。整治当天抵触明显减少，成效也不差。",
                AttrDelta(coordination = 7, masses = 5, work = 4, colleagues = 3)
            ),
            c(
                "按名单完成任务即可", Attitude.CAUTIOUS,
                "名单上的都查了，名单外的没管。任务完成，效果一般。",
                AttrDelta(work = 1, coordination = 2, ability = -2, reputation = -3)
            )
        )
    ),
    ev(
        id = "cadre_training", title = "干部培训",
        background = "有一期为期一个月的培训班",
        description = "名额有限，去学习意味着暂时离开手头的工作。",
        tags = tags(EventTag.MEETING),
        weight = 9,
        choices = listOf(
            c(
                "争取名额，认真学满全程", Attitude.BOLD,
                "你在班上做了两次发言，认识了不少外单位的同行。回来时工作堆了一桌。",
                AttrDelta(politics = 6, ability = 5, writing = 4, energy = 3, work = -3),
                HiddenDelta(network = 8, mentor = 3)
            ),
            c(
                "参加培训，同时兼顾工作", Attitude.STEADY,
                "你晚上回单位处理紧急事项，两边都没有耽误太多。",
                AttrDelta(politics = 4, ability = 4, coordination = 3, work = 1, energy = -3)
            ),
            c(
                "把名额让给其他同事", Attitude.CAUTIOUS,
                "同事去了，回来后在会上作了交流发言。你继续守着自己的那一摊事。",
                AttrDelta(colleagues = 5, work = 4, politics = -2),
                HiddenDelta(opportunity = -4)
            )
        )
    ),
    ev(
        id = "democratic_review", title = "民主测评",
        background = "年度民主测评正在进行",
        description = "同事们会给你打分。过去一年你怎么做，这张票上都有影子。",
        tags = tags(EventTag.ASSESSMENT, EventTag.MEETING),
        weight = 25,
        minPos = 1,
        choices = listOf(
            c(
                "平时怎么做，现在就怎么面对", Attitude.STEADY,
                "你没有做任何额外动作。结果出来时，分数不低，也不虚高。",
                AttrDelta(reputation = 3, colleagues = 3, leaderTrust = 2)
            ),
            c(
                "主动与同事沟通，说明工作思路", Attitude.BOLD,
                "你在会前找了很多人聊。测评分数上升了，也有人觉得你太在意。",
                AttrDelta(colleagues = 4, coordination = 3, leaderTrust = 3, reputation = -2),
                HiddenDelta(network = 5)
            ),
            c(
                "请熟悉的同事帮忙打招呼", Attitude.RISKY,
                "分数确实好看了。但拉票这种事，总有人会记住。",
                AttrDelta(reputation = 5, colleagues = -4, integrityRisk = 8, politics = -4),
                HiddenDelta(riskPressure = 8)
            )
        )
    ),
    ev(
        id = "internal_audit", title = "内部审计",
        background = "审计组进驻你所在的部门",
        description = "几笔几年前的账目需要解释，经手的不是一个人。",
        tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY),
        weight = 9,
        minRisk = 20,
        choices = listOf(
            c(
                "主动说明情况，配合核查", Attitude.STEADY,
                "你把能提供的材料全部整理好，包括当时的判断依据。审计组认可了你的配合。",
                AttrDelta(politics = 6, integrityRisk = -4, leaderTrust = 2, work = 2)
            ),
            c(
                "只回答被问到的问题", Attitude.CAUTIOUS,
                "你回答了所有提问，没有多说一个字。审计报告里，你的部分没有异常。",
                AttrDelta(politics = 3, integrityRisk = -2)
            ),
            c(
                "把责任推给当时的经办人", Attitude.RISKY,
                "你撇清了自己。事情过去了，但那位同事后来再也没有和你说过话。",
                AttrDelta(colleagues = -9, reputation = -6, integrityRisk = 5),
                HiddenDelta(network = -6, riskPressure = 4)
            )
        )
    ),
    ev(
        id = "media_interview", title = "媒体采访",
        background = "记者突然出现在办公楼门口",
        description = "对方想了解一项正在推进的民生工作，没有提前预约。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.RESEARCH),
        weight = 8,
        minPos = 2,
        choices = listOf(
            c(
                "坦诚回应并可核实内容", Attitude.BOLD,
                "你讲了进展，也讲了难处。报道出来后，评价比预想的好。",
                AttrDelta(reputation = 8, masses = 5, politics = 4),
                HiddenDelta(socialInfluence = 8)
            ),
            c(
                "联系宣传部门统一口径", Attitude.STEADY,
                "采访在规范的流程下完成，内容四平八稳，没有出错。",
                AttrDelta(politics = 4, coordination = 4, reputation = 2)
            ),
            c(
                "婉拒采访，请对方走流程", Attitude.CAUTIOUS,
                "记者离开了。第二天出现了一篇语气平淡但并不友好的短稿。",
                AttrDelta(reputation = -4, politics = 2),
                HiddenDelta(socialInfluence = -4)
            )
        )
    ),
    ev(
        id = "visiting_day", title = "群众来访接待日",
        background = "今天是领导接待日，也是你的值班日",
        description = "来访的十几批群众里，有三批的问题本不该由你回答。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.PETITION),
        weight = 10,
        choices = listOf(
            c(
                "每一批都认真听完并记录", Attitude.BOLD,
                "你从早上九点坐到下午两点，记满了两个本子。有人临走时鞠了一躬。",
                AttrDelta(masses = 8, work = 4, reputation = 5, energy = -7)
            ),
            c(
                "分类处理，转办与答复并行", Attitude.STEADY,
                "你把问题分为当场答复和转办两类，效率明显提高。",
                AttrDelta(coordination = 6, masses = 4, work = 4, politics = 2)
            ),
            c(
                "请群众按程序预约再来", Attitude.CAUTIOUS,
                "秩序很好，接待日按时结束。有几批群众在门口站了很久才走。",
                AttrDelta(masses = -6, politics = 2, reputation = -4, energy = 2)
            )
        )
    ),
    ev(
        id = "aftermath", title = "突发事件善后",
        background = "事故后的善后工作刚刚开始",
        description = "赔偿、安置、家属情绪、后续整改，每一项都不轻松。",
        tags = tags(EventTag.EMERGENCY, EventTag.COORDINATION),
        weight = 9,
        minPos = 1,
        choices = listOf(
            c(
                "一户一户上门谈", Attitude.BOLD,
                "你陪着家属一条条过政策，谈到第七天终于签完了协议。你瘦了四斤。",
                AttrDelta(work = 7, masses = 6, ability = 4, health = -6, energy = -8),
                HiddenDelta(stress = 10)
            ),
            c(
                "依法依规统一口径处理", Attitude.STEADY,
                "处理过程规范，没有出纰漏。少数家属觉得缺少温度，但结果公正。",
                AttrDelta(politics = 5, work = 5, integrityRisk = -3, coordination = 3)
            ),
            c(
                "交由专门工作组处理", Attitude.CAUTIOUS,
                "工作组接手了后续。你回到日常岗位，心里松了一口气，也有一点空。",
                AttrDelta(work = -2, energy = 3, leaderTrust = -2),
                HiddenDelta(stress = -4)
            )
        )
    ),
    ev(
        id = "year_end_summary", title = "年终总结",
        background = "又到了写年终总结的时候",
        description = "这一年做了什么、没做什么，要落在纸上。写实还是写好看，由你决定。",
        tags = tags(EventTag.MATERIAL, EventTag.MEETING),
        weight = 14,
        repeatable = true,
        cooldown = 4,
        choices = listOf(
            c(
                "如实盘点，把问题写进去", Attitude.BOLD,
                "你把失误也写进了总结。领导看了很久，说这份材料有价值。",
                AttrDelta(politics = 5, writing = 5, ability = 4, reputation = 4, leaderTrust = 2)
            ),
            c(
                "突出成绩，问题一笔带过", Attitude.STEADY,
                "材料四平八稳，成绩写得充分。总结会被当作范本传阅了一次。",
                AttrDelta(writing = 6, politics = 4, work = 3, ability = 2)
            ),
            c(
                "照着去年的模板改数据", Attitude.CAUTIOUS,
                "你两个小时就交了稿，没有人提出问题。这份总结和去年有七成相似。",
                AttrDelta(writing = -2, work = -3, ability = -2, energy = 4)
            )
        )
    ),
    ev(
        id = "rotation_offer", title = "岗位轮换机会",
        background = "组织上问你是否愿意换一个岗位",
        description = "新岗位更累，但接触面更广；留在原岗位熟悉、稳当，也容易被忽略。",
        tags = tags(EventTag.LEADER_TALK, EventTag.CAREER),
        weight = 9,
        minPos = 1,
        choices = listOf(
            c(
                "接受轮换，去压力最大的岗位", Attitude.BOLD,
                "你收拾东西去了新部门。第一周就发现，这里的问题比想象的多。",
                AttrDelta(ability = 7, coordination = 5, work = 4, energy = -7, health = -4),
                HiddenDelta(opportunity = 10, network = 6, stress = 8)
            ),
            c(
                "先了解情况再决定", Attitude.STEADY,
                "你找新部门的同事聊了两天，最后接受了安排，并提前做了准备。",
                AttrDelta(ability = 5, politics = 4, coordination = 3, work = 2),
                HiddenDelta(opportunity = 5)
            ),
            c(
                "留在熟悉岗位继续深耕", Attitude.CAUTIOUS,
                "你留了下来。业务更加熟练，但能见到的人和事，也就那么多了。",
                AttrDelta(work = 4, ability = 2, coordination = -2),
                HiddenDelta(opportunity = -6, network = -3)
            )
        )
    ),
    ev(
        id = "secondment", title = "挂职锻炼",
        background = "有一个到上级机关挂职一年的名额",
        description = "离开熟悉的环境，去一个完全陌生的部门，从头证明自己。",
        tags = tags(EventTag.CAREER, EventTag.LEADER_TASK),
        weight = 8,
        minPos = 2,
        choices = listOf(
            c(
                "主动报名参加挂职", Attitude.BOLD,
                "你在新单位从最基础的事做起。一年后回来，你的材料能力和视野都不一样了。",
                AttrDelta(ability = 6, writing = 6, politics = 5, work = 3, family = -5, energy = -6),
                HiddenDelta(mentor = 9, network = 9, opportunity = 8)
            ),
            c(
                "报名，但先与家人商量好", Attitude.STEADY,
                "你把家里安顿好才走。挂职期间没有后顾之忧，表现稳定。",
                AttrDelta(ability = 5, writing = 4, politics = 4, family = -2),
                HiddenDelta(mentor = 5, network = 6)
            ),
            c(
                "放弃这次机会", Attitude.CAUTIOUS,
                "名额给了别人。你继续做熟悉的事，家里也少了一次折腾。",
                AttrDelta(family = 4, energy = 3, work = 2),
                HiddenDelta(opportunity = -8, mentor = -4)
            )
        )
    ),
    ev(
        id = "old_colleague_favor", title = "老同事求助",
        background = "退休多年的老同事找到你",
        description = "他的一件小事卡在流程里，按规矩能办但很慢，打个招呼能快很多。",
        tags = tags(EventTag.GIFT, EventTag.COLLEAGUE),
        weight = 9,
        choices = listOf(
            c(
                "帮忙盯进度，但不越流程", Attitude.STEADY,
                "你打电话问了进度，也把流程讲给老人听。事情在两周内办完了。",
                AttrDelta(colleagues = 5, reputation = 4, masses = 3, politics = 3)
            ),
            c(
                "直接找经办人优先办理", Attitude.BOLD,
                "事情当天就办好了，老人连声道谢。经办人嘴上没说什么。",
                AttrDelta(colleagues = 4, reputation = 3, integrityRisk = 5, politics = -2),
                HiddenDelta(network = 4, riskPressure = 4)
            ),
            c(
                "按规定告知只能等", Attitude.CAUTIOUS,
                "你把政策讲得很清楚，但老人离开时的背影让你有点难受。",
                AttrDelta(politics = 3, colleagues = -3, masses = -2, reputation = -2)
            )
        )
    ),
    ev(
        id = "weekend_choice", title = "周末的安排",
        background = "这个周末，工作和家里都需要你",
        description = "手上有材料要赶，家里也早就说好了要一起出门。时间只有一份。",
        tags = tags(EventTag.FAMILY, EventTag.OVERTIME),
        weight = 12,
        repeatable = true,
        cooldown = 6,
        choices = listOf(
            c(
                "陪家人，把手头的事往后放一放", Attitude.STEADY,
                "你关掉手机陪了一整天。家里人的情绪明显好了，材料在周一早上才赶完。",
                AttrDelta(family = 9, energy = 4, health = 2, work = -2, leaderTrust = -1)
            ),
            c(
                "加班把材料赶出来", Attitude.BOLD,
                "材料按时交了，领导在会上点名肯定。你回到家时，家里人已经吃完晚饭。",
                AttrDelta(work = 6, leaderTrust = 4, family = -6, health = -4, energy = -6),
                HiddenDelta(stress = 6)
            ),
            c(
                "先陪家人，晚上再回单位补", Attitude.CAUTIOUS,
                "你把两边都尽量顾上。家里满意了一半，材料也勉强赶上了。",
                AttrDelta(family = 3, work = 2, energy = -3, health = -1)
            )
        )
    )
)

// 第三批：责任、家庭、以及全部事件链节点。
private val eventsGroupThree: List<GameEvent> = listOf(
    ev(
        id = "subordinate_error", title = "下级失误担责",
        background = "科室报出的材料出了明显错误",
        description = "具体经办的是刚来两年的年轻人，责任在你这一级。领导已经知道了。",
        tags = tags(EventTag.COLLEAGUE, EventTag.MATERIAL),
        weight = 9,
        minPos = 3,
        choices = listOf(
            c(
                "先把责任担下来，再内部整改", Attitude.BOLD,
                "你在会上把责任揽了过来，回去后和年轻人一起梳理流程。他后来做事稳了很多。",
                AttrDelta(leaderTrust = 4, colleagues = 7, reputation = 5, work = -3, politics = 4)
            ),
            c(
                "说明情况并推动流程改进", Attitude.STEADY,
                "你把差错原因讲清楚，同时拿出改进方案。领导认可了处理方式。",
                AttrDelta(politics = 4, work = 3, ability = 3, colleagues = 3)
            ),
            c(
                "如实指出经办人责任", Attitude.CAUTIOUS,
                "责任划分清楚了。年轻人被通报批评，之后见到你会绕开走。",
                AttrDelta(politics = 3, colleagues = -7, reputation = -4, leaderTrust = 2)
            )
        )
    ),
    ev(
        id = "high_temp_care", title = "高温作业慰问",
        background = "连续高温，一线作业人员仍在户外",
        description = "上级要求开展慰问，也要求如实上报防暑措施落实情况。",
        tags = tags(EventTag.MASS_APPEAL, EventTag.RURAL),
        weight = 9,
        choices = listOf(
            c(
                "到现场一起待上一整天", Attitude.BOLD,
                "你在工地上待了一整天，把防暑物资一件件发到人手里。晚上你的胳膊晒脱了皮。",
                AttrDelta(masses = 7, reputation = 5, health = -4, work = 4, energy = -6)
            ),
            c(
                "督促落实防暑措施并检查记录", Attitude.STEADY,
                "你逐条检查了措施落实情况，也补齐了台账。一线工人拿到了物资。",
                AttrDelta(work = 5, coordination = 4, masses = 4, politics = 3)
            ),
            c(
                "按文件要求完成慰问流程", Attitude.CAUTIOUS,
                "照片拍了，材料交了，流程完整。一位工人问了一句「明天还来吗」。",
                AttrDelta(politics = 3, writing = 3, masses = -4, reputation = -3)
            )
        )
    ),
    ev(
        id = "family_moment", title = "家庭重要时刻",
        background = "家人有一个重要的日子",
        description = "同一天，单位有一个不能缺席的会。两边都在等你。",
        tags = tags(EventTag.FAMILY),
        weight = 12,
        choices = listOf(
            c(
                "请半天假，去参加家庭的事", Attitude.BOLD,
                "你赶上了那个时刻。家人笑得很开心，你在回单位的路上一直在看手机。",
                AttrDelta(family = 10, energy = 3, leaderTrust = -3, work = -1)
            ),
            c(
                "先参加会议，晚上赶过去", Attitude.STEADY,
                "你把两头都尽量顾上。会议没耽误，家里也只迟到了半小时。",
                AttrDelta(family = 3, work = 2, coordination = 2, energy = -3)
            ),
            c(
                "留在单位，事后补偿家人", Attitude.CAUTIOUS,
                "会议开得很成功。你回家时，饭桌上的菜已经收进了冰箱。",
                AttrDelta(work = 4, leaderTrust = 4, family = -9),
                HiddenDelta(stress = 6),
                chain = "family", flag = "family_debt"
            )
        )
    ),
    ev(
        id = "road_start", title = "道路项目启动",
        background = "群众反映多年的道路问题终于立项",
        description = "项目有了资金和计划，但怎么干、干成什么样，需要有人从头盯到底。",
        tags = tags(EventTag.PROJECT, EventTag.MASS_APPEAL, EventTag.RURAL),
        weight = 12,
        minPos = 1,
        choices = listOf(
            c(
                "主动接手，把项目抓在手里", Attitude.BOLD,
                "你成了项目的实际负责人。开工那天，村里的老人在路边看了很久。",
                AttrDelta(work = 8, ability = 4, masses = 6, energy = -7, health = -4),
                HiddenDelta(opportunity = 7, stress = 7),
                chain = "road", flag = "road_push"
            ),
            c(
                "参与推进，按程序稳步实施", Attitude.STEADY,
                "你按程序盯进度、盯质量。工程不快，但每一步都有记录。",
                AttrDelta(work = 5, coordination = 4, politics = 3, energy = -4)
            ),
            c(
                "把项目交给更专业的同事", Attitude.CAUTIOUS,
                "项目交给了别人。你继续做原来的工作，偶尔会听到那边的进展。",
                AttrDelta(colleagues = 4, work = -2, energy = 3),
                HiddenDelta(opportunity = -6)
            )
        )
    ),
    ev(
        id = "road_funding", title = "道路资金出现问题",
        background = "项目开工后发现资金存在缺口",
        description = "材料商开始催款，施工队情绪不稳，群众每天在路边看进度。",
        tags = tags(EventTag.PROJECT, EventTag.INTEGRITY),
        weight = 0,
        chain = "road", step = 1, after = 3,
        choices = listOf(
            c(
                "向上争取追加资金", Attitude.BOLD,
                "你带着材料跑了三个部门，追加资金在半个月后到账。工程保住了进度。",
                AttrDelta(work = 6, coordination = 6, ability = 5, energy = -6),
                HiddenDelta(network = 6, mentor = 4),
                chain = "road", flag = "road_support"
            ),
            c(
                "严格按程序压缩成本", Attitude.STEADY,
                "你重新核了预算，砍掉了不必要的开支，质量要求一条没降。",
                AttrDelta(work = 5, politics = 5, ability = 4, integrityRisk = -3),
                chain = "road", flag = "road_quality"
            ),
            c(
                "接受材料商提出的更便宜的方案", Attitude.RISKY,
                "成本降下来了，工程继续推进。只有你知道，那批材料比标准薄了一点。",
                AttrDelta(work = 3, wealth = 6, integrityRisk = 15),
                HiddenDelta(riskPressure = 15),
                chain = "road", flag = "road_cheap",
                hint = "省了钱，留下了隐患"
            )
        )
    ),
    ev(
        id = "road_check", title = "上级检查",
        background = "道路项目被列入抽查名单",
        description = "检查组要看资料、看现场、看资金流向。之前的每个决定都会被翻出来。",
        tags = tags(EventTag.INSPECTION, EventTag.PROJECT),
        weight = 0,
        chain = "road", step = 2, after = 4,
        choices = listOf(
            c(
                "全程陪同，如实说明情况", Attitude.BOLD,
                "你把过程和判断依据都讲清楚了。检查组留下了整改意见，但没有发现原则问题。",
                AttrDelta(politics = 5, work = 4, reputation = 4, ability = 3),
                bonus = listOf(
                    ConditionalEffect(
                        requiredFlag = "road_cheap",
                        effects = AttrDelta(integrityRisk = 12, reputation = -8, politics = -5),
                        hiddenEffects = HiddenDelta(riskPressure = 15),
                        extraOutcome = "而那批材料的问题，也在这次检查中被抽查了出来。"
                    )
                )
            ),
            c(
                "提前整改，把问题消化在现场", Attitude.STEADY,
                "你连夜组织返工与补资料。检查顺利通过，代价是两个月几乎没有休息。",
                AttrDelta(work = 6, energy = -8, health = -5, integrityRisk = -4),
                bonus = listOf(
                    ConditionalEffect(
                        requiredFlag = "road_quality",
                        effects = AttrDelta(work = 3, reputation = 4),
                        extraOutcome = "因为你一直坚持质量要求，整改的工作量比预想小得多。"
                    )
                )
            ),
            c(
                "把汇报重点放在成效上", Attitude.CAUTIOUS,
                "汇报很顺利，检查也过关了。资料里那几个对不上的数字，暂时没人追问。",
                AttrDelta(politics = 3, writing = 3, integrityRisk = 5),
                HiddenDelta(riskPressure = 6)
            )
        )
    ),
    ev(
        id = "road_finish", title = "项目收官",
        background = "道路终于修到了最后一段",
        description = "路通那天，村里放了鞭炮。你的名字写在项目公示牌上。",
        tags = tags(EventTag.PROJECT, EventTag.ACCEPTANCE),
        weight = 0,
        chain = "road", step = 3, after = 3,
        choices = listOf(
            c(
                "认真做完整套收尾工作", Attitude.STEADY,
                "你把资料、验收、移交一项项做完。这条路后来被当作示范项目。",
                AttrDelta(work = 8, reputation = 7, masses = 5, ability = 4),
                bonus = listOf(
                    ConditionalEffect(
                        requiredFlag = "road_quality",
                        effects = AttrDelta(reputation = 5, work = 3, leaderTrust = 4),
                        extraOutcome = "当初坚持质量的那次决定，让你今天可以坦然面对所有人。"
                    ),
                    ConditionalEffect(
                        requiredFlag = "road_cheap",
                        effects = AttrDelta(integrityRisk = 10, reputation = -6),
                        extraOutcome = "但你知道，路下面埋着一笔旧账。"
                    )
                )
            ),
            c(
                "总结经验，向上面推荐做法", Attitude.BOLD,
                "你把经验写成了材料，在更大范围推广。同行来参观时，你站在路边讲解。",
                AttrDelta(work = 7, writing = 6, reputation = 8, leaderTrust = 5),
                HiddenDelta(socialInfluence = 6, opportunity = 6)
            ),
            c(
                "完成验收，尽快投入新工作", Attitude.CAUTIOUS,
                "验收通过，你很快转向下一摊工作。这条路的后续管护，你不太清楚了。",
                AttrDelta(work = 4, ability = 2, reputation = 1)
            )
        )
    ),
    ev(
        id = "project_bid", title = "招标争议",
        background = "项目招标出现争议",
        description = "一家未中标企业提出质疑，材料里有几处说法对你并不有利。",
        tags = tags(EventTag.PROJECT, EventTag.INTEGRITY),
        weight = 0,
        chain = "project", step = 1, after = 3,
        choices = listOf(
            c(
                "公开全过程资料，接受质疑", Attitude.BOLD,
                "你把评分依据、评审记录全部摊开。质疑不成立，你的公信力反而更强了。",
                AttrDelta(politics = 6, reputation = 7, integrityRisk = -5, work = 4)
            ),
            c(
                "重新组织复核，补齐程序", Attitude.STEADY,
                "你申请重新复核。程序上找出了两处瑕疵并作了补正，项目继续推进。",
                AttrDelta(politics = 5, work = 4, ability = 4, coordination = 3)
            ),
            c(
                "私下与质疑企业沟通撤诉", Attitude.RISKY,
                "对方同意不再追究。事情平息了，但这次沟通本身留下了新的问题。",
                AttrDelta(integrityRisk = 14, colleagues = -3),
                HiddenDelta(riskPressure = 14, stress = 8),
                chain = "project", flag = "project_shadow"
            )
        )
    ),
    ev(
        id = "project_adjust", title = "项目中期调整",
        background = "项目进行到一半，需要调整方案",
        description = "上级要求压缩工期，你判断这样做会牺牲一部分质量。",
        tags = tags(EventTag.PROJECT, EventTag.LEADER_TASK),
        weight = 0,
        chain = "project", step = 2, after = 4,
        choices = listOf(
            c(
                "书面说明风险，坚持质量优先", Attitude.BOLD,
                "你把风险写成了正式报告。工期末能压缩，但项目保住了底线。",
                AttrDelta(politics = 5, ability = 5, work = 4, leaderTrust = -3, reputation = 5),
                chain = "project", flag = "project_quality"
            ),
            c(
                "按期完成，同时增加投入保质量", Attitude.STEADY,
                "你通过增加人手和夜班保住了工期与质量。预算超了一点，但没有出问题。",
                AttrDelta(work = 6, coordination = 5, energy = -7, health = -4),
                HiddenDelta(stress = 7)
            ),
            c(
                "按上级要求压缩工期", Attitude.CAUTIOUS,
                "工期如期完成。几个月后，几处细节开始出现小问题。",
                AttrDelta(work = 3, leaderTrust = 4, integrityRisk = 8, reputation = -3),
                HiddenDelta(riskPressure = 8),
                chain = "project", flag = "project_shadow"
            )
        )
    ),
    ev(
        id = "project_result", title = "项目落地见效",
        background = "项目正式投入使用",
        description = "这一步会检验前面所有决定的结果。",
        tags = tags(EventTag.PROJECT, EventTag.ACCEPTANCE),
        weight = 0,
        chain = "project", step = 3, after = 3,
        choices = listOf(
            c(
                "继续跟踪运行效果", Attitude.STEADY,
                "你在半年里跑了六七次现场，把问题一条条解决。项目运行得很稳。",
                AttrDelta(work = 8, ability = 4, reputation = 5, energy = -5),
                bonus = listOf(
                    ConditionalEffect(
                        requiredFlag = "project_quality",
                        effects = AttrDelta(reputation = 6, leaderTrust = 5, work = 3),
                        extraOutcome = "当初坚持质量的判断，在这次运行中得到了验证。"
                    ),
                    ConditionalEffect(
                        requiredFlag = "project_shadow",
                        effects = AttrDelta(integrityRisk = 12, reputation = -8),
                        hiddenEffects = HiddenDelta(riskPressure = 12),
                        extraOutcome = "运行中暴露的问题，把之前的几处妥协重新翻了出来。"
                    )
                )
            ),
            c(
                "总结经验并形成制度", Attitude.BOLD,
                "你把项目管理经验固化成了一套流程，后来被好几个部门借用。",
                AttrDelta(writing = 7, work = 6, ability = 5, reputation = 7, leaderTrust = 4)
            ),
            c(
                "移交运维，转向新任务", Attitude.CAUTIOUS,
                "项目顺利移交。你转身投入下一项工作，运行好坏不再由你操心。",
                AttrDelta(work = 4, ability = 2, energy = 3, reputation = 1)
            )
        )
    ),
    ev(
        id = "rural_industry", title = "产业项目落地",
        background = "村里的产业项目开始见效益",
        description = "第一批产品卖出去了，但分配方式和后续投入，村里争论得很厉害。",
        tags = tags(EventTag.RURAL, EventTag.PROJECT),
        weight = 0,
        chain = "rural", step = 1, after = 4,
        choices = listOf(
            c(
                "组织村民代表共同议定分配方案", Attitude.STEADY,
                "会开了三个晚上，方案最终由村民自己定下来。虽然慢，但没有人再说不公平。",
                AttrDelta(masses = 8, coordination = 6, work = 5, energy = -5)
            ),
            c(
                "先把收益投入再生产", Attitude.BOLD,
                "你主张先扩大规模。第二年产能翻倍，但也有村民抱怨没见到现钱。",
                AttrDelta(work = 8, ability = 4, masses = -3, reputation = 3),
                HiddenDelta(opportunity = 5)
            ),
            c(
                "按上级模板平均分配", Attitude.CAUTIOUS,
                "分配很快完成，没有争议。产业的下一步该怎么办，没有人提出来。",
                AttrDelta(politics = 4, masses = 2, ability = -2, work = 1)
            )
        )
    ),
    ev(
        id = "rural_dividend", title = "分红争议",
        background = "产业分红名单引发上访",
        description = "几户村民认为名单不公平，情绪激动地找到了村部。",
        tags = tags(EventTag.RURAL, EventTag.PETITION),
        weight = 0,
        chain = "rural", step = 2, after = 4,
        choices = listOf(
            c(
                "逐户走访，重新核对名单", Attitude.STEADY,
                "你用了十天逐户核对，调整了七户。最终没有人再上访。",
                AttrDelta(masses = 8, work = 5, reputation = 6, energy = -6, health = -3)
            ),
            c(
                "公开全部账目接受质询", Attitude.BOLD,
                "你把账目贴在村口，开了公开说明会。质疑声慢慢消失了。",
                AttrDelta(politics = 5, reputation = 7, masses = 6, integrityRisk = -4)
            ),
            c(
                "维持原方案，解释政策依据", Attitude.CAUTIOUS,
                "你讲了政策，讲得很清楚。上访的人走了，但村里的议论持续了很久。",
                AttrDelta(politics = 3, masses = -6, reputation = -5),
                HiddenDelta(riskPressure = 6)
            )
        )
    ),
    ev(
        id = "petition_followup", title = "信访回访",
        background = "两个月前承诺的事情要回访",
        description = "你当时答应限期解决，现在期限到了，问题只解决了一半。",
        tags = tags(EventTag.PETITION, EventTag.MASS_APPEAL),
        weight = 0,
        chain = "petition", step = 1, after = 3,
        choices = listOf(
            c(
                "登门说明进度，并把问题转成项目", Attitude.BOLD,
                "你把半成品的进度讲清楚，并推动把问题纳入了一个正式项目。对方表示理解。",
                AttrDelta(work = 5, masses = 5, reputation = 5, coordination = 4),
                HiddenDelta(network = 4, opportunity = 4),
                chain = "road", flag = "petition_resolved"
            ),
            c(
                "继续按流程跟进", Attitude.STEADY,
                "你把情况记录在案，继续督办。事情在缓慢推进。",
                AttrDelta(politics = 3, work = 3, coordination = 3, masses = 2)
            ),
            c(
                "解释客观困难，请对方再等", Attitude.CAUTIOUS,
                "对方沉默地听完了你的解释，然后说「我明白了」。你知道那不是理解。",
                AttrDelta(masses = -7, reputation = -5, work = -3),
                HiddenDelta(stress = 5)
            )
        )
    ),
    ev(
        id = "integrity_talk", title = "廉政谈话",
        background = "纪检部门请你谈一次话",
        description = "谈话是提醒性质，但提到了一些「反映」和「线索」。",
        tags = tags(EventTag.INTEGRITY, EventTag.INSPECTION),
        weight = 0,
        chain = "integrity", step = 1, after = 2,
        choices = listOf(
            c(
                "如实说明，主动上交问题", Attitude.BOLD,
                "你把那件事原原本本讲了出来，并把东西交了上去。谈话结束时，气氛比开始时轻松。",
                AttrDelta(politics = 6, integrityRisk = -10),
                HiddenDelta(riskPressure = -10, stress = -4),
                chain = "integrity", flag = "integrity_honest"
            ),
            c(
                "说明情况，配合核实", Attitude.STEADY,
                "你配合提供了全部材料，没有隐瞒也没有多说。核实结果与你所说一致。",
                AttrDelta(politics = 4, integrityRisk = -5),
                chain = "integrity", flag = "integrity_cooperate"
            ),
            c(
                "否认并强调自己没有违规", Attitude.RISKY,
                "你坚持说自己没有问题。谈话结束了，但事情并没有结束。",
                AttrDelta(integrityRisk = 12, politics = -4),
                HiddenDelta(riskPressure = 14, stress = 12),
                chain = "integrity", flag = "integrity_deny"
            )
        )
    ),
    ev(
        id = "integrity_investigation", title = "组织调查",
        background = "组织决定对相关问题进行核实",
        description = "调查组已经进驻，同事们的目光变得复杂。",
        tags = tags(EventTag.INTEGRITY, EventTag.INSPECTION),
        weight = 0,
        chain = "integrity", step = 2, after = 4,
        choices = listOf(
            c(
                "全面配合，交出全部材料", Attitude.BOLD,
                "你把经手的所有事情都整理成清单交给调查组。过程漫长，但你心里越来越清楚。",
                AttrDelta(politics = 6, integrityRisk = -8, reputation = 3),
                HiddenDelta(stress = -4),
                bonus = listOf(
                    ConditionalEffect(
                        requiredFlag = "integrity_honest",
                        effects = AttrDelta(integrityRisk = -10, reputation = 6),
                        extraOutcome = "你早先主动说明的那次谈话，成为这次核实中最关键的依据。"
                    )
                )
            ),
            c(
                "保持沉默，等待结论", Attitude.CAUTIOUS,
                "你没有多做解释。调查持续了几个月，你的工作几乎停摆。",
                AttrDelta(work = -8, energy = -6, integrityRisk = 4),
                HiddenDelta(stress = 10)
            ),
            c(
                "四处打听，找人帮忙说话", Attitude.RISKY,
                "你托了几个人。有人愿意帮忙，也有人把这件事记在了心里。",
                AttrDelta(integrityRisk = 10, reputation = -6, colleagues = -5),
                HiddenDelta(riskPressure = 12, network = -4)
            )
        )
    ),
    ev(
        id = "integrity_result", title = "处理结果",
        background = "调查结论即将公布",
        description = "无论结果如何，这件事都会留在你的档案里。",
        tags = tags(EventTag.INTEGRITY),
        weight = 0,
        chain = "integrity", step = 3, after = 2,
        choices = listOf(
            c(
                "接受结论，继续做好本职", Attitude.STEADY,
                "结论出来了。你按照规定接受了处理，也把剩下的工作交接得清清楚楚。",
                AttrDelta(politics = 5, reputation = 3, integrityRisk = -12, work = -2),
                bonus = listOf(
                    ConditionalEffect(
                        requiredFlag = "integrity_honest",
                        effects = AttrDelta(reputation = 6, leaderTrust = 4),
                        extraOutcome = "由于你此前主动说明并退交，处理从轻，组织上保留了你的岗位。"
                    ),
                    ConditionalEffect(
                        requiredFlag = "integrity_deny",
                        effects = AttrDelta(reputation = -10, leaderTrust = -8, work = -6),
                        extraOutcome = "而此前否认与回避的态度，被明确写进了结论。"
                    )
                )
            ),
            c(
                "主动申请调整岗位，重新开始", Attitude.BOLD,
                "你申请离开原来的岗位。新岗位级别不高，但没有了过去那些眼神。",
                AttrDelta(work = -5, reputation = -3, integrityRisk = -10, health = 3),
                HiddenDelta(stress = -6)
            ),
            c(
                "提出申诉", Attitude.RISKY,
                "申诉程序启动了。结果没有改变，但你因此又多花了两年时间。",
                AttrDelta(work = -6, reputation = -5, energy = -6),
                HiddenDelta(riskPressure = 8, stress = 10)
            )
        )
    ),
    ev(
        id = "family_repair", title = "家庭的账单",
        background = "长期缺席之后，家人开口了",
        description = "这一次不是抱怨，而是一次认真的谈话：你打算怎么办。",
        tags = tags(EventTag.FAMILY),
        weight = 0,
        chain = "family", step = 1, after = 4,
        choices = listOf(
            c(
                "重新安排时间，把家庭放回日程", Attitude.STEADY,
                "你开始每周留出固定的时间。工作没有少做，但家里重新有了笑声。",
                AttrDelta(family = 14, health = 4, energy = 4, work = -3, leaderTrust = -2)
            ),
            c(
                "带家人一起出门几天", Attitude.BOLD,
                "你请了年假，一家人出去待了五天。回来后，很多话说开了。",
                AttrDelta(family = 18, health = 5, energy = 5, work = -5, leaderTrust = -4),
                HiddenDelta(stress = -10)
            ),
            c(
                "承诺以后补偿，但现在走不开", Attitude.CAUTIOUS,
                "你说完这句话，对方没有再说什么。家里的安静从此变成了另一种安静。",
                AttrDelta(family = -10, work = 4),
                HiddenDelta(stress = 8)
            )
        )
    )
)

// 第四批：只在隐性状态积累到一定程度后才会出现的事件。
// 它们是暗线机制的出口：让压力、健康隐患、廉政压力、社会影响与人际网络
// 变成玩家真正能遇到的场面。
private val eventsGroupFour: List<GameEvent> = listOf(
    ev(
        id = "health_check", title = "体检通知",
        background = "单位组织年度体检",
        description = "体检报告上出现了几个需要复查的指标。医生建议你尽快做进一步检查，" +
            "而这一周正好有一项重点工作在你手上。",
        tags = tags(EventTag.HEALTH),
        weight = 14,
        repeatable = true, cooldown = 16,
        minHealthRisk = 45,
        choices = listOf(
            c(
                "请假去做进一步检查", Attitude.STEADY,
                "检查结果没有大问题，但医生让你必须调整作息。你把这件事记进了日程表。",
                AttrDelta(health = 8, energy = 4, work = -3),
                HiddenDelta(healthRisk = -20, stress = -4)
            ),
            c(
                "先把工作忙完，复查往后放一放", Attitude.CAUTIOUS,
                "你把报告折好放进抽屉。几个月后整理东西时又翻出来，已经过了复查建议的期限。",
                AttrDelta(work = 5, health = -6),
                HiddenDelta(healthRisk = 12, stress = 5)
            ),
            c(
                "一边工作一边抽空处理", Attitude.BOLD,
                "你利用两个下午跑完了复查，工作只受了一点影响，但也确实都赶上了。",
                AttrDelta(work = 2, health = -2),
                HiddenDelta(healthRisk = -6)
            )
        )
    ),
    ev(
        id = "old_matter", title = "旧事重提",
        background = "巡察组进驻",
        description = "一份几年前的项目材料被重新调阅。当时是你签的字，" +
            "其中几处程序上的瑕疵，至今说不清楚。",
        tags = tags(EventTag.INTEGRITY, EventTag.INSPECTION),
        weight = 13,
        repeatable = true, cooldown = 20,
        minRiskPressure = 45,
        choices = listOf(
            c(
                "主动向组织说明当年的情况", Attitude.BOLD,
                "你把来龙去脉写成了一份说明材料，连当时的会议记录一并附上。有人觉得你多此一举。",
                AttrDelta(integrityRisk = -4, reputation = 2, leaderTrust = 1),
                HiddenDelta(riskPressure = -22, stress = 4)
            ),
            c(
                "先看看别人怎么说，再决定", Attitude.CAUTIOUS,
                "材料在几个人手里传了一圈，谁都没有给你一个明确的说法。等待本身也是一种消耗。",
                AttrDelta(integrityRisk = 2, leaderTrust = -2),
                HiddenDelta(riskPressure = 6, stress = 7)
            ),
            c(
                "找人打个招呼，把事情压下来", Attitude.RISKY,
                "事情暂时没有下文。但你心里清楚，从此多了一个把柄在别人手里。",
                AttrDelta(reputation = -3, integrityRisk = 6),
                HiddenDelta(riskPressure = 16, network = -4)
            )
        )
    ),
    ev(
        id = "burnout_edge", title = "过劳边缘",
        background = "连续三个月没有完整休息",
        description = "你把车停在单位楼下，熄火之后在驾驶座上坐了十几分钟，" +
            "才想起来要下车。",
        tags = tags(EventTag.HEALTH, EventTag.OVERTIME),
        weight = 12,
        repeatable = true, cooldown = 12,
        minStress = 65,
        choices = listOf(
            c(
                "向领导申请调整分工", Attitude.STEADY,
                "领导不太高兴，但还是把一部分任务分给了别人。你第一次按时下了班。",
                AttrDelta(leaderTrust = -3, work = -3, health = 5, energy = 6),
                HiddenDelta(stress = -20, healthRisk = -6)
            ),
            c(
                "再撑一个季度，等项目收尾", Attitude.BOLD,
                "项目推进得很快，你的睡眠时间越来越短，靠浓茶和咖啡顶过去。",
                AttrDelta(work = 8, leaderTrust = 3, energy = -8, health = -6),
                HiddenDelta(stress = 12, healthRisk = 10)
            ),
            c(
                "请几天年假，什么都不做", Attitude.CAUTIOUS,
                "三天里你关掉了工作群。回来的时候发现，天没有塌。",
                AttrDelta(work = -5, family = 6, health = 6, energy = 8),
                HiddenDelta(stress = -24, healthRisk = -8)
            )
        )
    ),
    ev(
        id = "old_colleague", title = "老同事求助",
        background = "早年攒下的人情",
        description = "一位早年在基层共事过的老同事找到你，" +
            "想请你在件不算大的事情上帮个忙。",
        tags = tags(EventTag.COLLEAGUE, EventTag.COORDINATION),
        weight = 11,
        repeatable = true, cooldown = 16,
        minNetwork = 50,
        choices = listOf(
            c(
                "按规矩办，能帮的在范围内帮", Attitude.STEADY,
                "你按流程帮他把材料理顺，没有走任何捷径。他连声道谢。",
                AttrDelta(colleagues = 5, masses = 2, integrityRisk = -1),
                HiddenDelta(network = 6, riskPressure = -4)
            ),
            c(
                "尽力帮他推动，动用一些关系", Attitude.BOLD,
                "事情办成了。中间绕过去的几个环节，只有你和他清楚。",
                AttrDelta(colleagues = 8, leaderTrust = 2),
                HiddenDelta(network = 12, riskPressure = 9)
            ),
            c(
                "委婉拒绝，避免麻烦", Attitude.CAUTIOUS,
                "他客气地说了句「理解」，此后就再没有联系过你。",
                AttrDelta(colleagues = -4),
                HiddenDelta(network = -8, riskPressure = -5, socialInfluence = -2)
            )
        )
    ),
    ev(
        id = "public_report", title = "公开报道",
        background = "媒体采访邀约",
        description = "一项你参与推进的工作被媒体注意到，记者希望采访你，" +
            "稿子会署上你的名字。",
        tags = tags(EventTag.PUBLIC_OPINION, EventTag.LEADER_TASK),
        weight = 11,
        repeatable = true, cooldown = 20,
        minSocial = 50,
        choices = listOf(
            c(
                "接受采访，如实介绍工作", Attitude.BOLD,
                "报道反响不错，你的名字第一次被很多人看到。也有同事提醒你「树大招风」。",
                AttrDelta(reputation = 6, masses = 3),
                HiddenDelta(socialInfluence = 14, riskPressure = 4)
            ),
            c(
                "请单位集体出面，自己不出镜", Attitude.CAUTIOUS,
                "报道里只出现了单位名字，你的名字被写在括号里。",
                AttrDelta(leaderTrust = 3, reputation = 1),
                HiddenDelta(socialInfluence = 4, network = 3)
            ),
            c(
                "把机会让给具体经办的人", Attitude.STEADY,
                "经办的人从此把你当成了自己人，这份人情比稿子本身更值钱。",
                AttrDelta(colleagues = 6, leaderTrust = 4, reputation = -1),
                HiddenDelta(network = 10, socialInfluence = -3)
            )
        )
    )
)

// 第五批：里程碑事件。满足条件时优先于普通事件出现，用来推进长期目标。
private val eventsGroupFive: List<GameEvent> = listOf(
    ev(
        id = "route_choice", title = "选择方向",
        background = "组织谈话",
        description = "领导找你谈话，说你在现在的岗位上已经站稳了，接下来要选一个方向深耕。" +
            "「你自己怎么想？」",
        tags = tags(EventTag.CAREER, EventTag.LEADER_TALK),
        weight = 10,
        minPos = 2,
        priority = true,
        choices = listOf(
            c(
                "走文字这条路，把材料写好", Attitude.STEADY,
                "你开始接手单位最重要的几份稿子。加班的夜晚变多了，" +
                    "但你的名字开始出现在领导的案头。",
                AttrDelta(writing = 6, work = 2, leaderTrust = 2, masses = -2),
                HiddenDelta(mentor = 6, stress = 4),
                route = CareerRoute.WRITING,
                flag = "route_writing",
                hint = "材料与文稿"
            ),
            c(
                "走业务这条路，把专业做深", Attitude.STEADY,
                "你主动要来了最难啃的那个项目。进度慢，但每一页都经得起追问。",
                AttrDelta(ability = 6, work = 4, coordination = 2, masses = -1),
                HiddenDelta(opportunity = 5),
                route = CareerRoute.PROFESSIONAL,
                flag = "route_professional",
                hint = "项目与专业"
            ),
            c(
                "走群众这条路，把一线跑熟", Attitude.BOLD,
                "你把办公桌搬到了接访室。群众的事琐碎、重复，但每一件都有人记着。",
                AttrDelta(masses = 6, colleagues = 2, work = 2, writing = -2),
                HiddenDelta(network = 6, socialInfluence = 4),
                route = CareerRoute.MASS_WORK,
                flag = "route_mass_work",
                hint = "一线与群众"
            ),
            c(
                "走综合这条路，把人和事拢起来", Attitude.BOLD,
                "你开始出现在各种协调会上。事情不是你亲手做完的，但都是你推动的。",
                AttrDelta(coordination = 6, leaderTrust = 3, colleagues = 2, work = 1),
                HiddenDelta(opportunity = 6, network = 4),
                route = CareerRoute.MANAGEMENT,
                flag = "route_management",
                hint = "协调与统筹"
            )
        )
    )
)

// 事件库总入口。
object EventLibrary {

    val events: List<GameEvent> =
        eventsGroupOne + eventsGroupTwo + eventsGroupThree + eventsGroupFour +
            eventsGroupFive + PoliticalEventLibrary.events + GovernanceEventLibrary.events +
            PoliticalDecisionLibrary.events + FactionEventLibrary.events +
            LifeEventLibrary.events + regionEventLibrary + jobTrackEventLibrary +
            chainEventLibrary + familyEventLibrary + positionEventLibrary +
            CityEventLibrary.events

    private val byId: Map<String, GameEvent> = events.associateBy { it.id }

    // 随机池只包含非事件链节点，事件链节点由调度器按时间触发。
    val randomPool: List<GameEvent> = events.filter { it.chainId == null }

    fun find(id: String): GameEvent? = byId[id]

    fun stepsOf(chainId: String): List<GameEvent> =
        events.filter { it.chainId == chainId }.sortedBy { it.chainStep }

    // 取事件链中紧接在 afterStep 之后的节点。
    fun nextChainStep(chainId: String, afterStep: Int): GameEvent? =
        stepsOf(chainId).firstOrNull { it.chainStep > afterStep }

    fun firstChainStep(chainId: String): GameEvent? = stepsOf(chainId).firstOrNull()

    /** 事件链在界面上的名字。 */
    fun chainName(chainId: String): String = when (chainId) {
        "road" -> "修路"
        "project" -> "项目"
        "rural" -> "乡村"
        "integrity" -> "廉政"
        "petition" -> "信访"
        "family" -> "家庭"
        "favor" -> "人情"
        "c_industrial" -> "企业落户"
        "c_village" -> "城中村改造"
        "c_water" -> "水源地保护"
        "c_talent" -> "人才引进"
        "c_school" -> "学校合并"
        "c_food" -> "食品安全"
        "c_wage" -> "企业欠薪"
        "c_stall" -> "楼盘续建"
        "c_fiscal" -> "财政周转"
        "c_online" -> "网络舆情"
        "c_invest" -> "重大招商"
        "c_collective" -> "群众诉求"
        "c_special" -> "专项检查"
        "c_sneak" -> "突击暗访"
        "c_medical" -> "医共体改革"
        "c_nimby" -> "设施选址"
        "c_brand" -> "农产品品牌"
        "c_digital" -> "数字政务"
        "c_transition" -> "班子调整"
        "c_tourroad" -> "旅游公路"
        else -> chainId
    }
}
