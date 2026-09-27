package com.mf650.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.data.model.DeviceInfo
import com.mf650.manager.data.model.SystemStatus
import com.mf650.manager.data.parser.UnitNormalizer
import com.mf650.manager.ui.theme.AccentCyan
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.theme.StatusDanger
import com.mf650.manager.ui.theme.StatusSuccess

@Composable
fun DeviceHealthCard(
    systemStatus: SystemStatus,
    deviceInfo: DeviceInfo,
    isAdvancedOnline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val devSys = deviceInfo.systemStatus
    val cpuStr = UnitNormalizer.formatCpuUsage(devSys?.cpuUsage)
    val ramStr = if (systemStatus.ramTotalKb > 0L) {
        UnitNormalizer.formatRamMb(systemStatus.ramUsedKb, systemStatus.ramTotalKb)
    } else if (devSys?.memoryUsage != null) {
        "${devSys.memoryUsage}%"
    } else {
        "--"
    }
    val uptimeStr = devSys?.uptime?.ifEmpty { null }
        ?: "${systemStatus.uptimeDays}天 ${systemStatus.uptimeHours}时${systemStatus.uptimeMinutes}分"

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = GlassTokens.RadiusLarge,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "系统与设备健康",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "详情 ›",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthMetricItem("CPU 利用率", cpuStr)
                HealthMetricItem("内存负载", ramStr)
                HealthMetricItem("连续运行", uptimeStr)
                HealthMetricItem(
                    label = "高级后台",
                    value = if (isAdvancedOnline) "在线" else "离线",
                    isOnlineState = isAdvancedOnline
                )
            }
        }
    }
}

@Composable
private fun HealthMetricItem(
    label: String,
    value: String,
    isOnlineState: Boolean? = null
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isOnlineState != null) {
                if (isOnlineState) StatusSuccess else StatusDanger
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
