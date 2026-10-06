package com.shitu.app.game

import com.shitu.app.model.CareerEndingResult
import com.shitu.app.model.Ending
import com.shitu.app.model.FamilyState
import com.shitu.app.model.positionAt

// 结局判定。中途结局看的是即时红线，终局结局交给 CareerEndingEvaluator 统一计算。
object EndingEngine {

    // 中途可能触发的结局。
    fun terminalEnding(state: GameState): Ending? {
        val attrs = state.player.attributes
        return when {
            attrs.health <= 0 -> ending(
                "health_ruin", "健康透支", "身体", "败",
                "长期高强度的工作最终透支了身体。你不得不离开岗位，长期休养。",
                state
            )
            attrs.family <= 5 -> ending(
                "family_break", "家庭破裂", "家庭", "败",
                "工作的优先级永远排在家庭前面。等你回过头时，家里已经没有人等你回去了。",
                state
            )
            attrs.integrityRisk >= 95 -> ending(
                "integrity_fail", "廉政风险失败", "纪律", "败",
                "长期累积的廉政风险最终被查实。职业生涯就此中断，此前的一切积累归零。",
                state
            )
            state.warningCount >= 3 -> ending(
                "total_fail", "最终失败", "职业", "败",
                "连续多年的考核不合格与提醒谈话之后，组织不再安排新的任务给你。",
                state
            )
            else -> null
        }
    }

    // 退休或游戏结束时的综合结局：交给统一判定器。
    fun finalEnding(state: GameState): Ending {
        val result = CareerEndingEvaluator.evaluate(state)
        return Ending(
            id = result.id,
            title = result.title,
            category = result.category,
            description = result.description,
            highlights = snapshotOf(state) + evaluationSummary(state, result),
            tier = result.tier,
            reasons = result.reasons,
            keyNodes = result.keyNodes,
            keyChoices = result.keyChoices
        )
    }

    // ===== 展示数据 =====

    private fun ending(
        id: String,
        title: String,
        category: String,
        tier: String,
        description: String,
        state: GameState
    ): Ending = Ending(
        id = id,
        title = title,
        category = category,
        description = description,
        highlights = snapshotOf(state),
        tier = tier,
        reasons = listOf("这段人生在到达退休之前就走到了终点。")
    )

    private fun snapshotOf(state: GameState): List<Pair<String, String>> {
        val attrs = state.player.attributes
        val player = state.player
        return listOf(
            "姓名" to player.name,
            "学历" to player.education,
            "天赋" to player.traits.joinToString("、").ifEmpty { "无" },
            "发展路线" to (state.route?.label ?: "未确定"),
            "最终职位" to state.displayPositionTitle,
            "最后任职地" to "${state.currentProvince} · ${state.currentCityName}",
            "最高职级" to positionAt(state.career.highestPositionIndex).name,
            "工作单位" to player.unit.label,
            "服务年限" to "${player.serviceYears} 年",
            "退休年龄" to "${player.age} 岁",
            "晋升次数" to "${state.promotionCount} 次",
            "年度考核" to "${state.reviewCount} 次",
            "历任岗位" to "${state.career.positionsHeld} 个",
            "任职城市" to "${state.careerCities.size} 个",
            "处理事件" to "${state.log.size} 件",
            "综合政绩" to state.governance.merit.overall.toString(),
            "辖区民望" to state.governance.jurisdiction.approval.toString(),
            "家庭状况" to familyLabel(player.family),
            "主要人脉" to topRelations(state),
            "工作实绩" to attrs.work.toString(),
            "个人声望" to attrs.reputation.toString(),
            "个人积累" to attrs.wealth.toString(),
            "群众基础" to attrs.masses.toString(),
            "廉政风险" to attrs.integrityRisk.toString(),
            "健康" to attrs.health.toString(),
            "家庭关系" to attrs.family.toString()
        )
    }

    // 家庭状态的一句话描述，用于结局页。
    private fun familyLabel(family: FamilyState): String {
        val partner = if (family.hasPartner) "已婚" else "未婚"
        val children = if (family.children > 0) "育有 ${family.children} 名子女" else "暂无子女"
        return "$partner · $children"
    }

    // 关系最深的两位人物，用于结局页的「主要人脉」。
    private fun topRelations(state: GameState): String {
        val names = state.relationship.trust.entries
            .sortedByDescending { it.value }
            .take(2)
            .mapNotNull { entry -> NpcCatalog.find(entry.key)?.let { "${it.name}(${entry.value})" } }
        return names.joinToString("、").ifEmpty { "尚无深交" }
    }

    private fun evaluationSummary(
        state: GameState,
        result: CareerEndingResult
    ): List<Pair<String, String>> {
        val evaluation = CareerEngine.evaluate(state)
        val lines = mutableListOf<Pair<String, String>>()
        lines += "结局等级" to result.tier
        lines += "人生评价" to "${evaluation.overall} · ${evaluation.rating}"
        lines += "综合能力" to "${state.career.overallAbility} · 等级 ${state.career.level}"
        lines += "主导路线" to state.politics.dominantTendency.label
        return lines
    }
}
