package com.shitu.app.game

import com.shitu.app.model.AnnualReview
import com.shitu.app.model.Attr
import com.shitu.app.model.ReviewItem

// 年度考核。
// 权重是游戏模拟参数，仅用于玩法平衡，不代表任何现实评价体系。
object AnnualReviewEngine {

    /** 年度考核的基础权重；选定发展路线后会按路线的侧重重新分配。 */
    val WEIGHTS: List<Pair<Attr, Int>> = RouteEngine.BASE_WEIGHTS

    /** 治理成绩在年度考核中的比重（其余为干部自身表现）。 */
    const val GOVERNANCE_WEIGHT = 35

    fun conduct(state: GameState): AnnualReview {
        val attrs = state.player.attributes
        val weights = RouteEngine.reviewWeights(state.route)
        val weighted = weights.sumOf { (attr, weight) -> attrs.valueOf(attr) * weight } / 100
        val riskPenalty = attrs.integrityRisk / 5
        val attributeScore = (weighted - riskPenalty).coerceIn(0, 100)
        // 政绩、民望、治安、财政一起进考核，光靠个人表现拿不到高分。
        val governanceScore = GovernanceEngine.governanceScore(state)
        val score = (
            attributeScore * (100 - GOVERNANCE_WEIGHT) + governanceScore * GOVERNANCE_WEIGHT
            ) / 100
        val grade = gradeOf(score)
        val gov = state.governance
        val financeScore = GovernanceEngine.financeScore(state)
        val items = listOf(
            ReviewItem("工作实绩", band(attrs.work), attrs.work),
            ReviewItem("能力表现", band(attrs.ability), attrs.ability),
            ReviewItem("政治素养", band(attrs.politics), attrs.politics),
            ReviewItem("群众评价", band(attrs.masses), attrs.masses),
            ReviewItem("领导评价", band(attrs.leaderTrust), attrs.leaderTrust),
            ReviewItem("同事评价", band(attrs.colleagues), attrs.colleagues),
            ReviewItem("廉政情况", riskBand(attrs.integrityRisk), 100 - attrs.integrityRisk),
            ReviewItem("健康情况", band(attrs.health), attrs.health),
            ReviewItem("家庭情况", band(attrs.family), attrs.family),
            ReviewItem("综合政绩", band(gov.merit.overall), gov.merit.overall),
            ReviewItem("民望", band(gov.jurisdiction.approval), gov.jurisdiction.approval),
            ReviewItem("治安", band(gov.jurisdiction.security), gov.jurisdiction.security),
            ReviewItem("财政", band(financeScore), financeScore)
        )
        return AnnualReview(
            year = state.year,
            score = score,
            grade = grade,
            summary = summaryText(state, score, grade),
            items = items,
            promotionText = "",
            promotionGranted = false
        )
    }

    fun gradeOf(score: Int): String = when {
        score >= 85 -> "优秀"
        score >= 75 -> "良好"
        score >= 60 -> "称职"
        score >= 45 -> "基本称职"
        else -> "不称职"
    }

    fun band(value: Int): String = when {
        value >= 85 -> "突出"
        value >= 70 -> "良好"
        value >= 55 -> "称职"
        value >= 40 -> "待提高"
        else -> "较弱"
    }

    fun riskBand(value: Int): String = when {
        value >= 70 -> "风险较高"
        value >= 45 -> "需要提醒"
        value >= 25 -> "基本可控"
        else -> "清白"
    }

    private fun summaryText(state: GameState, score: Int, grade: String): String {
        val name = state.player.name
        val position = state.displayPositionTitle
        val unit = state.player.unit.label
        val route = state.route
        val head = if (route == null) {
            "第 ${state.year} 年，$name 在${unit}担任$position。"
        } else {
            "第 ${state.year} 年，$name 在${unit}担任$position，按${route.label}方向考察。"
        }
        val gov = state.governance
        val governanceLine = "辖区综合政绩 ${gov.merit.overall} 分，" +
            "民望 ${gov.jurisdiction.approval}，" +
            "治理综合得分 ${GovernanceEngine.governanceScore(state)} 分。"
        val body = when {
            score >= 85 -> "全年承担了大量任务，多项工作获得肯定，同事与群众评价都很好。"
            score >= 75 -> "全年工作完成情况良好，重点任务落实到位，整体表现稳定。"
            score >= 65 -> "全年工作按计划完成，没有出现明显失误，仍有提升空间。"
            score >= 55 -> "全年工作基本完成，但有几项任务推进不力，需要继续努力。"
            else -> "全年工作完成情况不理想，多项工作出现滞后，需要认真反思。"
        }
        return "$head$body$governanceLine 年度综合评分 $score 分（$grade）。"
    }
}
