package com.tntxiaojia.minigames.game.jump

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.tntxiaojia.minigames.game.jump.JumpEngine.State
import com.tntxiaojia.minigames.game.jump.JumpEngine.View
import com.tntxiaojia.minigames.ui.components.CornerHud
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.util.Records

private const val DT = 1f / 60f
private val PLAT_COLORS = listOf(Color(0xFFFF8A65), Color(0xFF4DB6AC), Color(0xFF81C784))

@Composable
fun JumpScreen(onExit: () -> Unit) {
    val engine = remember { JumpEngine().apply { newGame() } }
    val zoom = rememberZoomState()
    var v by remember { mutableStateOf(engine.snapshot()) }
    val context = LocalContext.current
    var best by remember { mutableStateOf(Records.best(context, "jump")) }

    LaunchedEffect(v.over) {
        if (v.over) best = Records.submit(context, "jump", v.score)
    }

    LaunchedEffect(engine) {
        while (true) {
            if (engine.state != State.OVER) {
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
                            detectTapGestures(
                                onPress = {
                                    if (zoom.zooming) return@detectTapGestures
                                    engine.press()
                                    v = engine.snapshot()
                                    val released = tryAwaitRelease()
                                    if (released) {
                                        engine.release()
                                        v = engine.snapshot()
                                    }
                                }
                            )
                        }
                ) {
                    drawJump(v)
                }
            }
        }
        CornerHud(zoom) {
            Text("${v.score}", color = SecondaryAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        EndOverlay(
            visible = v.over,
            headline = "没跳上去",
            message = "共跳过 ${v.score} 块 · 最高 $best",
            onAction = { restart() },
            onExit = onExit
        )
    }
}

private fun DrawScope.drawJump(view: View) {
    val side = size.width
    val topY = JumpEngine.PLAT_TOP * side
    val bottomY = 0.92f * side

    for ((i, p) in view.platforms.withIndex()) {
        if (p.x < -0.35f || p.x > 1.35f) continue
        val color = if (i == view.activeIndex) Color(0xFFE8A33D) else PLAT_COLORS[p.colorIdx % PLAT_COLORS.size]
        platform(p.x, p.w, color, side, topY, bottomY)
    }

    val pw = side * JumpEngine.PLAYER_SIZE
    val ph = side * JumpEngine.PLAYER_SIZE
    val squash = if (view.state == State.CHARGE) 1f - 0.22f * view.power else 1f
    val px = view.playerSX * side
    val py = topY - ph * squash / 2f - view.offsetY * side
    val corner = CornerRadius(pw * 0.18f)
    drawRoundRect(
        color = Color(0xFF4DD0E1),
        topLeft = Offset(px - pw / 2f, py - ph * squash / 2f),
        size = Size(pw, ph * squash),
        cornerRadius = corner
    )
    drawRoundRect(
        color = Color(0xFFFFFFFF).copy(alpha = 0.4f),
        topLeft = Offset(px - pw / 2f + pw * 0.12f, py - ph * squash / 2f + pw * 0.12f),
        size = Size(pw * 0.3f, ph * squash * 0.25f),
        cornerRadius = CornerRadius(pw * 0.1f)
    )

    if (view.state == State.CHARGE || view.state == State.FLY) {
        val barW = side * 0.36f
        val barH = side * 0.045f
        val bx = side * 0.32f
        val by = side * 0.08f
        drawRoundRect(Color(0xFF333B47), Offset(bx, by), Size(barW, barH), CornerRadius(barH / 2f))
        val fill = if (view.state == State.CHARGE) view.power else 1f
        if (fill > 0.01f) {
            drawRoundRect(Color(0xFF4DD0E1), Offset(bx, by), Size(barW * fill, barH), CornerRadius(barH / 2f))
        }
    }
}

private fun DrawScope.platform(cx: Float, w: Float, color: Color, side: Float, topY: Float, bottomY: Float) {
    val left = cx * side - w * side / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(left, topY),
        size = Size(w * side, bottomY - topY),
        cornerRadius = CornerRadius(side * 0.02f)
    )
    drawRoundRect(
        color = color.copy(alpha = 0.55f),
        topLeft = Offset(left, topY),
        size = Size(w * side, (bottomY - topY) * 0.22f),
        cornerRadius = CornerRadius(side * 0.02f)
    )
}
