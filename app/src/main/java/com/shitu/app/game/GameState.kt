package com.shitu.app.game

import com.shitu.app.model.AnnualReview
import com.shitu.app.model.ActiveProject
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.CareerProfile
import com.shitu.app.model.Ending
import com.shitu.app.model.GovernanceState
import com.shitu.app.model.JurisdictionLevel
import com.shitu.app.model.LogEntry
import com.shitu.app.model.OutcomeRecord
import com.shitu.app.model.Player
import com.shitu.app.model.PositionCatalog
import com.shitu.app.model.PositionNode
import com.shitu.app.model.positionDisplay
import com.shitu.app.model.PoliticalState
import com.shitu.app.model.CityData
import com.shitu.app.model.CityDataRepository
import com.shitu.app.model.CityStint
import com.shitu.app.model.EventTag
import com.shitu.app.model.PromotionPosition
import com.shitu.app.model.PositionRepository
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.RelationshipState
import com.shitu.app.model.TransferOffer
import com.shitu.app.model.WorkPlan
import com.shitu.app.model.WorldState

// 游戏阶段，决定界面显示哪一部分。
enum class GamePhase {
    EVENT,
    OUTCOME,
    REVIEW,
    ENDING
}

// 事件链的待触发节点。
data class PendingChain(
    val chainId: String,
    val nextStep: Int,
    val dueQuarter: Int
)

// 完整的游戏状态，所有字段都可以被存档序列化。
data class GameState(
    val player: Player,
    val year: Int = 1,
    val quarter: Int = 1,
    val reviewCount: Int = 0,
    val totalWorkGained: Int = 0,
    val log: List<LogEntry> = emptyList(),
    val reviews: List<AnnualReview> = emptyList(),
    val currentEventId: String = "",
    val outcome: OutcomeRecord? = null,
    val triggeredEvents: Set<String> = emptySet(),
    val eventCooldown: Map<String, Int> = emptyMap(),
    val flags: Set<String> = emptySet(),
    val pendingChains: List<PendingChain> = emptyList(),
    val rngState: Long = GameRng.DEFAULT_SEED,
    val promotionCount: Int = 0,
    val warningCount: Int = 0,
    val route: CareerRoute? = null,
    val relationship: RelationshipState = RelationshipState(),
    /** 第九阶段：政治资源、派系格局、任职机会、情报、联盟、危机与履历。 */
    val politics: PoliticalState = PoliticalState(),
    /** 第十阶段：个人成长、履历、人物记忆、资产与成就。 */
    val career: CareerProfile = CareerProfile(),
    val governance: GovernanceState = GovernanceState(
        jurisdiction = JurisdictionFactory.profileOf(JurisdictionLevel.TOWNSHIP),
        finance = JurisdictionLevel.TOWNSHIP.baseFinance
    ),
    /** 第十二阶段：当前任职地区、在建项目、世界状态、季度工作安排与调任机会。 */
    val regionId: String = RegionCatalog.default.id,
    val projects: List<ActiveProject> = emptyList(),
    val world: WorldState = WorldState(),
    val workPlan: WorkPlan = WorkPlan(),
    val transferOffer: TransferOffer? = null,
    val reachedTopYear: Int? = null,
    /** 任内被降职 / 停职的次数。仅由重大失误或连续低考核触发。 */
    val demotionCount: Int = 0,
    /** 第十六阶段：全国仕途履历，按任职城市记录每一段经历。 */
    val careerCities: List<CityStint> = emptyList(),
    val ending: Ending? = null,
    val phase: GamePhase = GamePhase.EVENT,
    /**
     * 第十八阶段：年度考核已经研究通过晋升，等玩家在【任职选择】里
     * 决定具体职务。旧存档没有该字段时默认为 false。
     */
    val pendingPromotion: Boolean = false
) {
    // 从开局算起的绝对季度序号，用于冷却与事件链计时。
    val absoluteQuarter: Int get() = (year - 1) * 4 + (quarter - 1)

    val isFinished: Boolean get() = ending != null

    // ===== 退休系统（65 岁人生节点）=====

    /** 当前年龄，随游戏年份增长，季度不增加。 */
    val playerAge: Int get() = player.age

    /** 是否正处于退休节点（65 岁、低于副国级、尚未做出选择）。 */
    val retirementDue: Boolean get() = RetirementEngine.isDue(this)

    /** 是否已经退出正常仕途（退休或退居二线），不再参与晋升。 */
    val postCareer: Boolean get() = RetirementEngine.hasLeftActiveCareer(this)

    /** 退休状态文案：退休节点 / 已退休 / 退居二线；未涉及时为空串。 */
    val retirementStatus: String get() = RetirementEngine.statusLabel(this)

    val positionName: String get() = player.positionName

    // ===== 第十六阶段：真实城市体系的派生字段 =====

    /** 当前任职城市 id（与 [regionId] 同源）。 */
    val currentCityId: String get() = regionId

    /** 当前任职城市档案。旧存档缺少城市时自动回退到默认城市。 */
    val currentCity: CityData get() = CityDataRepository.findOrDefault(regionId)

    val currentCityName: String get() = currentCity.name

    val currentProvince: String get() = currentCity.province

    val currentCityType: String get() = currentCity.cityType

    val currentCityPower: Int get() = currentCity.powerWeight

    /** 当前职位标识：城市 + 职级。 */
    val currentPositionId: String get() = currentCityId + "#" + player.positionIndex

    /** 当前职级名称。 */
    val currentPositionRank: String get() = player.positionName

    /** 当前职级名称（统一命名）。 */
    val currentRank: String get() = player.currentRank

    /**
     * 当前具体职位名称（如「市长」）。
     * 有明确任命时直接用；旧存档只有职级时，按当前城市与职级解析出默认职位。
     */
    val currentPositionName: String
        get() = if (player.hasExplicitPosition) {
            player.currentPositionName
        } else {
            PositionCatalog.defaultNode(player.positionIndex, currentCityId, player.unit).positionName
        }

    /** 完整职位显示名「职级·具体职位」，如「正厅级·市长」。 */
    val displayPositionTitle: String get() = positionDisplay(currentRank, currentPositionName)

    /** 当前职位的完整档案（职级 / 具体职位 / 城市 / 权力 / 影响力等）。 */
    val currentPositionNode: PositionNode
        get() = PositionCatalog.resolveNode(
            rankIndex = player.positionIndex,
            cityId = player.appointmentCityId.ifBlank { currentCityId },
            unit = player.unit,
            positionId = player.appointmentId,
            positionName = player.effectiveAppointmentTitle
        )

    /** 正在任职的城市履历段。 */
    val currentStint: CityStint? get() = careerCities.lastOrNull { it.isCurrent }

    // ===== 第十八阶段：具体职务的派生字段 =====

    /** 当前具体职务档案；旧存档按职级与城市解析出默认职务。 */
    val currentAppointment: PromotionPosition?
        get() = PositionRepository.resolve(
            rankIndex = player.positionIndex,
            cityId = player.appointmentCityId.ifBlank { currentCityId },
            unit = player.unit,
            positionId = player.appointmentId,
            positionName = player.effectiveAppointmentTitle
        )

    /** 当前职务的管辖部门类型，例如「党委」「政府办公室」。 */
    val currentDepartment: String
        get() = currentAppointment?.departmentType?.ifBlank { null }
            ?: currentPositionNode.department

    /** 当前职务的条线，例如「组织人事」。 */
    val currentPositionRoute: String
        get() = currentAppointment?.routeType?.ifBlank { null }
            ?: currentPositionNode.routeType

    /**
     * 当前职务对口的事件标签：任职后相关事件出现概率更高。
     *
     * - 旧存档只有职级、没有具体任命时，按默认职务取标签（与旧版本行为一致）；
     * - 有任命但名称无法识别时返回空集合，不给错误职务加成，也不影响任何既有逻辑。
     */
    val currentPositionTags: Set<EventTag>
        get() {
            if (!player.hasExplicitPosition) return currentAppointment?.eventTags ?: emptySet()
            return PositionRepository.match(
                rankIndex = player.positionIndex,
                cityId = player.appointmentCityId.ifBlank { currentCityId },
                unit = player.unit,
                positionId = player.appointmentId,
                positionName = player.effectiveAppointmentTitle
            )?.eventTags ?: emptySet()
        }
}
