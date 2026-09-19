package com.tntxiaojia.minigames.game.floors

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.game.floors.FloorsEngine.Companion.PLAT_H
import com.tntxiaojia.minigames.game.floors.FloorsEngine.Companion.PLAYER_SIZE
import com.tntxiaojia.minigames.game.floors.FloorsEngine.Companion.SPIKE_H
import com.tntxiaojia.minigames.game.floors.FloorsEngine.Companion.SPIKE_TOP_H
import com.tntxiaojia.minigames.game.floors.FloorsEngine.Kind
import com.tntxiaojia.minigames.game.floors.FloorsEngine.PlatformView
import com.tntxiaojia.minigames.game.floors.FloorsEngine.View
import com.tntxiaojia.minigames.ui.components.CornerHud
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.util.Records

private const val DT = 1f / 60f
private const val DRAG_SCALE = 0.6f

@Composable
fun FloorsScreen(onExit: () -> Unit) {
    val engine = remember { FloorsEngine().apply { newGame() } }
    val zoom = rememberZoomState()
    var v by remember { mutableStateOf(engine.snapshot()) }
    val context = LocalContext.current
    var best by remember { mutableStateOf(Records.best(context, "floors")) }

    LaunchedEffect(v.over) {
        if (v.over) best = Records.submit(context, "floors", v.floors)
    }

    LaunchedEffect(engine) {
        while (true) {
            if (!engine.over) {
                engine.step(DT)
                v = engine.snapshot()
                kotlinx.coroutines.delay(16)
            } else {
                kotlinx.coroutines.delay(150)
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
                                val edge = when {
                                    down.position.x < size.width * 0.22f -> -1
                                    down.position.x > size.width * 0.78f -> 1
                                    else -> 0
                                }
                                if (edge != 0) {
                                    engine.setHoldDir(edge)
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!ch.pressed) break
                                        if (zoom.zooming) {
                                            engine.setHoldDir(0)
                                            break
                                        }
                                        ch.consume()
                                    }
                                    engine.setHoldDir(0)
                                } else {
                                    var last = down.position
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!ch.pressed) break
                                        if (ch.position != ch.previousPosition) {
                                            engine.moveBy((ch.position.x - last.x) / size.width * DRAG_SCALE)
                                            last = ch.position
                                        }
                                        ch.consume()
                                    }
                                }
                            }
                        }
                ) {
                    drawFloors(v)
                }
            }
        }

        CornerHud(zoom) {
            Text("第 ${v.floors} 层", color = SecondaryAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        EndOverlay(
            visible = v.over,
            headline = "游戏结束",
            message = "本局 ${v.floors} 层 · 最高 $best 层",
            onAction = { restart() },
            onExit = onExit
        )
    }
}

private fun DrawScope.drawFloors(view: View) {
    val side = size.width

    // 顶部朝下尖刺
    val topH = SPIKE_TOP_H * side
    val count = 18
    val step = side / count
    for (k in 0 until count) {
        val path = Path()
        val x0 = k * step
        path.moveTo(x0, 0f)
        path.lineTo(x0 + step, 0f)
        path.lineTo(x0 + step / 2f, topH)
        path.close()
        drawPath(path, Color(0xFFE57373))
    }

    // 两侧墙壁
    drawRect(Color(0xFF161C25), Offset(0f, 0f), Size(side * 0.04f, side))
    drawRect(Color(0xFF161C25), Offset(side * 0.96f, 0f), Size(side * 0.04f, side))

    for (p in view.platforms) {
        drawPlatform(p, side)
    }

    val ps = PLAYER_SIZE * side
    val px = view.playerX * side
    val py = view.playerY * side
    drawRoundRect(
        Color(0xFF4DD0E1),
        Offset(px - ps / 2f, py - ps / 2f),
        Size(ps, ps),
        CornerRadius(ps * 0.18f)
    )
}

private fun DrawScope.drawPlatform(p: PlatformView, side: Float) {
    val kind = Kind.values()[p.kind]
    val w = p.w * side
    val h = PLAT_H * side
    val left = p.x * side - w / 2f
    val top = p.y * side - h / 2f
    if (top > side || top + h < 0f) return
    val color = when (kind) {
        Kind.NORMAL -> Color(0xFFF5F5F5)
        Kind.VANISH -> Color(0xFFEF5350)
        Kind.SPIKE -> Color(0xFF546E7A)
        Kind.CONVEYOR_LEFT, Kind.CONVEYOR_RIGHT -> Color(0xFF42A5F5)
        Kind.BOUNCE -> Color(0xFFFFD54F)
        Kind.MOVING -> Color(0xFF4DB6AC)
        Kind.PURPLE -> Color(0xFFAB47BC)
    }
    drawRoundRect(color, Offset(left, top), Size(w, h), CornerRadius(h * 0.4f))

    when (kind) {
        Kind.SPIKE -> {
            val cnt = 5
            val sw = w / cnt
            for (k in 0 until cnt) {
                val path = Path()
                val x0 = left + k * sw
                path.moveTo(x0, top)
                path.lineTo(x0 + sw, top)
                path.lineTo(x0 + sw / 2f, top - SPIKE_H * side)
                path.close()
                drawPath(path, Color(0xFFE57373))
            }
        }
        Kind.CONVEYOR_LEFT, Kind.CONVEYOR_RIGHT -> {
            val dir = if (kind == Kind.CONVEYOR_LEFT) -1f else 1f
            val cy = p.y * side
            for (k in 0 until 3) {
                val cx = left + w * (0.25f + 0.25f * k)
                val a = side * 0.014f * dir
                val path = Path()
                path.moveTo(cx + a, cy - side * 0.018f)
                path.lineTo(cx - a, cy)
                path.lineTo(cx + a, cy + side * 0.018f)
                path.close()
                drawPath(path, Color(0xFF0D47A1))
            }
        }
        else -> Unit
    }
}
