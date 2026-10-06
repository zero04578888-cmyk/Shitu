package com.shitu.app.model

/**
 * 第二十阶段（提前落地）：退休年龄所需的架空纪年。
 *
 * 游戏内部只记录「第 N 年」，没有现实历法。为了在存档里保存
 * 出生年份并据此计算年龄，这里定义一个完全虚构的纪元起点：
 * 游戏第 1 年对应架空纪年 [EPOCH_YEAR] 年。
 *
 * 与任何现实历法、现实事件、现实人物无关。
 */
object GameCalendar {

    /** 架空纪元的开局之年（游戏第 1 年）。 */
    const val EPOCH_YEAR: Int = 1000

    /** 游戏第 [gameYear] 年对应的架空纪年。 */
    fun worldYear(gameYear: Int): Int = EPOCH_YEAR + (gameYear - 1).coerceAtLeast(0)

    /** 由开局年龄回推出生年份（旧存档没有 birthYear 时使用）。 */
    fun birthYearOf(ageAtStart: Int): Int = EPOCH_YEAR - ageAtStart.coerceAtLeast(0)

    /** 由出生年份与当前游戏年份计算年龄，最小为 0。 */
    fun ageOf(birthYear: Int, gameYear: Int): Int =
        (worldYear(gameYear) - birthYear).coerceAtLeast(0)
}