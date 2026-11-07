package com.jayra.app.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jayra.app.R
import com.jayra.app.data.AuthRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.AuthUiState
import com.jayra.app.model.Country
import com.jayra.app.security.EncryptionHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _availableCountries = MutableStateFlow(
        listOf(
            Country(name = "Indonesia", code = "ID", dialCode = "+62"),
            Country(name = "Amerika Serikat", code = "US", dialCode = "+1"),
            Country(name = "Inggris", code = "GB", dialCode = "+44"),
            Country(name = "India", code = "IN", dialCode = "+91"),
            Country(name = "Jepang", code = "JP", dialCode = "+81")
        )
    )
    val availableCountries: StateFlow<List<Country>> = _availableCountries.asStateFlow()

    private val _selectedCountry = MutableStateFlow<Country?>(null)
    val selectedCountry: StateFlow<Country?> = _selectedCountry.asStateFlow()

    private val _generatedPhoneNumber = MutableStateFlow("")
    val generatedPhoneNumber: StateFlow<String> = _generatedPhoneNumber.asStateFlow()

    private val _loginHeaderSubtitle = MutableStateFlow(
        "Masuk ke akun Jayra Anda untuk menyinkronkan pesan terenkripsi di seluruh perangkat."
    )
    val loginHeaderSubtitle: StateFlow<String> = _loginHeaderSubtitle.asStateFlow()

    private val _registerHeaderSubtitle = MutableStateFlow(
        "Daftarkan email dan pilih negara untuk mendapatkan nomor Jayra otomatis."
    )
    val registerHeaderSubtitle: StateFlow<String> = _registerHeaderSubtitle.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun onCountrySelected(country: Country) {
        _selectedCountry.value = country
        _generatedPhoneNumber.value = generatePhoneNumberForCountry(country)
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun generatePhoneNumberForCountry(country: Country): String {
        return when (country.code) {
            "ID" -> "+6281" + (10000000..99999999).random()
            "US" -> "+1202" + (1000000..9999999).random()
            "GB" -> "+447" + (100000000..999999999).random()
            "IN" -> "+919" + (100000000..999999999).random()
            "JP" -> "+8190" + (10000000..99999999).random()
            else -> "${country.dialCode}${(10000000..99999999).random()}"
        }
    }

    fun attemptSilentAutoSignIn(context: Context) {
        if (auth.currentUser != null) {
            _uiState.value = AuthUiState.Success
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return
        }
        if (clientId.isBlank()) return

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    persistEncryptedSession(context)
                    _uiState.value = AuthUiState.Success
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Silent auto sign-in unavailable; remain in Idle state for interactive sign-in
            }
        }
    }

    fun onRegisterClick(
        context: Context,
        emailInput: String = _email.value,
        passwordInput: String = _password.value
    ) {
        val cleanEmail = emailInput.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.value = AuthUiState.Error("Format email tidak valid")
            return
        }
        if (passwordInput.length < 6) {
            _uiState.value = AuthUiState.Error("Kata sandi minimal 6 karakter")
            return
        }
        val country = _selectedCountry.value
        val phone = _generatedPhoneNumber.value
        if (country == null || phone.isBlank()) {
            _uiState.value = AuthUiState.Error("Silakan pilih negara terlebih dahulu")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.signUp(cleanEmail, passwordInput).fold(
                onSuccess = {
                    try {
                        auth.currentUser?.getIdToken(true)?.await()
                        val saveResult = authRepository.saveRegisteredUser(
                            email = cleanEmail,
                            phoneNumber = phone,
                            countryName = country.name
                        )
                        saveResult.fold(
                            onSuccess = {
                                persistEncryptedSession(
                                    context,
                                    phoneNumber = phone,
                                    countryName = country.name
                                )
                                _uiState.value = AuthUiState.Success
                            },
                            onFailure = { error ->
                                _uiState.value = AuthUiState.Error(
                                    error.localizedMessage ?: "Akun dibuat, tapi gagal menyimpan profil"
                                )
                            }
                        )
                    } catch (e: Exception) {
                        _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Gagal menyimpan profil")
                    }
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.localizedMessage ?: "Registrasi gagal"
                    )
                }
            )
        }
    }

    fun onLoginClick(
        context: Context,
        emailInput: String = _email.value,
        passwordInput: String = _password.value
    ) {
        val cleanEmail = emailInput.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() || passwordInput.length < 6) {
            _uiState.value = AuthUiState.Error("Email atau password salah")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.signIn(cleanEmail, passwordInput).fold(
                onSuccess = {
                    try {
                        // Tunggu Auth state benar-benar terpropagasi ke Firestore
                        auth.currentUser?.getIdToken(true)?.await()
                        ensureFirestoreAccountExists(fallbackEmail = cleanEmail)
                        persistEncryptedSession(context)
                        _uiState.value = AuthUiState.Success
                    } catch (e: Exception) {
                        _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Gagal memuat profil pengguna")
                    }
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(
                        error.localizedMessage ?: "Email atau password salah"
                    )
                }
            )
        }
    }

    @Suppress("unused")
    fun onGoogleSignInClick(context: Context) {
        _uiState.value = AuthUiState.Loading
        authenticateAndComplete(
            context = context,
            onAuthenticated = {
                val currentEmail = auth.currentUser?.email ?: _email.value.trim().ifBlank { "pengguna@jayra.app" }
                ensureFirestoreAccountExists(fallbackEmail = currentEmail)
                persistEncryptedSession(context)
                _uiState.value = AuthUiState.Success
            }
        )
    }

    private fun authenticateAndComplete(
        context: Context,
        onAuthenticated: suspend () -> Unit
    ) {
        if (auth.currentUser != null) {
            viewModelScope.launch {
                onAuthenticated()
            }
            return
        }

        val activity = context as? Activity
        if (activity == null) {
            _uiState.value = AuthUiState.Error("Konteks aktivitas tidak tersedia untuk autentikasi")
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            _uiState.value = AuthUiState.Error("Konfigurasi Google Sign-In (default_web_client_id) belum tersedia")
            return
        }

        val credentialManager = CredentialManager.create(context)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onAuthenticated()
                } else {
                    _uiState.value = AuthUiState.Error("Tipe kredensial tidak dikenali")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                _uiState.value = AuthUiState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Email atau password salah")
            }
        }
    }

    private suspend fun ensureFirestoreAccountExists(fallbackEmail: String) {
        try {
            val existing = authRepository.getUserAccount().getOrNull()
            if (existing == null) {
                val defaultCountry = _selectedCountry.value ?: _availableCountries.value.first()
                val phone = _generatedPhoneNumber.value.ifBlank {
                    generatePhoneNumberForCountry(defaultCountry)
                }
                authRepository.saveRegisteredUser(
                    email = fallbackEmail,
                    phoneNumber = phone,
                    countryName = defaultCountry.name
                )
            }
        } catch (_: Exception) {
            // Handled via structured FirestoreErrorHandler inside AuthRepository
        }
    }

    private fun persistEncryptedSession(
        context: Context,
        phoneNumber: String? = null,
        countryName: String? = null
    ) {
        val uid = auth.currentUser?.uid ?: return
        EncryptionHelper.saveEncryptedPref(context.applicationContext, KEY_SESSION_UID, uid)
        if (!phoneNumber.isNullOrBlank()) {
            EncryptionHelper.saveEncryptedPref(context.applicationContext, KEY_SESSION_PHONE, phoneNumber)
        }
        if (!countryName.isNullOrBlank()) {
            EncryptionHelper.saveEncryptedPref(context.applicationContext, KEY_SESSION_COUNTRY, countryName)
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        auth.signOut()
        EncryptionHelper.removeEncryptedPref(context.applicationContext, KEY_SESSION_UID)
        EncryptionHelper.removeEncryptedPref(context.applicationContext, KEY_SESSION_PHONE)
        EncryptionHelper.removeEncryptedPref(context.applicationContext, KEY_SESSION_COUNTRY)
        val credentialManager = CredentialManager.create(context)
        viewModelScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                _uiState.value = AuthUiState.Idle
                onComplete()
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    companion object {
        private const val TAG = "Auth"
        private const val KEY_SESSION_UID = "auth_session_uid"
        private const val KEY_SESSION_PHONE = "auth_session_phone"
        private const val KEY_SESSION_COUNTRY = "auth_session_country"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                AuthViewModel(AppModule.provideAuthRepository(app))
            }
        }
    }
}
