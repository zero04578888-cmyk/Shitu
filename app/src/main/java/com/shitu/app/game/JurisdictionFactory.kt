package com.shitu.app.game

import com.shitu.app.model.Indicator
import com.shitu.app.model.Jurisdiction
import com.shitu.app.model.JurisdictionLevel
import com.shitu.app.model.UnitType

/**
 * 辖区生成。官职越高、平台越大，管的地方越大。
 * 晋升到跨层级的岗位时会换辖区，但会保留一半的治理成果。
 */
object JurisdictionFactory {

    fun levelFor(unit: UnitType, positionIndex: Int): JurisdictionLevel = when {
        positionIndex >= 6 -> JurisdictionLevel.COUNTY
        positionIndex >= 4 -> when (unit) {
            UnitType.TOWN -> JurisdictionLevel.KEY_TOWN
            UnitType.STREET -> JurisdictionLevel.STREET
            UnitType.BUSINESS -> JurisdictionLevel.KEY_TOWN
            else -> JurisdictionLevel.COUNTY
        }
        unit == UnitType.STREET -> JurisdictionLevel.STREET
        unit == UnitType.CITY || unit == UnitType.GENERAL || unit == UnitType.COUNTY ->
            JurisdictionLevel.STREET
        else -> JurisdictionLevel.TOWNSHIP
    }

    fun forPlayer(unit: UnitType, positionIndex: Int): Jurisdiction =
        profileOf(levelFor(unit, positionIndex))

    fun profileOf(level: JurisdictionLevel): Jurisdiction {
        val base = when (level) {
            JurisdictionLevel.TOWNSHIP -> listOf(46, 48, 55, 45, 44, 46, 55, 50, 60)
            JurisdictionLevel.STREET -> listOf(55, 52, 52, 55, 52, 58, 48, 50, 58)
            JurisdictionLevel.KEY_TOWN -> listOf(52, 50, 54, 50, 48, 52, 50, 50, 58)
            JurisdictionLevel.COUNTY -> listOf(58, 55, 55, 56, 54, 56, 52, 50, 60)
        }
        return Jurisdiction(
            name = level.defaultName,
            level = level,
            population = level.population,
            economy = base[0],
            livelihood = base[1],
            security = base[2],
            education = base[3],
            medical = base[4],
            infrastructure = base[5],
            environment = base[6],
            approval = base[7],
            officialSatisfaction = base[8]
        )
    }

    /**
     * 换辖区：新辖区沿用一半旧成果，避免升职后政绩归零，
     * 也不会把小镇的成绩直接带成县区的成绩。
     */
    fun upgraded(current: Jurisdiction, level: JurisdictionLevel): Jurisdiction {
        val fresh = profileOf(level)
        var result = fresh
        Indicator.entries.forEach { indicator ->
            val blended = (fresh.valueOf(indicator) + current.valueOf(indicator)) / 2
            result = result.withValue(indicator, blended)
        }
        return result
    }
}
