package com.mf650.manager

import com.mf650.manager.data.parser.UnitNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitNormalizerTest {

    @Test
    fun testFormatSpeed() {
        assertEquals("0 KB/s", UnitNormalizer.formatSpeed(0f))
        assertEquals("512 KB/s", UnitNormalizer.formatSpeed(512f))
        assertEquals("1.00 MB/s", UnitNormalizer.formatSpeed(1024f))
        assertEquals("10.50 MB/s", UnitNormalizer.formatSpeed(10752f))
        assertEquals("1.00 GB/s", UnitNormalizer.formatSpeed(1024f * 1024f))
        assertEquals("--", UnitNormalizer.formatSpeed(-1f))
    }

    @Test
    fun testParseSpeedToKb() {
        // String formats
        assertEquals(512f, UnitNormalizer.parseSpeedToKb("512 KB/s"), 0.01f)
        assertEquals(1024f, UnitNormalizer.parseSpeedToKb("1 MB/s"), 0.01f)
        assertEquals(266.24f, UnitNormalizer.parseSpeedToKb("0.26 MB/s"), 0.01f)
        // Raw decimal without unit from 8081 device/info (in MB/s)
        assertEquals(266.24f, UnitNormalizer.parseSpeedToKb("0.26"), 0.01f)
        // Numeric directly
        assertEquals(1024f, UnitNormalizer.parseSpeedToKb(1024), 0.01f)
        assertEquals(0f, UnitNormalizer.parseSpeedToKb(null), 0.01f)
    }

    @Test
    fun testFormatTrafficGb() {
        assertEquals("500.0 MB", UnitNormalizer.formatTrafficGb(500.0 / 1024.0))
        assertEquals("1.50 GB", UnitNormalizer.formatTrafficGb(1.5))
        assertEquals("1.20 TB", UnitNormalizer.formatTrafficGb(1.2 * 1024.0))
        assertEquals("--", UnitNormalizer.formatTrafficGb(null))
        assertEquals("--", UnitNormalizer.formatTrafficGb(-5.0))
    }

    @Test
    fun testParseTrafficStringToGb() {
        assertEquals(1.5, UnitNormalizer.parseTrafficStringToGb("1.5 GB") ?: 0.0, 0.001)
        assertEquals(0.5, UnitNormalizer.parseTrafficStringToGb("512 MB") ?: 0.0, 0.001)
        assertEquals(2048.0, UnitNormalizer.parseTrafficStringToGb("2 TB") ?: 0.0, 0.001)
        assertEquals(10.0, UnitNormalizer.parseTrafficStringToGb("10") ?: 0.0, 0.001)
    }

    @Test
    fun testFormatTemperature() {
        assertEquals("45.2°C", UnitNormalizer.formatTemperature("45.2"))
        assertEquals("45.0°C", UnitNormalizer.formatTemperature("45°C"))
        assertEquals("--", UnitNormalizer.formatTemperature(""))
        assertEquals("--", UnitNormalizer.formatTemperature("invalid"))
        assertEquals("--", UnitNormalizer.formatTemperature("150")) // Exceeds realistic temp
    }

    @Test
    fun testFormatSignalMetrics() {
        // RSRP
        assertEquals("-85 dBm", UnitNormalizer.formatRsrp("-85"))
        assertEquals("-90 dBm", UnitNormalizer.formatRsrp(-90))
        assertEquals("--", UnitNormalizer.formatRsrp("-200"))
        assertEquals("--", UnitNormalizer.formatRsrp("invalid"))

        // SINR
        assertEquals("15.5 dB", UnitNormalizer.formatSinr("15.5"))
        assertEquals("-5.0 dB", UnitNormalizer.formatSinr(-5))
        assertEquals("--", UnitNormalizer.formatSinr("55"))

        // RSRQ
        assertEquals("-10 dB", UnitNormalizer.formatRsrq("-10"))
        assertEquals("--", UnitNormalizer.formatRsrq("10"))
    }

    @Test
    fun testFormatCpuUsageAndRam() {
        assertEquals("25.0%", UnitNormalizer.formatCpuUsage("25.0%"))
        assertEquals("12.5%", UnitNormalizer.formatCpuUsage(12.5))
        assertEquals("--", UnitNormalizer.formatCpuUsage(120))

        assertEquals("256 / 512 MB", UnitNormalizer.formatRamMb(262144L, 524288L))
        assertEquals("--", UnitNormalizer.formatRamMb(0L, 0L))
    }
}
