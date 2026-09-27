package com.mf650.manager.ui.screens.more

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mf650.manager.data.model.RiskLevel
import com.mf650.manager.ui.components.AmbientBackground
import com.mf650.manager.ui.components.LiquidGlassCard
import com.mf650.manager.ui.components.RiskConfirmDialog
import com.mf650.manager.ui.theme.CyanPrimary
import com.mf650.manager.ui.theme.RiskR2
import com.mf650.manager.ui.theme.RiskR3
import com.mf650.manager.ui.theme.RiskR4
import com.mf650.manager.ui.theme.SignalExcellent
import com.mf650.manager.ui.viewmodel.MoreViewModel

@Composable
fun BatterySubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showCalibrateConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("供电与电池控制", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("硬件充放电控制 (IP5332 PMIC)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "支持直供电模式（电池旁路）。插电长久使用推荐开启直供电，防止电池鼓包老化。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setChargeMode(0) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("直供电 (停充)")
                        }
                        Button(
                            onClick = { viewModel.setChargeMode(1) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("恢复充电")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("智能充放电保护", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("达到预设阈值自动停充/复充", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = true,
                            onCheckedChange = { viewModel.setAutoCharge(it) }
                        )
                    }
                }
            }

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("库仑计与电池校准", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "当电量显示异常跳跃或与实际使用时长不符时，执行库仑计校准。校准期间请勿拔掉供电源。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Button(
                        onClick = { showCalibrateConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("触发库仑计校准")
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCalibrateConfirm) {
        RiskConfirmDialog(
            riskLevel = RiskLevel.R1,
            title = "触发库仑计校准",
            message = "校准过程将重置硬件电量统计芯片寄存器，期间需要经历一个完整充放电循环以重新标定满充容量。",
            confirmText = "开始校准",
            onConfirm = {
                showCalibrateConfirm = false
                viewModel.triggerBatteryCalibration()
            },
            onDismiss = { showCalibrateConfirm = false }
        )
    }
}

@Composable
fun ModemSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.modemConfig
    var showConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("调制解调器与 APN", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = config.apn,
                        onValueChange = { viewModel.updateModemConfig(config.copy(apn = it)) },
                        label = { Text("蜂窝接入点 (APN)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = config.ratMode,
                        onValueChange = { viewModel.updateModemConfig(config.copy(ratMode = it)) },
                        label = { Text("网络首选模式 (AUTO / NR5G / LTE)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("数据漫游开关", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("允许在异地/异网漫游时连接蜂窝网络", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = config.roamingEnabled,
                            onCheckedChange = { viewModel.updateModemConfig(config.copy(roamingEnabled = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("飞行模式", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("关闭高通 SDX55 射频发射 (CFUN=0)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = uiState.isAirplaneMode,
                            onCheckedChange = { viewModel.toggleAirplaneMode() }
                        )
                    }

                    Button(
                        onClick = { showConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("保存蜂窝网络设置", fontWeight = FontWeight.Bold)
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showConfirm) {
        RiskConfirmDialog(
            riskLevel = RiskLevel.R2,
            title = "保存蜂窝网络配置",
            message = "修改 APN 或网络模式将导致蜂窝调制解调器重新附着注网，网络连接将短暂中断。",
            confirmText = "确认保存",
            onConfirm = {
                showConfirm = false
                viewModel.saveModem()
            },
            onDismiss = { showConfirm = false }
        )
    }
}

@Composable
fun SimSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showSwitchConfirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadSimStatus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("SIM 卡管理与切换", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("当前 SIM 卡状态", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("当前活动卡槽", color = Color.Gray)
                        Text("卡槽 ${uiState.currentSlot} ${if (uiState.currentSlot == "1") "(外部物理 Nano-SIM)" else "(内置贴片 eSIM)"}", fontWeight = FontWeight.Bold, color = CyanPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SIM 状态", color = Color.Gray)
                        Text(uiState.simStatus, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("卡槽快速切换", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showSwitchConfirm = "1" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.currentSlot == "1") CyanPrimary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("切至卡槽 1 (物理卡)")
                        }
                        Button(
                            onClick = { showSwitchConfirm = "2" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.currentSlot == "2") CyanPrimary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("切至卡槽 2 (eSIM)")
                        }
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    showSwitchConfirm?.let { targetSlot ->
        RiskConfirmDialog(
            riskLevel = RiskLevel.R2,
            title = "切换 SIM 卡槽至 $targetSlot",
            message = "切换卡槽会通过高通 GPIO 重置 UIM 接口并重新搜网，网络连接将中断约 10-15 秒。",
            confirmText = "确认切换",
            onConfirm = {
                val slot = targetSlot
                showSwitchConfirm = null
                viewModel.switchSim(slot)
            },
            onDismiss = { showSwitchConfirm = null }
        )
    }
}

@Composable
fun LanDhcpSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.lanConfig

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("局域网与 DHCP", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = config.lanIp,
                        onValueChange = { viewModel.updateLanConfig(config.copy(lanIp = it)) },
                        label = { Text("路由器网关 IP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = config.poolStart,
                        onValueChange = { viewModel.updateLanConfig(config.copy(poolStart = it)) },
                        label = { Text("DHCP 起始分配 IP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = config.poolEnd,
                        onValueChange = { viewModel.updateLanConfig(config.copy(poolEnd = it)) },
                        label = { Text("DHCP 结束分配 IP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.saveLanDhcp() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("应用局域网与 DHCP 设置", fontWeight = FontWeight.Bold)
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FirewallSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.firewallConfig

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("安全防火墙", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SPI 状态检测防火墙", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = config.spiEnabled,
                            onCheckedChange = { viewModel.updateFirewallConfig(config.copy(spiEnabled = it)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("响应 WAN 口 ICMP Ping", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = config.respondPing,
                            onCheckedChange = { viewModel.updateFirewallConfig(config.copy(respondPing = it)) }
                        )
                    }
                    OutlinedTextField(
                        value = config.dmzIp,
                        onValueChange = { viewModel.updateFirewallConfig(config.copy(dmzIp = it)) },
                        label = { Text("DMZ 主机局域网 IP") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.saveFirewall() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("下发防火墙规则", fontWeight = FontWeight.Bold)
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CronSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var cronExpr by remember { mutableStateOf("0 4 * * *") }
    var cronCmd by remember { mutableStateOf("/sbin/reboot") }

    LaunchedEffect(Unit) {
        viewModel.loadCronList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("自动化与定时任务 (Cron)", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("新建定时调度任务", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = cronExpr,
                        onValueChange = { cronExpr = it },
                        label = { Text("Cron 表达式 (例: 0 4 * * * 每天凌晨4点)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cronCmd,
                        onValueChange = { cronCmd = it },
                        label = { Text("调度命令 (例: /sbin/reboot)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                cronExpr = "0 4 * * *"
                                cronCmd = "/sbin/reboot"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("预设: 凌晨重启", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                cronExpr = "0 3 * * *"
                                cronCmd = "echo 1 > /tmp/reconnect_wan"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("预设: 重新搜网", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.saveCronTask("add", cronExpr, cronCmd) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("保存并注入 Crontab", fontWeight = FontWeight.Bold)
                    }
                }
            }

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前系统 Crontab 清单", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = uiState.cronList.ifEmpty { "暂无自定义任务" },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SmsForwardSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var tokenInput by remember { mutableStateOf(uiState.forwardToken) }
    var channelInput by remember { mutableStateOf(uiState.forwardChannel) }
    var enabledInput by remember { mutableStateOf(uiState.forwardEnabled) }

    LaunchedEffect(Unit) {
        viewModel.loadForwardConfig()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("短信自动化转发", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启用短信转发", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("收到验证码或短信时自动推送到手机", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = enabledInput,
                            onCheckedChange = { enabledInput = it }
                        )
                    }

                    OutlinedTextField(
                        value = channelInput,
                        onValueChange = { channelInput = it },
                        label = { Text("转发渠道 (Bark / Telegram / Webhook)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("渠道 Token / 推送密钥") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { viewModel.saveForwardConfig(tokenInput, channelInput, enabledInput) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("保存短信转发配置", fontWeight = FontWeight.Bold)
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LcdWallpaperSubScreen(viewModel: MoreViewModel) {
    var lcdOn by remember { mutableStateOf(true) }
    var selectedWallpaper by remember { mutableStateOf(1) }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("LCD 屏幕与壁纸设置", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("机身 LCD 屏显示", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("关闭屏幕可显著降低整机功耗", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = lcdOn,
                            onCheckedChange = { lcdOn = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("选择 LCD 待机主题壁纸", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedWallpaper = 1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedWallpaper == 1) CyanPrimary else MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("极简数字")
                        }
                        Button(
                            onClick = { selectedWallpaper = 2 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedWallpaper == 2) CyanPrimary else MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("机甲仪表")
                        }
                        Button(
                            onClick = { selectedWallpaper = 3 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedWallpaper == 3) CyanPrimary else MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("波浪动感")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImeiSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showImeiConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadImeiInfo()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RiskR4)
                Spacer(modifier = Modifier.width(8.dp))
                Text("IMEI 备份与维护 (R4 极高危)", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = RiskR4)
            }

            // Compliance Notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RiskR4.copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "合规声明：修改 IMEI 属于深度底层操作，受有关法律法规约束，严禁用于伪造入网许可或非法网络活动。写入错误串号会导致高通 SDX55 基带 NVRAM 损坏、永久锁死或无服务！",
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("当前基带 IMEI 信息", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = uiState.imeiInfo.ifEmpty { "正在读取 NVRAM..." },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    OutlinedTextField(
                        value = uiState.newImeiInput,
                        onValueChange = { viewModel.updateNewImei(it) },
                        label = { Text("新 IMEI 串号 (15位数字)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = { showImeiConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RiskR4),
                        enabled = uiState.newImeiInput.trim().length == 15
                    ) {
                        Text("写入高通基带 NVRAM", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearImeiHistory() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清除历史修改记录", color = Color.Gray)
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showImeiConfirm) {
        RiskConfirmDialog(
            riskLevel = RiskLevel.R4,
            title = "极高危操作：写入高通基带 IMEI",
            message = "你即将向 SDX55 写入新 IMEI [${uiState.newImeiInput.trim()}]。此操作直接改写基带物理分区，存在变砖风险。请确认自担风险！",
            confirmText = "确认写入 NVRAM",
            onConfirm = {
                showImeiConfirm = false
                viewModel.modifyImei(uiState.newImeiInput.trim())
            },
            onDismiss = { showImeiConfirm = false }
        )
    }
}

@Composable
fun AtTerminalSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAtConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("高通 AT 交互终端 (R4)", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.atCommand,
                    onValueChange = { viewModel.updateAtCommand(it) },
                    label = { Text("AT 指令") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = { showAtConfirm = true },
                    modifier = Modifier.align(Alignment.CenterVertically),
                    colors = ButtonDefaults.buttonColors(containerColor = RiskR4)
                ) {
                    Text("发送")
                }
            }

            // Quick Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.updateAtCommand("AT+CSQ") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+CSQ", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.updateAtCommand("AT+CPIN?") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+CPIN?", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.updateAtCommand("AT+QNWINFO") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+QNWINFO", fontSize = 11.sp)
                }
            }

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("SDX55 终端响应", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.atResponse.ifEmpty { "等待发送指令..." },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = SignalExcellent,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    if (showAtConfirm) {
        RiskConfirmDialog(
            riskLevel = RiskLevel.R4,
            title = "执行裸 AT 指令",
            message = "向高通 SDX55 发送未知 AT 指令可能造成基带锁死或搜网异常！确认执行当前指令 [${uiState.atCommand}] 吗？",
            confirmText = "确认执行 AT",
            onConfirm = {
                showAtConfirm = false
                viewModel.sendAtCommand()
            },
            onDismiss = { showAtConfirm = false }
        )
    }
}

@Composable
fun LogViewerSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLogs()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("系统内核日志", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Button(
                    onClick = { viewModel.loadLogs() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("刷新日志", fontWeight = FontWeight.Bold)
                }
            }

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = uiState.systemLogs,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
fun TtydWebViewSubScreen(routerIp: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl("http://$routerIp:7689/")
                }
            }
        )
    }
}

@Composable
fun MaintenanceSubScreen(viewModel: MoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showRebootConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("系统维护与重启", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("软重启路由器", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("执行 Linux 内核热重启，整个过程约需 30 秒，期间所有网络与无线服务将中断。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Button(
                        onClick = { showRebootConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RiskR3),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("重启路由器", fontWeight = FontWeight.Bold)
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = SignalExcellent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showRebootConfirm) {
        RiskConfirmDialog(
            riskLevel = RiskLevel.R3,
            title = "重启设备",
            message = "即将向路由器发送热重启指令 (/sbin/reboot)，所有 Wi-Fi 连接和蜂窝数据将立即断开！",
            confirmText = "确认重启",
            onConfirm = {
                showRebootConfirm = false
                viewModel.rebootDevice()
            },
            onDismiss = { showRebootConfirm = false }
        )
    }
}
