package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.EventTag
import com.shitu.app.model.GameEvent
import com.shitu.app.model.GovernanceEffect
import com.shitu.app.model.GovernanceGate
import com.shitu.app.model.Indicator
import com.shitu.app.model.IndicatorGate
import com.shitu.app.model.MeritKind

// 治理影响的简写，只在本文件使用。
private fun g(
    indicators: Map<Indicator, Int> = emptyMap(),
    merit: Map<MeritKind, Int> = emptyMap(),
    finance: Int = 0,
    risk: Int = 0
) = GovernanceEffect(indicators = indicators, merit = merit, finance = finance, risk = risk)

private fun maxGate(indicator: Indicator, max: Int) =
    GovernanceGate(listOf(IndicatorGate(indicator, max = max)))

private fun minGate(indicator: Indicator, min: Int) =
    GovernanceGate(listOf(IndicatorGate(indicator, min = min)))

/**
 * 政策决策与突发治理事件。
 *
 * 政策是"先做哪一件"的取舍；突发事件由辖区状态决定会不会来：
 * 财政低更容易出财政危机，民望低更容易出投诉与舆情，环境差更容易出污染。
 *
 * 所有数值都是游戏模拟参数。
 */
object GovernanceEventLibrary {

    // ===== 12 个政策决策 + 15 个突发治理事件 =====
    val events: List<GameEvent> = listOf(
        ev(
            id = "gov_policy_budget", title = "财政紧张，基建老化",
            background = "班子会上的第一议题",
            description = "账上的钱只够做一件大事。修路、建学校，还是先稳住盘子？" +
                "会上没有人先开口。",
            tags = tags(EventTag.MEETING, EventTag.PROJECT),
            weight = 18,
            repeatable = true, cooldown = 24,
            choices = listOf(
                c(
                    "先把路修出去", Attitude.BOLD,
                    "资金压到了道路上。路通了，学校的操场还是那片水泥地。",
                    AttrDelta(work = 3, energy = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.INFRASTRUCTURE to 8,
                            Indicator.ECONOMY to 3,
                            Indicator.APPROVAL to 2
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.LIVELIHOOD to 1),
                        finance = -20
                    )
                ),
                c(
                    "先建学校", Attitude.STEADY,
                    "新教学楼开工那天，很多家长站在路边看。",
                    AttrDelta(politics = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.EDUCATION to 8,
                            Indicator.LIVELIHOOD to 3,
                            Indicator.APPROVAL to 3
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 4),
                        finance = -20
                    )
                ),
                c(
                    "暂停大项目，控制财政", Attitude.CAUTIOUS,
                    "几个在建项目压了下来，账上松了一口气，工地安静了。",
                    AttrDelta(reputation = -2, politics = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.INFRASTRUCTURE to -3,
                            Indicator.APPROVAL to -2
                        ),
                        merit = mapOf(MeritKind.FINANCE to 5),
                        finance = 15
                    )
                ),
                c(
                    "靠招商引资解决财政", Attitude.RISKY,
                    "你把希望押在招商上，谈成了两笔，也欠下了人情。",
                    AttrDelta(work = 4, reputation = 2),
                    governance = g(
                        indicators = mapOf(Indicator.ECONOMY to 5, Indicator.APPROVAL to 1),
                        merit = mapOf(MeritKind.ECONOMIC to 4),
                        finance = -8,
                        risk = 4
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_industry", title = "支柱产业往哪边走",
            background = "产业规划论证会",
            description = "本地没有像样的支柱产业。专家给了三条路，各有人反对。",
            tags = tags(EventTag.PROJECT, EventTag.CAREER),
            weight = 16,
            repeatable = true, cooldown = 24,
            choices = listOf(
                c(
                    "引进一家大型企业", Attitude.BOLD,
                    "企业落地很快，环保和用工的争议也跟着来了。",
                    AttrDelta(work = 4, reputation = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 7,
                            Indicator.LIVELIHOOD to 2,
                            Indicator.ENVIRONMENT to -3
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 5),
                        finance = -15,
                        risk = 3
                    )
                ),
                c(
                    "扶持本地小微企业", Attitude.STEADY,
                    "钱撒得碎，但每一家都有人记得。",
                    AttrDelta(coordination = 3, colleagues = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 4,
                            Indicator.LIVELIHOOD to 4,
                            Indicator.APPROVAL to 3
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.LIVELIHOOD to 3),
                        finance = -10
                    )
                ),
                c(
                    "发展文旅", Attitude.CAUTIOUS,
                    "你把青山绿水当成资源，见效慢，但这条路走得稳。",
                    AttrDelta(ability = 2, reputation = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 3,
                            Indicator.ENVIRONMENT to 3,
                            Indicator.APPROVAL to 4
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 4, MeritKind.LIVELIHOOD to 2),
                        finance = -12
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_school", title = "学区怎么划",
            background = "家长把办公室围住了",
            description = "新学校建好了，划片方案一出，两边的家长都不满意。",
            tags = tags(EventTag.MASS_APPEAL, EventTag.MEETING),
            weight = 15,
            repeatable = true, cooldown = 20,
            choices = listOf(
                c(
                    "按户籍就近划片", Attitude.STEADY,
                    "规则简单清楚，老城区的家长不干了。",
                    AttrDelta(politics = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.EDUCATION to 2,
                            Indicator.APPROVAL to 1,
                            Indicator.OFFICIAL_SATISFACTION to -2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 2)
                    )
                ),
                c(
                    "摇号加多校划片", Attitude.BOLD,
                    "方案复杂，但两边都觉得有指望。执行起来很累。",
                    AttrDelta(ability = 3, energy = -4),
                    governance = g(
                        indicators = mapOf(
                            Indicator.EDUCATION to 4,
                            Indicator.APPROVAL to 5,
                            Indicator.OFFICIAL_SATISFACTION to -5
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 3, MeritKind.LIVELIHOOD to 2),
                        risk = -2
                    )
                ),
                c(
                    "维持原有划片", Attitude.CAUTIOUS,
                    "什么也没变。家长们散了，议论没散。",
                    AttrDelta(reputation = -2, politics = 1),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to -4,
                            Indicator.OFFICIAL_SATISFACTION to 3
                        ),
                        merit = mapOf(MeritKind.FINANCE to 2)
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_old_town", title = "城中村改不改",
            background = "拆迁办门口的对峙",
            description = "城中村管网老化、消防通道堵塞，但整体拆迁的成本高得吓人。",
            tags = tags(EventTag.PROJECT, EventTag.MASS_APPEAL),
            weight = 15,
            repeatable = true, cooldown = 24,
            choices = listOf(
                c(
                    "整体拆迁重建", Attitude.BOLD,
                    "推土机进场那天，有人放鞭炮，也有人坐在门口不走。",
                    AttrDelta(work = 5, energy = -5),
                    governance = g(
                        indicators = mapOf(
                            Indicator.LIVELIHOOD to 4,
                            Indicator.INFRASTRUCTURE to 8,
                            Indicator.APPROVAL to 2,
                            Indicator.OFFICIAL_SATISFACTION to -3
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 4, MeritKind.ECONOMIC to 2),
                        finance = -25,
                        risk = 5
                    )
                ),
                c(
                    "微改造，先解决隐患", Attitude.STEADY,
                    "管网换了，消防通道通了，房子还是那些房子。",
                    AttrDelta(coordination = 3, work = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.INFRASTRUCTURE to 3,
                            Indicator.APPROVAL to 3,
                            Indicator.ENVIRONMENT to 2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.INNOVATION to 2),
                        finance = -10
                    )
                ),
                c(
                    "暂缓，先研究", Attitude.CAUTIOUS,
                    "会开了三次，方案还是没拿出来。",
                    AttrDelta(politics = 2, reputation = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to -3,
                            Indicator.OFFICIAL_SATISFACTION to 2
                        ),
                        merit = mapOf(MeritKind.FINANCE to 2)
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_land", title = "一块地的用法",
            background = "项目等地，耕地红线也在那里",
            description = "投资方看中了一块连片耕地。指标、红线、进度，挤在一起。",
            tags = tags(EventTag.PROJECT, EventTag.INTEGRITY),
            weight = 14,
            repeatable = true, cooldown = 24,
            choices = listOf(
                c(
                    "优先保障项目用地", Attitude.BOLD,
                    "项目按期开工，国土部门的意见留在了会议记录里。",
                    AttrDelta(work = 4, reputation = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 5,
                            Indicator.APPROVAL to -4,
                            Indicator.ENVIRONMENT to -2
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 4),
                        risk = 4
                    )
                ),
                c(
                    "严守耕地，另找地块", Attitude.STEADY,
                    "项目慢了两个季度，红线守住了。",
                    AttrDelta(politics = 4, work = -2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to -2,
                            Indicator.APPROVAL to 4,
                            Indicator.ENVIRONMENT to 2,
                            Indicator.OFFICIAL_SATISFACTION to 2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.FINANCE to 2)
                    )
                ),
                c(
                    "争取异地置换指标", Attitude.CAUTIOUS,
                    "你跑了几趟省城，置换方案批下来了，花的钱也不少。",
                    AttrDelta(coordination = 3, energy = -3),
                    governance = g(
                        indicators = mapOf(Indicator.ECONOMY to 3, Indicator.APPROVAL to 1),
                        merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.INNOVATION to 2),
                        finance = -8
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_tax", title = "企业的减负请求",
            background = "企业家座谈会",
            description = "会上十几位企业主都在说同一件事：税费和检查太多，撑不住了。",
            tags = tags(EventTag.MEETING, EventTag.COORDINATION),
            weight = 14,
            repeatable = true, cooldown = 20,
            choices = listOf(
                c(
                    "全面减负让利", Attitude.BOLD,
                    "企业主拍手叫好，财政所的人脸都绿了。",
                    AttrDelta(reputation = 3, coordination = 2),
                    governance = g(
                        indicators = mapOf(Indicator.ECONOMY to 5, Indicator.APPROVAL to 4),
                        merit = mapOf(MeritKind.ECONOMIC to 4),
                        finance = -12
                    )
                ),
                c(
                    "定向减免困难企业", Attitude.STEADY,
                    "钱花在了刀刃上，也有人说你不公平。",
                    AttrDelta(politics = 3, coordination = 1),
                    governance = g(
                        indicators = mapOf(Indicator.ECONOMY to 3, Indicator.APPROVAL to 2),
                        merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.FINANCE to 1),
                        finance = -6
                    )
                ),
                c(
                    "维持现有征收", Attitude.CAUTIOUS,
                    "你解释了财政的难处。会开得很安静。",
                    AttrDelta(politics = 2, reputation = -2),
                    governance = g(
                        indicators = mapOf(Indicator.ECONOMY to -2, Indicator.APPROVAL to -3),
                        merit = mapOf(MeritKind.FINANCE to 4),
                        finance = 8
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_green", title = "环保执法的尺度",
            background = "一家纳税大户的排污口",
            description = "这家企业贡献了本地三分之一的税收，但排污口的数据不好看。",
            tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY),
            weight = 15,
            repeatable = true, cooldown = 24,
            choices = listOf(
                c(
                    "严格执法，先关停再说", Attitude.BOLD,
                    "企业停产整改，工人的情绪和税收一起下降。",
                    AttrDelta(politics = 4, reputation = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ENVIRONMENT to 8,
                            Indicator.ECONOMY to -4,
                            Indicator.APPROVAL to 3,
                            Indicator.OFFICIAL_SATISFACTION to -3
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 4),
                        risk = -2
                    )
                ),
                c(
                    "限期整改，边生产边治理", Attitude.STEADY,
                    "企业上了治污设备，进度不快，但确实动了。",
                    AttrDelta(coordination = 3, ability = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ENVIRONMENT to 4,
                            Indicator.ECONOMY to -1,
                            Indicator.OFFICIAL_SATISFACTION to 1
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 3)
                    )
                ),
                c(
                    "先发展，后治理", Attitude.RISKY,
                    "你压下了报告。数据在下个季度会更难看。",
                    AttrDelta(work = 3, reputation = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 4,
                            Indicator.ENVIRONMENT to -5,
                            Indicator.APPROVAL to -4
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 3),
                        risk = 5
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_talent", title = "人才怎么引",
            background = "组织部和财政局坐在同一张桌子上",
            description = "周边县市都在抢人，给的补贴一家比一家高。",
            tags = tags(EventTag.CAREER, EventTag.MEETING),
            weight = 13,
            repeatable = true, cooldown = 24,
            choices = listOf(
                c(
                    "重金引才", Attitude.BOLD,
                    "补贴方案一出，来了几个博士，也有人说这钱花得不值。",
                    AttrDelta(ability = 3, reputation = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 4,
                            Indicator.EDUCATION to 3,
                            Indicator.APPROVAL to -1
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 5),
                        finance = -15,
                        risk = 3
                    )
                ),
                c(
                    "本地培养为主", Attitude.STEADY,
                    "钱投给了本地教师和医生，见效慢，但留得住。",
                    AttrDelta(politics = 3, ability = 1),
                    governance = g(
                        indicators = mapOf(
                            Indicator.EDUCATION to 3,
                            Indicator.MEDICAL to 2,
                            Indicator.LIVELIHOOD to 2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.INNOVATION to 2),
                        finance = -8
                    )
                ),
                c(
                    "不额外投入", Attitude.CAUTIOUS,
                    "你说财政紧张，先把现有的人用好。",
                    AttrDelta(politics = 2, reputation = -1),
                    governance = g(merit = mapOf(MeritKind.FINANCE to 3), finance = 5)
                )
            )
        ),
        ev(
            id = "gov_policy_welfare", title = "低保要不要扩面",
            background = "民政局送来一份测算",
            description = "按现有标准，一批边缘困难户卡在门槛外面。扩面就要多花钱。",
            tags = tags(EventTag.MASS_APPEAL, EventTag.MEETING),
            weight = 14,
            repeatable = true, cooldown = 20,
            choices = listOf(
                c(
                    "扩大覆盖面", Attitude.BOLD,
                    "救助面铺开了，财政的压力立刻显出来。",
                    AttrDelta(reputation = 3, politics = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.LIVELIHOOD to 6,
                            Indicator.APPROVAL to 5,
                            Indicator.OFFICIAL_SATISFACTION to -2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 5),
                        finance = -12
                    )
                ),
                c(
                    "精准救助，把钱花在刀刃上", Attitude.STEADY,
                    "你让民政局逐户核实，名单短了，但每一户都实。",
                    AttrDelta(politics = 3, coordination = 2),
                    governance = g(
                        indicators = mapOf(Indicator.LIVELIHOOD to 3, Indicator.APPROVAL to 3),
                        merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.FINANCE to 1),
                        finance = -6
                    )
                ),
                c(
                    "维持现有标准", Attitude.CAUTIOUS,
                    "门槛没有动。来问的人越来越多。",
                    AttrDelta(politics = 1, reputation = -2),
                    governance = g(
                        indicators = mapOf(Indicator.APPROVAL to -3),
                        merit = mapOf(MeritKind.FINANCE to 3),
                        finance = 5
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_traffic", title = "堵在路上的早晚高峰",
            background = "群众反映最集中的问题",
            description = "县城两条主路一到早晚高峰就堵死，改造方案的钱要一次投进去。",
            tags = tags(EventTag.COMPLAINT, EventTag.PROJECT),
            weight = 13,
            repeatable = true, cooldown = 20,
            choices = listOf(
                c(
                    "扩建道路", Attitude.BOLD,
                    "工程上马，半年的拥堵之后是通畅。",
                    AttrDelta(work = 3, energy = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.INFRASTRUCTURE to 7,
                            Indicator.APPROVAL to 3,
                            Indicator.ENVIRONMENT to -2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.ECONOMIC to 2),
                        finance = -18
                    )
                ),
                c(
                    "优化公交与信号", Attitude.STEADY,
                    "公交班次加密，信号灯联动，花小钱办了事。",
                    AttrDelta(ability = 3, coordination = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.INFRASTRUCTURE to 3,
                            Indicator.ENVIRONMENT to 3,
                            Indicator.APPROVAL to 3
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 3),
                        finance = -8
                    )
                ),
                c(
                    "限行管控", Attitude.CAUTIOUS,
                    "堵是通了一点，被限的人意见很大。",
                    AttrDelta(politics = 2, reputation = -2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to -3,
                            Indicator.ENVIRONMENT to 2,
                            Indicator.SECURITY to 2
                        ),
                        merit = mapOf(MeritKind.SECURITY to 2)
                    )
                )
            )
        ),
        ev(
            id = "gov_policy_ecommerce", title = "山货怎么卖出去",
            background = "几个返乡年轻人的提议",
            description = "本地农产品品质好，但没有品牌、没有渠道，卖不上价。",
            tags = tags(EventTag.RURAL, EventTag.PROJECT),
            weight = 13,
            repeatable = true, cooldown = 20,
            choices = listOf(
                c(
                    "建电商产业园", Attitude.BOLD,
                    "园区立起来了，招商和运营还得慢慢熬。",
                    AttrDelta(work = 3, coordination = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 5,
                            Indicator.LIVELIHOOD to 3,
                            Indicator.INFRASTRUCTURE to 2
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.INNOVATION to 2),
                        finance = -14
                    )
                ),
                c(
                    "组织培训加直播带货", Attitude.STEADY,
                    "第一批直播卖出去了八百单，群众第一次觉得这事能成。",
                    AttrDelta(coordination = 3, reputation = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 3,
                            Indicator.LIVELIHOOD to 3,
                            Indicator.APPROVAL to 3
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.INNOVATION to 3),
                        finance = -6
                    )
                ),
                c(
                    "暂不投入", Attitude.CAUTIOUS,
                    "你觉得这事应该让市场自己来。",
                    AttrDelta(politics = 2),
                    governance = g(merit = mapOf(MeritKind.FINANCE to 2), finance = 4)
                )
            )
        ),
        ev(
            id = "gov_policy_medical", title = "看病还要不要往外跑",
            background = "县医院门诊量在下降",
            description = "群众小病跑市里、大病跑省城。医共体方案摆在桌上，钱是最大的问题。",
            tags = tags(EventTag.HEALTH, EventTag.MEETING),
            weight = 13,
            repeatable = true, cooldown = 24,
            choices = listOf(
                c(
                    "建医共体，统一调配", Attitude.BOLD,
                    "县乡两级打通了，医生开始下沉，院内也有人不情愿。",
                    AttrDelta(work = 3, politics = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.MEDICAL to 7,
                            Indicator.LIVELIHOOD to 3,
                            Indicator.OFFICIAL_SATISFACTION to -3
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 4, MeritKind.INNOVATION to 3),
                        finance = -16
                    )
                ),
                c(
                    "请专家定期坐诊", Attitude.STEADY,
                    "专家门诊挂满了号，群众少跑了一趟市里。",
                    AttrDelta(coordination = 3, reputation = 2),
                    governance = g(
                        indicators = mapOf(Indicator.MEDICAL to 4, Indicator.APPROVAL to 3),
                        merit = mapOf(MeritKind.LIVELIHOOD to 3),
                        finance = -8
                    )
                ),
                c(
                    "维持现有投入", Attitude.CAUTIOUS,
                    "门诊量继续下降。",
                    AttrDelta(politics = 1, reputation = -2),
                    governance = g(
                        indicators = mapOf(Indicator.APPROVAL to -3),
                        merit = mapOf(MeritKind.FINANCE to 2),
                        finance = 4
                    )
                )
            )
        ),

        // ===== 15 个突发治理事件 =====
        ev(
            id = "gov_inc_flood", title = "暴雨洪灾",
            background = "连续三天强降雨",
            description = "低洼片区内涝，两个村断电，群众在群里不停发视频。",
            tags = tags(EventTag.DISASTER, EventTag.FLOOD),
            weight = 15,
            repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "带队上一线抢险", Attitude.BOLD,
                    "你在水里泡了一整天，照片被群众发到了网上。",
                    AttrDelta(work = 3, energy = -8, reputation = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.SECURITY to 4,
                            Indicator.APPROVAL to 5,
                            Indicator.INFRASTRUCTURE to -2
                        ),
                        merit = mapOf(MeritKind.SECURITY to 4),
                        finance = -8
                    )
                ),
                c(
                    "先保重点企业和工地", Attitude.RISKY,
                    "企业损失降到了最低，被淹的住户在群里骂了一晚上。",
                    AttrDelta(work = 3, reputation = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 3,
                            Indicator.APPROVAL to -6,
                            Indicator.SECURITY to 1
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 3),
                        risk = 3
                    )
                ),
                c(
                    "按程序上报，请求支援", Attitude.STEADY,
                    "救援队第二天到了，损失清点得清清楚楚。",
                    AttrDelta(politics = 3, energy = -3),
                    governance = g(
                        indicators = mapOf(Indicator.SECURITY to 2, Indicator.APPROVAL to -2),
                        merit = mapOf(MeritKind.FINANCE to 3),
                        finance = 10
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_road_accident", title = "道路事故",
            background = "国道口的一起事故",
            description = "一起事故带出三处安全隐患，其中一处半年前就有人反映过。",
            tags = tags(EventTag.ACCIDENT, EventTag.EMERGENCY),
            weight = 14,
            repeatable = true, cooldown = 12,
            governanceGate = maxGate(Indicator.SECURITY, 60),
            choices = listOf(
                c(
                    "全面排查同类隐患", Attitude.BOLD,
                    "你带着人把辖区路口走了一遍，当场定下了整改清单。",
                    AttrDelta(work = 3, energy = -5),
                    governance = g(
                        indicators = mapOf(Indicator.SECURITY to 5, Indicator.APPROVAL to 2),
                        merit = mapOf(MeritKind.SECURITY to 4),
                        finance = -8,
                        risk = -2
                    )
                ),
                c(
                    "依规处理责任人", Attitude.STEADY,
                    "处理结果公布了，隐患还在那里。",
                    AttrDelta(politics = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.SECURITY to 2,
                            Indicator.OFFICIAL_SATISFACTION to -3
                        ),
                        merit = mapOf(MeritKind.SECURITY to 2)
                    )
                ),
                c(
                    "先把事情压下来", Attitude.CAUTIOUS,
                    "事故通报写得含糊，网上的议论没有停。",
                    AttrDelta(reputation = -3, politics = 1),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to -4,
                            Indicator.SECURITY to -2
                        ),
                        merit = mapOf(MeritKind.SECURITY to -2),
                        risk = 8
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_food_safety", title = "食品安全",
            background = "一段后厨视频在本地群里传开",
            description = "视频里的卫生状况很差，涉事的是本地一家老字号。",
            tags = tags(EventTag.HEALTH, EventTag.PUBLIC_OPINION),
            weight = 14,
            repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "连夜全面清查", Attitude.BOLD,
                    "三天查了四十多家，两家被停业整顿。",
                    AttrDelta(work = 4, energy = -6),
                    governance = g(
                        indicators = mapOf(
                            Indicator.SECURITY to 4,
                            Indicator.APPROVAL to 4,
                            Indicator.OFFICIAL_SATISFACTION to -2
                        ),
                        merit = mapOf(MeritKind.SECURITY to 3),
                        finance = -6,
                        risk = -2
                    )
                ),
                c(
                    "先处理涉事商户", Attitude.STEADY,
                    "商户被罚了，其他店照旧。",
                    AttrDelta(politics = 3, work = 1),
                    governance = g(
                        indicators = mapOf(
                            Indicator.SECURITY to 3,
                            Indicator.APPROVAL to 1
                        ),
                        merit = mapOf(MeritKind.SECURITY to 2),
                        finance = 3
                    )
                ),
                c(
                    "等上级统一部署", Attitude.CAUTIOUS,
                    "你让宣传口先别回应，视频又多了几个版本。",
                    AttrDelta(reputation = -3, energy = 1),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to -5,
                            Indicator.SECURITY to -1
                        ),
                        merit = mapOf(MeritKind.SECURITY to -1),
                        risk = 5
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_wage_arrears", title = "企业欠薪",
            background = "厂门口的几十个人",
            description = "一家企业拖欠了三个月的工资，工人堵在门口，也堵在了政府门口。",
            tags = tags(EventTag.MASS_APPEAL, EventTag.COMPLAINT),
            weight = 15,
            repeatable = true, cooldown = 12,
            governanceGate = maxGate(Indicator.ECONOMY, 58),
            choices = listOf(
                c(
                    "约谈企业主，先垫付一部分", Attitude.BOLD,
                    "财政先垫了，工人散了大半，账要慢慢追。",
                    AttrDelta(work = 3, energy = -4, reputation = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.LIVELIHOOD to 5,
                            Indicator.APPROVAL to 5,
                            Indicator.ECONOMY to -1
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 4),
                        finance = -10
                    )
                ),
                c(
                    "引导走法律程序", Attitude.STEADY,
                    "工人拿到了法律援助，钱还没有着落。",
                    AttrDelta(politics = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.LIVELIHOOD to 2,
                            Indicator.APPROVAL to -2,
                            Indicator.SECURITY to 1
                        ),
                        merit = mapOf(MeritKind.SECURITY to 2)
                    )
                ),
                c(
                    "让企业自己想办法", Attitude.CAUTIOUS,
                    "工人等了三天，第四天又来了。",
                    AttrDelta(politics = 1, reputation = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to -6,
                            Indicator.SECURITY to -2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to -2),
                        risk = 6
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_pollution", title = "工厂排污",
            background = "河边的死鱼",
            description = "下游群众捞上来一片死鱼，上游只有一家企业。",
            tags = tags(EventTag.DISASTER, EventTag.INSPECTION),
            weight = 15,
            repeatable = true, cooldown = 12,
            governanceGate = maxGate(Indicator.ENVIRONMENT, 52),
            choices = listOf(
                c(
                    "立即取样，顶格处罚", Attitude.BOLD,
                    "检测结果出来了，企业认罚，河道开始清淤。",
                    AttrDelta(politics = 4, energy = -4),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ENVIRONMENT to 6,
                            Indicator.APPROVAL to 4,
                            Indicator.ECONOMY to -2
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 3),
                        finance = -4
                    )
                ),
                c(
                    "罚款并限期整改", Attitude.STEADY,
                    "企业交了罚款，也装了设备。",
                    AttrDelta(coordination = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ENVIRONMENT to 3,
                            Indicator.APPROVAL to 1
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 2),
                        finance = 5
                    )
                ),
                c(
                    "让企业自行处理", Attitude.RISKY,
                    "事情暂时过去了，河水还是那个颜色。",
                    AttrDelta(work = 2, reputation = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ENVIRONMENT to -3,
                            Indicator.APPROVAL to -5
                        ),
                        merit = mapOf(MeritKind.INNOVATION to -2),
                        risk = 6
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_hospital", title = "医院投诉",
            background = "一次凌晨的急诊",
            description = "一位病人家属在网上发帖，说急诊等了三个小时。帖子里提到了你的名字。",
            tags = tags(EventTag.HEALTH, EventTag.PUBLIC_OPINION),
            weight = 13,
            repeatable = true, cooldown = 12,
            governanceGate = maxGate(Indicator.MEDICAL, 52),
            choices = listOf(
                c(
                    "公开调查，结果向社会说明", Attitude.BOLD,
                    "调查组查了流程，医院改了急诊分诊，帖子的评论转向了。",
                    AttrDelta(politics = 3, energy = -4),
                    governance = g(
                        indicators = mapOf(
                            Indicator.MEDICAL to 4,
                            Indicator.APPROVAL to 4,
                            Indicator.OFFICIAL_SATISFACTION to -3
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 3),
                        risk = -3
                    )
                ),
                c(
                    "协调医院与家属沟通", Attitude.STEADY,
                    "双方见了一面，帖子删了，问题还在。",
                    AttrDelta(coordination = 3, reputation = 1),
                    governance = g(
                        indicators = mapOf(
                            Indicator.MEDICAL to 1,
                            Indicator.APPROVAL to 1
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 1)
                    )
                ),
                c(
                    "让医院自己处理", Attitude.CAUTIOUS,
                    "医院发了一封措辞生硬的说明，评论区更热闹了。",
                    AttrDelta(reputation = -3, politics = 1),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to -5,
                            Indicator.MEDICAL to -1
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to -1),
                        risk = 4
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_school_safety", title = "学校安全",
            background = "一段家长拍的视频",
            description = "视频里，小学门口的护栏锈得只剩两根螺丝，学生放学时挤成一团。",
            tags = tags(EventTag.EMERGENCY, EventTag.PUBLIC_OPINION),
            weight = 14,
            repeatable = true, cooldown = 12,
            governanceGate = maxGate(Indicator.EDUCATION, 52),
            choices = listOf(
                c(
                    "先停用，立即加固", Attitude.BOLD,
                    "护栏当天拆了重装，家长群里第一次出现了感谢。",
                    AttrDelta(work = 3, energy = -4),
                    governance = g(
                        indicators = mapOf(
                            Indicator.EDUCATION to 3,
                            Indicator.SECURITY to 3,
                            Indicator.APPROVAL to 4
                        ),
                        merit = mapOf(MeritKind.SECURITY to 3),
                        finance = -8
                    )
                ),
                c(
                    "排查全区校园设施", Attitude.STEADY,
                    "你顺手把全区学校都查了一遍，发现了另外七处问题。",
                    AttrDelta(ability = 3, work = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.EDUCATION to 4,
                            Indicator.SECURITY to 2,
                            Indicator.APPROVAL to 3
                        ),
                        merit = mapOf(MeritKind.SECURITY to 4),
                        finance = -8
                    )
                ),
                c(
                    "批评校长，要求限期整改", Attitude.CAUTIOUS,
                    "校长写了检查。视频还在传。",
                    AttrDelta(politics = 2, reputation = -2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.EDUCATION to -2,
                            Indicator.APPROVAL to -4
                        ),
                        merit = mapOf(MeritKind.SECURITY to -1),
                        risk = 4
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_old_town_dispute", title = "改造工地上的对峙",
            background = "几十户人家没签协议",
            description = "施工队被拦在村口，双方情绪都上来了，有人开始录像。",
            tags = tags(EventTag.MASS_APPEAL, EventTag.PETITION),
            weight = 14,
            repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "开一场公开听证会", Attitude.STEADY,
                    "补偿标准逐条过了一遍，方案改了三处，签字的人多了。",
                    AttrDelta(coordination = 3, politics = 2, energy = -4),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to 5,
                            Indicator.INFRASTRUCTURE to 2,
                            Indicator.OFFICIAL_SATISFACTION to -3
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 3, MeritKind.LIVELIHOOD to 2),
                        risk = -3
                    )
                ),
                c(
                    "按方案依法推进", Attitude.BOLD,
                    "工地复工了，矛盾也留下来了。",
                    AttrDelta(work = 4, energy = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.INFRASTRUCTURE to 5,
                            Indicator.APPROVAL to -8
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 2),
                        risk = 8
                    )
                ),
                c(
                    "先停下来再谈", Attitude.CAUTIOUS,
                    "工地安静了，进度也停了。",
                    AttrDelta(politics = 2, work = -2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to 2,
                            Indicator.INFRASTRUCTURE to -2
                        ),
                        merit = mapOf(MeritKind.FINANCE to 2)
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_housing_stall", title = "房地产项目停工",
            background = "塔吊停在半空",
            description = "两百多户业主交了钱，工地停了两个月，售楼处天天有人来。",
            tags = tags(EventTag.MASS_APPEAL, EventTag.PROJECT),
            weight = 14,
            repeatable = true, cooldown = 14,
            governanceGate = maxGate(Indicator.ECONOMY, 52),
            choices = listOf(
                c(
                    "引入国企接盘续建", Attitude.BOLD,
                    "国企进场了，业主群里第一次有人发笑脸。",
                    AttrDelta(work = 4, reputation = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 4,
                            Indicator.APPROVAL to 5,
                            Indicator.LIVELIHOOD to 3
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.LIVELIHOOD to 3),
                        finance = -15,
                        risk = 3
                    )
                ),
                c(
                    "协调各方先行复工", Attitude.STEADY,
                    "施工队回来了，进度慢，但塔吊动了。",
                    AttrDelta(coordination = 3, work = 2),
                    governance = g(
                        indicators = mapOf(Indicator.ECONOMY to 2, Indicator.APPROVAL to 2),
                        merit = mapOf(MeritKind.ECONOMIC to 2),
                        finance = -6
                    )
                ),
                c(
                    "引导业主依法维权", Attitude.CAUTIOUS,
                    "业主们请了律师，售楼处的人少了一些，事没有解决。",
                    AttrDelta(politics = 2, reputation = -2),
                    governance = g(
                        indicators = mapOf(Indicator.APPROVAL to -7),
                        merit = mapOf(MeritKind.FINANCE to 2),
                        risk = 5
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_finance_crisis", title = "账上快没钱了",
            background = "财政所所长的紧急汇报",
            description = "下个月的工资和在建项目款撞在了一起，账上的钱只够付一样。",
            tags = tags(EventTag.MEETING, EventTag.INTEGRITY),
            weight = 20,
            repeatable = true, cooldown = 12,
            governanceGate = GovernanceGate(maxFinance = 25),
            choices = listOf(
                c(
                    "压减一般性支出", Attitude.STEADY,
                    "会议、接待、外出全部压紧，抱怨声从机关内部传来。",
                    AttrDelta(politics = 4, reputation = -2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.OFFICIAL_SATISFACTION to -6,
                            Indicator.APPROVAL to -1
                        ),
                        merit = mapOf(MeritKind.FINANCE to 5),
                        finance = 12
                    )
                ),
                c(
                    "向上争取转移支付", Attitude.BOLD,
                    "你带着方案跑了两趟市里，钱下来了一半。",
                    AttrDelta(coordination = 3, energy = -4),
                    governance = g(
                        indicators = mapOf(Indicator.APPROVAL to -1),
                        merit = mapOf(MeritKind.FINANCE to 3),
                        finance = 15
                    )
                ),
                c(
                    "暂缓在建项目", Attitude.CAUTIOUS,
                    "工地上贴了停工通知，群众议论纷纷。",
                    AttrDelta(work = -2, reputation = -3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.INFRASTRUCTURE to -4,
                            Indicator.APPROVAL to -4
                        ),
                        merit = mapOf(MeritKind.FINANCE to 4),
                        finance = 8
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_opinion", title = "网络舆情",
            background = "一个帖子被顶上了热搜",
            description = "帖子说的是一件没处理好的小事，转评已经过万，标题带上了辖区名字。",
            tags = tags(EventTag.PUBLIC_OPINION, EventTag.COMPLAINT),
            weight = 18,
            repeatable = true, cooldown = 10,
            governanceGate = maxGate(Indicator.APPROVAL, 45),
            choices = listOf(
                c(
                    "第一时间回应并公布处理", Attitude.BOLD,
                    "回应稿写得实在，评论区的风向慢慢转了。",
                    AttrDelta(politics = 3, energy = -4),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to 6,
                            Indicator.OFFICIAL_SATISFACTION to -2
                        ),
                        merit = mapOf(MeritKind.INNOVATION to 2),
                        risk = -3
                    )
                ),
                c(
                    "按规定流程处置", Attitude.STEADY,
                    "回应比舆论慢了半拍，但事实说清楚了。",
                    AttrDelta(politics = 2),
                    governance = g(
                        indicators = mapOf(Indicator.APPROVAL to 1),
                        merit = mapOf(MeritKind.FINANCE to 1)
                    )
                ),
                c(
                    "先冷处理，等热度过去", Attitude.RISKY,
                    "热度没有过去，第二个帖子又来了。",
                    AttrDelta(reputation = -3, energy = 2),
                    governance = g(
                        indicators = mapOf(Indicator.APPROVAL to -4),
                        merit = mapOf(MeritKind.INNOVATION to -1),
                        risk = 5
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_investment", title = "重大招商机会",
            background = "一家龙头企业在周边选址",
            description = "对方给的时间只有一周，要地、要政策、要承诺。",
            tags = tags(EventTag.PROJECT, EventTag.CAREER),
            weight = 15,
            repeatable = true, cooldown = 16,
            governanceGate = minGate(Indicator.ECONOMY, 45),
            choices = listOf(
                c(
                    "全力争取，条件先答应", Attitude.BOLD,
                    "企业签了意向。后面的地、电、路，一件一件都要兑现。",
                    AttrDelta(work = 4, reputation = 3, energy = -5),
                    governance = g(
                        indicators = mapOf(
                            Indicator.ECONOMY to 8,
                            Indicator.LIVELIHOOD to 2,
                            Indicator.ENVIRONMENT to -2
                        ),
                        merit = mapOf(MeritKind.ECONOMIC to 5),
                        finance = -15,
                        risk = 4
                    )
                ),
                c(
                    "先做评估，再谈条件", Attitude.STEADY,
                    "评估花了十天，企业去了隔壁县，但你没有留下隐患。",
                    AttrDelta(politics = 3, ability = 2),
                    governance = g(
                        indicators = mapOf(Indicator.ECONOMY to 3),
                        merit = mapOf(MeritKind.ECONOMIC to 2, MeritKind.FINANCE to 2)
                    )
                ),
                c(
                    "放弃，风险太大", Attitude.CAUTIOUS,
                    "你把文件退回去了。",
                    AttrDelta(politics = 2, reputation = -1),
                    governance = g(
                        indicators = mapOf(Indicator.ECONOMY to -2),
                        merit = mapOf(MeritKind.FINANCE to 3),
                        finance = 3
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_petition", title = "群众集体投诉",
            background = "办公楼下聚了三十多人",
            description = "他们反映的是同一个问题：片区改造后，补偿款迟迟没到账。",
            tags = tags(EventTag.PETITION, EventTag.MASS_APPEAL),
            weight = 18,
            repeatable = true, cooldown = 10,
            governanceGate = maxGate(Indicator.APPROVAL, 45),
            choices = listOf(
                c(
                    "到楼下去接访", Attitude.BOLD,
                    "你在院子里站了一个多小时，把时间表一条条念给他们听。",
                    AttrDelta(politics = 3, energy = -6, reputation = 3),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to 8,
                            Indicator.LIVELIHOOD to 2,
                            Indicator.SECURITY to 2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to 3),
                        risk = -3
                    )
                ),
                c(
                    "交办相关部门限期答复", Attitude.STEADY,
                    "答复按时贴上公示栏，人散了。",
                    AttrDelta(politics = 3, coordination = 1),
                    governance = g(
                        indicators = mapOf(Indicator.APPROVAL to 2),
                        merit = mapOf(MeritKind.LIVELIHOOD to 1)
                    )
                ),
                c(
                    "不符合政策，不予受理", Attitude.CAUTIOUS,
                    "政策确实没有问题，但人群第二天又来了。",
                    AttrDelta(politics = 2, reputation = -4),
                    governance = g(
                        indicators = mapOf(
                            Indicator.APPROVAL to -8,
                            Indicator.SECURITY to -2
                        ),
                        merit = mapOf(MeritKind.LIVELIHOOD to -2),
                        risk = 6
                    )
                )
            )
        ),
        ev(
            id = "gov_inc_inspection", title = "上级专项检查",
            background = "检查组不打招呼就来了",
            description = "带队的人说要抽查三个项目，材料要当天调出来。",
            tags = tags(EventTag.INSPECTION, EventTag.INTEGRITY),
            weight = 16,
            repeatable = true, cooldown = 14,
            choices = listOf(
                c(
                    "全面自查，主动暴露问题", Attitude.BOLD,
                    "你交出了两份自查报告，里面的问题写得比检查组发现的还多。",
                    AttrDelta(politics = 4, work = -2),
                    governance = g(
                        merit = mapOf(MeritKind.FINANCE to 4),
                        risk = -5
                    )
                ),
                c(
                    "按清单配合检查", Attitude.STEADY,
                    "材料齐、流程清，检查组没有多说什么。",
                    AttrDelta(politics = 3),
                    governance = g(merit = mapOf(MeritKind.FINANCE to 3), risk = -3)
                ),
                c(
                    "临时补材料应付", Attitude.RISKY,
                    "有两份文件的日期对不上，检查组的人多看了两眼。",
                    AttrDelta(work = 2, reputation = -2),
                    governance = g(merit = mapOf(MeritKind.FINANCE to -2), risk = 8)
                )
            )
        ),
        ev(
            id = "gov_inc_surprise_check", title = "突击暗访",
            background = "窗口单位被拍到了",
            description = "暗访记者的镜头里，服务大厅有两个人趴在桌上睡觉。",
            tags = tags(EventTag.INSPECTION, EventTag.PUBLIC_OPINION),
            weight = 15,
            repeatable = true, cooldown = 12,
            choices = listOf(
                c(
                    "主动公开整改", Attitude.BOLD,
                    "你让大厅装了监控，也把整改情况放到了公众号上。",
                    AttrDelta(politics = 3, reputation = 2),
                    governance = g(
                        indicators = mapOf(
                            Indicator.OFFICIAL_SATISFACTION to -3,
                            Indicator.APPROVAL to 3
                        ),
                        merit = mapOf(MeritKind.FINANCE to 2),
                        risk = -4
                    )
                ),
                c(
                    "内部通报批评", Attitude.STEADY,
                    "通报发了，窗口的作风好了一阵。",
                    AttrDelta(politics = 2, work = 1),
                    governance = g(
                        indicators = mapOf(Indicator.OFFICIAL_SATISFACTION to -2),
                        merit = mapOf(MeritKind.FINANCE to 1)
                    )
                ),
                c(
                    "想办法把片子压下来", Attitude.RISKY,
                    "片子没有立刻播，但你多了一个把柄在别人手里。",
                    AttrDelta(reputation = -2, energy = 1),
                    governance = g(merit = mapOf(MeritKind.FINANCE to -1), risk = 9)
                )
            )
        )
    )
}
