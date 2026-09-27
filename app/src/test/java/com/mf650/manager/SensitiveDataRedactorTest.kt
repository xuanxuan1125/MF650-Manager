package com.mf650.manager

import com.mf650.manager.data.security.SensitiveDataRedactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataRedactorTest {

    @Test
    fun testRedactImei() {
        val imei = "860123456789012"
        val redacted = SensitiveDataRedactor.redactImei(imei)
        assertEquals("860123******012", redacted)
        assertFalse(redacted.contains("456789"))
    }

    @Test
    fun testRedactMac() {
        val mac = "AA:BB:CC:DD:EE:FF"
        val redacted = SensitiveDataRedactor.redactMac(mac)
        assertEquals("AA:BB:CC:**:**:FF", redacted)
    }

    @Test
    fun testRedactPhoneNumber() {
        val phone = "13812345678"
        val redacted = SensitiveDataRedactor.redactPhoneNumber(phone)
        assertEquals("138****5678", redacted)
    }

    @Test
    fun testRedactLogString() {
        val rawLog = "Modem attached. IMEI: 860123456789012, client MAC AA:BB:CC:DD:EE:FF, SMS from 13812345678"
        val cleanLog = SensitiveDataRedactor.redactLog(rawLog)
        assertFalse(cleanLog.contains("860123456789012"))
        assertFalse(cleanLog.contains("13812345678"))
        assertTrue(cleanLog.contains("860123******012"))
        assertTrue(cleanLog.contains("138****5678"))
    }
}
