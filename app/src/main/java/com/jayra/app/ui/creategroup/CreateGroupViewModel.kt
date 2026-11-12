package com.jayra.app.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ChatRepository
import com.jayra.app.data.ContactRepository
import com.jayra.app.data.GroupRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CreateGroupUiState {
    data object Idle : CreateGroupUiState
    data object Loading : CreateGroupUiState
    data class Success(val groupId: String) : CreateGroupUiState
    data class Error(val message: String) : CreateGroupUiState
}

class CreateGroupViewModel(
    private val groupRepository: GroupRepository = AppModule.groupRepository,
    private val contactRepository: ContactRepository = AppModule.contactRepository,
    private val chatRepository: ChatRepository = AppModule.chatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateGroupUiState>(CreateGroupUiState.Idle)
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _groupDescription = MutableStateFlow("")
    val groupDescription: StateFlow<String> = _groupDescription.asStateFlow()

    private val _disappearingDuration = MutableStateFlow("Mati")
    val disappearingDuration: StateFlow<String> = _disappearingDuration.asStateFlow()

    private val _adminApproval = MutableStateFlow(false)
    val adminApproval: StateFlow<Boolean> = _adminApproval.asStateFlow()

    private val _inviteViaLink = MutableStateFlow(true)
    val inviteViaLink: StateFlow<Boolean> = _inviteViaLink.asStateFlow()

    private val _mediaVisibility = MutableStateFlow(true)
    val mediaVisibility: StateFlow<Boolean> = _mediaVisibility.asStateFlow()

    private val _groupPermissions = MutableStateFlow(
        mapOf(
            "send_messages" to "Semua Anggota",
            "send_media" to "Semua Anggota",
            "add_members" to "Semua Anggota",
            "pin_messages" to "Semua Anggota",
            "change_info" to "Semua Anggota"
        )
    )
    val groupPermissions: StateFlow<Map<String, String>> = _groupPermissions.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _groupAvatar = MutableStateFlow("")
    val groupAvatar: StateFlow<String> = _groupAvatar.asStateFlow()

    private val _selectedMemberIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMemberIds: StateFlow<Set<String>> = _selectedMemberIds.asStateFlow()

    val contacts: StateFlow<List<User>> = contactRepository.getContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onGroupDescriptionChange(desc: String) {
        if (desc.length <= 512) _groupDescription.value = desc
    }

    fun onDisappearingChange(value: String) { _disappearingDuration.value = value }
    fun onAdminApprovalChange(v: Boolean) { _adminApproval.value = v }
    fun onInviteViaLinkChange(v: Boolean) { _inviteViaLink.value = v }
    fun onMediaVisibilityChange(v: Boolean) { _mediaVisibility.value = v }

    fun onPermissionChange(key: String, value: String) {
        _groupPermissions.update { it + (key to value) }
    }

    fun onGroupNameChange(newName: String) {
        if (newName.length <= 100) {
            _groupName.value = newName
        }
    }

    fun onPhotoSelected(url: String) {
        _groupAvatar.value = url
    }

    fun toggleMember(userId: String) {
        _selectedMemberIds.update { current ->
            if (current.contains(userId)) {
                current - userId
            } else {
                current + userId
            }
        }
    }

    fun removeMember(userId: String) {
        _selectedMemberIds.update { it - userId }
    }

    fun createGroup(onSuccess: (String) -> Unit) {
        val name = _groupName.value.trim()
        val members = _selectedMemberIds.value.toList()

        if (name.isBlank()) {
            _uiState.value = CreateGroupUiState.Error("Nama grup tidak boleh kosong")
            return
        }

        if (members.isEmpty()) {
            _uiState.value = CreateGroupUiState.Error("Pilih minimal satu anggota untuk grup")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateGroupUiState.Loading
            try {
                val avatar = _groupAvatar.value.ifBlank {
                    "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400"
                }
                val newGroupId = groupRepository.createGroup(
                    name = name,
                    memberIds = members,
                    avatarUrl = avatar,
                    description = _groupDescription.value.trim(),
                    permissions = _groupPermissions.value,
                    disappearingDuration = _disappearingDuration.value,
                    adminApproval = _adminApproval.value,
                    inviteViaLinkEnabled = _inviteViaLink.value,
                    mediaVisibility = _mediaVisibility.value
                )
                chatRepository.createGroupChat(
                    chatId = newGroupId,
                    name = name,
                    avatarUrl = avatar,
                    participants = listOf("user_telegram_me") + members
                )
                _uiState.value = CreateGroupUiState.Success(newGroupId)
                onSuccess(newGroupId)
            } catch (e: Exception) {
                _uiState.value = CreateGroupUiState.Error(e.message ?: "Gagal membuat grup")
            }
        }
    }

    // ============ Web Search (Pinterest via Siputzx) ============
    private val _webSearchQuery = MutableStateFlow("")
    val webSearchQuery: StateFlow<String> = _webSearchQuery.asStateFlow()

    private val _webSearchResults = MutableStateFlow<List<String>>(emptyList())
    val webSearchResults: StateFlow<List<String>> = _webSearchResults.asStateFlow()

    private val _webSearchLoading = MutableStateFlow(false)
    val webSearchLoading: StateFlow<Boolean> = _webSearchLoading.asStateFlow()

    private val siputzxApi = com.jayra.app.data.SiputzxApi.create()

    fun onWebSearchQueryChange(q: String) {
        _webSearchQuery.value = q
    }

    fun searchWebImages() {
        val q = _webSearchQuery.value.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _webSearchLoading.value = true
            try {
                val res = siputzxApi.searchPinterest(query = q)
                if (res.isSuccessful && res.body()?.status == true) {
                    _webSearchResults.value = res.body()?.data
                        ?.mapNotNull { it.imageUrl }
                        ?.distinct()
                        ?: emptyList()
                } else {
                    _webSearchResults.value = emptyList()
                }
            } catch (_: Exception) {
                _webSearchResults.value = emptyList()
            } finally {
                _webSearchLoading.value = false
            }
        }
    }

    fun clearAvatar() {
        _groupAvatar.value = ""
    }

    fun resetState() {
        _uiState.value = CreateGroupUiState.Idle
    }
}
