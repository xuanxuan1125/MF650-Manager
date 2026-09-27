package com.mf650.manager.data.parser

enum class BatteryDataSource {
    PORT_8081_REST,
    PORT_80_ASP
}

data class BatteryRawValue(
    val source: BatteryDataSource,
    val levelString: String? = null,
    val barValue: Int? = null,
    val voltageString: String? = null,
    val isCharging: Boolean? = null,
    val autoChargeEnabled: Boolean? = null
)

data class BatteryState(
    val percent: Int?, // 0..100, or null if corrupt/invalid
    val displayPercent: String, // "75%" or "--"
    val voltageVolts: Double?,
    val voltageDisplay: String, // "3.98 V" or "--"
    val isCharging: Boolean,
    val autoChargeEnabled: Boolean,
    val isValid: Boolean,
    val source: BatteryDataSource,
    val errorMessage: String? = null
)

object BatteryNormalizer {

    // Official SDX55 + IP5332 PMIC battery level percentage lookup table
    // Extracted directly from 11_pulled_web/8081/html/charge.html lines 768-770
    // percentages = [0, 10, 25, 50, 75, 90, 100]
    private val OFFICIAL_LEVEL_PERCENTAGES = intArrayOf(0, 10, 25, 50, 75, 90, 100)

    fun normalize(raw: BatteryRawValue): BatteryState {
        var calculatedPercent: Int? = null
        var isDataValid = true
        var errorMsg: String? = null

        when (raw.source) {
            BatteryDataSource.PORT_8081_REST -> {
                val levelStr = raw.levelString?.trim()
                if (levelStr.isNullOrEmpty() || levelStr.equals("N/A", ignoreCase = true)) {
                    isDataValid = false
                    errorMsg = "Empty or N/A battery level from Port 8081"
                } else {
                    // 1. Check for standard "X/6" fraction format
                    val fractionMatch = Regex("""^(\d+)/6$""").find(levelStr)
                    if (fractionMatch != null) {
                        val level = fractionMatch.groupValues[1].toIntOrNull()
                        if (level != null && level in 0..6) {
                            calculatedPercent = OFFICIAL_LEVEL_PERCENTAGES[level]
                        } else {
                            isDataValid = false
                            errorMsg = "Level out of 0..6 hardware scale: $levelStr"
                        }
                    } else if (levelStr.endsWith("%")) {
                        val numeric = levelStr.removeSuffix("%").trim().toIntOrNull()
                        if (numeric != null && numeric in 0..100) {
                            calculatedPercent = numeric
                        } else {
                            isDataValid = false
                            errorMsg = "Percentage out of bounds: $levelStr"
                        }
                    } else {
                        // Check if direct integer percentage 0..100 or ratio 0.0..1.0
                        val dblVal = levelStr.toDoubleOrNull()
                        if (dblVal != null) {
                            if (dblVal in 0.0..1.0 && levelStr.contains(".")) {
                                calculatedPercent = (dblVal * 100).toInt()
                            } else if (dblVal in 0.0..100.0) {
                                calculatedPercent = dblVal.toInt()
                            } else {
                                isDataValid = false
                                errorMsg = "Raw number out of bounds: $levelStr"
                            }
                        } else {
                            isDataValid = false
                            errorMsg = "Unparseable battery string: $levelStr"
                        }
                    }
                }
            }

            BatteryDataSource.PORT_80_ASP -> {
                val bar = raw.barValue
                if (bar == null || bar < 0 || bar > 6) {
                    isDataValid = false
                    errorMsg = "Invalid bar value from Port 80: $bar (expected 0..6)"
                } else {
                    calculatedPercent = OFFICIAL_LEVEL_PERCENTAGES[bar]
                }
            }
        }

        // Strict validation: percent must never exceed 100 or be less than 0
        if (calculatedPercent != null && (calculatedPercent < 0 || calculatedPercent > 100)) {
            isDataValid = false
            calculatedPercent = null
            errorMsg = "Calculated percent strictly out of range [0, 100]"
        }

        // Voltage parsing
        var voltageVal: Double? = null
        val voltStr = raw.voltageString
        if (!voltStr.isNullOrBlank()) {
            val vMatch = Regex("""([\d.]+)""").find(voltStr)
            val num = vMatch?.groupValues?.get(1)?.toDoubleOrNull()
            if (num != null) {
                voltageVal = if (voltStr.contains("mV", ignoreCase = true) || num > 100.0) num / 1000.0 else num
            }
        }

        val displayPct = if (isDataValid && calculatedPercent != null) "$calculatedPercent%" else "--"
        val displayVolt = voltageVal?.let { String.format(java.util.Locale.US, "%.2f V", it) } ?: "--"

        return BatteryState(
            percent = calculatedPercent,
            displayPercent = displayPct,
            voltageVolts = voltageVal,
            voltageDisplay = displayVolt,
            isCharging = raw.isCharging ?: false,
            autoChargeEnabled = raw.autoChargeEnabled ?: false,
            isValid = isDataValid,
            source = raw.source,
            errorMessage = errorMsg
        )
    }
}
