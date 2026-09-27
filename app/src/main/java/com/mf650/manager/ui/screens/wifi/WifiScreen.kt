package com.mf650.manager.ui.screens.wifi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.data.model.RiskLevel
import com.mf650.manager.ui.components.AmbientBackground
import com.mf650.manager.ui.components.LiquidGlassCard
import com.mf650.manager.ui.components.RiskConfirmDialog
import com.mf650.manager.ui.theme.CyanPrimary
import com.mf650.manager.ui.theme.RiskR2
import com.mf650.manager.ui.theme.SignalExcellent
import com.mf650.manager.ui.viewmodel.WifiViewModel

@Composable
fun WifiScreen(
    viewModel: WifiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = 2.4G, 1 = 5G
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val config24 = uiState.wifi24Config
    val config5 = uiState.wifi5Config

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AmbientBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // Restarting Countdown Banner
            AnimatedVisibility(
                visible = uiState.isRestarting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RiskR2)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "无线射频服务正在重启...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${uiState.restartCountdown}s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (15 - uiState.restartCountdown) / 15f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Wi-Fi 管理",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "双频无线网络与射频参数配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // Active band badge
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "802.11ax/ac",
                            color = CyanPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tabs for 2.4G and 5G with Liquid Glass style
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyanPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (config24.enabled) SignalExcellent else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "2.4 GHz 频段",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (config5.enabled) SignalExcellent else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "5 GHz 高速频段",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    // 2.4G Configuration Section
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("启用 2.4 GHz 无线网络", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("穿透力强，兼容旧设备与 IoT", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = config24.enabled,
                                    onCheckedChange = { viewModel.updateWifi24(config24.copy(enabled = it)) }
                                )
                            }

                            OutlinedTextField(
                                value = config24.ssid,
                                onValueChange = { viewModel.updateWifi24(config24.copy(ssid = it)) },
                                label = { Text("无线网络名称 (SSID)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = config24.password,
                                onValueChange = { viewModel.updateWifi24(config24.copy(password = it)) },
                                label = { Text("无线接入密码 (WPA2-PSK)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "切换密码可见性"
                                        )
                                    }
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("隐藏网络 (不广播 SSID)", style = MaterialTheme.typography.bodyMedium)
                                    Text("连接时需手动输入网络名称", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = config24.hideSsid,
                                    onCheckedChange = { viewModel.updateWifi24(config24.copy(hideSsid = it)) }
                                )
                            }

                            Button(
                                onClick = { showConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                            ) {
                                Text("保存 2.4G 设置并生效", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // RF Parameters Card
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("射频参数与特性", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("信道带宽", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Text("20/40 MHz 自动", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("加密模式", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Text("WPA2-Personal (AES)", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("最大连接数", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Text("32 终端", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    // 5G Configuration Section
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("启用 5 GHz 高速网络", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("高速率低干扰，适合影音游戏", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = config5.enabled,
                                    onCheckedChange = { viewModel.updateWifi5(config5.copy(enabled = it)) }
                                )
                            }

                            OutlinedTextField(
                                value = config5.ssid,
                                onValueChange = { viewModel.updateWifi5(config5.copy(ssid = it)) },
                                label = { Text("5G 网络名称 (SSID)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = config5.password,
                                onValueChange = { viewModel.updateWifi5(config5.copy(password = it)) },
                                label = { Text("5G 接入密码 (WPA2/WPA3)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "切换密码可见性"
                                        )
                                    }
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("隐藏 5G SSID", style = MaterialTheme.typography.bodyMedium)
                                    Text("隐藏广播增强隐私安全", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Switch(
                                    checked = config5.hideSsid,
                                    onCheckedChange = { viewModel.updateWifi5(config5.copy(hideSsid = it)) }
                                )
                            }

                            Button(
                                onClick = { showConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                            ) {
                                Text("保存 5G 设置并生效", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 5G RF Parameters Card
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("5G 射频参数与特性", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("频宽支持", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Text("80 MHz 高速带宽", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("理论最高速率", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Text("1201 Mbps (Wi-Fi 6)", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("加密模式", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Text("WPA2/WPA3-Personal Mixed", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                uiState.actionSuccessMessage?.let {
                    Text(
                        text = "✓ $it",
                        color = SignalExcellent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showConfirmDialog) {
        val targetBand = if (selectedTab == 0) "2.4 GHz" else "5 GHz"
        RiskConfirmDialog(
            riskLevel = RiskLevel.R2,
            title = "保存 $targetBand Wi-Fi 设置",
            message = "保存 Wi-Fi 参数会触发路由器重启无线驱动 (/sbin/restart_wifis)，所有连接中的无线终端将短暂断开约 15 秒并重新连接。",
            confirmText = "确认保存并重启 Wi-Fi",
            onConfirm = {
                showConfirmDialog = false
                if (selectedTab == 0) viewModel.saveWifi24() else viewModel.saveWifi5()
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}
