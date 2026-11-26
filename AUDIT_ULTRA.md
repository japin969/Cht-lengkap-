# AUDIT_ULTRA.md - Laporan Audit Mendalam Project Streaming Android

**Tanggal Audit:** 2026-11-26  
**Branch Aktif:** `(HEAD detached at <commit>)` - *perlu dicek dengan `git branch --show-current`*  
**Model:** NVIDIA Nemotron 3 Ultra  
**Agent:** Senior Android/Backend/QA/Performance Engineer

---

## 1. PETA STRUKTUR PROJECT

```
/workspace/app/
├── app/
│   ├── src/main/java/com/jayra/app/
│   │   ├── data/                          # Layer Data (API, Models, Repository)
│   │   │   ├── AnimeApi.kt                # API Anime LAMA (apii.nexadev.my.id) - MATI/503
│   │   │   ├── AnimeModels.kt             # DTO Anime lama
│   │   │   ├── KitsuApi.kt                # API Anime BARU (kitsu.io) - AKTIF
│   │   │   ├── KomikApi.kt                # API Komik LAMA (apii.nexadev.my.id) - MATI/503
│   │   │   ├── KomikModels.kt             # DTO Komik lama
│   │   │   ├── MangaDexApi.kt             # API Komik BARU (api.mangadex.org) - AKTIF
│   │   │   ├── DramaModels.kt             # DTO Drama/Stream lama
│   │   │   ├── TvmazeApi.kt               # API Drama BARU (api.tvmaze.com) - AKTIF
│   │   │   ├── NexadevApi.kt              # API Nexus (Spotify, YouTube, DramaBox) - AKTIF
│   │   │   ├── SiputzxApi.kt              # API AI (GPT-OSS, DeepSeek) - AKTIF
│   │   │   ├── DeezerApi.kt               # API Music (api.deezer.com) - AKTIF
│   │   │   ├── *Repository.kt             # Repository Firebase/Chat/Contact dll
│   │   │   └── local/                     # Room Database (Chat, Message)
│   │   ├── di/
│   │   │   └── AppModule.kt               # Dependency Injection (singleton repositories)
│   │   ├── model/
│   │   │   ├── TelegramModels.kt          # Model Chat, NavItem, SearchResult, dll
│   │   │   └── CallSignal.kt
│   │   ├── ui/
│   │   │   ├── anime/
│   │   │   │   ├── AnimeViewModel.kt      # ViewModel Anime (hanya KitsuApi)
│   │   │   │   ├── AnimeHomeScreen.kt     # UI Home Anime (LazyColumn + LazyRow)
│   │   │   │   ├── AnimeDetailScreen.kt   # UI Detail Anime (TERMASUK WebView DI LARANG!)
│   │   │   │   └── AnimePlayerScreen.kt   # **WEBVIEW DI SINI - HARUS DIHAPUS**
│   │   │   ├── komik/
│   │   │   │   ├── KomikViewModel.kt      # ViewModel Komik (hanya MangaDexApi)
│   │   │   │   ├── KomikHomeScreen.kt     # UI Home Komik (LazyVerticalGrid)
│   │   │   │   ├── KomikDetailScreen.kt   # UI Detail Komik
│   │   │   │   └── KomikReaderScreen.kt   # UI Baca Komik (LazyColumn + AsyncImage)
│   │   │   ├── drama/
│   │   │   │   ├── DramaViewModel.kt      # ViewModel Drama (hanya TvmazeApi)
│   │   │   │   ├── DramaScreen.kt         # UI Home Drama (Banner + LazyRow)
│   │   │   │   └── DramaDetailScreen.kt   # UI Detail Drama (FullScreenVideoPlayer pakai ExoPlayer)
│   │   │   ├── main/
│   │   │   │   └── MainScreen.kt          # **NAVIGASI UTAMA + NavHost + BottomNav**
│   │   │   ├── component/
│   │   │   │   └── LiquidNavBar.kt        # Bottom Nav kustom (glass effect no blur)
│   │   │   ├── music/
│   │   │   │   ├── MusicViewModel.kt      # ViewModel Music (DeezerApi + NexadevApi fallback)
│   │   │   │   ├── MusicHomeScreen.kt
│   │   │   │   └── MusicPlayerScreen.kt
│   │   │   ├── assistant/                 # AI Assistant (SiputzxApi)
│   │   │   ├── auth/, call/, chatlist/, chatroom/, contacts/, settings/, dll
│   │   │   └── VidHubScreen.kt            # Tab container: Drama | Anime | Komik
│   │   ├── util/
│   │   │   ├── GlassSurface.kt            # Glass effect v1 (liquid-glass lib)
│   │   │   ├── GlassSurfaceV2.kt          # Glass effect v2 (spec-driven, zero blur di LOW)
│   │   │   ├── GlassEffectPrefs.kt        # Pref glass effect (NO BLUR POLICY = LOW tier)
│   │   │   ├── LiquidGlassSpecV2.kt       # Spec glass iOS 26 (data class only)
│   │   │   └── ErrorHandler.kt, Performance.kt, dll
│   │   ├── theme/
│   │   │   ├── Theme.kt                   # JayraTheme (Material3 + animasi warna)
│   │   │   ├── Color.kt, Type.kt, LiquidGlassSpecV2.kt, dll
│   │   └── security/, service/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/libs.versions.toml
└── AUDIT_ULTRA.md (file ini)
```

---

## 2. ANALISIS API PER FITUR

### 2.1 ANIME

| Aspek | Detail |
|-------|--------|
| **API Aktif** | `KitsuApi` (`https://kitsu.io/api/edge/`) |
| **API Mati** | `AnimeApi` (`https://apii.nexadev.my.id/animasu`) - return 503 |
| **Endpoints Kitsu** | `GET /anime` (list, search), `GET /anime/{id}` (detail) |
| **Header** | `Accept: application/vnd.api+json` |
| **Timeout** | 30s connect/read |
| **Converter** | Moshi + KotlinJsonAdapterFactory |
| **Parallel?** | **TIDAK** - Hanya 1 sumber (Kitsu). AnimeApi lama tidak dipakai. |
| **Dipanggil di Composable?** | TIDAK - Dipanggil di `AnimeViewModel.loadHome()` via `viewModelScope.launch` |
| **Caching/Debounce** | TIDAK ADA - Setiap `loadHome()` hit API fresh |
| **Auth** | Tidak perlu auth (public API) |
| **Video Streaming** | **TIDAK ADA** - Kitsu tidak menyediakan stream URL. `loadStream()` return null. |
| **Field Poster** | `posterImage.large` → `original` → `medium` → `small` → `tiny` (via `KitsuImage.best()`) |

**Response Contoh (Kitsu):**
```json
{
  "data": [{
    "id": "1",
    "attributes": {
      "canonicalTitle": "Attack on Titan",
      "posterImage": { "large": "https://...", "original": "https://..." },
      "synopsis": "...",
      "averageRating": "90.5",
      "episodeCount": 25,
      "status": "finished",
      "startDate": "2013-04-07",
      "subtype": "TV",
      "genres": [...]
    }
  }]
}
```

### 2.2 KOMIK / MANGA

| Aspek | Detail |
|-------|--------|
| **API Aktif** | `MangaDexApi` (`https://api.mangadex.org/`) |
| **API Mati** | `KomikApi` (`https://apii.nexadev.my.id/komik`) - return 503 |
| **Endpoints MangaDex** | `GET /manga` (list, search), `GET /manga/{id}` (detail), `GET /manga/{id}/feed` (chapters), `GET /at-home/server/{chapterId}` (images) |
| **Header** | Tidak ada header khusus (public API) |
| **Timeout** | 30s connect/read |
| **Converter** | Moshi + KotlinJsonAdapterFactory |
| **Parallel?** | **TIDAK** - Hanya 1 sumber (MangaDex). KomikApi lama tidak dipakai. |
| **Dipanggil di Composable?** | TIDAK - Dipanggil di `KomikViewModel` |
| **Caching/Debounce** | TIDAK ADA |
| **Field Cover** | `relationships[cover_art].attributes.fileName` → `https://uploads.mangadex.org/covers/{mangaId}/{fileName}.512.jpg` |
| **Chapter Images** | `baseUrl/data/{hash}/{fileName}` (dari `/at-home/server/{chapterId}`) |

**Masalah Cover:** Cover URL dibangun manual di `MdxManga.coverUrl()`. Jika `relationships` tidak include `cover_art`, return null.

### 2.3 STREAM / DRAMA / FILM / SERIES

| Aspek | Detail |
|-------|--------|
| **API Aktif** | `TvmazeApi` (`https://api.tvmaze.com/`) |
| **API Mati** | `NexadevApi.getDramaHome()` (`https://api.nexadev.my.id/api/dramabox/home`) - CertPathValidatorException |
| **Endpoints TVMaze** | `GET /shows` (list), `GET /search/shows` (search), `GET /shows/{id}?embed=episodes` (detail + episodes) |
| **Header** | Tidak ada header khusus |
| **Timeout** | 30s connect/read |
| **Converter** | Moshi + KotlinJsonAdapterFactory |
| **Parallel?** | **TIDAK** - Hanya 1 sumber (TVMaze). |
| **Dipanggil di Composable?** | TIDAK - Dipanggil di `DramaViewModel` |
| **Caching/Debounce** | TIDAK ADA |
| **Video URL** | **TIDAK ADA** - TVMaze hanya metadata. `DramaEpisode.mp4` & `videoUrl` selalu null. |
| **Field Cover** | `image.original` → `image.medium` |

**Response Contoh (TVMaze):**
```json
{
  "id": 1,
  "name": "Breaking Bad",
  "image": { "medium": "https://...", "original": "https://..." },
  "summary": "<p>...</p>",
  "genres": ["Crime", "Drama", "Thriller"],
  "_embedded": { "episodes": [{ "id": 1, "name": "Pilot", "season": 1, "number": 1 }] }
}
```

### 2.4 NEKO

| Aspek | Detail |
|-------|--------|
| **Status** | **TIDAK DITEMUKAN DI CODEBASE** - Tidak ada file, class, atau referensi "Neko" / "neko" |
| **Kemungkinan** | Bisa jadi nama lain (misal: "Anime" atau fitur yang belum diimplementasikan) |

### 2.5 MUSIC (Referensi Pola Paralel)

| Aspek | Detail |
|-------|--------|
| **API** | `DeezerApi` (primary) + `NexadevApi.searchYouTube` (fallback) |
| **Pola** | **FALLBACK BERANTAI** - Coba Deezer dulu, kalau gagal/blank baru coba YouTube. **BUKAN PARALEL.** |
| **Masalah** | Harus diubah jadi `async` + `awaitAll` paralel. |

---

## 3. KONFIGURASI RETROFIT & OKHTTP

### Pola Umum (Setiap API punya `create()` sendiri):
```kotlin
fun create(): XxxApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()  // ← TIDAK ADA INTERCEPTOR UMUM, TIDAK ADA HEADER DEFAULT
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(XxxApi::class.java)
}
```

### Masalah Kritis:
1. **OkHttpClient dibuat berulang** di setiap `create()` - tidak di-reuse
2. **Tidak ada LoggingInterceptor** (dependency `logging-interceptor` ada di gradle tapi tidak dipakai)
3. **Tidak ada header default** (User-Agent, Referer, Accept) - setiap API handle sendiri
4. **SiputzxApi** punya interceptor sendiri (User-Agent browser + Accept: application/json)
5. **BaseURL hardcode** di setiap file API - tidak terpusat

---

## 4. REPOSITORY & VIEWMODEL

| Fitur | Repository | ViewModel | Sumber Data |
|-------|------------|-----------|-------------|
| Anime | TIDAK ADA (langsung di VM) | `AnimeViewModel` | KitsuApi (1 sumber) |
| Komik | TIDAK ADA (langsung di VM) | `KomikViewModel` | MangaDexApi (1 sumber) |
| Drama/Stream | TIDAK ADA (langsung di VM) | `DramaViewModel` | TvmazeApi (1 sumber) |
| Music | TIDAK ADA (langsung di VM) | `MusicViewModel` | DeezerApi + NexadevApi (fallback) |
| Assistant | TIDAK ADA (langsung di VM) | `AssistantViewModel` | SiputzxApi |

**Pola:** Semua ViewModel memanggil API langsung, tidak ada layer Repository terpisah. Ini membuat testing sulit dan caching tidak terpusat.

---

## 5. IMAGE LOADING (COIL)

### Konfigurasi Saat Ini:
- **Dependency:** `coil-compose:2.7.0`
- **Penggunaan:** `AsyncImage(model = url, ...)` di semua screen
- **Coil Config:** **TIDAK ADA KUSTOMISASI** - pakai default Coil
- **OkHttpClient untuk Coil:** **TIDAK ADA** - Coil pakai default OkHttpClient tanpa header

### Masalah Poster/Gambar:
1. **Tidak ada header Referer/User-Agent** - Banyak server anime/komik/streaming blokir hotlink (403)
2. **Tidak ada placeholder/error placeholder** - Kotak kosong saat load gagal
3. **Tidak ada cache config eksplisit** - Pakai default Coil (memory + disk)
4. **URL Relative/Absolute** - MangaDex cover URL absolute OK. Kitsu poster URL absolute OK. TVMaze cover URL absolute OK.
3. **Tidak ada shimmer loading** - Langsung kosong sampai load selesai

---

## 6. VIDEO PLAYER (EXOPLAYER)

### Implementasi Saat Ini:

#### `DramaDetailScreen.kt` - `FullScreenVideoPlayer`:
```kotlin
@Composable
fun FullScreenVideoPlayer(videoUrl: String, onBack: () -> Unit) {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    // AndroidView(PlayerView).apply { player = exoPlayer; useController = true }
}
```

#### `AnimeDetailScreen.kt` - `AnimePlayerScreen`:
```kotlin
@Composable
fun AnimePlayerScreen(videoUrl: String) {
    // **MENGGUNAKAN WEBVIEW - DILARANG KERAS**
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            loadUrl(videoUrl)
        }
    })
}
```

### Masalah Kritis:
1. **WEBVIEW DI AnimePlayerScreen** - Pelanggaran larangan mutlak. Harus diganti ExoPlayer native.
2. **ExoPlayer dibuat baru setiap kali** - Bukan singleton. `remember { ExoPlayer.Builder(...).build() }` di Composable = leak potensial.
3. **Tidak ada VideoPlayerManager** - Tidak ada single instance management.
4. **Tidak ada header untuk video** - Bebutuhkan `DefaultHttpDataSource.Factory` dengan header custom.
5. **Tidak ada error listener yang log error asli** - Error ditelan, user hanya lihat layar hitam.
6. **Tidak ada cache/prefetch** - Video load dari nol setiap kali.
7. **Tidak ada lifecycle handling proper** - `onDispose` release tapi tidak handle background/foreground.
8. **TVMaze tidak punya video URL** - `DramaEpisode.mp4` & `videoUrl` selalu null → video tidak akan pernah main.

---

## 7. NAVIGASI & KLIK

### Struktur Navigasi (MainScreen.kt):
```
Bottom Nav (5 item): Chat | Calls | Music | VidHub (Drama) | Settings
```
**Tidak ada Genre di Bottom Nav** - Sudah tidak ada (sesuai requirement "hapus Genre dari nav").

### VidHubScreen.kt (Tab Container):
```
Tabs: Drama | Anime | Komik
- Drama → DramaScreen → DramaDetailScreen → FullScreenVideoPlayer
- Anime → AnimeHomeScreen → AnimeDetailScreen → AnimePlayerScreen (WEBVIEW!)
- Komik → KomikHomeScreen → KomikDetailScreen → KomikReaderScreen
```

### Masalah Klik Neko:
- **Neko tidak ditemukan** - Tidak bisa audit klik.

### Masalah Klik Umum:
- `AnimePosterCard`, `KomikCard`, `DramaPosterCard`, `BannerItem` semua pakai `Modifier.clickable` → OK
- `ListItem` di Detail screen pakai `Modifier.clickable(enabled = isUnlocked)` → OK

---

## 8. HUB / HOME (MainScreen + VidHub)

### MainScreen.kt:
- Scaffold dengan custom `LiquidNavBarRow` (glass effect no blur)
- NavHost dengan 50+ route
- Animasi transisi slide + fade 300ms
- **Tidak ada section Genre** - Genre sudah tidak ada di nav

### VidHubScreen.kt:
- TabRow sederhana (Material3 TabRow)
- 3 tab: Drama, Anime, Komik
- **Terasa kaku** - Tidak ada animasi spring, card standar Material3

---

## 9. GLASS EFFECT & TEMA

### Implementasi:
- `GlassEffectPrefs` → **NO BLUR POLICY** = selalu return `GlassTier.LOW`
- `GlassSurfaceV2` → Spec-driven, zero blur, draw-phase only (gradient sheen + border + glare)
- `LiquidGlassSpecV2` → Data class spec iOS 26 (baseAlpha 0.65, sheen, border gradient, glare)
- `LiquidNavBar` → Custom bottom nav dengan glass effect manual (background semi-transparan + gradient sheen + border + shadow 3dp)

### Status:
- **Sudah no-blur** ✓
- **Sudah iOS 26 style** ✓ (putih transparan, border putih, highlight atas)
- **Di LOW tier** → zero RenderEffect, zero blur ✓

---

## 10. TABEL PENCOCOKAN FIELD POSTER PER SUMBER

| Fitur | Sumber API | Field API | Field Code (DTO) | Mapping Code | Status |
|-------|------------|-----------|------------------|--------------|--------|
| Anime | Kitsu | `attributes.posterImage.large/original/medium/small/tiny` | `KitsuImage.best()` | `AnimeViewModel.toItem()` → `AnimeSearchItem.image` | ✓ COCOK |
| Anime | AnimeApi (mati) | `image` | `AnimeSearchItem.image` | Langsung | N/A (API mati) |
| Komik | MangaDex | `relationships[cover_art].attributes.fileName` | `MdxManga.coverUrl()` → `KomikItem.cover` | `KomikViewModel.toItem()` | ⚠ RAPUH (butuh `includes[]=cover_art`) |
| Komik | KomikApi (mati) | `cover` | `KomikItem.cover` | Langsung | N/A (API mati) |
| Drama | TVMaze | `image.original` → `image.medium` | `TvmazeShow.image` → `DramaItem.cover` | `DramaViewModel.toItem()` | ✓ COCOK |
| Drama | NexadevApi (mati) | `cover` | `DramaItem.cover` | Langsung | N/A (API mati/cert error) |

---

## 11. TABEL PENCOCOKAN FIELD VIDEO PER SUMBER

| Fitur | Sumber API | Field Video API | Field Code | Status |
|-------|------------|-----------------|------------|--------|
| Anime | Kitsu | **TIDAK ADA** | `AnimeStreamData.streamUrl` (null) | **TIDAK ADA STREAMING** |
| Anime | AnimeApi (mati) | `AnimeStreamData.streamUrl`, `alternativeLinks` | `AnimeStreamData` | N/A (API mati) |
| Komik | MangaDex | `baseUrl/data/hash/file` (images) | `KomikPage.url` | ✓ COCOK (untuk image chapter) |
| Drama | TVMaze | **TIDAK ADA** | `DramaEpisode.mp4`, `videoUrl` (null) | **TIDAK ADA VIDEO URL** |
| Drama | NexadevApi (mati) | `DramaEpisode.mp4`, `videoUrl` | `DramaEpisode` | N/A (cert error) |

---

## 12. DIAGRAM ALUR LOAD PER FITUR

### Anime:
```
User buka AnimeHomeScreen
    → AnimeViewModel.init → loadHome()
        → KitsuApi.list() x3 (popular, ongoing, completed) SEQUENTIAL (bukan paralel!)
        → map KitsuAnime → AnimeSearchItem
        → _uiState.value = copy(popular, ongoing, completed)
    → UI collectAsState → LazyColumn/LazyRow
    → AnimePosterCard → AsyncImage(anime.image)
User klik item
    → NavController.navigate("anime_detail?url={id}")
    → AnimeDetailScreen → LaunchedEffect → loadDetail(id)
        → KitsuApi.detail(id)
        → map → AnimeDetailData + episodes (KOSONG!)
    → UI tampil detail + list episode (KOSONG!)
User klik episode
    → onPlayEpisode(epUrl) → navigate("anime_player?url={epUrl}")
    → anime_player route → LaunchedEffect → loadStream(epUrl) → RETURN NULL
    → AnimePlayerScreen(videoUrl="") → WEBVIEW loadUrl("") → BLACK SCREEN
```

### Komik:
```
User buka KomikHomeScreen
    → KomikViewModel.init → loadHome()
        → MangaDexApi.list() (includes=cover_art)
        → map MdxManga → KomikItem (coverUrl() build manual)
        → _uiState.value = copy(latest)
    → UI → LazyVerticalGrid → KomikCard → AsyncImage(komik.cover)
User klik item
    → navigate("komik_detail?slug={id}")
    → KomikDetailScreen → loadDetail(slug)
        → MangaDexApi.detail(slug) + chapters(slug) SEQUENTIAL
        → map → KomikDetailResponse + chapters
    → UI tampil detail + list chapter
User klik chapter
    → navigate("komik_reader?slug={chapterId}")
    → KomikReaderScreen → loadStream(chapterId)
        → MangaDexApi.chapterImages(chapterId)
        → build image URLs: baseUrl/data/hash/file
    → UI → LazyColumn → AsyncImage(page.url)
```

### Drama/Stream:
```
User buka VidHubScreen → Tab Drama → DramaScreen
    → DramaViewModel.init → loadHome()
        → TvmazeApi.shows(page=0)
        → filter (has image) → map → DramaItem
        → _uiState.value = copy(featured)
    → UI → BannerItem + LazyRow → AsyncImage(drama.cover)
User klik item
    → navigate("drama_detail?url={id}")
    → DramaDetailScreen → loadDetail(id)
        → TvmazeApi.detail(id, embed=episodes)
        → map episodes → DramaEpisode (mp4=null, videoUrl=null, unlock=false)
    → UI tampil list episode (SEMUA TERKUNCI - unlock=false)
User klik episode (enabled=false)
    → TIDAK BISA KLIK (isUnlocked = false)
```

---

## 13. ROOT CAUSE YANG DITEMUKAN

| # | Root Cause | Bukti (File:Line) | Dampak |
|---|------------|-------------------|--------|
| 1 | **WEBVIEW di AnimePlayerScreen** | `AnimeDetailScreen.kt:79-118` | Pelanggaran larangan mutlak; tidak hardware accelerated; leak; tidak native |
| 2 | **Hanya 1 sumber API per fitur** (Kitsu, MangaDex, TVMaze) | `AnimeViewModel.kt:27`, `KomikViewModel.kt:28`, `DramaViewModel.kt:24` | List sepi; tidak ada fallback paralel; single point of failure |
| 3 | **API lama (apii.nexadev.my.id) sudah mati/503** | `AnimeApi.kt:36`, `KomikApi.kt:27`, `NexadevApi.kt:32` | Code mati masih ada di codebase; tidak dipakai tapi mengganggu audit |
| 4 | **TVMaze tidak punya video URL** | `DramaViewModel.kt:69-76`, `DramaModels.kt:41-42` | Episode tidak bisa di-play; `unlock=false` selalu |
| 5 | **Kitsu tidak punya streaming** | `AnimeViewModel.kt:111-114` | `loadStream()` return null; AnimePlayerScreen tidak bisa play |
| 6 | **ExoPlayer dibuat di Composable (bukan singleton)** | `DramaDetailScreen.kt:128-134` | Leak memori; tidak reuse; berat; tidak ada lifecycle management |
| 7 | **Tidak ada VideoPlayerManager singleton** | - (tidak ada file) | Setiap play buat ExoPlayer baru; tidak ada cache/prefetch/header management |
| 8 | **Coil tanpa OkHttpClient custom + header** | Semua `AsyncImage` usage | Poster gagal load (403 hotlink protection); tidak ada Referer/User-Agent |
9 | **Tidak ada placeholder/error/shimmer di AsyncImage** | `AnimePosterCard:123`, `KomikCard:110`, `DramaPosterCard:183` | UI kosong saat load; tidak ada feedback visual |
10 | **API dipanggil SEQUENTIAL di loadHome (bukan paralel)** | `AnimeViewModel.kt:37-47`, `DramaViewModel.kt:34` | Load lambat; user menunggu total waktu semua API |
11 | **MusicViewModel pakai FALLBACK bukan PARALEL** | `MusicViewModel.kt:134-161`, `181-210` | Deezer gagal → baru coba YouTube; delay total = timeout Deezer + timeout YouTube |
12 | **Tidak ada Repository layer** | Semua ViewModel call API langsung | Sulit test; caching tidak terpusat; code duplikasi |
13 | **OkHttpClient dibuat berulang di setiap API.create()** | Semua file `*Api.kt` | Waste resource; tidak ada connection pooling efektif |
14 | **Tidak ada LoggingInterceptor** | `build.gradle.kts:133` (dependency ada tapi tidak dipakai) | Debug sulit; tidak lihat request/response mentah |
15 | **Neko tidak ditemukan di codebase** | `grep -r "Neko\|neko"` → 0 hasil | Fitur yang diminta tidak exist; perlu klarifikasi |
16 | **Genre sudah tidak ada di Bottom Nav** | `MainScreen.kt:159-188`, `TelegramModels.kt:826` | Requirement "hapus Genre dari nav" sudah terpenuhi |
17 | **Glass effect sudah no-blur (LOW tier)** | `GlassEffectPrefs.kt:56-63` | Requirement "no blur" sudah terpenuhi |
18 | **Tidak ada caching/debounce di Repository/VM** | Semua ViewModel | Request duplikat; lag scroll; kuota API habis |

---

## 14. DAFTAR API PARALEL vs FALLBACK

| Fitur | Pola Saat Ini | Harus Jadi |
|-------|---------------|------------|
| Anime | **SINGLE** (Kitsu only) | **PARALEL**: Kitsu + AnimeApi (jika dihidupkan) + sumber lain |
| Komik | **SINGLE** (MangaDex only) | **PARALEL**: MangaDex + KomikApi (jika dihidupkan) + sumber lain |
| Drama | **SINGLE** (TVMaze only) | **PARALEL**: TVMaze + NexadevApi DramaBox (jika cert fixed) + sumber lain |
| Music | **FALLBACK** (Deezer → YouTube) | **PARALEL**: Deezer + YouTube (gabungkan hasil, dedup) |
| Assistant | **SINGLE** (SiputzxApi GPT-OSS/DeepSeek) | **PARALEL**: GPT-OSS + DeepSeek (jika mode thinking) |

---

## 15. WEBVIEW YANG DITEMUKAN

| File | Line | Jenis | Status |
|------|------|-------|--------|
| `AnimeDetailScreen.kt` | 79-118 | `AnimePlayerScreen` - Full WebView dengan WebChromeClient, WebViewClient, evaluateJavascript | **HARUS DIHAPUS TOTAL** - Ganti ExoPlayer native |

**Total WebView: 1 lokasi** (AnimePlayerScreen)

---

## 16. KESIMPULAN AUDIT

### Yang Sudah Benar (Tidak Perlu Diubah):
1. ✅ Genre sudah tidak ada di Bottom Nav
2. ✅ Glass effect sudah no-blur (LOW tier enforced)
3. ✅ Struktur navigasi NavHost sudah bagus
4. ✅ Material3 + Jetpack Compose modern
5. ✅ Moshi + KotlinJsonAdapterFactory untuk parsing JSON
6. ✅ Coil 2.7.0 untuk image loading

### Yang HARUS Diperbaiki (Prioritas Kritis):
1. 🔴 **HAPUS WebView di AnimePlayerScreen** → Ganti ExoPlayer native
2. 🔴 **Buat VideoPlayerManager singleton** → Manage ExoPlayer lifecycle, header, cache, prefetch
3. 🔴 **Implementasikan PARALEL API FETCHING** untuk semua fitur (Anime, Komik, Drama, Music)
4. 🔴 **Tambahkan sumber API kedua/ketiga** yang valid untuk setiap fitur
5. 🔴 **Perbaiki video streaming**: Anime (cari sumber streaming), Drama (cari sumber video URL)
6. 🔴 **Coil config custom OkHttpClient** dengan header Referer/User-Agent per sumber
7. 🔴 **Tambahkan placeholder/shimmer/error di AsyncImage**
8. 🔴 **Buat Repository layer** untuk caching, dedup, testability
9. 🔴 **Single OkHttpClient instance** + LoggingInterceptor
10. 🔴 **Neko**: Klarifikasi apakah fitur ini exist dengan nama lain atau perlu dibuat baru

### Yang Perlu Diperbaiki (Prioritas Tinggi):
11. 🟠 Debounce search di semua fitur
12. 🟠 Caching di Repository (memory + disk)
13. 🟠 Error handling ExoPlayer yang log error asli
14. 🟠 Prefetch video 3 detik pertama
15. 🟠 Animasi spring di HUB/VidHub (ganti tween ke spring)
16. 🟠 Genre section di Home (sesuai requirement "pindahkan Genre ke Home")

---

## 17. RENCANA PATCH (FASE 2)

### Fase 2.1: Core Infrastructure
1. Buat `NetworkModule` - Single OkHttpClient + LoggingInterceptor + Header Interceptor per domain
2. Buat `CoilModule` - ImageLoader dengan custom OkHttpClient per domain (Referer, UA)
3. Buat `VideoPlayerManager` - Singleton ExoPlayer dengan DataSource.Factory custom header
4. Buat `BaseRepository` - Caching, dedup, error handling

### Fase 2.2: Anime
1. Hidupkan `AnimeApi` (cari baseURL baru jika apii.nexadev.my.id mati) ATAU cari sumber Anime kedua (misal: AniList, Jikan, dll)
2. `AnimeRepository` - Parallel fetch Kitsu + AnimeApi + sumber lain
3. `AnimeViewModel` - Gunakan Repository, merge hasil, dedup by ID/slug
4. `AnimePlayerScreen` - **HAPUS WEBVIEW**, gunakan `NativeVideoPlayer` (ExoPlayer via VideoPlayerManager)
5. Parsing stream URL dari API yang menyediakan streaming

### Fase 2.3: Komik
1. `KomikRepository` - Parallel fetch MangaDex + sumber lain
2. Perbaiki `coverUrl()` handle null cover_art
3. `KomikReaderScreen` - Optimasi LazyColumn + prefetch image + retry per image

### Fase 2.4: Drama/Stream
1. Cari sumber video URL yang valid (TVMaze tidak punya)
2. `DramaRepository` - Parallel fetch TVMaze (metadata) + sumber video
3. `DramaDetailScreen` - Enable episode click jika video URL tersedia

### Fase 2.5: Music
1. Ubah `MusicViewModel` - Parallel fetch Deezer + YouTube (bukan fallback)
2. Merge & dedup hasil

### Fase 2.6: HUB & Navigasi
1. Tambah section Genre di Home (AnimeHomeScreen, KomikHomeScreen, DramaScreen)
2. Animasi spring di VidHubScreen & card
3. Pastikan glass effect konsisten iOS 26 di semua card

### Fase 2.7: Verifikasi
1. Build & test semua fitur
2. Log paralel API: item per sumber, total merge, error per API
3. Test poster load rate > 95%
4. Test video play dari semua sumber
5. Test no WebView (grep android.webkit = 0)
6. Test no blur (grep RenderEffect/blur = 0)

---

**CATATAN:** Audit ini didasarkan pada pembacaan code static. Beberapa asumsi (seperti API mati 503) perlu divalidasi dengan network call aktual saat patching. File `AUDIT_ULTRA.md` ini menjadi referensi untuk seluruh Fase 2 patching.