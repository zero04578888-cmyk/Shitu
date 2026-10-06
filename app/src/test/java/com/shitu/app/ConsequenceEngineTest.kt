package com.shitu.app

import com.shitu.app.game.ConsequenceEngine
import com.shitu.app.game.EventEngine
import com.shitu.app.game.EventLibrary
import com.shitu.app.game.GameEngine
import com.shitu.app.game.GameState
import com.shitu.app.model.Attr
import com.shitu.app.model.HiddenKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 暗线机制：隐性状态必须真的产生后果，而不是只记数字。
class ConsequenceEngineTest {

    private fun newGame(seed: Long = 3L): GameState =
        GameEngine.newGame("暗线测试", "女", "本科", "街道公务员", seed)

    private fun GameState.withAttributes(
        energy: Int? = null,
        health: Int? = null,
        integrityRisk: Int? = null,
        reputation: Int? = null,
        colleagues: Int? = null,
        leaderTrust: Int? = null
    ): GameState {
        var attrs = player.attributes
        if (energy != null) attrs = attrs.withValue(Attr.ENERGY, energy)
        if (health != null) attrs = attrs.withValue(Attr.HEALTH, health)
        if (integrityRisk != null) attrs = attrs.withValue(Attr.INTEGRITY_RISK, integrityRisk)
        if (reputation != null) attrs = attrs.withValue(Attr.REPUTATION, reputation)
        if (colleagues != null) attrs = attrs.withValue(Attr.COLLEAGUES, colleagues)
        if (leaderTrust != null) attrs = attrs.withValue(Attr.LEADER_TRUST, leaderTrust)
        return copy(player = player.copy(attributes = attrs))
    }

    private fun GameState.withHidden(
        stress: Int? = null,
        healthRisk: Int? = null,
        riskPressure: Int? = null,
        socialInfluence: Int? = null,
        network: Int? = null
    ): GameState {
        var hidden = player.hidden
        if (stress != null) hidden = hidden.withValue(HiddenKey.STRESS, stress)
        if (healthRisk != null) hidden = hidden.withValue(HiddenKey.HEALTH_RISK, healthRisk)
        if (riskPressure != null) hidden = hidden.withValue(HiddenKey.RISK_PRESSURE, riskPressure)
        if (socialInfluence != null) {
            hidden = hidden.withValue(HiddenKey.SOCIAL_INFLUENCE, socialInfluence)
        }
        if (network != null) hidden = hidden.withValue(HiddenKey.NETWORK, network)
        return copy(player = player.copy(hidden = hidden))
    }

    @Test
    fun heavyStressCostsEnergyAndHealth() {
        val state = newGame().withAttributes(energy = 80, health = 80).withHidden(stress = 75)
        val result = ConsequenceEngine.settle(state)
        assertTrue("高压应当给出提醒", result.notes.isNotEmpty())
        assertEquals(75, result.state.player.attributes.energy)
        assertEquals(77, result.state.player.attributes.health)
    }

    @Test
    fun mildStressOnlyCostsEnergy() {
        val state = newGame().withAttributes(energy = 80, health = 80).withHidden(stress = 55)
        val result = ConsequenceEngine.settle(state)
        assertEquals(78, result.state.player.attributes.energy)
        assertEquals(80, result.state.player.attributes.health)
        assertTrue("普通压力不需要打扰玩家", result.notes.isEmpty())
    }

    @Test
    fun healthRiskAccumulatesIntoHealthLoss() {
        val state = newGame().withAttributes(health = 80, energy = 80).withHidden(healthRisk = 65)
        val result = ConsequenceEngine.settle(state)
        assertEquals(77, result.state.player.attributes.health)
        assertTrue("健康隐患严重时必须提醒", result.notes.isNotEmpty())
    }

    @Test
    fun riskPressureRaisesIntegrityRiskAndCalmLowersIt() {
        val risky = newGame().withAttributes(integrityRisk = 30, reputation = 60)
            .withHidden(riskPressure = 75)
        val riskyResult = ConsequenceEngine.settle(risky)
        assertEquals(33, riskyResult.state.player.attributes.integrityRisk)
        assertEquals(58, riskyResult.state.player.attributes.reputation)
        assertTrue("高廉政压力应当给出提醒", riskyResult.notes.isNotEmpty())

        val mid = newGame().withAttributes(integrityRisk = 30).withHidden(riskPressure = 45)
        assertEquals(
            32,
            ConsequenceEngine.settle(mid).state.player.attributes.integrityRisk
        )

        val calm = newGame().withAttributes(integrityRisk = 30).withHidden(riskPressure = 5)
        assertEquals(
            29,
            ConsequenceEngine.settle(calm).state.player.attributes.integrityRisk
        )
    }

    @Test
    fun socialInfluenceAndNetworkTurnIntoReputationAndRelations() {
        val state = newGame()
            .withAttributes(reputation = 50, colleagues = 50, leaderTrust = 50)
            .withHidden(socialInfluence = 65, network = 65)
        val result = ConsequenceEngine.settle(state)
        assertEquals(52, result.state.player.attributes.reputation)
        assertEquals(52, result.state.player.attributes.colleagues)
        assertEquals(51, result.state.player.attributes.leaderTrust)
    }

    @Test
    fun drainedEnergyFeedsBackIntoStress() {
        val state = newGame().withAttributes(energy = 20).withHidden(stress = 10)
        val result = ConsequenceEngine.settle(state)
        assertTrue(
            "精力透支应当推高压力，形成恶性循环",
            result.state.player.hidden.stress > 10
        )
    }

    @Test
    fun hiddenStatesDecayOverTime() {
        val state = newGame().withHidden(riskPressure = 60, socialInfluence = 60, network = 60)
        val result = ConsequenceEngine.settle(state)
        val hidden = result.state.player.hidden
        assertEquals(60 - ConsequenceEngine.RISK_PRESSURE_DECAY, hidden.riskPressure)
        assertEquals(60 - ConsequenceEngine.SOCIAL_INFLUENCE_DECAY, hidden.socialInfluence)
        assertEquals(60 - ConsequenceEngine.NETWORK_DECAY, hidden.network)
    }

    @Test
    fun settlementIsPureAndKeepsValuesInRange() {
        val state = newGame().withHidden(
            stress = 100,
            healthRisk = 100,
            riskPressure = 100,
            socialInfluence = 100,
            network = 100
        )
        val first = ConsequenceEngine.settle(state)
        val second = ConsequenceEngine.settle(state)
        assertEquals("演算必须是纯函数，便于存档复现", first, second)

        val extremes = listOf(state, newGame().withAttributes(health = 0, energy = 0))
        extremes.forEach { sample ->
            val result = ConsequenceEngine.settle(sample).state
            Attr.entries.forEach { attr ->
                val value = result.player.attributes.valueOf(attr)
                assertTrue("属性越界：${attr.label}=$value", value in 0..100)
            }
            HiddenKey.entries.forEach { key ->
                val value = result.player.hidden.valueOf(key)
                assertTrue("隐藏变量越界：${key.label}=$value", value in 0..100)
            }
        }
    }

    @Test
    fun gatedEventsOnlyAppearWhenTheStateQualifies() {
        val gated = listOf(
            "health_check", "old_matter", "burnout_edge", "old_colleague", "public_report"
        )
        gated.forEach { id ->
            assertTrue("事件 $id 应当在事件库中", EventLibrary.find(id) != null)
        }

        val plain = newGame()
        val plainIds = EventEngine.eligibleEvents(plain).map { it.id }
        gated.forEach { id ->
            assertFalse("状态未达标时不应出现 $id", id in plainIds)
        }

        val qualified = listOf(
            newGame().withHidden(healthRisk = 50) to "health_check",
            newGame().withHidden(riskPressure = 50) to "old_matter",
            newGame().withHidden(stress = 70) to "burnout_edge",
            newGame().withHidden(network = 55) to "old_colleague",
            newGame().withHidden(socialInfluence = 55) to "public_report"
        )
        qualified.forEach { (state, id) ->
            val ids = EventEngine.eligibleEvents(state).map { it.id }
            assertTrue("状态达标后应当出现 $id", id in ids)
        }
    }
}
