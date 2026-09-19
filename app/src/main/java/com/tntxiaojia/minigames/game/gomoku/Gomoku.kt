package com.tntxiaojia.minigames.game.gomoku

import kotlin.random.Random

/** 五子棋逻辑引擎：15x15 棋盘 + 简单贪心人机。黑(1)先行。 */
class Gomoku {
    enum class Mode { HUMAN_AI, HUMAN_HUMAN }
    enum class Difficulty { EASY, NORMAL, HARD }

    data class View(
        val cells: IntArray,
        val turn: Int,
        val winner: Int,
        val over: Boolean,
        val mode: Mode,
        val difficulty: Difficulty,
    )

    companion object {
        const val N = 15
        private const val EMPTY = 0
        private const val BLACK = 1
        private const val WHITE = 2
        private const val HARD_DEPTH = 4
        private const val HARD_BRANCH = 8
        private const val WIN_SCORE = Int.MAX_VALUE / 2
        private const val INF = Int.MAX_VALUE / 4
        private val DIRS = arrayOf(
            0 to 1, 1 to 0, 1 to 1, 1 to -1,
        )
    }

    val cells = IntArray(N * N)
    private val history = ArrayList<Int>()
    var turn = BLACK
        private set
    var over = false
        private set
    var winner = 0
        private set
    var mode = Mode.HUMAN_AI
        private set
    var difficulty = Difficulty.NORMAL
        private set

    fun start(newMode: Mode, newDifficulty: Difficulty = difficulty) {
        mode = newMode
        difficulty = newDifficulty
        cells.fill(EMPTY)
        history.clear()
        turn = BLACK
        over = false
        winner = 0
    }

    fun setDifficulty(newDifficulty: Difficulty) {
        difficulty = newDifficulty
    }

    fun snapshot(): View = View(cells.copyOf(), turn, winner, over, mode, difficulty)

    /** 落子：仅当轮到可落子方时成功。 */
    fun place(x: Int, y: Int): Boolean {
        if (over || cells[y * N + x] != EMPTY) return false
        if (mode == Mode.HUMAN_AI && turn == WHITE) return false
        return doPlace(x, y, turn)
    }

    private fun doPlace(x: Int, y: Int, color: Int): Boolean {
        cells[y * N + x] = color
        history.add(y * N + x)
        if (checkWin(x, y, color)) {
            over = true
            winner = color
        } else if (cells.none { it == EMPTY }) {
            over = true
            winner = 0
        } else {
            turn = if (color == BLACK) WHITE else BLACK
        }
        return true
    }

    fun canUndo(): Boolean = history.isNotEmpty()

    /** 悔棋：人机模式下撤到轮到玩家（黑）为止。 */
    fun undo() {
        if (history.isEmpty()) return
        do {
            val idx = history.removeAt(history.size - 1)
            val color = cells[idx]
            cells[idx] = EMPTY
            turn = color
        } while (mode == Mode.HUMAN_AI && turn != BLACK && history.isNotEmpty())
        over = false
        winner = 0
    }

    /** 电脑走一步（只在 人机模式且轮到白棋时有效）。 */
    fun aiPlace() {
        if (mode != Mode.HUMAN_AI || turn != WHITE || over) return
        val empties = ArrayList<Int>(N * N)
        for (i in cells.indices) if (cells[i] == EMPTY) empties.add(i)
        if (empties.isEmpty()) return
        val pick = when (difficulty) {
            Difficulty.EASY -> easyPick(empties)
            Difficulty.NORMAL -> greedyPick(empties)
            Difficulty.HARD -> hardPick(empties)
        }
        doPlace(pick % N, pick / N, WHITE)
    }

    private fun greedyPick(empties: List<Int>): Int {
        var bestIdx = empties[Random.nextInt(empties.size)]
        var bestScore = -1
        for (i in empties) {
            val s = pointScore(i % N, i / N, WHITE) + pointScore(i % N, i / N, BLACK)
            if (s > bestScore) {
                bestScore = s
                bestIdx = i
            }
        }
        return bestIdx
    }

    /** 简单：多半随机，偶尔只看自己的进攻。 */
    private fun easyPick(empties: List<Int>): Int {
        if (Random.nextFloat() < 0.45f) return empties[Random.nextInt(empties.size)]
        var bestIdx = empties[Random.nextInt(empties.size)]
        var bestScore = -1
        for (i in empties) {
            val s = pointScore(i % N, i / N, WHITE)
            if (s > bestScore) {
                bestScore = s
                bestIdx = i
            }
        }
        return bestIdx
    }

    /** 困难：先赢/先堵，再做 4 层极大极小搜索（alpha-beta + 候选剪枝）。 */
    private fun hardPick(empties: List<Int>): Int {
        for (i in empties) if (pointScore(i % N, i / N, WHITE) >= 2_000_000) return i
        for (i in empties) if (pointScore(i % N, i / N, BLACK) >= 2_000_000) return i
        val cands = orderedCandidates()
        if (cands.isEmpty()) return empties[Random.nextInt(empties.size)]
        var bestIdx = cands[0]
        var bestValue = -INF
        for (i in cands) {
            cells[i] = WHITE
            val value = if (checkWin(i % N, i / N, WHITE)) {
                WIN_SCORE
            } else {
                search(HARD_DEPTH - 1, false, -INF, INF)
            }
            cells[i] = EMPTY
            if (value > bestValue) {
                bestValue = value
                bestIdx = i
            }
        }
        return bestIdx
    }

    /** 只考虑已有棋子两格内的空点，并按启发分排序，取前 HARD_BRANCH 个。 */
    private fun orderedCandidates(): List<Int> {
        val set = LinkedHashSet<Int>()
        for (y in 0 until N) {
            for (x in 0 until N) {
                if (cells[y * N + x] == EMPTY) continue
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until N && ny in 0 until N && cells[ny * N + nx] == EMPTY) {
                            set.add(ny * N + nx)
                        }
                    }
                }
            }
        }
        if (set.isEmpty()) return emptyList()
        return set
            .sortedByDescending { pointScore(it % N, it / N, WHITE) + pointScore(it % N, it / N, BLACK) }
            .take(HARD_BRANCH)
    }

    private fun search(depth: Int, maximizing: Boolean, alpha: Int, beta: Int): Int {
        if (depth <= 0) return evaluate()
        val cands = orderedCandidates()
        if (cands.isEmpty()) return evaluate()
        var a = alpha
        var b = beta
        if (maximizing) {
            for (i in cands) {
                cells[i] = WHITE
                val v = if (checkWin(i % N, i / N, WHITE)) {
                    WIN_SCORE - (HARD_DEPTH - depth)
                } else {
                    search(depth - 1, false, a, b)
                }
                cells[i] = EMPTY
                if (v > a) a = v
                if (a >= b) return a
            }
            return a
        }
        for (i in cands) {
            cells[i] = BLACK
            val v = if (checkWin(i % N, i / N, BLACK)) {
                -(WIN_SCORE - (HARD_DEPTH - depth))
            } else {
                search(depth - 1, true, a, b)
            }
            cells[i] = EMPTY
            if (v < b) b = v
            if (b <= a) return b
        }
        return b
    }

    private fun evaluate(): Int {
        val white = sideScore(WHITE).toLong()
        val black = sideScore(BLACK).toLong()
        return (white - black * 11L / 10L).toInt()
    }

    /** 某一方全盘棋型总分（每条同色序列只统计一次）。 */
    private fun sideScore(color: Int): Int {
        var total = 0
        for (y in 0 until N) {
            for (x in 0 until N) {
                if (cells[y * N + x] != color) continue
                for ((dx, dy) in DIRS) {
                    val px = x - dx
                    val py = y - dy
                    if (px in 0 until N && py in 0 until N && cells[py * N + px] == color) continue
                    var len = 1
                    var i = x + dx
                    var j = y + dy
                    while (i in 0 until N && j in 0 until N && cells[j * N + i] == color) {
                        len++
                        i += dx
                        j += dy
                    }
                    var open = 0
                    if (i in 0 until N && j in 0 until N && cells[j * N + i] == EMPTY) open++
                    if (px in 0 until N && py in 0 until N && cells[py * N + px] == EMPTY) open++
                    total += patternScore(len, open)
                }
            }
        }
        return total
    }

    private fun pointScore(x: Int, y: Int, color: Int): Int {
        var total = 0
        for ((dx, dy) in DIRS) {
            var len = 1
            var open = 0
            var i = x + dx
            var j = y + dy
            while (i in 0 until N && j in 0 until N && cells[j * N + i] == color) {
                len++
                i += dx
                j += dy
            }
            if (i in 0 until N && j in 0 until N && cells[j * N + i] == EMPTY) open++
            i = x - dx
            j = y - dy
            while (i in 0 until N && j in 0 until N && cells[j * N + i] == color) {
                len++
                i -= dx
                j -= dy
            }
            if (i in 0 until N && j in 0 until N && cells[j * N + i] == EMPTY) open++
            total += patternScore(len, open)
        }
        return total
    }

    private fun patternScore(len: Int, open: Int): Int = when {
        len >= 5 -> 2_000_000
        len == 4 -> if (open == 2) 200_000 else if (open == 1) 10_000 else 0
        len == 3 -> if (open == 2) 4_000 else if (open == 1) 500 else 0
        len == 2 -> if (open == 2) 300 else if (open == 1) 60 else 0
        else -> if (open > 0) 10 else 0
    }

    private fun checkWin(x: Int, y: Int, color: Int): Boolean {
        for ((dx, dy) in DIRS) {
            var count = 1
            var i = x + dx
            var j = y + dy
            while (i in 0 until N && j in 0 until N && cells[j * N + i] == color) {
                count++
                i += dx
                j += dy
            }
            i = x - dx
            j = y - dy
            while (i in 0 until N && j in 0 until N && cells[j * N + i] == color) {
                count++
                i -= dx
                j -= dy
            }
            if (count >= 5) return true
        }
        return false
    }
}
