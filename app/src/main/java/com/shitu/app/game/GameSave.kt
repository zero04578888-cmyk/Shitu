package com.shitu.app.game

import com.shitu.app.model.CareerRecord
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.PositionRepository
import com.shitu.app.model.positionAt

/**
 * 第十五阶段：统一存档模型。
 *
 * 把完整的游戏状态 [GameState] 与存档元信息（结构版本、开局时间、最后保存时间）
 * 包成一个信封。所有派生属性都直接读取 [state]，不重复存储，避免两份数据不一致；
 * UI 只需要拿到 [GameSave] 就能展示"正在玩的是哪一局、什么时候存的、进度如何"。
 *
 * 新增字段一律带默认值，因此旧版本存档（甚至没有信封的旧档）也能安全读取。
 */
data class GameSave(
    /** 存档信封结构版本，与 [GameCodec.FORMAT_VERSION] 相互独立，便于将来单独升级存档结构。 */
    val version: Int = DEFAULT_VERSION,
    /** 本局开始时间（毫秒）。0 表示未知（旧存档或未记录）。 */
    val startedAt: Long = 0L,
    /** 最后保存时间（毫秒）。0 表示未知。 */
    val savedAt: Long = 0L,
    val state: GameState
) {

    companion object {
        /** 当前存档信封版本。升级信封结构时递增，旧版本仍可读取。 */
        const val DEFAULT_VERSION: Int = 2
    }

    // ===== 身份 =====
    val playerName: String get() = state.player.name
    /** 当前职级，如「正科级」。 */
    val positionName: String get() = state.player.positionName
    /** 当前职级（别名，便于存档界面语义更清晰）。 */
    val currentRank: String get() = state.player.positionName
    /** 正式职位名称，如「业务科室主任」。 */
    val positionTitle: String get() = state.player.positionTitle
    /** 正式职位名称（别名）。 */
    val currentPosition: String get() = state.player.positionTitle
    /** 完整职位显示名「职级·具体职位」，如「正厅级·市长」。 */
    val displayPositionTitle: String get() = state.displayPositionTitle
    /** 任职时间文案，如「2 年 3 个月」。 */
    val positionStartLabel: String
        get() {
            val months = if (state.player.positionStartAbs > 0) {
                (state.absoluteQuarter - state.player.positionStartAbs).coerceAtLeast(0) * 3
            } else {
                state.player.serviceYears * 12
            }
            val years = months / 12
            val rest = months % 12
            return when {
                years <= 0 && rest <= 0 -> "刚上任"
                years <= 0 -> "$rest 个月"
                rest <= 0 -> "$years 年"
                else -> "$years 年 $rest 个月"
            }
        }
    /** 晋升进度百分比（0～100）。 */
    val promotionProgress: Int get() = PromotionEngine.promotionProgress(state)
    /** 晋升 / 职务变动历史。 */
    val promotionHistory: List<CareerRecord>
        get() = state.career.records.filter {
            it.kind == CareerRecordKind.PROMOTION || it.kind == CareerRecordKind.TRANSFER
        }
    /** 调任与降职记录。 */
    val transferHistory: List<CareerRecord>
        get() = state.career.records.filter {
            it.kind == CareerRecordKind.TRANSFER || it.kind == CareerRecordKind.SETBACK
        }
    /** 当前等级 / 职级序号（0 起）。 */
    val positionLevel: Int get() = state.player.positionIndex
    /** 学历（大专 / 本科 / 硕士 / 博士）。 */
    val education: String get() = state.player.education
    /** 起点身份：博士为「中央选调生」，其余为基层起点职业。 */
    val startTrack: String get() = state.player.startTrack
    /** 起点年龄。大专 / 本科 / 硕士 22 岁，博士 26 岁。 */
    val startingAge: Int get() = state.player.ageAtStart
    /** 起点职级名称。博士为副处级，其余为科员。 */
    val initialRank: String
        get() = if (isDoctorStart) {
            positionAt(PositionRepository.CENTRAL_SELECTED_RANK_INDEX).name
        } else {
            positionAt(0).name
        }
    /** 起点具体职位。 */
    val initialPosition: String
        get() = if (isDoctorStart) PositionRepository.CENTRAL_SELECTED_NAME else positionAt(0).name
    /** 是否博士起点（中央选调生路线）。 */
    val isDoctorStart: Boolean get() = state.player.startTrack == PositionRepository.CENTRAL_SELECTED_NAME
    /** 当前仕途路线，例如「政策综合」。 */
    val careerRoute: String
        get() = state.currentPositionRoute.ifBlank { state.route?.label ?: "未确定" }
    /** 等级名称，与官职同名，单独暴露以便界面区分"等级"与"部门"。 */
    val levelLabel: String get() = state.player.position.name
    /** 当前部门 / 单位。 */
    val unitLabel: String get() = state.player.unit.label
    /** 当前任职地区（辖区）。 */
    val regionName: String get() = state.governance.jurisdiction.name
    /** 第十六阶段：当前任职城市。 */
    val currentCityName: String get() = state.currentCityName
    /** 当前任职城市所在省级行政区。 */
    val currentProvince: String get() = state.currentProvince
    /** 当前城市的行政层级（直辖市 / 副省级 / 省会 / 计划单列 / 地级市）。 */
    val currentCityType: String get() = state.currentCityType
    /** 城市含权量。 */
    val currentCityPower: Int get() = state.currentCityPower
    /** 全国仕途履历中的城市数量。 */
    val careerCityCount: Int get() = state.careerCities.size
    /** 全国仕途履历的城市轨迹。 */
    val careerCityLabel: String
        get() = state.careerCities.joinToString(" → ") { it.cityName }

    // ===== 时间 =====
    val year: Int get() = state.year
    val quarter: Int get() = state.quarter
    /** 季度换算出的月份（1 / 4 / 7 / 10）。游戏以季度推进，这里提供月份便于展示。 */
    val month: Int get() = (state.quarter - 1) * 3 + 1

    // ===== 核心数值 =====
    /** 政绩：辖区综合政绩。 */
    val merit: Int get() = state.governance.merit.overall
    /** 民望：辖区群众满意度。 */
    val approval: Int get() = state.governance.jurisdiction.approval
    /** 威望：综合政治声望。 */
    val reputation: Int get() = PoliticsEngine.reputation(state)
    /** 财富。 */
    val wealth: Int get() = state.player.attributes.wealth
    /** 健康。 */
    val health: Int get() = state.player.attributes.health
    /** 精力。 */
    val energy: Int get() = state.player.attributes.energy
    /** 年龄。随游戏年份增长，季度不增加。 */
    val age: Int get() = state.playerAge
    /** 退休状态文案：退休节点 / 已退休 / 退居二线；未涉及时为空串。 */
    val retirementStatus: String get() = state.retirementStatus
    /** 辖区财政（万）。 */
    val finance: Int get() = state.governance.finance

    // ===== 进度 =====
    /** 已触发过的事件 id。 */
    val triggeredEvents: Set<String> get() = state.triggeredEvents
    /** 已完成的事件数量（以人生日志为准）。 */
    val completedEventCount: Int get() = state.log.size
    /** 选择记录数量。 */
    val decisionCount: Int get() = state.log.size
    /** 主线进度：累计晋升次数。 */
    val mainLineProgress: Int get() = state.promotionCount
    /** 支线进度：仍在等待触发的事件链数量。 */
    val pendingChainCount: Int get() = state.pendingChains.size
    /** 发展路线。 */
    val routeLabel: String get() = state.route?.label ?: "未确定"
    /** 已获得成就。 */
    val unlockedAchievements: Set<String> get() = state.career.unlockedAchievements

    val isFinished: Boolean get() = state.isFinished
    val endingTitle: String? get() = state.ending?.title

    /** 一行式摘要，用于首页与存档界面。 */
    fun summaryLine(): String =
        "$playerName · $displayPositionTitle · $unitLabel\n" +
            "$currentProvince · $currentCityName（$currentCityType）\n" +
            "第 $year 年 · 第 $quarter 季度 · ${state.playerAge} 岁 · 服务 ${state.player.serviceYears} 年"
}