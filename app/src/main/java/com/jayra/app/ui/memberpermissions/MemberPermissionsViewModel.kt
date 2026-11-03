package com.jayra.app.ui.memberpermissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.GroupRepository
import com.jayra.app.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MemberPermissionsUiState {
    data object Loading : MemberPermissionsUiState
    data class Success(
        val permissions: Map<String, String>,
        val isUpdating: Boolean = false
    ) : MemberPermissionsUiState
    data class Error(val message: String) : MemberPermissionsUiState
}

class MemberPermissionsViewModel(
    private val groupRepository: GroupRepository = AppModule.groupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemberPermissionsUiState>(MemberPermissionsUiState.Loading)
    val uiState: StateFlow<MemberPermissionsUiState> = _uiState.asStateFlow()

    private var currentGroupId: String = ""

    fun loadPermissions(groupId: String) {
        currentGroupId = groupId
        viewModelScope.launch {
            _uiState.value = MemberPermissionsUiState.Loading
            try {
                groupRepository.getGroup(groupId).collect { group ->
                    _uiState.value = MemberPermissionsUiState.Success(
                        permissions = group.permissions
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MemberPermissionsUiState.Error(
                    e.message ?: "Gagal memuat izin anggota"
                )
            }
        }
    }

    fun updatePermission(key: String, role: String) {
        val currentState = _uiState.value
        if (currentState is MemberPermissionsUiState.Success) {
            viewModelScope.launch {
                try {
                    val updated = currentState.permissions.toMutableMap()
                    updated[key] = role
                    _uiState.value = currentState.copy(permissions = updated)
                    groupRepository.updateGroupPermission(currentGroupId, key, role)
                } catch (e: Exception) {
                    _uiState.value = MemberPermissionsUiState.Error(
                        e.message ?: "Gagal memperbarui izin"
                    )
                }
            }
        }
    }
}
