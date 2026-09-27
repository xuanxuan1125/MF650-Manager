package com.mf650.manager

import com.mf650.manager.data.model.FirewallConfig
import com.mf650.manager.data.model.LanDhcpConfig
import com.mf650.manager.data.model.ModemConfig
import com.mf650.manager.data.model.Wifi24Config
import com.mf650.manager.data.model.Wifi5Config
import com.mf650.manager.data.parser.PadavanFormBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PadavanFormBuilderTest {

    @Test
    fun testBuildWifi24Form() {
        val config = Wifi24Config(
            enabled = true,
            ssid = "TestSSID_24",
            hideSsid = false,
            channel = "6",
            password = "secret_password_24"
        )
        val form = PadavanFormBuilder.buildWifi24Form(config)

        assertEquals("Advanced_Wireless2g_Content.asp", form["current_page"])
        assertEquals(" Apply ", form["action_mode"])
        assertEquals("restart_wifis", form["action_script"])
        assertEquals("WLANConfig11b;", form["sid_list"])
        assertEquals("1", form["rt_radio_x"])
        assertEquals("TestSSID_24", form["rt_ssid"])
        assertEquals("0", form["rt_closed"])
        assertEquals("6", form["rt_channel"])
        assertEquals("secret_password_24", form["rt_wpa_psk"])
    }

    @Test
    fun testBuildWifi5Form() {
        val config = Wifi5Config(
            enabled = true,
            ssid = "TestSSID_5G",
            hideSsid = true,
            channel = "36",
            password = "secret_password_5g"
        )
        val form = PadavanFormBuilder.buildWifi5Form(config)

        assertEquals("Advanced_Wireless_Content.asp", form["current_page"])
        assertEquals(" Apply ", form["action_mode"])
        assertEquals("restart_wifis", form["action_script"])
        assertEquals("WLANConfig11a;", form["sid_list"])
        assertEquals("TestSSID_5G", form["wl_ssid"])
        assertEquals("1", form["wl_closed"])
        assertEquals("36", form["wl_channel"])
    }

    @Test
    fun testBuildDhcpForm() {
        val config = LanDhcpConfig(
            lanIp = "192.168.100.1",
            poolStart = "192.168.100.50",
            poolEnd = "192.168.100.150",
            leaseSeconds = 43200L
        )
        val form = PadavanFormBuilder.buildDhcpForm(config)

        assertEquals("Advanced_LAN_Content.asp", form["current_page"])
        assertEquals("LANHostConfig;", form["sid_list"])
        assertEquals("restart_dnsd", form["action_script"])
        assertEquals("192.168.100.50", form["dhcp_start"])
        assertEquals("192.168.100.150", form["dhcp_end"])
        assertEquals("43200", form["dhcp_lease"])
    }

    @Test
    fun testBuildRebootForm() {
        val form = PadavanFormBuilder.buildRebootForm()
        assertEquals("Advanced_SettingBackup_Content.asp", form["current_page"])
        assertEquals(" Reboot ", form["action_mode"])
    }

    @Test
    fun testBuildSendSmsForm() {
        val form = PadavanFormBuilder.buildSendSmsForm("10010", "CXLL")
        assertEquals("smsSend.asp", form["current_page"])
        assertEquals("handle_sms", form["action_script"])
        assertEquals("SEND", form["handle_sms_mode"])
        assertEquals("10010", form["number"])
        assertEquals("CXLL", form["content"])
    }
}
