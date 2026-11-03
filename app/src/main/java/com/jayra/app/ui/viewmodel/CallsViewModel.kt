package com.jayra.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.CallRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.CallFilter
import com.jayra.app.model.CallLog
import com.jayra.app.model.CallType
import com.jayra.app.model.CallsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CallsViewModel(
    private val callRepository: CallRepository = AppModule.callRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(CallFilter.ALL)
    val filter: StateFlow<CallFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CallsUiState> = combine(
        callRepository.getCalls(),
        _filter,
        _searchQuery,
        _errorMessage
    ) { calls, currentFilter, query, error ->
        if (error != null) {
            CallsUiState.Error(error)
        } else {
            val filteredByType = when (currentFilter) {
                CallFilter.ALL -> calls
                CallFilter.MISSED -> calls.filter { it.type == CallType.MISSED }
            }

            val filteredByQuery = if (query.isBlank()) {
                filteredByType
            } else {
                filteredByType.filter {
                    it.participantName.contains(query, ignoreCase = true)
                }
            }

            if (filteredByQuery.isEmpty() && calls.isEmpty()) {
                CallsUiState.Empty
            } else {
                CallsUiState.Success(filteredByQuery)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallsUiState.Loading)

    fun setFilter(filter: CallFilter) {
        _filter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearching.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun startVoiceCall(context: Context, participantId: String, participantName: String) {
        callRepository.startCall(context, participantId, participantName, CallType.VOICE)
    }

    fun startVideoCall(context: Context, participantId: String, participantName: String) {
        callRepository.startCall(context, participantId, participantName, CallType.VIDEO)
    }

    fun deleteCall(callId: String) {
        viewModelScope.launch {
            callRepository.deleteCall(callId)
        }
    }

    fun blockContact(userId: String) {
        viewModelScope.launch {
            callRepository.blockContact(userId)
        }
    }
}
