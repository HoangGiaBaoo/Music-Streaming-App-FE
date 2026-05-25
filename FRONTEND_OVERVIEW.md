# FRONTEND OVERVIEW — Music Streaming App (Android)

Tài liệu này tóm tắt toàn bộ phần **Android FE** của dự án Spotify-clone: kiến trúc, luồng hoạt động và vai trò của từng class trong source code. Đối ứng với `PROJECT_OVERVIEW.md` (backend).

> Source code tại `app/src/main/java/com/example/musicstreamingapp/`. Backend Spring Boot là dự án tách rời (xem `PROJECT_OVERVIEW.md`).

---

## 1. Tổng quan

- **Mục tiêu:** Client Android cho hệ thống nghe nhạc trực tuyến kiểu Spotify, target người dùng Việt Nam. Giao diện tham khảo `giaodien.pdf` (23 ảnh chụp app Spotify thật).
- **Nghiệp vụ hỗ trợ:**
    - Đăng ký / đăng nhập / multi-account, JWT-based
    - Home feed nhiều section (featured, top picks, recently played, recommended, chart, mood, new releases, popular artists)
    - 5 bottom tab: **Trang chủ · Tìm kiếm · Thư viện · Premium · Tạo**
    - Tìm kiếm bài hát + nghệ sĩ
    - Thư viện cá nhân: playlist, liked tracks, followed artists
    - Trang chi tiết: Artist, Album, Playlist, Recent
    - Player ExoPlayer với mini-player, lyrics đồng bộ LRC
    - Quản lý playlist (CRUD + upload cover, public/private, edit details)
    - Premium plans + subscribe / cancel
    - Profile + edit profile + listening stats + settings (5 toggle)
- **Tech stack:**
    - **Android Java**, `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`, source `Java 11`
    - **ViewBinding** (no Kotlin synthetics, no Hilt — DI tay qua `VmFactory`)
    - **Lifecycle 2.8.7** (`ViewModel` + `LiveData`)
    - **Retrofit 2.9 + Gson + OkHttp** cho HTTP
    - **ExoPlayer (media3 1.3.1)** cho stream audio (hỗ trợ HTTP Range)
    - **Glide 4.16 + glide-transformations** cho ảnh
    - **Material 1.13** + **CircleImageView** + **ViewPager2** + **AndroidX Palette** + **Facebook Shimmer**
- **Base URL:** `http://10.0.2.2:8080/musicapp/` (emulator) hoặc `http://<LAN_IP>:8080/musicapp/` (thiết bị thật)

---

## 2. Kiến trúc tổng thể — MVVM

```
        ┌──────────────────────────────────────────┐
        │  View  (Activity / Fragment)             │  ← lifecycle, bind View ↔ VM, không gọi Retrofit
        └────────────────────┬─────────────────────┘
                             │ observe LiveData
                             ▼
        ┌──────────────────────────────────────────┐
        │  ViewModel  (androidx.lifecycle)         │  ← state qua LiveData, sự kiện qua Event<T>
        └────────────────────┬─────────────────────┘
                             │ gọi callback
                             ▼
        ┌──────────────────────────────────────────┐
        │  Repository  (data/repository)           │  ← bọc Retrofit, không phụ thuộc lifecycle
        └────────────────────┬─────────────────────┘
                             │ enqueue Call<T>
                             ▼
        ┌──────────────────────────────────────────┐
        │  ApiService  (Retrofit interface)        │  ← khai báo endpoint
        └────────────────────┬─────────────────────┘
                             │ HTTP + AuthInterceptor (Bearer token)
                             ▼
                       Spring Boot Backend
```

**Quy tắc:**
1. **Activity/Fragment chỉ làm hai việc:** quan sát LiveData từ ViewModel + bind View ↔ user event. Không gọi Retrofit, không giữ state nghiệp vụ.
2. **ViewModel sống qua config change**, expose `LiveData<T>` (state) và `LiveData<Event<T>>` (snackbar / navigate one-shot).
3. **Repository wrap Retrofit thành callback đơn giản** (`RepoCallback<T>`), không kế thừa Android lifecycle.
4. **ViewBinding** thay 100% `findViewById`. Layout XML giữ nguyên.
5. **Không dùng coroutines/RxJava** — Retrofit `enqueue` + `MutableLiveData.postValue` là đủ.
6. Không Hilt — `VmFactory` là factory tay duy nhất cho tất cả ViewModel.

---

## 3. Cấu trúc package

```
com.example.musicstreamingapp/
├── *Activity.java          (18 Activity ở package gốc)
├── adapter/                (28 RecyclerView adapter)
├── data/
│   ├── Resource.java       (Loading | Success | Error wrapper)
│   ├── Event.java          (SingleLiveEvent one-shot)
│   ├── RepoCallback.java   (interface 2 method: onSuccess / onError)
│   └── repository/         (7 Repository)
├── fragment/               (11 Fragment + BottomSheet)
├── model/                  (21 POJO/DTO)
├── network/                (ApiService + RetrofitClient + AuthInterceptor)
├── ui/                     (PlaylistCoverView — custom view)
├── util/                   (PlayerManager + TokenManager + AccountStore + …)
└── viewmodel/              (22 ViewModel + VmFactory)
```

Layout XML: `app/src/main/res/layout/` — **71 file** (18 activity_*, 6 fragment_*, 3 bottom_sheet_*, 1 dialog_*, 5 layout_shimmer_*, 38 item_*, 1 sheet_*, 1 view_*).

---

## 4. Tầng Network (`network/`)

| Class | Vai trò |
|-------|---------|
| `RetrofitClient` | Singleton tạo `Retrofit` (`BASE_URL = http://10.0.2.2:8080/musicapp/`) + `BASE_MEDIA_URL` để prepend cho `coverUrl`/`audioUrl`/`avatarUrl`. Cài `AuthInterceptor` + `HttpLoggingInterceptor (BODY)`. `reset()` để clear khi đổi account. |
| `AuthInterceptor` | Đọc `token` từ `SharedPreferences` (`auth` prefs), gắn `Authorization: Bearer <token>` cho mọi request không thuộc `/api/auth/**`. Khi gặp 401/403 thì gọi `SessionManager.markExpired()` để app điều hướng về Login. |
| `ApiService` | Retrofit interface — **toàn bộ 46 endpoint** của hệ thống: auth, users, settings, stats, home, tracks, artists, albums, genres, playlists (kèm PUT/DELETE/upload cover), charts, recommendations, history, subscription, search. |

---

## 5. Tầng Data — Repository (`data/repository/`)

Mỗi Repository nhận `ApiService api` qua constructor, expose method dạng `void getXxx(args, RepoCallback<T> cb)`. Callback Retrofit chạy trên main thread của Android nên không cần `Handler`.

| Repository | Trách nhiệm |
|------------|-------------|
| `AuthRepository` | `login` / `register` / `logout`; lưu token + username + role vào `TokenManager` sau khi login thành công. |
| `UserRepository` | Profile (`/me`, `/me/profile`), update profile, upload avatar (multipart), `UserSettings` GET/PUT, listening stats. |
| `HomeRepository` | `/api/home/feed?filter=` — trả `List<HomeSection>`. |
| `LibraryRepository` | Tâm điểm của các màn nghe nhạc: albums, artists, tracks, genres, playlists (cả `getMyPlaylists`, `getPlaylist`, `getPlaylistTracks`, CRUD playlist, **upload cover** dạng multipart, `toggleLike`, `toggleFollow`, `checkFollowState`, `getFollowedArtists`, `getRecentTracks`…). Hầu hết VM phụ thuộc vào repo này. |
| `PlayerRepository` | `recordPlay(trackId)` (POST `/api/history?trackId=`), `getRelatedTracks`. |
| `SearchRepository` | `search(query)` — trả `SearchResult { tracks, artists }`. |
| `SubscriptionRepository` | `getMine`, `getPlans`, `subscribe(plan)`, `cancel`. |

**Helper chung trong Repository:**
- `enqueue(Call<T>, RepoCallback<T>)` — bridge Retrofit Callback → RepoCallback, map `isSuccessful` → onSuccess, ngược lại → onError với `HTTP <code>`.
- `boolCb(RepoCallback<Boolean>)` — wrap endpoint trả `Map<String,String>` thành callback boolean (đa số endpoint toggle).

---

## 6. Tầng ViewModel (`viewmodel/`)

22 ViewModel + 1 `VmFactory`. Mỗi VM chỉ inject 1-2 repository qua constructor. State expose qua `LiveData`, error/navigation qua `LiveData<Event<String>>`.

| ViewModel | Repo phụ thuộc | View tương ứng |
|-----------|----------------|----------------|
| `LoginViewModel` | AuthRepository | `LoginActivity` |
| `RegisterViewModel` | AuthRepository | `RegisterActivity` |
| `MainViewModel` | UserRepository | `MainActivity` (avatar letter cho drawer, dùng chung qua `activityViewModels` scope) |
| `HomeViewModel` | HomeRepository | `HomeFragment` (parse `HomeSection` theo `kind`) |
| `SearchViewModel` | SearchRepository | `SearchFragment` |
| `LibraryViewModel` | LibraryRepository | `LibraryFragment` — chip filter PLAYLIST/ARTIST/ALBUM/LIKED |
| `PlaylistsViewModel` | LibraryRepository | `PlaylistsFragment` |
| `LikedTracksViewModel` | LibraryRepository | `LikedTracksFragment` |
| `FollowingArtistsViewModel` | LibraryRepository | `FollowingArtistsFragment` |
| `AlbumDetailViewModel` | LibraryRepository | `AlbumDetailActivity` |
| `ArtistDetailViewModel` | LibraryRepository | `ArtistDetailActivity` |
| `PlaylistDetailViewModel` | LibraryRepository | `PlaylistDetailActivity` + `PlaylistEditBottomSheet` (share VM qua `requireActivity()`) |
| `RecentViewModel` | LibraryRepository | `RecentActivity` |
| `PlayerViewModel` | PlayerRepository | `PlayerActivity` |
| `SubscriptionViewModel` | SubscriptionRepository | `PremiumPlansActivity` + `PremiumFragment` |
| `ProfileViewModel` | UserRepository | `ProfileActivity` (own + foreign profile) |
| `EditProfileViewModel` | UserRepository | `EditProfileActivity` |
| `ListeningStatsViewModel` | UserRepository | `ListeningStatsActivity` |
| `SettingsViewModel` | UserRepository | `SettingsActivity` |
| `AddArtistViewModel` | LibraryRepository | `AddArtistActivity` |
| `AddToPlaylistViewModel` | LibraryRepository | `AddToPlaylistBottomSheet` |

**`VmFactory`** map `Class<T> → T` bằng chuỗi `if (clazz.isAssignableFrom(...))`. Mỗi factory tự `RetrofitClient.getApiService(TokenManager.getPrefs(appCtx))` rồi `new XxxRepository(api)`.

**`PlaylistDetailViewModel` đáng chú ý:**
- Có enum `EditResult { UPDATED, DELETED, UPDATE_FAILED, DELETE_FAILED }` để báo về Activity sau khi bottom sheet edit playlist xong.
- `loadSuggestionsIfNeeded()` lazy-fetch top 10 tracks cho empty state ("Các bài hát được đề xuất").
- `reload()` được gọi từ `onResume()` của Activity sau khi quay lại từ `PlaylistCoverPickerActivity` (refresh `coverUrl`).

---

## 7. Tầng View — Activity (`*Activity.java`)

18 Activity ở package gốc (giữ vị trí cũ để không phải sửa `AndroidManifest.xml`).

| Activity | Vai trò |
|----------|---------|
| `SplashActivity` | Entry point (`MAIN/LAUNCHER`). Check `TokenManager.isLoggedIn()` → MainActivity, else LoginActivity. |
| `LoginActivity` / `RegisterActivity` | Auth flow. Sau login thành công nhảy về MainActivity. |
| `AddAccountActivity` | Login thêm tài khoản khác (multi-account qua `AccountStore`). |
| `MainActivity` | Container chính: bottom nav 5 tab (Home/Search/Library/Premium/Create) + NavigationDrawer (account + settings + profile) + **mini player** (`layout_mini_player.xml`) gắn dưới bottom nav. |
| `PlayerActivity` | Toàn màn hình phát nhạc — seek bar, prev/next/play, Palette gradient từ cover, mở Lyrics. Theme riêng `Theme.MusicStreamingApp.Player`. |
| `LyricsActivity` | Hiển thị lời bài hát đồng bộ theo timestamp LRC, scroll auto theo `player.getCurrentPosition()`. |
| `AlbumDetailActivity` | CollapsingToolbar + track list của album. |
| `ArtistDetailActivity` | Thông tin nghệ sĩ, popular tracks, albums, related artists, toggle follow. |
| `PlaylistDetailActivity` | CollapsingToolbar + cover (custom `PlaylistCoverView`) + track list. Có FAB play, btn shuffle, btn edit, empty state với suggestions. Gradient app bar lấy màu dominant từ cover qua `Palette`. |
| `PlaylistCoverPickerActivity` | Pick ảnh từ Photo Picker (Android 13+ `ActivityResultContracts.PickVisualMedia`, không cần permission), upload multipart qua `LibraryRepository.uploadPlaylistCover`. |
| `RecentActivity` | "Nghe gần đây" — list từ `/api/history/recent`. |
| `PremiumPlansActivity` | Render `PlanInfo` (INDIVIDUAL/STUDENT/FAMILY) + subscribe. |
| `ProfileActivity` | Xem profile của user (own hoặc foreign qua `EXTRA_USER_ID`). |
| `EditProfileActivity` | Sửa displayName, bio, upload avatar. |
| `ListeningStatsActivity` | Top artist/track tuần/tháng/năm. |
| `SettingsActivity` | 5 toggle/select của `UserSettings`. |
| `AddArtistActivity` | Tìm + follow nhiều nghệ sĩ một lúc (dùng cho onboarding "Thêm nghệ sĩ"). |

---

## 8. Tầng View — Fragment (`fragment/`)

| Fragment | Vai trò |
|----------|---------|
| `HomeFragment` | Tab "Trang chủ". Render `List<HomeSection>` qua `HomeFeedAdapter` — mỗi section là một row carousel, item kiểu khác nhau (`PlaylistCardAdapter`, `TrackCardAdapter`, `ArtistCircleAdapter`, `AlbumCardAdapter`, `ChartCardAdapter`…). Hỗ trợ shimmer skeleton lúc load. |
| `SearchFragment` | Tab "Tìm kiếm". 2 mode: chưa nhập → grid genre tiles (`GenreTileAdapter`), đã nhập → kết quả `tracks + artists`. |
| `LibraryFragment` | Tab "Thư viện" (cha). 4 chip filter PLAYLIST/ARTIST/ALBUM/LIKED. Dùng `LibraryAdapter` (multi-type: TYPE_PLAYLIST/ARTIST/ALBUM/TRACK/ACTION). Playlist row dùng `PlaylistCoverView` để **auto-derive cover** từ tracks khi `coverUrl == null`. |
| `PlaylistsFragment` | Tab phụ trong LibraryPager. List playlist với `PlaylistAdapter` + cùng cơ chế auto-cover. |
| `LikedTracksFragment` | Danh sách bài đã like. |
| `FollowingArtistsFragment` | Danh sách nghệ sĩ đang follow. |
| `PremiumFragment` | Tab "Premium". |
| `AddToPlaylistBottomSheet` | Chọn playlist để thêm track vào (dùng `PlaylistPickerAdapter`). |
| `CreateBottomSheet` | Tab "Tạo" — chọn loại item muốn tạo (playlist mới, playlist cộng tác…). |
| `TrackMenuBottomSheet` | Menu dài cho track: like/unlike, add to playlist, view artist, share… |
| `PlaylistEditBottomSheet` | Sửa tên + public/private + xoá playlist. **Share VM với `PlaylistDetailActivity`** qua `new ViewModelProvider(requireActivity(), ...).get(PlaylistDetailViewModel.class)`. |

---

## 9. Adapter (`adapter/`) — 28 file

Phần lớn là `RecyclerView.Adapter` cho 1 layout cụ thể. Một số đáng chú ý:

| Adapter | Layout | Đặc biệt |
|---------|--------|----------|
| `HomeFeedAdapter` | `item_home_section.xml` | Multi-viewtype theo `HomeSection.kind` (FEATURED, TOP_PICKS, RECENTLY_PLAYED, RECOMMENDED, CHART, MOOD_PLAYLIST, NEW_RELEASES, POPULAR_ARTISTS), mỗi viewtype inflate adapter con khác nhau (`PlaylistCardAdapter`, `TrackCardAdapter`, `ArtistCircleAdapter`, `RecentTileAdapter`, …). |
| `LibraryAdapter` | `item_library_row.xml` + `item_library_artist.xml` + `item_library_action.xml` | Multi-type row cho Library tab. Có `LibraryRepository` + `tracksCache` để auto-fetch tracks khi cần render playlist cover. |
| `PlaylistAdapter` | `item_playlist_list.xml` | Cùng logic auto-cover qua `PlaylistCoverView`. |
| `RecentSectionAdapter` | `item_recent_header.xml` + tile | Section "Nghe gần đây" trên Home: 2-col grid tile. |
| `LyricLineAdapter` | `item_lyric_line.xml` | Highlight dòng đang phát dựa trên timestamp LRC. |
| `TrackDetailAdapter` | `item_track_detail.xml` | Track row trong PlaylistDetail, có menu "..." mở `TrackMenuBottomSheet`. |
| `SuggestedTrackAdapter` | `item_suggested_track.xml` | Empty state PlaylistDetail — "Các bài hát được đề xuất" với nút "+" thêm thẳng. |

Tất cả adapter dùng chung `ItemAnim.animate(view, position, lastPosition)` — fade + slide-up 300ms cho entrance animation (chỉ animate item lần đầu xuất hiện).

---

## 10. Custom View (`ui/`)

| Class | Vai trò |
|-------|---------|
| `PlaylistCoverView` | `FrameLayout` inflate `view_playlist_cover.xml` (`<merge>`). Có 2 mode: **single** (`ImageView`) và **grid** (`GridLayout 2x2` với 4 ImageView). Phương thức `bind(coverUrl, tracks)`: <br>· `coverUrl != null` → load single image qua Glide<br>· `tracks ≥ 4` → 2x2 grid từ 4 track đầu<br>· `1 ≤ tracks ≤ 3` → cover track đầu<br>· `null/empty` → placeholder (`placeholder_gradient`)<br>Được dùng ở `LibraryAdapter`, `PlaylistAdapter`, `PlaylistDetailActivity`. |

---

## 11. Util (`util/`)

| Class | Vai trò |
|-------|---------|
| `TokenManager` | `SharedPreferences` (`prefs="auth"`) lưu `token`, `username`, `role`. Có `isLoggedIn()`, `saveAuth()`, `clear()`. |
| `AccountStore` | Multi-account: lưu list account đã đăng nhập (`username` + `token`) để chuyển nhanh không cần login lại. Dùng ở `AddAccountActivity` và drawer. |
| `SessionManager` | `static volatile boolean expired` set bởi `AuthInterceptor` khi gặp 401/403. MainActivity quan sát để điều hướng về Login. |
| `PlayerManager` | **Singleton** wrap ExoPlayer (media3). Quản lý queue + currentIndex + currentTrack, expose `play(ctx, track, list, index)`, `togglePlayPause`, `seekTo`, `playNext/Previous`, auto-`playNext` khi `STATE_ENDED`. Có `OnTrackChangeListener` để mini-player + PlayerActivity cùng sync. Tắt `setEnableAudioTrackPlaybackParams(false)` để tránh speed drift trên emulator. |
| `LrcParser` | Parse `.lrc`-style lyrics (`[mm:ss.xx]text`) thành `List<LyricLine>` cho `LyricsActivity`. |
| `TimeUtil` | Format ms → `mm:ss`. |
| `ItemAnim` | Helper RecyclerView item entrance animation (fade + slide-up 300ms decelerate, không re-animate khi scroll back). |

---

## 12. Model (`model/`) — 21 POJO

POJO khớp 1-1 với DTO/Entity backend, dùng Gson de/serialize. Field public-free (private + getter).

| Model | Tương ứng backend |
|-------|-------------------|
| `User`, `UserMe`, `UserProfile`, `UserSettings` | `Users` + `User_Settings` |
| `Artist`, `Album`, `Track`, `Genre`, `Playlist` | Entity gốc cùng tên |
| `RecentItem` | `RecentItemDto { track, playedAt }` |
| `HomeSection` | `HomeSectionDto { kind, title, subtitle, items }` — `items` là `List<Object>` để parse runtime theo `kind` |
| `Subscription`, `PlanInfo` | `Subscriptions` + `PlanInfoDto` |
| `ListeningStats` | `/api/stats/listening` response |
| `LoginRequest`, `RegisterRequest`, `JwtResponse` | Auth DTO |
| `PlaylistRequest`, `ProfileUpdateRequest`, `SubscribeRequest` | Request body |
| `SearchResult` | `{ tracks: List<Track>, artists: List<Artist> }` |

---

## 13. Luồng nghiệp vụ chính

### 13.1 Đăng nhập

```
SplashActivity → TokenManager.isLoggedIn()?
  ├─ true  → MainActivity
  └─ false → LoginActivity
              user nhập username/password
              → LoginViewModel.login()
              → AuthRepository.login(...)
              → ApiService.login → JwtResponse
              → TokenManager.saveAuth(token, username, role)
              → RetrofitClient.reset() (rebuild với token mới)
              → finish() + start MainActivity
```

### 13.2 Phát nhạc + ghi lượt nghe

```
1. User tap track ở bất kỳ list nào:
     PlayerManager.getInstance().play(ctx, track, queue, index)
       → init ExoPlayer (lần đầu)
       → setMediaItem(BASE_MEDIA_URL + track.audioUrl)
       → prepare() + play()
       → ExoPlayer tự gửi Range header, server trả 206 Partial Content
     startActivity(PlayerActivity)

2. PlayerActivity quan sát PlayerManager via OnTrackChangeListener:
     - render cover (Glide) + Palette gradient
     - update seek bar mỗi 500ms
     - btn play/pause/next/prev → PlayerManager.togglePlayPause()/playNext()/playPrevious()

3. Ghi lượt nghe (chưa tự động — TODO):
     Khi track đã phát ≥ 30s, gọi PlayerViewModel.recordPlay(trackId)
       → PlayerRepository.recordPlay → POST /api/history?trackId=
       → backend tăng playCount + lưu Play_History

4. Khi STATE_ENDED:
     PlayerManager tự gọi playNext() (queue tròn).
```

### 13.3 Home feed

```
MainActivity onCreate → HomeFragment.onViewCreated
  → HomeViewModel.load() → HomeRepository.getFeed("all")
  → ApiService.homeFeed → List<HomeSection>
  → HomeFeedAdapter.notifyDataSetChanged()
  → inflate item_home_section.xml cho mỗi section
  → mỗi section spawn adapter con (PlaylistCardAdapter, TrackCardAdapter, …)
  → click item → mở Activity tương ứng (AlbumDetail/ArtistDetail/PlaylistDetail/PlayerActivity)
```

### 13.4 Playlist auto-cover (cơ chế đặc biệt)

```
LibraryAdapter.bindRow(holder, item):
  if item.coverUrl set:
    holder.cover.bind(coverUrl, null)   // single image
  elif item.type == PLAYLIST && item.playlistId != null:
    cached = tracksCache.get(playlistId)
    if cached: holder.cover.bind(null, cached)
    else:
      holder.cover.bind(null, null)     // placeholder
      repo.getPlaylistTracks(playlistId, callback)
        on success:
          tracksCache.put(playlistId, data)
          if holder.boundPlaylistId still matches → cover.bind(null, data)
  else:
    holder.cover.bind(null, null)

PlaylistCoverView.bind(coverUrl, tracks):
  - coverUrl set → single Glide
  - tracks ≥ 4 → 2x2 grid (Glide × 4)
  - 1 ≤ tracks ≤ 3 → cover bài đầu
  - empty → placeholder_gradient (nốt nhạc trên nền xám)
```

Lý do: backend `GET /api/playlists` không trả sẵn `sampleTrackCovers`. FE fallback bằng cách lazy-fetch `/api/playlists/{id}/tracks` cho mỗi playlist không có `coverUrl`. Cache trong VH lifetime để tránh fetch lại khi scroll.

### 13.5 Edit / Delete / Cover playlist

```
PlaylistDetailActivity → btnEdit click
  → PlaylistEditBottomSheet.show()
  → sheet share PlaylistDetailViewModel qua requireActivity()
  → user sửa name + isPublic → save() → vm.updateDetails(name, isPublic)
     → repo.updatePlaylist → PUT /api/playlists/{id}
     → editResult.postValue(Event(UPDATED)) → Activity show Snackbar
  → user delete → confirmDelete() → vm.deletePlaylist()
     → repo.deletePlaylist → DELETE /api/playlists/{id}
     → editResult.postValue(Event(DELETED)) → Activity.finish()

PlaylistDetailActivity → coverView click
  → openCoverPicker() → PlaylistCoverPickerActivity
  → user pick ảnh (PickVisualMedia, không cần permission)
  → activity đọc URI → byte[] → repo.uploadPlaylistCover (multipart)
  → POST /api/playlists/{id}/cover → { coverUrl: "/images/..." }
  → finish()
  → DetailActivity.onResume() → vm.reload() → refresh cover
```

### 13.6 Multi-account

```
NavigationDrawer → "Thêm tài khoản"
  → AddAccountActivity (giống LoginActivity nhưng không clear current)
  → AccountStore.save(username, token)

NavigationDrawer → tap account khác
  → AccountStore.switchTo(username) → TokenManager rewrite token
  → RetrofitClient.reset()
  → MainActivity restart
```

---

## 14. Cheat sheet UI → ViewModel → Repository → API

| Screen | ViewModel | Repository | Endpoint |
|--------|-----------|------------|----------|
| Login | LoginViewModel | AuthRepository | `POST /api/auth/login` |
| Home tab | HomeViewModel | HomeRepository | `GET /api/home/feed?filter=` |
| Search tab | SearchViewModel | SearchRepository | `GET /api/search?q=` |
| Library tab | LibraryViewModel | LibraryRepository | `GET /api/playlists`, `/artists/followed`, `/tracks/liked` |
| Playlists subtab | PlaylistsViewModel | LibraryRepository | `GET /api/playlists` |
| Liked Tracks | LikedTracksViewModel | LibraryRepository | `GET /api/tracks/liked` |
| Following Artists | FollowingArtistsViewModel | LibraryRepository | `GET /api/artists/followed` |
| PlaylistDetail | PlaylistDetailViewModel | LibraryRepository | `GET /api/playlists/{id}` + `/tracks`, `PUT/DELETE /api/playlists/{id}`, `POST /api/playlists/{id}/cover` |
| AlbumDetail | AlbumDetailViewModel | LibraryRepository | `GET /api/albums/{id}` + `/tracks` |
| ArtistDetail | ArtistDetailViewModel | LibraryRepository | `GET /api/artists/{id}` + `/albums` + `/tracks/popular` + `/related` + `/follow` |
| Recent | RecentViewModel | LibraryRepository | `GET /api/history/recent?limit=` |
| Player | PlayerViewModel | PlayerRepository | `POST /api/history?trackId=` |
| Lyrics | (đọc thẳng từ `Track.lyrics`) | — | — |
| Premium plans | SubscriptionViewModel | SubscriptionRepository | `GET /api/subscriptions/me + /plans`, `POST /subscribe + /cancel` |
| Profile | ProfileViewModel | UserRepository | `GET /api/users/me/profile` hoặc `/{id}/profile` |
| EditProfile | EditProfileViewModel | UserRepository | `PUT /api/users/me/profile` + `POST /api/users/me/avatar` |
| Settings | SettingsViewModel | UserRepository | `GET/PUT /api/users/me/settings` |
| Listening Stats | ListeningStatsViewModel | UserRepository | `GET /api/stats/listening?period=&offset=` |
| AddArtist | AddArtistViewModel | LibraryRepository | `GET /api/artists` + `POST /api/artists/{id}/follow` |
| AddToPlaylist (bottom sheet) | AddToPlaylistViewModel | LibraryRepository | `GET /api/playlists` + `POST /api/playlists/{id}/tracks` |

---

## 15. Theme & resource (`res/`)

- **Theme:** `Theme.MusicStreamingApp` (Material 3 dark, dựa trên Spotify palette token). `PlayerActivity` có theme con `Theme.MusicStreamingApp.Player`.
- **Color tokens:** `spotify_black`, `spotify_green` (#1DB954), `spotify_surface`, `spotify_elevated`, `accent_white`, `text_secondary`.
- **Edge-to-edge (targetSdk 36):** Android 15+ ép edge-to-edge. Đã opt-out qua `values-v35` + `fitsSystemWindows`. Xem memory `project_target_sdk_36_edge_to_edge`.
- **Font:** Montserrat bundle (offline, không Downloadable Fonts).
- **Shimmer:** Facebook Shimmer cho skeleton ở Home/Library/Search/Playlist/Album.

---

## 16. Lưu ý kỹ thuật / Gotcha

1. **URL ảnh + audio** là path tương đối từ backend (`/images/...`, `/audio/...`). FE phải prepend `RetrofitClient.BASE_MEDIA_URL` trước khi đưa cho Glide/ExoPlayer.
2. **Auth Interceptor không gắn token cho `/api/auth/**`** — tránh trường hợp register/login bị reject 403 khi token cũ còn trong prefs.
3. **`SessionManager.expired`** là volatile static — đơn giản hoá, không thread-safe tuyệt đối nhưng đủ cho FE.
4. **PlayerManager là singleton process-wide** — không bind theo Activity lifecycle. Cần `release()` thủ công khi user logout (`SettingsActivity.logout()`).
5. **`PlaylistCoverView` không phải `<include>`** — là custom FrameLayout dùng `<merge>` để tránh dư layer.
6. **VH-recycle-safe cho lazy fetch cover:** adapter giữ `boundPlaylistId` trên VH. Khi callback fetch về, chỉ rebind nếu `holder.boundPlaylistId` vẫn match — tránh ghi nhầm cover vào VH đã recycle cho playlist khác.
7. **Không chạy emulator/adb trong workflow code-only** — theo memory `feedback_no_emulator`. Tự đối chiếu XML với ảnh trong `giaodien.pdf` để verify giao diện.
8. **Uninstall app giữa các build** khi UI lạ không tái hiện được — 90% nguyên nhân là app data cũ trong `/data/data/`. Theo memory `feedback_uninstall_between_builds`.
9. **Không thêm Claude làm co-author** trong commit message. Theo memory `feedback_no_claude_coauthor`.
10. **N+1 fetch của Playlist cover** là chấp nhận được với ~10-20 playlist mỗi library. Nếu cần tối ưu, backend có thể thêm field `sampleTrackCovers: List<String>` (max 4) vào response `GET /api/playlists`.

---

## 17. Trạng thái dự án

- **MVVM migration:** Hoàn tất 7/7 phase — toàn bộ Activity/Fragment đã refactor sang ViewModel + Repository + ViewBinding. Không còn `findViewById` hay Retrofit call trong View.
- **UI sprint (giaodien.pdf):** Đã làm tới bước 8b (shimmer skeleton trên Library/Search/Playlist/Album/Home, RecyclerView item anim, Fragment transitions, palette gradient, Spotify token, Montserrat font).
- **Playlist editing**: hoàn tất CRUD + upload cover + auto-cover ở Library list + PlaylistsFragment + ProfileActivity.
- **Còn lại / TODO:**
    - Lyrics đồng bộ — chỉ render static hiện tại, chưa scroll theo timestamp 100% (xem `LYRICS_SYNC_SPEC.md`).
    - PlayerViewModel.recordPlay chưa được gọi tự động sau 30s — phải tự gọi từ PlayerActivity khi đủ ngưỡng.
    - Premium gate (HQ audio, offline download tracking) — chưa wire.
    - Test FE: chưa có unit test/UI test.

---

> File này được sinh từ scan source code thực tế. Khi có thay đổi lớn (thêm Activity/ViewModel/Repository, sửa cấu trúc package, đổi base URL, đổi pattern MVVM), nhớ cập nhật lại các bảng tương ứng.
