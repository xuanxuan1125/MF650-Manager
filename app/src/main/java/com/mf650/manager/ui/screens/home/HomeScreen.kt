package com.mf650.manager.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mf650.manager.ui.components.AdvancedControlGrid
import com.mf650.manager.ui.components.AmbientBackground
import com.mf650.manager.ui.components.BackendHealthBadge
import com.mf650.manager.ui.components.BatteryCapsule
import com.mf650.manager.ui.components.CompactMetricsRow
import com.mf650.manager.ui.components.DeviceHealthCard
import com.mf650.manager.ui.components.NetworkHeroCard
import com.mf650.manager.ui.components.OfflineBanner
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.viewmodel.DashboardViewModel

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onNavigateShortcut: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val appState by viewModel.appState.collectAsState()

    val sys = uiState.systemStatus
    val dev = uiState.deviceInfo
    val cell = uiState.cellularStatus
    val battery = uiState.batteryState

    AmbientBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            OfflineBanner(isOffline = uiState.isOffline, routerIp = appState.activeIp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = GlassTokens.ScreenPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(GlassTokens.SectionSpacing)
            ) {
                // Top App Bar / Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MF650 Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Qualcomm SDX55 · ${appState.activeIp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BackendHealthBadge(appState = appState)
                        Spacer(modifier = Modifier.width(8.dp))
                        BatteryCapsule(
                            batteryState = battery,
                            onClick = { onNavigateShortcut("battery") }
                        )
                    }
                }

                // 1. Network Hero Card with 60s Speed Graph
                NetworkHeroCard(
                    cellularStatus = cell,
                    deviceInfo = dev,
                    downloadSpeeds = uiState.downloadSpeeds,
                    uploadSpeeds = uiState.uploadSpeeds
                )

                // 2. Compact Four-Metric Cards (Signal, Battery, Temperature, Traffic)
                CompactMetricsRow(
                    rsrp = cell.nr?.rsrpInt ?: cell.lte?.rsrpInt ?: if (sys.rsrp5g != 0) sys.rsrp5g else sys.rsrp4g,
                    sinr = cell.nr?.sinrDouble ?: cell.lte?.sinrDouble,
                    batteryState = battery,
                    temperatureStr = dev.systemStatus?.sdxTemperature ?: dev.systemStatus?.cpuTemperature,
                    totalTrafficStr = dev.trafficStats?.total ?: uiState.trafficStatus.trafficStats?.total
                )

                // 3. Advanced Control Center (8 Prominent Shortcuts)
                AdvancedControlGrid(
                    isAdvancedOnline = appState.advancedOnline,
                    onNavigate = onNavigateShortcut
                )

                // 4. Device Health Card
                DeviceHealthCard(
                    systemStatus = sys,
                    deviceInfo = dev,
                    isAdvancedOnline = appState.advancedOnline,
                    onClick = { onNavigateShortcut("device_info") }
                )

                // 5. Bottom Action Button
                Button(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp).height(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("手动刷新状态")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
