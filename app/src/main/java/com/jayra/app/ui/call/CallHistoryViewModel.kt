package com.jayra.app.ui.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.di.AppModule
import com.jayra.app.model.CallFilter
import com.jayra.app.model.CallHistoryUiState
import com.jayra.app.model.CallLog
import com.jayra.app.model.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallHistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val callRepository = AppModule.callRepository

    private val _uiState = MutableStateFlow<CallHistoryUiState>(CallHistoryUiState.Loading)
    val uiState: StateFlow<CallHistoryUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(CallFilter.ALL)
    val selectedTab: StateFlow<CallFilter> = _selectedTab.asStateFlow()

    private var allCalls = listOf<CallLog>()

    init {
        loadCalls()
    }

    fun loadCalls() {
        viewModelScope.launch {
            _uiState.value = CallHistoryUiState.Loading
            try {
                callRepository.callLogs.collect { logs ->
                    allCalls = logs
                    applyFilter(_selectedTab.value)
                }
            } catch (e: Exception) {
                _uiState.value = CallHistoryUiState.Error(e.localizedMessage ?: "Gagal memuat riwayat panggilan")
            }
        }
    }

    fun onTabChange(tab: CallFilter) {
        _selectedTab.value = tab
        applyFilter(tab)
    }

    private fun applyFilter(tab: CallFilter) {
        val filtered = when (tab) {
            CallFilter.ALL -> allCalls
            CallFilter.MISSED -> allCalls.filter { it.type == CallType.MISSED }
        }

        _uiState.value = if (filtered.isEmpty()) {
            CallHistoryUiState.Empty
        } else {
            CallHistoryUiState.Success(filtered)
        }
    }
}
