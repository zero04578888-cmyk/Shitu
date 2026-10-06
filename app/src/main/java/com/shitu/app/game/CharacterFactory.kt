package com.shitu.app.game

import com.shitu.app.model.AttrDelta
import com.shitu.app.model.Attributes
import com.shitu.app.model.FamilyState
import com.shitu.app.model.HiddenDelta
import com.shitu.app.model.HiddenStats
import com.shitu.app.model.Player
import com.shitu.app.model.PositionRepository
import com.shitu.app.model.UnitType

// 学历选项。数值为游戏设计参数。
data class EducationOption(
    val name: String,
    val description: String,
    val bonus: AttrDelta,
    val ageAtStart: Int
)

// 初始职业选项。数值为游戏设计参数。
data class StartOption(
    val name: String,
    val unit: UnitType,
    val description: String,
    val bonus: AttrDelta,
    val hidden: HiddenDelta
)

// 天赋特质选项。每一项都有长处，也都有代价，最多选两个。
data class TraitOption(
    val name: String,
    val description: String,
    val upside: String,
    val downside: String,
    val bonus: AttrDelta,
    val hidden: HiddenDelta = HiddenDelta()
)

// 人物创建的选择，交给引擎生成初始角色。
data class CharacterDraft(
    val name: String,
    val gender: String,
    val education: String,
    val start: String,
    val traits: List<String>,
    /** 第十二阶段：玩家选择的任职地区 id；为空时按种子随机。 */
    val regionId: String = ""
)

// 人物创建工厂：把学历、起点与天赋转换为初始属性。
// 所有数值都是游戏模拟参数，不代表现实情况。
object CharacterFactory {

    const val MAX_TRAITS = 2

    val GENDER_OPTIONS = listOf("男", "女")

    val EDUCATION_OPTIONS = listOf(
        EducationOption(
            name = "大专",
            description = "起步早，偏重实践经验",
            bonus = AttrDelta(work = 6, ability = 3, writing = -4, masses = 4),
            ageAtStart = 22
        ),
        EducationOption(
            name = "本科",
            description = "各项能力较为均衡",
            bonus = AttrDelta(work = 3, ability = 4, politics = 3, writing = 2, coordination = 2),
            ageAtStart = 22
        ),
        EducationOption(
            name = "硕士",
            description = "文字与政策功底更好",
            bonus = AttrDelta(ability = 5, politics = 4, writing = 7, masses = -2, work = 1),
            ageAtStart = 22
        ),
        EducationOption(
            name = "博士",
            description = "理论功底强，但群众工作起步慢",
            bonus = AttrDelta(ability = 7, politics = 6, writing = 11, coordination = 2, masses = -5, work = -1),
            ageAtStart = 26
        )
    )

    /** 博士学历：固定走中央选调生路线。 */
    const val DOCTOR_EDUCATION = "博士"

    /** 中央选调生：博士专属起点。 */
    const val CENTRAL_SELECTED_START = "中央选调生"

    /**
     * 中央选调生起点。只有博士可以选择；
     * 它有真实的单位、属性加成与隐藏变量，进入游戏后从副处级起步。
     */
    val CENTRAL_SELECTED_OPTION = StartOption(
        name = CENTRAL_SELECTED_START,
        unit = UnitType.GENERAL,
        description = "面向重点院校博士的政策研究与综合协调岗位，起点为副处级。",
        bonus = AttrDelta(
            politics = 6,
            writing = 4,
            coordination = 4,
            ability = 3,
            reputation = 3,
            masses = -2
        ),
        hidden = HiddenDelta(mentor = 6, opportunity = 8, network = 5)
    )

    /** 是否为博士学历（走中央选调生路线）。 */
    fun isDoctor(educationName: String): Boolean = educationOf(educationName).name == DOCTOR_EDUCATION

    /** 学历对应的固定起始年龄：大专 / 本科 / 硕士 22 岁，博士 26 岁。 */
    fun startingAgeOf(educationName: String): Int = educationOf(educationName).ageAtStart

    /** 学历对应的起始职级下标：博士直接副处级，其余从科员起步。 */
    fun startingRankIndexOf(educationName: String): Int =
        if (isDoctor(educationName)) PositionRepository.CENTRAL_SELECTED_RANK_INDEX else 0

    /** 该学历可选的初始职业：博士只有中央选调生。 */
    fun startOptionsFor(educationName: String): List<StartOption> =
        if (isDoctor(educationName)) listOf(CENTRAL_SELECTED_OPTION) else START_OPTIONS

    /** 该学历的默认初始职业。选择学历时用它同步初始职业，避免残留旧选项。 */
    fun defaultStartOf(educationName: String): String =
        if (isDoctor(educationName)) CENTRAL_SELECTED_START else START_OPTIONS.first().name

    val START_OPTIONS = listOf(
        StartOption(
            name = "基层工作人员",
            unit = UnitType.TOWN,
            description = "乡镇一线，群众基础成长快",
            bonus = AttrDelta(work = 4, masses = 9, health = 3, energy = 3, wealth = -6, reputation = 2),
            hidden = HiddenDelta(network = 5, opportunity = -3)
        ),
        StartOption(
            name = "乡镇公务员",
            unit = UnitType.TOWN,
            description = "乡镇机关，事务综合",
            bonus = AttrDelta(work = 3, masses = 6, coordination = 4, wealth = -3),
            hidden = HiddenDelta(opportunity = 2, network = 3)
        ),
        StartOption(
            name = "街道公务员",
            unit = UnitType.STREET,
            description = "街道机关，协调任务密集",
            bonus = AttrDelta(work = 2, masses = 4, coordination = 7, wealth = -1),
            hidden = HiddenDelta(opportunity = 3, network = 4)
        ),
        StartOption(
            name = "机关科员",
            unit = UnitType.COUNTY,
            description = "区县机关，材料与政策任务多",
            bonus = AttrDelta(writing = 7, politics = 4, coordination = 2, masses = -3, wealth = 3),
            hidden = HiddenDelta(mentor = 6, opportunity = 4, network = 2)
        )
    )

    val TRAIT_OPTIONS = listOf(
        TraitOption(
            name = "笔杆子",
            description = "别人写三天，你一个晚上就能交稿",
            upside = "文字能力 +8，工作能力 +2",
            downside = "健康 −4，精力 −4，压力 +6",
            bonus = AttrDelta(writing = 8, ability = 2, health = -4, energy = -4),
            hidden = HiddenDelta(stress = 6)
        ),
        TraitOption(
            name = "好口才",
            description = "开会发言不用稿子，场面上的话张口就来",
            upside = "协调能力 +6，领导信任 +4，同事关系 +3",
            downside = "文字能力 −4",
            bonus = AttrDelta(coordination = 6, leaderTrust = 4, colleagues = 3, writing = -4),
            hidden = HiddenDelta(socialInfluence = 6)
        ),
        TraitOption(
            name = "酒量好",
            description = "饭桌上能替领导挡酒，很多人是在酒桌上认识你的",
            upside = "领导信任 +5，同事关系 +4，人际网络 +12",
            downside = "健康 −8，精力 −3，健康隐患 +10",
            bonus = AttrDelta(leaderTrust = 5, colleagues = 4, health = -8, energy = -3),
            hidden = HiddenDelta(network = 12, healthRisk = 10)
        ),
        TraitOption(
            name = "老好人",
            description = "谁开口你都答应，办公室里最忙的那个人",
            upside = "同事关系 +8，群众基础 +5",
            downside = "领导信任 −6，政治素养 −2，压力 +5",
            bonus = AttrDelta(colleagues = 8, masses = 5, leaderTrust = -6, politics = -2),
            hidden = HiddenDelta(stress = 5)
        ),
        TraitOption(
            name = "急性子",
            description = "事情压不到第二天，但也常常因此得罪人",
            upside = "工作实绩 +8，精力 +6",
            downside = "协调能力 −6，同事关系 −4，压力 +8",
            bonus = AttrDelta(work = 8, energy = 6, coordination = -6, colleagues = -4),
            hidden = HiddenDelta(stress = 8)
        ),
        TraitOption(
            name = "稳得住",
            description = "再乱的局面也不慌，只是机会来了反应慢半拍",
            upside = "政治素养 +6，家庭关系 +6，廉政风险 −4",
            downside = "工作实绩 −4，机遇 −6",
            bonus = AttrDelta(politics = 6, family = 6, integrityRisk = -4, work = -4),
            hidden = HiddenDelta(opportunity = -6, riskPressure = -6)
        ),
        TraitOption(
            name = "会来事",
            description = "场面上的事门儿清，谁都愿意给你三分面子",
            upside = "协调能力 +6，个人声望 +2，社会影响 +10",
            downside = "廉政风险 +6，风险值 +5",
            bonus = AttrDelta(coordination = 6, reputation = 2, integrityRisk = 6),
            hidden = HiddenDelta(socialInfluence = 10, riskPressure = 5)
        ),
        TraitOption(
            name = "记性好",
            description = "政策条文、数据口径，别人要翻本子，你张口就有",
            upside = "工作能力 +7，政治素养 +3",
            downside = "精力 −3，家庭关系 −3，压力 +4",
            bonus = AttrDelta(ability = 7, politics = 3, energy = -3, family = -3),
            hidden = HiddenDelta(stress = 4)
        )
    )

    fun educationOf(name: String): EducationOption =
        EDUCATION_OPTIONS.firstOrNull { it.name == name } ?: EDUCATION_OPTIONS[1]

    fun startOf(name: String): StartOption =
        (START_OPTIONS + CENTRAL_SELECTED_OPTION).firstOrNull { it.name == name } ?: START_OPTIONS[0]

    fun traitOf(name: String): TraitOption? =
        TRAIT_OPTIONS.firstOrNull { it.name == name }

    /** 按名称取天赋，去重并限制数量，未知名称直接忽略。 */
    fun traitsOf(names: List<String>): List<TraitOption> =
        names.mapNotNull { traitOf(it) }.distinctBy { it.name }.take(MAX_TRAITS)

    // 生成初始玩家。
    fun create(
        name: String,
        gender: String,
        educationName: String,
        startName: String,
        traitNames: List<String> = emptyList()
    ): Player {
        val education = educationOf(educationName)
        // 博士固定走中央选调生路线，不允许从普通基层起点进入。
        val doctor = isDoctor(education.name)
        val requestedStart = startOf(startName)
        val start = when {
            doctor -> CENTRAL_SELECTED_OPTION
            // 中央选调生是博士专属起点，其他学历传进来一律回退到默认起点。
            requestedStart.name == CENTRAL_SELECTED_START -> START_OPTIONS.first()
            else -> requestedStart
        }
        val startRankIndex = if (doctor) PositionRepository.CENTRAL_SELECTED_RANK_INDEX else 0
        val startTitle = if (doctor) CENTRAL_SELECTED_START else ""
        val startId = if (doctor) PositionRepository.CENTRAL_SELECTED_ID else ""
        val traits = traitsOf(traitNames)
        val base = Attributes(
            work = 62,
            ability = 64,
            politics = 64,
            writing = 56,
            coordination = 62,
            masses = 60,
            leaderTrust = 56,
            colleagues = 62,
            reputation = 52,
            integrityRisk = 5,
            health = 84,
            energy = 80,
            family = 74,
            wealth = 34
        )
        var attributes = base.apply(education.bonus).apply(start.bonus)
        var hidden = HiddenStats().apply(start.hidden)
        traits.forEach { trait ->
            attributes = attributes.apply(trait.bonus)
            hidden = hidden.apply(trait.hidden)
        }
        attributes = attributes.normalized()
        hidden = hidden.normalized()
        val family = FamilyState(
            hasParents = true,
            hasPartner = education.ageAtStart >= 26,
            children = if (education.ageAtStart >= 30) 1 else 0
        )
        return Player(
            name = name,
            gender = gender,
            education = education.name,
            startTrack = start.name,
            traits = traits.map { it.name },
            unit = start.unit,
            positionIndex = startRankIndex,
            appointmentTitle = startTitle,
            appointmentId = startId,
            attributes = attributes,
            hidden = hidden,
            family = family,
            ageAtStart = education.ageAtStart,
            serviceYears = 0,
            yearsInPosition = 0,
            birthYear = com.shitu.app.model.GameCalendar.birthYearOf(education.ageAtStart)
        )
    }
}
