package com.shitu.app.game

import com.shitu.app.model.CityLevel
import com.shitu.app.model.GameEvent

/**
 * 第十六阶段：城市专属事件池总入口。
 *
 * 玩家在不同城市任职会遇到完全不同的事件：科技城谈芯片与人才，
 * 旅游城市谈旺季与品牌，资源城市谈转型，沿海城市谈港口与外贸。
 * 事件通过 [GameEvent.requiredCityTags] 与城市标签绑定，
 * 由 [EventEngine] 在抽取时按当前任职城市过滤。
 */
object CityEventLibrary {

    val events: List<GameEvent> =
        cityEventLibraryA + cityEventLibraryB + cityEventLibraryC + cityEventLibraryD

    /** 事件 id 覆盖到的城市层级（供测试与数据校验使用）。 */
    val supportedLevels: List<CityLevel> = CityLevel.entries

    fun find(id: String): GameEvent? = events.firstOrNull { it.id == id }

    /** 任职城市标签下可见的事件。 */
    fun visibleFor(tags: Set<String>): List<GameEvent> =
        events.filter { it.requiredCityTags.isEmpty() || it.requiredCityTags.any { tag -> tag in tags } }
}