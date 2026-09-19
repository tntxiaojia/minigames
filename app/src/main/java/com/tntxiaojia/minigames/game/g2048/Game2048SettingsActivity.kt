package com.tntxiaojia.minigames.game.g2048

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.ui.theme.MiniGamesTheme
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.ui.theme.TextSecondary
import com.tntxiaojia.minigames.util.Records

/** 2048 独立设置页：棋盘布局选择 + 各布局历史最高分 + 返回游戏/返回列表。 */
class Game2048SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val size = intent.getIntExtra(EXTRA_SIZE, 4)
        setContent {
            MiniGamesTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    Game2048SettingsScreen(size) { selectedSize, action ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent()
                                .putExtra(EXTRA_SIZE, selectedSize)
                                .putExtra(EXTRA_ACTION, action)
                        )
                        finish()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SIZE = "size"
        const val EXTRA_ACTION = "action"
        const val ACTION_NONE = 0
        const val ACTION_EXIT = 1
    }
}

@Composable
private fun Game2048SettingsScreen(initialSize: Int, onFinish: (size: Int, action: Int) -> Unit) {
    var size by remember { mutableIntStateOf(initialSize) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("2048 设置", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Text("棋盘布局", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            SizeChip("4×4", size == 4) { size = 4 }
            Spacer(Modifier.width(8.dp))
            SizeChip("5×5", size == 5) { size = 5 }
            Spacer(Modifier.width(8.dp))
            SizeChip("6×6", size == 6) { size = 6 }
        }

        Spacer(Modifier.height(16.dp))
        Text("历史最高分", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        for (n in 4..6) {
            Text(
                "$n×$n　${Records.best(context, "2048_$n")}",
                color = SecondaryAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        BigButton("返回游戏") { onFinish(size, Game2048SettingsActivity.ACTION_NONE) }
        Spacer(Modifier.height(8.dp))
        BigButton("返回列表") { onFinish(size, Game2048SettingsActivity.ACTION_EXIT) }
    }
}

@Composable
private fun SizeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF4DD0E1) else Color(0xFF2A3340)
    val fg = if (selected) Color.Black else Color(0xFFE6EAF0)
    Box(
        modifier = Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = fg,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
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
