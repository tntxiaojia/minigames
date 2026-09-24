package com.tntxiaojia.minigames.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.R
import com.tntxiaojia.minigames.ui.theme.MiniGamesTheme
import com.tntxiaojia.minigames.ui.theme.PrimaryCyan
import com.tntxiaojia.minigames.ui.theme.SecondaryAmber
import com.tntxiaojia.minigames.ui.theme.SurfaceDark
import com.tntxiaojia.minigames.ui.theme.TextSecondary
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PROJECT_URL = "https://github.com/tntxiaojia/minigames"
private const val DEV_URL = "https://github.com/tntxiaojia"
private const val VERSION_URL =
    "https://raw.githubusercontent.com/tntxiaojia/minigames/refs/heads/main/version.txt"

/** 关于页：图标/名称/版本/项目地址/开发者/检查更新。 */
class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val info = runCatching { packageManager.getPackageInfo(packageName, 0) }.getOrNull()
        val versionName = info?.versionName.orEmpty()
        val versionCode = info?.let {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                it.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION") it.versionCode
            }
        } ?: 0
        setContent {
            MiniGamesTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    AboutScreen(versionName, versionCode, onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(versionName: String, versionCode: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(SurfaceDark)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.app_name),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text("版本 $versionName ($versionCode)", color = TextSecondary, fontSize = 12.sp)

        Spacer(Modifier.height(16.dp))
        Text(
            "github.com/tntxiaojia/minigames",
            color = PrimaryCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { openUrl(context, PROJECT_URL) }
        )

        Spacer(Modifier.height(16.dp))
        DeveloperRow(R.drawable.touxiang, "tntxiaojia", DEV_URL, context)
        Spacer(Modifier.height(10.dp))
        DeveloperRow(R.drawable.deepseek, "deepseek", null, context)

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                Toast.makeText(context, "正在向github查询更新中", Toast.LENGTH_SHORT).show()
                if (!isOnline(context)) {
                    Toast.makeText(context, "请连接互联网", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        val info = fetchVersionInfo()
                        if (info == null) {
                            Toast.makeText(context, "检查更新失败，请稍后再试", Toast.LENGTH_SHORT).show()
                        } else {
                            val remoteCode = info["versioncode"]?.toIntOrNull() ?: 0
                            if (remoteCode > versionCode) {
                                context.startActivity(
                                    Intent(context, UpdateActivity::class.java)
                                        .putExtra(UpdateActivity.EXTRA_VERSION, info["lastestversion"] ?: "")
                                        .putExtra(UpdateActivity.EXTRA_INFO, info["versioninfo"] ?: "")
                                )
                            } else {
                                Toast.makeText(context, "当前为最新版本", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "检查更新",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("返回列表", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun DeveloperRow(avatarRes: Int, name: String, homeUrl: String?, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF232A35))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(avatarRes),
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (homeUrl != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "github.com/tntxiaojia",
                    color = PrimaryCyan,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { openUrl(context, homeUrl) }
                )
            }
        }
    }
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/** 拉取并解析 version.txt（每行 key=value）。 */
private suspend fun fetchVersionInfo(): Map<String, String>? = withContext(Dispatchers.IO) {
    try {
        val conn = (URL(VERSION_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        text.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }
            .toMap()
    } catch (e: Exception) {
        null
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}
