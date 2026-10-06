package com.shitu.app.util

/**
 * 第十四阶段：存档完整性校验。
 *
 * 使用纯 Kotlin 实现的轻量校验和，用来发现被截断、半写入或损坏的存档。
 * 它不是安全意义上的加密，只服务于「主存档损坏时回退到备份」这一稳定目标。
 * 不依赖 Android 框架，便于在单元测试中直接验证。
 */
object SaveIntegrity {

    // FNV-1a 64 位常量：实现简单、无依赖，对内容与长度的变化都敏感。
    private const val FNV_OFFSET: Long = -3750763034362895579L
    private const val FNV_PRIME: Long = 1099511628211L

    /** 计算文本的校验和，返回固定的 16 进制字符串。 */
    fun checksum(text: String): String {
        var hash = FNV_OFFSET
        text.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= FNV_PRIME
        }
        return java.lang.Long.toUnsignedString(hash, 16)
    }

    /**
     * 校验文本是否与记录一致。
     * [expected] 为空表示这是没有校验和的旧存档，按兼容处理直接通过。
     */
    fun verify(text: String, expected: String?): Boolean {
        if (expected.isNullOrBlank()) return true
        return checksum(text) == expected
    }

    /** 存档回退决策的结果。 */
    enum class Decision { NONE, PRIMARY, BACKUP, CORRUPT }

    /**
     * 决定读取主存档还是备份。
     * 校验通过者优先；主存档不可信而备份可信时回退备份；
     * 两份都存在但都不可用时返回 [Decision.CORRUPT]；都不存在返回 [Decision.NONE]。
     */
    fun decide(
        primary: String?,
        primaryChecksum: String?,
        backup: String?,
        backupChecksum: String?
    ): Decision {
        if (primary != null && verify(primary, primaryChecksum)) return Decision.PRIMARY
        if (backup != null && verify(backup, backupChecksum)) return Decision.BACKUP
        return if (primary != null || backup != null) Decision.CORRUPT else Decision.NONE
    }

    /** 从存档文本里读出格式版本；读不到返回 0。 */
    fun versionOf(text: String): Int {
        text.lineSequence().forEach { line ->
            val index = line.indexOf('\t')
            if (index <= 0) return@forEach
            if (line.substring(0, index) == "version") {
                return line.substring(index + 1).trim().toIntOrNull() ?: 0
            }
        }
        return 0
    }
}