package com.mf650.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MobileFriendly
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.ui.theme.AccentCyan
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.theme.StatusDanger

data class ShortcutItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val targetRoute: String
)

@Composable
fun AdvancedControlGrid(
    isAdvancedOnline: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts = listOf(
        ShortcutItem("锁频", "LTE/NR频段", Icons.Outlined.Lock, "lock_band"),
        ShortcutItem("锁小区", "PCI/ARFCN", Icons.Outlined.MobileFriendly, "lock_cell"),
        ShortcutItem("SIM管理", "卡槽切换", Icons.Outlined.SimCard, "sim"),
        ShortcutItem("直供电", "保护模式", Icons.Outlined.BatteryChargingFull, "battery"),
        ShortcutItem("流量守护", "超额断网", Icons.Outlined.DataUsage, "traffic"),
        ShortcutItem("短信转发", "Webhook", Icons.Outlined.Send, "forward"),
        ShortcutItem("定时任务", "Cron守护", Icons.Outlined.Schedule, "cron"),
        ShortcutItem("屏幕显示", "LCD/壁纸", Icons.Outlined.Tv, "lcd")
    )

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = GlassTokens.RadiusLarge
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "高级控制中心",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Port 8081",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAdvancedOnline) AccentCyan else StatusDanger
                    )
                }

                if (!isAdvancedOnline) {
                    Text(
                        text = "高级服务离线",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isAdvancedOnline) {
                Text(
                    text = "高级后台服务当前离线，已自动禁用底层基带控制；基础路由管理仍可正常使用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // 4x2 Grid of shortcuts
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0..3) {
                            val item = shortcuts[row * 4 + col]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(GlassTokens.RadiusSmall))
                                    .background(
                                        if (isAdvancedOnline)
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    )
                                    .then(
                                        if (isAdvancedOnline) Modifier.clickable { onNavigate(item.targetRoute) }
                                        else Modifier
                                    )
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = if (isAdvancedOnline) AccentCyan else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isAdvancedOnline) MaterialTheme.colorScheme.onSurface else Color.Gray
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 9.sp,
                                        color = if (isAdvancedOnline) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
