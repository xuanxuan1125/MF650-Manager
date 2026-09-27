package com.mf650.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.mf650.manager.data.parser.BatteryState
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.theme.StatusDanger
import com.mf650.manager.ui.theme.StatusSuccess
import com.mf650.manager.ui.theme.StatusWarning

@Composable
fun BatteryCapsule(
    batteryState: BatteryState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val pct = batteryState.percent
    val levelColor = when {
        pct == null -> Color.Gray
        pct <= 10 -> StatusDanger
        pct <= 25 -> StatusWarning
        else -> StatusSuccess
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(GlassTokens.RadiusPill))
            .background(levelColor.copy(alpha = 0.15f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (batteryState.isCharging) "⚡" else "🔋",
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = batteryState.displayPercent,
            color = levelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
