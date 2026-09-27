package com.mf650.manager.data.security

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Security interceptor ensuring requests are strictly directed to the approved router host.
 * Prevents SSRF or accidental routing to unauthorized third-party IPs.
 */
class HostAllowlistInterceptor(
    private val allowedHostProvider: () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestedHost = request.url.host
        val allowedHost = allowedHostProvider()

        val isPermitted = requestedHost.equals(allowedHost, ignoreCase = true) ||
                requestedHost.equals("192.168.100.1", ignoreCase = true) ||
                requestedHost.equals("127.0.0.1", ignoreCase = true) ||
                requestedHost.equals("localhost", ignoreCase = true)

        if (!isPermitted) {
            throw IOException("Security Violation: Request host '$requestedHost' is not permitted. Only MF650 gateway '$allowedHost' is allowed.")
        }

        return chain.proceed(request)
    }
}
