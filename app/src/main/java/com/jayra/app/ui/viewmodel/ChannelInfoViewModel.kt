package com.jayra.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.CallRepository
import com.jayra.app.data.ChannelRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Channel
import com.jayra.app.model.ChannelInfoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelInfoViewModel(
    private val channelRepository: ChannelRepository = AppModule.channelRepository,
    private val callRepository: CallRepository = AppModule.callRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChannelInfoUiState>(ChannelInfoUiState.Loading)
    val uiState: StateFlow<ChannelInfoUiState> = _uiState.asStateFlow()

    private var currentChannelId: String = ""

    fun loadChannel(channelId: String) {
        currentChannelId = channelId
        _uiState.value = ChannelInfoUiState.Loading
        viewModelScope.launch {
            try {
                channelRepository.getChannel(channelId).collect { channel ->
                    _uiState.value = ChannelInfoUiState.Success(channel)
                }
            } catch (e: Exception) {
                _uiState.value = ChannelInfoUiState.Error(e.message ?: "Gagal memuat info channel")
            }
        }
    }

    fun onAudioClick(context: Context) {
        val currentState = _uiState.value
        if (currentState is ChannelInfoUiState.Success) {
            callRepository.startChannelVoiceStream(context, currentChannelId, currentState.channel.name)
        }
    }

    fun onVideoClick(context: Context) {
        val currentState = _uiState.value
        if (currentState is ChannelInfoUiState.Success) {
            callRepository.startChannelVideoStream(context, currentChannelId, currentState.channel.name)
        }
    }

    fun onMuteToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                channelRepository.updateMute(currentChannelId, enabled)
            } catch (e: Exception) {
                _uiState.value = ChannelInfoUiState.Error(e.message ?: "Gagal memperbarui bisukan")
            }
        }
    }

    fun onCustomNotificationToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                channelRepository.updateCustomNotification(currentChannelId, enabled)
            } catch (e: Exception) {
                _uiState.value = ChannelInfoUiState.Error(e.message ?: "Gagal memperbarui notifikasi")
            }
        }
    }

    fun onSignaturesToggle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                channelRepository.updateSignatures(currentChannelId, enabled)
            } catch (e: Exception) {
                _uiState.value = ChannelInfoUiState.Error(e.message ?: "Gagal memperbarui tanda tangan")
            }
        }
    }

    fun onLeaveChannel(userId: String = "user_telegram_me", onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                channelRepository.leaveChannel(currentChannelId, userId)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = ChannelInfoUiState.Error(e.message ?: "Gagal meninggalkan channel")
            }
        }
    }

    fun onDeleteChannel(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                channelRepository.deleteChannel(currentChannelId)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = ChannelInfoUiState.Error(e.message ?: "Gagal menghapus channel")
            }
        }
    }

    fun onRetry() {
        if (currentChannelId.isNotEmpty()) {
            loadChannel(currentChannelId)
        }
    }
}
