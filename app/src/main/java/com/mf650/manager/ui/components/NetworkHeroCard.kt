package com.mf650.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.data.model.CellularStatus
import com.mf650.manager.data.model.DeviceInfo
import com.mf650.manager.data.parser.UnitNormalizer
import com.mf650.manager.ui.theme.AccentCyan
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.theme.SpeedDownload
import com.mf650.manager.ui.theme.SpeedUpload
import com.mf650.manager.ui.theme.StatusSuccess

@Composable
fun NetworkHeroCard(
    cellularStatus: CellularStatus,
    deviceInfo: DeviceInfo,
    downloadSpeeds: List<Float>,
    uploadSpeeds: List<Float>,
    modifier: Modifier = Modifier
) {
    val cell = cellularStatus
    val nr = cell.nr
    val lte = cell.lte
    val netInfo = deviceInfo.networkInfo
    val cellInfo = deviceInfo.cellularInfo

    val operator = cellInfo?.operator?.ifEmpty { null } ?: "蜂窝网络"
    val networkMode = cell.network ?: netInfo?.band ?: cellInfo?.networkType ?: "5G/4G"
    val band = cell.band?.ifEmpty { null } ?: nr?.band?.ifEmpty { null } ?: lte?.band?.ifEmpty { null } ?: netInfo?.band ?: "--"
    val pci = nr?.pci?.toString() ?: lte?.pci?.toString() ?: "--"
    val rsrpStr = UnitNormalizer.formatRsrp(nr?.rsrp ?: lte?.rsrp)
    val sinrStr = UnitNormalizer.formatSinr(nr?.sinr ?: lte?.sinr)

    val currentDownKb = downloadSpeeds.lastOrNull() ?: 0f
    val currentUpKb = uploadSpeeds.lastOrNull() ?: 0f

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = GlassTokens.RadiusLarge
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header Row: Operator & Network Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = operator,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$networkMode · Band $band · PCI $pci",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }

                StatusBadge(
                    text = networkMode,
                    isActive = cell.status == "success" || cell.status == 1
                )
            }

            // Signal Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(label = "RSRP 功率", value = rsrpStr, accentColor = StatusSuccess)
                MetricColumn(label = "SINR 信噪比", value = sinrStr, accentColor = AccentCyan)
                MetricColumn(label = "实时下载", value = "↓ ${UnitNormalizer.formatSpeed(currentDownKb)}", accentColor = SpeedDownload)
                MetricColumn(label = "实时上传", value = "↑ ${UnitNormalizer.formatSpeed(currentUpKb)}", accentColor = SpeedUpload)
            }

            // Real-time Canvas Graph
            SpeedCanvasChart(
                downloadSpeeds = downloadSpeeds,
                uploadSpeeds = uploadSpeeds,
                heightDp = 110
            )
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, accentColor: Color) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}
