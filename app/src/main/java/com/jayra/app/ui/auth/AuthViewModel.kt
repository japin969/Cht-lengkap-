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
import com.jayra.app.data.UserRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.AuthUiState
import com.jayra.app.model.Country
import com.jayra.app.model.UserAccount
import com.jayra.app.security.EncryptionHelper
import java.util.Locale
import com.jayra.app.util.findActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

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

    private val _forgotPasswordEvent = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val forgotPasswordEvent: SharedFlow<String> = _forgotPasswordEvent.asSharedFlow()

    private val _loginHeaderSubtitle = MutableStateFlow(
        "Masuk ke akun Jayra Anda untuk menyinkronkan pesan terenkripsi di seluruh perangkat."
    )
    val loginHeaderSubtitle: StateFlow<String> = _loginHeaderSubtitle.asStateFlow()

    private val _registerHeaderSubtitle = MutableStateFlow(
        "Buat username unik dan daftarkan email Anda. Nomor Jayra dibuat otomatis."
    )
    val registerHeaderSubtitle: StateFlow<String> = _registerHeaderSubtitle.asStateFlow()

    fun onUsernameChange(newUsername: String) {
        val sanitized = newUsername
            .replace(" ", "")
            .replace("\n", "")
            .removePrefix("@")
            .lowercase()
            .take(32)
        _username.value = sanitized
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

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

    private fun resolveBackgroundCountryAndPhone(): Pair<Country, String> {
        val localeCode = Locale.getDefault().country.uppercase()
        val defaultCountry = _availableCountries.value.firstOrNull { it.code == localeCode }
            ?: _availableCountries.value.first()
        val country = _selectedCountry.value ?: defaultCountry.also { _selectedCountry.value = it }
        val phone = _generatedPhoneNumber.value.ifBlank {
            generatePhoneNumberForCountry(country).also { _generatedPhoneNumber.value = it }
        }
        return country to phone
    }

    private fun validateUsername(raw: String): String? {
        val clean = raw.trim().removePrefix("@").lowercase()
        return when {
            clean.isBlank() -> "Username wajib diisi"
            clean.length < 3 -> "Username minimal 3 karakter"
            clean.length > 32 -> "Username maksimal 32 karakter"
            !USERNAME_REGEX.matches(clean) -> "Username hanya boleh huruf kecil, angka, titik, atau underscore"
            else -> null
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

    private fun mapFirebaseError(e: Throwable): String {
        return when ((e as? FirebaseAuthException)?.errorCode) {
            "ERROR_TOO_MANY_REQUESTS" -> "Terlalu banyak permintaan. Tunggu 1 menit."
            "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Cek koneksi internet Anda."
            "ERROR_USER_DISABLED" -> "Akun ini dinonaktifkan."
            "ERROR_WEAK_PASSWORD" -> "Kata sandi terlalu lemah (min 6 karakter)."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah terdaftar."
            "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD" ->
                "Email atau password salah."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Metode login ini tidak diaktifkan."
            else -> e.localizedMessage ?: "Terjadi kesalahan. Coba lagi."
        }
    }

    fun onRegisterClick(
        context: Context,
        emailInput: String = _email.value,
        passwordInput: String = _password.value,
        usernameInput: String = _username.value
    ) {
        val cleanUsername = usernameInput.trim().removePrefix("@").lowercase()
        validateUsername(cleanUsername)?.let { errMsg ->
            _uiState.value = AuthUiState.Error(errMsg)
            return
        }
        val cleanEmail = emailInput.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.value = AuthUiState.Error("Format email tidak valid")
            return
        }
        if (passwordInput.length < 6) {
            _uiState.value = AuthUiState.Error("Kata sandi minimal 6 karakter")
            return
        }
        val (country, phone) = resolveBackgroundCountryAndPhone()

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                authRepository.signUp(cleanEmail, passwordInput).fold(
                    onSuccess = { uid ->
                        try {
                            // Register wajib fail-fast, capped 10s.
                            val saveResult = withTimeoutOrNull(10_000) {
                                try {
                                    auth.currentUser?.getIdToken(true)?.await()
                                } catch (e: CancellationException) { throw e }
                                  catch (_: Exception) { }
                                authRepository.saveRegisteredUser(
                                    email = cleanEmail,
                                    phoneNumber = phone,
                                    countryName = country.name,
                                    username = cleanUsername,
                                    name = cleanUsername
                                )
                            }
                            if (saveResult == null) {
                                _uiState.value = AuthUiState.Error(
                                    "Pendaftaran timeout 10s. Cek koneksi lalu coba lagi."
                                )
                            } else saveResult.fold(
                                onSuccess = { savedUid ->
                                    val activeUid = auth.currentUser?.uid ?: savedUid
                                    val jayraId = UserRepository.deriveJayraId(activeUid)
                                    persistEncryptedSession(
                                        context = context,
                                        email = cleanEmail,
                                        username = cleanUsername,
                                        name = cleanUsername,
                                        phoneNumber = phone,
                                        countryName = country.name,
                                        jayraId = jayraId
                                    )
                                    AppModule.userRepository.syncAuthenticatedUser(
                                        uid = activeUid,
                                        email = cleanEmail,
                                        username = cleanUsername,
                                        name = cleanUsername,
                                        phoneNumber = phone,
                                        jayraId = jayraId
                                    )
                                    _uiState.value = AuthUiState.Success
                                },
                                onFailure = { error ->
                                    _uiState.value = AuthUiState.Error(mapFirebaseError(error))
                                }
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            _uiState.value = AuthUiState.Error(mapFirebaseError(e))
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = AuthUiState.Error(mapFirebaseError(error))
                    }
                )
            } finally {
                if (_uiState.value is AuthUiState.Loading) {
                    _uiState.value = AuthUiState.Error(
                        "Pendaftaran tidak selesai. Cek koneksi internet lalu coba lagi."
                    )
                }
            }
        }
    }

    fun onLoginClick(
        context: Context,
        emailInput: String = _email.value,
        passwordInput: String = _password.value
    ) {
        val cleanEmail = emailInput.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() || passwordInput.length < 6) {
            _uiState.value = AuthUiState.Error("Email atau password salah")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                authRepository.signIn(cleanEmail, passwordInput).fold(
                    onSuccess = { uid ->
                        try {
                            // Best-effort Firestore+token, capped 8s (non-critical utk navigasi).
                            // Timeout -> graceful degradation, tetap emit Success.
                            val account = withTimeoutOrNull(8_000) {
                                try {
                                    auth.currentUser?.getIdToken(true)?.await()
                                } catch (e: CancellationException) { throw e }
                                  catch (_: Exception) { }
                                ensureFirestoreAccountExists(fallbackEmail = cleanEmail)
                            }
                            val activeUid = auth.currentUser?.uid ?: uid
                            val resolvedEmail = account?.email?.takeIf { it.isNotBlank() }
                                ?: auth.currentUser?.email ?: cleanEmail
                            val resolvedUsername = account?.username?.takeIf { it.isNotBlank() }
                                ?: UserRepository.deriveUsernameFromEmail(resolvedEmail)
                            val resolvedName = account?.name?.takeIf { it.isNotBlank() }
                                ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                                ?: resolvedUsername
                            val resolvedPhone = account?.phoneNumber.orEmpty()
                            val resolvedCountry = account?.country.orEmpty()
                            val jayraId = UserRepository.deriveJayraId(activeUid)

                            persistEncryptedSession(
                                context = context,
                                email = resolvedEmail,
                                username = resolvedUsername,
                                name = resolvedName,
                                phoneNumber = resolvedPhone,
                                countryName = resolvedCountry,
                                jayraId = jayraId
                            )
                            AppModule.userRepository.syncAuthenticatedUser(
                                uid = activeUid,
                                email = resolvedEmail,
                                username = resolvedUsername,
                                name = resolvedName,
                                phoneNumber = resolvedPhone,
                                jayraId = jayraId
                            )
                            _uiState.value = AuthUiState.Success
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            _uiState.value = AuthUiState.Error(mapFirebaseError(e))
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = AuthUiState.Error(
                            mapFirebaseError(error)
                        )
                    }
                )
            } finally {
                if (_uiState.value is AuthUiState.Loading) {
                    _uiState.value = AuthUiState.Error(
                        "Login tidak selesai. Cek koneksi internet lalu coba lagi."
                    )
                }
            }
        }
    }

    fun onForgotPasswordClick(emailInput: String = _email.value) {
        val cleanEmail = emailInput.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.value = AuthUiState.Error("Format email tidak valid")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                authRepository.sendPasswordResetEmail(cleanEmail).fold(
                    onSuccess = {
                        _uiState.value = AuthUiState.Idle
                        _forgotPasswordEvent.emit(
                            "Email reset password terkirim ke $cleanEmail. Cek inbox Anda."
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = AuthUiState.Error(mapFirebaseError(error))
                    }
                )
            } finally {
                if (_uiState.value is AuthUiState.Loading) {
                    _uiState.value = AuthUiState.Error(
                        "Reset password tidak selesai. Cek koneksi lalu coba lagi."
                    )
                }
            }
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
                try {
                    onAuthenticated()
                } finally {
                    if (_uiState.value is AuthUiState.Loading) {
                        _uiState.value = AuthUiState.Idle
                    }
                }
            }
            return
        }

        val activity = context.findActivity()
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
                Log.w(TAG, "Google Sign-In cancelled")
                _uiState.value = AuthUiState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val code = (e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName
                Log.e(TAG, "Google Sign-In failed: $code")
                _uiState.value = AuthUiState.Error(mapFirebaseError(e))
            } finally {
                if (_uiState.value is AuthUiState.Loading) {
                    _uiState.value = AuthUiState.Idle
                }
            }
        }
    }

    private suspend fun ensureFirestoreAccountExists(fallbackEmail: String): UserAccount? {
        return try {
            val uid = auth.currentUser?.uid.orEmpty()
            val existing = authRepository.getUserAccount().getOrNull()
            val (bgCountry, bgPhone) = resolveBackgroundCountryAndPhone()
            if (existing == null) {
                val fallbackUsername = UserRepository.deriveUsernameFromEmail(fallbackEmail)
                val fallbackName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: fallbackUsername
                authRepository.saveRegisteredUser(
                    email = fallbackEmail,
                    phoneNumber = bgPhone,
                    countryName = bgCountry.name,
                    username = fallbackUsername,
                    name = fallbackName
                )
                UserAccount(
                    id = uid,
                    userId = uid,
                    email = fallbackEmail,
                    username = fallbackUsername,
                    name = fallbackName,
                    phoneNumber = bgPhone,
                    country = bgCountry.name
                )
            } else {
                val resolvedEmail = existing.email.ifBlank { fallbackEmail }
                val missingUsername = existing.username.isBlank()
                val missingName = existing.name.isBlank()
                val missingPhone = existing.phoneNumber.isBlank()
                val resolvedUsername = existing.username.ifBlank {
                    UserRepository.deriveUsernameFromEmail(resolvedEmail)
                }
                val resolvedName = existing.name.ifBlank {
                    auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: resolvedUsername
                }
                val resolvedPhone = existing.phoneNumber.ifBlank { bgPhone }
                val resolvedCountry = existing.country.ifBlank { bgCountry.name }
                if (missingUsername || missingName || missingPhone) {
                    authRepository.updateProfileIdentity(
                        username = resolvedUsername,
                        name = resolvedName,
                        phoneNumber = if (missingPhone) resolvedPhone else null,
                        countryName = if (existing.country.isBlank()) resolvedCountry else null
                    )
                }
                existing.copy(
                    email = resolvedEmail,
                    username = resolvedUsername,
                    name = resolvedName,
                    phoneNumber = resolvedPhone,
                    country = resolvedCountry
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Session persistence involves AndroidKeyStore load + AES/GCM cipher op,
     * which is BLOCKING IO (documented: KeyStore.load can take 100-500ms on
     * low-end devices, worse on cold start). Must run on IO dispatcher to keep
     * the Main thread free for Compose rendering (anti-freeze on spinner).
     */
    private suspend fun persistEncryptedSession(
        context: Context,
        email: String? = null,
        username: String? = null,
        name: String? = null,
        phoneNumber: String? = null,
        countryName: String? = null,
        jayraId: String? = null
    ) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val appCtx = context.applicationContext
        EncryptionHelper.saveEncryptedPref(appCtx, KEY_SESSION_UID, uid)
        if (!email.isNullOrBlank()) {
            EncryptionHelper.saveEncryptedPref(appCtx, KEY_SESSION_EMAIL, email)
        }
        if (!username.isNullOrBlank()) {
            EncryptionHelper.saveEncryptedPref(appCtx, KEY_SESSION_USERNAME, username)
        }
        if (!name.isNullOrBlank()) {
            EncryptionHelper.saveEncryptedPref(appCtx, KEY_SESSION_NAME, name)
        }
        if (!phoneNumber.isNullOrBlank()) {
            EncryptionHelper.saveEncryptedPref(appCtx, KEY_SESSION_PHONE, phoneNumber)
        }
        if (!countryName.isNullOrBlank()) {
            EncryptionHelper.saveEncryptedPref(appCtx, KEY_SESSION_COUNTRY, countryName)
        }
        if (!jayraId.isNullOrBlank()) {
            EncryptionHelper.saveEncryptedPref(appCtx, KEY_SESSION_JAYRA_ID, jayraId)
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        auth.signOut()
        AppModule.userRepository.clearAuthenticatedSession()
        val credentialManager = CredentialManager.create(context)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val appCtx = context.applicationContext
                    EncryptionHelper.removeEncryptedPref(appCtx, KEY_SESSION_UID)
                    EncryptionHelper.removeEncryptedPref(appCtx, KEY_SESSION_EMAIL)
                    EncryptionHelper.removeEncryptedPref(appCtx, KEY_SESSION_USERNAME)
                    EncryptionHelper.removeEncryptedPref(appCtx, KEY_SESSION_NAME)
                    EncryptionHelper.removeEncryptedPref(appCtx, KEY_SESSION_PHONE)
                    EncryptionHelper.removeEncryptedPref(appCtx, KEY_SESSION_COUNTRY)
                    EncryptionHelper.removeEncryptedPref(appCtx, KEY_SESSION_JAYRA_ID)
                }
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state: ${e.javaClass.simpleName}")
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
        private val USERNAME_REGEX = Regex("^[a-z0-9_.]{3,32}$")
        private const val KEY_SESSION_UID = "auth_session_uid"
        private const val KEY_SESSION_EMAIL = "auth_session_email"
        private const val KEY_SESSION_USERNAME = "auth_session_username"
        private const val KEY_SESSION_NAME = "auth_session_name"
        private const val KEY_SESSION_PHONE = "auth_session_phone"
        private const val KEY_SESSION_COUNTRY = "auth_session_country"
        private const val KEY_SESSION_JAYRA_ID = "auth_session_jayra_id"

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
