# AUDIT — Jayra Media (Anime/Comic/Stream/Neko/HUB)

Branch: `fix/bugs-and-api` · Tanggal audit: 2026-11-26

## 1. Alur per fitur (nyata, dari code)

### HUB
`MainScreen` (NavHost) → route `hub` → `HubScreen` (3+1 card: anime/comic/strem/neko) → tap card → `navController.navigate(ROUTE_*)`.
Bottom nav utama muncul di HUB (ROUTE_HUB ada di BOTTOM_BAR_ROUTES).

### ANIME
`AnimeHomeScreen` → `AnimeViewModel.loadHome()` (PARELAL: NexaDev animasu + Animekuindo) → `AnimeApi`/`AnimeKuindoApi` (Retrofit+OkHttp) → Moshi → `AnimeSearchItem` → state → `AnimePosterCard` (Coil).
Detail: `AnimeDetailScreen` → `loadDetail(slug)` → `AnimeKuindoApi.detail` → `AnimeDetailData` → episode list.
Play: `AnimePlayerScreen` → `loadStream(episodeSlug)` → `AnimeKuindoApi.episode` → `streams[].url` → `NativeVideoPlayer` (ExoPlayer).

### COMIC
`ComicHomeScreen` → `ComicViewModel.loadHome()` (PARELAL: nexadev komik + bacakomik + maid) → `ComicApi` → `KomikItem` → `ComicPosterCard` (Coil).
Detail/Reader: `loadBkDetail`/`loadMaidManga` + `loadBkChapter`/`loadMaidChapter`.

### STREAM / DONGHUA
`DonghuaHomeScreen` → `DonghuaViewModel.loadHome()` → `DonghuaApi.latest()` → `DramaItem` → poster (Coil).
Play: `DonghuaPlayerScreen` → `loadStream(slug)` → `DonghuaApi.episode` → `streaming.servers[].url` (ok.ru/rumble embed) → `NativeVideoPlayer`.

### NEKO
`NekoScreen` (route `neko`) → `NekoViewModel.load()` (PARELAL latestHentai+home) → `NekopoiApi` → `NkItem` → `NekoCard` (clickable).

## 2. Endpoint (baseUrl, path, header)

| Fitur | baseUrl | path | header |
|---|---|---|---|
| Anime (nexadev) | https://api.nexadev.my.id/ | /animasu/?popular/ongoing/completed/search/details/stream | Accept json |
| Anime (kuindo) | https://www.sankavollerei.web.id/ | /anime/animekuindo/{home,latest,popular,movie,genres,seasons,search,detail,episode} | UA+Referer+Accept |
| Comic (nexadev) | https://api.nexadev.my.id/ | /komik/?q/page | Accept json |
| Comic (sanka) | https://www.sankavollerei.web.id/ | /comic/{search,genre,bacakomik/*,maid/*} | UA+Referer |
| Donghua | https://www.sankavollerei.web.id/ | /anime/donghua/{latest,search,detail,episode} | UA+Referer |
| Neko | https://www.sankavollerei.web.id/ | /anime/nekopoi/{home,latest-hentai,detail,episode} | UA+Referer |

Semua HTTPS, SSL valid (Cloudflare). OkHttp: connect/read 30s, followRedirects=true. Tidak ada token/cookie. Tidak ada retry otomatis.

## 3. Titik gagal (file:line + root cause)

| # | File:line | Root cause |
|---|---|---|
| 1 | AnimeHomeScreen.kt:88-89 | `isEmpty` hanya cek 3 list nexadev; saat kuindo/neko ada data tapi nexadev lambat → tampil "Gagal memuat daftar anime". (sudah dipatch sebagian) |
| 2 | AnimePlayerScreen → loadStream | `streams[].url` = blogger `video.g?token=` (HTML), bukan mp4 → ExoPlayer hitam. Perlu ekstrak googlevideo mp4 di Kotlin. |
| 3 | DonghuaPlayerScreen.kt:84 | Pesan "Sumber embed tidak dapat diputar native" muncul untuk ok.ru/rumble embed. Perlu ekstrak direct URL / error jelas tanpa pesan itu. |
| 4 | NekoScreen.kt:98-101 | Card clickable ada; jika data kosong (parse/SSL) → empty state → tampak "tidak bisa klik". Root: parsing/SSL di device. |
| 5 | ComicViewModel | Poster data-URI (maid banner) → sanitize sudah ada. Parsing field cocok. |
| 6 | HUB VidHubScreen | Masih flat Android (RoundedCornerShape 20dp, gradient biasa) → perlu iOS 26 glass kristal. |

## 4. Image loader (Coil)
`MediaImageLoader` (UA browser, memory 25%, disk 80MB). Poster gagal bila: URL null/data-URI (maid), atau CDN hotlink (komiku/i2.wp). Fix: sanitize + placeholder gradient (sudah).

## 5. ExoPlayer
`VideoPlayerManager` singleton, hardware decoder (DefaultRenderersFactory no fallback), CacheDataSource 100MB. `NativeVideoPlayer` useController=false, dispose pause. Belum ada: error listener yang tampilkan error asli, prefetch 3s, play/pause overlay.

## 6. Rencana patch (lanjut, bukan dari nol)
- VideoPlayerManager: tambah Player.Listener (error asli), prefetch, play/pause.
- NativeVideoPlayer: overlay kontrol native (play/pause), Color.Black 0.3f (no blur), error state.
- DonghuaPlayer: hapus pesan "native", ekstrak direct URL dari embed di Kotlin (OkHttp) → ExoPlayer; fallback error jelas.
- AnimePlayer: ekstrak googlevideo dari blogger video.g.
- Neko: pastikan data load (retry) + card ripple.
- HUB: iOS 26 glass kristal putih (no blur) via graphicsLayer + spring.
- Comic: sanitize + retry (sudah), pastikan empty/error state.