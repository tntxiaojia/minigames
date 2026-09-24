package com.tntxiaojia.minigames.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.ui.theme.MiniGamesTheme
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.ui.theme.TextSecondary

/** 更新页：发现新版本 / 最新版本 / 更新内容 / 更新与取消。 */
class UpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val version = intent.getStringExtra(EXTRA_VERSION).orEmpty()
        val info = intent.getStringExtra(EXTRA_INFO).orEmpty()
        setContent {
            MiniGamesTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    UpdateScreen(version, info) { finish() }
                }
            }
        }
    }

    companion object {
        const val EXTRA_VERSION = "version"
        const val EXTRA_INFO = "info"
        const val RELEASE_URL =
            "https://github.com/tntxiaojia/minigames/releases/tag/releases/latest"
    }
}

@Composable
private fun UpdateScreen(version: String, info: String, onCancel: () -> Unit) {
    val context = LocalContext.current
    val content = if (info.isBlank() || info == "null") {
        "暂无更新说明"
    } else {
        info.replace("\\n", "\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(SurfaceDark)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("发现新版本", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("最新版本：$version", color = SecondaryAmber, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(content, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateActivity.RELEASE_URL)))
                } catch (e: Exception) {
                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "更新",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("取消", color = TextSecondary, fontSize = 14.sp)
        }
    }
}
