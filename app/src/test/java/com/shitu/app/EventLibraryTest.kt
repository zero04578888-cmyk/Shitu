package com.shitu.app

import com.shitu.app.game.EventLibrary
import com.shitu.app.model.Attr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 事件库内容与事件链结构检查。
class EventLibraryTest {

    @Test
    fun libraryHasEnoughEventsAndUniqueIds() {
        assertTrue("事件数量不足：${EventLibrary.events.size}", EventLibrary.events.size >= 30)
        val ids = EventLibrary.events.map { it.id }
        assertEquals("存在重复事件 id", ids.size, ids.toSet().size)
        assertTrue("随机事件池过小", EventLibrary.randomPool.size >= 25)
    }

    @Test
    fun everyEventHasThreeDistinctChoices() {
        EventLibrary.events.forEach { event ->
            assertTrue("事件 ${event.id} 选项不足三个", event.choices.size >= 3)
            assertTrue("事件 ${event.id} 缺少标题", event.title.isNotBlank())
            assertTrue("事件 ${event.id} 缺少背景", event.background.isNotBlank())
            assertTrue("事件 ${event.id} 缺少描述", event.description.isNotBlank())
            event.choices.forEach { choice ->
                assertTrue("事件 ${event.id} 存在空选项", choice.text.isNotBlank())
                assertTrue("事件 ${event.id} 存在空结果", choice.outcome.isNotBlank())
            }
            val signatures = event.choices.map { choice ->
                Attr.entries.joinToString("|") { choice.effects.valueOf(it).toString() }
            }
            assertEquals(
                "事件 ${event.id} 的三个选项结果完全相同",
                event.choices.size,
                signatures.toSet().size
            )
        }
    }

    @Test
    fun eventChainsAreComplete() {
        val chainIds = listOf(
            "road", "project", "rural", "integrity", "petition", "family", "favor"
        )
        chainIds.forEach { chainId ->
            val steps = EventLibrary.stepsOf(chainId).map { it.chainStep }.sorted()
            assertTrue("事件链 $chainId 没有节点", steps.isNotEmpty())
            assertEquals("事件链 $chainId 的步骤不连续", (1..steps.size).toList(), steps)
            val started = EventLibrary.events.any { event ->
                event.choices.any { it.startsChain == chainId }
            }
            assertTrue("没有事件可以开启事件链 $chainId", started)
        }
    }

    @Test
    fun chainStepsAreScheduledInOrder() {
        EventLibrary.stepsOf("road").forEach { step ->
            val next = EventLibrary.nextChainStep("road", step.chainStep)
            if (next != null) {
                assertTrue("事件链顺序错误", next.chainStep > step.chainStep)
            }
        }
        assertEquals(1, EventLibrary.firstChainStep("road")?.chainStep)
    }
}
