package com.shitu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.ui.components.AppearBox
import com.shitu.app.ui.components.ShituCard

private data class TutorialStep(val title: String, val body: String)

// 新手引导：最多 7 步，可随时跳过，完成状态由 SaveManager 保存。
private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        "这是你的职位",
        "首页顶部会显示你的姓名、当前官职、任职地区和任期。所有重要信息都从这里开始。"
    ),
    TutorialStep(
        "政务决定辖区",
        "「政务」页每季度有固定行动点，处理经济、民生、基建、治安与环境，会消耗财政与精力。"
    ),
    TutorialStep(
        "事件需要拍板",
        "每个季度都会遇到需要决定的事。三个选项没有绝对对错，但会带来不同的长期后果。"
    ),
    TutorialStep(
        "人物与势力",
        "在「我的 · 关系」里拜访、请教、汇报，会影响领导信任与势力评价，而关系会影响晋升。"
    ),
    TutorialStep(
        "年度考核",
        "每四个季度考核一次，结果是晋升的主要依据。政绩、民望与风险都要兼顾。"
    ),
    TutorialStep(
        "生涯与履历",
        "「生涯」里能看到个人成长、家庭、资产、完整履历、成就与结局图鉴。"
    ),
    TutorialStep(
        "结局由长期选择决定",
        "没有唯一正确的玩法。每一次重大选择都会写进履历，最终形成属于你的结局。"
    )
)

@Composable
fun TutorialOverlay(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val scheme = MaterialTheme.colorScheme
    val current = TUTORIAL_STEPS[step]
    val last = step == TUTORIAL_STEPS.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // 引导期间拦住底下的点击，避免误触到页面按钮。
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AppearBox(key = step, modifier = Modifier.fillMaxWidth()) {
            ShituCard {
                Text(
                    text = "新手引导 ${step + 1} / ${TUTORIAL_STEPS.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = current.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onFinish, modifier = Modifier.weight(1f)) {
                        Text("跳过", color = scheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { if (last) onFinish() else step += 1 },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(if (last) "开始仕途" else "下一步", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
