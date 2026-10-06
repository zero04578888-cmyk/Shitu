package com.shitu.app.game

import com.shitu.app.model.AnnualReview
import com.shitu.app.model.isTopPosition
import com.shitu.app.model.positionAt

/** 年度考核里的一条可解释说明。 */
data class ReviewReason(val label: String, val detail: String)

/**
 * 第十四阶段：把年度考核「为什么是这个结果」讲清楚。
 * 只呈现真实存在的评分项与晋升硬条件，不暴露具体公式与随机权重。
 * 晋升资格区域只列真实条件，不输出解释性长句。
 */
data class ReviewExplanation(
    val contributions: List<ReviewReason>,
    val risks: List<ReviewReason>,
    /** 晋升资格区域的逐项条件，形如「✓ 任职年限：2 / 3 年」。 */
    val promotionConditions: List<String>,
    val readyForPromotion: Boolean,
    /** 一行客观信息：下一职级名称，或已到顶说明。 */
    val opportunityLine: String,
    val opportunityLevel: String,
    val resultLine: String,
    val promotionApplies: Boolean
)

object ReviewExplainer {

    fun of(state: GameState, review: AnnualReview): ReviewExplanation {
        val itemByScore = review.items.sortedByDescending { it.score }
        val lowByScore = review.items.sortedBy { it.score }

        val contributions = itemByScore
            .filter { it.score >= 60 }
            .take(3)
            .map { ReviewReason(it.label, "评分 ${it.score} · ${it.comment}") }

        val risks = LinkedHashSet<ReviewReason>()
        lowByScore.filter { it.score < 60 }.take(3).forEach { item ->
            risks += ReviewReason(item.label, "评分 ${item.score} · ${item.comment}")
        }
        // 廉政与官场风险单独点名，避免玩家忽视。
        val riskValue = state.player.attributes.integrityRisk
        if (riskValue >= 45 && risks.none { it.label == "廉政情况" }) {
            risks += ReviewReason("廉政风险", "当前 $riskValue，年度考核会据此扣分")
        }
        if (state.relationship.risk >= 45 && risks.none { it.label == "官场风险" }) {
            risks += ReviewReason("官场风险", "当前 ${state.relationship.risk}，接近会影响动议的水平")
        }

        val blockers = PromotionEngine.blockersOf(state, review.score)
        // 逐项列出真实条件（达标与未达标），不写解释性文字。
        val conditions = PromotionEngine.requirementList(state, review.score)
            .map { "${if (it.met) "✓" else "○"} ${it.label}：${it.display}" }

        val top = isTopPosition(state.player.positionIndex)
        val targetIndex = com.shitu.app.model.nextPromotionIndex(state.player.positionIndex)
        val targetName = com.shitu.app.model.positionDisplay(
            positionAt(targetIndex).name,
            com.shitu.app.model.PositionCatalog
                .defaultNode(targetIndex, state.currentCityId, state.player.unit)
                .positionName
        )
        val eligible = PromotionEngine.isEligible(state, review.score)

        val opportunityLevel = when {
            top -> "已到顶"
            eligible -> "已具备"
            else -> "条件未满足"
        }
        val opportunityLine = if (top) {
            "已在当前梯队的最高职级。"
        } else {
            "下一职级：$targetName"
        }

        val resultLine = if (review.promotionText.isNotEmpty()) {
            review.promotionText
        } else {
            "本轮年度考核已结束。"
        }

        return ReviewExplanation(
            contributions = contributions,
            risks = risks.take(3),
            promotionConditions = conditions,
            readyForPromotion = blockers.isEmpty(),
            opportunityLine = opportunityLine,
            opportunityLevel = opportunityLevel,
            resultLine = resultLine,
            promotionApplies = review.promotionText.isNotEmpty()
        )
    }
}