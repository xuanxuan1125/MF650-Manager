package com.mf650.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.data.parser.SignalGrade
import com.mf650.manager.data.parser.SignalQualityEvaluator
import com.mf650.manager.ui.theme.SignalExcellent
import com.mf650.manager.ui.theme.SignalFair
import com.mf650.manager.ui.theme.SignalGood
import com.mf650.manager.ui.theme.SignalPoor
import com.mf650.manager.ui.theme.SignalUnknown

@Composable
fun SignalStrengthIndicator(
    rsrp: Int?,
    sinr: Double?,
    rsrq: Int? = null,
    networkType: String? = "5G SA",
    modifier: Modifier = Modifier
) {
    val evaluation = SignalQualityEvaluator.evaluate(rsrp, sinr, rsrq, networkType)
    val barColor = when (evaluation.grade) {
        SignalGrade.EXCELLENT -> SignalExcellent
        SignalGrade.GOOD -> SignalGood
        SignalGrade.FAIR -> SignalFair
        SignalGrade.POOR -> SignalPoor
        SignalGrade.UNKNOWN -> SignalUnknown
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Visual signal bars (1 to 5)
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(24.dp)
        ) {
            for (i in 1..5) {
                val isActive = i <= evaluation.bars
                val barHeight = (6 + (i * 3.5)).dp
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isActive) barColor else Color.Gray.copy(alpha = 0.25f))
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = networkType ?: "5G",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = evaluation.grade.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = if (rsrp != null && rsrp != 0) "RSRP: $rsrp dBm | SINR: ${sinr?.let { "$it dB" } ?: "--"}" else "暂无信号",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}
