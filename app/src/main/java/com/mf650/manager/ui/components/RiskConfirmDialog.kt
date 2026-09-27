package com.mf650.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mf650.manager.data.model.RiskLevel
import com.mf650.manager.ui.theme.RiskR0
import com.mf650.manager.ui.theme.RiskR1
import com.mf650.manager.ui.theme.RiskR2
import com.mf650.manager.ui.theme.RiskR3
import com.mf650.manager.ui.theme.RiskR4
import kotlinx.coroutines.delay

@Composable
fun RiskConfirmDialog(
    riskLevel: RiskLevel,
    title: String,
    message: String,
    confirmText: String = "确认执行",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val badgeColor = when (riskLevel) {
        RiskLevel.R0 -> RiskR0
        RiskLevel.R1 -> RiskR1
        RiskLevel.R2 -> RiskR2
        RiskLevel.R3 -> RiskR3
        RiskLevel.R4 -> RiskR4
    }

    val requiresDelay = riskLevel == RiskLevel.R3 || riskLevel == RiskLevel.R4
    var countdown by remember { mutableIntStateOf(if (requiresDelay) 3 else 0) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = riskLevel.name,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (requiresDelay) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "高风险操作：请审慎确认，此操作可能导致断网、重启或数据丢失！",
                        style = MaterialTheme.typography.labelSmall,
                        color = RiskR4,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = countdown == 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (riskLevel >= RiskLevel.R3) RiskR4 else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (countdown > 0) "确认 ($countdown s)" else confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
