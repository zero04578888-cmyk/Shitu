package com.shitu.app.model

/** 属性分组，仅用于界面配色。 */
enum class AttrGroup {
    PERFORMANCE,
    RELATION,
    RISK,
    SELF
}

/**
 * 十四项可见属性。
 * [higherIsBetter] 为 false 时表示数值越高越不利（当前仅廉政风险）。
 */
enum class Attr(
    val label: String,
    val group: AttrGroup,
    val higherIsBetter: Boolean = true
) {
    WORK("工作实绩", AttrGroup.PERFORMANCE),
    ABILITY("工作能力", AttrGroup.PERFORMANCE),
    POLITICS("政治素养", AttrGroup.PERFORMANCE),
    WRITING("文字能力", AttrGroup.PERFORMANCE),
    COORDINATION("协调能力", AttrGroup.PERFORMANCE),
    MASSES("群众基础", AttrGroup.RELATION),
    LEADER_TRUST("领导信任", AttrGroup.RELATION),
    COLLEAGUES("同事关系", AttrGroup.RELATION),
    REPUTATION("个人声望", AttrGroup.RELATION),
    INTEGRITY_RISK("廉政风险", AttrGroup.RISK, higherIsBetter = false),
    HEALTH("健康", AttrGroup.SELF),
    ENERGY("精力", AttrGroup.SELF),
    FAMILY("家庭关系", AttrGroup.SELF),
    WEALTH("财富", AttrGroup.SELF);

    companion object {
        fun byName(value: String): Attr? = entries.firstOrNull { it.name == value }
    }
}

const val ATTR_MIN = 0
const val ATTR_MAX = 100

fun clampAttr(value: Int): Int = value.coerceIn(ATTR_MIN, ATTR_MAX)

/**
 * 玩家属性快照。所有数值始终位于 0～100。
 */
data class Attributes(
    val work: Int = 50,
    val ability: Int = 50,
    val politics: Int = 50,
    val writing: Int = 50,
    val coordination: Int = 50,
    val masses: Int = 50,
    val leaderTrust: Int = 50,
    val colleagues: Int = 50,
    val reputation: Int = 50,
    val integrityRisk: Int = 10,
    val health: Int = 80,
    val energy: Int = 75,
    val family: Int = 70,
    val wealth: Int = 40
) {
    fun valueOf(attr: Attr): Int = when (attr) {
        Attr.WORK -> work
        Attr.ABILITY -> ability
        Attr.POLITICS -> politics
        Attr.WRITING -> writing
        Attr.COORDINATION -> coordination
        Attr.MASSES -> masses
        Attr.LEADER_TRUST -> leaderTrust
        Attr.COLLEAGUES -> colleagues
        Attr.REPUTATION -> reputation
        Attr.INTEGRITY_RISK -> integrityRisk
        Attr.HEALTH -> health
        Attr.ENERGY -> energy
        Attr.FAMILY -> family
        Attr.WEALTH -> wealth
    }

    fun withValue(attr: Attr, value: Int): Attributes = when (attr) {
        Attr.WORK -> copy(work = clampAttr(value))
        Attr.ABILITY -> copy(ability = clampAttr(value))
        Attr.POLITICS -> copy(politics = clampAttr(value))
        Attr.WRITING -> copy(writing = clampAttr(value))
        Attr.COORDINATION -> copy(coordination = clampAttr(value))
        Attr.MASSES -> copy(masses = clampAttr(value))
        Attr.LEADER_TRUST -> copy(leaderTrust = clampAttr(value))
        Attr.COLLEAGUES -> copy(colleagues = clampAttr(value))
        Attr.REPUTATION -> copy(reputation = clampAttr(value))
        Attr.INTEGRITY_RISK -> copy(integrityRisk = clampAttr(value))
        Attr.HEALTH -> copy(health = clampAttr(value))
        Attr.ENERGY -> copy(energy = clampAttr(value))
        Attr.FAMILY -> copy(family = clampAttr(value))
        Attr.WEALTH -> copy(wealth = clampAttr(value))
    }

    /** 所有属性都夹在 0～100 之间。 */
    fun normalized(): Attributes = Attr.entries.fold(this) { acc, attr ->
        acc.withValue(attr, acc.valueOf(attr))
    }

    fun apply(delta: AttrDelta): Attributes = delta.entries().fold(this) { acc, pair ->
        acc.withValue(pair.first, acc.valueOf(pair.first) + pair.second)
    }
}

/**
 * 属性变化量。未列出的属性表示 0。
 */
data class AttrDelta(
    val work: Int = 0,
    val ability: Int = 0,
    val politics: Int = 0,
    val writing: Int = 0,
    val coordination: Int = 0,
    val masses: Int = 0,
    val leaderTrust: Int = 0,
    val colleagues: Int = 0,
    val reputation: Int = 0,
    val integrityRisk: Int = 0,
    val health: Int = 0,
    val energy: Int = 0,
    val family: Int = 0,
    val wealth: Int = 0
) {
    fun valueOf(attr: Attr): Int = when (attr) {
        Attr.WORK -> work
        Attr.ABILITY -> ability
        Attr.POLITICS -> politics
        Attr.WRITING -> writing
        Attr.COORDINATION -> coordination
        Attr.MASSES -> masses
        Attr.LEADER_TRUST -> leaderTrust
        Attr.COLLEAGUES -> colleagues
        Attr.REPUTATION -> reputation
        Attr.INTEGRITY_RISK -> integrityRisk
        Attr.HEALTH -> health
        Attr.ENERGY -> energy
        Attr.FAMILY -> family
        Attr.WEALTH -> wealth
    }

    /** 只保留非零项，顺序固定，便于界面展示与存档。 */
    fun entries(): List<Pair<Attr, Int>> =
        Attr.entries.map { it to valueOf(it) }.filter { it.second != 0 }

    val isEmpty: Boolean get() = entries().isEmpty()
}
