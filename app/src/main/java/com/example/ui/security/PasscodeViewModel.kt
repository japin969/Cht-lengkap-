package com.example.ui.security

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PasscodeUiState
import com.example.security.EncryptionHelper
import com.example.security.SecurityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PasscodeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PasscodeUiState>(PasscodeUiState.Loading)
    val uiState: StateFlow<PasscodeUiState> = _uiState.asStateFlow()

    private var lockCountdownJob: Job? = null

    init {
        initializeState()
    }

    fun initializeState() {
        val context = getApplication<Application>().applicationContext
        val isBiometric = EncryptionHelper.getEncryptedPref(context, "biometric_enabled")?.toBoolean() ?: true
        val remainingLockSeconds = SecurityRepository.getRemainingLockSeconds(context)

        if (remainingLockSeconds > 0) {
            _uiState.value = PasscodeUiState.Entering(
                currentInput = "",
                isBiometricEnabled = isBiometric,
                isLocked = true,
                lockRemainingSeconds = remainingLockSeconds,
                errorMessage = "Terlalu banyak percobaan salah. Tunggu $remainingLockSeconds detik."
            )
            startLockCountdown(remainingLockSeconds)
        } else {
            _uiState.value = PasscodeUiState.Entering(
                currentInput = "",
                isBiometricEnabled = isBiometric,
                isLocked = false,
                lockRemainingSeconds = 0,
                errorMessage = null
            )
        }
    }

    private fun startLockCountdown(initialSeconds: Int) {
        lockCountdownJob?.cancel()
        lockCountdownJob = viewModelScope.launch {
            var seconds = initialSeconds
            while (seconds > 0) {
                delay(1000L)
                seconds--
                val current = _uiState.value
                if (current is PasscodeUiState.Entering) {
                    if (seconds <= 0) {
                        _uiState.value = current.copy(
                            isLocked = false,
                            lockRemainingSeconds = 0,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = current.copy(
                            isLocked = true,
                            lockRemainingSeconds = seconds,
                            errorMessage = "Terlalu banyak percobaan salah. Tunggu $seconds detik."
                        )
                    }
                }
            }
        }
    }

    fun onDigitPress(digit: String, onSuccess: () -> Unit) {
        val current = _uiState.value as? PasscodeUiState.Entering ?: return
        if (current.isLocked) return

        if (current.currentInput.length < 6) {
            val newInput = current.currentInput + digit
            _uiState.value = current.copy(currentInput = newInput, errorMessage = null)

            if (newInput.length == 6) {
                validatePasscode(newInput, onSuccess)
            }
        }
    }

    fun onBackspace() {
        val current = _uiState.value as? PasscodeUiState.Entering ?: return
        if (current.isLocked) return

        if (current.currentInput.isNotEmpty()) {
            _uiState.value = current.copy(
                currentInput = current.currentInput.dropLast(1),
                errorMessage = null
            )
        }
    }

    private fun validatePasscode(input: String, onSuccess: () -> Unit) {
        val context = getApplication<Application>().applicationContext
        val storedHash = EncryptionHelper.getEncryptedPref(context, "app_passcode_hash")
        val inputHash = EncryptionHelper.hashPasswordWithSalt(input)

        // If no passcode was set before, set default or accept "123456"
        val isCorrect = if (storedHash.isNullOrEmpty()) {
            if (input == "123456" || input.length == 6) {
                EncryptionHelper.saveEncryptedPref(context, "app_passcode_hash", inputHash)
                true
            } else {
                false
            }
        } else {
            storedHash == inputHash
        }

        if (isCorrect) {
            SecurityRepository.resetFailedAttempts(context)
            _uiState.value = PasscodeUiState.Success
            onSuccess()
        } else {
            val (attempts, isLocked) = SecurityRepository.recordFailedAttempt(context)
            if (isLocked) {
                _uiState.value = PasscodeUiState.Entering(
                    currentInput = "",
                    isBiometricEnabled = (_uiState.value as? PasscodeUiState.Entering)?.isBiometricEnabled ?: true,
                    isLocked = true,
                    lockRemainingSeconds = 30,
                    errorMessage = "Terlalu banyak percobaan. Tunggu 30 detik."
                )
                startLockCountdown(30)
            } else {
                val remainingAttempts = (5 - attempts).coerceAtLeast(1)
                _uiState.value = PasscodeUiState.Entering(
                    currentInput = "",
                    isBiometricEnabled = (_uiState.value as? PasscodeUiState.Entering)?.isBiometricEnabled ?: true,
                    isLocked = false,
                    lockRemainingSeconds = 0,
                    errorMessage = "Sandi salah. Sisa percobaan: $remainingAttempts"
                )
            }
        }
    }

    fun onBiometricSuccess(onSuccess: () -> Unit) {
        val context = getApplication<Application>().applicationContext
        SecurityRepository.resetFailedAttempts(context)
        _uiState.value = PasscodeUiState.Success
        onSuccess()
    }
}
