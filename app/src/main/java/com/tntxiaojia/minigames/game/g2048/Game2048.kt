package com.tntxiaojia.minigames.game.g2048

import kotlin.random.Random

/** 2048 引擎：带 id 的方块模型，支持 4x4 / 5x5 / 6x6；输出滑动/合并所需的动画数据。 */
class Game2048 {
    enum class Dir { LEFT, RIGHT, UP, DOWN }

    data class TileView(val id: Int, val value: Int, val row: Int, val col: Int)

    data class Ghost(
        val value: Int,
        val fromRow: Int,
        val fromCol: Int,
        val toRow: Int,
        val toCol: Int,
    )

    data class View(
        val size: Int,
        val tiles: List<TileView>,
        val ghosts: List<Ghost>,
        val spawnId: Int?,
        val mergedIds: Set<Int>,
        val score: Int,
        val won: Boolean,
        val over: Boolean,
    )

    private class Tile(val id: Int, var value: Int, var row: Int, var col: Int) {
        var removed = false
    }

    private val tiles = ArrayList<Tile>()
    private var nextId = 1
    private var lastGhosts: List<Ghost> = emptyList()
    private var lastSpawnId: Int? = null
    private var lastMergedIds: Set<Int> = emptySet()
    private var announced = false

    var size = 4
        private set
    var score = 0
        private set
    var over = false
        private set
    var won = false
        private set

    /** 重开；可指定棋盘边长（4~6）。 */
    fun reset(boardSize: Int = size) {
        size = boardSize.coerceIn(4, 6)
        tiles.clear()
        nextId = 1
        score = 0
        over = false
        won = false
        announced = false
        clearAnim()
        spawnTile()
        spawnTile()
    }

    fun continueAfterWin() {
        won = false
    }

    private fun clearAnim() {
        lastGhosts = emptyList()
        lastSpawnId = null
        lastMergedIds = emptySet()
    }

    private fun tileAt(row: Int, col: Int): Tile? =
        tiles.firstOrNull { !it.removed && it.row == row && it.col == col }

    private fun spawnTile(): Int? {
        val free = ArrayList<Int>(size * size)
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (tileAt(r, c) == null) free.add(r * size + c)
            }
        }
        if (free.isEmpty()) return null
        val idx = free[Random.nextInt(free.size)]
        val tile = Tile(nextId++, if (Random.nextFloat() < 0.9f) 2 else 4, idx / size, idx % size)
        tiles.add(tile)
        return tile.id
    }

    /** 尝试向某方向滑动；返回棋盘是否发生变化。 */
    fun move(dir: Dir): Boolean {
        if (over) return false
        val from = HashMap<Int, Int>()
        for (t in tiles) from[t.id] = t.row * size + t.col

        val dr = when (dir) {
            Dir.UP -> -1
            Dir.DOWN -> 1
            else -> 0
        }
        val dc = when (dir) {
            Dir.LEFT -> -1
            Dir.RIGHT -> 1
            else -> 0
        }

        val ghosts = ArrayList<Ghost>()
        val mergedInto = HashSet<Int>()
        val mergedIds = HashSet<Int>()
        val ordered = tiles.sortedWith(compareBy { -(it.row * dr + it.col * dc) })
        var moved = false

        for (t in ordered) {
            if (t.removed) continue
            while (true) {
                val nr = t.row + dr
                val nc = t.col + dc
                if (nr !in 0 until size || nc !in 0 until size) break
                val occ = tileAt(nr, nc)
                if (occ == null) {
                    t.row = nr
                    t.col = nc
                    continue
                }
                if (occ.value == t.value && occ.id !in mergedInto) {
                    mergedInto.add(occ.id)
                    mergedIds.add(occ.id)
                    val start = from[t.id] ?: 0
                    ghosts.add(Ghost(t.value, start / size, start % size, occ.row, occ.col))
                    occ.value *= 2
                    score += occ.value
                    t.removed = true
                    moved = true
                    if (occ.value >= 2048 && !announced) {
                        announced = true
                        won = true
                    }
                }
                break
            }
            if (!t.removed && from[t.id] != t.row * size + t.col) moved = true
        }

        if (!moved) {
            clearAnim()
            return false
        }

        tiles.removeAll { it.removed }
        val spawned = spawnTile()
        lastGhosts = ghosts
        lastSpawnId = spawned
        lastMergedIds = mergedIds
        if (!canMove()) over = true
        return true
    }

    private fun canMove(): Boolean {
        for (r in 0 until size) {
            for (c in 0 until size) {
                val t = tileAt(r, c) ?: return true
                if (c + 1 < size && tileAt(r, c + 1)?.value == t.value) return true
                if (r + 1 < size && tileAt(r + 1, c)?.value == t.value) return true
            }
        }
        return false
    }

    fun snapshot(): View = View(
        size = size,
        tiles = tiles.map { TileView(it.id, it.value, it.row, it.col) },
        ghosts = lastGhosts,
        spawnId = lastSpawnId,
        mergedIds = lastMergedIds,
        score = score,
        won = won,
        over = over,
    )
}
