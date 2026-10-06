package com.shitu.app

import com.shitu.app.game.GameEngine
import com.shitu.app.game.GamePhase
import com.shitu.app.game.GovernanceEngine
import com.shitu.app.model.EndingCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

// 第十一阶段：数值平衡审查。跑 100+ 局自动模拟，统计晋升与结局分布。
// 只做宽松的异常检测，避免把正常波动当成失败。
class BalanceAuditTest {

    private data class AuditRow(
        val seed: Long,
        val strategy: Int,
        val years: Int,
        val promotions: Int,
        val endingId: String,
        val peakRisk: Int,
        val minFinance: Int,
        val merit: Int
    )

    private fun pick(strategy: Int, step: Int): Int = when (strategy) {
        0 -> 0
        1 -> 1
        2 -> 2
        3 -> step % 3
        else -> (step * 7) % 3
    }

    private fun audit(seed: Long, strategy: Int): AuditRow {
        var state = GameEngine.newGame("审计者", "男", "本科", "乡镇公务员", seed)
        var peakRisk = 0
        var minFinance = state.governance.finance
        var step = 0
        while (!state.isFinished && step < 4000) {
            step += 1
            peakRisk = maxOf(peakRisk, state.relationship.risk)
            minFinance = minOf(minFinance, state.governance.finance)
            when (state.phase) {
                GamePhase.EVENT -> state = GameEngine.resolveChoice(state, pick(strategy, step))
                GamePhase.OUTCOME -> state = GameEngine.advance(state)
                GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                GamePhase.ENDING -> break
            }
        }
        return AuditRow(
            seed = seed,
            strategy = strategy,
            years = state.player.serviceYears,
            promotions = state.promotionCount,
            endingId = state.ending?.id ?: "none",
            peakRisk = peakRisk,
            minFinance = minFinance,
            merit = state.governance.merit.overall
        )
    }

    @Test
    fun balanceAuditOverManyLives() {
        val rows = ArrayList<AuditRow>()
        for (strategy in 0..4) {
            for (seed in 1L..24L) {
                rows += audit(seed, strategy)
            }
        }
        val runs = rows.size
        assertTrue("样本量不足：$runs", runs >= 100)

        val endings = LinkedHashMap<String, Int>()
        var totalYears = 0
        var totalPromotions = 0
        var totalRisk = 0
        var stuck = 0
        var earlyExits = 0
        rows.forEach { row ->
            if (row.endingId == "none") stuck += 1
            assertTrue("结局 ${row.endingId} 未收录", EndingCatalog.find(row.endingId) != null)
            endings[row.endingId] = (endings[row.endingId] ?: 0) + 1
            totalYears += row.years
            totalPromotions += row.promotions
            totalRisk += row.peakRisk
            if (row.years < 10) earlyExits += 1
            assertTrue("财政不应该为负：${row.minFinance}", row.minFinance >= 0)
            assertTrue("风险越界：${row.peakRisk}", row.peakRisk in 0..100)
        }

        val avgYears = totalYears.toDouble() / runs
        val avgPromotions = totalPromotions.toDouble() / runs
        val avgPeakRisk = totalRisk.toDouble() / runs
        val topShare = endings.values.max().toDouble() / runs

        println("=== 平衡审查：$runs 局 ===")
        println("平均服务年限：" + "%.1f".format(avgYears))
        println("平均晋升次数：" + "%.2f".format(avgPromotions))
        println("平均最高风险：" + "%.1f".format(avgPeakRisk))
        println("10 年内提前结束占比：" + "%.0f".format(earlyExits * 100.0 / runs) + "%")
        println("结局分布：$endings")

        assertTrue("有 $stuck 局无法结束", stuck == 0)
        assertTrue("结局种类过少：$endings", endings.size >= 5)
        assertTrue("单一结局占比过高：$endings", topShare <= 0.8)
        assertTrue("平均服务年限异常：$avgYears", avgYears in 12.0..45.0)
        assertTrue("平均晋升次数异常：$avgPromotions", avgPromotions >= 0.3)
        assertTrue("提前结束比例过高：" + (earlyExits * 100.0 / runs) + "%", earlyExits <= runs / 3)
    }

    // 晋升不能只靠随机：认真做政务的玩法应当确实能拿到晋升。
    @Test
    fun governanceFocusedPlayIsRewarded() {
        var promoted = 0
        for (seed in 1L..6L) {
            var state = GameEngine.newGame("治理者", "男", "本科", "乡镇公务员", seed)
            var step = 0
            while (!state.isFinished && step < 4000) {
                step += 1
                when (state.phase) {
                    GamePhase.EVENT -> state = GameEngine.resolveChoice(state, step % 3)
                    GamePhase.OUTCOME -> {
                        var guard = 0
                        while (state.governance.actionsLeft > 0 && guard < 6) {
                            guard += 1
                            val action = GovernanceEngine.availableActions(state).firstOrNull() ?: break
                            val outcome = GovernanceEngine.perform(state, action.id)
                            if (outcome.state === state) break
                            state = outcome.state
                        }
                        state = GameEngine.advance(state)
                    }
                    GamePhase.REVIEW -> state = GameEngine.proceedFromReview(state)
                    GamePhase.ENDING -> break
                }
            }
            assertTrue("治理向的一局没有结束", state.isFinished)
            if (state.promotionCount >= 1) promoted += 1
        }
        println("=== 治理向玩法：6 局中 $promoted 局获得晋升 ===")
        assertTrue("专注治理的玩法应当能获得晋升", promoted >= 1)
    }
}
