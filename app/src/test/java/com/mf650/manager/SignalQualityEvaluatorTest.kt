package com.mf650.manager

import com.mf650.manager.data.parser.SignalGrade
import com.mf650.manager.data.parser.SignalQualityEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalQualityEvaluatorTest {

    @Test
    fun testExcellentSignal() {
        val eval = SignalQualityEvaluator.evaluate(
            rsrp = -75,
            sinr = 25.0,
            rsrq = -8,
            rat = "5G SA"
        )
        assertEquals(SignalGrade.EXCELLENT, eval.grade)
        assertEquals(5, eval.bars)
        assertTrue(eval.rsrpDescription.contains("极强"))
        assertTrue(eval.sinrDescription.contains("超高信噪比"))
    }

    @Test
    fun testGoodSignal() {
        val eval = SignalQualityEvaluator.evaluate(
            rsrp = -90,
            sinr = 15.0,
            rsrq = -12,
            rat = "5G SA"
        )
        assertEquals(SignalGrade.GOOD, eval.grade)
        assertEquals(4, eval.bars)
    }

    @Test
    fun testFairSignal() {
        val eval = SignalQualityEvaluator.evaluate(
            rsrp = -105,
            sinr = 5.0,
            rsrq = -16,
            rat = "LTE"
        )
        assertEquals(SignalGrade.FAIR, eval.grade)
        assertEquals(2, eval.bars)
    }

    @Test
    fun testPoorSignal() {
        val eval = SignalQualityEvaluator.evaluate(
            rsrp = -118,
            sinr = -2.0,
            rsrq = -22,
            rat = "LTE"
        )
        assertEquals(SignalGrade.POOR, eval.grade)
        assertEquals(1, eval.bars)
        assertTrue(eval.suggestion.contains("锁频段"))
    }

    @Test
    fun testNullOrZeroSignal() {
        val eval = SignalQualityEvaluator.evaluate(
            rsrp = null,
            sinr = null,
            rsrq = null
        )
        assertEquals(SignalGrade.UNKNOWN, eval.grade)
        assertEquals(0, eval.bars)
    }
}
