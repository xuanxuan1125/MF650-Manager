package com.mf650.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mf650.manager.data.model.Wifi24Config
import com.mf650.manager.data.model.Wifi5Config
import com.mf650.manager.data.repository.Mf650Repository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WifiUiState(
    val isLoading: Boolean = false,
    val wifi24Config: Wifi24Config = Wifi24Config(),
    val wifi5Config: Wifi5Config = Wifi5Config(),
    val isRestarting: Boolean = false,
    val restartCountdown: Int = 0,
    val actionSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class WifiViewModel(
    private val repository: Mf650Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    fun updateWifi24(config: Wifi24Config) {
        _uiState.value = _uiState.value.copy(wifi24Config = config)
    }

    fun updateWifi5(config: Wifi5Config) {
        _uiState.value = _uiState.value.copy(wifi5Config = config)
    }

    fun saveWifi24() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.applyWifi24Config(_uiState.value.wifi24Config)
            if (res.isSuccess) {
                startRestartCountdown("2.4G Wi-Fi 设置已保存，无线服务正在重启...")
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "保存失败"
                )
            }
        }
    }

    fun saveWifi5() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.applyWifi5Config(_uiState.value.wifi5Config)
            if (res.isSuccess) {
                startRestartCountdown("5G Wi-Fi 设置已保存，无线服务正在重启...")
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.exceptionOrNull()?.message ?: "保存失败"
                )
            }
        }
    }

    private fun startRestartCountdown(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRestarting = true,
                restartCountdown = 15,
                actionSuccessMessage = message
            )
            for (sec in 14 downTo 0) {
                delay(1000L)
                _uiState.value = _uiState.value.copy(restartCountdown = sec)
            }
            _uiState.value = _uiState.value.copy(
                isRestarting = false,
                actionSuccessMessage = "无线服务重启完成！"
            )
        }
    }
}
