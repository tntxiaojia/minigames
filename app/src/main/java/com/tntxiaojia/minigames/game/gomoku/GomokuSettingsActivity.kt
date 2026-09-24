package com.tntxiaojia.minigames.game.gomoku

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.ui.theme.MiniGamesTheme
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.ui.theme.TextSecondary

/** 五子棋独立设置页：模式 / 难度 / 战绩 / 悔棋 / 重新开始，结果通过 Activity Result 回传给游戏。 */
class GomokuSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val mode = intent.getIntExtra(EXTRA_MODE, Gomoku.Mode.HUMAN_AI.ordinal)
        val difficulty = intent.getIntExtra(EXTRA_DIFFICULTY, Gomoku.Difficulty.NORMAL.ordinal)
        val wins = intent.getIntExtra(EXTRA_WINS, 0)
        val losses = intent.getIntExtra(EXTRA_LOSSES, 0)
        setContent {
            MiniGamesTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    GomokuSettingsScreen(mode, difficulty, wins, losses) { m, d, action ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent()
                                .putExtra(EXTRA_MODE, m)
                                .putExtra(EXTRA_DIFFICULTY, d)
                                .putExtra(EXTRA_ACTION, action)
                        )
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_DIFFICULTY = "difficulty"
        const val EXTRA_WINS = "wins"
        const val EXTRA_LOSSES = "losses"
        const val EXTRA_ACTION = "action"
        const val ACTION_NONE = 0
        const val ACTION_UNDO = 1
        const val ACTION_RESTART = 2
        const val ACTION_EXIT = 3
    }
}

@Composable
private fun GomokuSettingsScreen(
    initialMode: Int,
    initialDifficulty: Int,
    wins: Int,
    losses: Int,
    onFinish: (mode: Int, difficulty: Int, action: Int) -> Unit,
) {
    var mode by remember { mutableIntStateOf(initialMode) }
    var difficulty by remember { mutableIntStateOf(initialDifficulty) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("五子棋设置", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Text("对局模式", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            OptionChip("人机", mode == Gomoku.Mode.HUMAN_AI.ordinal, onClick = { mode = Gomoku.Mode.HUMAN_AI.ordinal })
            Spacer(Modifier.width(8.dp))
            OptionChip("双人", mode == Gomoku.Mode.HUMAN_HUMAN.ordinal, onClick = { mode = Gomoku.Mode.HUMAN_HUMAN.ordinal })
        }

        if (mode == Gomoku.Mode.HUMAN_AI.ordinal) {
            Spacer(Modifier.height(14.dp))
            Text("电脑难度", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                OptionChip(
                    "简单",
                    difficulty == Gomoku.Difficulty.EASY.ordinal,
                    onClick = { difficulty = Gomoku.Difficulty.EASY.ordinal }
                )
                Spacer(Modifier.width(8.dp))
                OptionChip(
                    "普通",
                    difficulty == Gomoku.Difficulty.NORMAL.ordinal,
                    onClick = { difficulty = Gomoku.Difficulty.NORMAL.ordinal }
                )
                Spacer(Modifier.width(8.dp))
                OptionChip(
                    "困难",
                    difficulty == Gomoku.Difficulty.HARD.ordinal ||
                        difficulty == Gomoku.Difficulty.DEBUG.ordinal,
                    onClick = { difficulty = Gomoku.Difficulty.HARD.ordinal },
                    onLongPress = { difficulty = Gomoku.Difficulty.DEBUG.ordinal }
                )
            }
            if (difficulty == Gomoku.Difficulty.DEBUG.ordinal) {
                Spacer(Modifier.height(6.dp))
                Text("已启用隐藏难度 debug", color = Color(0xFFFF5252), fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("胜 $wins · 负 $losses", color = SecondaryAmber, fontSize = 13.sp)

        Spacer(Modifier.height(16.dp))
        BigButton("悔棋") { onFinish(mode, difficulty, GomokuSettingsActivity.ACTION_UNDO) }
        Spacer(Modifier.height(8.dp))
        BigButton("重新开始") { onFinish(mode, difficulty, GomokuSettingsActivity.ACTION_RESTART) }
        Spacer(Modifier.height(8.dp))
        BigButton("完成") { onFinish(mode, difficulty, GomokuSettingsActivity.ACTION_NONE) }
        Spacer(Modifier.height(8.dp))
        BigButton("返回列表") { onFinish(mode, difficulty, GomokuSettingsActivity.ACTION_EXIT) }
    }
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val bg = if (selected) Color(0xFF4DD0E1) else Color(0xFF2A3340)
    val fg = if (selected) Color.Black else Color(0xFFE6EAF0)
    Box(
        modifier = Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .pointerInput(onLongPress) {
                if (onLongPress == null) {
                    detectTapGestures(onTap = { onClick() })
                } else {
                    detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp))
    }
}

@Composable
private fun BigButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3340), contentColor = Color.White),
        contentPadding = PaddingValues(vertical = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
