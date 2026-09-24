package com.tntxiaojia.minigames.game.gomoku

import kotlin.random.Random

/**
 * 五子棋逻辑引擎：15x15 棋盘，黑(1)先行。
 * 难度：EASY/NORMAL 为启发式贪心；HARD 为 4 层 αβ；DEBUG（隐藏）为组合棋型 + VCF/VCT 算杀 + 带时间预算的深层 αβ。
 */
class Gomoku {
    enum class Mode { HUMAN_AI, HUMAN_HUMAN }
    enum class Difficulty { EASY, NORMAL, HARD, DEBUG }

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
        private const val DEBUG_DEPTH = 6
        private const val DEBUG_BRANCH = 12
        private const val DEBUG_TIME_MS = 500L
        private const val VCF_DEPTH = 12

        private const val WIN_SCORE = Int.MAX_VALUE / 2
        private const val INF = Int.MAX_VALUE / 4

        private const val LV_NONE = 0
        private const val LV_ONE = 1
        private const val LV_TWO = 2
        private const val LV_OPEN_TWO = 3
        private const val LV_SLEEP_THREE = 4
        private const val LV_OPEN_THREE = 5
        private const val LV_FOUR = 6
        private const val LV_OPEN_FOUR = 7
        private const val LV_FIVE = 8

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

    private var searchDeadline = Long.MAX_VALUE

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
            Difficulty.DEBUG -> debugPick(empties)
        }
        doPlace(pick % N, pick / N, WHITE)
    }

    // ---------------- 低难度 ----------------

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

    // ---------------- 困难（4 层 αβ） ----------------

    private fun hardPick(empties: List<Int>): Int {
        searchDeadline = Long.MAX_VALUE
        for (i in empties) if (pointScore(i % N, i / N, WHITE) >= 2_000_000) return i
        for (i in empties) if (pointScore(i % N, i / N, BLACK) >= 2_000_000) return i
        val cands = orderedCandidates(HARD_BRANCH)
        if (cands.isEmpty()) return empties[Random.nextInt(empties.size)]
        var bestIdx = cands[0]
        var bestValue = -INF
        for (i in cands) {
            cells[i] = WHITE
            val value = if (checkWin(i % N, i / N, WHITE)) {
                WIN_SCORE
            } else {
                search(HARD_DEPTH - 1, false, -INF, INF, HARD_BRANCH)
            }
            cells[i] = EMPTY
            if (value > bestValue) {
                bestValue = value
                bestIdx = i
            }
        }
        return bestIdx
    }

    private fun search(depth: Int, maximizing: Boolean, alpha: Int, beta: Int, limit: Int): Int {
        if (depth <= 0 || System.nanoTime() > searchDeadline) return evaluate()
        val cands = orderedCandidates(limit)
        if (cands.isEmpty()) return evaluate()
        var a = alpha
        var b = beta
        if (maximizing) {
            for (i in cands) {
                cells[i] = WHITE
                val v = if (checkWin(i % N, i / N, WHITE)) {
                    WIN_SCORE - (DEBUG_DEPTH - depth)
                } else {
                    search(depth - 1, false, a, b, limit)
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
                -(WIN_SCORE - (DEBUG_DEPTH - depth))
            } else {
                search(depth - 1, true, a, b, limit)
            }
            cells[i] = EMPTY
            if (v < b) b = v
            if (b <= a) return b
        }
        return b
    }

    // ---------------- 隐藏难度（组合棋型 + VCF/VCT + 深层搜索） ----------------

    private fun debugPick(empties: List<Int>): Int {
        searchDeadline = System.nanoTime() + DEBUG_TIME_MS * 1_000_000

        val myWin = winningPoints(WHITE)
        if (myWin.isNotEmpty()) return myWin[0]
        val oppWin = winningPoints(BLACK)
        if (oppWin.isNotEmpty()) return oppWin[0]

        val vcf = vcfMove(WHITE, VCF_DEPTH)
        if (vcf >= 0) return vcf

        val vct = vctMove(WHITE, 2)
        if (vct >= 0) return vct

        val block = blockOpponentShape()
        if (block >= 0) return block

        val cands = orderedCandidates(DEBUG_BRANCH)
        if (cands.isEmpty()) return empties[Random.nextInt(empties.size)]
        var bestIdx = cands[0]
        var bestValue = -INF
        for (i in cands) {
            cells[i] = WHITE
            val value = if (checkWin(i % N, i / N, WHITE)) {
                WIN_SCORE
            } else {
                search(DEBUG_DEPTH - 1, false, -INF, INF, DEBUG_BRANCH)
            }
            cells[i] = EMPTY
            if (value > bestValue) {
                bestValue = value
                bestIdx = i
            }
            if (System.nanoTime() > searchDeadline) break
        }
        return bestIdx
    }

    private fun other(color: Int) = if (color == BLACK) WHITE else BLACK

    /** 某方向的棋型等级（把 (x,y) 当作刚落下的子）。 */
    private fun dirLevel(x: Int, y: Int, dx: Int, dy: Int, color: Int): Int {
        var len = 1
        var i = x + dx
        var j = y + dy
        while (i in 0 until N && j in 0 until N && cells[j * N + i] == color) {
            len++
            i += dx
            j += dy
        }
        val open1 = i in 0 until N && j in 0 until N && cells[j * N + i] == EMPTY
        i = x - dx
        j = y - dy
        while (i in 0 until N && j in 0 until N && cells[j * N + i] == color) {
            len++
            i -= dx
            j -= dy
        }
        val open2 = i in 0 until N && j in 0 until N && cells[j * N + i] == EMPTY
        val open = (if (open1) 1 else 0) + (if (open2) 1 else 0)
        return when {
            len >= 5 -> LV_FIVE
            len == 4 -> if (open >= 2) LV_OPEN_FOUR else if (open == 1) LV_FOUR else LV_NONE
            len == 3 -> if (open >= 2) LV_OPEN_THREE else if (open == 1) LV_SLEEP_THREE else LV_NONE
            len == 2 -> if (open >= 2) LV_OPEN_TWO else if (open == 1) LV_TWO else LV_NONE
            else -> if (open >= 2) LV_ONE else LV_NONE
        }
    }

    /** 临时在 i 落子后，四个方向的等级。 */
    private fun shapeAfter(i: Int, color: Int): IntArray {
        cells[i] = color
        val x = i % N
        val y = i / N
        val levels = IntArray(4)
        for (d in DIRS.indices) {
            val dir = DIRS[d]
            levels[d] = dirLevel(x, y, dir.first, dir.second, color)
        }
        cells[i] = EMPTY
        return levels
    }

    private fun maxLevel(levels: IntArray): Int {
        var m = 0
        for (l in levels) if (l > m) m = l
        return m
    }

    private fun countLevel(levels: IntArray, level: Int): Int {
        var c = 0
        for (l in levels) if (l == level) c++
        return c
    }

    /** 落子后是否形成"必胜形状"：五 / 活四 / 双四 / 四三 / 双活三。 */
    private fun isWinShape(levels: IntArray): Boolean {
        if (maxLevel(levels) >= LV_FIVE) return true
        if (countLevel(levels, LV_OPEN_FOUR) >= 1) return true
        val fours = countLevel(levels, LV_FOUR)
        val openThrees = countLevel(levels, LV_OPEN_THREE)
        if (fours >= 2) return true
        if (fours >= 1 && openThrees >= 1) return true
        if (openThrees >= 2) return true
        return false
    }

    private fun winningPoints(color: Int): List<Int> {
        val out = ArrayList<Int>()
        for (i in cells.indices) {
            if (cells[i] != EMPTY) continue
            if (pointScore(i % N, i / N, color) >= 2_000_000) out.add(i)
        }
        return out
    }

    /** 落子后能形成四（冲四/活四）的点。 */
    private fun fourThreats(color: Int, limit: Int = 24): List<Int> {
        val out = ArrayList<Int>()
        for (i in cells.indices) {
            if (cells[i] != EMPTY) continue
            val levels = shapeAfter(i, color)
            if (countLevel(levels, LV_FOUR) + countLevel(levels, LV_OPEN_FOUR) >= 1) out.add(i)
            if (out.size >= limit) break
        }
        return out
    }

    /** 候选威胁手（活三及以上），带启发排序。 */
    private fun threatCandidates(color: Int, limit: Int = DEBUG_BRANCH * 2): List<Int> {
        val out = ArrayList<Int>()
        for (i in cells.indices) {
            if (cells[i] != EMPTY) continue
            if (pointScore(i % N, i / N, color) >= 500) out.add(i)
        }
        return out.sortedByDescending { pointScore(it % N, it / N, color) }.take(limit)
    }

    /** 连续冲四算杀：返回制胜首着，-1 表示没找到。 */
    private fun vcfMove(color: Int, depth: Int): Int {
        val wins = winningPoints(color)
        if (wins.isNotEmpty()) return wins.first()
        if (depth <= 0 || System.nanoTime() > searchDeadline) return -1
        for (i in fourThreats(color)) {
            cells[i] = color
            val myWins = winningPoints(color)
            var ok = false
            if (myWins.size >= 2) {
                ok = true
            } else if (myWins.size == 1 && winningPoints(other(color)).isEmpty()) {
                val block = myWins[0]
                cells[block] = other(color)
                ok = vcfMove(color, depth - 1) >= 0
                cells[block] = EMPTY
            }
            cells[i] = EMPTY
            if (ok) return i
        }
        return -1
    }

    /** 有限威胁算杀（含活四/双活三/四三），返回制胜首着，-1 表示没找到。 */
    private fun vctMove(color: Int, depth: Int): Int {
        if (System.nanoTime() > searchDeadline) return -1
        val wins = winningPoints(color)
        if (wins.isNotEmpty()) return wins.first()
        val opp = other(color)
        for (i in threatCandidates(color)) {
            val levels = shapeAfter(i, color)
            if (!isWinShape(levels)) continue
            cells[i] = color
            val safe = winningPoints(opp).isEmpty() && fourThreats(opp, 4).isEmpty()
            cells[i] = EMPTY
            if (safe) return i
        }
        if (depth <= 1) return -1
        for (i in threatCandidates(color)) {
            if (maxLevel(shapeAfter(i, color)) < LV_OPEN_THREE) continue
            cells[i] = color
            val follow = winShapePoint(color)
            val oppSafe = winningPoints(opp).isEmpty()
            cells[i] = EMPTY
            if (follow >= 0 && oppSafe) return i
        }
        return -1
    }

    private fun winShapePoint(color: Int): Int {
        for (i in threatCandidates(color)) {
            if (isWinShape(shapeAfter(i, color))) return i
        }
        return -1
    }

    /** 对手一手能形成必胜形状时，先堵该点。 */
    private fun blockOpponentShape(): Int {
        val opp = BLACK
        var fallback = -1
        var fallbackLevel = 0
        for (i in cells.indices) {
            if (cells[i] != EMPTY) continue
            if (pointScore(i % N, i / N, opp) < 500) continue
            val levels = shapeAfter(i, opp)
            if (isWinShape(levels)) return i
            val lv = maxLevel(levels)
            if (lv > fallbackLevel) {
                fallbackLevel = lv
                fallback = i
            }
        }
        return fallback
    }

    // ---------------- 通用：候选 / 评估 ----------------

    /** 只考虑已有棋子两格内的空点，按启发分排序取前 limit 个。 */
    private fun orderedCandidates(limit: Int): List<Int> {
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
            .take(limit)
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
