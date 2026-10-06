package com.shitu.app.game

import com.shitu.app.model.CityData
import com.shitu.app.model.CityDataRepository
import com.shitu.app.model.CityLevel
import com.shitu.app.model.CityPositionRole
import com.shitu.app.model.CityPositionSpec
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.PositionScope
import com.shitu.app.model.positionAt

/**
 * 第十六阶段：职位实际权力计算。
 *
 * 城市的含权量不直接等于职位含权量。最终权力由四部分共同决定：
 *
 *   职位基础权力 × 城市权力系数 × 个人影响力 × 当前政治地位
 *
 * 为了避免公式过于简单，这里把结果拆成政治影响力、经济影响力、
 * 组织影响力、资源调度能力、事件权限与晋升机会六个维度，
 * 分别供界面展示、事件门控与调任/晋升系统使用。
 * 全部为 0～100 的游戏化数值。
 */
object PositionPowerCalculator {

    /** 玩家当前的实际权力档案。 */
    data class PowerProfile(
        val rankIndex: Int,
        val rankLabel: String,
        val positionTitle: String,
        val scope: PositionScope,
        val cityId: String,
        val cityName: String,
        val cityLevel: CityLevel,
        val cityPowerWeight: Int,
        val politicalInfluence: Int,
        val economicInfluence: Int,
        val organizationalInfluence: Int,
        val resourceControl: Int,
        val eventAuthority: Int,
        val promotionOpportunity: Int,
        /** 综合实际权力，0～100。 */
        val overall: Int
    ) {
        /** 完整职位显示名「职级·具体职位」，如「正厅级·市长」。 */
        val displayTitle: String
            get() = com.shitu.app.model.positionDisplay(rankLabel, positionTitle)
    }

    fun profileOf(state: GameState): PowerProfile {
        val city = CityDataRepository.findOrDefault(state.regionId)
        val index = state.player.positionIndex.coerceIn(0, POSITION_LADDER.size - 1)
        val level = positionAt(index)
        val reputation = PoliticsEngine.reputation(state).coerceIn(0, 100)
        val merit = state.governance.merit.overall.coerceIn(0, 100)
        val capital = state.politics.capital.coerceIn(0, 100)

        // 具体职位档案：不同职位（市长 / 市委书记 / 部门负责人）影响力不同，
        // 因此同一职级、同一城市的实际权力也会不同。
        val node = state.currentPositionNode
        // 职位基础权力：1～13 级映射到 8～98，再叠加具体职位的影响力。
        val base = (8 + level.powerLevel * 7 + node.influence * 20 / 100).coerceIn(0, 100)
        // 城市权力系数：40～100，避免低权重城市直接把权力压没。
        val cityFactor = (40 + city.powerWeight * 60 / 100).coerceIn(40, 100)
        // 个人影响力：声望与政绩的综合，0～100。
        val personal = ((reputation * 55 + merit * 45) / 100).coerceIn(0, 100)
        // 政治地位：政治资源为主。
        val standing = capital.coerceIn(0, 100)

        fun blend(weightOfPosition: Int, weightOfCity: Int, weightOfPersonal: Int, weightOfStanding: Int): Int {
            val total = weightOfPosition + weightOfCity + weightOfPersonal + weightOfStanding
            if (total <= 0) return 0
            return ((base * weightOfPosition +
                cityFactor * weightOfCity +
                personal * weightOfPersonal +
                standing * weightOfStanding) / total).coerceIn(0, 100)
        }

        val political = blend(4, 2, 2, 3)
        val economic = blend(4, 3, 3, 1)
        val organizational = blend(4, 2, 2, 3)
        val resource = blend(3, 4, 2, 1)
        val authority = scopeRank(level.scope)
        val promotion = promotionOpportunity(city, index, personal)

        val overall = (
            political * 22 + economic * 20 + organizational * 18 +
                resource * 18 + authority * 12 + promotion * 10
            ) / 100

        return PowerProfile(
            rankIndex = index,
            rankLabel = level.name,
            positionTitle = node.positionName.ifBlank { level.title },
            scope = level.scope,
            cityId = city.id,
            cityName = city.name,
            cityLevel = city.level,
            cityPowerWeight = city.powerWeight,
            politicalInfluence = political,
            economicInfluence = economic,
            organizationalInfluence = organizational,
            resourceControl = resource,
            eventAuthority = authority,
            promotionOpportunity = promotion,
            overall = overall
        )
    }

    /** 权限域 → 事件权限（0～100）。 */
    fun scopeRank(scope: PositionScope): Int =
        (scope.ordinal * 100 / (PositionScope.entries.size - 1)).coerceIn(0, 100)

    /**
     * 晋升机会：城市层级越高、个人影响力越强，越容易遇到高级岗位。
     * 这里只反映平台能提供的机会，不做任何「竞争」折算，
     * 玩家能否晋升始终只取决于自己的条件。
     */
    fun promotionOpportunity(city: CityData, rankIndex: Int, personal: Int): Int {
        val levelBonus = city.level.tier * 12
        val rankBonus = if (rankIndex >= 5) 10 else 0
        return (levelBonus + rankBonus + personal * 40 / 100).coerceIn(0, 100)
    }

    /**
     * 某座城市党政主官对应的职级序号。
     * 直辖市：市委书记 副国级、市长 正部级；副省级/计划单列：书记与市长均为副部级。
     */
    fun topRank(city: CityData): Int = city.level.secretaryRankIndex
    fun mayorRank(city: CityData): Int = city.level.mayorRankIndex

    /** 同一城市内部不同职务的权力档案。 */
    fun rolesOf(cityId: String): List<CityPositionSpec> =
        CityDataRepository.positionSpecs(CityDataRepository.findOrDefault(cityId))

    fun roleOf(cityId: String, role: CityPositionRole): CityPositionSpec =
        CityDataRepository.specFor(CityDataRepository.findOrDefault(cityId), role)

    /** 玩家当前职级是否够得着某座城市的某个职务。 */
    fun canTake(city: CityData, playerRank: Int, role: CityPositionRole): Boolean {
        val required = CityDataRepository.rankIndexOf(city.level, role)
        return playerRank >= required - 1
    }
}