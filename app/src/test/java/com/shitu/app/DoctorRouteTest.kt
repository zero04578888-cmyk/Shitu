package com.shitu.app

import com.shitu.app.game.CharacterFactory
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.PromotionEngine
import com.shitu.app.game.RetirementEngine
import com.shitu.app.model.FIRST_GRADE_INDEX
import com.shitu.app.model.POSITION_LADDER
import com.shitu.app.model.PositionRepository
import com.shitu.app.model.UnitType
import com.shitu.app.model.nextPromotionIndex
import com.shitu.app.util.GameCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 人物创建：学历对应的固定起始年龄，以及博士专属的「中央选调生」路线。
 *
 * 规则：
 * - 大专 / 本科 / 硕士 22 岁起步，博士 26 岁起步，不存在任何随机年龄；
 * - 博士只能选中央选调生，直接以副处级进入游戏，不再从科员起步；
 * - 大专 / 本科 / 硕士 仍走普通基层路线，并保留初始职业选择；
 * - 起点身份进入存档、履历、职级与事件系统，而不是只改显示文字。
 */
class DoctorRouteTest {

    private fun create(education: String, start: String) =
        CharacterFactory.create("创建测试", "男", education, start)

    // ===== 一、学历对应的固定起始年龄 =====

    @Test
    fun educationStartingAgesAreFixed() {
        assertEquals(22, CharacterFactory.startingAgeOf("大专"))
        assertEquals(22, CharacterFactory.startingAgeOf("本科"))
        assertEquals(22, CharacterFactory.startingAgeOf("硕士"))
        assertEquals(26, CharacterFactory.startingAgeOf("博士"))

        val ages = CharacterFactory.EDUCATION_OPTIONS.map { it.ageAtStart }
        assertEquals("学历数量应为 4 项", 4, ages.size)
        assertEquals("只允许出现 22 与 26 两种起始年龄", setOf(22, 26), ages.toSet())

        CharacterFactory.EDUCATION_OPTIONS.forEach { option ->
            val player = create(option.name, CharacterFactory.defaultStartOf(option.name))
            assertEquals(
                "${option.name} 的起始年龄必须是固定值",
                option.ageAtStart,
                player.ageAtStart
            )
            assertEquals("开局服务年限必须为 0", 0, player.serviceYears)
            assertEquals("开局年龄等于起始年龄", option.ageAtStart, player.age)
        }
    }

    @Test
    fun educationOptionsAreUniqueAndComplete() {
        val names = CharacterFactory.EDUCATION_OPTIONS.map { it.name }
        assertEquals(listOf("大专", "本科", "硕士", "博士"), names)
        assertEquals("学历名称不得重复", names.size, names.toSet().size)
    }

    // ===== 二、博士专属初始职业 =====

    @Test
    fun doctorOnlyOffersCentralSelected() {
        assertEquals(
            listOf(CharacterFactory.CENTRAL_SELECTED_START),
            CharacterFactory.startOptionsFor("博士").map { it.name }
        )
        for (education in listOf("大专", "本科", "硕士")) {
            val options = CharacterFactory.startOptionsFor(education).map { it.name }
            assertTrue("$education 应保留普通起点", options.size >= 4)
            assertFalse(
                "$education 不应出现中央选调生",
                options.contains(CharacterFactory.CENTRAL_SELECTED_START)
            )
        }
    }

    @Test
    fun doctorStartsAtDeputyDivisionAsCentralSelected() {
        val player = create("博士", CharacterFactory.CENTRAL_SELECTED_START)
        val deputyRank = PositionRepository.CENTRAL_SELECTED_RANK_INDEX

        assertEquals("副处级下标必须来自职位阶梯", 5, deputyRank)
        assertEquals("副处级", POSITION_LADDER[deputyRank].name)
        assertEquals(26, player.ageAtStart)
        assertEquals(26, player.age)
        assertEquals(deputyRank, player.positionIndex)
        assertEquals(CharacterFactory.CENTRAL_SELECTED_START, player.startTrack)
        assertEquals(PositionRepository.CENTRAL_SELECTED_ID, player.appointmentId)
        assertEquals(CharacterFactory.CENTRAL_SELECTED_START, player.currentPositionName)
        assertEquals("副处级·中央选调生", player.displayTitle)
        assertTrue("开局职级必须高于科员", player.positionIndex >= FIRST_GRADE_INDEX)
        assertFalse("博士不能再从科员起步", player.positionName == "科员")
    }

    @Test
    fun doctorIgnoresNormalStartTrack() {
        val player = create("博士", "机关科员")
        assertEquals(
            "博士传入普通起点时应被忽略，固定为中央选调生",
            CharacterFactory.CENTRAL_SELECTED_START,
            player.startTrack
        )
        assertEquals(PositionRepository.CENTRAL_SELECTED_RANK_INDEX, player.positionIndex)
        assertEquals(26, player.ageAtStart)
    }

    @Test
    fun nonDoctorCannotUseCentralSelectedStart() {
        val player = create("本科", CharacterFactory.CENTRAL_SELECTED_START)
        assertEquals(
            "中央选调生是博士专属",
            CharacterFactory.START_OPTIONS.first().name,
            player.startTrack
        )
        assertEquals(0, player.positionIndex)
        assertEquals(22, player.ageAtStart)
    }

    // ===== 三、进入游戏后的真实逻辑 =====

    @Test
    fun doctorEntersGameWithRealIdentity() {
        val state = GameEngine.newGame(
            "博士测试", "男", "博士", CharacterFactory.CENTRAL_SELECTED_START, seed = 11L
        )
        assertEquals("副处级·中央选调生", state.displayPositionTitle)
        assertEquals(PositionRepository.CENTRAL_SELECTED_RANK_INDEX, state.player.positionIndex)
        assertEquals(26, state.playerAge)
        assertEquals("中央机关", state.currentDepartment)
        assertEquals("政策综合", state.currentPositionRoute)
        assertTrue("中央选调生必须有真实的对口事件标签", state.currentPositionTags.isNotEmpty())
        assertEquals(
            "履历第一条必须是参加工作",
            com.shitu.app.model.CareerRecordKind.START,
            state.career.records.first().kind
        )
        assertEquals(
            PositionRepository.CENTRAL_SELECTED_ID,
            state.currentAppointment?.id
        )
    }

    @Test
    fun doctorPromotionLeavesCentralSelectedForRankPositions() {
        val state = GameEngine.newGame(
            "博士晋升", "女", "博士", CharacterFactory.CENTRAL_SELECTED_START, seed = 3L
        )
        val next = nextPromotionIndex(state.player.positionIndex)
        assertEquals("副处级之后应是正处级", 6, next)

        val options = PromotionEngine.eligibleOptions(state)
        assertTrue("正处级必须至少有 3 个具体职务", options.size >= 3)
        options.forEach { option ->
            assertEquals("职务必须属于正处级", 6, option.rankIndex)
            assertFalse(
                "晋升后的职位池不应再出现中央选调生",
                option.id == PositionRepository.CENTRAL_SELECTED_ID
            )
        }
    }

    @Test
    fun centralSelectedPositionIsResolvedByRepository() {
        val matched = PositionRepository.match(
            rankIndex = PositionRepository.CENTRAL_SELECTED_RANK_INDEX,
            cityId = null,
            unit = UnitType.GENERAL,
            positionId = PositionRepository.CENTRAL_SELECTED_ID,
            positionName = CharacterFactory.CENTRAL_SELECTED_START
        )
        assertNotNull("中央选调生必须能被职位数据层识别", matched)
        assertEquals(PositionRepository.CENTRAL_SELECTED_NAME, matched!!.title)
        assertTrue(matched.eventTags.isNotEmpty())
        assertEquals(
            PositionRepository.CENTRAL_SELECTED_RANK_INDEX,
            PositionRepository.rankOfId(PositionRepository.CENTRAL_SELECTED_ID)
        )
    }

    // ===== 四、年龄与退休规则联动 =====

    @Test
    fun ageMilestonesFollowTheGameYear() {
        val bachelor = GameEngine.newGame("本科", "男", "本科", "机关科员", seed = 1L)
        assertEquals("第 1 年应为 22 岁", 22, bachelor.player.age)
        val at64 = bachelor.copy(player = bachelor.player.copy(serviceYears = 42))
        assertEquals("第 43 年应为 64 岁", 64, at64.player.age)
        assertFalse("64 岁不应触发退休", RetirementEngine.isDue(at64))

        val at65 = bachelor.copy(player = bachelor.player.copy(serviceYears = 43))
        assertEquals("第 44 年应为 65 岁", 65, at65.player.age)
        assertTrue("低于副国级，65 岁必须触发退休节点", RetirementEngine.isDue(at65))
    }

    @Test
    fun doctorRetiresUnderTheSameRule() {
        val doctor = GameEngine.newGame(
            "博士退休", "男", "博士", CharacterFactory.CENTRAL_SELECTED_START, seed = 2L
        )
        assertEquals(26, doctor.player.age)
        val at65 = doctor.copy(player = doctor.player.copy(serviceYears = 39))
        assertEquals(65, at65.player.age)
        assertTrue("副处级低于副国级，65 岁必须进入退休节点", RetirementEngine.isDue(at65))

        val exempt = doctor.copy(
            player = doctor.player.copy(
                positionIndex = RetirementEngine.EXEMPT_RANK_INDEX,
                serviceYears = 45
            )
        )
        assertEquals(71, exempt.player.age)
        assertFalse("副国级及以上不受年龄限制", RetirementEngine.isDue(exempt))
    }

    // ===== 五、存档 =====

    @Test
    fun doctorRunSurvivesSaveAndReload() {
        val state = GameEngine.newGame(
            "存档博士", "女", "博士", CharacterFactory.CENTRAL_SELECTED_START, seed = 5L
        )
        val restored = GameCodec.decode(GameCodec.encode(state))
        assertNotNull("博士存档必须能读取", restored)
        val other = restored!!
        assertEquals("博士", other.player.education)
        assertEquals(CharacterFactory.CENTRAL_SELECTED_START, other.player.startTrack)
        assertEquals(26, other.player.ageAtStart)
        assertEquals(PositionRepository.CENTRAL_SELECTED_RANK_INDEX, other.player.positionIndex)
        assertEquals("副处级·中央选调生", other.displayPositionTitle)
        assertEquals(GamePhase.EVENT, other.phase)
    }

    @Test
    fun saveExposesStartingFields() {
        val doctor = GameEngine.newGame(
            "字段博士", "男", "博士", CharacterFactory.CENTRAL_SELECTED_START, seed = 7L
        )
        val save = com.shitu.app.game.GameSave(state = doctor)
        assertEquals("博士", save.education)
        assertEquals(26, save.startingAge)
        assertEquals("副处级", save.initialRank)
        assertEquals(CharacterFactory.CENTRAL_SELECTED_START, save.initialPosition)
        assertTrue(save.isDoctorStart)
        assertEquals("政策综合", save.careerRoute)

        val bachelor = GameEngine.newGame("字段本科", "女", "本科", "街道公务员", seed = 8L)
        val bachelorSave = com.shitu.app.game.GameSave(state = bachelor)
        assertEquals(22, bachelorSave.startingAge)
        assertEquals("科员", bachelorSave.initialRank)
        assertFalse(bachelorSave.isDoctorStart)
    }

    // ===== 六、完整模拟：博士路线不崩溃 =====

    @Test
    fun doctorCareerRunsToAValidEnding() {
        var state = GameEngine.newGame(
            "全程博士", "男", "博士", CharacterFactory.CENTRAL_SELECTED_START, seed = 21L
        )
        var guard = 0
        while (!state.isFinished && guard < 4000) {
            guard += 1
            state = when (state.phase) {
                GamePhase.EVENT -> GameEngine.resolveChoice(state, guard % 3)
                GamePhase.OUTCOME -> GameEngine.advance(state)
                GamePhase.REVIEW -> GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> state
            }
        }
        assertTrue("博士路线必须能正常结束", state.isFinished)
        assertNotNull(state.ending)
        assertTrue(
            "年龄应随年份正常增长：${state.player.age}",
            state.player.age >= 26 && state.player.age <= 26 + GameEngine.MAX_SERVICE_YEARS
        )
        assertTrue(
            "博士的最高职级不应低于副处级",
            state.career.highestPositionIndex >= PositionRepository.CENTRAL_SELECTED_RANK_INDEX
        )
    }
}