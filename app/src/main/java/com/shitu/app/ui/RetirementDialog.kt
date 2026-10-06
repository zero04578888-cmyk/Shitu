package com.shitu.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 65 岁【人生节点】弹窗。
 *
 * 只有两个选项：退休 / 退居二线。点击外部不会关闭，玩家必须做出选择。
 * 低于副国级的玩家达到 65 岁后一定会看到它；达到副国级及以上者不会触发。
 */
@Composable
fun RetirementDialog(
    positionTitle: String,
    age: Int,
    onChoice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = scheme.surface,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "人生节点",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "你已达到 $age 岁。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "按照当前的仕途状态，你需要做出选择。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "当前职位",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Text(
                    text = positionTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                RetirementOption(
                    title = "退休",
                    detail = "办理退休手续，结束在一线的仕途，进入人生总结。",
                    onClick = { onChoice("RETIREMENT") }
                )
                Spacer(modifier = Modifier.height(10.dp))
                RetirementOption(
                    title = "退居二线",
                    detail = "不再参与职务晋升，但可以继续处理有限的事务与生活。",
                    onClick = { onChoice("SECOND_LINE") }
                )
            }
        }
    }
}

@Composable
private fun RetirementOption(
    title: String,
    detail: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}