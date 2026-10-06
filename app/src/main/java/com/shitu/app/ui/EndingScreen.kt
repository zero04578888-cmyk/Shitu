package com.shitu.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shitu.app.model.Ending
import com.shitu.app.ui.components.AppearBox
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituBackground

// 结局页：根据整局数据计算出的最终结果。
@Composable
fun EndingScreen(
    ending: Ending,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    galleryProgress: String = "",
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var leaving by remember(ending.id) { mutableStateOf(false) }
    ShituBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            AppearBox(key = ending.id, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "结局",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ending.title,
                        style = MaterialTheme.typography.displayLarge,
                        color = scheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = scheme.secondaryContainer
                    ) {
                        Text(
                            text = if (ending.tier.isNotEmpty()) {
                                "${ending.category} · ${ending.tier}"
                            } else {
                                ending.category
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = ending.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionCard(title = "人生总结") {
                ending.highlights.forEach { (label, value) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (galleryProgress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = galleryProgress,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            if (ending.keyChoices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(title = "影响你一生的几个选择") {
                    ending.keyChoices.forEach { line ->
                        Text(
                            text = "· $line",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (ending.keyNodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(title = "关键履历") {
                    ending.keyNodes.forEach { line ->
                        Text(
                            text = "· $line",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (ending.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                SectionCard(title = "结局是怎么形成的") {
                    ending.reasons.forEach { line ->
                        Text(
                            text = "· $line",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (!leaving) {
                        leaving = true
                        onRestart()
                    }
                },
                enabled = !leaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("开始新人生", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("回到首页")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
