package com.shitu.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shitu.app.ui.components.ShituBackground
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

/**
 * 首页（主菜单）。
 *
 * 第十五阶段：
 * - 没有存档时只显示「开始仕途」；
 * - 存在存档时显示「继续仕途」与「重新开始」，避免玩家在已有进度时误开新局。
 */
@Composable
fun StartScreen(
    hasSave: Boolean,
    saveSummary: String,
    galleryProgress: String,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onArchive: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    ShituBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "仕途",
                style = MaterialTheme.typography.displayLarge,
                color = scheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "公务员职业生涯模拟器",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "从基层起步，走过三十年的选择",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(36.dp))

            if (hasSave) {
                // 有存档：主按钮「继续仕途」，次按钮「重新开始」。
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("继续仕途", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.secondary)
                ) {
                    Text("重新开始", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                // 没有存档：只显示「开始仕途」。
                Button(
                    onClick = onNewGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("开始仕途", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row2(
                left = "人生档案",
                right = "设置",
                onLeft = onArchive,
                onRight = onSettings
            )

            if (galleryProgress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = galleryProgress,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }

            if (hasSave && saveSummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                ShituCard {
                    Text(
                        text = "最近存档",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = saveSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            ThinDivider(modifier = Modifier.padding(horizontal = 40.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "本作是虚构的模拟游戏，规则与数值仅用于玩法设计",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun Row2(
    left: String,
    right: String,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onLeft,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(left, fontSize = 15.sp)
        }
        OutlinedButton(
            onClick = onRight,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(right, fontSize = 15.sp)
        }
    }
}
