package com.shitu.app.model

/**
 * 第十二阶段：人生阶段。
 *
 * 不同的阶段会挂接不同的事件池与政策取向，避免 25 岁和 55 岁遇到同一批事。
 * 由任职年限、职级与年龄推导，不单独存档。
 */
enum class LifeStage(val label: String, val description: String) {
    EARLY("初入仕途", "刚参加工作，先把手上的事做扎实。"),
    GRASSROOTS("基层历练", "在基层摸爬滚打，攒群众基础和扛事经验。"),
    GROWTH("能力成长", "开始独当一面，能力和履历拉开差距。"),
    IMPORTANT("重要岗位", "手上有了资源和话语权，也开始被盯着。"),
    MID_CAREER("中层历练", "担子更重、责任更大，每一次考核都很关键。"),
    CORE("核心岗位", "进入班子的视野，一举一动都被放大。"),
    PEAK("职业巅峰", "站上职业生涯的高处，守成比进取更难。"),
    LATE("晚期选择", "开始考虑留下的东西，以及怎么交班。");

    companion object {
        fun of(positionIndex: Int, serviceYears: Int, age: Int): LifeStage = when {
            serviceYears >= 26 || age >= 54 -> LATE
            positionIndex >= 7 -> PEAK
            positionIndex >= 6 -> CORE
            positionIndex >= 4 -> MID_CAREER
            positionIndex >= 3 -> IMPORTANT
            positionIndex >= 2 || serviceYears >= 6 -> GROWTH
            serviceYears >= 2 -> GRASSROOTS
            else -> EARLY
        }
    }
}
