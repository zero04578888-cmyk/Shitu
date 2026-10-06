package com.shitu.app

import com.shitu.app.model.Attr
import com.shitu.app.model.AttrDelta
import com.shitu.app.model.Attributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 属性始终保持在 0 ~ 100 之间。
class AttributeTest {

    @Test
    fun attributesAreClampedOnWrite() {
        val attrs = Attributes()
        assertEquals(100, attrs.withValue(Attr.WORK, 250).work)
        assertEquals(0, attrs.withValue(Attr.WORK, -80).work)
        assertEquals(100, attrs.withValue(Attr.INTEGRITY_RISK, 1000).integrityRisk)
    }

    @Test
    fun applyingDeltaStaysInRange() {
        val attrs = Attributes(work = 96, health = 3, integrityRisk = 2)
        val result = attrs.apply(AttrDelta(work = 30, health = -40, integrityRisk = -10))
        assertEquals(100, result.work)
        assertEquals(0, result.health)
        assertEquals(0, result.integrityRisk)
        Attr.entries.forEach { attr ->
            val value = result.valueOf(attr)
            assertTrue("属性 ${attr.label} 越界：$value", value in 0..100)
        }
    }

    @Test
    fun deltaOnlyKeepsNonZeroEntries() {
        val delta = AttrDelta(work = 8, masses = -4)
        assertEquals(2, delta.entries().size)
        assertEquals(Attr.WORK, delta.entries()[0].first)
        assertEquals(Attr.MASSES, delta.entries()[1].first)
        assertTrue(AttrDelta().isEmpty)
    }

    @Test
    fun normalizedFixesOutOfRangeValues() {
        val broken = Attributes(work = 999, health = -20)
        val fixed = broken.normalized()
        assertEquals(100, fixed.work)
        assertEquals(0, fixed.health)
    }
}
