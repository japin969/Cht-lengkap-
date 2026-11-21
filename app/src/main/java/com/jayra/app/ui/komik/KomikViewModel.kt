package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikDetailResponse
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.NexadevApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class KomikUiState(
    val isLoading: Boolean = false,
    val latest: List<KomikItem> = emptyList(),
    val detail: KomikDetailResponse? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    val error: String? = null
)

class KomikViewModel : ViewModel() {
    // Sumber data: Nexadev API (https://api.nexadev.my.id) — client & base URL
    // yang sama persis dengan fitur Music yang sudah jalan di project ini.
    private val api = NexadevApi.create()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    /** Slug terakhir yang diminta, dipakai untuk retry. */
    private var lastSlug: String? = null

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.getKomikList(page = 1)
                val items = res.body()?.results.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    latest = items,
                    error = if (items.isEmpty()) "Gagal memuat daftar komik" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun retry() = loadHome()

    fun loadDetail(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Komik tidak ditemukan"
            )
            return
        }
        lastSlug = slug
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val body = api.getKomikDetail(slug = slug, type = "detail").body()
                if (body != null && body.success == true) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = body,
                        chapters = body.chapters.orEmpty(),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat detail komik"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat detail komik"
                )
            }
        }
    }

    fun retryDetail() {
        lastSlug?.let { loadDetail(it) }
    }

    fun loadStream(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Chapter tidak ditemukan"
            )
            return
        }
        lastSlug = slug
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val body = api.getKomikPages(slug = slug, type = "stream").body()
                val pages = body?.pages.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    streamTitle = body?.chapterTitle
                        ?: body?.mangaTitle
                        ?: _uiState.value.detail?.title,
                    pages = pages,
                    error = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat chapter"
                )
            }
        }
    }

    fun retryStream() {
        lastSlug?.let { loadStream(it) }
    }

    /** Pencarian komik (dipakai bila halaman search diaktifkan). */
    suspend fun search(query: String): List<KomikItem> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            api.searchKomik(query.trim()).body()?.results.orEmpty()
        }.getOrElse { emptyList() }
    }
}