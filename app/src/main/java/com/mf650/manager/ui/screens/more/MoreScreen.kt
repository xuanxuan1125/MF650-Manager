package com.mf650.manager.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.ForwardToInbox
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.ui.components.AmbientBackground
import com.mf650.manager.ui.components.LiquidGlassCard
import com.mf650.manager.ui.theme.CyanPrimary
import com.mf650.manager.ui.theme.RiskR4
import com.mf650.manager.ui.viewmodel.MoreViewModel

enum class MoreSubRoute(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val category: MoreCategory
) {
    // 1. 硬件与电源
    BATTERY("供电与电池控制", "直供电、智能停充保护与库仑计校准", Icons.Default.BatteryChargingFull, MoreCategory.POWER),
    LCD_WALLPAPER("LCD 屏幕与待机壁纸", "息屏节能模式与机身彩屏壁纸切换", Icons.Default.DisplaySettings, MoreCategory.POWER),

    // 2. 网络与通信
    MODEM("蜂窝网络与 APN", "首选模式(5G/4G)、APN 与漫游配置", Icons.Default.WifiTethering, MoreCategory.NETWORK),
    SIM("SIM 卡管理与切换", "物理 SIM1 与内置贴片 eSIM 快速切换", Icons.Default.SimCard, MoreCategory.NETWORK),
    LAN_DHCP("局域网与 DHCP", "网关 IP 与动态分配地址池", Icons.Default.Lan, MoreCategory.NETWORK),
    FIREWALL("安全防火墙", "SPI 状态检测、Ping 响应与 DMZ 主机", Icons.Default.Security, MoreCategory.NETWORK),

    // 3. 自动化与定时
    CRON("定时调度任务 (Cron)", "定时重启、定时搜网与执行脚本", Icons.Default.Schedule, MoreCategory.AUTOMATION),
    SMS_FORWARD("短信自动化转发", "推送至 Bark、Telegram Bot 与 Webhook", Icons.Default.ForwardToInbox, MoreCategory.AUTOMATION),

    // 4. 系统维护与监控
    SYSTEM_LOGS("系统运行日志", "Padavan 内核与系统服务运行日志", Icons.Default.TextSnippet, MoreCategory.SYSTEM),
    MAINTENANCE("系统维护与重启", "Linux 内核热重启与状态重置", Icons.Default.RestartAlt, MoreCategory.SYSTEM),

    // 5. 高级玩家与底层协议
    AT_TERMINAL("高通 AT 指令终端 (R4)", "直接向 SDX55 发送裸 AT 控制指令", Icons.Default.Terminal, MoreCategory.ADVANCED),
    IMEI("IMEI 备份与写入 (R4)", "基带 NVRAM 串号备份与合规维护", Icons.Default.Warning, MoreCategory.ADVANCED),
    TTYD_TERMINAL("ttyd 远程网页终端", "Port 7689 Linux 交互式 Shell", Icons.Default.Computer, MoreCategory.ADVANCED),

    // 6. 关于与版本
    CHANGELOG("版本更新日志", "v0.2.0 重构记录与历史说明", Icons.Default.History, MoreCategory.ABOUT)
}

enum class MoreCategory(val title: String) {
    POWER("硬件与电源控制"),
    NETWORK("网络与通信设置"),
    AUTOMATION("自动化与定时任务"),
    SYSTEM("系统监控与维护"),
    ADVANCED("高级玩家与底层协议 (R4)"),
    ABOUT("关于与版本")
}

@Composable
fun MoreScreen(
    viewModel: MoreViewModel,
    routerIp: String = "192.168.100.1",
    initialSubRoute: MoreSubRoute? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeSubRoute by remember { mutableStateOf<MoreSubRoute?>(initialSubRoute) }

    if (activeSubRoute != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sub-screen header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activeSubRoute = null },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = activeSubRoute!!.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = activeSubRoute!!.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (activeSubRoute!!) {
                    MoreSubRoute.BATTERY -> BatterySubScreen(viewModel)
                    MoreSubRoute.LCD_WALLPAPER -> LcdWallpaperSubScreen(viewModel)
                    MoreSubRoute.MODEM -> ModemSubScreen(viewModel)
                    MoreSubRoute.SIM -> SimSubScreen(viewModel)
                    MoreSubRoute.LAN_DHCP -> LanDhcpSubScreen(viewModel)
                    MoreSubRoute.FIREWALL -> FirewallSubScreen(viewModel)
                    MoreSubRoute.CRON -> CronSubScreen(viewModel)
                    MoreSubRoute.SMS_FORWARD -> SmsForwardSubScreen(viewModel)
                    MoreSubRoute.SYSTEM_LOGS -> LogViewerSubScreen(viewModel)
                    MoreSubRoute.MAINTENANCE -> MaintenanceSubScreen(viewModel)
                    MoreSubRoute.AT_TERMINAL -> AtTerminalSubScreen(viewModel)
                    MoreSubRoute.IMEI -> ImeiSubScreen(viewModel)
                    MoreSubRoute.TTYD_TERMINAL -> TtydWebViewSubScreen(routerIp)
                    MoreSubRoute.CHANGELOG -> ChangelogScreen(onBack = { activeSubRoute = null })
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "高级功能",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Port 80 Padavan + Port 8081 飞流双引擎全量功能矩阵",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("搜索功能与协议...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Categorized List
            MoreCategory.entries.forEach { category ->
                val categoryRoutes = MoreSubRoute.entries.filter { route ->
                    route.category == category &&
                    (searchQuery.isBlank() ||
                     route.title.contains(searchQuery, ignoreCase = true) ||
                     route.subtitle.contains(searchQuery, ignoreCase = true))
                }

                if (categoryRoutes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (category == MoreCategory.ADVANCED) RiskR4 else CyanPrimary
                        )

                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                categoryRoutes.forEachIndexed { index, route ->
                                    MoreItemRow(
                                        route = route,
                                        onClick = { activeSubRoute = route }
                                    )
                                    if (index < categoryRoutes.size - 1) {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MoreItemRow(
    route: MoreSubRoute,
    onClick: () -> Unit
) {
    val isRisk = route.category == MoreCategory.ADVANCED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background((if (isRisk) RiskR4 else CyanPrimary).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = route.icon,
                contentDescription = null,
                tint = if (isRisk) RiskR4 else CyanPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = route.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = route.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.6f)
        )
    }
}
