package com.jayra.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.CallRepository
import com.jayra.app.data.GroupRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Group
import com.jayra.app.model.GroupInfoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupInfoViewModel(
    private val groupRepository: GroupRepository = AppModule.groupRepository,
    private val callRepository: CallRepository = AppModule.callRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupInfoUiState>(GroupInfoUiState.Loading)
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private var currentGroupId: String = ""

    fun loadGroup(groupId: String) {
        currentGroupId = groupId
        _uiState.value = GroupInfoUiState.Loading
        viewModelScope.launch {
            try {
                groupRepository.getGroup(groupId).collect { group ->
                    _uiState.value = GroupInfoUiState.Success(group)
                }
            } catch (e: Exception) {
                _uiState.value = GroupInfoUiState.Error(e.message ?: "Gagal memuat info grup")
            }
        }
    }

    fun onAudioClick(context: Context) {
        val currentState = _uiState.value
        if (currentState is GroupInfoUiState.Success) {
            callRepository.startGroupVoiceCall(context, currentGroupId, currentState.group.name)
        }
    }

    fun onVideoClick(context: Context) {
        val currentState = _uiState.value
        if (currentState is GroupInfoUiState.Success) {
            callRepository.startGroupVideoCall(context, currentGroupId, currentState.group.name)
        }
    }

    fun onMuteToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                groupRepository.updateMute(currentGroupId, enabled)
            } catch (e: Exception) {
                _uiState.value = GroupInfoUiState.Error(e.message ?: "Gagal memperbarui bisukan")
            }
        }
    }

    fun onCustomNotificationToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                groupRepository.updateCustomNotification(currentGroupId, enabled)
            } catch (e: Exception) {
                _uiState.value = GroupInfoUiState.Error(e.message ?: "Gagal memperbarui notifikasi")
            }
        }
    }

    fun onLockToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                groupRepository.updateLocked(currentGroupId, enabled)
            } catch (e: Exception) {
                _uiState.value = GroupInfoUiState.Error(e.message ?: "Gagal memperbarui status kunci")
            }
        }
    }

    fun onLeaveGroup(userId: String = "user_telegram_me", onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                groupRepository.leaveGroup(currentGroupId, userId)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = GroupInfoUiState.Error(e.message ?: "Gagal keluar dari grup")
            }
        }
    }

    fun onDeleteGroup(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                groupRepository.deleteGroup(currentGroupId)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = GroupInfoUiState.Error(e.message ?: "Gagal menghapus grup")
            }
        }
    }

    fun onRetry() {
        if (currentGroupId.isNotEmpty()) {
            loadGroup(currentGroupId)
        }
    }
}
