package com.example.ui.addmember

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContactRepository
import com.example.data.GroupRepository
import com.example.di.AppModule
import com.example.model.AddMemberUiState
import com.example.model.Group
import com.example.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AddMemberViewModel(
    private val groupRepository: GroupRepository = AppModule.groupRepository,
    private val contactRepository: ContactRepository = AppModule.contactRepository
) : ViewModel() {

    private val _groupId = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedUserIds: StateFlow<Set<String>> = _selectedUserIds.asStateFlow()

    private val _uiState = MutableStateFlow<AddMemberUiState>(AddMemberUiState.Loading)
    val uiState: StateFlow<AddMemberUiState> = _uiState.asStateFlow()

    fun loadGroup(groupId: String) {
        _groupId.value = groupId
        viewModelScope.launch {
            combine(
                groupRepository.getGroup(groupId),
                contactRepository.getContacts(),
                _searchQuery,
                _selectedUserIds
            ) { group, contacts, query, selectedIds ->
                val currentMemberIds = (group.memberIds.ifEmpty { group.members }).toSet()

                // Exclude already added members
                val nonMembers = contacts.filter { !currentMemberIds.contains(it.id) }

                val filteredContacts = if (query.isBlank()) {
                    nonMembers
                } else {
                    nonMembers.filter { user ->
                        user.name.contains(query, ignoreCase = true) ||
                                user.username.contains(query, ignoreCase = true) ||
                                user.phoneNumber.contains(query, ignoreCase = true)
                    }
                }

                if (nonMembers.isEmpty()) {
                    AddMemberUiState.Empty
                } else {
                    AddMemberUiState.Success(
                        group = group,
                        availableContacts = filteredContacts,
                        selectedUserIds = selectedIds
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

    fun toggleUserSelection(userId: String) {
        val current = _selectedUserIds.value
        _selectedUserIds.value = if (current.contains(userId)) {
            current - userId
        } else {
            current + userId
        }
    }

    fun deselectUser(userId: String) {
        _selectedUserIds.value = _selectedUserIds.value - userId
    }

    fun addSelectedMembers(context: Context? = null, onSuccess: () -> Unit) {
        val toAdd = _selectedUserIds.value.toList()
        if (toAdd.isEmpty()) return

        val gid = _groupId.value
        viewModelScope.launch {
            groupRepository.addMembers(gid, toAdd)
            context?.let {
                Toast.makeText(it, "${toAdd.size} anggota baru ditambahkan", Toast.LENGTH_SHORT).show()
            }
            onSuccess()
        }
    }

    fun onRetry() {
        val gid = _groupId.value
        if (gid.isNotEmpty()) {
            loadGroup(gid)
        }
    }
}
