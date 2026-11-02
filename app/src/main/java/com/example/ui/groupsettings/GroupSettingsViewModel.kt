package com.example.ui.groupsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GroupRepository
import com.example.di.AppModule
import com.example.model.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed interface GroupSettingsUiState {
    data object Loading : GroupSettingsUiState
    data class Success(val group: Group, val isSaving: Boolean = false) : GroupSettingsUiState
    data class Error(val message: String) : GroupSettingsUiState
}

class GroupSettingsViewModel(
    private val groupRepository: GroupRepository = AppModule.groupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupSettingsUiState>(GroupSettingsUiState.Loading)
    val uiState: StateFlow<GroupSettingsUiState> = _uiState.asStateFlow()

    private var currentGroupId: String = ""

    var groupName = MutableStateFlow("")
        private set
    var groupDescription = MutableStateFlow("")
        private set
    var groupAvatar = MutableStateFlow("")
        private set

    fun loadGroup(groupId: String) {
        currentGroupId = groupId
        viewModelScope.launch {
            _uiState.value = GroupSettingsUiState.Loading
            try {
                groupRepository.getGroup(groupId).collect { group ->
                    groupName.value = group.name
                    groupDescription.value = group.description
                    groupAvatar.value = group.avatarUrl
                    _uiState.value = GroupSettingsUiState.Success(group)
                }
            } catch (e: Exception) {
                _uiState.value = GroupSettingsUiState.Error(e.message ?: "Gagal memuat pengaturan grup")
            }
        }
    }

    fun onNameChange(name: String) {
        if (name.length <= 50) {
            groupName.value = name
        }
    }

    fun onDescriptionChange(desc: String) {
        if (desc.length <= 255) {
            groupDescription.value = desc
        }
    }

    fun onAvatarChange(url: String) {
        groupAvatar.value = url
    }

    fun onLockToggle(locked: Boolean) {
        viewModelScope.launch {
            groupRepository.updateLocked(currentGroupId, locked)
        }
    }

    fun onSensitiveContentToggle(enabled: Boolean) {
        viewModelScope.launch {
            groupRepository.updateSensitiveContent(currentGroupId, enabled)
        }
    }

    fun onDisappearingDurationChange(duration: String) {
        viewModelScope.launch {
            groupRepository.updateDisappearingDuration(currentGroupId, duration)
        }
    }

    fun saveSettings(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (currentState is GroupSettingsUiState.Success) {
            viewModelScope.launch {
                _uiState.value = currentState.copy(isSaving = true)
                try {
                    groupRepository.updateGroupInfo(
                        groupId = currentGroupId,
                        name = groupName.value,
                        description = groupDescription.value,
                        avatarUrl = groupAvatar.value
                    )
                    _uiState.value = currentState.copy(isSaving = false)
                    onSuccess()
                } catch (e: Exception) {
                    _uiState.value = GroupSettingsUiState.Error(e.message ?: "Gagal menyimpan perubahan")
                }
            }
        }
    }

    fun deleteGroup(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                groupRepository.deleteGroup(currentGroupId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = GroupSettingsUiState.Error(e.message ?: "Gagal menghapus grup")
            }
        }
    }
}
