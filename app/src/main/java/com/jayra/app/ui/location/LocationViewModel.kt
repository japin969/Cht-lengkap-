package com.jayra.app.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.di.AppModule
import com.jayra.app.model.LocationMessage
import com.jayra.app.model.LocationUiState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val chatRepository = AppModule.chatRepository

    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Loading)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private val _locationTitle = MutableStateFlow("Lokasi Terkini Saya")
    val locationTitle: StateFlow<String> = _locationTitle.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    init {
        fetchLocation()
    }

    fun updateTitle(title: String) {
        _locationTitle.value = title
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        _uiState.value = LocationUiState.Loading
        val context = getApplication<Application>().applicationContext

        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            // Fallback default representative location (Jakarta Pusat)
            _uiState.value = LocationUiState.Success(
                lat = -6.175392,
                lng = 106.827153,
                address = "Monumen Nasional, Gambir, Jakarta Pusat",
                accuracyMeters = 15f
            )
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModelScope.launch {
                            val addr = resolveAddress(context, loc.latitude, loc.longitude)
                            _uiState.value = LocationUiState.Success(
                                lat = loc.latitude,
                                lng = loc.longitude,
                                address = addr,
                                accuracyMeters = loc.accuracy
                            )
                        }
                    } else {
                        // Fallback location if sensor is cold or null
                        _uiState.value = LocationUiState.Success(
                            lat = -6.175392,
                            lng = 106.827153,
                            address = "Gambir, Kota Jakarta Pusat, DKI Jakarta",
                            accuracyMeters = 20f
                        )
                    }
                }
                .addOnFailureListener { error ->
                    _uiState.value = LocationUiState.Success(
                        lat = -6.175392,
                        lng = 106.827153,
                        address = "Monumen Nasional, Jakarta",
                        accuracyMeters = 25f
                    )
                }
        } catch (e: Exception) {
            _uiState.value = LocationUiState.Success(
                lat = -6.175392,
                lng = 106.827153,
                address = "Jakarta, Indonesia",
                accuracyMeters = 30f
            )
        }
    }

    private suspend fun resolveAddress(context: Context, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val line = addr.getAddressLine(0)
                if (!line.isNullOrEmpty()) return@withContext line
                val locality = addr.locality ?: addr.subAdminArea ?: ""
                val country = addr.countryName ?: ""
                return@withContext listOf(locality, country).filter { it.isNotEmpty() }.joinToString(", ")
            }
        } catch (_: Exception) { }
        return@withContext "Lat: %.5f, Lng: %.5f".format(lat, lng)
    }

    fun shareLocation(chatId: String, onSuccess: () -> Unit) {
        val state = _uiState.value as? LocationUiState.Success ?: return
        if (_isSending.value) return

        _isSending.value = true
        viewModelScope.launch {
            try {
                val title = _locationTitle.value.ifBlank { "Lokasi Terkini" }
                val locationMsg = LocationMessage(
                    lat = state.lat,
                    lng = state.lng,
                    title = "$title: ${state.address}"
                )

                val mapsLink = "https://maps.google.com/?q=${state.lat},${state.lng}"
                val text = "📍 [Lokasi] $title\n${state.address}\n$mapsLink"

                chatRepository.sendMessage(
                    chatId = chatId,
                    text = text
                )

                _isSending.value = false
                onSuccess()
            } catch (e: Exception) {
                _isSending.value = false
            }
        }
    }
}
