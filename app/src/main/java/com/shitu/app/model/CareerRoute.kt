package com.shitu.app.model

/**
 * 职业发展路线。玩家在副科级之前会做一次方向选择，
 * 之后遇到的事件、年度考核的侧重、调岗去向都会随之改变。
 *
 * [tags] 用于提高对口事件的出现权重，[focusLabel] 是界面上的方向说明。
 */
enum class CareerRoute(
    val label: String,
    val description: String,
    val focusLabel: String,
    val tags: Set<EventTag>
) {
    WRITING(
        "文字参谋",
        "以文稿、材料和政策研究立身，容易被领导看见，也容易被文字框住。",
        "文字能力",
        setOf(EventTag.MATERIAL, EventTag.LEADER_TASK)
    ),
    PROFESSIONAL(
        "业务专业",
        "以项目和专业能力立身，成长慢一些，但很难被替代。",
        "工作能力",
        setOf(EventTag.PROJECT, EventTag.RESEARCH, EventTag.ACCEPTANCE)
    ),
    MASS_WORK(
        "群众工作",
        "以一线事务和群众基础立身，评价来自下面，也受制于下面。",
        "群众基础",
        setOf(
            EventTag.MASS_APPEAL,
            EventTag.PETITION,
            EventTag.COMPLAINT,
            EventTag.RURAL
        )
    ),
    MANAGEMENT(
        "综合管理",
        "以协调、会议和综合事务立身，最接近“当领导”的那条路。",
        "协调能力",
        setOf(EventTag.MEETING, EventTag.COORDINATION, EventTag.INSPECTION)
    ),

    // 第十二阶段新增路线：与原有四条共同构成七条可用路线。
    ECONOMY_ROUTE(
        "经济发展",
        "以项目、招商和产业立身，成绩容易量化，也容易被拿来比较。",
        "经济发展",
        setOf(EventTag.PROJECT, EventTag.RESEARCH, EventTag.LEADER_TASK, EventTag.CAREER)
    ),
    ORGANIZATION_ROUTE(
        "组织人事",
        "以干部管理、考核评价立身，掌握信息，也背负人情。",
        "组织评价",
        setOf(EventTag.CAREER, EventTag.ASSESSMENT, EventTag.MEETING, EventTag.COORDINATION)
    ),
    REFORM_ROUTE(
        "改革创新",
        "以新办法、新机制立身，收益高，非议也多。",
        "创新能力",
        setOf(EventTag.PUBLIC_OPINION, EventTag.PROJECT, EventTag.CAREER, EventTag.INTEGRITY)
    );

    companion object {
        fun byName(value: String): CareerRoute? = entries.firstOrNull { it.name == value }
    }
}
