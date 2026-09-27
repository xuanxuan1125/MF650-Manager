package com.mf650.manager

import com.mf650.manager.data.security.HostAllowlistInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class HostAllowlistInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testAllowedHostPasses() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("Allowed"))

        val allowedHost = server.hostName
        val interceptor = HostAllowlistInterceptor { allowedHost }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val req = Request.Builder()
            .url(server.url("/check"))
            .build()

        val resp = client.newCall(req).execute()
        assertEquals(200, resp.code)
    }

    @Test(expected = IOException::class)
    fun testDisallowedHostThrowsIOException() {
        val interceptor = HostAllowlistInterceptor { "192.168.100.1" }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val req = Request.Builder()
            .url("http://external-malicious-site.com/steal-creds")
            .build()

        client.newCall(req).execute()
    }
}
