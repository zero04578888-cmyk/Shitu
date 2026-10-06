package com.shitu.app

import com.shitu.app.game.ArchiveFactory
import com.shitu.app.game.CareerEndingEvaluator
import com.shitu.app.game.CareerEngine
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GameState
import com.shitu.app.game.NpcCatalog
import com.shitu.app.model.AchievementCatalog
import com.shitu.app.model.CareerAbility
import com.shitu.app.model.CareerProfile
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.EndingCatalog
import com.shitu.app.model.LifeArchive
import com.shitu.app.model.LifeRecord
import com.shitu.app.model.Merit
import com.shitu.app.model.PoliticalTendency
import com.shitu.app.util.ArchiveCodec
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 第十阶段：完整仕途人生与多结局系统的端到端检查。
// 覆盖：成长上限、履历与人物记忆、结局判定分路线、多周目档案与存档往返。
class CareerLifeTest {

    private fun playCareer(seed: Long, choicePicker: (Int) -> Int): GameState {
        var state = GameEngine.newGame("生命测试者", "男", "本科", "乡镇公务员", seed)
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

    // 构造一个走到中层的稳定状态，供结局分支测试使用。
    private fun baseState(positionIndex: Int = 1, serviceYears: Int = 8): GameState {
        val state = GameEngine.newGame("分支测试者", "男", "硕士", "机关科员", 7L)
        return state.copy(
            player = state.player.copy(
                positionIndex = positionIndex,
                serviceYears = serviceYears,
                yearsInPosition = 3,
                attributes = state.player.attributes.copy(
                    work = 55,
                    ability = 55,
                    politics = 55,
                    writing = 55,
                    coordination = 50,
                    masses = 50,
                    leaderTrust = 50,
                    colleagues = 50,
                    reputation = 50,
                    integrityRisk = 15,
                    health = 80,
                    energy = 70,
                    family = 70,
                    wealth = 40
                )
            ),
            promotionCount = 1,
            warningCount = 0
        )
    }

    private fun fullMerit(value: Int = 70): Merit =
        Merit(economic = value, livelihood = value, security = value, finance = value, innovation = value)

    private fun assertKnownEnding(state: GameState) {
        val result = CareerEndingEvaluator.evaluate(state)
        assertTrue("结局标题为空", result.title.isNotBlank())
        assertTrue("结局 ${result.id} 没有收录进图鉴", EndingCatalog.find(result.id) != null)
    }

    // 测试 A：新游戏 -> 工作 -> 晋升 -> 存档 -> 重启。
    @Test
    fun testA_newGameWorkPromotionAndSaveRoundTrip() {
        val promoted = (1L..30L)
            .map { seed -> playCareer(seed) { step -> step % 3 } }
            .firstOrNull { it.promotionCount >= 1 }
        assertNotNull("模拟中应当出现至少一次晋升", promoted)
        val state = promoted!!

        assertEquals("第 0 条履历必须是参加工作", CareerRecordKind.START, state.career.records.first().kind)
        assertTrue("晋升后应当留下履历", state.career.records.any { it.kind == CareerRecordKind.PROMOTION })
        assertTrue("晋升应当解锁成就", state.career.unlockedAchievements.contains("first_promotion"))

        val text = GameCodec.encode(state)
        val restored = GameCodec.decode(text)
        assertNotNull("职业生涯存档无法还原", restored)
        val other = restored!!
        assertEquals(state.career.experience, other.career.experience)
        assertEquals(state.career.records, other.career.records)
        assertEquals(state.career.memories, other.career.memories)
        assertEquals(state.career.assets, other.career.assets)
        assertEquals(state.career.highestPositionIndex, other.career.highestPositionIndex)
        assertEquals(state.career.unlockedAchievements, other.career.unlockedAchievements)
        PoliticalTendency.entries.forEach { tendency ->
            assertEquals(
                "路线倾向 ${tendency.label} 没有正确还原",
                state.politics.tendencyOf(tendency),
                other.politics.tendencyOf(tendency)
            )
        }
        assertEquals(state.politics.history, other.politics.history)
        assertEquals(state.politics.majorChoices, other.politics.majorChoices)
        assertEquals(state.year, other.year)
        assertEquals(state.quarter, other.quarter)
        assertEquals("重复编码应当一致", text, GameCodec.encode(other))
    }

    // 测试 B：高政绩路线 -> 实干型结局。
    @Test
    fun testB_highMeritRouteEnding() {
        val base = baseState(positionIndex = 5, serviceYears = 12)
        val state = base.copy(
            route = CareerRoute.MANAGEMENT,
            politics = base.politics.withTendency(PoliticalTendency.PRAGMATIC, 10),
            governance = base.governance.copy(merit = fullMerit(72))
        )
        val result = CareerEndingEvaluator.evaluate(state)
        assertEquals("高政绩 + 实干倾向应当走实干型结局", "pragmatic_achiever", result.id)
        assertKnownEnding(state)
        assertTrue("结局原因不应为空", result.reasons.isNotEmpty())
    }

    // 测试 C：高家庭关系路线 -> 平衡人生结局。
    @Test
    fun testC_highFamilyRouteEnding() {
        val base = baseState(positionIndex = 2, serviceYears = 6)
        val state = base.copy(
            player = base.player.copy(
                attributes = base.player.attributes.copy(health = 82, family = 84)
            )
        )
        val result = CareerEndingEvaluator.evaluate(state)
        assertEquals("家庭与健康都好应当走平衡人生", "balanced_life", result.id)
        assertKnownEnding(state)
    }

    // 测试 D：高风险路线 -> 廉政风险失败。
    @Test
    fun testD_highRiskRouteEnding() {
        val base = baseState(positionIndex = 1, serviceYears = 10)
        val state = base.copy(
            player = base.player.copy(
                attributes = base.player.attributes.copy(integrityRisk = 82)
            )
        )
        val result = CareerEndingEvaluator.evaluate(state)
        assertEquals("长期高风险应当走廉政失败结局", "integrity_fail", result.id)
        assertKnownEnding(state)
    }

    // 测试 E：改革路线 -> 改革型干部。
    @Test
    fun testE_reformRouteEnding() {
        val base = baseState(positionIndex = 4, serviceYears = 11)
        val state = base.copy(
            politics = base.politics.withTendency(PoliticalTendency.REFORMIST, 10),
            governance = base.governance.copy(merit = fullMerit(70))
        )
        val result = CareerEndingEvaluator.evaluate(state)
        assertEquals("长期改革倾向应当走改革型结局", "reform_pioneer", result.id)
        assertKnownEnding(state)
    }

    // 测试 F：中途改变路线 -> 与原本不同的结局。
    @Test
    fun testF_midwayRouteChangeProducesDifferentEnding() {
        val base = baseState(positionIndex = 4, serviceYears = 11)
        val reform = base.copy(
            politics = base.politics.withTendency(PoliticalTendency.REFORMIST, 10),
            governance = base.governance.copy(merit = fullMerit(70))
        )
        val steady = base.copy(
            politics = base.politics.withTendency(PoliticalTendency.STEADY, 10),
            governance = base.governance.copy(merit = fullMerit(70))
        )
        val reformEnding = CareerEndingEvaluator.evaluate(reform).id
        val steadyEnding = CareerEndingEvaluator.evaluate(steady).id
        assertNotEquals("改变长期路线应当产生不同结局", reformEnding, steadyEnding)
        assertKnownEnding(reform)
        assertKnownEnding(steady)
    }

    // 测试 G：完成一局 -> 汇入档案 -> 图鉴保留 -> 开始第二局。
    @Test
    fun testG_multiLifeArchiveAndSecondRun() {
        val state = playCareer(seed = 21L) { step -> step % 3 }
        assertTrue("第一局应当结束", state.isFinished)
        val record = ArchiveFactory.lifeRecordOf(state)
        assertNotNull("结束的一局应当生成人生记录", record)

        val archive = LifeArchive.EMPTY
            .appended(record!!)
            .mergedWith(
                record,
                ArchiveFactory.discoveredRoutesOf(state),
                ArchiveFactory.discoveredNpcsOf(state)
            )
        assertEquals(1, archive.records.size)
        assertTrue("应当记录到最高职级", archive.highestPositionIndex >= 0)

        val restored = ArchiveCodec.decode(ArchiveCodec.encode(archive))
        assertNotNull("人生档案无法还原", restored)
        assertEquals("人生档案往返应当完全一致", archive, restored)

        val next = GameEngine.newGame(
            "第二代", "女", "硕士", "机关科员", 22L, emptyList(), archive.records.size
        )
        assertEquals("第二局应当读取周目数", 1, next.career.priorLives)
        val withAchievement = CareerEngine.checkAchievements(next)
        assertTrue(
            "第二局应当解锁重开人生",
            withAchievement.career.unlockedAchievements.contains("restart_life")
        )
    }

    // 数值平衡：经验与能力都有上限，不能无限成长。
    @Test
    fun experienceAndAbilityGrowthAreBounded() {
        var state = GameEngine.newGame("成长测试", "男", "本科", "乡镇公务员", 3L)
        assertEquals(1, state.career.level)
        state = CareerEngine.gainExperience(state, CareerProfile.EXPERIENCE_PER_LEVEL)
        assertEquals("满一级经验应当升级", 2, state.career.level)
        repeat(60) { state = CareerEngine.gainExperience(state, CareerProfile.EXPERIENCE_PER_LEVEL) }
        assertEquals("等级存在上限", CareerProfile.MAX_LEVEL, state.career.level)
        assertEquals("满级后不再需要经验", 0, state.career.experienceToNext)
        CareerAbility.entries.forEach { ability ->
            val value = state.career.valueOf(ability)
            assertTrue("能力越界：${ability.label}=$value", value in 0..CareerProfile.MAX_ABILITY)
        }
        assertEquals(
            "能力写入也必须被截断",
            CareerProfile.MAX_ABILITY,
            CareerProfile().withValue(CareerAbility.LEADERSHIP, 999).leadership
        )
    }

    // 履历与人物记忆：重要行为会被长期记住并进入存档。
    @Test
    fun careerRecordsAndNpcMemoriesAreWritten() {
        val npcId = NpcCatalog.npcs.first().id
        var state = GameEngine.newGame("记忆测试", "男", "本科", "街道公务员", 4L)
        state = CareerEngine.remember(state, npcId, "在关键任务上帮过他", 6, 3)
        state = CareerEngine.record(state, CareerRecordKind.PROJECT, "主持一次专项整治", "按期完成")
        state = CareerEngine.checkAchievements(state)

        assertTrue("人物记忆应当写入", state.career.memoryOf(npcId).isNotEmpty())
        assertEquals(6, state.career.memoryOf(npcId).last().relationBias)
        assertTrue(state.career.records.any { it.kind == CareerRecordKind.PROJECT })

        val restored = GameCodec.decode(GameCodec.encode(state))
        assertNotNull(restored)
        assertEquals(state.career.memories, restored!!.career.memories)
    }

    // 图鉴元成就：解锁多个不同结局后应当收集到结局收藏家。
    @Test
    fun metaCollectorUnlocksAcrossDistinctEndings() {
        val ids = listOf("ordinary_life", "grassroots", "balanced_life", "frustration", "normal_retire")
        var archive = LifeArchive.EMPTY
        ids.forEachIndexed { index, id ->
            val record = LifeRecord(
                name = "人生$index",
                endingId = id,
                endingTitle = id,
                endingCategory = "测试",
                positionName = "一级科员",
                unitLabel = "乡镇机关",
                serviceYears = 20,
                promotionCount = 1,
                reviewCount = 20
            )
            archive = archive.appended(record).mergedWith(record, emptySet(), emptySet())
        }
        assertEquals(ids.size, archive.unlockedCount)
        assertFalse(
            "不足门槛时不应解锁元成就",
            LifeArchive.EMPTY.appended(
                LifeRecord("单局", "grassroots", "基层", "职业", "一级科员", "乡镇", 20, 1, 20)
            ).mergedWith(archive.records.first(), emptySet(), emptySet())
                .achievements.contains(AchievementCatalog.META_COLLECTOR)
        )
        assertTrue("收集多个结局后应当解锁", archive.achievements.contains(AchievementCatalog.META_COLLECTOR))
    }

    // 全量人生模拟产出的结局必须都能回到统一判定器与图鉴。
    @Test
    fun simulatedLifeEndingsStayCoherent() {
        for (seed in 1L..10L) {
            val state = playCareer(seed) { step -> (step * 5) % 3 }
            assertTrue("seed=$seed 的人生没有结束", state.isFinished)
            assertTrue("服务年限越界", state.player.serviceYears in 1..GameEngine.MAX_SERVICE_YEARS)
            assertKnownEnding(state)
            assertTrue("结束的一局必须写入结局日志", state.log.last().title.startsWith("结局"))
        }
    }
}
