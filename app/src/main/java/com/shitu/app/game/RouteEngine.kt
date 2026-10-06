package com.shitu.app.game

import com.shitu.app.model.Attr
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.GameEvent
import com.shitu.app.model.UnitType

/**
 * 职业发展路线的规则集合。
 *
 * 路线不直接加属性，而是改变三件事：
 * 1. 遇到什么样的任务（对口事件权重更高）；
 * 2. 年度考核看重什么（考核权重重排，总和仍为 100）；
 * 3. 关键台阶上被安排到哪里（调岗去向）。
 *
 * 所有数值都是游戏模拟参数。
 */
object RouteEngine {

    /** 对口事件在随机抽取时的权重加成。 */
    const val AFFINITY_BONUS = 8

    /** 没有选路线时使用的年度考核权重。 */
    val BASE_WEIGHTS: List<Pair<Attr, Int>> = listOf(
        Attr.WORK to 30,
        Attr.ABILITY to 20,
        Attr.POLITICS to 15,
        Attr.MASSES to 10,
        Attr.LEADER_TRUST to 15,
        Attr.COLLEAGUES to 10
    )

    // 各路线的考核权重，每一项都保证总和为 100。
    private val WRITING_WEIGHTS: List<Pair<Attr, Int>> = listOf(
        Attr.WORK to 22,
        Attr.ABILITY to 16,
        Attr.POLITICS to 15,
        Attr.WRITING to 16,
        Attr.MASSES to 6,
        Attr.LEADER_TRUST to 15,
        Attr.COLLEAGUES to 10
    )

    private val PROFESSIONAL_WEIGHTS: List<Pair<Attr, Int>> = listOf(
        Attr.WORK to 26,
        Attr.ABILITY to 26,
        Attr.POLITICS to 12,
        Attr.WRITING to 6,
        Attr.MASSES to 6,
        Attr.LEADER_TRUST to 12,
        Attr.COLLEAGUES to 12
    )

    private val MASS_WORK_WEIGHTS: List<Pair<Attr, Int>> = listOf(
        Attr.WORK to 26,
        Attr.ABILITY to 14,
        Attr.POLITICS to 14,
        Attr.MASSES to 22,
        Attr.LEADER_TRUST to 12,
        Attr.COLLEAGUES to 12
    )

    private val MANAGEMENT_WEIGHTS: List<Pair<Attr, Int>> = listOf(
        Attr.WORK to 26,
        Attr.ABILITY to 16,
        Attr.POLITICS to 14,
        Attr.COORDINATION to 16,
        Attr.MASSES to 8,
        Attr.LEADER_TRUST to 12,
        Attr.COLLEAGUES to 8
    )

    private val ECONOMY_WEIGHTS: List<Pair<Attr, Int>> = listOf(
        Attr.WORK to 28,
        Attr.ABILITY to 18,
        Attr.POLITICS to 12,
        Attr.COORDINATION to 14,
        Attr.MASSES to 6,
        Attr.LEADER_TRUST to 14,
        Attr.COLLEAGUES to 8
    )

    private val ORGANIZATION_WEIGHTS: List<Pair<Attr, Int>> = listOf(
        Attr.WORK to 20,
        Attr.ABILITY to 14,
        Attr.POLITICS to 22,
        Attr.COORDINATION to 12,
        Attr.MASSES to 6,
        Attr.LEADER_TRUST to 16,
        Attr.COLLEAGUES to 10
    )

    private val REFORM_WEIGHTS: List<Pair<Attr, Int>> = listOf(
        Attr.WORK to 24,
        Attr.ABILITY to 22,
        Attr.POLITICS to 12,
        Attr.COORDINATION to 12,
        Attr.MASSES to 10,
        Attr.LEADER_TRUST to 12,
        Attr.COLLEAGUES to 8
    )

    fun reviewWeights(route: CareerRoute?): List<Pair<Attr, Int>> = when (route) {
        null -> BASE_WEIGHTS
        CareerRoute.WRITING -> WRITING_WEIGHTS
        CareerRoute.PROFESSIONAL -> PROFESSIONAL_WEIGHTS
        CareerRoute.MASS_WORK -> MASS_WORK_WEIGHTS
        CareerRoute.MANAGEMENT -> MANAGEMENT_WEIGHTS
        CareerRoute.ECONOMY_ROUTE -> ECONOMY_WEIGHTS
        CareerRoute.ORGANIZATION_ROUTE -> ORGANIZATION_WEIGHTS
        CareerRoute.REFORM_ROUTE -> REFORM_WEIGHTS
    }

    /** 对口事件的权重加成；没有路线或不对口时为 0。 */
    fun eventAffinity(route: CareerRoute?, event: GameEvent): Int {
        val current = route ?: return 0
        return if (event.tags.any { it in current.tags }) AFFINITY_BONUS else 0
    }

    /**
     * 关键台阶上的调岗去向。路线决定被安排到哪一类平台：
     * 文字参谋与综合管理更容易进机关，业务专业留在业务部门，
     * 群众工作则留在基层。
     */
    fun preferredUnit(route: CareerRoute, current: UnitType): UnitType? = when (route) {
        CareerRoute.WRITING -> when (current) {
            UnitType.CITY, UnitType.GENERAL -> null
            else -> UnitType.CITY
        }
        CareerRoute.MANAGEMENT -> when (current) {
            UnitType.GENERAL, UnitType.COUNTY -> null
            else -> UnitType.GENERAL
        }
        CareerRoute.PROFESSIONAL -> when (current) {
            UnitType.BUSINESS -> null
            else -> UnitType.BUSINESS
        }
        CareerRoute.MASS_WORK -> when (current) {
            UnitType.TOWN, UnitType.STREET -> null
            else -> UnitType.TOWN
        }
        CareerRoute.ECONOMY_ROUTE -> when (current) {
            UnitType.BUSINESS, UnitType.COUNTY -> null
            else -> UnitType.BUSINESS
        }
        CareerRoute.ORGANIZATION_ROUTE -> when (current) {
            UnitType.GENERAL, UnitType.COUNTY -> null
            else -> UnitType.GENERAL
        }
        CareerRoute.REFORM_ROUTE -> when (current) {
            UnitType.CITY, UnitType.COUNTY -> null
            else -> UnitType.CITY
        }
    }

    /** 路线的晋升机会加成：方向明确的干部更容易被安排。 */
    fun promotionBonus(route: CareerRoute?): Int = if (route == null) 0 else 3
}
