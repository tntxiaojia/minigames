package com.tntxiaojia.minigames.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.ui.theme.Background
import com.tntxiaojia.minigames.ui.theme.TextPrimary

@Composable
fun MainScreen(onPick: (GameId) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp)) {
            Text("小游戏合集", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(top = 5.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(GameId.entries) { game ->
                GameCard(game = game, onClick = { onPick(game) })
            }
            item {
                InfoCard("关于", Color(0xFF90A4AE)) {
                    context.startActivity(Intent(context, AboutActivity::class.java))
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameId, onClick: () -> Unit) {
    val accent = accentOf(game)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.13f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.24f)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(26.dp)) {
                drawGameIcon(game, size.width, size.height)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(game.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoCard(title: String, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.13f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.24f)),
            contentAlignment = Alignment.Center
        ) {
            Text("i", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun accentOf(game: GameId): Color = when (game) {
    GameId.G2048 -> Color(0xFFFFB74D)
    GameId.GOMOKU -> Color(0xFFA1887F)
    GameId.PLANE -> Color(0xFF4FC3F7)
    GameId.JUMP -> Color(0xFFFF8A65)
    GameId.FLAPPY -> Color(0xFF81C784)
    GameId.TETRIS -> Color(0xFFBA68C8)
    GameId.SNAKE -> Color(0xFF4DB6AC)
    GameId.MINES -> Color(0xFFE57373)
    GameId.FLOORS -> Color(0xFF7E57C2)
}

private fun DrawScope.drawGameIcon(game: GameId, w: Float, h: Float) {
    val white = Color.White
    when (game) {
        GameId.G2048 -> {
            val c = w / 4f
            val gap = w * 0.04f
            drawRoundRect(white.copy(alpha = 0.95f), Offset(gap, gap), Size(c - 2 * gap, c - 2 * gap), CornerRadius(c * 0.15f))
            drawRoundRect(white.copy(alpha = 0.65f), Offset(c + gap, gap), Size(c - 2 * gap, c - 2 * gap), CornerRadius(c * 0.15f))
            drawRoundRect(white.copy(alpha = 0.65f), Offset(gap, c + gap), Size(c - 2 * gap, c - 2 * gap), CornerRadius(c * 0.15f))
            drawRoundRect(white.copy(alpha = 0.45f), Offset(c + gap, c + gap), Size(c - 2 * gap, c - 2 * gap), CornerRadius(c * 0.15f))
        }
        GameId.GOMOKU -> {
            val a = w * 0.08f
            val b = w - a
            for (i in 0..2) {
                val p = a + (b - a) * i / 2f
                drawLine(white.copy(alpha = 0.9f), Offset(p, a), Offset(p, b), w * 0.025f)
                drawLine(white.copy(alpha = 0.9f), Offset(a, p), Offset(b, p), w * 0.025f)
            }
            drawCircle(white, radius = w * 0.12f, center = Offset(a + (b - a) * 0.33f, a + (b - a) * 0.33f))
            drawCircle(Color(0xFF10141B), radius = w * 0.1f, center = Offset(a + (b - a) * 0.66f, a + (b - a) * 0.66f))
        }
        GameId.PLANE -> {
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.06f)
                lineTo(w * 0.8f, h * 0.78f)
                lineTo(w * 0.5f, h * 0.58f)
                lineTo(w * 0.2f, h * 0.78f)
                close()
            }
            drawPath(path, white)
        }
        GameId.JUMP -> {
            drawRoundRect(white.copy(alpha = 0.5f), Offset(w * 0.06f, h * 0.68f), Size(w * 0.34f, h * 0.14f), CornerRadius(w * 0.05f))
            drawRoundRect(white.copy(alpha = 0.85f), Offset(w * 0.5f, h * 0.68f), Size(w * 0.34f, h * 0.14f), CornerRadius(w * 0.05f))
            drawRoundRect(white, Offset(w * 0.2f, h * 0.5f), Size(w * 0.22f, h * 0.18f), CornerRadius(w * 0.05f))
        }
        GameId.FLAPPY -> {
            drawCircle(white, radius = w * 0.13f, center = Offset(w * 0.34f, h * 0.5f))
            drawRoundRect(white, Offset(w * 0.62f, h * 0.04f), Size(w * 0.14f, h * 0.3f), CornerRadius(w * 0.02f))
            drawRoundRect(white, Offset(w * 0.62f, h * 0.64f), Size(w * 0.14f, h * 0.32f), CornerRadius(w * 0.02f))
        }
        GameId.TETRIS -> {
            val c = w * 0.16f
            val colors = listOf(white.copy(alpha = 0.9f), white.copy(alpha = 0.55f), white.copy(alpha = 0.75f), white.copy(alpha = 0.4f))
            val pts = listOf(Offset(w * 0.2f, h * 0.26f), Offset(w * 0.38f, h * 0.26f), Offset(w * 0.56f, h * 0.26f), Offset(w * 0.56f, h * 0.44f))
            pts.forEachIndexed { i, o ->
                drawRoundRect(colors[i], o, Size(c, c), CornerRadius(c * 0.12f))
            }
        }
        GameId.SNAKE -> {
            drawRoundRect(white.copy(alpha = 0.6f), Offset(w * 0.1f, h * 0.66f), Size(w * 0.26f, w * 0.26f), CornerRadius(w * 0.08f))
            drawRoundRect(white.copy(alpha = 0.8f), Offset(w * 0.36f, h * 0.46f), Size(w * 0.26f, w * 0.26f), CornerRadius(w * 0.08f))
            drawRoundRect(white.copy(alpha = 0.95f), Offset(w * 0.62f, h * 0.26f), Size(w * 0.26f, w * 0.26f), CornerRadius(w * 0.08f))
            drawCircle(Color(0xFFFF6B6B), radius = w * 0.07f, center = Offset(w * 0.76f, h * 0.3f))
        }
        GameId.MINES -> {
            val c = Offset(w * 0.5f, h * 0.5f)
            val r = w * 0.22f
            drawCircle(white.copy(alpha = 0.9f), radius = r, center = c)
            drawCircle(Background.copy(alpha = 0.5f), radius = r * 0.72f, center = c)
            drawCircle(Color(0xFFFF5252), radius = w * 0.08f, center = c)
            val pts = listOf(Offset(-1f, 0f), Offset(1f, 0f), Offset(0f, -1f), Offset(0f, 1f))
            for (d in pts) {
                drawLine(
                    white.copy(alpha = 0.85f),
                    Offset(c.x + d.x * r * 1.35f, c.y + d.y * r * 1.35f),
                    Offset(c.x + d.x * r * 1.9f, c.y + d.y * r * 1.9f),
                    w * 0.06f
                )
            }
        }
        GameId.FLOORS -> {
            drawRoundRect(white.copy(alpha = 0.9f), Offset(w * 0.1f, h * 0.62f), Size(w * 0.34f, h * 0.1f), CornerRadius(w * 0.04f))
            drawRoundRect(white.copy(alpha = 0.6f), Offset(w * 0.56f, h * 0.78f), Size(w * 0.34f, h * 0.1f), CornerRadius(w * 0.04f))
            val arrow = Path().apply {
                moveTo(w * 0.5f, h * 0.08f)
                lineTo(w * 0.72f, h * 0.4f)
                lineTo(w * 0.58f, h * 0.4f)
                lineTo(w * 0.58f, h * 0.58f)
                lineTo(w * 0.42f, h * 0.58f)
                lineTo(w * 0.42f, h * 0.4f)
                lineTo(w * 0.28f, h * 0.4f)
                close()
            }
            drawPath(arrow, white)
        }
    }
}
