package com.mf650.manager.data.parser

enum class SignalGrade(val label: String, val level: Int) {
    EXCELLENT("极佳", 4),
    GOOD("良好", 3),
    FAIR("一般", 2),
    POOR("极弱", 1),
    UNKNOWN("无信号", 0)
}

data class SignalEvaluation(
    val grade: SignalGrade,
    val bars: Int, // 0 to 5
    val rsrpDescription: String,
    val sinrDescription: String,
    val rsrqDescription: String,
    val suggestion: String
)

object SignalQualityEvaluator {

    fun evaluate(rsrp: Int?, sinr: Double?, rsrq: Int?, rat: String? = null): SignalEvaluation {
        if (rsrp == null || rsrp == 0 || rsrp < -140) {
            return SignalEvaluation(
                grade = SignalGrade.UNKNOWN,
                bars = 0,
                rsrpDescription = "无有效 RSRP",
                sinrDescription = "--",
                rsrqDescription = "--",
                suggestion = "请检查天线连接或 SIM 卡状态"
            )
        }

        val rsrpGrade = when {
            rsrp >= -80 -> SignalGrade.EXCELLENT
            rsrp >= -95 -> SignalGrade.GOOD
            rsrp >= -108 -> SignalGrade.FAIR
            else -> SignalGrade.POOR
        }

        val sinrGrade = when {
            sinr == null -> rsrpGrade
            sinr >= 20.0 -> SignalGrade.EXCELLENT
            sinr >= 13.0 -> SignalGrade.GOOD
            sinr >= 3.0 -> SignalGrade.FAIR
            else -> SignalGrade.POOR
        }

        // Weighted grade: RSRP 60%, SINR 40%
        val compositeScore = (rsrpGrade.level * 0.6) + (sinrGrade.level * 0.4)
        val finalGrade = when {
            compositeScore >= 3.5 -> SignalGrade.EXCELLENT
            compositeScore >= 2.6 -> SignalGrade.GOOD
            compositeScore >= 1.6 -> SignalGrade.FAIR
            else -> SignalGrade.POOR
        }

        val bars = when (finalGrade) {
            SignalGrade.EXCELLENT -> 5
            SignalGrade.GOOD -> 4
            SignalGrade.FAIR -> 2
            SignalGrade.POOR -> 1
            SignalGrade.UNKNOWN -> 0
        }

        val rsrpDesc = when {
            rsrp >= -80 -> "$rsrp dBm (极强信号)"
            rsrp >= -95 -> "$rsrp dBm (信号良好)"
            rsrp >= -108 -> "$rsrp dBm (中等信号，边缘)"
            else -> "$rsrp dBm (信号微弱，易断连)"
        }

        val sinrDesc = if (sinr != null) {
            when {
                sinr >= 20.0 -> "$sinr dB (无干扰，超高信噪比)"
                sinr >= 13.0 -> "$sinr dB (信噪比良好)"
                sinr >= 3.0 -> "$sinr dB (轻度同频干扰)"
                else -> "$sinr dB (严重底噪与同频干扰)"
            }
        } else {
            "--"
        }

        val rsrqDesc = if (rsrq != null) {
            when {
                rsrq >= -10 -> "$rsrq dB (信道负载极低)"
                rsrq >= -15 -> "$rsrq dB (信道正常)"
                else -> "$rsrq dB (信道严重拥塞)"
            }
        } else {
            "--"
        }

        val suggestion = when (finalGrade) {
            SignalGrade.EXCELLENT -> "当前蜂窝连接品质极高，可获得最大吞吐带宽。"
            SignalGrade.GOOD -> "网络运行平稳，适合日常全场景高速联网。"
            SignalGrade.FAIR -> "建议调整设备天线朝向或放置于窗户边以减少建筑穿透衰减。"
            SignalGrade.POOR -> "信号严重受损，建议手动锁频段或切换外置天线接口。"
            SignalGrade.UNKNOWN -> "未探测到可用基站载波。"
        }

        return SignalEvaluation(
            grade = finalGrade,
            bars = bars,
            rsrpDescription = rsrpDesc,
            sinrDescription = sinrDesc,
            rsrqDescription = rsrqDesc,
            suggestion = suggestion
        )
    }
}
