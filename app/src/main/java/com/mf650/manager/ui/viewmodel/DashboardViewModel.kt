package com.mf650.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mf650.manager.data.model.AppState
import com.mf650.manager.data.model.BatteryStatus
import com.mf650.manager.data.model.CellularStatus
import com.mf650.manager.data.model.DeviceInfo
import com.mf650.manager.data.model.SystemStatus
import com.mf650.manager.data.model.TrafficStatus
import com.mf650.manager.data.parser.BatteryDataSource
import com.mf650.manager.data.parser.BatteryState
import com.mf650.manager.data.parser.UnitNormalizer
import com.mf650.manager.data.repository.Mf650Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val systemStatus: SystemStatus = SystemStatus(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val cellularStatus: CellularStatus = CellularStatus(),
    val batteryStatus: BatteryStatus = BatteryStatus(),
    val batteryState: BatteryState = BatteryState(
        percent = null,
        displayPercent = "--",
        voltageVolts = null,
        voltageDisplay = "--",
        isCharging = false,
        autoChargeEnabled = false,
        isValid = false,
        source = BatteryDataSource.PORT_8081_REST
    ),
    val trafficStatus: TrafficStatus = TrafficStatus(),
    val downloadSpeeds: List<Float> = List(30) { 0f },
    val uploadSpeeds: List<Float> = List(30) { 0f },
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val repository: Mf650Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val appState: StateFlow<AppState> = repository.appState

    private var pollingJob: Job? = null
    private val maxSpeedHistory = 60

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                // 1. Fast polling: System Status via Port 80 (6.89ms) every 2 seconds
                val sysResult = repository.getSystemStatus()
                if (sysResult.isSuccess) {
                    val status = sysResult.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        systemStatus = status,
                        isOffline = false,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isOffline = true)
                }

                // 2. Battery status via BatteryNormalizer every 2 seconds
                val batStateRes = repository.getBatteryState()
                if (batStateRes.isSuccess) {
                    _uiState.value = _uiState.value.copy(batteryState = batStateRes.getOrThrow())
                }

                // 3. Cellular status via 8081 primary / 80 fallback every 2 seconds
                val cellRes = repository.getCellularStatus()
                if (cellRes.isSuccess) {
                    _uiState.value = _uiState.value.copy(cellularStatus = cellRes.getOrThrow())
                }

                // 4. Device info & Real-time speed from 8081 every 2 seconds
                val devResult = repository.getDeviceInfo()
                if (devResult.isSuccess) {
                    val dev = devResult.getOrThrow()
                    val speedObj = dev.trafficStats?.currentSpeed
                    val downKb = UnitNormalizer.parseSpeedToKb(speedObj?.download)
                    val upKb = UnitNormalizer.parseSpeedToKb(speedObj?.upload)

                    val updatedDown = (_uiState.value.downloadSpeeds + downKb).takeLast(maxSpeedHistory)
                    val updatedUp = (_uiState.value.uploadSpeeds + upKb).takeLast(maxSpeedHistory)

                    _uiState.value = _uiState.value.copy(
                        deviceInfo = dev,
                        downloadSpeeds = updatedDown,
                        uploadSpeeds = updatedUp
                    )
                }

                // 5. Traffic status via 8081 every 4 seconds (tick % 2 == 0)
                if (tick % 2 == 0) {
                    val trafficResult = repository.getTrafficStatus()
                    if (trafficResult.isSuccess) {
                        _uiState.value = _uiState.value.copy(trafficStatus = trafficResult.getOrThrow())
                    }
                }

                tick++
                delay(2000L)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getSystemStatus()
            repository.getBatteryState()
            repository.getCellularStatus()
            repository.getDeviceInfo()
            repository.getTrafficStatus()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
