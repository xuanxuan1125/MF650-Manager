package com.mf650.manager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.data.model.AppState
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.theme.StatusDanger
import com.mf650.manager.ui.theme.StatusSuccess
import com.mf650.manager.ui.theme.StatusWarning

@Composable
fun BackendHealthBadge(
    appState: AppState,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Port 80 Dot
            HealthDot(label = "80", isOnline = appState.padavanOnline)
            // Port 8081 Dot
            HealthDot(label = "8081", isOnline = appState.advancedOnline)
            // Cellular Dot
            HealthDot(label = "Cell", isOnline = appState.cellularConnected)
        }

        AnimatedVisibility(visible = expanded) {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                cornerRadius = GlassTokens.RadiusSmall
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "后台服务连通性诊断",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    ServiceRow("Port 80 Padavan 基础后台", appState.padavanOnline)
                    ServiceRow("Port 8081 飞流高级定制后台", appState.advancedOnline)
                    ServiceRow("蜂窝网络调制解调器连接", appState.cellularConnected)
                    Text(
                        text = "当前活动网关: ${appState.activeIp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthDot(label: String, isOnline: Boolean) {
    val dotColor = if (isOnline) StatusSuccess else StatusDanger
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ServiceRow(name: String, isOnline: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodySmall)
        Text(
            text = if (isOnline) "● 运行正常" else "○ 连接中断",
            style = MaterialTheme.typography.labelSmall,
            color = if (isOnline) StatusSuccess else StatusDanger,
            fontWeight = FontWeight.Bold
        )
    }
}
