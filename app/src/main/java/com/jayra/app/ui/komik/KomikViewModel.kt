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
    val detail: KomikDetailResponse? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    val error: String? = null
)

class KomikViewModel : ViewModel() {
    // Sumber data baru: MangaDex API (API lama apii.nexadev.my.id sudah mati / 503).
    private val api = MangaDexApi.create()
    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = api.list()
                val items = res.body()?.data.orEmpty()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    latest = items.map { it.toItem() },
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val detailRes = api.detail(slug)
                val manga = detailRes.body()?.data
                val chapterItems = runCatching {
                    api.chapters(slug).body()?.data.orEmpty()
                }.getOrDefault(emptyList())
                if (detailRes.isSuccessful && manga != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detail = KomikDetailResponse(
                            success = true,
                            title = manga.attributes?.displayTitle(),
                            cover = manga.coverUrl(),
                            synopsis = manga.attributes?.displayDescription(),
                            chapters = chapterItems.map { it.toChapter() }
                        ),
                        chapters = chapterItems.map { it.toChapter() }
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

    fun loadStream(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
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
                    isLoading = false,
                    streamTitle = _uiState.value.detail?.title,
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
