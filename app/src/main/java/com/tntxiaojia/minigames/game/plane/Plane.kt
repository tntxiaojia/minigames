package com.tntxiaojia.minigames.game.plane

import kotlin.random.Random

/** 飞机大战引擎。坐标归一化：x∈[0,1] 宽，y∈[0,1] 高（顶端 y=0）。 */
class PlaneEngine {
    data class Bullet(val x: Float, val y: Float)
    data class Enemy(val x: Float, val y: Float, val kind: Int)
    data class View(
        val px: Float,
        val blink: Boolean,
        val score: Int,
        val lives: Int,
        val over: Boolean,
        val bullets: List<Bullet>,
        val enemies: List<Enemy>,
    )

    var px = 0.5f
        private set
    var score = 0
        private set
    var lives = 3
        private set
    var over = false
        private set

    private val bullets = ArrayList<Bullet>()
    private val enemies = ArrayList<Enemy>()
    private var fireCd = 0f
    private var spawnCd = 0.6f
    private var invincible = 0f

    fun newGame() {
        px = 0.5f
        score = 0
        lives = 3
        over = false
        bullets.clear()
        enemies.clear()
        fireCd = 0f
        spawnCd = 0.6f
        invincible = 0f
    }

    fun moveBy(dx: Float) {
        if (over) return
        px = (px + dx).coerceIn(0.05f, 0.95f)
    }

    fun step(dt: Float) {
        if (over) return
        invincible = (invincible - dt).coerceAtLeast(0f)

        fireCd -= dt
        if (fireCd <= 0f) {
            fireCd = 0.16f
            bullets.add(Bullet(px, 0.86f))
        }
        spawnCd -= dt
        if (spawnCd <= 0f) {
            enemies.add(
                Enemy(
                    x = Random.nextFloat() * 0.8f + 0.1f,
                    y = -0.08f,
                    kind = Random.nextInt(3),
                )
            )
            spawnCd = Random.nextFloat() * 0.6f + 0.35f
        }

        val itB = bullets.iterator()
        while (itB.hasNext()) {
            val b = itB.next()
            if (b.y < -0.05f) itB.remove()
        }
        for (i in bullets.indices) {
            bullets[i] = Bullet(bullets[i].x, bullets[i].y - 1.05f * dt)
        }

        val speedUp = 1f + score * 0.0015f
        for (i in enemies.indices) {
            val e = enemies[i]
            val vy = (0.35f + e.kind * 0.16f) * speedUp * dt
            enemies[i] = Enemy(e.x, e.y + vy, e.kind)
        }
        enemies.removeAll { it.y > 1.1f }

        // 子弹 vs 敌机
        val hit = HashSet<Enemy>()
        for (b in bullets) {
            for (e in enemies) {
                if (hit.contains(e)) continue
                if (kotlin.math.abs(b.x - e.x) < 0.065f && kotlin.math.abs(b.y - e.y) < 0.075f) {
                    hit.add(e)
                    score += 10
                }
            }
        }
        if (hit.isNotEmpty()) {
            enemies.removeAll(hit)
            val removeB = HashSet<Bullet>()
            for (b in bullets) {
                for (e in hit) {
                    if (kotlin.math.abs(b.x - e.x) < 0.07f && kotlin.math.abs(b.y - e.y) < 0.08f) {
                        removeB.add(b)
                    }
                }
            }
            bullets.removeAll(removeB)
        }

        // 敌机 vs 玩家
        if (invincible <= 0f) {
            for (e in enemies) {
                if (kotlin.math.abs(e.x - px) < 0.1f && kotlin.math.abs(e.y - 0.88f) < 0.12f) {
                    lives--
                    invincible = 1.2f
                    enemies.remove(e)
                    if (lives <= 0) over = true
                    break
                }
            }
        }
    }

    fun snapshot(): View {
        val blink = invincible > 0f && (invincible * 12f).toInt() % 2 == 0
        return View(
            px = px,
            blink = blink,
            score = score,
            lives = lives,
            over = over,
            bullets = bullets.toList(),
            enemies = enemies.toList(),
        )
    }
}
