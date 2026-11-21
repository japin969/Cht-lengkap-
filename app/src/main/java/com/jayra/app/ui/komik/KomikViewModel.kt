package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikDetailResponse
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.MangaDexApi
import com.jayra.app.data.MdxChapter
import com.jayra.app.data.MdxManga
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
    // Sumber data baru: MangaDex API (API lama apii.nexadev.my.id sudah mati / 503).
    private val api = MangaDexApi.create()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    /** Slug terakhir yang diminta, dipakai untuk retry. */
    private var lastSlug: String? = null

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, homeError = null)
            try {
                val res = api.list()
                val mapped = res.body()?.data.orEmpty().map { it.toItem() }
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
                val detailRes = api.detail(slug)
                val manga = detailRes.body()?.data
                val chapterItems = runCatching {
                    api.chapters(slug).body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                if (detailRes.isSuccessful && manga != null) {
                    val mapped = chapterItems.map { it.toChapter() }
                    _uiState.value = _uiState.value.copy(
                        detailLoading = false,
                        detail = KomikDetailResponse(
                            success = true,
                            title = manga.attributes?.displayTitle(),
                            cover = manga.coverUrl(),
                            synopsis = manga.attributes?.displayDescription(),
                            chapters = mapped
                        ),
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
                val res = api.chapterImages(slug)
                val body = res.body()
                val base = body?.baseUrl
                val hash = body?.chapter?.hash
                val files = body?.chapter?.data.orEmpty()
                val pages = if (base != null && hash != null) {
                    files.mapIndexed { index, file ->
                        KomikPage(page = index + 1, url = "$base/data/$hash/$file")
                    }
                } else emptyList()
                _uiState.value = _uiState.value.copy(
                    detailLoading = false,
                    streamTitle = _uiState.value.detail?.title,
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

    /** Pencarian MangaDex. Return hasil agar screen bisa menampilkan state sendiri. */
    suspend fun search(query: String): List<KomikItem> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            api.search(query.trim()).body()?.data.orEmpty().map { it.toItem() }
        }.getOrElse { emptyList() }
    }

    private fun MdxManga.toItem(): KomikItem = KomikItem(
        title = attributes?.displayTitle(),
        slug = id,
        cover = coverUrl(),
        updatedAt = attributes?.lastChapter?.takeIf { it.isNotBlank() }?.let { "Ch. $it" }
            ?: attributes?.status,
        rating = null
    )

    private fun MdxChapter.toChapter(): KomikChapter {
        val number = attributes?.chapter?.takeIf { it.isNotBlank() }
        val title = attributes?.title?.takeIf { it.isNotBlank() }
        val label = when {
            number != null && title != null -> "Chapter $number · $title"
            number != null -> "Chapter $number"
            title != null -> title
            else -> "Chapter"
        }
        return KomikChapter(title = label, slug = id, link = id)
    }
}