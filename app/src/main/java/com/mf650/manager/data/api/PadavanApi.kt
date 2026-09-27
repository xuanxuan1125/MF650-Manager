package com.mf650.manager.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface PadavanApi {

    @GET("/system_status_data.asp")
    suspend fun getSystemStatusData(): Response<ResponseBody>

    @GET("/status_internet.asp")
    suspend fun getStatusInternet(): Response<ResponseBody>

    @GET("/sms_in.asp")
    suspend fun getSmsInbox(): Response<ResponseBody>

    @GET("/sms_out.asp")
    suspend fun getSmsOutbox(): Response<ResponseBody>

    @GET("/update_clients.asp")
    suspend fun getConnectedClients(): Response<ResponseBody>

    @GET("/syslog.asp")
    suspend fun getSystemLog(): Response<ResponseBody>

    @GET("/Advanced_Wireless2g_Content.asp")
    suspend fun getWireless2gPage(): Response<ResponseBody>

    @GET("/Advanced_Wireless_Content.asp")
    suspend fun getWireless5gPage(): Response<ResponseBody>

    @GET("/Advanced_LAN_Content.asp")
    suspend fun getLanPage(): Response<ResponseBody>

    @GET("/Advanced_BasicFirewall_Content.asp")
    suspend fun getFirewallPage(): Response<ResponseBody>

    @GET("/Advanced_Settings_Content.asp")
    suspend fun getModemSettingsPage(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/apply.cgi")
    suspend fun applyCgi(
        @FieldMap params: Map<String, String>
    ): Response<ResponseBody>
}
