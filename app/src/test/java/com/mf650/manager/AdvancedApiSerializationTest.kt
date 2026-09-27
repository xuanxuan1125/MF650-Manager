package com.mf650.manager

import com.google.gson.Gson
import com.mf650.manager.data.model.BandConfig
import com.mf650.manager.data.model.BatteryStatus
import com.mf650.manager.data.model.CellularStatus
import com.mf650.manager.data.model.DeviceInfo
import com.mf650.manager.data.model.TrafficStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedApiSerializationTest {

    private val gson = Gson()

    @Test
    fun testDeviceInfoSerialization() {
        val json = """
            {
                "success": true,
                "device_info": {
                    "model": "MF650",
                    "software_version": "v1.0.8",
                    "hardware_version": "SDX55-V2",
                    "imei": "860123456789012",
                    "mac": "00:E0:4C:68:09:77",
                    "sn": "SN9876543210"
                },
                "system_status": {
                    "cpu_usage": "15%",
                    "memory_usage": "52%",
                    "temperature": "42°C",
                    "uptime": "24h 10m"
                },
                "network_info": {
                    "lan_ip": "192.168.100.1",
                    "connected_devices": 3,
                    "wifi_24g_status": true,
                    "wifi_5g_status": true
                },
                "cellular_info": {
                    "network_type": "5G SA",
                    "operator": "中国联通",
                    "signal_level": 4,
                    "sim_status": "READY"
                }
            }
        """.trimIndent()

        val dev = gson.fromJson(json, DeviceInfo::class.java)
        assertTrue(dev.success)
        assertEquals("MF650", dev.deviceInfo?.model)
        assertEquals("860123456789012", dev.deviceInfo?.imei)
        assertEquals(3, dev.networkInfo?.connectedDevices)
        assertEquals("5G SA", dev.cellularInfo?.networkType)
    }

    @Test
    fun testCellularStatusSerialization() {
        val json = """
            {
                "status": 1,
                "lte_lock": false,
                "lte_count": 0,
                "nr_lock": true,
                "network": "5G SA",
                "lte": {
                    "earfcn": 1650,
                    "pci": 210,
                    "rsrp": -92,
                    "rsrq": -11,
                    "sinr": 16.5,
                    "band": "B3"
                },
                "nr": {
                    "nrarfcn": 634080,
                    "pci": 420,
                    "rsrp": -82,
                    "rsrq": -9,
                    "sinr": 23.4,
                    "band": "n78"
                },
                "band": "n78",
                "lte_neighbors": [
                    { "pci": 211, "earfcn": 1650, "rsrp": -98, "rsrq": -14 }
                ]
            }
        """.trimIndent()

        val cell = gson.fromJson(json, CellularStatus::class.java)
        assertEquals(1, (cell.status as? Number)?.toInt() ?: cell.status?.toString()?.toIntOrNull())
        assertTrue(cell.isNrLocked)
        assertEquals("5G SA", cell.network)
        assertEquals("n78", cell.nr?.band)
        assertEquals(634080, (cell.nr?.nrarfcn as? Number)?.toInt())
        assertEquals(420, (cell.nr?.pci as? Number)?.toInt())
        assertEquals(-82, cell.nr?.rsrpInt)
        assertEquals(1, cell.lteNeighbors.size)
        assertEquals(211, (cell.lteNeighbors[0].pci as? Number)?.toInt())
    }

    @Test
    fun testBatteryStatusSerialization() {
        val json = """
            {
                "status": "success",
                "battery": { "level": "6/6", "voltage": "4.15V" },
                "usb_connection": "connected",
                "charging": { "enabled": 1, "status": "充电中" },
                "auto_charge": { "enabled": 1 }
            }
        """.trimIndent()

        val battery = gson.fromJson(json, BatteryStatus::class.java)
        assertEquals("6/6", battery.battery?.level)
        assertEquals("4.15V", battery.battery?.voltage)
        assertEquals("connected", battery.usbConnection)
        assertTrue(battery.charging?.isCharging == true)
        assertTrue(battery.autoCharge?.isAutoChargeActive == true)
    }

    @Test
    fun testTrafficStatusSerialization() {
        val json = """
            {
                "status": true,
                "traffic_stats": {
                    "rx_bytes": 1073741824,
                    "tx_bytes": 268435456,
                    "total_bytes": 1342177280,
                    "rx_speed": 1048576,
                    "tx_speed": 204800
                },
                "traffic_limit": 50.0,
                "limit_unit": "GB"
            }
        """.trimIndent()

        val traffic = gson.fromJson(json, TrafficStatus::class.java)
        assertTrue(traffic.success)
        assertEquals(1073741824L, traffic.trafficStats?.rxBytes)
        assertEquals(1048576L, traffic.trafficStats?.rxSpeedBytesPerSec)
        assertEquals(50.0, traffic.trafficLimit, 0.01)
    }

    @Test
    fun testBandConfigSerialization() {
        val json = """
            {
                "success": true,
                "sa_bands": ["n1", "n28", "n41", "n78"],
                "nsa_bands": ["n41", "n78"],
                "lte_bands": ["B1", "B3", "B5", "B8", "B34", "B38", "B39", "B40", "B41"]
            }
        """.trimIndent()

        val band = gson.fromJson(json, BandConfig::class.java)
        assertTrue(band.success)
        assertEquals(4, band.saBands.size)
        assertTrue(band.saBands.contains("n78"))
        assertEquals(9, band.lteBands.size)
    }
}
