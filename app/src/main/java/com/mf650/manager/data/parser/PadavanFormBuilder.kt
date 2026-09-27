package com.mf650.manager.data.parser

import com.mf650.manager.data.model.FirewallConfig
import com.mf650.manager.data.model.LanDhcpConfig
import com.mf650.manager.data.model.ModemConfig
import com.mf650.manager.data.model.Wifi24Config
import com.mf650.manager.data.model.Wifi5Config

/**
 * Constructs complete x-www-form-urlencoded parameter maps for Padavan's /apply.cgi.
 * Preserves all mandatory hidden fields and state parameters discovered in reverse engineering.
 */
object PadavanFormBuilder {

    fun buildWifi24Form(config: Wifi24Config): Map<String, String> {
        val form = mutableMapOf<String, String>()
        form["current_page"] = "Advanced_Wireless2g_Content.asp"
        form["next_page"] = ""
        form["sid_list"] = "WLANConfig11b;"
        form["group_id"] = ""
        form["action_mode"] = " Apply "
        form["action_script"] = "restart_wifis"
        form["rt_country_code"] = "CN"
        form["preferred_lang"] = "CN"
        form["rt_radio_date_x"] = "1111111"
        form["rt_radio_time_x"] = "00002359"
        form["rt_radio_time2_x"] = "00002359"
        form["rt_wpa_mode"] = "2"
        form["rt_gmode_protection"] = "auto"
        form["rt_mode_x"] = "0"
        form["rt_HT_EXTCHA_old"] = "1"
        form["rt_key_type"] = "0"

        form["rt_radio_x"] = if (config.enabled) "1" else "0"
        form["rt_ssid"] = config.ssid
        form["rt_closed"] = if (config.hideSsid) "1" else "0"
        form["rt_gmode"] = config.wirelessMode
        form["rt_channel"] = config.channel
        form["rt_wpa_psk"] = config.password
        form["rt_HT_BW"] = config.channelBandwidth
        return form
    }

    fun buildWifi5Form(config: Wifi5Config): Map<String, String> {
        val form = mutableMapOf<String, String>()
        form["current_page"] = "Advanced_Wireless_Content.asp"
        form["next_page"] = ""
        form["sid_list"] = "WLANConfig11a;"
        form["group_id"] = ""
        form["action_mode"] = " Apply "
        form["action_script"] = "restart_wifis"
        form["wl_country_code"] = "CN"
        form["preferred_lang"] = "CN"
        form["wl_radio_date_x"] = "1111111"
        form["wl_radio_time_x"] = "00002359"
        form["wl_radio_time2_x"] = "00002359"
        form["wl_wpa_mode"] = "2"
        form["wl_mode_x"] = "0"
        form["wl_key_type"] = "0"

        form["wl_radio_x"] = if (config.enabled) "1" else "0"
        form["wl_ssid"] = config.ssid
        form["wl_closed"] = if (config.hideSsid) "1" else "0"
        form["wl_gmode"] = config.wirelessMode
        form["wl_channel"] = config.channel
        form["wl_wpa_psk"] = config.password
        form["wl_HT_BW"] = config.channelBandwidth
        return form
    }

    fun buildDhcpForm(config: LanDhcpConfig): Map<String, String> {
        val form = mutableMapOf<String, String>()
        form["current_page"] = "Advanced_LAN_Content.asp"
        form["next_page"] = ""
        form["sid_list"] = "LANHostConfig;"
        form["group_id"] = "ManualDHCPList"
        form["action_mode"] = " Apply "
        form["action_script"] = "restart_dnsd"
        form["preferred_lang"] = "CN"

        form["dhcp_enable_x"] = if (config.dhcpEnabled) "1" else "0"
        form["dhcp_start"] = config.poolStart
        form["dhcp_end"] = config.poolEnd
        form["dhcp_lease"] = config.leaseSeconds.toString()
        form["dhcp_gateway_x"] = config.lanIp
        form["dhcp_dns1_x"] = config.customDns
        return form
    }

    fun buildLanForm(ipAddr: String, netmask: String): Map<String, String> {
        val form = mutableMapOf<String, String>()
        form["current_page"] = "Advanced_LAN_Content.asp"
        form["next_page"] = ""
        form["sid_list"] = "LANHostConfig;"
        form["action_mode"] = " Apply "
        form["action_script"] = "restart_net_and_phy"
        form["preferred_lang"] = "CN"

        form["lan_ipaddr"] = ipAddr
        form["lan_netmask"] = netmask
        return form
    }

    fun buildModemSettingsForm(config: ModemConfig): Map<String, String> {
        val form = mutableMapOf<String, String>()
        form["current_page"] = "Advanced_Settings_Content.asp"
        form["sid_list"] = "LANHostConfig;General;Storage;"
        form["action_mode"] = " Apply "
        form["action_script"] = "sys_settings"

        form["operationModeSelect"] = config.ratMode
        form["dial_policy"] = config.dialPolicy
        form["modem_roaming"] = if (config.roamingEnabled) "1" else "0"
        form["modem_apn"] = config.apn
        form["modem_user"] = config.user
        form["modem_pass"] = config.pass
        form["modem_auth"] = config.authType
        form["modem_pdp_type"] = config.pdpType
        return form
    }

    fun buildFirewallForm(config: FirewallConfig): Map<String, String> {
        val form = mutableMapOf<String, String>()
        form["current_page"] = "Advanced_BasicFirewall_Content.asp"
        form["sid_list"] = "FirewallConfig;"
        form["action_mode"] = " Apply "
        form["action_script"] = "restart_firewall"

        form["fw_enable_x"] = if (config.spiEnabled) "1" else "0"
        form["misc_ping_x"] = if (config.respondPing) "1" else "0"
        form["misc_http_x"] = if (config.allowRemoteWeb) "1" else "0"
        form["dmz_ip"] = config.dmzIp
        return form
    }

    fun buildRebootForm(): Map<String, String> {
        return mapOf(
            "current_page" to "Advanced_SettingBackup_Content.asp",
            "sid_list" to "General;Storage;",
            "action_mode" to " Reboot "
        )
    }

    fun buildSendSmsForm(number: String, content: String): Map<String, String> {
        return mapOf(
            "current_page" to "smsSend.asp",
            "next_page" to "",
            "sid_list" to "General;",
            "action_mode" to " Apply ",
            "action_script" to "handle_sms",
            "handle_sms_mode" to "SEND",
            "preferred_lang" to "CN",
            "number" to number,
            "content" to content
        )
    }

    fun buildDeleteSmsForm(boxType: String, smsId: String): Map<String, String> {
        return mapOf(
            "current_page" to "smsInbox.asp",
            "next_page" to "smsInbox.asp",
            "action_mode" to " Apply ",
            "action_script" to "handle_sms",
            "handle_sms_mode" to "DELETE",
            "box_type" to boxType,
            "sms_id" to smsId
        )
    }

    fun buildMarkSmsReadForm(smsId: String): Map<String, String> {
        return mapOf(
            "current_page" to "smsInbox.asp",
            "next_page" to "smsInbox.asp",
            "action_mode" to " Apply ",
            "action_script" to "handle_sms",
            "handle_sms_mode" to "MODIFYTYPE",
            "sms_id" to smsId
        )
    }
}
