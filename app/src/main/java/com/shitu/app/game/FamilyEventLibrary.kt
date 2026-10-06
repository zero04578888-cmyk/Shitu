package com.shitu.app.game

import com.shitu.app.model.Attitude
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.EventTag
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.PoliticalEffect

/**
 * 第十二阶段：家庭与个人生活事件。共 22 个。
 *
 * 家庭不是独立经营玩法，而是仕途人生的背景：
 * 长期高强度工作会让家庭关系下降，家庭关系又影响压力恢复。
 * 全部为虚构设定，不涉及任何现实政策。
 */
internal val familyEventLibrary: List<com.shitu.app.model.GameEvent> = listOf(
    ev(
        id = "f12_marriage", title = "谈婚论嫁",
        background = "相处多年的对象提起了结婚",
        description = "对方说，年纪不小了，也该把事办了。可你手上正好有一个关键任务。",
        tags = tags(EventTag.FAMILY),
        weight = 12, repeatable = false,
        choices = listOf(
            c("把工作往后挪，先把婚结了", Attitude.STEADY, "你请了几天假，把婚事办了。工作受了点影响，家里人都松了口气。",
                AttrDelta(family = 8, work = -2, energy = 3), HiddenDelta(stress = -6)),
            c("先把手上的任务做完再谈", Attitude.BOLD, "你把婚期推了。任务完成了，对象嘴上不说，心里有想法。",
                AttrDelta(work = 3, family = -5), HiddenDelta(stress = 4)),
            c("简单办，工作和家庭都不耽误", Attitude.CAUTIOUS, "你们简单办了几桌。日子照常过，谁也没耽误。",
                AttrDelta(family = 4, work = 1))
        )
    ),
    ev(
        id = "f12_child_birth", title = "孩子出生",
        background = "预产期就在这几天",
        description = "偏偏这个时候，单位有一件脱不开身的大事。",
        tags = tags(EventTag.FAMILY),
        weight = 12, repeatable = false,
        choices = listOf(
            c("请假守在产房外", Attitude.STEADY, "孩子出生的时候你在场。那一刻你觉得什么都值了。",
                AttrDelta(family = 9, energy = -2), HiddenDelta(stress = -8)),
            c("把工作安排好，尽量赶到", Attitude.BOLD, "你安排好了手头的事才赶过去，好在没有错过。",
                AttrDelta(work = 2, family = 4)),
            c("让家里人多担待，工作为重", Attitude.RISKY, "你没赶回去。事情办完了，家里的账也记下了。",
                AttrDelta(work = 3, family = -8), HiddenDelta(stress = 5))
        )
    ),
    ev(
        id = "f12_child_school", title = "孩子的升学",
        background = "孩子面临一个重要的选择",
        description = "是就近入学，还是想办法去更好的学校，家里意见不一致。",
        tags = tags(EventTag.FAMILY, EventTag.RESEARCH),
        weight = 11, repeatable = true, cooldown = 20,
        choices = listOf(
            c("尊重孩子的意愿", Attitude.STEADY, "你们听了孩子自己的想法，选了他更适应的那所。",
                AttrDelta(family = 5, ability = 1)),
            c("尽力争取最好的资源", Attitude.BOLD, "你动用了一些关系。孩子进了好学校，但欠了人情。",
                AttrDelta(family = 4, integrityRisk = 3), political = PoliticalEffect(relations = mapOf("ma_wenbin" to 2))),
            c("顺其自然，不折腾", Attitude.CAUTIOUS, "你没有额外运作。孩子就近入学，一切如常。",
                AttrDelta(family = 2))
        )
    ),
    ev(
        id = "f12_long_distance", title = "长期异地",
        background = "工作地和家不在一个地方",
        description = "你已经连续几个月只能周末回家，家里人开始有意见了。",
        tags = tags(EventTag.FAMILY, EventTag.OVERTIME),
        weight = 12, repeatable = true, cooldown = 12,
        choices = listOf(
            c("每周尽量回去，把时间留给家人", Attitude.STEADY, "你把周末固定下来回家。路上的奔波累，但家里安稳了。",
                AttrDelta(family = 6, energy = -3, work = -1), HiddenDelta(stress = -4)),
            c("把家人接过来一起生活", Attitude.BOLD, "你把家人接了过来，代价是重新安顿的麻烦。",
                AttrDelta(family = 7, wealth = -5, work = 1)),
            c("先忙过这一阵再说", Attitude.RISKY, "你把精力都放在了工作上。家里的电话越来越短。",
                AttrDelta(work = 3, family = -7), HiddenDelta(stress = 4))
        )
    ),
    ev(
        id = "f12_family_trip", title = "一次久违的家庭旅行",
        background = "孩子放假了",
        description = "家里早就计划好的一次旅行，和你一个重要的会议撞在了一起。",
        tags = tags(EventTag.FAMILY),
        weight = 11, repeatable = true, cooldown = 16,
        choices = listOf(
            c("推掉会议，陪家人去", Attitude.STEADY, "你们一家人出去玩了一趟。照片里每个人都在笑。",
                AttrDelta(family = 8, wealth = -4, leaderTrust = -1), HiddenDelta(stress = -8)),
            c("让家人先去，自己开完会再赶过去", Attitude.CAUTIOUS, "你赶到的时候，旅行已经过半。家人有些遗憾，但也理解。",
                AttrDelta(family = 3, work = 1)),
            c("取消旅行，改天再去", Attitude.RISKY, "会议开得很成功。孩子的失望，你看见了。",
                AttrDelta(work = 2, family = -6), HiddenDelta(stress = 3))
        )
    ),
    ev(
        id = "f12_parent_ill", title = "父母生病",
        background = "家里打来电话",
        description = "父亲住院了，需要人照顾，而你正在外地出差。",
        tags = tags(EventTag.FAMILY, EventTag.HEALTH),
        weight = 12, repeatable = true, cooldown = 16,
        choices = listOf(
            c("请假回去照顾", Attitude.STEADY, "你赶了回去，守了几天。工作受了影响，但你没有留下遗憾。",
                AttrDelta(family = 8, work = -3, energy = -3), HiddenDelta(stress = -4)),
            c("安排家人照顾，自己尽量协调时间", Attitude.STEADY, "你请兄弟姐妹先照看着，自己两头跑。",
                AttrDelta(family = 4, work = -1, energy = -2)),
            c("先把手上的事办完再回去", Attitude.RISKY, "你办完事才回去。老人没说什么，你自己心里过不去。",
                AttrDelta(work = 2, family = -7), HiddenDelta(stress = 6, healthRisk = 1))
        )
    ),
    ev(
        id = "f12_spouse_job", title = "爱人的工作",
        background = "爱人想换一份工作",
        description = "换工作意味着可能要去外地，也可能需要你出面说句话。",
        tags = tags(EventTag.FAMILY, EventTag.CAREER),
        weight = 11, repeatable = true, cooldown = 20,
        choices = listOf(
            c("支持对方的选择", Attitude.STEADY, "你支持了爱人的决定。家里多了一些变化，也多了一些盼头。",
                AttrDelta(family = 6, coordination = 1)),
            c("帮忙打招呼，找个稳定的单位", Attitude.RISKY, "你托了关系。事情办成了，但你也欠了人情。",
                AttrDelta(family = 5, integrityRisk = 4), political = PoliticalEffect(relations = mapOf("qian_jinming" to 3))),
            c("希望对方以家庭为重", Attitude.CAUTIOUS, "你希望对方先稳定。爱人接受了，但看得出有些失落。",
                AttrDelta(family = 1, work = 1))
        )
    ),
    ev(
        id = "f12_house_move", title = "要不要换套房",
        background = "孩子大了，房子不够住",
        description = "换房要花掉大部分积蓄，也意味着贷款压力。",
        tags = tags(EventTag.FAMILY),
        weight = 11, repeatable = true, cooldown = 20,
        choices = listOf(
            c("量力而行，先换个够住的", Attitude.STEADY, "你们换了一套实用的房子，压力可控。",
                AttrDelta(family = 5, wealth = -10), HiddenDelta(stress = -2)),
            c("一步到位，买个好点的", Attitude.BOLD, "房子是好了，月供也上来了。",
                AttrDelta(family = 6, wealth = -18), HiddenDelta(stress = 5)),
            c("先不换，把钱留着", Attitude.CAUTIOUS, "你们决定再等等。手头宽裕，住得挤一点。",
                AttrDelta(wealth = 4, family = -2))
        )
    ),
    ev(
        id = "f12_family_conflict", title = "家里的争执",
        background = "因为你的工作，家里吵起来了",
        description = "家人抱怨你总是缺席，话说得有点重。",
        tags = tags(EventTag.FAMILY, EventTag.OVERTIME),
        weight = 12, repeatable = true, cooldown = 12,
        choices = listOf(
            c("认真道歉，调整自己的节奏", Attitude.STEADY, "你认真聊了一次，把一些工作分了出去。家里的气氛缓和了。",
                AttrDelta(family = 7, work = -2, coordination = 1), HiddenDelta(stress = -5)),
            c("解释清楚，请家里理解", Attitude.CAUTIOUS, "你把工作的难处讲了一遍。家人听懂了，但情绪还在。",
                AttrDelta(family = 3, writing = 1)),
            c("不解释，照旧", Attitude.RISKY, "你什么也没说。冷战持续了一段时间。",
                AttrDelta(family = -6, work = 1), HiddenDelta(stress = 4))
        )
    ),
    ev(
        id = "f12_family_holiday", title = "春节回谁家",
        background = "又到了这个经典难题",
        description = "两边的老人都盼着你们回去，假期只有几天。",
        tags = tags(EventTag.FAMILY),
        weight = 10, repeatable = true, cooldown = 16,
        choices = listOf(
            c("两边都跑，累点也值", Attitude.BOLD, "你们两头奔波，路上花了不少时间。两边老人都高兴。",
                AttrDelta(family = 6, energy = -4, wealth = -3)),
            c("今年去一边，明年换过来", Attitude.STEADY, "你和爱人商量好轮流来。双方都能接受。",
                AttrDelta(family = 4, coordination = 1)),
            c("就在工作地过年，叫老人过来", Attitude.CAUTIOUS, "你把老人接了过来。省了奔波，也有老人不太习惯。",
                AttrDelta(family = 2, wealth = -2))
        )
    ),
    ev(
        id = "f12_child_rebellion", title = "孩子的叛逆期",
        background = "孩子最近很不对劲",
        description = "成绩下滑，话也变少了，老师说孩子上课总走神。",
        tags = tags(EventTag.FAMILY, EventTag.RESEARCH),
        weight = 11, repeatable = true, cooldown = 16,
        choices = listOf(
            c("放下工作，认真陪一段时间", Attitude.STEADY, "你抽出时间陪孩子聊了几次。孩子慢慢愿意打开心扉了。",
                AttrDelta(family = 8, work = -3, energy = -2), HiddenDelta(stress = -4)),
            c("请老师和家人一起帮忙", Attitude.STEADY, "你和老师、家人一起想办法，孩子的状态稳住了。",
                AttrDelta(family = 4, coordination = 2)),
            c("觉得只是阶段性的，没太在意", Attitude.CAUTIOUS, "你没有特别处理。孩子的情况时好时坏。",
                AttrDelta(family = -3))
        )
    ),
    ev(
        id = "f12_family_health", title = "体检报告",
        background = "你自己的体检结果出来了",
        description = "几项指标不太好，医生建议你注意休息、少熬夜。",
        tags = tags(EventTag.HEALTH, EventTag.FAMILY, EventTag.OVERTIME),
        weight = 12, repeatable = true, cooldown = 12,
        choices = listOf(
            c("听医生的，调整作息和饮食", Attitude.STEADY, "你开始按时吃饭、早点休息。身体慢慢缓过来了。",
                AttrDelta(health = 6, energy = 4, work = -1), HiddenDelta(stress = -4, healthRisk = -3)),
            c("把手上的事做完再调理", Attitude.BOLD, "你没有停下来。身体是自己的，你知道，但事情也是真的急。",
                AttrDelta(work = 2, health = -3), HiddenDelta(healthRisk = 3)),
            c("先吃点药顶一顶", Attitude.RISKY, "你靠药顶着。短期没事，长期是个隐患。",
                AttrDelta(health = -1, energy = 2), HiddenDelta(healthRisk = 2))
        )
    ),
    ev(
        id = "f12_family_relative", title = "亲戚来求办事",
        background = "一个远房亲戚找上门",
        description = "亲戚想让你帮忙在项目上「说句话」，还带了礼物。",
        tags = tags(EventTag.FAMILY, EventTag.INTEGRITY, EventTag.GIFT),
        weight = 12, repeatable = true, cooldown = 14,
        choices = listOf(
            c("婉言拒绝，把东西退回去", Attitude.BOLD, "你把礼物退了回去，也把规矩讲清楚了。亲戚不太高兴。",
                AttrDelta(integrityRisk = -5, family = -3), HiddenDelta(stress = 2)),
            c("按程序来，能办就办", Attitude.STEADY, "你告诉亲戚按正常程序走。事情办不办成，看条件。",
                AttrDelta(integrityRisk = -2, family = 1)),
            c("帮个小忙，下不为例", Attitude.RISKY, "你帮了这个忙。事情不大，但口子开了。",
                AttrDelta(integrityRisk = 6, family = 3), HiddenDelta(riskPressure = 4))
        )
    ),
    ev(
        id = "f12_family_elder", title = "老人的养老问题",
        background = "父母年纪大了",
        description = "是接到身边，还是留在老家，一家人商量不出结果。",
        tags = tags(EventTag.FAMILY, EventTag.HEALTH),
        weight = 11, repeatable = true, cooldown = 20,
        choices = listOf(
            c("接到身边照顾", Attitude.STEADY, "你把老人接了过来。房子挤了点，但一家人在一起。",
                AttrDelta(family = 7, wealth = -4, energy = -2)),
            c("请人照顾，常回去看看", Attitude.STEADY, "你安排了可靠的照护，也尽量抽时间回去。",
                AttrDelta(family = 4, wealth = -5)),
            c("尊重老人自己的意愿", Attitude.CAUTIOUS, "老人选择留在老家。你心里始终有些放不下。",
                AttrDelta(family = 2))
        )
    ),
    ev(
        id = "f12_family_money", title = "家庭开支",
        background = "这个月账单有点多",
        description = "孩子的培训班、老人的药费、人情往来，全挤在了一起。",
        tags = tags(EventTag.FAMILY),
        weight = 10, repeatable = true, cooldown = 12,
        choices = listOf(
            c("仔细盘一盘，该省的省", Attitude.STEADY, "你把开支列了个清单，砍掉了一些不必要的。",
                AttrDelta(wealth = 3, family = 1, coordination = 1)),
            c("该花的还是花，别委屈家人", Attitude.BOLD, "你没有省，家人的需求都满足了。存下的钱少了。",
                AttrDelta(family = 5, wealth = -4)),
            c("先紧一紧，等宽裕了再说", Attitude.CAUTIOUS, "你压缩了一些开支。家人没说什么，但你自己记着。",
                AttrDelta(wealth = 2, family = -2))
        )
    ),
    ev(
        id = "f12_family_old_friend", title = "老同学聚会",
        background = "多年不见的同学们要聚一聚",
        description = "有人专门从外地赶回来，就为了这场聚会。",
        tags = tags(EventTag.FAMILY, EventTag.COLLEAGUE),
        weight = 10, repeatable = true, cooldown = 14,
        choices = listOf(
            c("去，好好叙叙旧", Attitude.STEADY, "你们聊了很久。老同学的情分，是别处换不来的。",
                AttrDelta(family = 3, colleagues = 3, energy = -1), HiddenDelta(stress = -5, network = 2)),
            c("去，也顺便认识几个人", Attitude.BOLD, "你去了，也认识了几位对工作可能有帮助的人。",
                AttrDelta(colleagues = 2, coordination = 1), HiddenDelta(network = 4)),
            c("太忙了，下次再说", Attitude.CAUTIOUS, "你婉拒了。同学们拍了合照发给你，你看了很久。",
                AttrDelta(work = 1, family = -2))
        )
    ),
    ev(
        id = "f12_family_child_career", title = "孩子的职业选择",
        background = "孩子想走一条你不熟悉的路",
        description = "孩子说想学艺术，你觉得不稳定，希望他考个稳妥的专业。",
        tags = tags(EventTag.FAMILY, EventTag.CAREER),
        weight = 11, repeatable = true, cooldown = 20, minFamily = 40,
        choices = listOf(
            c("尊重孩子，帮他一起规划", Attitude.STEADY, "你认真了解了孩子想做的事，一起做了规划。孩子眼里有光。",
                AttrDelta(family = 7, ability = 2)),
            c("讲清利弊，让他自己决定", Attitude.STEADY, "你把两条路的利弊都摆了出来，让孩子自己选。",
                AttrDelta(family = 4, writing = 1)),
            c("坚持自己的想法，替他做决定", Attitude.BOLD, "你替孩子做了决定。他照做了，但很久没有和你深谈。",
                AttrDelta(family = -6, work = 1))
        )
    ),
    ev(
        id = "f12_family_anniversary", title = "结婚纪念日",
        background = "日历上圈着的那一天",
        description = "恰好那天晚上有一个推不开的应酬。",
        tags = tags(EventTag.FAMILY, EventTag.COORDINATION),
        weight = 10, repeatable = true, cooldown = 16,
        choices = listOf(
            c("推掉应酬，回家吃饭", Attitude.STEADY, "你回了家。桌上是一顿家常菜，比应酬的酒好喝。",
                AttrDelta(family = 7, colleagues = -1), HiddenDelta(stress = -6)),
            c("应酬结束后再赶回家", Attitude.CAUTIOUS, "你赶回去的时候，饭菜已经凉了。爱人还是笑了。",
                AttrDelta(family = 3, colleagues = 1)),
            c("让爱人改天补过", Attitude.RISKY, "应酬到很晚。纪念日就这么过去了。",
                AttrDelta(colleagues = 2, family = -5))
        )
    ),
    ev(
        id = "f12_family_neighbor", title = "家里被邻居投诉",
        background = "小事闹到了居委会",
        description = "因为装修和噪音，家里人跟邻居闹了点矛盾，对方话说得不太好听。",
        tags = tags(EventTag.FAMILY, EventTag.COMPLAINT),
        weight = 10, repeatable = true, cooldown = 16,
        choices = listOf(
            c("主动上门沟通，把矛盾化解", Attitude.STEADY, "你拎着水果上门聊了聊，两家握手言和。",
                AttrDelta(family = 4, masses = 1, coordination = 2)),
            c("按规矩来，请居委会调解", Attitude.STEADY, "居委会出面调解，事情解决了，关系一般。",
                AttrDelta(family = 2)),
            c("觉得对方无理取闹，不予理会", Attitude.RISKY, "两家关系僵了。家里人夹在中间也不舒服。",
                AttrDelta(family = -4))
        )
    ),
    ev(
        id = "f12_family_retire_plan", title = "退休后的打算",
        background = "年纪渐渐大了",
        description = "你开始想，退了之后的日子怎么过。",
        tags = tags(EventTag.FAMILY, EventTag.HEALTH),
        weight = 10, repeatable = true, cooldown = 24, minFamily = 30,
        choices = listOf(
            c("多陪家人，把欠的时间补回来", Attitude.STEADY, "你计划好了以后的生活。家人的期待写在脸上。",
                AttrDelta(family = 8, health = 2), HiddenDelta(stress = -6)),
            c("继续做点力所能及的事", Attitude.BOLD, "你还想再干点事。家人支持，也有点担心你的身体。",
                AttrDelta(work = 2, reputation = 2, health = -1)),
            c("顺其自然，走一步看一步", Attitude.CAUTIOUS, "你没想太远。日子就这么过着。",
                AttrDelta(family = 2))
        )
    ),
    ev(
        id = "f12_family_gift", title = "一件贵重的礼物",
        background = "有人给家里送了点东西",
        description = "你回家发现，有人送了一件不便宜的礼物，家人不知道该怎么处理。",
        tags = tags(EventTag.FAMILY, EventTag.GIFT, EventTag.INTEGRITY),
        weight = 11, repeatable = true, cooldown = 16,
        choices = listOf(
            c("立即退回，并说清规矩", Attitude.BOLD, "你把东西退了回去，还跟家人讲了利害。",
                AttrDelta(integrityRisk = -5, family = 2), HiddenDelta(stress = 1)),
            c("登记上交", Attitude.STEADY, "你把礼物登记后上交了。事情处理得干净。",
                AttrDelta(integrityRisk = -3, politics = 1)),
            c("收下，反正没人知道", Attitude.RISKY, "东西留下了。你自己知道这意味着什么。",
                AttrDelta(integrityRisk = 7), HiddenDelta(riskPressure = 5))
        )
    )
)



