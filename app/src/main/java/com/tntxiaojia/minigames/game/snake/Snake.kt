package com.tntxiaojia.minigames.game.snake

import kotlin.random.Random

/** 贪吃蛇引擎：20x20 网格，方向 0上/1右/2下/3左。 */
class Snake {
    data class View(
        val body: List<Int>,
        val food: Int,
        val dir: Int,
        val score: Int,
        val over: Boolean,
    )

    companion object {
        const val N = 20
    }

    private val body = ArrayDeque<Int>()
    private val dirQueue = ArrayDeque<Int>()
    private var dir = 1
    private var acc = 0f
    var score = 0
        private set
    var over = false
        private set
    private var food = 0

    fun newGame() {
        body.clear()
        dirQueue.clear()
        dir = 1
        score = 0
        over = false
        acc = 0f
        body.addFirst(idx(10, 10))
        body.addLast(idx(9, 10))
        body.addLast(idx(8, 10))
        placeFood()
    }

    private fun idx(x: Int, y: Int): Int = y * N + x

    fun requestDir(d: Int) {
        if (over) return
        val last = if (dirQueue.isNotEmpty()) dirQueue.last() else dir
        if ((last + 2) % 4 == d) return
        if (dirQueue.size < 3 && last != d) dirQueue.addLast(d)
    }

    fun step(dt: Float) {
        if (over) return
        acc += dt
        val interval = (0.24f - score * 0.0008f).coerceAtLeast(0.10f)
        while (acc >= interval) {
            acc -= interval
            if (dirQueue.isNotEmpty()) dir = dirQueue.removeFirst()
            val head = body.first()
            val hx = head % N
            val hy = head / N
            val dx = intArrayOf(0, 1, 0, -1)[dir]
            val dy = intArrayOf(-1, 0, 1, 0)[dir]
            val nx = hx + dx
            val ny = hy + dy
            if (nx !in 0 until N || ny !in 0 until N) {
                over = true
                return
            }
            val newHead = idx(nx, ny)
            val willEat = newHead == food
            val bodyToCheck = if (willEat) body else body.dropLast(1)
            if (newHead in bodyToCheck) {
                over = true
                return
            }
            body.addFirst(newHead)
            if (willEat) {
                score++
                placeFood()
            } else {
                body.removeLast()
            }
        }
    }

    private fun placeFood() {
        if (body.size >= N * N) return
        var candidate: Int
        do {
            candidate = Random.nextInt(N * N)
        } while (candidate in body)
        food = candidate
    }

    fun snapshot(): View = View(
        body = body.toList(),
        food = food,
        dir = dir,
        score = score,
        over = over,
    )
}
