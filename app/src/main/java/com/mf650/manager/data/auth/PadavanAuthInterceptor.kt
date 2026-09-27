package com.mf650.manager.data.auth

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Thread-safe HTTP Basic Auth Interceptor for Port 80 Padavan Web Management.
 */
class PadavanAuthInterceptor(
    private val credentialsProvider: () -> Pair<String, String>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val (username, password) = credentialsProvider()
        val authHeader = Credentials.basic(username, password, Charsets.UTF_8)
        
        val request = chain.request().newBuilder()
            .header("Authorization", authHeader)
            .build()
            
        return chain.proceed(request)
    }
}
