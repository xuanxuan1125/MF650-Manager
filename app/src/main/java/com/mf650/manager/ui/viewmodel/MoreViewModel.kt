package com.mf650.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mf650.manager.data.model.FirewallConfig
import com.mf650.manager.data.model.LanDhcpConfig
import com.mf650.manager.data.model.ModemConfig
import com.mf650.manager.data.repository.Mf650Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MoreUiState(
    val isLoading: Boolean = false,
    val lanConfig: LanDhcpConfig = LanDhcpConfig(),
    val firewallConfig: FirewallConfig = FirewallConfig(),
    val modemConfig: ModemConfig = ModemConfig(),
    val atCommand: String = "AT+CSQ",
    val atResponse: String = "",
    val systemLogs: String = "",
    val currentSlot: String = "1",
    val simStatus: String = "卡状态正常",
    val isAirplaneMode: Boolean = false,
    val batteryCalibrationStatus: String? = null,
    val imeiInfo: String = "",
    val newImeiInput: String = "",
    val cronList: String = "",
    val forwardToken: String = "",
    val forwardChannel: String = "Bark",
    val forwardEnabled: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null
)

class MoreViewModel(
    private val repository: Mf650Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoreUiState())
    val uiState: StateFlow<MoreUiState> = _uiState.asStateFlow()

    fun updateLanConfig(config: LanDhcpConfig) {
        _uiState.value = _uiState.value.copy(lanConfig = config)
    }

    fun updateFirewallConfig(config: FirewallConfig) {
        _uiState.value = _uiState.value.copy(firewallConfig = config)
    }

    fun updateModemConfig(config: ModemConfig) {
        _uiState.value = _uiState.value.copy(modemConfig = config)
    }

    fun updateAtCommand(cmd: String) {
        _uiState.value = _uiState.value.copy(atCommand = cmd)
    }

    fun updateNewImei(imei: String) {
        _uiState.value = _uiState.value.copy(newImeiInput = imei)
    }

    fun updateForwardSettings(token: String, channel: String, enabled: Boolean) {
        _uiState.value = _uiState.value.copy(forwardToken = token, forwardChannel = channel, forwardEnabled = enabled)
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null, errorMessage = null)
    }

    fun saveLanDhcp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.applyDhcpConfig(_uiState.value.lanConfig)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = if (res.isSuccess) "DHCP 设置已保存" else null,
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun saveFirewall() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.applyFirewallConfig(_uiState.value.firewallConfig)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = if (res.isSuccess) "防火墙规则已下发" else null,
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun saveModem() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.applyModemConfig(_uiState.value.modemConfig)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = if (res.isSuccess) "调制解调器设置已保存" else null,
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun setChargeMode(mode: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.setChargeMode(mode)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun setAutoCharge(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.setAutoCharge(enabled)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun triggerBatteryCalibration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.triggerBatteryCalibration()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                batteryCalibrationStatus = res.getOrNull(),
                actionMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun toggleAirplaneMode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.toggleAirplaneMode()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAirplaneMode = !_uiState.value.isAirplaneMode,
                actionMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun loadSimStatus() {
        viewModelScope.launch {
            val res = repository.getSimStatus()
            _uiState.value = _uiState.value.copy(simStatus = res.getOrDefault("SIM 正常"))
        }
    }

    fun switchSim(slot: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.switchSimSlot(slot)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentSlot = slot,
                actionMessage = res.getOrDefault("SIM 卡已切换至卡槽 $slot"),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun loadImeiInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getImeiInfo()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                imeiInfo = res.getOrDefault("{}"),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun modifyImei(imei: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.modifyImei(imei)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
            loadImeiInfo()
        }
    }

    fun clearImeiHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.clearImeiHistory("all")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
            loadImeiInfo()
        }
    }

    fun loadCronList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getCronList()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                cronList = res.getOrDefault("[]"),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun saveCronTask(action: String, expr: String, cmd: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.saveCronTask(action, expr, cmd)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
            loadCronList()
        }
    }

    fun loadForwardConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getForwardConfig()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = null,
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun saveForwardConfig(token: String, qudao: String, enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.saveForwardConfig(token, qudao, enabled)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                forwardToken = token,
                forwardChannel = qudao,
                forwardEnabled = enabled,
                actionMessage = res.getOrNull(),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun sendAtCommand() {
        viewModelScope.launch {
            val cmd = _uiState.value.atCommand.trim()
            if (cmd.isEmpty()) return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.sendAtCommand(cmd)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                atResponse = res.getOrDefault("AT command timed out or failed"),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun loadLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.getSystemLogs()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                systemLogs = res.getOrDefault("暂无日志记录"),
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }

    fun rebootDevice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.rebootDevice()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = if (res.isSuccess) "重启指令已发送，设备正在重启..." else null,
                errorMessage = res.exceptionOrNull()?.message
            )
        }
    }
}
