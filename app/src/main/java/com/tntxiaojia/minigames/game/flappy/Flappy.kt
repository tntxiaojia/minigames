package com.tntxiaojia.minigames.game.flappy

import kotlin.random.Random

/** 像素鸟引擎。坐标归一化：x∈[0,1]，y∈[0,1]（顶端 y=0），鸟固定横向位置。 */
class FlappyEngine {
    enum class State { READY, RUN, OVER }

    data class Pipe(val x: Float, val gapTop: Float, val scored: Boolean = false)
    data class View(
        val state: State,
        val y: Float,
        val vy: Float,
        val score: Int,
        val pipes: List<Pipe>,
        val over: Boolean,
    )

    companion object {
        const val BIRD_X = 0.3f
        const val BIRD_R = 0.045f
        const val PIPE_W = 0.11f
        const val GAP_H = 0.3f
    }

    var state = State.READY
        private set
    var score = 0
        private set
    var y = 0.45f
        private set
    var vy = 0f
        private set

    private val pipes = ArrayList<Pipe>()
    private var spawnCd = 1.2f

    fun newGame() {
        state = State.READY
        score = 0
        y = 0.45f
        vy = 0f
        pipes.clear()
        spawnCd = 1.2f
    }

    fun start() {
        if (state == State.READY) {
            state = State.RUN
            flap()
        }
    }

    fun flap() {
        if (state != State.RUN) return
        vy = -0.72f
    }

    fun step(dt: Float) {
        if (state != State.RUN) return
        vy += 1.9f * dt
        y += vy * dt

        spawnCd -= dt
        if (spawnCd <= 0f) {
            spawnCd = Random.nextFloat() * 0.5f + 1.15f
            val gapTop = Random.nextFloat() * 0.4f + 0.12f
            pipes.add(Pipe(1.05f, gapTop))
        }
        for (i in pipes.indices) {
            pipes[i] = Pipe(pipes[i].x - 0.55f * dt, pipes[i].gapTop, pipes[i].scored)
        }
        pipes.removeAll { it.x < -PIPE_W }

        // 通过即计分（管道继续左移，移出屏幕后才清理）
        for (i in pipes.indices) {
            val p = pipes[i]
            if (!p.scored && p.x + PIPE_W < BIRD_X - BIRD_R) {
                pipes[i] = Pipe(p.x, p.gapTop, scored = true)
                score++
            }
        }

        if (y <= 0.02f || y >= 0.98f) {
            gameOver()
            return
        }
        for (p in pipes) {
            val overlapX = BIRD_X + BIRD_R > p.x && BIRD_X - BIRD_R < p.x + PIPE_W
            if (overlapX) {
                val hitPipe = y - BIRD_R < p.gapTop || y + BIRD_R > p.gapTop + GAP_H
                if (hitPipe) {
                    gameOver()
                    return
                }
            }
        }
    }

    private fun gameOver() {
        state = State.OVER
    }

    fun snapshot(): View = View(
        state = state,
        y = y,
        vy = vy,
        score = score,
        pipes = pipes.toList(),
        over = state == State.OVER,
    )
}
