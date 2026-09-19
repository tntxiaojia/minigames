package com.tntxiaojia.minigames.game.tetris

import kotlin.math.max
import kotlin.random.Random

/** 俄罗斯方块引擎：10x16 网格。消除行时先进入短暂动画阶段再真正移除。 */
class Tetris {
    data class View(
        val board: IntArray,
        val piece: List<Pair<Int, Int>>,
        val pieceColor: Int,
        val next: List<Pair<Int, Int>>,
        val nextColor: Int,
        val score: Int,
        val lines: Int,
        val over: Boolean,
        val clearingRows: List<Int>,
        val clearProgress: Float,
    )

    companion object {
        const val COLS = 10
        const val ROWS = 16
        private const val CLEAR_DURATION = 0.4f
        private val SHAPES = listOf(
            listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1), // I
            listOf(1 to 0, 2 to 0, 1 to 1, 2 to 1), // O
            listOf(0 to 1, 1 to 0, 1 to 1, 2 to 1), // T
            listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1), // S
            listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1), // Z
            listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1), // J
            listOf(2 to 0, 0 to 1, 1 to 1, 2 to 1), // L
        )
    }

    private val board = IntArray(COLS * ROWS)
    private var current: List<Pair<Int, Int>> = SHAPES[Random.nextInt(SHAPES.size)]
    private var currentIdx = 0
    private var px = 3
    private var py = 0
    private var next = SHAPES[Random.nextInt(SHAPES.size)]
    private var nextIdx = 0
    private var acc = 0f
    private var clearingRows: List<Int> = emptyList()
    private var clearTimer = 0f
    private var clearProgress = 0f
    var score = 0
        private set
    private var clearedLines = 0
    var over = false
        private set

    fun newGame() {
        board.fill(0)
        score = 0
        clearedLines = 0
        over = false
        acc = 0f
        clearingRows = emptyList()
        clearTimer = 0f
        clearProgress = 0f
        spawn(forceNew = true)
    }

    private fun randomShape(): Int = Random.nextInt(SHAPES.size)

    private fun spawn(forceNew: Boolean = false) {
        if (forceNew) {
            currentIdx = randomShape()
            current = SHAPES[currentIdx]
            nextIdx = randomShape()
            next = SHAPES[nextIdx]
        } else {
            currentIdx = nextIdx
            current = next
            nextIdx = randomShape()
            next = SHAPES[nextIdx]
        }
        px = 3
        py = 0
        if (!fits(current, px, py)) over = true
    }

    private fun fits(cells: List<Pair<Int, Int>>, ox: Int, oy: Int): Boolean {
        for ((x, y) in cells) {
            val gx = ox + x
            val gy = oy + y
            if (gy < 0) continue
            if (gx !in 0 until COLS || gy >= ROWS) return false
            if (board[gy * COLS + gx] != 0) return false
        }
        return true
    }

    private fun rotateCells(cells: List<Pair<Int, Int>>): List<Pair<Int, Int>> =
        cells.map { (x, y) -> y to (3 - x) }

    private fun level(): Int = (clearedLines / 4).coerceAtMost(10)

    private fun interval(): Float = max(0.08f, 0.9f - level() * 0.06f)

    private fun isClearing(): Boolean = clearingRows.isNotEmpty()

    fun step(dt: Float) {
        if (over) return
        if (isClearing()) {
            clearTimer -= dt
            clearProgress = (1f - clearTimer / CLEAR_DURATION).coerceIn(0f, 1f)
            if (clearTimer <= 0f) applyClear()
            return
        }
        acc += dt
        while (acc >= interval()) {
            acc -= interval()
            if (!tryMove(0, 1)) {
                finishPiece()
                break
            }
        }
    }

    private fun tryMove(dx: Int, dy: Int): Boolean {
        if (fits(current, px + dx, py + dy)) {
            px += dx
            py += dy
            return true
        }
        return false
    }

    fun moveLeft(): Boolean {
        if (over || isClearing()) return false
        return tryMove(-1, 0)
    }

    fun moveRight(): Boolean {
        if (over || isClearing()) return false
        return tryMove(1, 0)
    }

    fun rotate() {
        if (over || isClearing() || currentIdx == 1) return
        val rotated = rotateCells(current)
        for (kick in intArrayOf(0, -1, 1, -2, 2)) {
            if (fits(rotated, px + kick, py)) {
                px += kick
                current = rotated
                return
            }
        }
    }

    fun softDrop() {
        if (over || isClearing()) return
        tryMove(0, 1)
    }

    fun hardDrop() {
        if (over || isClearing()) return
        while (tryMove(0, 1)) {
            // 一直落到底
        }
        finishPiece()
    }

    private fun finishPiece() {
        for ((x, y) in current) {
            val gy = py + y
            if (gy in 0 until ROWS) {
                board[gy * COLS + px + x] = currentIdx + 1
            }
        }
        current = emptyList()
        val fullRows = ArrayList<Int>()
        for (y in 0 until ROWS) {
            var full = true
            for (x in 0 until COLS) {
                if (board[y * COLS + x] == 0) {
                    full = false
                    break
                }
            }
            if (full) fullRows.add(y)
        }
        if (fullRows.isNotEmpty()) {
            clearingRows = fullRows
            clearTimer = CLEAR_DURATION
            clearProgress = 0f
            return
        }
        spawn()
    }

    private fun applyClear() {
        val rows = clearingRows
        val newBoard = IntArray(COLS * ROWS)
        var src = ROWS - 1
        var dst = ROWS - 1
        while (src >= 0) {
            if (src in rows) {
                src--
                continue
            }
            for (x in 0 until COLS) {
                newBoard[dst * COLS + x] = board[src * COLS + x]
            }
            src--
            dst--
        }
        board.fill(0)
        for (i in newBoard.indices) board[i] = newBoard[i]
        clearedLines += rows.size
        score += 10 * rows.size
        clearingRows = emptyList()
        clearTimer = 0f
        clearProgress = 0f
        spawn()
    }

    fun snapshot(): View {
        val piece = current.map { (x, y) -> (px + x) to (py + y) }.filter { (_, gy) -> gy >= 0 }
        val nextCells = next.toList()
        val minX = nextCells.minOf { it.first }
        val minY = nextCells.minOf { it.second }
        val normalized = nextCells.map { (x, y) -> (x - minX) to (y - minY) }
        return View(
            board = board.copyOf(),
            piece = piece,
            pieceColor = currentIdx + 1,
            next = normalized,
            nextColor = nextIdx + 1,
            score = score,
            lines = clearedLines,
            over = over,
            clearingRows = clearingRows,
            clearProgress = clearProgress,
        )
    }
}
