package com.tntxiaojia.minigames.game.mines

import kotlin.random.Random

/** 扫雷逻辑引擎：9x9、10 雷，首击安全。 */
class Mines {
    data class View(
        val grid: IntArray,
        val opened: BooleanArray,
        val flagged: BooleanArray,
        val over: Boolean,
        val won: Boolean,
        val mineCount: Int,
    )

    companion object {
        const val COLS = 9
        const val ROWS = 9
        const val MINES = 10
        const val MINE = 9
    }

    val grid = IntArray(ROWS * COLS)
    val opened = BooleanArray(ROWS * COLS)
    val flagged = BooleanArray(ROWS * COLS)
    var over = false
        private set
    var won = false
        private set
    private var firstClick = true

    fun reset() {
        grid.fill(0)
        opened.fill(false)
        flagged.fill(false)
        over = false
        won = false
        firstClick = true
    }

    fun flagCount(): Int {
        var c = 0
        for (f in flagged) if (f) c++
        return c
    }

    fun tap(x: Int, y: Int) {
        val idx = y * COLS + x
        if (over || won || flagged[idx] || opened[idx]) return
        if (firstClick) {
            firstClick = false
            placeMines(idx)
        }
        if (grid[idx] == MINE) {
            over = true
            for (i in grid.indices) if (grid[i] == MINE) opened[i] = true
            return
        }
        reveal(x, y)
        checkWin()
    }

    fun toggleFlag(x: Int, y: Int) {
        if (over || won) return
        val idx = y * COLS + x
        if (opened[idx]) return
        flagged[idx] = !flagged[idx]
    }

    private fun placeMines(safeIdx: Int) {
        var placed = 0
        while (placed < MINES) {
            val i = Random.nextInt(ROWS * COLS)
            if (i != safeIdx && grid[i] != MINE) {
                grid[i] = MINE
                placed++
            }
        }
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val idx = y * COLS + x
                if (grid[idx] != MINE) {
                    grid[idx] = mineAround(x, y)
                }
            }
        }
    }

    private fun mineAround(x: Int, y: Int): Int {
        var c = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until COLS && ny in 0 until ROWS && grid[ny * COLS + nx] == MINE) c++
            }
        }
        return c
    }

    private fun reveal(x: Int, y: Int) {
        if (x !in 0 until COLS || y !in 0 until ROWS) return
        val idx = y * COLS + x
        if (opened[idx] || flagged[idx] || grid[idx] == MINE) return
        opened[idx] = true
        if (grid[idx] == 0) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    reveal(x + dx, y + dy)
                }
            }
        }
    }

    private fun checkWin() {
        var openedCount = 0
        for (i in opened) if (i) openedCount++
        if (openedCount >= ROWS * COLS - MINES) {
            won = true
            for (i in grid.indices) if (grid[i] == MINE) flagged[i] = true
        }
    }

    fun snapshot(): View = View(grid.copyOf(), opened.copyOf(), flagged.copyOf(), over, won, MINES)
}
