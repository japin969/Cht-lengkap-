package com.jayra.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.UserRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.EditProfileUiState
import com.jayra.app.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val userRepository: UserRepository = AppModule.userRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    val nameInput = MutableStateFlow("")
    val bioInput = MutableStateFlow("")
    val usernameInput = MutableStateFlow("")
    val avatarUrlInput = MutableStateFlow("")

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                userRepository.getUserProfile().collect { user ->
                    nameInput.value = user.name
                    bioInput.value = user.bio
                    usernameInput.value = user.username
                    avatarUrlInput.value = user.avatarUrl
                    _uiState.value = EditProfileUiState.Success(user)
                }
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.Error(e.message ?: "Gagal memuat profil")
            }
        }
    }

    fun onNameChange(name: String) {
        nameInput.value = name
    }

    fun onBioChange(bio: String) {
        bioInput.value = bio
    }

    fun onUsernameChange(username: String) {
        usernameInput.value = username.removePrefix("@").trim()
    }

    fun onImagePick(uriString: String) {
        avatarUrlInput.value = uriString
        viewModelScope.launch {
            userRepository.updateAvatar(uriString)
        }
    }

    fun onSaveClick(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedName = nameInput.value.trim()
        val trimmedUsername = usernameInput.value.trim()
        val trimmedBio = bioInput.value.trim()

        if (trimmedName.isBlank()) {
            onError("Nama tidak boleh kosong")
            return
        }

        if (trimmedUsername.length < 3) {
            onError("Username minimal 3 karakter")
            return
        }

        viewModelScope.launch {
            val current = (_uiState.value as? EditProfileUiState.Success)?.user
            _uiState.value = EditProfileUiState.Success(
                user = current ?: User(),
                isSaving = true
            )
            try {
                userRepository.updateProfile(
                    name = trimmedName,
                    bio = trimmedBio,
                    username = trimmedUsername
                )
                _uiState.value = EditProfileUiState.Success(
                    user = (current ?: User()).copy(
                        name = trimmedName,
                        bio = trimmedBio,
                        username = trimmedUsername,
                        avatarUrl = avatarUrlInput.value
                    ),
                    isSaving = false
                )
                onSuccess("Profil diperbarui")
            } catch (e: Exception) {
                _uiState.value = EditProfileUiState.Success(
                    user = current ?: User(),
                    isSaving = false
                )
                onError(e.message ?: "Gagal memperbarui profil")
            }
        }
    }
}
