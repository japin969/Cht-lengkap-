package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikDetailResponse
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.KomikStreamResponse
import com.jayra.app.data.NexadevApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class KomikUiState(
    val isLoading: Boolean = false,
    val latest: List<KomikItem> = emptyList(),
    val popular: List<KomikItem> = emptyList(),
    val detail: KomikDetailResponse? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    /** Error untuk aksi detail/stream; tidak mengganggu home. */
    val detailError: String? = null,
    /** Menandakan detail/stream sedang dimuat. */
    val detailLoading: Boolean = false,
    /** Error khusus home. */
    val homeError: String? = null
)

class KomikViewModel : ViewModel() {
    // Sumber data: Nexadev API (https://api.nexadev.my.id) — client & base URL
    // yang sudah ada di project, sama persis dengan fitur lain.
    private val api = NexadevApi.create()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    /** Slug terakhir yang diminta, dipakai untuk retry. */
    private var lastSlug: String? = null

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            try {
                val res = api.getKomikList(page = 1)
                val mapped = res.body()?.results.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    latest = mapped,
                    popular = mapped,
                    homeError = if (mapped.isEmpty()) "Daftar komik kosong" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    homeError = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    /** Muat satu halaman daftar komik. */
    fun loadPage(page: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            try {
                val mapped = api.getKomikList(page = page).body()?.results.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    latest = mapped,
                    popular = mapped,
                    homeError = if (mapped.isEmpty()) "Daftar komik kosong" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    homeError = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun retry() = loadHome()

    fun loadDetail(slug: String) {
        if (slug.isBlank()) {
            _uiState.value = _uiState.value.copy(
                detailLoading = false,
                detailError = "Komik tidak ditemukan"
            )
            return
        }
        lastSlug = slug
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLoading = true, detailError = null)
            try {
                val body = api.getKomikDetail(slug = slug, type = "detail").body()
                if (body != null && body.success == true) {
                    val mapped = body.chapters.orEmpty()
                    _uiState.value = _uiState.value.copy(
                        detailLoading = false,
                        detail = body,
                        chapters = mapped,
                        detailError = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        detailLoading = false,
                        detailError = "Gagal memuat detail komik"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    detailLoading = false,
                    detailError = e.localizedMessage ?: "Gagal memuat detail komik"
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
                detailLoading = false,
                detailError = "Chapter tidak ditemukan"
            )
            return
        }
        lastSlug = slug
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLoading = true, detailError = null)
            try {
                val body = api.getKomikPages(slug = slug, type = "stream").body()
                val pages = body?.pages.orEmpty()
                _uiState.value = _uiState.value.copy(
                    detailLoading = false,
                    streamTitle = body?.chapterTitle
                        ?: body?.mangaTitle
                        ?: _uiState.value.detail?.title,
                    pages = pages,
                    detailError = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    detailLoading = false,
                    detailError = e.localizedMessage ?: "Gagal memuat chapter"
                )
            }
        }
    }

    fun retryStream() {
        lastSlug?.let { loadStream(it) }
    }

    /** Pencarian komik. Return hasil agar screen bisa menampilkan state sendiri. */
    suspend fun search(query: String): List<KomikItem> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            api.searchKomik(query.trim()).body()?.results.orEmpty()
        }.getOrElse { emptyList() }
    }
}