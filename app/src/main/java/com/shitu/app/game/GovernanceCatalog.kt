package com.shitu.app.game

import com.shitu.app.model.GovernanceAction
import com.shitu.app.model.GovernanceCategory
import com.shitu.app.model.GovernanceEffect
import com.shitu.app.model.Indicator
import com.shitu.app.model.MeritKind
import com.shitu.app.model.Attr
import com.shitu.app.model.HiddenKey

/**
 * 政务大厅的措施清单。
 *
 * 每一项都要花财政、精力和一个行动点，因此不可能什么都做。
 * 数值都是游戏模拟参数。
 */
object GovernanceCatalog {

    private fun effect(
        indicators: Map<Indicator, Int> = emptyMap(),
        merit: Map<MeritKind, Int> = emptyMap(),
        finance: Int = 0,
        savings: Int = 0,
        risk: Int = 0,
        attributes: Map<Attr, Int> = emptyMap(),
        hidden: Map<HiddenKey, Int> = emptyMap()
    ) = GovernanceEffect(
        indicators = indicators,
        merit = merit,
        finance = finance,
        savings = savings,
        risk = risk,
        attributes = attributes,
        hidden = hidden
    )

    val actions: List<GovernanceAction> = listOf(
        // ===== 经济 =====
        GovernanceAction(
            id = "act_investment", label = "招商推介", category = GovernanceCategory.ECONOMY,
            description = "带队外出招商，谈下项目就能带动经济，但接待和优惠都要花钱。",
            financeCost = 12, energyCost = 4, cooldownQuarters = 6,
            effect = effect(
                indicators = mapOf(Indicator.ECONOMY to 4, Indicator.APPROVAL to 1),
                merit = mapOf(MeritKind.ECONOMIC to 4),
                risk = 2
            )
        ),
        GovernanceAction(
            id = "act_industry", label = "产业规划", category = GovernanceCategory.ECONOMY,
            description = "把本地的产业家底摸一遍，定一个三五年能干的方向。",
            financeCost = 8, energyCost = 5, cooldownQuarters = 8,
            effect = effect(
                indicators = mapOf(Indicator.ECONOMY to 3),
                merit = mapOf(MeritKind.ECONOMIC to 4, MeritKind.INNOVATION to 3)
            )
        ),
        GovernanceAction(
            id = "act_subsidy", label = "企业扶持", category = GovernanceCategory.ECONOMY,
            description = "给困难企业一点政策空间，能稳住就业，也要担一点风险。",
            financeCost = 10, energyCost = 3, cooldownQuarters = 6,
            effect = effect(
                indicators = mapOf(Indicator.ECONOMY to 3, Indicator.LIVELIHOOD to 1),
                merit = mapOf(MeritKind.ECONOMIC to 3),
                risk = 3
            )
        ),

        // ===== 民生 =====
        GovernanceAction(
            id = "act_jobs", label = "就业帮扶", category = GovernanceCategory.LIVELIHOOD,
            description = "组织招聘会、技能培训，把最难就业的那批人先托住。",
            financeCost = 8, energyCost = 3,
            effect = effect(
                indicators = mapOf(Indicator.LIVELIHOOD to 4, Indicator.APPROVAL to 3),
                merit = mapOf(MeritKind.LIVELIHOOD to 4)
            )
        ),
        GovernanceAction(
            id = "act_housing", label = "住房改造", category = GovernanceCategory.LIVELIHOOD,
            description = "改造老旧小区和危房，见效慢，但群众看得见。",
            financeCost = 15, energyCost = 4, cooldownQuarters = 8,
            effect = effect(
                indicators = mapOf(
                    Indicator.LIVELIHOOD to 5,
                    Indicator.INFRASTRUCTURE to 2,
                    Indicator.APPROVAL to 2
                ),
                merit = mapOf(MeritKind.LIVELIHOOD to 5)
            )
        ),
        GovernanceAction(
            id = "act_elderly", label = "养老服务", category = GovernanceCategory.LIVELIHOOD,
            description = "把社区养老和日间照料做起来，老人和子女都会记着。",
            financeCost = 10, energyCost = 3,
            effect = effect(
                indicators = mapOf(
                    Indicator.LIVELIHOOD to 3,
                    Indicator.MEDICAL to 2,
                    Indicator.APPROVAL to 2
                ),
                merit = mapOf(MeritKind.LIVELIHOOD to 3)
            )
        ),
        GovernanceAction(
            id = "act_medical", label = "医疗下沉", category = GovernanceCategory.LIVELIHOOD,
            description = "请上级医院带教、补充基层设备，让群众少跑一趟县城。",
            financeCost = 12, energyCost = 4, cooldownQuarters = 8,
            effect = effect(
                indicators = mapOf(Indicator.MEDICAL to 5, Indicator.LIVELIHOOD to 2),
                merit = mapOf(MeritKind.LIVELIHOOD to 3, MeritKind.INNOVATION to 2)
            )
        ),
        GovernanceAction(
            id = "act_school", label = "教育投入", category = GovernanceCategory.LIVELIHOOD,
            description = "修校舍、补师资，短期看不到政绩，长期看得见。",
            financeCost = 12, energyCost = 4, cooldownQuarters = 8,
            effect = effect(
                indicators = mapOf(
                    Indicator.EDUCATION to 5,
                    Indicator.LIVELIHOOD to 2,
                    Indicator.APPROVAL to 1
                ),
                merit = mapOf(MeritKind.LIVELIHOOD to 4)
            )
        ),

        // ===== 基建 =====
        GovernanceAction(
            id = "act_road", label = "道路建设", category = GovernanceCategory.INFRASTRUCTURE,
            description = "修路最花钱，也最容易被群众念叨。",
            financeCost = 18, energyCost = 4, cooldownQuarters = 10,
            effect = effect(
                indicators = mapOf(
                    Indicator.INFRASTRUCTURE to 6,
                    Indicator.ECONOMY to 2,
                    Indicator.APPROVAL to 1
                ),
                merit = mapOf(MeritKind.ECONOMIC to 3, MeritKind.LIVELIHOOD to 2)
            )
        ),
        GovernanceAction(
            id = "act_water", label = "供水改造", category = GovernanceCategory.INFRASTRUCTURE,
            description = "老管网换掉，停水投诉会立刻少一半。",
            financeCost = 14, energyCost = 3, cooldownQuarters = 10,
            effect = effect(
                indicators = mapOf(
                    Indicator.INFRASTRUCTURE to 4,
                    Indicator.LIVELIHOOD to 3,
                    Indicator.APPROVAL to 2
                ),
                merit = mapOf(MeritKind.LIVELIHOOD to 3)
            )
        ),
        GovernanceAction(
            id = "act_facility", label = "公共设施", category = GovernanceCategory.INFRASTRUCTURE,
            description = "广场、停车位、便民服务中心，一件一件补。",
            financeCost = 10, energyCost = 3,
            effect = effect(
                indicators = mapOf(Indicator.INFRASTRUCTURE to 3, Indicator.APPROVAL to 2),
                merit = mapOf(MeritKind.LIVELIHOOD to 2)
            )
        ),

        // ===== 治安 =====
        GovernanceAction(
            id = "act_security", label = "治安整顿", category = GovernanceCategory.SECURITY,
            description = "对高发区域做一轮集中整治。",
            financeCost = 8, energyCost = 3,
            effect = effect(
                indicators = mapOf(Indicator.SECURITY to 5, Indicator.APPROVAL to 2),
                merit = mapOf(MeritKind.SECURITY to 5)
            )
        ),
        GovernanceAction(
            id = "act_emergency", label = "应急演练", category = GovernanceCategory.SECURITY,
            description = "把预案真的走一遍，出事的时候才不至于乱。",
            financeCost = 6, energyCost = 3, cooldownQuarters = 6,
            effect = effect(
                indicators = mapOf(Indicator.SECURITY to 3),
                merit = mapOf(MeritKind.SECURITY to 3),
                risk = -2
            )
        ),
        GovernanceAction(
            id = "act_safety", label = "安全检查", category = GovernanceCategory.SECURITY,
            description = "查隐患、开整改单，麻烦但必要。",
            financeCost = 5, energyCost = 3,
            effect = effect(
                indicators = mapOf(Indicator.SECURITY to 3, Indicator.OFFICIAL_SATISFACTION to -1),
                merit = mapOf(MeritKind.SECURITY to 2),
                risk = -3
            )
        ),

        // ===== 环境 =====
        GovernanceAction(
            id = "act_pollution", label = "污染治理", category = GovernanceCategory.ENVIRONMENT,
            description = "关停整改一批排污企业，环境会好，经济会疼。",
            financeCost = 16, energyCost = 4, cooldownQuarters = 8,
            effect = effect(
                indicators = mapOf(
                    Indicator.ENVIRONMENT to 6,
                    Indicator.LIVELIHOOD to 2,
                    Indicator.ECONOMY to -2
                ),
                merit = mapOf(MeritKind.INNOVATION to 3, MeritKind.LIVELIHOOD to 2)
            )
        ),
        GovernanceAction(
            id = "act_green", label = "绿化工程", category = GovernanceCategory.ENVIRONMENT,
            description = "种树、补绿，花钱不多，观感变化明显。",
            financeCost = 6, energyCost = 2,
            effect = effect(
                indicators = mapOf(Indicator.ENVIRONMENT to 3, Indicator.APPROVAL to 1),
                merit = mapOf(MeritKind.INNOVATION to 2)
            )
        ),
        GovernanceAction(
            id = "act_sanitation", label = "环卫整治", category = GovernanceCategory.ENVIRONMENT,
            description = "把背街小巷的卫生死角和乱堆放清一遍。",
            financeCost = 7, energyCost = 2,
            effect = effect(
                indicators = mapOf(Indicator.ENVIRONMENT to 4, Indicator.APPROVAL to 2),
                merit = mapOf(MeritKind.LIVELIHOOD to 2)
            )
        ),

        // ===== 个人 =====
        GovernanceAction(
            id = "self_study", label = "学习进修", category = GovernanceCategory.PERSONAL,
            description = "用个人存款报一个班，学点真东西。",
            savingsCost = 20, energyCost = 4, cooldownQuarters = 12,
            effect = effect(
                attributes = mapOf(Attr.ABILITY to 2, Attr.WRITING to 2, Attr.POLITICS to 1)
            )
        ),
        GovernanceAction(
            id = "self_home", label = "改善住房", category = GovernanceCategory.PERSONAL,
            description = "把家里的居住条件改善一下，家里人会更支持你。",
            savingsCost = 60, energyCost = 2, cooldownQuarters = 20,
            effect = effect(
                attributes = mapOf(Attr.FAMILY to 8, Attr.HEALTH to 2, Attr.WEALTH to 6)
            )
        ),
        GovernanceAction(
            id = "self_family", label = "家庭支出", category = GovernanceCategory.PERSONAL,
            description = "带家人出去一趟，或者给父母添点东西。",
            savingsCost = 10, energyCost = 1, cooldownQuarters = 4,
            effect = effect(attributes = mapOf(Attr.FAMILY to 5, Attr.ENERGY to 2))
        ),
        GovernanceAction(
            id = "self_survey", label = "调研走访", category = GovernanceCategory.PERSONAL,
            description = "到一线跑一圈，摸清实际情况，也让自己少说外行话。",
            energyCost = 3, cooldownQuarters = 8,
            effect = effect(
                attributes = mapOf(Attr.ABILITY to 2, Attr.WORK to 1),
                hidden = mapOf(HiddenKey.OPPORTUNITY to 2, HiddenKey.NETWORK to 2)
            )
        ),
        GovernanceAction(
            id = "self_rest", label = "休息调整", category = GovernanceCategory.PERSONAL,
            description = "把手头的事放一放，睡个好觉，缓一缓。",
            energyCost = 0, cooldownQuarters = 4,
            effect = effect(
                attributes = mapOf(Attr.ENERGY to 8, Attr.HEALTH to 2),
                hidden = mapOf(HiddenKey.STRESS to -6)
            )
        ),
        GovernanceAction(
            id = "self_transport", label = "改善通勤", category = GovernanceCategory.PERSONAL,
            description = "换一种更省心的通勤方式，路上少折腾。",
            savingsCost = 40, energyCost = 0, cooldownQuarters = 24,
            effect = effect(
                attributes = mapOf(Attr.FAMILY to 3, Attr.HEALTH to 2),
                hidden = mapOf(HiddenKey.STRESS to -4)
            )
        ),
        GovernanceAction(
            id = "self_daily", label = "日常消费", category = GovernanceCategory.PERSONAL,
            description = "改善一下日常吃穿用度，日子过得体面一点。",
            savingsCost = 8, energyCost = 0, cooldownQuarters = 4,
            effect = effect(
                attributes = mapOf(Attr.FAMILY to 3, Attr.HEALTH to 1),
                hidden = mapOf(HiddenKey.STRESS to -4)
            )
        )
    )

    private val byId: Map<String, GovernanceAction> = actions.associateBy { it.id }

    fun find(id: String): GovernanceAction? = byId[id]

    fun byCategory(category: GovernanceCategory): List<GovernanceAction> =
        actions.filter { it.category == category }
}
