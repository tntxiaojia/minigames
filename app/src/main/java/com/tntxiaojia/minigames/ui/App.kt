package com.tntxiaojia.minigames.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tntxiaojia.minigames.game.flappy.FlappyScreen
import com.tntxiaojia.minigames.game.floors.FloorsScreen
import com.tntxiaojia.minigames.game.g2048.Game2048Screen
import com.tntxiaojia.minigames.game.gomoku.GomokuScreen
import com.tntxiaojia.minigames.game.jump.JumpScreen
import com.tntxiaojia.minigames.game.mines.MinesScreen
import com.tntxiaojia.minigames.game.plane.PlaneScreen
import com.tntxiaojia.minigames.game.snake.SnakeScreen
import com.tntxiaojia.minigames.game.tetris.TetrisScreen

enum class GameId(val title: String) {
    G2048("2048"),
    GOMOKU("五子棋"),
    PLANE("飞机大战"),
    JUMP("跳一跳"),
    FLAPPY("像素鸟"),
    TETRIS("俄罗斯方块"),
    SNAKE("贪吃蛇"),
    MINES("扫雷"),
    FLOORS("勇下100层"),
}

@Composable
fun App() {
    var gameName by rememberSaveable { mutableStateOf<String?>(null) }
    val game = gameName?.let { runCatching { GameId.valueOf(it) }.getOrNull() }

    if (game != null) {
        BackHandler { gameName = null }
        val exit = { gameName = null }
        when (game) {
            GameId.G2048 -> Game2048Screen(onExit = exit)
            GameId.GOMOKU -> GomokuScreen(onExit = exit)
            GameId.PLANE -> PlaneScreen(onExit = exit)
            GameId.JUMP -> JumpScreen(onExit = exit)
            GameId.FLAPPY -> FlappyScreen(onExit = exit)
            GameId.TETRIS -> TetrisScreen(onExit = exit)
            GameId.SNAKE -> SnakeScreen(onExit = exit)
            GameId.MINES -> MinesScreen(onExit = exit)
            GameId.FLOORS -> FloorsScreen(onExit = exit)
        }
    } else {
        MainScreen(onPick = { gameName = it.name })
    }
}
