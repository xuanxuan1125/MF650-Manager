package com.mf650.manager.data.api

import com.mf650.manager.data.model.BandConfig
import com.mf650.manager.data.model.BatteryStatus
import com.mf650.manager.data.model.CellularStatus
import com.mf650.manager.data.model.DeviceInfo
import com.mf650.manager.data.model.TrafficStatus
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface AdvancedApi {

    // --- Dashboard & Device Overview ---
    @GET("/api/device/info")
    suspend fun getDeviceInfo(): Response<DeviceInfo>

    // --- Cellular & Lock Cell ---
    @GET("/api/cell")
    suspend fun getCellStatus(
        @Query("ACTION") action: String = "get_status"
    ): Response<CellularStatus>

    @GET("/api/cell")
    suspend fun getBandConfig(
        @Query("ACTION") action: String = "get_band"
    ): Response<BandConfig>

    @FormUrlEncoded
    @POST("/api/cell")
    suspend fun lockLte(
        @Field("ACTION") action: String = "lock_lte",
        @Field("LTE_ARFCN") arfcn: String,
        @Field("LTE_PCI") pci: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/cell")
    suspend fun lockNr(
        @Field("ACTION") action: String = "lock_nr",
        @Field("NR_ARFCN") arfcn: String,
        @Field("NR_PCI") pci: String
    ): Response<ResponseBody>

    @GET("/api/cell")
    suspend fun unlockLte(
        @Query("ACTION") action: String = "unlock_lte"
    ): Response<ResponseBody>

    @GET("/api/cell")
    suspend fun unlockNr(
        @Query("ACTION") action: String = "unlock_nr"
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/cell")
    suspend fun setBands(
        @FieldMap fields: Map<String, String>
    ): Response<ResponseBody>

    // --- Battery & Power Management ---
    @GET("/api/device-status")
    suspend fun getBatteryStatus(): Response<BatteryStatus>

    @FormUrlEncoded
    @POST("/api/control-charge")
    suspend fun controlCharge(
        @Field("mode") mode: Int
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/set-auto-charge")
    suspend fun setAutoCharge(
        @Field("status") status: Int
    ): Response<ResponseBody>

    @POST("/api/battery-calibration")
    suspend fun triggerBatteryCalibration(): Response<ResponseBody>

    // --- Traffic & Usage ---
    @GET("/api/liuliang?traffic-usage")
    suspend fun getTrafficUsage(): Response<TrafficStatus>

    @GET("/api/liuliang?liuliang-cx")
    suspend fun getTrafficPluginStatus(): Response<ResponseBody>

    @POST("/api/liuliang?liuliang-kg")
    suspend fun toggleTrafficPlugin(): Response<ResponseBody>

    @GET("/api/liuliang?feixing-cx")
    suspend fun getAirplaneMode(): Response<ResponseBody>

    @POST("/api/liuliang?feixing-kg")
    suspend fun toggleAirplaneMode(): Response<ResponseBody>

    // --- SIM Card Management ---
    @GET("/api/sim")
    suspend fun getSimStatus(
        @Query("ACTION") action: String = "get_status"
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/sim")
    suspend fun switchSim(
        @Field("ACTION") action: String = "switch_sim",
        @Field("slot") slot: String
    ): Response<ResponseBody>

    // --- IMEI Management (R4) ---
    @GET("/api/imei?get-imei-info")
    suspend fun getImeiInfo(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/imei?modify-imei")
    suspend fun modifyImei(
        @Field("imei") imei: String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/imei?clear-history")
    suspend fun clearImeiHistory(
        @Field("type") type: String
    ): Response<ResponseBody>

    // --- AT Command Debug (R4) ---
    @FormUrlEncoded
    @POST("/api/at-debug")
    suspend fun sendAtCommand(
        @Field("atcmd") atcmd: String
    ): Response<ResponseBody>

    // --- Cron / Scheduled Tasks ---
    @GET("/api/cron")
    suspend fun getCronList(
        @Query("ACTION") action: String = "get"
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/cron")
    suspend fun saveCron(
        @FieldMap fields: Map<String, String>
    ): Response<ResponseBody>

    // --- SMS Forwarding ---
    @GET("/api/forward?action=load")
    suspend fun getForwardConfig(): Response<ResponseBody>

    @GET("/api/forward?action=status")
    suspend fun getForwardStatus(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/forward")
    suspend fun saveForwardConfig(
        @FieldMap fields: Map<String, String>
    ): Response<ResponseBody>

    // --- System & Modem Logs ---
    @GET("/api/log")
    suspend fun getSystemLogs(
        @Query("ACTION") action: String = "get"
    ): Response<ResponseBody>
}
