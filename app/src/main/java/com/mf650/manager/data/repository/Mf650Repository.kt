package com.mf650.manager.data.repository

import com.google.gson.Gson
import com.mf650.manager.data.api.AdvancedApi
import com.mf650.manager.data.api.PadavanApi
import com.mf650.manager.data.auth.PadavanAuthInterceptor
import com.mf650.manager.data.model.AppState
import com.mf650.manager.data.model.BandConfig
import com.mf650.manager.data.model.BatteryStatus
import com.mf650.manager.data.model.CellularStatus
import com.mf650.manager.data.model.DeviceInfo
import com.mf650.manager.data.model.FirewallConfig
import com.mf650.manager.data.model.LanDhcpConfig
import com.mf650.manager.data.model.LteCellInfo
import com.mf650.manager.data.model.ModemConfig
import com.mf650.manager.data.model.NrCellInfo
import com.mf650.manager.data.model.SmsItem
import com.mf650.manager.data.model.SystemStatus
import com.mf650.manager.data.model.TrafficData
import com.mf650.manager.data.model.TrafficStatus
import com.mf650.manager.data.model.Wifi24Config
import com.mf650.manager.data.model.Wifi5Config
import com.mf650.manager.data.parser.PadavanFormBuilder
import com.mf650.manager.data.parser.PadavanParsers
import com.mf650.manager.data.security.HostAllowlistInterceptor
import com.mf650.manager.data.security.SecureCredentialStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class Mf650Repository(
    private val credentialStorage: SecureCredentialStorage? = null
) {
    private var currentHost: String = credentialStorage?.getRouterIp() ?: "192.168.100.1"

    private val _appState = MutableStateFlow(
        AppState(
            deviceReachable = false,
            padavanOnline = false,
            advancedOnline = false,
            ttydOnline = false,
            activeIp = currentHost
        )
    )
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val authInterceptor = PadavanAuthInterceptor {
        val creds = credentialStorage?.getCredentials() ?: ("admin" to "admin")
        creds.first to creds.second
    }

    private val allowlistInterceptor = HostAllowlistInterceptor { currentHost }

    private val padavanClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(allowlistInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    private val advancedClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(allowlistInterceptor)
        .build()

    private var padavanApi: PadavanApi = createPadavanApi(currentHost)
    private var advancedApi: AdvancedApi = createAdvancedApi(currentHost)

    private fun createPadavanApi(host: String): PadavanApi {
        return Retrofit.Builder()
            .baseUrl("http://$host:80/")
            .client(padavanClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PadavanApi::class.java)
    }

    private fun createAdvancedApi(host: String): AdvancedApi {
        return Retrofit.Builder()
            .baseUrl("http://$host:8081/")
            .client(advancedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdvancedApi::class.java)
    }

    fun updateRouterHost(newHost: String) {
        currentHost = newHost
        credentialStorage?.saveRouterIp(newHost)
        padavanApi = createPadavanApi(newHost)
        advancedApi = createAdvancedApi(newHost)
        _appState.value = _appState.value.copy(activeIp = newHost)
    }

    // --- Fast Polling System Status (Port 80 ~6.89ms) ---
    suspend fun getSystemStatus(): Result<SystemStatus> = withContext(Dispatchers.IO) {
        try {
            val response = padavanApi.getSystemStatusData()
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: ""
                val status = PadavanParsers.parseSystemStatus(body)
                _appState.value = _appState.value.copy(
                    deviceReachable = true,
                    padavanOnline = true,
                    cellularConnected = status.simCardReady && (status.rsrp4g != 0 || status.rsrp5g != 0),
                    errorMessage = null
                )
                Result.success(status)
            } else {
                _appState.value = _appState.value.copy(padavanOnline = false)
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            _appState.value = _appState.value.copy(padavanOnline = false)
            Result.failure(e)
        }
    }

    // --- Cellular Status with 8081 Primary -> 80 Fallback ---
    suspend fun getCellularStatus(): Result<CellularStatus> = withContext(Dispatchers.IO) {
        try {
            val resp8081 = advancedApi.getCellStatus()
            if (resp8081.isSuccessful && resp8081.body() != null) {
                _appState.value = _appState.value.copy(advancedOnline = true, deviceReachable = true)
                return@withContext Result.success(resp8081.body()!!)
            }
        } catch (_: Exception) {
            _appState.value = _appState.value.copy(advancedOnline = false)
        }

        // Fallback to Padavan Port 80
        try {
            val resp80 = padavanApi.getSystemStatusData()
            if (resp80.isSuccessful) {
                val body = resp80.body()?.string() ?: ""
                val sysStatus = PadavanParsers.parseSystemStatus(body)
                _appState.value = _appState.value.copy(padavanOnline = true, deviceReachable = true)
                val fallback = CellularStatus(
                    status = 1,
                    network = sysStatus.networkType.ifEmpty { "LTE/5G" },
                    lte = LteCellInfo(
                        rsrp = sysStatus.rsrp4g,
                        rsrq = sysStatus.rsrq4g
                    ),
                    nr = NrCellInfo(
                        rsrp = sysStatus.rsrp5g,
                        rsrq = sysStatus.rsrq5g
                    )
                )
                return@withContext Result.success(fallback)
            }
        } catch (e: Exception) {
            _appState.value = _appState.value.copy(padavanOnline = false)
            return@withContext Result.failure(e)
        }

        Result.failure(Exception("无法获取蜂窝基站状态"))
    }

    // --- Device Overview ---
    suspend fun getDeviceInfo(): Result<DeviceInfo> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getDeviceInfo()
            if (resp.isSuccessful && resp.body() != null) {
                _appState.value = _appState.value.copy(advancedOnline = true, deviceReachable = true)
                return@withContext Result.success(resp.body()!!)
            }
        } catch (_: Exception) {
            _appState.value = _appState.value.copy(advancedOnline = false)
        }

        // Fallback: build minimal DeviceInfo from Port 80
        try {
            val sysResp = padavanApi.getSystemStatusData()
            if (sysResp.isSuccessful) {
                _appState.value = _appState.value.copy(padavanOnline = true, deviceReachable = true)
                return@withContext Result.success(DeviceInfo(success = true))
            }
        } catch (e: Exception) {
            _appState.value = _appState.value.copy(deviceReachable = false)
            return@withContext Result.failure(e)
        }
        Result.failure(Exception("获取设备概览信息失败"))
    }

    // --- Battery Status & State with Failover ---
    suspend fun getBatteryStatus(): Result<BatteryStatus> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getBatteryStatus()
            if (resp.isSuccessful && resp.body() != null) {
                return@withContext Result.success(resp.body()!!)
            }
        } catch (_: Exception) {}

        // Fallback to Port 80 system status
        try {
            val sysResp = padavanApi.getSystemStatusData()
            if (sysResp.isSuccessful) {
                val sys = PadavanParsers.parseSystemStatus(sysResp.body()?.string() ?: "")
                return@withContext Result.success(
                    BatteryStatus(
                        status = "success",
                        battery = com.mf650.manager.data.model.BatterySubData(
                            level = "${sys.batteryLevelBar}/6",
                            voltage = null
                        ),
                        charging = com.mf650.manager.data.model.ChargingSubData(
                            enabled = if (sys.isBatteryCharging) 1 else 0,
                            status = if (sys.isBatteryCharging) "充电中" else "未充电"
                        )
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        Result.failure(Exception("无法获取电池状态"))
    }

    suspend fun getBatteryState(): Result<com.mf650.manager.data.parser.BatteryState> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getBatteryStatus()
            if (resp.isSuccessful && resp.body() != null) {
                val b = resp.body()!!
                val raw = com.mf650.manager.data.parser.BatteryRawValue(
                    source = com.mf650.manager.data.parser.BatteryDataSource.PORT_8081_REST,
                    levelString = b.battery?.level,
                    voltageString = b.battery?.voltage,
                    isCharging = b.charging?.isCharging,
                    autoChargeEnabled = b.autoCharge?.isAutoChargeActive
                )
                return@withContext Result.success(com.mf650.manager.data.parser.BatteryNormalizer.normalize(raw))
            }
        } catch (_: Exception) {}

        try {
            val sysResp = padavanApi.getSystemStatusData()
            if (sysResp.isSuccessful) {
                val sys = PadavanParsers.parseSystemStatus(sysResp.body()?.string() ?: "")
                val raw = com.mf650.manager.data.parser.BatteryRawValue(
                    source = com.mf650.manager.data.parser.BatteryDataSource.PORT_80_ASP,
                    barValue = sys.batteryLevelBar,
                    isCharging = sys.isBatteryCharging
                )
                return@withContext Result.success(com.mf650.manager.data.parser.BatteryNormalizer.normalize(raw))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        Result.failure(Exception("无法获取电池状态"))
    }

    // --- Traffic Stats ---
    suspend fun getTrafficStatus(): Result<TrafficStatus> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getTrafficUsage()
            if (resp.isSuccessful && resp.body() != null) {
                return@withContext Result.success(resp.body()!!)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        Result.failure(Exception("获取流量统计失败"))
    }

    // --- Band Configuration ---
    suspend fun getBandConfig(): Result<BandConfig> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getBandConfig()
            if (resp.isSuccessful && resp.body() != null) {
                return@withContext Result.success(resp.body()!!)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        Result.failure(Exception("获取频段配置失败"))
    }

    // --- Lock / Unlock Cell ---
    suspend fun lockLteCell(arfcn: String, pci: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.lockLte(arfcn = arfcn, pci = pci)
            if (resp.isSuccessful) {
                Result.success(resp.body()?.string() ?: "LTE 小区锁定成功")
            } else {
                Result.failure(Exception("锁定失败: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun lockNrCell(arfcn: String, pci: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.lockNr(arfcn = arfcn, pci = pci)
            if (resp.isSuccessful) {
                Result.success(resp.body()?.string() ?: "5G NR 小区锁定成功")
            } else {
                Result.failure(Exception("锁定失败: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockLteCell(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.unlockLte()
            if (resp.isSuccessful) Result.success("LTE 锁定已解除") else Result.failure(Exception("解除失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockNrCell(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.unlockNr()
            if (resp.isSuccessful) Result.success("5G NR 锁定已解除") else Result.failure(Exception("解除失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Battery Controls ---
    suspend fun setChargeMode(mode: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.controlCharge(mode)
            if (resp.isSuccessful) Result.success("供电模式切换成功") else Result.failure(Exception("切换失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setAutoCharge(enabled: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.setAutoCharge(if (enabled) 1 else 0)
            if (resp.isSuccessful) Result.success("自动充电设置成功") else Result.failure(Exception("设置失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SMS Management (Port 80) ---
    suspend fun getSmsInbox(): Result<List<SmsItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = padavanApi.getSmsInbox()
            if (resp.isSuccessful) {
                val list = PadavanParsers.parseSmsArray(resp.body()?.string() ?: "", 0)
                Result.success(list)
            } else {
                Result.failure(Exception("获取收件箱失败: ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmsOutbox(): Result<List<SmsItem>> = withContext(Dispatchers.IO) {
        try {
            val resp = padavanApi.getSmsOutbox()
            if (resp.isSuccessful) {
                val list = PadavanParsers.parseSmsArray(resp.body()?.string() ?: "", 1)
                Result.success(list)
            } else {
                Result.failure(Exception("获取发件箱失败: ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendSms(number: String, content: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildSendSmsForm(number, content)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("发送失败: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSms(boxType: String, smsId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildDeleteSmsForm(boxType, smsId)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("删除失败: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markSmsRead(smsId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildMarkSmsReadForm(smsId)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("标记失败: ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Wi-Fi Configuration ---
    suspend fun applyWifi24Config(config: Wifi24Config): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildWifi24Form(config)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("2.4G Wi-Fi 保存失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyWifi5Config(config: Wifi5Config): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildWifi5Form(config)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("5G Wi-Fi 保存失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- LAN / DHCP Configuration ---
    suspend fun applyDhcpConfig(config: LanDhcpConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildDhcpForm(config)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("DHCP 保存失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyLanConfig(ip: String, netmask: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildLanForm(ip, netmask)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("LAN IP 保存失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Modem / APN ---
    suspend fun applyModemConfig(config: ModemConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildModemSettingsForm(config)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("调制解调器设置保存失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Firewall ---
    suspend fun applyFirewallConfig(config: FirewallConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildFirewallForm(config)
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("防火墙设置保存失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- System Reboot (R3) ---
    suspend fun rebootDevice(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val form = PadavanFormBuilder.buildRebootForm()
            val resp = padavanApi.applyCgi(form)
            if (resp.isSuccessful) Result.success(true) else Result.failure(Exception("重启指令失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- AT Command Debug (R4) ---
    suspend fun sendAtCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.sendAtCommand(command)
            if (resp.isSuccessful) {
                Result.success(resp.body()?.string() ?: "OK")
            } else {
                Result.failure(Exception("AT 执行失败: HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Battery Calibration (R1) ---
    suspend fun triggerBatteryCalibration(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.triggerBatteryCalibration()
            if (resp.isSuccessful) Result.success("库仑计校准已触发，请保持连接充电器完成完整充放电周期")
            else Result.failure(Exception("校准触发失败: HTTP ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Airplane Mode (R1) ---
    suspend fun toggleAirplaneMode(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.toggleAirplaneMode()
            if (resp.isSuccessful) Result.success("飞行模式状态已切换")
            else Result.failure(Exception("飞行模式切换失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SIM Management (R2) ---
    suspend fun getSimStatus(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getSimStatus()
            if (resp.isSuccessful) Result.success(resp.body()?.string() ?: "SIM 正常")
            else Result.failure(Exception("获取 SIM 状态失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun switchSimSlot(slot: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.switchSim(slot = slot)
            if (resp.isSuccessful) Result.success("SIM 卡槽切换指令已下发，正在重置射频模块")
            else Result.failure(Exception("SIM 切换失败: HTTP ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- IMEI Management (R4) ---
    suspend fun getImeiInfo(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getImeiInfo()
            if (resp.isSuccessful) Result.success(resp.body()?.string() ?: "{}")
            else Result.failure(Exception("获取 IMEI 信息失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun modifyImei(imei: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.modifyImei(imei)
            if (resp.isSuccessful) Result.success("IMEI 写入成功，重启基带后生效")
            else Result.failure(Exception("IMEI 写入失败: HTTP ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearImeiHistory(type: String = "all"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.clearImeiHistory(type)
            if (resp.isSuccessful) Result.success("IMEI 历史记录已清除")
            else Result.failure(Exception("清除失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Cron / Scheduled Tasks (R3) ---
    suspend fun getCronList(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getCronList()
            if (resp.isSuccessful) Result.success(resp.body()?.string() ?: "[]")
            else Result.failure(Exception("获取定时任务失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCronTask(action: String, expr: String, cmd: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("action" to action, "cron_expr" to expr, "cmd" to cmd)
            val resp = advancedApi.saveCron(params)
            if (resp.isSuccessful) Result.success("定时任务保存成功")
            else Result.failure(Exception("定时任务保存失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SMS Forwarding (R2) ---
    suspend fun getForwardConfig(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = advancedApi.getForwardConfig()
            if (resp.isSuccessful) Result.success(resp.body()?.string() ?: "{}")
            else Result.failure(Exception("获取转发配置失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveForwardConfig(token: String, qudao: String, enabled: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("action" to "save", "token" to token, "qudao" to qudao, "enabled" to if (enabled) "1" else "0")
            val resp = advancedApi.saveForwardConfig(params)
            if (resp.isSuccessful) Result.success("短信转发设置已保存")
            else Result.failure(Exception("短信转发保存失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- System Logs ---
    suspend fun getSystemLogs(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = padavanApi.getSystemLog()
            if (resp.isSuccessful) {
                Result.success(resp.body()?.string() ?: "")
            } else {
                Result.failure(Exception("无法获取系统日志"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
