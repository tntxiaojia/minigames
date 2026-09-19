package com.tntxiaojia.minigames.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged

/** 游戏画面的双指缩放/平移状态。1x~3x。 */
@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
        internal set
    var offset by mutableStateOf(Offset.Zero)
        internal set
    var zooming by mutableStateOf(false)
        internal set

    private var viewportW by mutableFloatStateOf(0f)
    private var viewportH by mutableFloatStateOf(0f)

    val isDefault: Boolean
        get() = scale <= 1.001f && offset == Offset.Zero

    fun reset() {
        scale = 1f
        offset = Offset.Zero
        zooming = false
    }

    internal fun onViewport(w: Float, h: Float) {
        viewportW = w
        viewportH = h
    }

    internal fun clamp(next: Offset): Offset {
        val extra = if (scale > 1.001f) 0.3f else 0f
        val maxX = viewportW * ((scale - 1f) / 2f + extra)
        val maxY = viewportH * ((scale - 1f) / 2f + extra)
        return Offset(next.x.coerceIn(-maxX, maxX), next.y.coerceIn(-maxY, maxY))
    }
}

@Composable
fun rememberZoomState(): ZoomState = remember { ZoomState() }

/**
 * 双指捏合缩放 + 平移。单指时不消费事件，游戏自身手势不受影响。
 * 触摸坐标会由 graphicsLayer 自动逆变换，游戏逻辑无需感知缩放。
 */
fun Modifier.pinchZoom(state: ZoomState): Modifier = this
    .onSizeChanged { state.onViewport(it.width.toFloat(), it.height.toFloat()) }
    .pointerInput(state) {
        awaitEachGesture {
            var prevDist = 0f
            var prevCenter = Offset.Zero
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val p0 = pressed[0].position
                    val p1 = pressed[1].position
                    val dist = (p1 - p0).getDistance()
                    val center = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                    if (!state.zooming) {
                        state.zooming = true
                    } else if (prevDist > 1f) {
                        state.scale = (state.scale * (dist / prevDist)).coerceIn(1f, 3f)
                        state.offset = state.clamp(state.offset + (center - prevCenter))
                    }
                    prevDist = dist
                    prevCenter = center
                    event.changes.forEach { it.consume() }
                } else if (pressed.isEmpty()) {
                    state.zooming = false
                    break
                } else {
                    // 缩放中只剩一指：保持缩放态并继续消费，避免事件漏给游戏手势造成误触
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
    .graphicsLayer(
        scaleX = state.scale,
        scaleY = state.scale,
        translationX = state.offset.x,
        translationY = state.offset.y,
        transformOrigin = TransformOrigin.Center,
    )
