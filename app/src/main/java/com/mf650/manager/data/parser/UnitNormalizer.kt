package com.mf650.manager.data.parser

import java.util.Locale

object UnitNormalizer {

    fun formatSpeed(speedKbPerSec: Float): String {
        return when {
            speedKbPerSec < 0f -> "--"
            speedKbPerSec >= 1024f * 1024f -> String.format(Locale.US, "%.2f GB/s", speedKbPerSec / (1024f * 1024f))
            speedKbPerSec >= 1024f -> String.format(Locale.US, "%.2f MB/s", speedKbPerSec / 1024f)
            else -> String.format(Locale.US, "%.0f KB/s", speedKbPerSec)
        }
    }

    fun parseSpeedToKb(raw: Any?): Float {
        if (raw == null) return 0f
        return when (raw) {
            is Number -> raw.toFloat()
            is String -> {
                val str = raw.trim().uppercase()
                val numeric = Regex("""^([\d.]+)""").find(str)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                when {
                    str.contains("GB/S") -> numeric * 1024f * 1024f
                    str.contains("MB/S") || str.contains("M/S") -> numeric * 1024f
                    str.contains("KB/S") || str.contains("K/S") -> numeric
                    str.contains("B/S") -> numeric / 1024f
                    // If dimensionless decimal (e.g. "0.26" in 8081 device/info), it is in MB/s!
                    numeric in 0f..200f && str.contains(".") -> numeric * 1024f
                    else -> numeric
                }
            }
            else -> 0f
        }
    }

    fun formatTrafficGb(trafficGb: Double?): String {
        if (trafficGb == null || trafficGb < 0.0) return "--"
        return when {
            trafficGb >= 1024.0 -> String.format(Locale.US, "%.2f TB", trafficGb / 1024.0)
            trafficGb >= 1.0 -> String.format(Locale.US, "%.2f GB", trafficGb)
            else -> String.format(Locale.US, "%.1f MB", trafficGb * 1024.0)
        }
    }

    fun parseTrafficStringToGb(rawStr: String?): Double? {
        if (rawStr.isNullOrBlank()) return null
        val str = rawStr.trim().uppercase()
        val numeric = Regex("""^([\d.]+)""").find(str)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return when {
            str.contains("TB") -> numeric * 1024.0
            str.contains("GB") -> numeric
            str.contains("MB") -> numeric / 1024.0
            str.contains("KB") -> numeric / (1024.0 * 1024.0)
            str.contains("B") -> numeric / (1024.0 * 1024.0 * 1024.0)
            else -> numeric // Defaults to GB as per 8081 specification
        }
    }

    fun formatTemperature(tempStr: String?): String {
        if (tempStr.isNullOrBlank()) return "--"
        val numeric = Regex("""^([\d.]+)""").find(tempStr.trim())?.groupValues?.get(1)?.toDoubleOrNull()
        return if (numeric != null && numeric in -30.0..120.0) {
            String.format(Locale.US, "%.1f°C", numeric)
        } else {
            "--"
        }
    }

    fun formatRsrp(rsrp: Any?): String {
        val num = when (rsrp) {
            is Number -> rsrp.toInt()
            is String -> rsrp.trim().toDoubleOrNull()?.toInt()
            else -> null
        }
        return if (num != null && num in -140..-40) "$num dBm" else "--"
    }

    fun formatSinr(sinr: Any?): String {
        val num = when (sinr) {
            is Number -> sinr.toDouble()
            is String -> sinr.trim().toDoubleOrNull()
            else -> null
        }
        return if (num != null && num in -30.0..40.0) String.format(Locale.US, "%.1f dB", num) else "--"
    }

    fun formatRsrq(rsrq: Any?): String {
        val num = when (rsrq) {
            is Number -> rsrq.toInt()
            is String -> rsrq.trim().toDoubleOrNull()?.toInt()
            else -> null
        }
        return if (num != null && num in -30..0) "$num dB" else "--"
    }

    fun formatCpuUsage(usage: Any?): String {
        val num = when (usage) {
            is Number -> usage.toDouble()
            is String -> usage.trim().removeSuffix("%").toDoubleOrNull()
            else -> null
        }
        return if (num != null && num in 0.0..100.0) String.format(Locale.US, "%.1f%%", num) else "--"
    }

    fun formatRamMb(usedKb: Long, totalKb: Long): String {
        if (totalKb <= 0L) return "--"
        val usedMb = usedKb / 1024
        val totalMb = totalKb / 1024
        return "$usedMb / $totalMb MB"
    }
}
