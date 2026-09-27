package com.mf650.manager.data.security

import java.util.regex.Pattern

object SensitiveDataRedactor {

    private val IMEI_PATTERN = Pattern.compile("\\b(\\d{6})\\d{6}(\\d{3})\\b")
    private val PHONE_PATTERN = Pattern.compile("\\b(1[3-9]\\d)\\d{4}(\\d{4})\\b")
    private val MAC_PATTERN = Pattern.compile("\\b([0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2})[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-]([0-9A-Fa-f]{2})\\b")

    fun maskImei(imei: String?): String {
        if (imei.isNullOrBlank()) return "--"
        if (imei.length < 10) return "****"
        return IMEI_PATTERN.matcher(imei).replaceAll("$1******$2")
    }

    fun redactImei(imei: String): String = maskImei(imei)

    fun maskPhone(phone: String?): String {
        if (phone.isNullOrBlank()) return "--"
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2")
    }

    fun redactPhoneNumber(phone: String): String = maskPhone(phone)

    fun maskMac(mac: String?): String {
        if (mac.isNullOrBlank()) return "--"
        return MAC_PATTERN.matcher(mac).replaceAll("$1:**:**:$2")
    }

    fun redactMac(mac: String): String = maskMac(mac)

    fun maskToken(token: String?): String {
        if (token.isNullOrBlank()) return "--"
        if (token.length <= 6) return "******"
        return token.take(3) + "******" + token.takeLast(3)
    }

    fun maskPassword(password: String?): String {
        if (password.isNullOrBlank()) return "--"
        return "••••••••"
    }

    fun redactLog(log: String): String {
        var result = IMEI_PATTERN.matcher(log).replaceAll("$1******$2")
        result = PHONE_PATTERN.matcher(result).replaceAll("$1****$2")
        result = MAC_PATTERN.matcher(result).replaceAll("$1:**:**:$2")
        return result
    }
}
