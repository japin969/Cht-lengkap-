package com.jayra.app.ui.groupmembers

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.ContactRepository
import com.jayra.app.data.GroupRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.Group
import com.jayra.app.model.GroupMembersUiState
import com.jayra.app.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class GroupMembersViewModel(
    private val groupRepository: GroupRepository = AppModule.groupRepository,
    private val contactRepository: ContactRepository = AppModule.contactRepository
) : ViewModel() {

    private val _groupId = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<GroupMembersUiState>(GroupMembersUiState.Loading)
    val uiState: StateFlow<GroupMembersUiState> = _uiState.asStateFlow()

    fun loadGroup(groupId: String) {
        _groupId.value = groupId
        viewModelScope.launch {
            combine(
                groupRepository.getGroup(groupId),
                contactRepository.getContacts(),
                _searchQuery
            ) { group, contacts, query ->
                val allMemberIds = (group.memberIds.ifEmpty { group.members }).distinct()
                val adminIds = group.adminIds.ifEmpty { listOf(group.creatorId.ifEmpty { "user_telegram_me" }) }.toSet()

                // Map member IDs to User entities
                val allUsers = allMemberIds.map { memberId ->
                    contactRepository.getUserById(memberId)
                }

                // Filter by query if present
                val filteredUsers = if (query.isBlank()) {
                    allUsers
                } else {
                    allUsers.filter { user ->
                        user.name.contains(query, ignoreCase = true) ||
                                user.username.contains(query, ignoreCase = true) ||
                                user.phoneNumber.contains(query, ignoreCase = true)
                    }
                }

                val admins = filteredUsers.filter { adminIds.contains(it.id) }
                val regularMembers = filteredUsers.filter { !adminIds.contains(it.id) }

                if (filteredUsers.isEmpty()) {
                    GroupMembersUiState.Empty
                } else {
                    GroupMembersUiState.Success(
                        group = group,
                        admins = admins,
                        members = regularMembers,
                        isCurrentUserAdmin = group.isAdmin || adminIds.contains("user_telegram_me")
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onClearSearch() {
        _searchQuery.value = ""
    }

    fun removeMember(userId: String, context: Context? = null) {
        viewModelScope.launch {
            val gid = _groupId.value
            groupRepository.removeMember(gid, userId)
            context?.let {
                Toast.makeText(it, "Anggota telah dihapus dari grup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleAdmin(userId: String, context: Context? = null) {
        viewModelScope.launch {
            val gid = _groupId.value
            groupRepository.toggleAdmin(gid, userId)
            context?.let {
                Toast.makeText(it, "Status admin diperbarui", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onRetry() {
        val gid = _groupId.value
        if (gid.isNotEmpty()) {
            loadGroup(gid)
        }
    }
}
