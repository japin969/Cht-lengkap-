package com.example.ui.security

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.TwoStepData
import com.example.model.TwoStepVerificationUiState
import com.example.security.EncryptionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TwoStepVerificationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    private val firestore: FirebaseFirestore? = com.example.di.AppModule.provideFirestore(application)

    private val currentUserId: String
        get() = auth?.currentUser?.uid ?: "user_telegram_me"

    private val _uiState = MutableStateFlow<TwoStepVerificationUiState>(TwoStepVerificationUiState.Loading)
    val uiState: StateFlow<TwoStepVerificationUiState> = _uiState.asStateFlow()

    init {
        load2FA()
    }

    fun load2FA() {
        viewModelScope.launch {
            _uiState.value = TwoStepVerificationUiState.Loading
            val context = getApplication<Application>().applicationContext

            val localPasswordHash = EncryptionHelper.getEncryptedPref(context, "two_step_password_hash")
            val localEmail = EncryptionHelper.getEncryptedPref(context, "two_step_email") ?: ""
            val localBiometric = EncryptionHelper.getEncryptedPref(context, "biometric_enabled")?.toBoolean() ?: true
            val local2FA = EncryptionHelper.getEncryptedPref(context, "two_step_enabled")?.toBoolean() ?: false

            if (firestore != null) {
                firestore.collection("users").document(currentUserId).get()
                    .addOnSuccessListener { snapshot ->
                        val remote2FA = snapshot.getBoolean("two_step_enabled") ?: local2FA
                        val remoteEmail = snapshot.getString("two_step_email") ?: localEmail
                        val remoteBiometric = snapshot.getBoolean("biometric_enabled") ?: localBiometric

                        _uiState.value = TwoStepVerificationUiState.Success(
                            TwoStepData(
                                isEnabled = remote2FA,
                                isBiometricEnabled = remoteBiometric,
                                recoveryEmail = remoteEmail,
                                hasPassword = !localPasswordHash.isNullOrEmpty()
                            )
                        )
                    }
                    .addOnFailureListener {
                        _uiState.value = TwoStepVerificationUiState.Success(
                            TwoStepData(
                                isEnabled = local2FA,
                                isBiometricEnabled = localBiometric,
                                recoveryEmail = localEmail,
                                hasPassword = !localPasswordHash.isNullOrEmpty()
                            )
                        )
                    }
            } else {
                _uiState.value = TwoStepVerificationUiState.Success(
                    TwoStepData(
                        isEnabled = local2FA,
                        isBiometricEnabled = localBiometric,
                        recoveryEmail = localEmail,
                        hasPassword = !localPasswordHash.isNullOrEmpty()
                    )
                )
            }
        }
    }

    fun onPasswordChange(
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{12,}$".toRegex()
        if (!passwordRegex.matches(password)) {
            onError("Kata sandi harus minimal 12 karakter dan kombinasi huruf besar, kecil, angka, serta simbol khusus (@$!%*?&).")
            return
        }

        val context = getApplication<Application>().applicationContext
        val hashed = EncryptionHelper.hashPasswordWithSalt(password)
        EncryptionHelper.saveEncryptedPref(context, "two_step_password_hash", hashed)

        val currentState = (_uiState.value as? TwoStepVerificationUiState.Success)?.data
        if (currentState != null) {
            _uiState.value = TwoStepVerificationUiState.Success(
                currentState.copy(hasPassword = true)
            )
        }
        onSuccess()
    }

    fun onEmailChange(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            onError("Format email tidak valid. Masukkan alamat email yang benar.")
            return
        }

        val context = getApplication<Application>().applicationContext
        EncryptionHelper.saveEncryptedPref(context, "two_step_email", cleanEmail)

        try {
            firestore?.collection("users")?.document(currentUserId)?.update("two_step_email", cleanEmail)
        } catch (_: Exception) {}

        val currentState = (_uiState.value as? TwoStepVerificationUiState.Success)?.data
        if (currentState != null) {
            _uiState.value = TwoStepVerificationUiState.Success(
                currentState.copy(recoveryEmail = cleanEmail)
            )
        }
        onSuccess()
    }

    fun onBiometricToggle(enabled: Boolean) {
        val context = getApplication<Application>().applicationContext
        EncryptionHelper.saveEncryptedPref(context, "biometric_enabled", enabled.toString())

        try {
            firestore?.collection("users")?.document(currentUserId)?.update("biometric_enabled", enabled)
        } catch (_: Exception) {}

        val currentState = (_uiState.value as? TwoStepVerificationUiState.Success)?.data
        if (currentState != null) {
            _uiState.value = TwoStepVerificationUiState.Success(
                currentState.copy(isBiometricEnabled = enabled)
            )
        }
    }

    fun onToggle2FA(
        enabled: Boolean,
        onError: (String) -> Unit
    ) {
        val currentState = (_uiState.value as? TwoStepVerificationUiState.Success)?.data ?: return

        if (enabled) {
            if (!currentState.hasPassword) {
                onError("Harap atur Kata Sandi Kuat terlebih dahulu.")
                return
            }
            if (currentState.recoveryEmail.isEmpty()) {
                onError("Harap atur Email Pemulihan terlebih dahulu.")
                return
            }
        }

        val context = getApplication<Application>().applicationContext
        EncryptionHelper.saveEncryptedPref(context, "two_step_enabled", enabled.toString())

        try {
            firestore?.collection("users")?.document(currentUserId)?.update("two_step_enabled", enabled)
        } catch (_: Exception) {}

        _uiState.value = TwoStepVerificationUiState.Success(
            currentState.copy(isEnabled = enabled)
        )
    }
}
