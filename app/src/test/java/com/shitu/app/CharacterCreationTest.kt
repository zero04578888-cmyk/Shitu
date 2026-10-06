package com.shitu.app

import com.shitu.app.game.CharacterFactory
import com.shitu.app.game.GameEngine
import com.shitu.app.model.Attr
import com.shitu.app.model.HiddenKey
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 人物创建：天赋特质必须真的改变初始角色，并且能被存档完整还原。
class CharacterCreationTest {

    private fun create(traits: List<String> = emptyList()) =
        CharacterFactory.create("创建测试", "女", "本科", "机关科员", traits)

    @Test
    fun traitOptionsAreWellFormed() {
        val names = CharacterFactory.TRAIT_OPTIONS.map { it.name }
        assertEquals("天赋选项不应重名", names.size, names.toSet().size)
        assertTrue("天赋选项过少", CharacterFactory.TRAIT_OPTIONS.size >= 6)
        CharacterFactory.TRAIT_OPTIONS.forEach { trait ->
            assertTrue("${trait.name} 缺少说明", trait.description.isNotBlank())
            assertTrue("${trait.name} 缺少长处说明", trait.upside.isNotBlank())
            assertTrue("${trait.name} 缺少代价说明", trait.downside.isNotBlank())
            assertTrue("${trait.name} 没有任何属性影响", !trait.bonus.isEmpty)
            assertTrue("${trait.name} 应当至少影响一项属性", trait.bonus.entries().isNotEmpty())
        }
    }

    @Test
    fun traitsAreCappedAndDeduplicated() {
        val all = CharacterFactory.TRAIT_OPTIONS.map { it.name }
        assertEquals(
            "最多只能选两项",
            CharacterFactory.MAX_TRAITS,
            CharacterFactory.traitsOf(all).size
        )
        assertEquals(
            "重复的天赋只算一次",
            1,
            CharacterFactory.traitsOf(listOf(all[0], all[0])).size
        )
        assertTrue(
            "未知天赋应当被忽略",
            CharacterFactory.traitsOf(listOf("不存在的天赋")).isEmpty()
        )
        assertEquals(
            "超过上限时按顺序截断",
            all.take(CharacterFactory.MAX_TRAITS),
            CharacterFactory.traitsOf(all).map { it.name }
        )
    }

    @Test
    fun traitAppliesItsDeclaredEffects() {
        val trait = CharacterFactory.TRAIT_OPTIONS.first { it.name == "笔杆子" }
        val baseline = create()
        val applied = create(traits = listOf(trait.name))

        Attr.entries.forEach { attr ->
            val expected = (baseline.attributes.valueOf(attr) + trait.bonus.valueOf(attr))
                .coerceIn(0, 100)
            assertEquals(
                "属性 ${attr.label} 的天赋加成不正确",
                expected,
                applied.attributes.valueOf(attr)
            )
        }
        HiddenKey.entries.forEach { key ->
            val expected = (baseline.hidden.valueOf(key) + trait.hidden.valueOf(key))
                .coerceIn(0, 100)
            assertEquals(
                "隐藏变量 ${key.label} 的天赋加成不正确",
                expected,
                applied.hidden.valueOf(key)
            )
        }
    }

    @Test
    fun creationIsDeterministicAndStaysInRange() {
        val first = create(traits = listOf("好口才", "会来事"))
        val second = create(traits = listOf("好口才", "会来事"))
        assertEquals("相同输入必须得到相同角色", first, second)

        val traits = CharacterFactory.TRAIT_OPTIONS.map { it.name }
        for (i in traits.indices) {
            for (j in i + 1 until traits.size) {
                val player = create(traits = listOf(traits[i], traits[j]))
                Attr.entries.forEach { attr ->
                    val value = player.attributes.valueOf(attr)
                    assertTrue(
                        "属性越界：${traits[i]}+${traits[j]} ${attr.label}=$value",
                        value in 0..100
                    )
                }
                HiddenKey.entries.forEach { key ->
                    val value = player.hidden.valueOf(key)
                    assertTrue(
                        "隐藏变量越界：${traits[i]}+${traits[j]} ${key.label}=$value",
                        value in 0..100
                    )
                }
            }
        }
    }

    @Test
    fun creationChoicesAreCarriedIntoTheRun() {
        val state = GameEngine.newGame(
            "张三", "男", "硕士", "机关科员",
            seed = 42L,
            traits = listOf("笔杆子", "稳得住")
        )
        assertEquals(listOf("笔杆子", "稳得住"), state.player.traits)
    }

    @Test
    fun saveRoundTripsCreationChoices() {
        val state = GameEngine.newGame(
            "李四", "女", "本科", "街道公务员",
            seed = 7L,
            traits = listOf("酒量好")
        )
        val encoded = GameCodec.encode(state)
        val restored = GameCodec.decode(encoded)
        assertNotNull(restored)
        assertEquals(listOf("酒量好"), restored!!.player.traits)

        // 旧存档没有这些字段，应当按默认值读取而不是失败。
        val legacy = encoded.lines()
            .filterNot { it.startsWith("player.trait.") }
            .joinToString("\n")
        val legacyState = GameCodec.decode(legacy)
        assertNotNull("旧存档仍应可读", legacyState)
        assertTrue(legacyState!!.player.traits.isEmpty())
    }
}
