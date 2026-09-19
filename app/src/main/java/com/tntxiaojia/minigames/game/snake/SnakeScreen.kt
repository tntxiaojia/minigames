package com.tntxiaojia.minigames.game.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.game.snake.Snake.Companion.N
import com.tntxiaojia.minigames.game.snake.Snake.View
import com.tntxiaojia.minigames.ui.components.CornerHud
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.util.Records
import kotlin.math.abs

private const val DT = 1f / 60f

@Composable
fun SnakeScreen(onExit: () -> Unit) {
    val engine = remember { Snake().apply { newGame() } }
    val zoom = rememberZoomState()
    var v by remember { mutableStateOf(engine.snapshot()) }
    val context = LocalContext.current
    var best by remember { mutableStateOf(Records.best(context, "snake")) }

    LaunchedEffect(v.over) {
        if (v.over) best = Records.submit(context, "snake", v.score)
    }

    LaunchedEffect(engine) {
        while (true) {
            if (!engine.over) {
                engine.step(DT)
                v = engine.snapshot()
                kotlinx.coroutines.delay(16)
            } else {
                kotlinx.coroutines.delay(120)
            }
        }
    }

    fun restart() {
        engine.newGame()
        v = engine.snapshot()
    }

    Box(Modifier.fillMaxSize().background(SurfaceDark)) {
        BoxWithConstraints(Modifier.fillMaxSize().pinchZoom(zoom)) {
            val side = minOf(maxWidth, maxHeight)
            Box(Modifier.size(side).align(Alignment.Center)) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(engine) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                if (zoom.zooming) return@awaitEachGesture
                                var acc = Offset.Zero
                                var acted = false
                                val slop = size.width * 0.03f
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!ch.pressed) {
                                        break
                                    }
                                    if (ch.position != ch.previousPosition) {
                                        acc += ch.position - ch.previousPosition
                                        if (!acted && acc.getDistance() >= slop) {
                                            acted = true
                                            val d = when {
                                                abs(acc.x) > abs(acc.y) ->
                                                    if (acc.x > 0) 1 else 3
                                                acc.y > 0 -> 2
                                                else -> 0
                                            }
                                            engine.requestDir(d)
                                            v = engine.snapshot()
                                        }
                                    }
                                    ch.consume()
                                }
                            }
                        }
                ) {
                    drawSnake(v)
                }
            }
        }
        CornerHud(zoom) {
            Text("${v.score}", color = SecondaryAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        EndOverlay(
            visible = v.over,
            headline = "撞上了",
            message = "吃到 ${v.score} 个 · 最高 $best",
            onAction = { restart() },
            onExit = onExit
        )
    }
}

private fun DrawScope.drawSnake(view: View) {
    val side = size.width
    val cell = side / N
    drawRoundRect(Color(0xFF171C25), Offset.Zero, Size(side, side), CornerRadius(cell * 0.2f))

    val head = view.body.firstOrNull() ?: return
    val inset = cell * 0.08f
    for ((i, idx) in view.body.withIndex()) {
        val x = (idx % N) * cell
        val y = (idx / N) * cell
        val color = if (i == 0) Color(0xFFB2FF59) else Color(0xFF4CAF50).copy(alpha = 1f - 0.35f * i / view.body.size.coerceAtLeast(1))
        drawRoundRect(
            color,
            Offset(x + inset, y + inset),
            Size(cell - inset * 2, cell - inset * 2),
            CornerRadius(cell * 0.22f)
        )
    }
    val hx = (head % N) * cell
    val hy = (head / N) * cell
    drawCircle(Color(0xFF212121), radius = cell * 0.09f, center = Offset(hx + cell * 0.3f, hy + cell * 0.35f))
    drawCircle(Color(0xFF212121), radius = cell * 0.09f, center = Offset(hx + cell * 0.7f, hy + cell * 0.35f))

    val fx = (view.food % N) * cell + cell / 2f
    val fy = (view.food / N) * cell + cell / 2f
    drawCircle(Color(0xFFFF5252), radius = cell * 0.34f, center = Offset(fx, fy))
    drawCircle(Color(0xFFFFCDD2), radius = cell * 0.1f, center = Offset(fx - cell * 0.1f, fy - cell * 0.1f))
}
