package com.mf650.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mf650.manager.data.model.SmsItem
import com.mf650.manager.data.repository.Mf650Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SmsUiState(
    val isLoading: Boolean = false,
    val selectedTab: Int = 0, // 0 = Inbox, 1 = Outbox
    val inboxList: List<SmsItem> = emptyList(),
    val outboxList: List<SmsItem> = emptyList(),
    val isSending: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null
)

class SmsViewModel(
    private val repository: Mf650Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmsUiState())
    val uiState: StateFlow<SmsUiState> = _uiState.asStateFlow()

    init {
        loadSms()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun loadSms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val inRes = repository.getSmsInbox()
            val outRes = repository.getSmsOutbox()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                inboxList = inRes.getOrDefault(emptyList()),
                outboxList = outRes.getOrDefault(emptyList()),
                errorMessage = if (inRes.isFailure) "获取收件箱失败" else null
            )
        }
    }

    fun sendSms(number: String, content: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            val res = repository.sendSms(number, content)
            _uiState.value = _uiState.value.copy(
                isSending = false,
                actionMessage = if (res.isSuccess) "短信发送成功" else null,
                errorMessage = if (res.isFailure) "短信发送失败: ${res.exceptionOrNull()?.message}" else null
            )
            loadSms()
        }
    }

    fun deleteSms(smsId: String) {
        viewModelScope.launch {
            val boxType = _uiState.value.selectedTab.toString()
            val res = repository.deleteSms(boxType, smsId)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(actionMessage = "短信已删除")
                loadSms()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "删除失败")
            }
        }
    }

    fun markRead(smsId: String) {
        viewModelScope.launch {
            repository.markSmsRead(smsId)
            loadSms()
        }
    }
}
