package com.mf650.manager.data.parser

import com.mf650.manager.data.model.SignalStatus
import com.mf650.manager.data.model.SmsItem
import com.mf650.manager.data.model.SystemStatus
import java.util.regex.Pattern

object PadavanParsers {

    fun parseSystemStatus(rawJs: String): SystemStatus {
        val lavg = extractValue(rawJs, "(?:lavg)[\\s:=]+\"([^\"]+)\"") ?: ""
        val days = extractInt(rawJs, "(?:days|uptime_days)[\\s:=]+(\\d+)")
        val hours = extractInt(rawJs, "(?:hours|uptime_hours)[\\s:=]+(\\d+)")
        val minutes = extractInt(rawJs, "(?:minutes|uptime_minutes)[\\s:=]+(\\d+)")
        val ramTotal = extractLong(rawJs, "(?:total|mem_total)[\\s:=]+(\\d+)")
        val ramUsed = extractLong(rawJs, "(?:used|mem_used)[\\s:=]+(\\d+)")
        val ramFree = extractLong(rawJs, "(?:free|mem_free)[\\s:=]+(\\d+)")
        val batVal = extractInt(rawJs, "(?:bat_value|battery_val)[\\s:=]+(\\d+)")
        val batCharge = extractInt(rawJs, "(?:bat_charge|is_charging)[\\s:=]+(\\d+)") == 1
        val simReady = extractInt(rawJs, "(?:simcard_status|sim_status)[\\s:=]+(\\d+)") == 1
        val unreadSms = extractInt(rawJs, "(?:sms_unread_count|unread_sms)[\\s:=]+(\\d+)")
        val netType = extractValue(rawJs, "(?:network_type|net_type)[\\s:=]+\"([^\"]+)\"") ?: ""
        val rsrp = extractInt(rawJs, "(?:rsrp|rsrp_4g)[\\s:=]+(-?\\d+)")
        val rsrq = extractInt(rawJs, "(?:rsrq|rsrq_4g)[\\s:=]+(-?\\d+)")
        val rsrp5g = extractInt(rawJs, "rsrp_5g[\\s:=]+(-?\\d+)")
        val rsrq5g = extractInt(rawJs, "rsrq_5g[\\s:=]+(-?\\d+)")

        return SystemStatus(
            loadAverage = lavg,
            uptimeDays = days,
            uptimeHours = hours,
            uptimeMinutes = minutes,
            ramTotalKb = ramTotal,
            ramUsedKb = ramUsed,
            ramFreeKb = ramFree,
            batteryLevelBar = batVal,
            isBatteryCharging = batCharge,
            simCardReady = simReady,
            unreadSmsCount = unreadSms,
            networkType = netType,
            rsrp4g = rsrp,
            rsrq4g = rsrq,
            rsrp5g = rsrp5g,
            rsrq5g = rsrq5g
        )
    }

    fun parseStatusInternet(rawJs: String): Int {
        return extractInt(rawJs, "status_internet[\\s:=]+\"?(\\d+)\"?")
    }

    fun parseSignalStatus(rawJs: String): SignalStatus {
        val s4g = extractInt(rawJs, "function\\s+signal_strength\\(\\)\\s*\\{\\s*return\\s*(\\d+);")
        val s5g = extractInt(rawJs, "function\\s+signal_strength_5g\\(\\)\\s*\\{\\s*return\\s*(\\d+);")
        val rsrp = extractInt(rawJs, "function\\s+rsrp\\(\\)\\s*\\{\\s*return\\s*'(-?\\d+)';")
        val rsrq = extractInt(rawJs, "function\\s+rsrq\\(\\)\\s*\\{\\s*return\\s*'(-?\\d+)';")
        val rsrp5g = extractInt(rawJs, "function\\s+rsrp_5g\\(\\)\\s*\\{\\s*return\\s*'(-?\\d+)';")
        val rsrq5g = extractInt(rawJs, "function\\s+rsrq_5g\\(\\)\\s*\\{\\s*return\\s*'(-?\\d+)';")
        val cellId = extractValue(rawJs, "function\\s+cellid\\(\\)\\s*\\{\\s*return\\s*'([^']*)';") ?: ""
        val netType = extractValue(rawJs, "function\\s+network_type\\(\\)\\s*\\{\\s*return\\s*'([^']*)';") ?: ""
        val operator = extractValue(rawJs, "function\\s+operator\\(\\)\\s*\\{\\s*return\\s*'([^']*)';") ?: ""

        return SignalStatus(
            signalStrength4g = s4g,
            signalStrength5g = s5g,
            rsrp4g = rsrp,
            rsrq4g = rsrq,
            rsrp5g = rsrp5g,
            rsrq5g = rsrq5g,
            cellId = cellId,
            networkType = netType,
            operator = operator
        )
    }

    fun parseSmsList(rawJs: String, defaultBoxType: Int = 0): List<SmsItem> {
        val items = mutableListOf<SmsItem>()
        val rowMatcher = Pattern.compile("\\[\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\",\\s*\"([^\"]*)\"\\]").matcher(rawJs)
        while (rowMatcher.find()) {
            val box = rowMatcher.group(1)?.toIntOrNull() ?: defaultBoxType
            val id = rowMatcher.group(2) ?: ""
            val isRead = rowMatcher.group(3) == "1"
            val time = rowMatcher.group(4) ?: ""
            val addr = rowMatcher.group(5) ?: ""
            val text = rowMatcher.group(6) ?: ""
            items.add(SmsItem(box, id, isRead, time, addr, text))
        }
        return items
    }

    fun parseSmsArray(rawJs: String, defaultBoxType: Int = 0): List<SmsItem> = parseSmsList(rawJs, defaultBoxType)

    private fun extractValue(src: String, regex: String): String? {
        val m = Pattern.compile(regex).matcher(src)
        return if (m.find()) m.group(1) else null
    }

    private fun extractInt(src: String, regex: String): Int {
        return extractValue(src, regex)?.toIntOrNull() ?: 0
    }

    private fun extractLong(src: String, regex: String): Long {
        return extractValue(src, regex)?.toLongOrNull() ?: 0L
    }
}
