package com.shitu.app.util

import com.shitu.app.model.LifeArchive
import com.shitu.app.model.LifeRecord

// 收集档案的编解码。与 GameCodec 相同，使用纯 Kotlin 实现，
// 不依赖 Android 框架，方便单元测试。
object ArchiveCodec {

    /** v1：只有结局记录。v2：加入成就、已发现路线与人物。 */
    const val FORMAT_VERSION = 2
    private const val SEP = '\t'

    fun encode(archive: LifeArchive): String {
        val sb = StringBuilder()
        fun put(key: String, value: String) {
            sb.append(key).append(SEP).append(escape(value)).append('\n')
        }
        fun putInt(key: String, value: Int) = put(key, value.toString())
        fun putList(prefix: String, values: List<String>) {
            putInt("$prefix.count", values.size)
            values.forEachIndexed { index, value -> put("$prefix.$index", value) }
        }

        put("version", FORMAT_VERSION.toString())
        putInt("record.count", archive.records.size)
        archive.records.forEachIndexed { index, record ->
            val prefix = "record.$index"
            put("$prefix.name", record.name)
            put("$prefix.endingId", record.endingId)
            put("$prefix.endingTitle", record.endingTitle)
            put("$prefix.endingCategory", record.endingCategory)
            put("$prefix.positionName", record.positionName)
            put("$prefix.positionTitle", record.positionTitle)
            put("$prefix.unitLabel", record.unitLabel)
            putInt("$prefix.serviceYears", record.serviceYears)
            putInt("$prefix.promotionCount", record.promotionCount)
            putInt("$prefix.reviewCount", record.reviewCount)
            put("$prefix.endingTier", record.endingTier)
            putInt("$prefix.lifeScore", record.lifeScore)
            put("$prefix.highestPosition", record.highestPosition)
            putList("$prefix.ach", record.achievements)
        }
        putList("archive.ach", archive.achievements.toList())
        putList("archive.routes", archive.discoveredRoutes.toList())
        putList("archive.npcs", archive.discoveredNpcs.toList())
        putInt("archive.highest", archive.highestPositionIndex)
        return sb.toString()
    }

    /** 任何格式问题都返回 null，由调用方决定回退策略。 */
    fun decode(text: String): LifeArchive? {
        if (text.isBlank()) return null
        return try {
            val map = parse(text)
            val version = map["version"]?.toIntOrNull() ?: return null
            if (version > FORMAT_VERSION) return null
            val count = map["record.count"]?.toIntOrNull() ?: return null
            if (count < 0 || count > LifeArchive.MAX_RECORDS) return null

            val records = ArrayList<LifeRecord>(count)
            for (index in 0 until count) {
                val prefix = "record.$index"
                records.add(
                    LifeRecord(
                        name = map["$prefix.name"] ?: return null,
                        endingId = map["$prefix.endingId"] ?: return null,
                        endingTitle = map["$prefix.endingTitle"] ?: return null,
                        endingCategory = map["$prefix.endingCategory"] ?: return null,
                        positionName = map["$prefix.positionName"] ?: return null,
                        positionTitle = map["$prefix.positionTitle"].orEmpty(),
                        unitLabel = map["$prefix.unitLabel"] ?: return null,
                        serviceYears = map["$prefix.serviceYears"]?.toIntOrNull() ?: return null,
                        promotionCount = map["$prefix.promotionCount"]?.toIntOrNull() ?: return null,
                        reviewCount = map["$prefix.reviewCount"]?.toIntOrNull() ?: return null,
                        achievements = readList(map, "$prefix.ach"),
                        endingTier = map["$prefix.endingTier"].orEmpty(),
                        lifeScore = map["$prefix.lifeScore"]?.toIntOrNull() ?: 0,
                        highestPosition = map["$prefix.highestPosition"].orEmpty()
                    )
                )
            }
            LifeArchive(
                records = records,
                achievements = readList(map, "archive.ach").toSet(),
                discoveredRoutes = readList(map, "archive.routes").toSet(),
                discoveredNpcs = readList(map, "archive.npcs").toSet(),
                highestPositionIndex = map["archive.highest"]?.toIntOrNull() ?: 0
            )
        } catch (error: Exception) {
            null
        }
    }

    private fun readList(map: Map<String, String>, prefix: String): List<String> {
        val count = map["$prefix.count"]?.toIntOrNull() ?: 0
        if (count <= 0) return emptyList()
        val result = ArrayList<String>(count)
        for (index in 0 until count) {
            map["$prefix.$index"]?.let { result.add(it) }
        }
        return result
    }

    private fun parse(text: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        text.split('\n').forEach { rawLine ->
            val line = rawLine.trimEnd('\r')
            if (line.isEmpty()) return@forEach
            val index = line.indexOf(SEP)
            if (index <= 0) return@forEach
            map[line.substring(0, index)] = unescape(line.substring(index + 1))
        }
        return map
    }

    private fun escape(value: String): String {
        val sb = StringBuilder(value.length + 8)
        value.forEach { ch ->
            when (ch) {
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                SEP -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun unescape(value: String): String {
        val sb = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val ch = value[index]
            if (ch == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append(SEP)
                    else -> sb.append(next)
                }
                index += 2
            } else {
                sb.append(ch)
                index += 1
            }
        }
        return sb.toString()
    }
}
