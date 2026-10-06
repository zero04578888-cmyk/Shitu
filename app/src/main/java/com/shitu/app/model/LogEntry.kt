package com.shitu.app.model

/** 日志类型，界面按类型着色。 */
enum class LogKind {
    EVENT,
    REVIEW,
    PROMOTION,
    ROUTE,
    PROJECT,
    WARNING,
    ENDING
}

/** 事件日志。每次选择都会留下一条记录。 */
data class LogEntry(
    val year: Int,
    val quarter: Int,
    val title: String,
    val choiceText: String,
    val outcome: String,
    val deltas: List<Pair<String, Int>>,
    val kind: LogKind = LogKind.EVENT
)

/** 一次选择的结果，用于界面即时反馈。 */
data class OutcomeRecord(
    val eventId: String,
    val eventTitle: String,
    val choiceText: String,
    val outcome: String,
    val deltas: List<Pair<String, Int>>,
    val hiddenDeltas: List<Pair<String, Int>>,
    /** 这次选择带来的官场关系变化。 */
    val relationDeltas: List<Pair<String, Int>> = emptyList(),
    /** 这次选择带来的辖区与政绩变化。 */
    val governanceDeltas: List<Pair<String, Int>> = emptyList()
)

