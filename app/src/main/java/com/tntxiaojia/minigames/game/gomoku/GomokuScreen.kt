package com.tntxiaojia.minigames.game.gomoku

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.game.gomoku.Gomoku.Companion.N
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import kotlin.math.roundToInt

private val BOARD_COLOR = Color(0xFFC19054)
private val LINE_COLOR = Color(0xFF4E3B23)

@Composable
fun GomokuScreen(onExit: () -> Unit) {
    val engine = remember { Gomoku().apply { start(Gomoku.Mode.HUMAN_AI) } }
    val zoom = rememberZoomState()
    var v by remember { mutableStateOf(engine.snapshot()) }
    var recorded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gomoku_stats", Context.MODE_PRIVATE) }
    var wins by remember { mutableStateOf(prefs.getInt("wins", 0)) }
    var losses by remember { mutableStateOf(prefs.getInt("losses", 0)) }

    fun restart(mode: Gomoku.Mode, difficulty: Gomoku.Difficulty) {
        engine.start(mode, difficulty)
        v = engine.snapshot()
        recorded = false
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val newMode = data.getIntExtra(GomokuSettingsActivity.EXTRA_MODE, v.mode.ordinal)
        val newDifficulty = data.getIntExtra(GomokuSettingsActivity.EXTRA_DIFFICULTY, v.difficulty.ordinal)
        val action = data.getIntExtra(GomokuSettingsActivity.EXTRA_ACTION, GomokuSettingsActivity.ACTION_NONE)
        when (action) {
            GomokuSettingsActivity.ACTION_UNDO -> {
                engine.undo()
                v = engine.snapshot()
            }
            GomokuSettingsActivity.ACTION_RESTART -> restart(
                Gomoku.Mode.values()[newMode],
                Gomoku.Difficulty.values()[newDifficulty]
            )
            GomokuSettingsActivity.ACTION_EXIT -> onExit()
            else -> {
                if (newMode != v.mode.ordinal) {
                    restart(Gomoku.Mode.values()[newMode], Gomoku.Difficulty.values()[newDifficulty])
                } else if (newDifficulty != v.difficulty.ordinal) {
                    engine.setDifficulty(Gomoku.Difficulty.values()[newDifficulty])
                    v = engine.snapshot()
                }
            }
        }
    }

    LaunchedEffect(v.turn, v.over, v.mode) {
        if (!v.over && v.mode == Gomoku.Mode.HUMAN_AI && v.turn == 2) {
            kotlinx.coroutines.delay(360)
            if (!engine.over && engine.turn == 2) {
                engine.aiPlace()
                v = engine.snapshot()
            }
        }
    }

    LaunchedEffect(v.over) {
        if (v.over && !recorded && v.mode == Gomoku.Mode.HUMAN_AI && v.winner != 0) {
            recorded = true
            when (v.winner) {
                1 -> {
                    wins += 1
                    prefs.edit().putInt("wins", wins).apply()
                }
                2 -> {
                    losses += 1
                    prefs.edit().putInt("losses", losses).apply()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(SurfaceDark)) {
        BoxWithConstraints(Modifier.fillMaxSize().pinchZoom(zoom)) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side).align(Alignment.Center)) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(engine) {
                            detectTapGestures { pos ->
                                if (zoom.zooming) return@detectTapGestures
                                val m = size.width * 0.05f
                                val cell = (size.width - m * 2) / (N - 1).toFloat()
                                val cx = ((pos.x - m) / cell).roundToInt().coerceIn(0, N - 1)
                                val cy = ((pos.y - m) / cell).roundToInt().coerceIn(0, N - 1)
                                if (engine.place(cx, cy)) v = engine.snapshot()
                            }
                        }
                ) {
                    drawBoard(v)
                }
            }
        }

        // 设置入口：屏幕最右上角
        val debug = v.difficulty == Gomoku.Difficulty.DEBUG
        TextButton(
            onClick = {
                val intent = Intent(context, GomokuSettingsActivity::class.java)
                    .putExtra(GomokuSettingsActivity.EXTRA_MODE, v.mode.ordinal)
                    .putExtra(GomokuSettingsActivity.EXTRA_DIFFICULTY, v.difficulty.ordinal)
                    .putExtra(GomokuSettingsActivity.EXTRA_WINS, wins)
                    .putExtra(GomokuSettingsActivity.EXTRA_LOSSES, losses)
                settingsLauncher.launch(intent)
            },
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(30.dp)
        ) {
            Text(
                if (debug) "debug" else "设置",
                color = if (debug) Color.Red else Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        EndOverlay(
            visible = v.over,
            headline = when {
                v.winner == 1 -> "黑方获胜"
                v.winner == 2 -> "白方获胜"
                else -> "平局"
            },
            onAction = { restart(v.mode, v.difficulty) },
            onExit = onExit
        )
    }
}

private fun DrawScope.drawBoard(view: Gomoku.View) {
    val side = size.width
    val m = side * 0.05f
    val cell = (side - m * 2) / (N - 1).toFloat()
    drawRoundRect(BOARD_COLOR, Offset.Zero, Size(side, side), CornerRadius(side * 0.02f))
    for (i in 0 until N) {
        val p = m + i * cell
        drawLine(LINE_COLOR, Offset(p, m), Offset(p, side - m), side * 0.006f)
        drawLine(LINE_COLOR, Offset(m, p), Offset(side - m, p), side * 0.006f)
    }
    val stars = listOf(3 to 3, 3 to 11, 7 to 7, 11 to 3, 11 to 11)
    for ((sx, sy) in stars) {
        drawCircle(LINE_COLOR, radius = side * 0.012f, center = Offset(m + sx * cell, m + sy * cell))
    }
    for (y in 0 until N) {
        for (x in 0 until N) {
            val c = view.cells[y * N + x]
            if (c == 0) continue
            val center = Offset(m + x * cell, m + y * cell)
            val r = cell * 0.43f
            when (c) {
                1 -> {
                    drawCircle(Color(0xFF17181A), radius = r, center = center)
                    drawCircle(Color.White.copy(alpha = 0.08f), radius = r * 0.55f, center = Offset(center.x - r * 0.25f, center.y - r * 0.25f))
                }
                else -> {
                    drawCircle(Color.White, radius = r, center = center)
                    drawCircle(Color(0xFFBDBDBD), radius = r, center = center, style = Stroke(side * 0.006f))
                }
            }
        }
    }
}
