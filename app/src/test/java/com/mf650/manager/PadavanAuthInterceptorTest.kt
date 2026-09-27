package com.mf650.manager

import com.mf650.manager.data.auth.PadavanAuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PadavanAuthInterceptorTest {

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
    fun testBasicAuthHeaderInjected() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        var user = "admin"
        var pass = "admin"
        val interceptor = PadavanAuthInterceptor { user to pass }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val req = Request.Builder()
            .url(server.url("/test"))
            .build()

        val resp = client.newCall(req).execute()
        assertEquals(200, resp.code)

        val recorded = server.takeRequest()
        // base64("admin:admin") == "YWRtaW46YWRtaW4="
        assertEquals("Basic YWRtaW46YWRtaW4=", recorded.getHeader("Authorization"))
    }

    @Test
    fun testDynamicCredentialsUpdate() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))

        var user = "custom_user"
        var pass = "custom_pass"
        val interceptor = PadavanAuthInterceptor { user to pass }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val req = Request.Builder()
            .url(server.url("/test2"))
            .build()

        client.newCall(req).execute()
        val recorded = server.takeRequest()

        // base64("custom_user:custom_pass") == "Y3VzdG9tX3VzZXI6Y3VzdG9tX3Bhc3M="
        assertEquals("Basic Y3VzdG9tX3VzZXI6Y3VzdG9tX3Bhc3M=", recorded.getHeader("Authorization"))
    }
}
