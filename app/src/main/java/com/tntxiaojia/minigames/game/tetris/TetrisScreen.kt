package com.tntxiaojia.minigames.game.tetris

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.game.tetris.Tetris.Companion.COLS
import com.tntxiaojia.minigames.game.tetris.Tetris.Companion.ROWS
import com.tntxiaojia.minigames.game.tetris.Tetris.View
import com.tntxiaojia.minigames.ui.components.CornerHud
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.util.Records
import com.tntxiaojia.minigames.ui.theme.TextSecondary
import kotlin.math.abs

private const val DT = 1f / 60f

private val PIECE_COLORS = listOf(
    Color(0xFF00BCD4),
    Color(0xFFFFEB3B),
    Color(0xFFAB47BC),
    Color(0xFF66BB6A),
    Color(0xFFEF5350),
    Color(0xFF42A5F5),
    Color(0xFFFFA726),
)

@Composable
fun TetrisScreen(onExit: () -> Unit) {
    val engine = remember { Tetris().apply { newGame() } }
    val zoom = rememberZoomState()
    var v by remember { mutableStateOf(engine.snapshot()) }
    val tm = rememberTextMeasurer()
    val context = LocalContext.current
    var best by remember { mutableStateOf(Records.best(context, "tetris")) }

    LaunchedEffect(v.over) {
        if (v.over) best = Records.submit(context, "tetris", v.score)
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
                                        if (!acted && acc.getDistance() < slop) {
                                            engine.rotate()
                                            v = engine.snapshot()
                                        }
                                        break
                                    }
                                    if (ch.position != ch.previousPosition) {
                                        acc += ch.position - ch.previousPosition
                                        if (!acted && acc.getDistance() >= slop) {
                                            acted = true
                                            when {
                                                abs(acc.x) > abs(acc.y) -> {
                                                    if (acc.x > 0) engine.moveRight() else engine.moveLeft()
                                                }
                                                acc.y > 0 -> engine.hardDrop()
                                                else -> engine.rotate()
                                            }
                                            v = engine.snapshot()
                                        }
                                    }
                                    ch.consume()
                                }
                            }
                        }
                ) {
                    drawTetris(v, tm)
                }
            }
        }
        CornerHud(zoom) {
            Text("${v.score}", color = SecondaryAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("行数 ${v.lines}", color = TextSecondary, fontSize = 10.sp)
        }
        EndOverlay(
            visible = v.over,
            headline = "方块堆满了",
            message = "得分 ${v.score}，消行 ${v.lines} · 最高 $best",
            onAction = { restart() },
            onExit = onExit
        )
    }
}

private fun DrawScope.drawTetris(view: View, tm: TextMeasurer) {
    val side = size.width
    val cell = side / ROWS
    val boardW = COLS * cell
    val x0 = side * 0.045f

    // 网格区底色
    drawRoundRect(
        Color(0xFF171C25),
        Offset(x0 - cell * 0.06f, 0f),
        Size(boardW + cell * 0.12f, side),
        CornerRadius(cell * 0.16f)
    )

    for (y in 0 until ROWS) {
        for (x in 0 until COLS) {
            val c = view.board[y * COLS + x]
            if (c != 0) {
                block(x, y, PIECE_COLORS[(c - 1) % PIECE_COLORS.size], cell, x0)
            }
        }
    }
    for ((gx, gy) in view.piece) {
        block(gx, gy, PIECE_COLORS[(view.pieceColor - 1) % PIECE_COLORS.size], cell, x0)
    }

    // 消行闪烁
    if (view.clearingRows.isNotEmpty()) {
        val alpha = if ((view.clearProgress * 8f).toInt() % 2 == 0) 0.85f else 0.2f
        for (r in view.clearingRows) {
            drawRoundRect(
                Color.White.copy(alpha = alpha),
                Offset(x0, r * cell),
                Size(boardW, cell),
                CornerRadius(cell * 0.1f)
            )
        }
    }

    // 右侧下一块预览
    val panelX = x0 + boardW + side * 0.025f
    val panelW = side - panelX - side * 0.02f
    val labelStyle = TextStyle(color = TextSecondary, fontSize = (panelW * 0.3f).toSp(), fontWeight = FontWeight.Bold)
    val label = tm.measure(AnnotatedString("下一块"), labelStyle)
    drawText(label, topLeft = Offset(panelX + (panelW - label.size.width) / 2f, side * 0.08f))

    val mc = panelW / 4.2f
    val cellsW = (view.next.maxOfOrNull { it.first } ?: 3) + 1
    val cellsH = (view.next.maxOfOrNull { it.second } ?: 3) + 1
    val startX = panelX + (panelW - cellsW * mc) / 2f
    val startY = side * 0.24f
    for ((nx, ny) in view.next) {
        drawRoundRect(
            PIECE_COLORS[(view.nextColor - 1) % PIECE_COLORS.size],
            Offset(startX + nx * mc + mc * 0.1f, startY + ny * mc + mc * 0.1f),
            Size(mc * 0.8f, mc * 0.8f),
            CornerRadius(mc * 0.18f)
        )
    }
}

private fun DrawScope.block(gx: Int, gy: Int, color: Color, cell: Float, x0: Float, inset: Float = cell * 0.07f) {
    drawRoundRect(
        color,
        Offset(x0 + gx * cell + inset, gy * cell + inset),
        Size(cell - inset * 2, cell - inset * 2),
        CornerRadius(cell * 0.14f)
    )
}
