package com.tntxiaojia.minigames.game.jump

import kotlin.random.Random

/** 跳一跳引擎（2D 侧视）：平台序列 + 跟随镜头；落在平台范围内即成功（保留落点），落空则下坠出屏才结束。 */
class JumpEngine {
    enum class State { IDLE, CHARGE, FLY, FALLING, OVER }

    data class PlatformView(val x: Float, val w: Float, val colorIdx: Int)

    data class View(
        val state: State,
        val power: Float,
        val score: Int,
        val platforms: List<PlatformView>,
        val activeIndex: Int,
        val playerSX: Float,
        val offsetY: Float,
        val over: Boolean,
    )

    companion object {
        const val PLAT_TOP = 0.78f
        const val MIN_JUMP = 0.14f
        const val MAX_JUMP = 0.72f
        const val CHARGE_SPEED = 0.9f
        const val PLAYER_SIZE = 0.13f
        const val CAM_LEFT = 0.2f
        const val GRAVITY = 2.2f
        private val WIDTHS = floatArrayOf(0.15f, 0.22f, 0.3f)
    }

    private class Platform(val center: Float, val width: Float, val colorIdx: Int)

    var state = State.IDLE
        private set
    var score = 0
        private set
    var power = 0f
        private set

    private val platforms = ArrayList<Platform>()
    private var standingIndex = 0
    private var playerX = 0f
    private var offsetY = 0f
    private var camX = 0f
    private var x0 = 0f
    private var dist = 0f
    private var t = 0f
    private var fallVy = 0f
    private val flightDur = 0.55f

    fun newGame() {
        state = State.IDLE
        score = 0
        power = 0f
        platforms.clear()
        platforms.add(Platform(0.35f, WIDTHS[1], 1))
        repeat(8) { addNext() }
        standingIndex = 0
        playerX = platforms[0].center
        offsetY = 0f
        t = 0f
        fallVy = 0f
        camX = playerX - CAM_LEFT
    }

    private fun addNext() {
        val last = platforms.last()
        val idx = Random.nextInt(WIDTHS.size)
        val w = WIDTHS[idx]
        val center = last.center + last.width / 2f + w / 2f + Random.nextFloat() * 0.16f + 0.08f
        platforms.add(Platform(center, w, idx))
    }

    fun press() {
        if (state == State.IDLE) {
            state = State.CHARGE
            power = 0f
        }
    }

    fun release() {
        if (state != State.CHARGE) return
        dist = MIN_JUMP + (MAX_JUMP - MIN_JUMP) * power
        x0 = playerX
        t = 0f
        state = State.FLY
    }

    fun step(dt: Float) {
        when (state) {
            State.CHARGE -> power = (power + dt * CHARGE_SPEED).coerceAtMost(1f)
            State.FLY -> {
                t += dt / flightDur
                if (t >= 1f) land()
            }
            State.FALLING -> {
                fallVy += GRAVITY * dt
                offsetY -= fallVy * dt
                if (offsetY < -1.3f) state = State.OVER
            }
            State.OVER, State.IDLE -> Unit
        }
        ensurePlatforms()
        updateCamera(dt)
    }

    private fun land() {
        val landX = x0 + dist
        var hit = -1
        for (i in platforms.indices) {
            val p = platforms[i]
            if (kotlin.math.abs(landX - p.center) <= p.width / 2f + PLAYER_SIZE / 2f) {
                hit = i
                break
            }
        }
        if (hit >= 0) {
            score++
            standingIndex = hit
            playerX = landX
            offsetY = 0f
            power = 0f
            state = State.IDLE
        } else {
            playerX = landX
            offsetY = 0f
            fallVy = 0f
            state = State.FALLING
        }
    }

    private fun ensurePlatforms() {
        while (platforms.last().center < camX + 1.4f) addNext()
    }

    private fun updateCamera(dt: Float) {
        val target = playerX - CAM_LEFT
        camX += (target - camX) * (dt * 6f).coerceAtMost(1f)
    }

    fun snapshot(): View {
        val progress = t.coerceIn(0f, 1f)
        val playerSX = when (state) {
            State.FLY -> (x0 - camX) + dist * progress
            else -> playerX - camX
        }
        val arc = if (state == State.FLY) 4f * progress * (1f - progress) * 0.34f else 0f
        val offY = when (state) {
            State.FALLING, State.OVER -> offsetY
            State.FLY -> arc
            else -> 0f
        }
        val activeIdx = when (state) {
            State.FLY, State.FALLING, State.OVER -> -1
            else -> standingIndex
        }
        return View(
            state = state,
            power = power,
            score = score,
            platforms = platforms.map { PlatformView(it.center - camX, it.width, it.colorIdx) },
            activeIndex = activeIdx,
            playerSX = playerSX,
            offsetY = offY,
            over = state == State.OVER,
        )
    }
}
