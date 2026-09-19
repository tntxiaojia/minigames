package com.tntxiaojia.minigames.game.plane

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.game.plane.PlaneEngine.View
import com.tntxiaojia.minigames.ui.components.CornerHud
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.util.Records

private const val DT = 1f / 60f

@Composable
fun PlaneScreen(onExit: () -> Unit) {
    val engine = remember { PlaneEngine().apply { newGame() } }
    val zoom = rememberZoomState()
    var v by remember { mutableStateOf(engine.snapshot()) }
    val context = LocalContext.current
    var best by remember { mutableStateOf(Records.best(context, "plane")) }

    LaunchedEffect(v.over) {
        if (v.over) best = Records.submit(context, "plane", v.score)
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
                            detectDragGestures { change, amount ->
                                if (zoom.zooming) return@detectDragGestures
                                change.consume()
                                engine.moveBy(amount.x / size.width)
                            }
                        }
                ) {
                    drawPlane(v)
                }
            }
        }
        CornerHud(zoom) {
            Text("${v.score}", color = SecondaryAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        EndOverlay(
            visible = v.over,
            headline = "飞机被击落",
            message = "得分 ${v.score} · 最高 $best",
            onAction = { restart() },
            onExit = onExit
        )
    }
}

private fun DrawScope.drawPlane(view: View) {
    val side = size.width
    for (i in 0..9) {
        drawLine(Color(0xFF141A23), Offset(0f, side * i / 10f), Offset(side, side * i / 10f), side * 0.004f)
        drawLine(Color(0xFF141A23), Offset(side * i / 10f, 0f), Offset(side * i / 10f, side), side * 0.004f)
    }

    for (b in view.bullets) {
        val cx = b.x * side
        val cy = b.y * side
        drawCircle(Color(0xFFFFE082), radius = side * 0.018f, center = Offset(cx, cy))
    }

    val enemyColors = listOf(Color(0xFFEF5350), Color(0xFFFF9800), Color(0xFFAB47BC))
    for (e in view.enemies) {
        val cx = e.x * side
        val cy = e.y * side
        val r = side * 0.05f
        val path = Path().apply {
            moveTo(cx, cy + r)
            lineTo(cx + r, cy - r)
            lineTo(cx - r, cy - r)
            close()
        }
        drawPath(path, enemyColors[e.kind % 3])
    }

    val playerColor = if (view.blink) Color(0xFF4FC3F7).copy(alpha = 0.35f) else Color(0xFF4FC3F7)
    val px = view.px * side
    val py = 0.88f * side
    val w = side * 0.09f
    val h = side * 0.12f
    val plane = Path().apply {
        moveTo(px, py - h / 2f)
        lineTo(px + w / 2f, py + h / 2f)
        lineTo(px, py + h / 3f)
        lineTo(px - w / 2f, py + h / 2f)
        close()
    }
    drawPath(plane, playerColor)
    drawCircle(playerColor.copy(alpha = 0.8f), radius = w * 0.16f, center = Offset(px, py - h / 2f - side * 0.008f))

    for (i in 0 until view.lives.coerceAtMost(3)) {
        val lx = side * 0.09f + i * side * 0.075f
        val ly = side * 0.07f
        val heart = Path().apply {
            moveTo(lx, ly + side * 0.02f)
            lineTo(lx + side * 0.02f, ly)
            lineTo(lx + side * 0.035f, ly + side * 0.014f)
            lineTo(lx + side * 0.05f, ly)
            lineTo(lx + side * 0.07f, ly + side * 0.02f)
            lineTo(lx + side * 0.035f, ly + side * 0.05f)
            close()
        }
        drawPath(heart, Color(0xFFEF5350))
    }
}
