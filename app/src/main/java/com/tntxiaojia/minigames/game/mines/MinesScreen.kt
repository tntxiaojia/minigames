package com.tntxiaojia.minigames.game.mines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.game.mines.Mines.Companion.COLS
import com.tntxiaojia.minigames.game.mines.Mines.Companion.MINES
import com.tntxiaojia.minigames.game.mines.Mines.Companion.ROWS
import com.tntxiaojia.minigames.ui.components.CornerHud
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.ui.theme.TextSecondary
import kotlin.math.floor

private val NUM_COLORS = longArrayOf(
    0xFF8A94A6,
    0xFF64B5F6,
    0xFF81C784,
    0xFFE57373,
    0xFFBA68C8,
    0xFFFFB74D,
    0xFF4DB6AC,
    0xFFE0E0E0,
    0xFF90A4AE,
)

@Composable
fun MinesScreen(onExit: () -> Unit) {
    val engine = remember { Mines() }
    val zoom = rememberZoomState()
    var v by remember { mutableStateOf(engine.snapshot()) }
    val tm = rememberTextMeasurer()

    fun restart() {
        engine.reset()
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
                                onTap = { pos ->
                                    if (zoom.zooming) return@detectTapGestures
                                    val cell = size.width / COLS
                                    val x = floor(pos.x / cell).toInt().coerceIn(0, COLS - 1)
                                    val y = floor(pos.y / cell).toInt().coerceIn(0, ROWS - 1)
                                    engine.tap(x, y)
                                    v = engine.snapshot()
                                },
                                onLongPress = { pos ->
                                    if (zoom.zooming) return@detectTapGestures
                                    val cell = size.width / COLS
                                    val x = floor(pos.x / cell).toInt().coerceIn(0, COLS - 1)
                                    val y = floor(pos.y / cell).toInt().coerceIn(0, ROWS - 1)
                                    engine.toggleFlag(x, y)
                                    v = engine.snapshot()
                                }
                            )
                        }
                ) {
                    drawMines(v, tm)
                }
            }
        }
        CornerHud(zoom) {
            Text("剩余雷", color = TextSecondary, fontSize = 10.sp)
            Text(
                text = "${MINES - engine.flagCount()}",
                color = SecondaryAmber,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        EndOverlay(
            visible = v.over || v.won,
            headline = if (v.won) "扫雷成功！" else "踩到地雷",
            message = if (v.won) "所有安全格都已翻开" else "长按可标记旗帜",
            onAction = { restart() },
            onExit = onExit
        )
    }
}

private fun DrawScope.drawMines(view: Mines.View, tm: TextMeasurer) {
    val side = size.width
    val cell = side / COLS
    val pad = side * 0.006f
    for (y in 0 until ROWS) {
        for (x in 0 until COLS) {
            val idx = y * COLS + x
            val isOpened = view.opened[idx]
            val isFlag = view.flagged[idx]
            val isMine = view.grid[idx] == Mines.MINE
            val topLeft = Offset(x * cell + pad / 2, y * cell + pad / 2)
            val s = Size(cell - pad, cell - pad)
            val radius = CornerRadius(cell * 0.14f)
            if (!isOpened) {
                drawRoundRect(Color(0xFF3A4350), topLeft, s, radius)
                val hl = Offset(x * cell + pad, y * cell + pad)
                drawRoundRect(Color(0xFF46505F), Offset(hl.x, hl.y), Size(s.width - pad, s.height * 0.5f - pad / 2), CornerRadius(cell * 0.1f))
            } else {
                drawRoundRect(Color(0xFF232A35), topLeft, s, radius)
                if (isMine) {
                    drawMine(topLeft, s, cell)
                } else if (view.grid[idx] > 0) {
                    val style = TextStyle(
                        color = Color(NUM_COLORS[view.grid[idx]]),
                        fontSize = (cell * 0.52f).toSp(),
                        fontWeight = FontWeight.Bold
                    )
                    val text = view.grid[idx].toString()
                    val tr = tm.measure(AnnotatedString(text), style)
                    drawText(
                        tr,
                        topLeft = Offset(
                            topLeft.x + (s.width - tr.size.width) / 2f,
                            topLeft.y + (s.height - tr.size.height) / 2f
                        )
                    )
                }
            }
            if (isFlag) {
                drawFlag(topLeft, s, cell)
            }
        }
    }
}

private fun DrawScope.drawFlag(topLeft: Offset, s: Size, cell: Float) {
    val poleX = topLeft.x + s.width * 0.5f
    val poleTop = topLeft.y + s.height * 0.2f
    drawLine(
        Color(0xFFE0E0E0),
        Offset(poleX, topLeft.y + s.height * 0.85f),
        Offset(poleX, poleTop),
        cell * 0.06f
    )
    val flag = Path().apply {
        moveTo(poleX, poleTop)
        lineTo(poleX + s.width * 0.32f, poleTop + s.height * 0.13f)
        lineTo(poleX, poleTop + s.height * 0.26f)
        close()
    }
    drawPath(flag, Color(0xFFFF5252))
}

private fun DrawScope.drawMine(topLeft: Offset, s: Size, cell: Float) {
    val cx = topLeft.x + s.width / 2f
    val cy = topLeft.y + s.height / 2f
    val r = cell * 0.2f
    drawCircle(Color(0xFF14181F), radius = r, center = Offset(cx, cy))
    val pts = listOf(Offset(-1f, 0f), Offset(1f, 0f), Offset(0f, -1f), Offset(0f, 1f))
    for (d in pts) {
        drawLine(
            Color(0xFFE0E0E0),
            Offset(cx + d.x * r * 0.8f, cy + d.y * r * 0.8f),
            Offset(cx + d.x * r * 1.6f, cy + d.y * r * 1.6f),
            cell * 0.06f
        )
    }
    drawCircle(Color(0xFFFF5252), radius = r * 0.45f, center = Offset(cx, cy))
}
