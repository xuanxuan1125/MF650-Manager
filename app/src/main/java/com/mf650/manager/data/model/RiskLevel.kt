package com.mf650.manager.data.model

import androidx.compose.ui.graphics.Color

/**
 * Operation risk hierarchy.
 * R0: Read-only safe queries
 * R1: Lightweight non-destructive toggles
 * R2: Radio & network configurations (Wi-Fi, APN, Band, DHCP, Port Forward)
 * R3: System reboot, power actions, cron modifications
 * R4: High-risk NVRAM alteration, IMEI modification, bare AT commands, root shell
 */
enum class RiskLevel(
    val levelName: String,
    val description: String,
    val badgeColor: Color,
    val requiresConfirmation: Boolean,
    val requiresDoubleConfirmation: Boolean = false
) {
    R0("R0 只读", "纯状态读取，无任何系统副作用", Color(0xFF00C853), false),
    R1("R1 轻量控制", "临时软开关，可快速安全恢复", Color(0xFF00B0FF), true),
    R2("R2 配置修改", "影响无线或局域网连接，需要确认变更内容", Color(0xFFFF9100), true),
    R3("R3 系统控制", "整机重启或关机，存在服务中断", Color(0xFFFF3D00), true, true),
    R4("R4 硬件高危", "写入高通基带NVRAM或Root终端，误操作可能破坏设备", Color(0xFFD50000), true, true)
}
