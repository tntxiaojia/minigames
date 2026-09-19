package com.tntxiaojia.minigames.game.floors

import kotlin.math.abs
import kotlin.random.Random

/** 勇下100层：平台持续上升（每 20 层加速），踩平台加层；被顶部尖刺/尖刺平台/掉出底部即结束。 */
class FloorsEngine {
    enum class Kind { NORMAL, VANISH, SPIKE, CONVEYOR_LEFT, CONVEYOR_RIGHT, BOUNCE, MOVING, PURPLE }

    data class PlatformView(val x: Float, val y: Float, val w: Float, val kind: Int)

    data class View(
        val platforms: List<PlatformView>,
        val playerX: Float,
        val playerY: Float,
        val floors: Int,
        val over: Boolean,
    )

    companion object {
        const val PLAYER_SIZE = 0.045f
        const val PLAT_W = 0.2f
        const val PLAT_H = 0.028f
        const val GRAVITY = 1.2f
        const val MAX_FALL = 1.15f
        const val RISE_SPEED = 0.06f
        const val RISE_STEP = 0.4f
        const val BOUNCE_VY = -0.85f
        const val CONVEYOR_SPEED = 0.18f
        const val HOLD_SPEED = 0.45f
        const val OPPOSITE_FACTOR = 0.6f
        const val MOVE_SPEED = 0.18f
        const val SPIKE_TOP_H = 0.03f
        const val SPIKE_H = 0.04f
        const val GAP = 0.26f
        const val VANISH_DELAY = 1.0f
    }

    private class Platform(
        var x: Float,
        var y: Float,
        val kind: Kind,
        var vx: Float = 0f,
        var stepped: Boolean = false,
        var gone: Boolean = false,
        var timer: Float = 0f,
    )

    private val platforms = ArrayList<Platform>()

    var playerX = 0.5f
        private set
    private var playerY = 0.18f
    private var vy = 0f
    private var holdDir = 0
    private var standing: Platform? = null
    var floors = 0
        private set
    var over = false
        private set

    fun newGame() {
        platforms.clear()
        playerX = 0.5f
        playerY = 0.18f
        vy = 0f
        holdDir = 0
        standing = null
        floors = 0
        over = false
        platforms.add(Platform(0.5f, 0.95f, Kind.NORMAL))
        ensureBelow(1.6f)
    }

    private fun randomKind(): Kind {
        val r = Random.nextFloat()
        return when {
            r < 0.3f -> Kind.NORMAL
            r < 0.45f -> Kind.VANISH
            r < 0.56f -> Kind.SPIKE
            r < 0.66f -> Kind.CONVEYOR_LEFT
            r < 0.76f -> Kind.CONVEYOR_RIGHT
            r < 0.85f -> Kind.BOUNCE
            r < 0.92f -> Kind.MOVING
            else -> Kind.PURPLE
        }
    }

    private fun makePlatform(y: Float): Platform {
        val kind = randomKind()
        val x = Random.nextFloat() * (1f - PLAT_W) + PLAT_W / 2f
        val vx = if (kind == Kind.MOVING || kind == Kind.PURPLE) {
            if (Random.nextBoolean()) MOVE_SPEED else -MOVE_SPEED
        } else {
            0f
        }
        return Platform(x, y, kind, vx)
    }

    private fun ensureBelow(bottomY: Float) {
        var lowest = platforms.maxOfOrNull { it.y } ?: bottomY
        while (lowest < bottomY) {
            lowest += GAP
            platforms.add(makePlatform(lowest))
        }
    }

    private fun riseSpeed(): Float = RISE_SPEED * (1f + (floors / 20) * RISE_STEP)

    fun setHoldDir(dir: Int) {
        holdDir = dir
    }

    fun moveBy(dx: Float) {
        if (over) return
        movePlayer(dx)
    }

    private fun movePlayer(dx: Float) {
        playerX = (playerX + dx).coerceIn(PLAYER_SIZE / 2f, 1f - PLAYER_SIZE / 2f)
    }

    fun step(dt: Float) {
        if (over) return

        if (holdDir != 0) {
            var speed = HOLD_SPEED
            val st = standing
            if (st != null) {
                val opposite = (st.kind == Kind.CONVEYOR_LEFT && holdDir > 0) ||
                    (st.kind == Kind.CONVEYOR_RIGHT && holdDir < 0)
                if (opposite) speed *= OPPOSITE_FACTOR
            }
            movePlayer(holdDir * speed * dt)
        }

        val rise = riseSpeed()
        for (p in platforms) {
            p.y -= rise * dt
            if (p.kind == Kind.MOVING || p.kind == Kind.PURPLE) {
                p.x += p.vx * dt
                if (p.x - PLAT_W / 2f < 0f) {
                    p.x = PLAT_W / 2f
                    p.vx = -p.vx
                }
                if (p.x + PLAT_W / 2f > 1f) {
                    p.x = 1f - PLAT_W / 2f
                    p.vx = -p.vx
                }
            }
        }

        var s = standing
        if (s != null && (s.gone || !onPlatform(s))) {
            standing = null
            s = null
        }
        if (s != null && s.kind == Kind.VANISH) {
            s.timer -= dt
            if (s.timer <= 0f) {
                s.gone = true
                standing = null
                s = null
            }
        }
        if (s != null) {
            playerY = s.y - PLAYER_SIZE / 2f
            vy = 0f
            when (s.kind) {
                Kind.CONVEYOR_LEFT -> movePlayer(-CONVEYOR_SPEED * dt)
                Kind.CONVEYOR_RIGHT -> movePlayer(CONVEYOR_SPEED * dt)
                Kind.MOVING -> movePlayer(s.vx * dt)
                else -> Unit
            }
        } else {
            vy = (vy + GRAVITY * dt).coerceAtMost(MAX_FALL)
            val prevBottom = playerY + PLAYER_SIZE / 2f
            playerY += vy * dt
            val newBottom = playerY + PLAYER_SIZE / 2f
            if (vy > 0f) {
                for (p in platforms) {
                    if (p.gone) continue
                    val overlapX = abs(playerX - p.x) <= (PLAT_W + PLAYER_SIZE) / 2f
                    val crosses = prevBottom <= p.y && newBottom >= p.y
                    // 平台上升变快时可能从下方穿过玩家，只要底部贴近平台顶面就判定接触
                    val nearTop = newBottom >= p.y - PLAT_H && newBottom <= p.y + PLAT_H + PLAYER_SIZE
                    if (!overlapX || (!crosses && !nearTop)) continue
                    when (p.kind) {
                        Kind.SPIKE -> {
                            over = true
                            return
                        }
                        Kind.VANISH -> {
                            playerY = p.y - PLAYER_SIZE / 2f
                            vy = 0f
                            standing = p
                            p.timer = VANISH_DELAY
                            countStep(p)
                        }
                        Kind.BOUNCE -> {
                            playerY = p.y - PLAYER_SIZE / 2f
                            vy = BOUNCE_VY
                            countStep(p)
                        }
                        else -> {
                            playerY = p.y - PLAYER_SIZE / 2f
                            vy = 0f
                            standing = p
                            countStep(p)
                        }
                    }
                    break
                }
            }
        }

        if (playerY - PLAYER_SIZE / 2f < SPIKE_TOP_H) {
            over = true
            return
        }
        if (playerY - PLAYER_SIZE / 2f > 1.02f) {
            over = true
            return
        }

        platforms.removeAll { it.gone || it.y < -0.2f }
        ensureBelow(1.6f)
    }

    private fun countStep(p: Platform) {
        if (p.stepped) return
        p.stepped = true
        floors++
    }

    private fun onPlatform(p: Platform): Boolean {
        val bottom = playerY + PLAYER_SIZE / 2f
        return abs(playerX - p.x) <= (PLAT_W + PLAYER_SIZE) / 2f && abs(bottom - p.y) <= PLAT_H * 2f
    }

    fun snapshot(): View {
        val list = platforms
            .filter { !it.gone && it.y in -0.2f..1.25f }
            .map { PlatformView(it.x, it.y, PLAT_W, it.kind.ordinal) }
        return View(
            platforms = list,
            playerX = playerX,
            playerY = playerY,
            floors = floors,
            over = over,
        )
    }
}
