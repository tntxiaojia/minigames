package com.tntxiaojia.minigames.game.g2048

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.ui.components.CornerHud
import com.tntxiaojia.minigames.ui.components.EndOverlay
import com.tntxiaojia.minigames.ui.components.pinchZoom
import com.tntxiaojia.minigames.ui.components.rememberZoomState
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.util.Records
import kotlin.math.abs
import kotlinx.coroutines.launch

private val TILE_COLORS = longArrayOf(
    0xFF1E2530, // 空
    0xFF46566B, // 2
    0xFF5A6D85, // 4
    0xFFF0A35C, // 8
    0xFFEE8844, // 16
    0xFFED6A3F, // 32
    0xFFE2503C, // 64
    0xFFC93A34, // 128
    0xFFE9C46A, // 256
    0xFFE2B54E, // 512
    0xFFF2D76E, // 1024
    0xFFF6E08E, // 2048
    0xFFF2C94C, // 4096
    0xFFF2994A, // 8192
    0xFFEB5757, // 16384
    0xFF9B51E0, // 32768
)
private const val RECORD_KEY_PREFIX = "2048_"

@Composable
fun Game2048Screen(onExit: () -> Unit) {
    val engine = remember { Game2048().apply { reset(4) } }
    val zoom = rememberZoomState()
    var view by remember { mutableStateOf(engine.snapshot()) }
    var fromView by remember { mutableStateOf<Game2048.View?>(null) }
    var animating by remember { mutableStateOf(false) }
    val tm = rememberTextMeasurer()
    val progress = remember { Animatable(1f) }
    val bounce = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    var best by remember { mutableStateOf(0) }

    LaunchedEffect(view.size) {
        best = Records.best(context, RECORD_KEY_PREFIX + view.size)
    }
    LaunchedEffect(view.over) {
        if (view.over) best = Records.submit(context, RECORD_KEY_PREFIX + view.size, view.score)
    }

    fun restart(size: Int) {
        engine.reset(size)
        view = engine.snapshot()
        fromView = null
        scope.launch {
            progress.snapTo(1f)
            bounce.snapTo(1f)
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val newSize = data.getIntExtra(Game2048SettingsActivity.EXTRA_SIZE, view.size)
        val action = data.getIntExtra(Game2048SettingsActivity.EXTRA_ACTION, Game2048SettingsActivity.ACTION_NONE)
        if (action == Game2048SettingsActivity.ACTION_EXIT) {
            onExit()
        } else if (newSize != view.size) {
            restart(newSize)
        }
    }

    fun openSettings() {
        settingsLauncher.launch(
            Intent(context, Game2048SettingsActivity::class.java)
                .putExtra(Game2048SettingsActivity.EXTRA_SIZE, view.size)
        )
    }

    fun applyMove(dir: Game2048.Dir) {
        if (animating) return
        val before = engine.snapshot()
        if (!engine.move(dir)) return
        val after = engine.snapshot()
        animating = true
        scope.launch {
            fromView = before
            view = after
            bounce.snapTo(1f)
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(durationMillis = 110, easing = LinearEasing))
            if (after.mergedIds.isNotEmpty()) {
                bounce.animateTo(1.12f, animationSpec = tween(durationMillis = 70))
                bounce.animateTo(1f, animationSpec = tween(durationMillis = 80))
            }
            fromView = null
            animating = false
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
                            var acc = Offset.Zero
                            detectDragGestures(
                                onDragStart = { acc = Offset.Zero },
                                onDrag = { change, dragAmount ->
                                    if (zoom.zooming) return@detectDragGestures
                                    change.consume()
                                    acc += dragAmount
                                    val threshold = minOf(size.width, size.height) * 0.05f
                                    if (abs(acc.x) > threshold || abs(acc.y) > threshold) {
                                        val dir = if (abs(acc.x) > abs(acc.y)) {
                                            if (acc.x > 0) Game2048.Dir.RIGHT else Game2048.Dir.LEFT
                                        } else {
                                            if (acc.y > 0) Game2048.Dir.DOWN else Game2048.Dir.UP
                                        }
                                        applyMove(dir)
                                        acc = Offset.Zero
                                    }
                                }
                            )
                        }
                ) {
                    drawBoard(view, fromView, tm, progress.value, bounce.value)
                }
            }
        }

        CornerHud(zoom) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { openSettings() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("${view.score}", color = SecondaryAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        val wonShow = view.won && !view.over
        EndOverlay(
            visible = wonShow || view.over,
            headline = if (wonShow) "达成 2048！" else "游戏结束",
            message = if (wonShow) "继续挑战更高分数" else "得分 ${view.score} · 最高 $best",
            actionText = if (wonShow) "继续挑战" else "再来一局",
            secondText = if (wonShow) "再来一局" else null,
            onAction = {
                if (wonShow) {
                    engine.continueAfterWin()
                    view = engine.snapshot()
                } else {
                    restart(view.size)
                }
            },
            onSecond = if (wonShow) {
                { restart(view.size) }
            } else null,
            onExit = onExit
        )
    }
}

private fun DrawScope.drawBoard(view: Game2048.View, fromView: Game2048.View?, tm: TextMeasurer, progress: Float, bounce: Float) {
    val side = size.width
    val n = view.size
    val pad = side * 0.035f
    val gap = side * 0.014f * (4f / n)
    val usable = side - pad * 2
    val cell = (usable - gap * (n + 1)) / n
    drawRoundRect(
        color = Color(0xFF141920),
        topLeft = Offset(pad * 0.7f, pad * 0.7f),
        size = Size(usable + pad * 0.6f, usable + pad * 0.6f),
        cornerRadius = CornerRadius(side * 0.045f)
    )

    fun leftOf(col: Float) = pad + gap + col * (cell + gap)
    fun topOf(row: Float) = pad + gap + row * (cell + gap)

    // 每个格子的方格底
    for (r in 0 until n) {
        for (c in 0 until n) {
            drawRoundRect(
                color = Color(0xFF232A35),
                topLeft = Offset(leftOf(c.toFloat()), topOf(r.toFloat())),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(cell * 0.13f)
            )
        }
    }

    // 被合并的方块：从起点滑到目标位置后缩小消失
    if (fromView != null) {
        for (g in view.ghosts) {
            val p = progress.coerceIn(0f, 1f)
            val row = g.fromRow + (g.toRow - g.fromRow) * p
            val col = g.fromCol + (g.toCol - g.fromCol) * p
            val fade = if (p < 0.55f) 1f else 1f - (p - 0.55f) / 0.45f
            val s = cell * (0.55f + 0.45f * p)
            drawTile(tm, g.value, leftOf(col) + (cell - s) / 2f, topOf(row) + (cell - s) / 2f, s, fade)
        }
    }

    for (t in view.tiles) {
        val src = fromView?.tiles?.firstOrNull { it.id == t.id }
        val startRow = src?.row ?: t.row
        val startCol = src?.col ?: t.col
        val p = if (src == null) 1f else progress.coerceIn(0f, 1f)
        val row = startRow + (t.row - startRow) * p
        val col = startCol + (t.col - startCol) * p
        var scale = 1f
        var alpha = 1f
        if (view.spawnId == t.id) {
            scale = 0.5f + 0.5f * progress
            alpha = progress
        }
        if (t.id in view.mergedIds) scale *= bounce
        val s = cell * scale
        drawTile(tm, t.value, leftOf(col) + (cell - s) / 2f, topOf(row) + (cell - s) / 2f, s, alpha)
    }
}

private fun DrawScope.drawTile(tm: TextMeasurer, value: Int, x: Float, y: Float, s: Float, alpha: Float) {
    if (s < 1f || alpha <= 0.02f) return
    val exp = Integer.numberOfTrailingZeros(value).coerceIn(1, 15)
    drawRoundRect(
        color = Color(TILE_COLORS[exp]).copy(alpha = alpha),
        topLeft = Offset(x, y),
        size = Size(s, s),
        cornerRadius = CornerRadius(s * 0.13f)
    )
    val factor = when {
        value >= 16384 -> 0.17f
        value >= 1024 -> 0.2f
        value >= 100 -> 0.28f
        value >= 10 -> 0.36f
        else -> 0.46f
    }
    val textColor = when {
        exp >= 14 -> Color.White
        exp >= 3 -> Color(0xFF2B2117)
        else -> Color(0xFFF2F4F8)
    }.copy(alpha = alpha)
    val style = TextStyle(
        color = textColor,
        fontSize = (s * factor).toSp(),
        fontWeight = FontWeight.Bold
    )
    val tr = tm.measure(AnnotatedString(value.toString()), style)
    drawText(tr, topLeft = Offset(x + (s - tr.size.width) / 2f, y + (s - tr.size.height) / 2f))
}
