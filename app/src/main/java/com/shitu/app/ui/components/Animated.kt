package com.shitu.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

// 数字滚动动画，用于属性与评分变化。
@Composable
fun AnimatedNumber(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 700),
        label = "animatedNumber"
    )
    Text(text = animated.toString(), style = style, color = color, modifier = modifier)
}

// 出现动画进度：内容切换时淡入并轻微上移，不引入额外依赖。
@Composable
fun rememberAppearProgress(key: Any): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 420))
    }
    return progress.value
}

// 带淡入与上移的内容容器。
@Composable
fun AppearBox(
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val progress = rememberAppearProgress(key)
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24f
        },
        content = content
    )
}

// 带序号的淡入：同一批结果依次出现，形成轻微的“数字跳出来”效果。
@Composable
fun StaggerAppear(
    index: Int,
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(key, index) {
        progress.snapTo(0f)
        delay((index.coerceAtMost(8) * 70).toLong())
        progress.animateTo(1f, animationSpec = tween(durationMillis = 320))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 14f
        },
        content = content
    )
}

// 播放一次的高度变化提示，用于晋升等强调信息。
@Composable
fun HighlightBanner(
    text: String,
    key: Any,
    modifier: Modifier = Modifier
) {
    val progress = rememberAppearProgress(key)
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            scaleX = 0.92f + 0.08f * progress
            scaleY = 0.92f + 0.08f * progress
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
