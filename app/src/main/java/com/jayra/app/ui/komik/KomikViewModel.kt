package com.jayra.app.ui.komik

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.KitsuAnime
import com.jayra.app.data.KitsuApi
import com.jayra.app.data.KomikChapter
import com.jayra.app.data.KomikDetailResponse
import com.jayra.app.data.KomikItem
import com.jayra.app.data.KomikPage
import com.jayra.app.data.MangaDexApi
import com.jayra.app.data.MdxChapter
import com.jayra.app.data.MdxManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KomikUiState(
    // --- Home ---
    val isLoading: Boolean = false,
    val latest: List<KomikItem> = emptyList(),
    /** true = memakai sumber cadangan (Kitsu) atau data contoh. */
    val isFallback: Boolean = false,
    val error: String? = null,

    // --- Detail ---
    val isDetailLoading: Boolean = false,
    val detail: KomikDetailResponse? = null,
    val chapters: List<KomikChapter> = emptyList(),
    val detailError: String? = null,

    // --- Reader ---
    val isReaderLoading: Boolean = false,
    val streamTitle: String? = null,
    val pages: List<KomikPage> = emptyList(),
    val readerError: String? = null
)

class KomikViewModel : ViewModel() {

    // Sumber utama: MangaDex. Cadangan: Kitsu manga.
    private val api = MangaDexApi.create()
    private val kitsu = KitsuApi.create()
    private val kitsuMirror = KitsuApi.create(KitsuApi.BASE_URL_MIRROR)

    private val _uiState = MutableStateFlow(KomikUiState())
    val uiState: StateFlow<KomikUiState> = _uiState

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isFallback = false) }

            // 1) MangaDex
            val primary = runCatching {
                val res = api.list()
                if (!res.isSuccessful) error("MangaDex HTTP ${res.code()}")
                res.body()?.data.orEmpty()
                    .filter { !it.attributes?.displayTitle().isNullOrBlank() }
                    .map { it.toItem() }
            }
            val fromMangaDex = primary.getOrDefault(emptyList())
            if (fromMangaDex.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        latest = fromMangaDex,
                        isFallback = false,
                        error = null
                    )
                }
                return@launch
            }

            // 2) Kitsu manga (mirror)
            val fallback = runCatching { kitsu.mangaList(limit = 20).body()?.data.orEmpty() }
                .getOrDefault(emptyList())
                .ifEmpty {
                    runCatching { kitsuMirror.mangaList(limit = 20).body()?.data.orEmpty() }
                        .getOrDefault(emptyList())
                }
            if (fallback.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        latest = fallback.map { it.toItem() },
                        isFallback = true,
                        error = "Sumber utama komik sedang gangguan. Memakai sumber cadangan."
                    )
                }
                return@launch
            }

            // 3) Data contoh — jangan biarkan layar loading/error kosong terus.
            _uiState.update {
                it.copy(
                    isLoading = false,
                    latest = demoKomik,
                    isFallback = true,
                    error = "Tidak bisa menghubungi server komik. Menampilkan data contoh."
                )
            }
        }
    }

    fun retry() = loadHome()

    fun loadDetail(slug: String) {
        val id = slug.trim()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetailLoading = true,
                    detailError = null,
                    detail = null,
                    chapters = emptyList()
                )
            }
            if (id.isBlank()) {
                _uiState.update { it.copy(isDetailLoading = false, detailError = "Komik tidak ditemukan") }
                return@launch
            }

            // 1) MangaDex detail + daftar chapter
            val mdx = runCatching {
                val detailRes = api.detail(id)
                if (!detailRes.isSuccessful) error("MangaDex HTTP ${detailRes.code()}")
                val manga = detailRes.body()?.data ?: error("Data komik kosong")
                val chapters = fetchChapters(id)
                manga to chapters
            }

            val mdxData = mdx.getOrNull()
            if (mdxData != null) {
                val (manga, chapterItems) = mdxData
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        detail = KomikDetailResponse(
                            success = true,
                            title = manga.attributes?.displayTitle(),
                            cover = manga.coverUrl(),
                            synopsis = manga.attributes?.displayDescription(),
                            chapters = chapterItems.map { it.toChapter() }
                        ),
                        chapters = chapterItems.map { it.toChapter() },
                        detailError = null
                    )
                }
                return@launch
            }

            // 2) Cadangan Kitsu (hanya kalau id berupa angka milik Kitsu)
            if (id.toLongOrNull() != null) {
                val kitsuDetail = runCatching { kitsu.mangaDetail(id).body()?.data }
                    .getOrNull()
                    ?: runCatching { kitsuMirror.mangaDetail(id).body()?.data }.getOrNull()
                if (kitsuDetail != null) {
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            detail = KomikDetailResponse(
                                success = true,
                                title = kitsuDetail.attributes?.displayTitle(),
                                cover = kitsuCover(kitsuDetail),
                                synopsis = kitsuDetail.attributes?.synopsis,
                                chapters = emptyList()
                            ),
                            chapters = emptyList(),
                            detailError = null
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    isDetailLoading = false,
                    detailError = mdx.exceptionOrNull()?.userMessage() ?: "Gagal memuat detail komik"
                )
            }
        }
    }

    fun retryDetail(slug: String) = loadDetail(slug)

    private suspend fun fetchChapters(mangaId: String): List<MdxChapter> {
        // Bahasa utama: Inggris + Indonesia.
        val preferred = runCatching {
            api.chapters(mangaId, languages = listOf("en", "id")).body()?.data.orEmpty()
                .filter { it.attributes?.externalUrl.isNullOrBlank() }
        }.getOrDefault(emptyList())
        if (preferred.isNotEmpty()) return preferred

        // Kalau kosong, coba bahasa lain supaya detail tetap bisa dibaca.
        return runCatching {
            api.chapters(
                mangaId,
                languages = listOf("en", "id", "es", "pt-br", "fr", "ru", "de", "ja", "ko", "zh")
            ).body()?.data.orEmpty()
                .filter { it.attributes?.externalUrl.isNullOrBlank() }
        }.getOrDefault(emptyList())
    }

    fun loadStream(slug: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isReaderLoading = true, readerError = null, pages = emptyList())
            }
            try {
                val res = api.chapterImages(slug)
                if (!res.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isReaderLoading = false,
                            readerError = "Gagal memuat chapter (HTTP ${res.code()})"
                        )
                    }
                    return@launch
                }
                val body = res.body()
                val base = body?.baseUrl
                val hash = body?.chapter?.hash
                val dataFiles = body?.chapter?.data.orEmpty()
                val saverFiles = body?.chapter?.dataSaver.orEmpty()
                // dataSaver punya path /data-saver/, bukan /data/.
                val useSaver = dataFiles.isEmpty() && saverFiles.isNotEmpty()
                val files = if (useSaver) saverFiles else dataFiles
                val quality = if (useSaver) "data-saver" else "data"
                val pages = if (base != null && hash != null) {
                    files.mapIndexed { index, file ->
                        KomikPage(page = index + 1, url = "$base/$quality/$hash/$file")
                    }
                } else emptyList()
                _uiState.update {
                    it.copy(
                        isReaderLoading = false,
                        streamTitle = _uiState.value.detail?.title,
                        pages = pages,
                        readerError = if (pages.isEmpty()) "Halaman chapter tidak tersedia" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isReaderLoading = false,
                        readerError = e.userMessage("Gagal memuat chapter")
                    )
                }
            }
        }
    }

    fun retryStream(slug: String) = loadStream(slug)

    private fun Throwable.userMessage(default: String): String = when (this) {
        is java.net.UnknownHostException -> "Tidak ada koneksi internet"
        is java.net.SocketTimeoutException -> "Server komik lambat / timeout. Coba lagi."
        is javax.net.ssl.SSLException -> "Koneksi aman ke server komik gagal"
        is java.io.IOException -> localizedMessage ?: "Koneksi bermasalah"
        else -> localizedMessage ?: default
    }

    private fun MdxManga.toItem(): KomikItem = KomikItem(
        title = attributes?.displayTitle(),
        slug = id,
        cover = coverUrl(),
        updatedAt = attributes?.lastChapter?.takeIf { it.isNotBlank() }?.let { "Ch. $it" }
            ?: attributes?.status,
        rating = null
    )

    private fun KitsuAnime.toItem(): KomikItem = KomikItem(
        title = attributes?.displayTitle(),
        slug = id,
        cover = kitsuCover(this),
        updatedAt = attributes?.chapterCount?.let { "$it ch" } ?: attributes?.status,
        rating = null
    )

    private fun kitsuCover(manga: KitsuAnime): String? =
        manga.attributes?.posterImage?.best()?.takeIf { it.isNotBlank() }
            ?: manga.attributes?.coverImage?.best()

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

    companion object {
        /** Data contoh terakhir kalau MangaDex & Kitsu sama-sama tidak bisa dihubungi. */
        private val demoKomik: List<KomikItem> = listOf(
            KomikItem(title = "Solo Leveling", slug = "", cover = null, updatedAt = "Contoh"),
            KomikItem(title = "One Piece", slug = "", cover = null, updatedAt = "Contoh"),
            KomikItem(title = "Naruto", slug = "", cover = null, updatedAt = "Contoh"),
            KomikItem(title = "Jujutsu Kaisen", slug = "", cover = null, updatedAt = "Contoh"),
            KomikItem(title = "Demon Slayer", slug = "", cover = null, updatedAt = "Contoh"),
            KomikItem(title = "Attack on Titan", slug = "", cover = null, updatedAt = "Contoh"),
            KomikItem(title = "My Hero Academia", slug = "", cover = null, updatedAt = "Contoh"),
            KomikItem(title = "Tokyo Revengers", slug = "", cover = null, updatedAt = "Contoh"),
            KomikItem(title = "Blue Lock", slug = "", cover = null, updatedAt = "Contoh")
        )
    }
}
