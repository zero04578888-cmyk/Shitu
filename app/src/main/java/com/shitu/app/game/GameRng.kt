package com.shitu.app.game

// 可存档的随机数发生器。同一个存档的随机序列可以完整复现。
class GameRng(seed: Long) {

    var state: Long = if (seed == 0L) DEFAULT_SEED else seed
        private set

    fun nextInt(bound: Int): Int {
        if (bound <= 0) return 0
        advance()
        return ((state ushr 33) % bound).toInt()
    }

    fun nextDouble(): Double {
        advance()
        return (state ushr 11).toDouble() / (1L shl 53).toDouble()
    }

    private fun advance() {
        state = state * MULTIPLIER + INCREMENT
    }

    companion object {
        const val DEFAULT_SEED = 20261004L
        private const val MULTIPLIER = 6364136223846793005L
        private const val INCREMENT = 1442695040888963407L
    }
}
