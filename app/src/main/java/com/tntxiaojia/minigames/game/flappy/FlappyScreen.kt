package com.tntxiaojia.minigames.game.flappy

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
import com.tntxiaojia.minigames.game.flappy.FlappyEngine.Companion.BIRD_R
import com.tntxiaojia.minigames.game.flappy.FlappyEngine.Companion.BIRD_X
import com.tntxiaojia.minigames.game.flappy.FlappyEngine.Companion.GAP_H
import com.tntxiaojia.minigames.game.flappy.FlappyEngine.Companion.PIPE_W
import com.tntxiaojia.minigames.game.flappy.FlappyEngine.State
import com.tntxiaojia.minigames.game.flappy.FlappyEngine.View
import com.tntxiaojia.minigames.ui.components.CornerHud
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.util.Records

private const val DT = 1f / 60f
private const val GROUND_Y = 0.92f

@Composable
fun FlappyScreen(onExit: () -> Unit) {
    val engine = remember { FlappyEngine().apply { newGame() } }
    val zoom = rememberZoomState()
    var v by remember { mutableStateOf(engine.snapshot()) }
    val context = LocalContext.current
    var best by remember { mutableStateOf(Records.best(context, "flappy")) }

    LaunchedEffect(v.over) {
        if (v.over) best = Records.submit(context, "flappy", v.score)
    }

    LaunchedEffect(engine) {
        while (true) {
            if (engine.state == State.RUN) {
                engine.step(DT)
                v = engine.snapshot()
                kotlinx.coroutines.delay(16)
            } else {
                kotlinx.coroutines.delay(80)
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
                                down.consume()
                                if (engine.state == State.READY) engine.start() else engine.flap()
                                v = engine.snapshot()
                            }
                        }
                ) {
                    drawFlappy(v)
                }
                if (v.state == State.READY) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "点击开始",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
        CornerHud(zoom) {
            Text("${v.score}", color = SecondaryAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        EndOverlay(
            visible = v.over,
            headline = "撞上了",
            message = "得分 ${v.score} · 最高 $best",
            onAction = { restart() },
            onExit = onExit
        )
    }
}

private fun DrawScope.drawFlappy(view: View) {
    val side = size.width

    // 地面
    drawRect(Color(0xFF3E2723), topLeft = Offset(0f, GROUND_Y * side), size = Size(side, side * (1f - GROUND_Y)))
    drawRect(Color(0xFF5D4037), topLeft = Offset(0f, GROUND_Y * side), size = Size(side, side * 0.014f))

    for (p in view.pipes) {
        pipe(p.x * side, 0f, p.gapTop * side)
        pipe(p.x * side, (p.gapTop + GAP_H) * side, side)
    }

    val bx = BIRD_X * side
    val by = view.y * side
    val r = BIRD_R * side
    drawCircle(Color(0xFFFFEB3B), radius = r, center = Offset(bx, by))
    val eye = Offset(bx + r * 0.3f, by - r * 0.25f)
    drawCircle(Color.White, radius = r * 0.22f, center = eye)
    drawCircle(Color(0xFF212121), radius = r * 0.1f, center = eye)
    val beak = Path().apply {
        moveTo(bx + r * 0.6f, by)
        lineTo(bx + r * 1.25f, by + r * 0.08f)
        lineTo(bx + r * 0.6f, by + r * 0.3f)
        close()
    }
    drawPath(beak, Color(0xFFFF8A65))
}

private fun DrawScope.pipe(left: Float, top: Float, bottom: Float) {
    if (bottom - top < 1f) return
    val side = size.width
    val w = PIPE_W * side
    val body = Color(0xFF43A047)
    val edge = Color(0xFF2E7D32)
    drawRect(body, topLeft = Offset(left, top), size = Size(w, bottom - top))
    drawRect(edge, topLeft = Offset(left, top), size = Size(w, side * 0.006f))
    val capH = side * 0.05f
    val capOver = side * 0.014f
    val capTop = if (top < 0.01f) bottom - capH else top
    if (top < 0.01f) {
        drawRoundRect(body, Offset(left - capOver, capTop), Size(w + capOver * 2, capH), CornerRadius(side * 0.012f))
    } else {
        drawRoundRect(body, Offset(left - capOver, top), Size(w + capOver * 2, capH), CornerRadius(side * 0.012f))
    }
}
