package com.mf650.manager

import com.mf650.manager.data.parser.PadavanParsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PadavanParserTest {

    @Test
    fun testParseSystemStatus() {
        val sampleJs = """
            cpu_usage = "12%";
            mem_usage = "48%";
            mem_total = 256000;
            mem_used = 122880;
            uptime_days = 2;
            uptime_hours = 5;
            uptime_minutes = 30;
            battery_val = 3;
            is_charging = 1;
            sim_status = 1;
            unread_sms = 2;
            net_type = "5G SA";
            rsrp_5g = -88;
            sinr_5g = 18;
        """.trimIndent()

        val status = PadavanParsers.parseSystemStatus(sampleJs)
        assertEquals(256000L, status.ramTotalKb)
        assertEquals(122880L, status.ramUsedKb)
        assertEquals(2, status.uptimeDays)
        assertEquals(5, status.uptimeHours)
        assertEquals(30, status.uptimeMinutes)
        assertEquals(3, status.batteryLevelBar)
        assertTrue(status.isBatteryCharging)
        assertTrue(status.simCardReady)
        assertEquals(2, status.unreadSmsCount)
        assertEquals("5G SA", status.networkType)
        assertEquals(-88, status.rsrp5g)
    }

    @Test
    fun testParseStatusInternet() {
        val sampleJs = """
            status_internet = "3";
        """.trimIndent()

        val code = PadavanParsers.parseStatusInternet(sampleJs)
        assertEquals(3, code)
    }

    @Test
    fun testParseSmsArray() {
        val sampleJs = """
            var smsmonitor_last = [
                ["0", "15", "1", "2026-09-27 10:15:00", "10010", "【中国联通】您的账户余额为52.40元。"],
                ["0", "16", "0", "2026-09-27 11:20:00", "10086", "【中国移动】验证码：829103，请在5分钟内输入。"]
            ];
        """.trimIndent()

        val list = PadavanParsers.parseSmsArray(sampleJs, 0)
        assertEquals(2, list.size)

        val item1 = list[0]
        assertEquals("15", item1.id)
        assertTrue(item1.isRead)
        assertEquals("10010", item1.address)
        assertEquals("【中国联通】您的账户余额为52.40元。", item1.content)

        val item2 = list[1]
        assertEquals("16", item2.id)
        assertFalse(item2.isRead)
        assertEquals("10086", item2.address)
        assertEquals("【中国移动】验证码：829103，请在5分钟内输入。", item2.content)
    }
}
