package com.mf650.manager

import com.mf650.manager.data.parser.BatteryDataSource
import com.mf650.manager.data.parser.BatteryNormalizer
import com.mf650.manager.data.parser.BatteryRawValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryNormalizerTest {

    @Test
    fun testDiscreteScale0to6Mapping() {
        // According to official charge.html logic: [0, 10, 25, 50, 75, 90, 100]
        val expectedMapping = listOf(0, 10, 25, 50, 75, 90, 100)
        for (i in 0..6) {
            val raw = BatteryRawValue(
                source = BatteryDataSource.PORT_80_ASP,
                barValue = i
            )
            val state = BatteryNormalizer.normalize(raw)
            assertEquals("Bar value $i should map to ${expectedMapping[i]}%", expectedMapping[i], state.percent)
            assertTrue(state.isValid)
        }
    }

    @Test
    fun testStringFractionFromPort8081() {
        // Port 8081 returns level as "6/6", "5/6", etc.
        val raw6 = BatteryRawValue(
            source = BatteryDataSource.PORT_8081_REST,
            levelString = "6/6",
            voltageString = "3.98V",
            isCharging = true
        )
        val state6 = BatteryNormalizer.normalize(raw6)
        assertEquals(100, state6.percent)
        assertEquals(3.98, state6.voltageVolts ?: 0.0, 0.01)
        assertTrue(state6.isCharging)
        assertTrue(state6.isValid)

        val raw0 = BatteryRawValue(
            source = BatteryDataSource.PORT_8081_REST,
            levelString = "0/6"
        )
        val state0 = BatteryNormalizer.normalize(raw0)
        assertEquals(0, state0.percent)

        val raw3 = BatteryRawValue(
            source = BatteryDataSource.PORT_8081_REST,
            levelString = "3/6"
        )
        val state3 = BatteryNormalizer.normalize(raw3)
        assertEquals(50, state3.percent)
    }

    @Test
    fun testDirectPercentageStrings() {
        val rawPercent = BatteryRawValue(
            source = BatteryDataSource.PORT_8081_REST,
            levelString = "85%"
        )
        val state = BatteryNormalizer.normalize(rawPercent)
        assertEquals(85, state.percent)
        assertTrue(state.isValid)
    }

    @Test
    fun testVoltageParsing() {
        val raw1 = BatteryRawValue(source = BatteryDataSource.PORT_8081_REST, voltageString = "4.15V")
        val state1 = BatteryNormalizer.normalize(raw1)
        assertEquals(4.15, state1.voltageVolts ?: 0.0, 0.001)

        val raw2 = BatteryRawValue(source = BatteryDataSource.PORT_8081_REST, voltageString = "3850mV")
        val state2 = BatteryNormalizer.normalize(raw2)
        assertEquals(3.85, state2.voltageVolts ?: 0.0, 0.001)

        val raw3 = BatteryRawValue(source = BatteryDataSource.PORT_8081_REST, voltageString = "invalid")
        val state3 = BatteryNormalizer.normalize(raw3)
        assertNull(state3.voltageVolts)
    }

    @Test
    fun testOutOfRangeInputsDoNotClampSilently() {
        // Value 7 is out of 0..6 scale
        val rawOver = BatteryRawValue(
            source = BatteryDataSource.PORT_80_ASP,
            barValue = 7
        )
        val stateOver = BatteryNormalizer.normalize(rawOver)
        assertFalse("Out of range discrete step must be marked invalid", stateOver.isValid)

        // Negative value
        val rawNeg = BatteryRawValue(
            source = BatteryDataSource.PORT_80_ASP,
            barValue = -1
        )
        val stateNeg = BatteryNormalizer.normalize(rawNeg)
        assertFalse(stateNeg.isValid)
    }

    @Test
    fun testNullOrEmptyFallsBackGracefully() {
        val rawEmpty = BatteryRawValue(source = BatteryDataSource.PORT_8081_REST)
        val stateEmpty = BatteryNormalizer.normalize(rawEmpty)
        assertFalse(stateEmpty.isValid)
        assertFalse(stateEmpty.isCharging)
    }
}
