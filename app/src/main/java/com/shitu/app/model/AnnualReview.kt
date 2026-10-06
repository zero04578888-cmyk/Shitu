package com.shitu.app.model

// 年度考核中的单项评价。
data class ReviewItem(
    val label: String,
    val comment: String,
    val score: Int
)

// 年度考核结果。综合评分由游戏内部的模拟权重计算，
// 权重定义在 AnnualReviewEngine 中，并明确标注为游戏模拟参数。
data class AnnualReview(
    val year: Int,
    val score: Int,
    val grade: String,
    val summary: String,
    val items: List<ReviewItem>,
    val promotionText: String,
    val promotionGranted: Boolean
)
