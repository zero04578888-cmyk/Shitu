package com.shitu.app.model

/**
 * 家庭构成。家庭关系的强弱由 [Attr.FAMILY] 表示。
 */
data class FamilyState(
    val hasParents: Boolean = true,
    val hasPartner: Boolean = false,
    val children: Int = 0,
    val marriedYear: Int = 0
) {
    val summary: String
        get() {
            val parts = mutableListOf<String>()
            parts.add(if (hasParents) "父母健在" else "父母年迈")
            parts.add(if (hasPartner) "已婚" else "未婚")
            if (children > 0) parts.add("子女 $children 人")
            return parts.joinToString(" · ")
        }
}
