package com.tntxiaojia.minigames.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tntxiaojia.minigames.ui.theme.TextSecondary

/** 右上角信息区（分数/状态等）。缩放过时可一键还原视图。 */
@Composable
fun BoxScope.CornerHud(zoom: ZoomState? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        content()
        if (zoom != null && !zoom.isDefault) {
            TextButton(onClick = { zoom.reset() }) {
                Text("还原视图", color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

/** 游戏结束遮罩。 */
@Composable
fun EndOverlay(
    visible: Boolean,
    headline: String,
    message: String = "",
    actionText: String = "再来一局",
    onAction: () -> Unit,
    secondText: String? = null,
    onSecond: (() -> Unit)? = null,
    onExit: () -> Unit,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = headline,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(text = message, color = TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionText, fontSize = 15.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
            if (secondText != null && onSecond != null) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onSecond) {
                    Text(secondText, color = TextSecondary)
                }
            }
            TextButton(onClick = onExit) {
                Text("返回列表", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
