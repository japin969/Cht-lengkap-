package com.jayra.app.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayra.app.data.AnimeKompiApi
import com.jayra.app.data.AnimeKompiItem
import com.jayra.app.data.AnimeKompiDetailData
import com.jayra.app.data.AnimeKompiEpisodeItem
import com.jayra.app.data.AnimeKompiEpisodeData
import com.jayra.app.data.AnimeKompiMirrorsResponse
import com.jayra.app.data.AnimeKompiDanmakuResponse
import com.jayra.app.data.AnimeKompiCommentsResponse
import com.jayra.app.data.AnimeKompiReactionsResponse
import com.jayra.app.data.AnimeKompiSettingsData
import com.jayra.app.data.AnimeKompiScheduleData
import com.jayra.app.data.AnimeKompiAzData
import com.jayra.app.data.AnimeKompiSuggestItem
import com.jayra.app.data.AnimeKompiFilterListData
import com.jayra.app.data.AnimeKompiGenreItem
import com.jayra.app.data.AnimeKompiSeasonItem
import com.jayra.app.data.AnimeKompiStudioItem
import com.jayra.app.data.AnimeKompiStatusItem
import com.jayra.app.data.AnimeKompiTypeItem
import com.jayra.app.data.AnimeKompiOrderItem
import com.jayra.app.data.AnimeKompiGenreDetailData
import com.jayra.app.data.AnimeKompiTooltipData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

// ============================================================================
// UI STATE CLASSES
// ============================================================================

data class AnimeHomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Home page data
    val heroItems: List<AnimeKompiItem> = emptyList(),
    val trendingItems: List<AnimeKompiItem> = emptyList(),
    val terbaruItems: List<AnimeKompiItem> = emptyList(),
    val popularItems: List<AnimeKompiItem> = emptyList(),
    val ongoingItems: List<AnimeKompiItem> = emptyList(),
    val completedItems: List<AnimeKompiItem> = emptyList(),
    val batchItems: List<AnimeKompiItem> = emptyList(),
    val movieItems: List<AnimeKompiItem> = emptyList(),
    val liveActionItems: List<AnimeKompiItem> = emptyList(),
    val donghuaItems: List<AnimeKompiItem> = emptyList(),
    val tokusatsuItems: List<AnimeKompiItem> = emptyList(),
    val upcomingItems: List<AnimeKompiItem> = emptyList(),
    // Schedule
    val scheduleData: AnimeKompiScheduleData? = null,
    // Filter chips
    val selectedChip: String = "Semua",
)

data class AnimeDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val detail: AnimeKompiDetailData? = null,
    val episodes: List<AnimeKompiEpisodeItem> = emptyList(),
    val recommendations: List<AnimeKompiItem> = emptyList(),
    val tooltip: AnimeKompiTooltipData? = null,
)

data class AnimeEpisodeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val episodeData: AnimeKompiEpisodeData? = null,
    val mirrors: List<com.jayra.app.data.AnimeKompiMirror> = emptyList(),
    val danmaku: List<com.jayra.app.data.AnimeKompiDanmaku> = emptyList(),
    val comments: AnimeKompiCommentsResponse? = null,
    val reactions: AnimeKompiReactionsResponse? = null,
)

data class AnimeSearchUiState(
    val isLoading: Boolean = false,
    val isSuggestLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val results: List<AnimeKompiItem> = emptyList(),
    val suggestions: List<AnimeKompiSuggestItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
)

data class AnimeFilterUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterList: AnimeKompiFilterListData? = null,
    val selectedGenres: List<String> = emptyList(),
    val selectedSeasons: List<String> = emptyList(),
    val selectedStudios: List<String> = emptyList(),
    val selectedStatus: List<String> = emptyList(),
    val selectedTypes: List<String> = emptyList(),
    val selectedOrder: String? = null,
)

data class AnimeCategoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<AnimeKompiItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val categoryTitle: String = "",
)

data class AnimeListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val genres: List<AnimeKompiGenreItem> = emptyList(),
    val seasons: List<AnimeKompiSeasonItem> = emptyList(),
    val studios: List<AnimeKompiStudioItem> = emptyList(),
    val statuses: List<AnimeKompiStatusItem> = emptyList(),
    val types: List<AnimeKompiTypeItem> = emptyList(),
    val orders: List<AnimeKompiOrderItem> = emptyList(),
    val azData: AnimeKompiAzData? = null,
)

data class AnimeSettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val settings: AnimeKompiSettingsData? = null,
)

// ============================================================================
// MAIN VIEW MODEL
// ============================================================================

class AnimeViewModel : ViewModel() {
    private val api = AnimeKompiApi.create()
    private val _homeState = MutableStateFlow(AnimeHomeUiState())
    val homeState: StateFlow<AnimeHomeUiState> = _homeState

    private val _detailState = MutableStateFlow(AnimeDetailUiState())
    val detailState: StateFlow<AnimeDetailUiState> = _detailState

    private val _episodeState = MutableStateFlow(AnimeEpisodeUiState())
    val episodeState: StateFlow<AnimeEpisodeUiState> = _episodeState

    private val _searchState = MutableStateFlow(AnimeSearchUiState())
    val searchState: StateFlow<AnimeSearchUiState> = _searchState

    private val _filterState = MutableStateFlow(AnimeFilterUiState())
    val filterState: StateFlow<AnimeFilterUiState> = _filterState

    private val _categoryState = MutableStateFlow(AnimeCategoryUiState())
    val categoryState: StateFlow<AnimeCategoryUiState> = _categoryState

    private val _listState = MutableStateFlow(AnimeListUiState())
    val listState: StateFlow<AnimeListUiState> = _listState

    private val _settingsState = MutableStateFlow(AnimeSettingsUiState())
    val settingsState: StateFlow<AnimeSettingsUiState> = _settingsState

    // ========================================================================
    // HOME SCREEN - Load all data for Netflix-style home
    // ========================================================================
    fun loadHome() {
        viewModelScope.launch {
            _homeState.value = _homeState.value.copy(isLoading = true, error = null)
            try {
                val homeRes: Response<com.jayra.app.data.AnimeKompiHomeResponse> = api.getHome()
                if (homeRes.isSuccessful && homeRes.body()?.data != null) {
                    val data = homeRes.body()?.data!!
                    _homeState.value = _homeState.value.copy(
                        isLoading = false,
                        heroItems = data.hero?.take(5) ?: emptyList(),
                        trendingItems = data.trending ?: emptyList(),
                        terbaruItems = data.terbaru ?: emptyList(),
                        popularItems = data.popular ?: emptyList(),
                        ongoingItems = data.ongoing ?: emptyList(),
                        completedItems = data.completed ?: emptyList(),
                        batchItems = data.batch ?: emptyList(),
                        movieItems = data.movie ?: emptyList(),
                        liveActionItems = data.liveAction ?: emptyList(),
                        donghuaItems = data.donghua ?: emptyList(),
                        tokusatsuItems = data.tokusatsu ?: emptyList(),
                        upcomingItems = data.upcoming ?: emptyList(),
                    )
                    // Load schedule in parallel
                    loadSchedule()
                } else {
                    _homeState.value = _homeState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat data anime"
                    )
                }
            } catch (e: Exception) {
                _homeState.value = _homeState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun retryHome() = loadHome()

    // Schedule (Endpoint 9)
    private fun loadSchedule() {
        viewModelScope.launch {
            try {
                val res: Response<com.jayra.app.data.AnimeKompiScheduleResponse> = api.getSchedule()
                if (res.isSuccessful && res.body()?.data != null) {
                    _homeState.value = _homeState.value.copy(
                        scheduleData = res.body()?.data
                    )
                }
            } catch (e: Exception) {
                // Schedule is optional, don't show error
            }
        }
    }

    // ========================================================================
    // DETAIL SCREEN (Endpoint 29, 30)
    // ========================================================================
    fun loadDetail(slug: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, error = null)
            try {
                val detailRes: Response<com.jayra.app.data.AnimeKompiDetailResponse> = api.getDetail(slug)
                if (detailRes.isSuccessful && detailRes.body()?.data != null) {
                    val data = detailRes.body()?.data!!
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        detail = data,
                        episodes = data.episodes ?: emptyList(),
                        recommendations = data.recommendations ?: emptyList(),
                    )
                    // Load tooltip for quick info
                    loadTooltip(slug)
                } else {
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat detail anime"
                    )
                }
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat detail anime"
                )
            }
        }
    }

    // Tooltip (Endpoint 30)
    private fun loadTooltip(slug: String) {
        viewModelScope.launch {
            try {
                val res: Response<com.jayra.app.data.AnimeKompiTooltipResponse> = api.getTooltip(slug)
                if (res.isSuccessful && res.body()?.data != null) {
                    _detailState.value = _detailState.value.copy(
                        tooltip = res.body()?.data
                    )
                }
            } catch (e: Exception) {
                // Tooltip is optional
            }
        }
    }

    // ========================================================================
    // EPISODE / PLAYER (Endpoints 31, 32, 33, 34, 35)
    // ========================================================================
    fun loadEpisode(episodeSlug: String) {
        viewModelScope.launch {
            _episodeState.value = _episodeState.value.copy(isLoading = true, error = null)
            try {
                // Load episode data (video URL, subtitles, audio)
                val epRes: Response<com.jayra.app.data.AnimeKompiEpisodeResponse> = api.getEpisode(episodeSlug)
                var mirrors = emptyList<com.jayra.app.data.AnimeKompiMirror>()
                var danmaku = emptyList<com.jayra.app.data.AnimeKompiDanmaku>()
                var comments: AnimeKompiCommentsResponse? = null
                var reactions: AnimeKompiReactionsResponse? = null

                if (epRes.isSuccessful && epRes.body()?.data != null) {
                    val epData = epRes.body()?.data!!
                    // Load mirrors, danmaku, comments, reactions in parallel
                    val mirrorsRes = api.getEpisodeMirrors(episodeSlug)
                    val danmakuRes = api.getEpisodeDanmaku(episodeSlug)
                    val commentsRes = api.getEpisodeComments(episodeSlug)
                    val reactionsRes = api.getEpisodeReactions(episodeSlug)

                    if (mirrorsRes.isSuccessful) mirrors = mirrorsRes.body()?.data ?: emptyList()
                    if (danmakuRes.isSuccessful) danmaku = danmakuRes.body()?.data ?: emptyList()
                    if (commentsRes.isSuccessful) comments = commentsRes.body()
                    if (reactionsRes.isSuccessful) reactions = reactionsRes.body()

                    _episodeState.value = _episodeState.value.copy(
                        isLoading = false,
                        episodeData = epData,
                        mirrors = mirrors,
                        danmaku = danmaku,
                        comments = comments,
                        reactions = reactions,
                    )
                } else {
                    _episodeState.value = _episodeState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat episode"
                    )
                }
            } catch (e: Exception) {
                _episodeState.value = _episodeState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat episode"
                )
            }
        }
    }

    // ========================================================================
    // SEARCH (Endpoints 12, 13)
    // ========================================================================
    fun search(query: String, page: Int = 1) {
        viewModelScope.launch {
            val isNewSearch = page == 1
            _searchState.value = _searchState.value.copy(
                isLoading = isNewSearch,
                error = null,
                query = query,
                currentPage = page,
                results = if (isNewSearch) emptyList() else _searchState.value.results,
                hasMore = true
            )
            try {
                val res: Response<com.jayra.app.data.AnimeKompiListResponse> = api.search(query, page)
                if (res.isSuccessful && res.body()?.data != null) {
                    val data = res.body()?.data!!
                    val newItems = data.anime ?: emptyList()
                    _searchState.value = _searchState.value.copy(
                        isLoading = false,
                        results = if (isNewSearch) newItems else _searchState.value.results + newItems,
                        hasMore = (data.page ?: 1) < (data.totalPage ?: 1),
                        currentPage = page
                    )
                } else {
                    _searchState.value = _searchState.value.copy(
                        isLoading = false,
                        error = "Gagal mencari anime"
                    )
                }
            } catch (e: Exception) {
                _searchState.value = _searchState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun searchSuggest(query: String) {
        if (query.length < 2) {
            _searchState.value = _searchState.value.copy(suggestions = emptyList())
            return
        }
        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(isSuggestLoading = true)
            try {
                val res: Response<com.jayra.app.data.AnimeKompiSuggestResponse> = api.searchSuggest(query)
                if (res.isSuccessful && res.body()?.data != null) {
                    _searchState.value = _searchState.value.copy(
                        isSuggestLoading = false,
                        suggestions = res.body()?.data ?: emptyList()
                    )
                } else {
                    _searchState.value = _searchState.value.copy(isSuggestLoading = false)
                }
            } catch (e: Exception) {
                _searchState.value = _searchState.value.copy(isSuggestLoading = false)
            }
        }
    }

    fun clearSearch() {
        _searchState.value = AnimeSearchUiState()
    }

    // ========================================================================
    // FILTER (Endpoints 14, 15)
    // ========================================================================
    fun loadFilterList() {
        viewModelScope.launch {
            _filterState.value = _filterState.value.copy(isLoading = true, error = null)
            try {
                val res: Response<com.jayra.app.data.AnimeKompiFilterListResponse> = api.getFilterList()
                if (res.isSuccessful && res.body()?.data != null) {
                    _filterState.value = _filterState.value.copy(
                        isLoading = false,
                        filterList = res.body()?.data
                    )
                } else {
                    _filterState.value = _filterState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat filter"
                    )
                }
            } catch (e: Exception) {
                _filterState.value = _filterState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    fun applyFilter(
        genres: List<String> = emptyList(),
        seasons: List<String> = emptyList(),
        studios: List<String> = emptyList(),
        status: List<String> = emptyList(),
        types: List<String> = emptyList(),
        order: String? = null
    ) {
        _filterState.value = _filterState.value.copy(
            selectedGenres = genres,
            selectedSeasons = seasons,
            selectedStudios = studios,
            selectedStatus = status,
            selectedTypes = types,
            selectedOrder = order
        )
        // The actual filtering will be done by navigating to category screen with these params
    }

    fun resetFilter() {
        _filterState.value = _filterState.value.copy(
            selectedGenres = emptyList(),
            selectedSeasons = emptyList(),
            selectedStudios = emptyList(),
            selectedStatus = emptyList(),
            selectedTypes = emptyList(),
            selectedOrder = null
        )
    }

    // ========================================================================
    // CATEGORY / GRID SCREENS (Endpoints 3, 4, 5, 6, 7, 8, 22, 24, 25, 26, 27, 28)
    // ========================================================================
    fun loadCategory(categoryType: String, slug: String? = null, page: Int = 1, title: String = "") {
        viewModelScope.launch {
            val isNewLoad = page == 1
            _categoryState.value = _categoryState.value.copy(
                isLoading = isNewLoad,
                error = null,
                categoryTitle = title,
                currentPage = page,
                items = if (isNewLoad) emptyList() else _categoryState.value.items,
                hasMore = true
            )
            try {
                val res: Response<com.jayra.app.data.AnimeKompiListResponse> = when (categoryType) {
                    "batch" -> api.getBatch()
                    "movie" -> api.getMovie()
                    "live-action" -> api.getLiveAction()
                    "donghua" -> api.getDonghua()
                    "tokusatsu" -> api.getTokusatsu()
                    "upcoming" -> api.getUpcoming()
                    "genre" -> slug?.let { api.getGenre(it, page) } ?: Response.success(null)
                    "season" -> slug?.let { api.getSeason(it, page) } ?: Response.success(null)
                    "studio" -> slug?.let { api.getStudio(it, page) } ?: Response.success(null)
                    "status" -> slug?.let { api.getStatusDetail(it, page) } ?: Response.success(null)
                    "type" -> slug?.let { api.getType(it, page) } ?: Response.success(null)
                    "order" -> slug?.let { api.getOrder(it, page) } ?: Response.success(null)
                    else -> Response.success(null)
                }
                if (res.isSuccessful && res.body()?.data != null) {
                    val data = res.body()?.data!!
                    val newItems = data.anime ?: emptyList()
                    _categoryState.value = _categoryState.value.copy(
                        isLoading = false,
                        items = if (isNewLoad) newItems else _categoryState.value.items + newItems,
                        hasMore = (data.page ?: 1) < (data.totalPage ?: 1),
                        currentPage = page
                    )
                } else {
                    _categoryState.value = _categoryState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat data"
                    )
                }
            } catch (e: Exception) {
                _categoryState.value = _categoryState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    // Load A-Z list (Endpoint 11)
    fun loadAzList() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)
            try {
                val res: Response<com.jayra.app.data.AnimeKompiAzResponse> = api.getAz()
                if (res.isSuccessful && res.body()?.data != null) {
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        azData = res.body()?.data
                    )
                } else {
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat daftar A-Z"
                    )
                }
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    // Load all list metadata (Endpoints 16, 17, 18, 19, 20, 21)
    fun loadListMetadata() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)
            try {
                val genresRes = api.getGenres()
                val seasonsRes = api.getSeasons()
                val studiosRes = api.getStudios()
                val statusRes = api.getStatus()
                val typesRes = api.getTypes()
                val ordersRes = api.getOrders()

                _listState.value = _listState.value.copy(
                    isLoading = false,
                    genres = if (genresRes.isSuccessful) genresRes.body()?.data ?: emptyList() else emptyList(),
                    seasons = if (seasonsRes.isSuccessful) seasonsRes.body()?.data ?: emptyList() else emptyList(),
                    studios = if (studiosRes.isSuccessful) studiosRes.body()?.data ?: emptyList() else emptyList(),
                    statuses = if (statusRes.isSuccessful) statusRes.body()?.data ?: emptyList() else emptyList(),
                    types = if (typesRes.isSuccessful) typesRes.body()?.data ?: emptyList() else emptyList(),
                    orders = if (ordersRes.isSuccessful) ordersRes.body()?.data ?: emptyList() else emptyList(),
                )
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    // Load Genre Detail (Endpoint 23)
    fun loadGenreDetail(slug: String) {
        viewModelScope.launch {
            _categoryState.value = _categoryState.value.copy(isLoading = true, error = null, categoryTitle = "Genre")
            try {
                val res: Response<com.jayra.app.data.AnimeKompiGenreDetailResponse> = api.getGenreDetail(slug)
                if (res.isSuccessful && res.body()?.data != null) {
                    val data = res.body()?.data!!
                    _categoryState.value = _categoryState.value.copy(
                        isLoading = false,
                        items = data.anime ?: emptyList(),
                        categoryTitle = data.genre?.name ?: "Genre"
                    )
                } else {
                    _categoryState.value = _categoryState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat genre"
                    )
                }
            } catch (e: Exception) {
                _categoryState.value = _categoryState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }

    // ========================================================================
    // SETTINGS (Endpoint 36)
    // ========================================================================
    fun loadSettings() {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isLoading = true, error = null)
            try {
                val res: Response<com.jayra.app.data.AnimeKompiSettingsResponse> = api.getSettings()
                if (res.isSuccessful && res.body()?.data != null) {
                    _settingsState.value = _settingsState.value.copy(
                        isLoading = false,
                        settings = res.body()?.data
                    )
                } else {
                    _settingsState.value = _settingsState.value.copy(
                        isLoading = false,
                        error = "Gagal memuat pengaturan"
                    )
                }
            } catch (e: Exception) {
                _settingsState.value = _settingsState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Koneksi bermasalah"
                )
            }
        }
    }
}