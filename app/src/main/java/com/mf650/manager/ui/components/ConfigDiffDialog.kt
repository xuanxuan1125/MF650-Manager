package com.mf650.manager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mf650.manager.ui.theme.RiskR2
import com.mf650.manager.ui.theme.SignalExcellent

data class ConfigDiffItem(
    val label: String,
    val oldValue: String,
    val newValue: String
)

@Composable
fun ConfigDiffDialog(
    title: String,
    diffs: List<ConfigDiffItem>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column {
                Text(
                    text = "即将向路由器写入以下配置变更，请核对：",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                diffs.forEach { item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = item.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row {
                            Text(
                                text = item.oldValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = RiskR2
                            )
                            Text(text = " → ", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = item.newValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = SignalExcellent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确认下发")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("放弃修改")
            }
        }
    )
}
