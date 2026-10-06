package com.shitu.app

import com.shitu.app.game.ArchiveFactory
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.model.EndingCatalog
import com.shitu.app.model.LifeArchive
import com.shitu.app.model.LifeRecord
import com.shitu.app.model.LogKind
import com.shitu.app.util.ArchiveCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 结局图鉴与收集档案：图鉴必须覆盖模拟真正会产生的结局。
class EndingCatalogTest {

    private fun playCareer(seed: Long, choicePicker: (Int) -> Int): GameState {
        var state = GameEngine.newGame("图鉴者", "女", "硕士", "机关科员", seed)
        var step = 0
        while (!state.isFinished && step < 3000) {
            step += 1
            when (state.phase) {
                GamePhase.EVENT -> state = GameEngine.resolveChoice(state, choicePicker(step))
                GamePhase.OUTCOME -> state = GameEngine.advance(state)
                GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> break
            }
        }
        return state
    }

    private fun sampleRecord(seed: Int): LifeRecord = LifeRecord(
        name = "测试者$seed",
        endingId = "normal_retire",
        endingTitle = "正常退休",
        endingCategory = "职业",
        positionName = "正科级",
        unitLabel = "区县机关",
        serviceYears = 30,
        promotionCount = 3,
        reviewCount = 30
    )

    @Test
    fun catalogEntriesAreUniqueAndReadable() {
        val ids = EndingCatalog.entries.map { it.id }
        assertEquals("图鉴存在重复条目", ids.size, ids.toSet().size)
        assertTrue("图鉴条目过少：${ids.size}", ids.size >= 18)
        EndingCatalog.entries.forEach { entry ->
            assertTrue("${entry.id} 缺少标题", entry.title.isNotBlank())
            assertTrue("${entry.id} 缺少分类", entry.category.isNotBlank())
            assertTrue("${entry.id} 缺少未解锁提示", entry.hint.isNotBlank())
            assertEquals(entry, EndingCatalog.find(entry.id))
        }
        assertNull(EndingCatalog.find("not_a_real_ending"))
    }

    @Test
    fun everyProducedEndingExistsInTheCatalog() {
        val strategies: List<(Int) -> Int> = listOf(
            { 0 },
            { 1 },
            { 2 },
            { step -> step % 3 },
            { step -> (step * 5) % 3 }
        )
        val produced = LinkedHashMap<String, Int>()
        for (strategy in strategies) {
            for (seed in 1L..40L) {
                val state = playCareer(seed, strategy)
                val ending = state.ending
                assertNotNull("seed=$seed 没有产生结局", ending)
                val id = ending!!.id
                assertTrue(
                    "结局 $id（${ending.title}）没有收录进图鉴",
                    EndingCatalog.find(id) != null
                )
                produced[id] = (produced[id] ?: 0) + 1
            }
        }
        println("模拟产生的结局：$produced")
        assertTrue("样本覆盖的结局种类过少：$produced", produced.size >= 4)
    }

    @Test
    fun finishedCareerWritesAnEndingLogEntry() {
        val state = playCareer(2026L) { it % 3 }
        assertTrue("人生应当结束", state.isFinished)
        val last = state.log.last()
        assertEquals(LogKind.ENDING, last.kind)
        assertTrue("结局日志标题异常：${last.title}", last.title.contains("结局"))
    }

    @Test
    fun lifeRecordIsBuiltFromFinishedCareerOnly() {
        val unfinished = GameEngine.newGame("未完成", "男", "本科", "街道公务员", 5L)
        assertNull("未结束的游戏不应产生记录", ArchiveFactory.lifeRecordOf(unfinished))

        val finished = playCareer(11L) { it % 3 }
        val record = ArchiveFactory.lifeRecordOf(finished)
        assertNotNull(record)
        assertTrue("记录的结局必须在图鉴中", EndingCatalog.find(record!!.endingId) != null)
        assertTrue("服务年限越界：${record.serviceYears}", record.serviceYears in 1..GameEngine.MAX_SERVICE_YEARS)
        assertEquals(finished.player.name, record.name)
        assertEquals(finished.player.positionName, record.positionName)
    }

    @Test
    fun archiveCodecRoundTripsSpecialCharacters() {
        val archive = LifeArchive(
            listOf(
                sampleRecord(1).copy(name = "带\t制表符\\反斜杠\n换行的名字"),
                sampleRecord(2)
            )
        )
        val decoded = ArchiveCodec.decode(ArchiveCodec.encode(archive))
        assertEquals(archive, decoded)
    }

    @Test
    fun archiveCodecRejectsBrokenInput() {
        assertNull(ArchiveCodec.decode(""))
        assertNull(ArchiveCodec.decode("这不是合法内容"))
        assertNull(ArchiveCodec.decode("version\t91\nrecord.count\t0\n"))
        assertNull(ArchiveCodec.decode("version\t1\nrecord.count\t2\n"))
    }

    @Test
    fun archiveAppendsAndKeepsMostRecentRecords() {
        var archive = LifeArchive.EMPTY
        repeat(40) { index -> archive = archive.appended(sampleRecord(index)) }
        assertEquals(LifeArchive.MAX_RECORDS, archive.records.size)
        assertEquals("应当保留最近一次人生", sampleRecord(39), archive.records.last())
        assertEquals("最早的人生应被裁剪", sampleRecord(10), archive.records.first())
        assertEquals(LifeArchive.MAX_RECORDS, archive.countOf("normal_retire"))
        assertEquals(1, archive.unlockedCount)
    }

    @Test
    fun archiveIgnoresConsecutiveDuplicateRecords() {
        val record = sampleRecord(7)
        val once = LifeArchive.EMPTY.appended(record)
        val twice = once.appended(record)
        assertEquals("重复读档不应重复计数", once, twice)
        assertEquals(1, twice.records.size)
    }
}
