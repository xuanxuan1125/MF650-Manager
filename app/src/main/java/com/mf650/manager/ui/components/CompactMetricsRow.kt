package com.mf650.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.data.parser.BatteryState
import com.mf650.manager.data.parser.SignalGrade
import com.mf650.manager.data.parser.SignalQualityEvaluator
import com.mf650.manager.data.parser.UnitNormalizer
import com.mf650.manager.ui.theme.AccentCyan
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.theme.StatusDanger
import com.mf650.manager.ui.theme.StatusSuccess
import com.mf650.manager.ui.theme.StatusWarning

@Composable
fun CompactMetricsRow(
    rsrp: Int?,
    sinr: Double?,
    batteryState: BatteryState,
    temperatureStr: String?,
    totalTrafficStr: String?,
    modifier: Modifier = Modifier
) {
    val signalEval = SignalQualityEvaluator.evaluate(rsrp, sinr, null)
    val tempFormatted = UnitNormalizer.formatTemperature(temperatureStr)
    val trafficFormatted = totalTrafficStr ?: "--"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlassTokens.ItemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlassTokens.ItemSpacing)
        ) {
            // Metric 1: Signal
            MetricCard(
                title = "蜂窝信号",
                primaryValue = if (rsrp != null && rsrp != 0) "$rsrp dBm" else "--",
                subtitle = signalEval.grade.label,
                accentColor = when (signalEval.grade) {
                    SignalGrade.EXCELLENT, SignalGrade.GOOD -> StatusSuccess
                    SignalGrade.FAIR -> StatusWarning
                    else -> StatusDanger
                },
                modifier = Modifier.weight(1f)
            )

            // Metric 2: Battery
            MetricCard(
                title = "电池电量",
                primaryValue = batteryState.displayPercent,
                subtitle = if (batteryState.isCharging) "充电中" else "供电中",
                accentColor = if (batteryState.isCharging) StatusSuccess else AccentCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlassTokens.ItemSpacing)
        ) {
            // Metric 3: Temperature
            MetricCard(
                title = "设备温度",
                primaryValue = tempFormatted,
                subtitle = "SDX55 核心",
                accentColor = StatusSuccess,
                modifier = Modifier.weight(1f)
            )

            // Metric 4: Traffic
            MetricCard(
                title = "累计流量",
                primaryValue = trafficFormatted,
                subtitle = "本周期",
                accentColor = AccentCyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    primaryValue: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier,
        cornerRadius = GlassTokens.RadiusMedium,
        contentPadding = 12.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = primaryValue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )
        }
    }
}
