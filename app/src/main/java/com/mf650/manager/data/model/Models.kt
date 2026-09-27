package com.mf650.manager.data.model

import com.google.gson.annotations.SerializedName

// --- 1. Device Overview ---

data class DeviceInfo(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("device_info") val deviceInfo: DeviceSubInfo? = null,
    @SerializedName("system_status") val systemStatus: SystemSubStatus? = null,
    @SerializedName("network_info") val networkInfo: NetworkSubInfo? = null,
    @SerializedName("cellular_info") val cellularInfo: CellularSubInfo? = null,
    @SerializedName("traffic_stats") val trafficStats: TrafficSubStats? = null
)

data class DeviceSubInfo(
    @SerializedName("name") val name: String? = "MF650",
    @SerializedName("model") val model: String? = "MF650",
    @SerializedName("software_version") val softwareVersion: String? = null,
    @SerializedName("hardware_version") val hardwareVersion: String? = null,
    @SerializedName("serial_number") val serialNumber: String? = null,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("imei") val imei: String? = null,
    @SerializedName("mac") val mac: String? = null,
    @SerializedName("sn") val sn: String? = null
)

data class SystemSubStatus(
    @SerializedName("cpu_usage") val cpuUsage: Any? = null,
    @SerializedName("memory_usage") val memoryUsage: Any? = null,
    @SerializedName("cpu_temperature") val cpuTemperature: String? = null,
    @SerializedName("wifi_temperature") val wifiTemperature: String? = null,
    @SerializedName("sdx_temperature") val sdxTemperature: String? = null,
    @SerializedName("temperature") val temperature: String? = null,
    @SerializedName("uptime") val uptime: String? = null
)

data class NetworkSubInfo(
    @SerializedName("lan_ip") val lanIp: String? = "192.168.100.1",
    @SerializedName("lan_mac") val lanMac: String? = null,
    @SerializedName("ipv4_address") val ipv4Address: String? = null,
    @SerializedName("ipv6_address") val ipv6Address: String? = null,
    @SerializedName("band") val band: String? = null,
    @SerializedName("connected_devices") val connectedDevices: Int? = 0,
    @SerializedName("wifi_24g_status") val wifi24gStatus: Boolean? = true,
    @SerializedName("wifi_5g_status") val wifi5gStatus: Boolean? = true
)

data class CellularSubInfo(
    @SerializedName("network_type") val networkType: String? = null,
    @SerializedName("operator") val operator: String? = null,
    @SerializedName("signal_level") val signalLevel: Int? = 0,
    @SerializedName("sim_status") val simStatus: String? = null,
    @SerializedName("imei") val imei: String? = null,
    @SerializedName("iccid") val iccid: String? = null
)

data class TrafficSubStats(
    @SerializedName("download") val download: String? = null,
    @SerializedName("upload") val upload: String? = null,
    @SerializedName("total") val total: String? = null,
    @SerializedName("today_usage") val todayUsage: String? = null,
    @SerializedName("current_speed") val currentSpeed: SpeedSubData? = null
)

data class SpeedSubData(
    @SerializedName("download") val download: String? = "0.0",
    @SerializedName("upload") val upload: String? = "0.0",
    @SerializedName("realtime_speed") val realtimeSpeed: String? = "0.0"
)

// --- 2. Cellular & Base Station ---

data class CellularStatus(
    @SerializedName("status") val status: Any? = "success",
    @SerializedName("lte_lock") val lteLock: Any? = "unlocked",
    @SerializedName("lte_count") val lteCount: Int = 0,
    @SerializedName("nr_lock") val nrLock: Any? = "unlocked",
    @SerializedName("network") val network: String? = null,
    @SerializedName("lte") val lte: LteCellInfo? = null,
    @SerializedName("nr") val nr: NrCellInfo? = null,
    @SerializedName("band") val band: String? = null,
    @SerializedName("lte_neighbors") val lteNeighbors: List<LteNeighborItem> = emptyList()
) {
    val isLteLocked: Boolean
        get() = lteLock == true || lteLock?.toString()?.equals("locked", ignoreCase = true) == true

    val isNrLocked: Boolean
        get() = nrLock == true || nrLock?.toString()?.equals("locked", ignoreCase = true) == true
}

data class LteCellInfo(
    @SerializedName("earfcn") val earfcn: Any? = null,
    @SerializedName("pci") val pci: Any? = null,
    @SerializedName("rsrp") val rsrp: Any? = null,
    @SerializedName("rsrq") val rsrq: Any? = null,
    @SerializedName("rssi") val rssi: Any? = null,
    @SerializedName("sinr") val sinr: Any? = null,
    @SerializedName("tac") val tac: String? = null,
    @SerializedName("cqi") val cqi: Any? = null,
    @SerializedName("band") val band: String? = null
) {
    val rsrpInt: Int? get() = rsrp?.toString()?.toDoubleOrNull()?.toInt()
    val rsrqInt: Int? get() = rsrq?.toString()?.toDoubleOrNull()?.toInt()
    val sinrDouble: Double? get() = sinr?.toString()?.toDoubleOrNull()
}

data class NrCellInfo(
    @SerializedName("nrarfcn") val nrarfcn: Any? = null,
    @SerializedName("pci") val pci: Any? = null,
    @SerializedName("rsrp") val rsrp: Any? = null,
    @SerializedName("rsrq") val rsrq: Any? = null,
    @SerializedName("sinr") val sinr: Any? = null,
    @SerializedName("band") val band: String? = null
) {
    val rsrpInt: Int? get() = rsrp?.toString()?.toDoubleOrNull()?.toInt()
    val rsrqInt: Int? get() = rsrq?.toString()?.toDoubleOrNull()?.toInt()
    val sinrDouble: Double? get() = sinr?.toString()?.toDoubleOrNull()
}

data class LteNeighborItem(
    @SerializedName("pci") val pci: Any? = null,
    @SerializedName("earfcn") val earfcn: Any? = null,
    @SerializedName("rsrp") val rsrp: Any? = null,
    @SerializedName("rsrq") val rsrq: Any? = null,
    @SerializedName("rssi") val rssi: Any? = null
)

data class SignalStatus(
    val signalStrength4g: Int = 0,
    val signalStrength5g: Int = 0,
    val rsrp4g: Int = 0,
    val rsrq4g: Int = 0,
    val rsrp5g: Int = 0,
    val rsrq5g: Int = 0,
    val cellId: String = "",
    val networkType: String = "",
    val operator: String = ""
)

// --- 3. Battery & Power ---

data class BatteryStatus(
    @SerializedName("status") val status: Any? = "success",
    @SerializedName("battery") val battery: BatterySubData? = null,
    @SerializedName("usb_connection") val usbConnection: String? = null,
    @SerializedName("charging") val charging: ChargingSubData? = null,
    @SerializedName("auto_charge") val autoCharge: AutoChargeSubData? = null,
    @SerializedName("battery_calibration") val batteryCalibration: Map<String, Any>? = null,
    @SerializedName("low_voltage_shutdown") val lowVoltageShutdown: Map<String, Any>? = null
)

data class BatterySubData(
    @SerializedName("level") val level: String? = null,
    @SerializedName("voltage") val voltage: String? = null
)

data class ChargingSubData(
    @SerializedName("enabled") val enabled: Any? = 0,
    @SerializedName("status") val status: String? = null
) {
    val isCharging: Boolean
        get() = (enabled as? Number)?.toInt() == 1 || enabled == true || enabled?.toString()?.startsWith("1") == true
}

data class AutoChargeSubData(
    @SerializedName("enabled") val enabled: Any? = 1,
    @SerializedName("min_level") val minLevel: Int? = 2,
    @SerializedName("max_level") val maxLevel: Int? = 5,
    @SerializedName("controller_status") val controllerStatus: String? = null,
    @SerializedName("controller_running") val controllerRunning: Boolean? = true
) {
    val isAutoChargeActive: Boolean
        get() = (enabled as? Number)?.toInt() == 1 || enabled == true || enabled?.toString()?.startsWith("1") == true
}

// --- 4. Traffic & Usage ---

data class TrafficStatus(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("status") val status: Any? = null,
    @SerializedName("traffic_stats") val trafficStats: TrafficData? = null,
    @SerializedName("traffic_limit") val trafficLimit: Double = 0.0,
    @SerializedName("limit_unit") val limitUnit: String = "GB"
)

data class TrafficData(
    @SerializedName("download") val download: String? = null,
    @SerializedName("upload") val upload: String? = null,
    @SerializedName("total") val total: String? = null,
    @SerializedName("rx_bytes") val rxBytes: Long = 0L,
    @SerializedName("tx_bytes") val txBytes: Long = 0L,
    @SerializedName("rx_speed") val rxSpeedBytesPerSec: Long = 0L,
    @SerializedName("tx_speed") val txSpeedBytesPerSec: Long = 0L
)

// --- 5. Bands ---

data class BandConfig(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("sa_bands") val saBands: List<String> = emptyList(),
    @SerializedName("nsa_bands") val nsaBands: List<String> = emptyList(),
    @SerializedName("lte_bands") val lteBands: List<String> = emptyList()
)

// --- 6. SMS ---

data class SmsItem(
    val boxType: Int, // 0 = Inbox, 1 = Outbox, 2 = Draft/Unsent
    val id: String,
    val isRead: Boolean,
    val timestamp: String,
    val address: String,
    val content: String
)

// --- 7. System Load & Router Status ---

data class SystemStatus(
    val loadAverage: String = "",
    val uptimeDays: Int = 0,
    val uptimeHours: Int = 0,
    val uptimeMinutes: Int = 0,
    val ramTotalKb: Long = 0L,
    val ramUsedKb: Long = 0L,
    val ramFreeKb: Long = 0L,
    val batteryLevelBar: Int = 0,
    val isBatteryCharging: Boolean = false,
    val simCardReady: Boolean = true,
    val unreadSmsCount: Int = 0,
    val networkType: String = "",
    val rsrp4g: Int = 0,
    val rsrq4g: Int = 0,
    val rsrp5g: Int = 0,
    val rsrq5g: Int = 0
)

// --- 8. Wi-Fi Configuration Models ---

data class Wifi24Config(
    val enabled: Boolean = true,
    val ssid: String = "FL-2.4G-0977",
    val hideSsid: Boolean = false,
    val wirelessMode: String = "6",
    val channelBandwidth: String = "1",
    val channel: String = "0",
    val authMode: String = "psk",
    val encryption: String = "aes",
    val password: String = "1234567890",
    val maxStations: Int = 32
)

data class Wifi5Config(
    val enabled: Boolean = true,
    val ssid: String = "XUAN-MF650-MIFI",
    val hideSsid: Boolean = false,
    val wirelessMode: String = "5",
    val channelBandwidth: String = "2",
    val channel: String = "44",
    val authMode: String = "psk",
    val encryption: String = "aes",
    val password: String = "1234567890",
    val maxStations: Int = 32
)

// --- 9. LAN & DHCP ---

data class LanDhcpConfig(
    val lanIp: String = "192.168.100.1",
    val netmask: String = "255.255.255.0",
    val dhcpEnabled: Boolean = true,
    val poolStart: String = "192.168.100.100",
    val poolEnd: String = "192.168.100.200",
    val leaseSeconds: Long = 86400L,
    val customDns: String = "",
    val staticLeases: List<StaticLease> = emptyList()
)

data class StaticLease(
    val mac: String,
    val ip: String,
    val hostname: String
)

// --- 10. Firewall ---

data class FirewallConfig(
    val spiEnabled: Boolean = true,
    val respondPing: Boolean = false,
    val allowRemoteWeb: Boolean = false,
    val dmzEnabled: Boolean = false,
    val dmzIp: String = "192.168.100.196",
    val portForwardRules: List<PortForwardRule> = emptyList()
)

data class PortForwardRule(
    val name: String,
    val protocol: String, // TCP / UDP / BOTH
    val externalPort: String,
    val internalIp: String,
    val internalPort: String
)

// --- 11. Modem & APN ---

data class ModemConfig(
    val ratMode: String = "AUTO",
    val dialPolicy: String = "2",
    val simSlot: String = "1",
    val roamingEnabled: Boolean = true,
    val apn: String = "3gnet",
    val pdpType: String = "3",
    val authType: String = "0",
    val user: String = "",
    val pass: String = ""
)

// --- 12. Forward & Cron ---

data class ForwardConfig(
    val enabled: Boolean = false,
    val channel: String = "Webhook", // Bark, PushDeer, Telegram, Webhook
    val token: String = "",
    val log: String = ""
)

data class CronItem(
    val id: String,
    val name: String = "",
    val mode: String = "",
    val time: String = "",
    val days: String = "",
    val command: String = "",
    val enabled: Boolean = true
)

// --- 13. App High-level State ---

data class AppState(
    val deviceReachable: Boolean = false,
    val padavanOnline: Boolean = false,
    val advancedOnline: Boolean = false,
    val ttydOnline: Boolean = false,
    val cellularConnected: Boolean = false,
    val wifiRestarting: Boolean = false,
    val authenticated: Boolean = true,
    val activeIp: String = "192.168.100.1",
    val errorMessage: String? = null
)
