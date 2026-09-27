package com.mf650.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mf650.manager.data.model.BandConfig
import com.mf650.manager.data.model.CellularStatus
import com.mf650.manager.data.repository.Mf650Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CellularUiState(
    val isLoading: Boolean = false,
    val cellularStatus: CellularStatus = CellularStatus(),
    val bandConfig: BandConfig = BandConfig(),
    val isProMode: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null
)

class CellularViewModel(
    private val repository: Mf650Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CellularUiState())
    val uiState: StateFlow<CellularUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val cellRes = repository.getCellularStatus()
            val bandRes = repository.getBandConfig()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                cellularStatus = cellRes.getOrDefault(_uiState.value.cellularStatus),
                bandConfig = bandRes.getOrDefault(_uiState.value.bandConfig),
                errorMessage = if (cellRes.isFailure) "获取蜂窝网络状态失败" else null
            )
        }
    }

    fun toggleProMode() {
        _uiState.value = _uiState.value.copy(isProMode = !_uiState.value.isProMode)
    }

    fun lockLteCell(arfcn: String, pci: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.lockLteCell(arfcn, pci)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = if (res.isSuccess) "LTE 小区已锁定: ARFCN=$arfcn, PCI=$pci" else null,
                errorMessage = res.exceptionOrNull()?.message
            )
            loadData()
        }
    }

    fun lockNrCell(arfcn: String, pci: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.lockNrCell(arfcn, pci)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = if (res.isSuccess) "5G NR 小区已锁定: ARFCN=$arfcn, PCI=$pci" else null,
                errorMessage = res.exceptionOrNull()?.message
            )
            loadData()
        }
    }

    fun unlockLte() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.unlockLteCell()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = if (res.isSuccess) "LTE 锁定已解除" else null,
                errorMessage = res.exceptionOrNull()?.message
            )
            loadData()
        }
    }

    fun unlockNr() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = repository.unlockNrCell()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                actionMessage = if (res.isSuccess) "5G NR 锁定已解除" else null,
                errorMessage = res.exceptionOrNull()?.message
            )
            loadData()
        }
    }
}
