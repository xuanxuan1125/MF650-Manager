package com.mf650.manager.ui.screens.cellular

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mf650.manager.data.model.RiskLevel
import com.mf650.manager.data.parser.UnitNormalizer
import com.mf650.manager.ui.components.AmbientBackground
import com.mf650.manager.ui.components.LiquidGlassCard
import com.mf650.manager.ui.components.RiskConfirmDialog
import com.mf650.manager.ui.components.StatusBadge
import com.mf650.manager.ui.theme.AccentCyan
import com.mf650.manager.ui.theme.GlassTokens
import com.mf650.manager.ui.theme.StatusSuccess
import com.mf650.manager.ui.theme.StatusWarning
import com.mf650.manager.ui.viewmodel.CellularViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CellularScreen(
    viewModel: CellularViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val cell = uiState.cellularStatus
    val lte = cell.lte
    val nr = cell.nr
    val bands = uiState.bandConfig

    // 0 = 简洁模式, 1 = 专业模式
    var displayMode by remember { mutableIntStateOf(0) }

    var showLockDialog by remember { mutableStateOf(false) }
    var lockRat by remember { mutableStateOf("LTE") } // LTE or NR
    var targetArfcn by remember { mutableStateOf("") }
    var targetPci by remember { mutableStateOf("") }

    AmbientBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GlassTokens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(GlassTokens.SectionSpacing)
        ) {
            // Header Row: Title & Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "蜂窝与射频基站",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "5G NR / LTE 双模射频调度",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }

                // Segmented control (简洁 / 专业)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GlassTokens.RadiusPill))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    SegmentButton(label = "简洁", isSelected = displayMode == 0) { displayMode = 0 }
                    SegmentButton(label = "专业", isSelected = displayMode == 1) { displayMode = 1 }
                }
            }

            // 1. Cellular Hero Glass Card
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = GlassTokens.RadiusLarge
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "主服务小区 (Serving Cell)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatusBadge(
                            text = cell.network ?: "5G",
                            isActive = cell.status == "success" || cell.status == 1
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("网络制式", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(cell.network ?: "--", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                        }
                        Column {
                            Text("当前频段", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(cell.band?.ifEmpty { null } ?: nr?.band?.ifEmpty { null } ?: lte?.band?.ifEmpty { null } ?: "--", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("PCI / ARFCN", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("${nr?.pci ?: lte?.pci ?: "--"} / ${nr?.nrarfcn ?: lte?.earfcn ?: "--"}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("RSRP 功率", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(UnitNormalizer.formatRsrp(nr?.rsrp ?: lte?.rsrp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }
                        Column {
                            Text("SINR 信噪比", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(UnitNormalizer.formatSinr(nr?.sinr ?: lte?.sinr), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                        }
                        Column {
                            Text("RSRQ 质量", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(UnitNormalizer.formatRsrq(nr?.rsrq ?: lte?.rsrq), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Pro Mode: Extended Engineering Metrics & Neighbor Cells
            if (displayMode == 1) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = GlassTokens.RadiusMedium
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "专业工程参数 (RF Engineering Telemetry)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TAC 跟踪区编码: ${lte?.tac ?: "--"}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            Text("RSSI 场强: ${lte?.rssi ?: "--"} dBm", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "已探测邻区列表 (${cell.lteNeighbors.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )

                        if (cell.lteNeighbors.isEmpty()) {
                            Text("暂无邻区载波信息", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            cell.lteNeighbors.forEach { neighbor ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("PCI: ${neighbor.pci ?: "--"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("EARFCN: ${neighbor.earfcn ?: "--"}", fontSize = 12.sp)
                                    Text("RSRP: ${neighbor.rsrp ?: "--"} dBm", fontSize = 12.sp, color = StatusSuccess)
                                    Text("RSRQ: ${neighbor.rsrq ?: "--"} dB", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Action Card 1: 锁频 / 锁小区 (Cell Locking)
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = GlassTokens.RadiusLarge
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "小区频点与物理小区锁定 (Cell Locking)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "LTE 当前状态: ${if (cell.isLteLocked) "已锁定" else "自由搜网"} | 5G NR 当前状态: ${if (cell.isNrLocked) "已锁定" else "自由搜网"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                lockRat = "LTE"
                                targetArfcn = lte?.earfcn?.toString() ?: ""
                                targetPci = lte?.pci?.toString() ?: ""
                                showLockDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("锁定 LTE 小区")
                        }
                        Button(
                            onClick = {
                                lockRat = "NR"
                                targetArfcn = nr?.nrarfcn?.toString() ?: ""
                                targetPci = nr?.pci?.toString() ?: ""
                                showLockDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("锁定 5G NR 小区")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.unlockLte() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("解除 LTE 锁定")
                        }
                        OutlinedButton(
                            onClick = { viewModel.unlockNr() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("解除 5G 锁定")
                        }
                    }
                }
            }

            // 4. Action Card 2: 5G NR 支持频段 (Band Configuration)
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = GlassTokens.RadiusLarge
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "5G NR 与 LTE 频段选择",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "高通 SDX55 基带支持频段列表 (SA / NSA / LTE)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Text("5G SA 独立组网频段:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val saList = if (bands.saBands.isNotEmpty()) bands.saBands else listOf("n1", "n28", "n41", "n78")
                        saList.forEach { b ->
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(b) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentCyan
                                )
                            )
                        }
                    }

                    Text("4G LTE 核心频段:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val lteList = if (bands.lteBands.isNotEmpty()) bands.lteBands else listOf("B1", "B3", "B5", "B8", "B34", "B38", "B39", "B40", "B41")
                        lteList.forEach { b ->
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(b) }
                            )
                        }
                    }
                }
            }

            uiState.actionMessage?.let {
                Text(it, color = StatusSuccess, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLockDialog) {
        RiskConfirmDialog(
            riskLevel = RiskLevel.R2,
            title = "锁定 $lockRat 小区",
            message = "请输入目标 ARFCN 绝对频点与 PCI 物理小区编号。若输入不存在的基站参数，设备将短暂脱网无法连接，需点击解除锁定恢复自由搜网。",
            confirmText = "确认锁定",
            onConfirm = {
                showLockDialog = false
                if (lockRat == "LTE") {
                    viewModel.lockLteCell(targetArfcn, targetPci)
                } else {
                    viewModel.lockNrCell(targetArfcn, targetPci)
                }
            },
            onDismiss = { showLockDialog = false }
        )
    }
}

@Composable
private fun SegmentButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassTokens.RadiusPill))
            .background(if (isSelected) AccentCyan else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
