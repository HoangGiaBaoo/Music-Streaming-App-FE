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
    - Trang chi tiết: Artist, Album, Playlist, Genre, Recent, Liked Songs
    - Player ExoPlayer với mini-player, lyrics đồng bộ LRC
    - Quản lý playlist (CRUD + upload cover, public/private, edit details, **thêm/xoá/kéo sắp xếp bài**)
    - Premium: plans + **VNPay payment (WebView)** + subscribe / cancel, **AdMob** (banner + interstitial cho user Free), **premium gate** (HQ audio, ép trộn bài…)
    - Profile + edit profile + listening stats + settings (5 toggle)
- **Tech stack:**
    - **Android Java**, `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`, source `Java 11`
    - **ViewBinding** (no Kotlin synthetics, no Hilt — DI tay qua `VmFactory`)
    - **Lifecycle 2.8.7** (`ViewModel` + `LiveData`)
    - **Retrofit 2.9 + Gson + OkHttp** cho HTTP
    - **ExoPlayer (media3 1.3.1)** cho stream audio (hỗ trợ HTTP Range)
    - **Glide 4.16 + glide-transformations** cho ảnh
    - **Material 1.13** + **CircleImageView** + **ViewPager2** + **AndroidX Palette** + **Facebook Shimmer**
    - **Google Mobile Ads (AdMob)** cho banner + interstitial (test IDs) — wire qua `MusicApp` (Application class)
    - **WebView** cho VNPay sandbox payment
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

### 2.1 Điều hướng màn chi tiết — Fragment-in-Main (mới)

Các màn chi tiết (Album/Artist/Playlist/Recent/Liked) **không còn mở Activity riêng** khi đang ở trong `MainActivity`. Thay vào đó chúng được nạp dạng **Fragment** vào cùng `fragment_container` của Main → **bottom-nav + mini-player đứng yên**, chỉ phần nội dung trượt ngang (giống Spotify).

```
                 NavHelper.openXxx(ctx, id)
                          │
          ┌───────────────┴────────────────┐
   ctx nằm trong MainActivity?      ctx là Activity khác (Profile/AddArtist…)
          │ có                              │ không
          ▼                                 ▼
  main.openDetail(XxxDetailFragment)   startActivity(XxxDetailActivity)  ← fallback giữ Activity cũ
  · hide(current) + add (KHÔNG replace) → tab cũ không bị huỷ view
  · addToBackStack → Back quay lại tab nguyên trạng (không chạy lại shimmer)
  · slide_in_right / slide_out_left (translate, không alpha → mượt CollapsingToolbar)
```

- **`NavHelper`** (util) là router duy nhất: dò context-chain tìm `MainActivity`; có → mở Fragment, không → mở Activity. Mọi call site dùng chung một API.
- **`BaseDetailFragment`**: hoãn việc nặng (bật shimmer, observe, load ảnh bìa) tới **sau khi animation vào chạy xong** (`onEnterAnimationDone()`) để frame đầu lúc trượt không giật.
- Các **Activity chi tiết cũ vẫn còn** (`AlbumDetailActivity`, `ArtistDetailActivity`, `PlaylistDetailActivity`, `RecentActivity`, `LikedSongsActivity`) — dùng làm fallback khi mở từ ngoài Main; chúng gắn bottom-nav qua **`BottomNavHelper`** và mini-player qua **`MiniPlayerController`** (poll 500ms).
- Logic UI dùng chung được tách vào `*DetailFragment`; Activity tương ứng chỉ host fragment đó.

---

## 3. Cấu trúc package

```
com.example.musicstreamingapp/
├── MusicApp.java           (Application class — init AdMob + đếm bài cho interstitial)
├── *Activity.java          (23 Activity ở package gốc)
├── adapter/                (32 RecyclerView adapter)
├── data/
│   ├── Resource.java       (Loading | Success | Error wrapper)
│   ├── Event.java          (SingleLiveEvent one-shot)
│   ├── RepoCallback.java   (interface 2 method: onSuccess / onError)
│   └── repository/         (7 Repository)
├── fragment/               (20 Fragment + BottomSheet — gồm các *DetailFragment mới)
├── model/                  (25 POJO/DTO)
├── network/                (ApiService + RetrofitClient + AuthInterceptor)
├── ui/                     (PlaylistCoverView — custom view)
├── util/                   (12: PlayerManager, AdManager, PremiumChecker, NavHelper, BottomNavHelper, MiniPlayerController, TokenManager, AccountStore, …)
└── viewmodel/              (24 ViewModel + VmFactory)
```

Layout XML: `app/src/main/res/layout/` — **90 file** (activity_*, fragment_*, bottom_sheet_*, dialog_*, layout_shimmer_*, item_*, sheet_*, view_*).

---

## 4. Tầng Network (`network/`)

| Class | Vai trò |
|-------|---------|
| `RetrofitClient` | Singleton tạo `Retrofit` (`BASE_URL = http://10.0.2.2:8080/musicapp/`) + `BASE_MEDIA_URL` để prepend cho `coverUrl`/`audioUrl`/`avatarUrl`. Cài `AuthInterceptor` + `HttpLoggingInterceptor (BODY)`. `reset()` để clear khi đổi account. |
| `AuthInterceptor` | Đọc `token` từ `SharedPreferences` (`auth` prefs), gắn `Authorization: Bearer <token>` cho mọi request không thuộc `/api/auth/**`. Khi gặp 401/403 thì gọi `SessionManager.markExpired()` để app điều hướng về Login. |
| `ApiService` | Retrofit interface — **toàn bộ 57 endpoint** của hệ thống: auth, users, settings, stats, home, tracks, artists, albums, **genres (kèm `/feed`)**, playlists (kèm PUT/DELETE/upload cover, add/remove track, **`PUT /tracks/order` reorder**), charts, recommendations, history, subscription, **payment (VNPay `/api/payment/create`)**, search. |

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

24 ViewModel + 1 `VmFactory`. Mỗi VM chỉ inject 1-2 repository qua constructor. State expose qua `LiveData`, error/navigation qua `LiveData<Event<String>>`.

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
| `AlbumDetailViewModel` | LibraryRepository | `AlbumDetailActivity` / `AlbumDetailFragment` |
| `ArtistDetailViewModel` | LibraryRepository | `ArtistDetailActivity` / `ArtistDetailFragment` |
| `PlaylistDetailViewModel` | LibraryRepository | `PlaylistDetailActivity` / `PlaylistDetailFragment` + `PlaylistEditBottomSheet` (share VM qua `requireActivity()`) |
| `GenreDetailViewModel` | LibraryRepository | `GenreDetailActivity` (parse `GenreFeedDto` → sections) |
| `EditPlaylistViewModel` | LibraryRepository | `EditPlaylistActivity` (xoá + reorder bài → `PUT /tracks/order`) |
| `AddTracksViewModel` | LibraryRepository | `AddTracksBottomSheet` (3 tab + search, add bài vào playlist) |
| `RecentViewModel` | LibraryRepository | `RecentActivity` / `RecentFragment` |
| `PlayerViewModel` | PlayerRepository | `PlayerActivity` |
| `SubscriptionViewModel` | SubscriptionRepository | `PremiumPlansActivity` + `PremiumFragment` + `MainActivity` (ẩn/hiện ad theo Premium) |
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

23 Activity ở package gốc (giữ vị trí cũ để không phải sửa `AndroidManifest.xml`). Lưu ý: các Activity chi tiết (`AlbumDetail/ArtistDetail/PlaylistDetail/Recent/LikedSongs`) giờ chủ yếu là **fallback** khi mở ngoài `MainActivity` — luồng chính dùng Fragment (xem §2.1).

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
| `EditPlaylistActivity` | "Chỉnh sửa danh sách phát": nút trừ xoá bài, tay nắm 3 gạch kéo sắp xếp (`ItemTouchHelper`), Lưu → `PUT /api/playlists/{id}/tracks/order`. Hỏi xác nhận khi back nếu đã sửa. |
| `GenreDetailActivity` | Màn thể loại: header màu dominant từ cover (`Palette`) + nhiều section (`GenreFeedDto`) render qua `GenreFeedAdapter`. |
| `LikedSongsActivity` | Màn "Bài hát đã thích" độc lập (gradient header). Cũng có bản `LikedSongsFragment` cho Fragment-in-Main. |
| `RecentActivity` | "Nghe gần đây" — list từ `/api/history/recent`. |
| `PremiumPlansActivity` | Render `PlanInfo` (INDIVIDUAL/STUDENT/FAMILY) + subscribe → mở `PaymentActivity` qua `payUrlEvent`, nhận kết quả qua `registerForActivityResult`. |
| `PaymentActivity` | WebView mở VNPay sandbox. Bắt deep link `musicapp://payment/result?status=…` để trả kết quả. **Rewrite host `localhost`/`127.0.0.1` → host của `RetrofitClient.BASE_URL`** (tránh ERR_CONNECTION_REFUSED — xem memory `project_vnpay_localhost_gotcha`). |
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
| `LikedTracksFragment` | Danh sách bài đã like (subtab Library). |
| `FollowingArtistsFragment` | Danh sách nghệ sĩ đang follow. |
| `PremiumFragment` | Tab "Premium". |
| **`BaseDetailFragment`** | Lớp cha cho các màn chi tiết dạng Fragment — hoãn việc nặng tới sau animation vào (`onEnterAnimationDone()`) để trượt mượt. |
| **`AlbumDetailFragment`** / **`ArtistDetailFragment`** / **`PlaylistDetailFragment`** / **`RecentFragment`** / **`LikedSongsFragment`** | Bản Fragment của các màn chi tiết, host trong `MainActivity` qua `NavHelper.openDetail()` (xem §2.1). Chia sẻ ViewModel với Activity tương ứng. |
| `AddToPlaylistBottomSheet` | Chọn playlist để thêm track vào (dùng `PlaylistPickerAdapter`). |
| **`AddTracksBottomSheet`** | "Thêm vào danh sách phát" — 3 tab + ô tìm kiếm, nhấn + để thêm bài vào playlist hiện tại (`AddTracksViewModel` + `AddTrackAdapter`). |
| `CreateBottomSheet` | Tab "Tạo" — chọn loại item muốn tạo (playlist mới, playlist cộng tác…). |
| `TrackMenuBottomSheet` | Menu dài cho track: like/unlike, add to playlist, view artist, share… |
| `PlaylistEditBottomSheet` | Sửa tên + public/private + xoá playlist. **Share VM với `PlaylistDetailActivity`** qua `new ViewModelProvider(requireActivity(), ...).get(PlaylistDetailViewModel.class)`. |
| **`PremiumGateBottomSheet`** | Popup yêu cầu nâng cấp khi user Free chạm tính năng Premium (`newInstance("Chất lượng âm thanh cao")`) → mở `PremiumPlansActivity`. |
| **`ShuffleGateBottomSheet`** | Popup khi user Free cố tắt trộn bài (Free bị ép luôn bật trộn) → CTA "Khám phá Premium". |

---

## 9. Adapter (`adapter/`) — 32 file

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
| `GenreFeedAdapter` | `item_genre_section.xml` | Multi-section feed cho `GenreDetailActivity` (`GenreSectionDto`). |
| `EditPlaylistTrackAdapter` | `item_edit_playlist_track.xml` | Track row có nút trừ (xoá) + tay nắm kéo; `getItems()` trả thứ tự hiện tại để lưu reorder. |
| `AddTrackAdapter` | `item_add_track.xml` | Track row có nút "+" cho `AddTracksBottomSheet`. |

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
| `PlayerManager` | **Singleton** wrap ExoPlayer (media3). Quản lý queue + currentIndex + currentTrack, expose `play(ctx, track, list, index)`, `togglePlayPause`, `seekTo`, `playNext/Previous`, auto-`playNext` khi `STATE_ENDED`. Có `OnTrackChangeListener` (slot chính cho mini-player/PlayerActivity) **+ `setAdListener` (slot phụ cho AdManager đếm bài)**. Tắt `setEnableAudioTrackPlaybackParams(false)` để tránh speed drift trên emulator. |
| `AdManager` | Static AdMob helper (test IDs). Banner do layout khai báo; lớp này lo **interstitial**: cứ mỗi 3 bài (user Free) hiện 1 lần, `pendingShow` để hoãn lúc đang chuyển màn (không có Activity foreground), tự pause/resume nhạc quanh ad. `setPremium(true)` thì bỏ qua hết. |
| `PremiumChecker` | Quy tắc chuẩn xác định Premium từ `Subscription`: `plan != FREE/null` **và** `active == true` **và** `pending != true`. Dùng chung Main (ẩn ad) / Player (badge HQ) / Settings (gate). |
| `NavHelper` | Router mở màn chi tiết: trong Main → Fragment, ngoài Main → Activity (xem §2.1). |
| `BottomNavHelper` | Gắn bottom-nav 5 icon cho **Activity ngoài Main**; bấm icon → về Main đúng tab (CLEAR_TOP\|SINGLE_TOP), "Tạo" mở `CreateBottomSheet` tại chỗ. |
| `MiniPlayerController` | Mini-player cho **Activity ngoài Main**: poll `PlayerManager` mỗi 500ms (không tranh listener slot chính). Activity chỉ include `layout_mini_player` + gọi `onResume()/onPause()`. |
| `LrcParser` | Parse `.lrc`-style lyrics (`[mm:ss.xx]text`) thành `List<LyricLine>` cho `LyricsActivity`. |
| `TimeUtil` | Format ms → `mm:ss`. |
| `ItemAnim` | Helper RecyclerView item entrance animation (fade + slide-up 300ms decelerate, không re-animate khi scroll back). |

---

## 12. Model (`model/`) — 25 POJO

POJO khớp 1-1 với DTO/Entity backend, dùng Gson de/serialize. Field public-free (private + getter).

| Model | Tương ứng backend |
|-------|-------------------|
| `User`, `UserMe`, `UserProfile`, `UserSettings` | `Users` + `User_Settings` |
| `Artist`, `Album`, `Track`, `Genre`, `Playlist` | Entity gốc cùng tên |
| `RecentItem` | `RecentItemDto { track, playedAt }` |
| `HomeSection` | `HomeSectionDto { kind, title, subtitle, items }` — `items` là `List<Object>` để parse runtime theo `kind` |
| `Subscription`, `PlanInfo` | `Subscriptions` (có `active` + **`pending`**) + `PlanInfoDto` |
| `GenreFeedDto`, `GenreSectionDto` | Feed thể loại — `GET /api/genres/{id}/feed`, nhiều section render qua `GenreFeedAdapter` |
| `ListeningStats` | `/api/stats/listening` response |
| `LoginRequest`, `RegisterRequest`, `JwtResponse` | Auth DTO |
| `PlaylistRequest`, `ProfileUpdateRequest`, `SubscribeRequest` | Request body |
| `PlaylistReorderRequest` | Body cho `PUT /api/playlists/{id}/tracks/order` (thứ tự trackId mới) |
| `SubscribeResponse` | `{ subscriptionId }` — sub mới `active=false, pending=true`, **chưa premium**, phải tiếp tục flow payment |
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

### 13.7 Sửa danh sách bài (thêm / xoá / kéo sắp xếp)

```
PlaylistDetail → "Thêm bài"
  → AddTracksBottomSheet (3 tab + search)
  → nhấn + → AddTracksViewModel → POST /api/playlists/{id}/tracks?trackId=
  → callback onTracksChanged() → PlaylistDetail reload

PlaylistDetail → "Chỉnh sửa"
  → EditPlaylistActivity (load tracks vào EditPlaylistTrackAdapter)
  → nút trừ: adapter.removeAt(pos)
  → tay nắm 3 gạch: ItemTouchHelper.startDrag → kéo đổi vị trí
  → "Lưu": vm.save(adapter.getItems())
     → repo.reorder → PUT /api/playlists/{id}/tracks/order (PlaylistReorderRequest)
  → back khi đã sửa → AlertDialog xác nhận huỷ
```

### 13.8 Premium — subscribe + VNPay payment

```
PremiumPlansActivity → chọn plan → vm.subscribe(plan)
  → POST /api/subscriptions/subscribe → SubscribeResponse { subscriptionId }
     (sub mới active=false, pending=true — CHƯA premium)
  → POST /api/payment/create → { payUrl }
  → vm.payUrlEvent → paymentLauncher.launch(PaymentActivity(payUrl))

PaymentActivity (WebView VNPay sandbox)
  → rewrite host localhost/127.0.0.1 → host RetrofitClient.BASE_URL
  → user thanh toán → backend 302 → musicapp://payment/result?status=success|failed
  → Activity bắt deep link → setResult(status) → finish()
  → PremiumPlansActivity nhận status → reload subscription
  → MainActivity.onResume → subVm.loadCurrentSubscription
     → PremiumChecker.isPremium → AdManager.setPremium(true) → ẩn banner
```

### 13.9 Quảng cáo (AdMob) cho user Free

```
MusicApp.onCreate → MobileAds.initialize + theo dõi Activity foreground
MusicApp gắn listener PHỤ vào PlayerManager (setAdListener)
  → mỗi bài mới: AdManager.onTrackPlayed(currentActivity)
     → premium? bỏ qua : trackCount++
     → cứ 3 bài → pendingShow = true → maybeShowPending()
        → có Activity foreground + ad đã load → pause nhạc → show interstitial
        → đóng ad → resume nhạc + preload ad kế
Banner: layout activity_main khai báo adUnitId; Main load 1 lần nếu Free, GONE nếu Premium.
Gate: chạm tính năng Premium (HQ, tắt trộn) → PremiumGateBottomSheet / ShuffleGateBottomSheet.
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
| GenreDetail | GenreDetailViewModel | LibraryRepository | `GET /api/genres/{id}/feed` |
| EditPlaylist | EditPlaylistViewModel | LibraryRepository | `GET /api/playlists/{id}/tracks`, `PUT /api/playlists/{id}/tracks/order` |
| AddTracks (bottom sheet) | AddTracksViewModel | LibraryRepository | `GET /api/search` / liked / playlists, `POST /api/playlists/{id}/tracks` |
| Recent | RecentViewModel | LibraryRepository | `GET /api/history/recent?limit=` |
| Player | PlayerViewModel | PlayerRepository | `POST /api/history?trackId=` |
| Lyrics | (đọc thẳng từ `Track.lyrics`) | — | — |
| Premium plans + Payment | SubscriptionViewModel | SubscriptionRepository | `GET /api/subscriptions/me + /plans`, `POST /subscribe + /cancel`, `POST /api/payment/create` |
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
11. **VNPay `vnp_ReturnUrl` trỏ `localhost`** → WebView ERR_CONNECTION_REFUSED. `PaymentActivity` rewrite host `localhost`/`127.0.0.1` → host của `RetrofitClient.BASE_URL`. Xem memory `project_vnpay_localhost_gotcha`.
12. **`SubscribeResponse` chưa nghĩa là Premium** — sub mới `pending=true, active=false`; phải qua payment xong backend mới set active. Dùng `PremiumChecker.isPremium()` (kiểm cả `pending`) thay vì chỉ check `plan`.
13. **AdMob dùng listener PHỤ của PlayerManager** (`setAdListener`) — KHÔNG tranh slot chính (`OnTrackChangeListener`) của mini-player. `MusicApp` re-attach mỗi `onActivityResumed` vì `PlayerManager` có thể bị `release()` khi logout.
14. **Detail mở 2 đường** (Fragment trong Main / Activity ngoài Main) — luôn gọi qua `NavHelper`, đừng `startActivity(XxxDetailActivity)` trực tiếp nếu đang ở Main (sẽ mất bottom-nav/mini-player liền mạch).

---

## 17. Trạng thái dự án

- **MVVM migration:** Hoàn tất 7/7 phase — toàn bộ Activity/Fragment đã refactor sang ViewModel + Repository + ViewBinding. Không còn `findViewById` hay Retrofit call trong View.
- **UI sprint (giaodien.pdf):** Đã làm tới bước 8b (shimmer skeleton trên Library/Search/Playlist/Album/Home, RecyclerView item anim, Fragment transitions, palette gradient, Spotify token, Montserrat font).
- **Điều hướng Detail-in-Main:** màn chi tiết chuyển sang Fragment host trong MainActivity (`NavHelper` + `BaseDetailFragment`), bottom-nav/mini-player liền mạch; Activity cũ giữ làm fallback.
- **Playlist editing**: hoàn tất CRUD + upload cover + auto-cover + **thêm bài (`AddTracksBottomSheet`) + xoá/kéo sắp xếp (`EditPlaylistActivity` → reorder endpoint)**.
- **Genre feed/detail**: `GenreDetailActivity` + `GenreFeedAdapter` render `/api/genres/{id}/feed`.
- **Premium đã wire**: VNPay payment (`PaymentActivity`), AdMob banner + interstitial (`MusicApp` + `AdManager`), premium gate (`PremiumGateBottomSheet` HQ audio, `ShuffleGateBottomSheet` ép trộn bài), `PremiumChecker` quy tắc chuẩn.
- **Còn lại / TODO:**
    - Lyrics đồng bộ — chỉ render static hiện tại, chưa scroll theo timestamp 100% (xem `LYRICS_SYNC_SPEC.md`).
    - PlayerViewModel.recordPlay chưa được gọi tự động sau 30s — phải tự gọi từ PlayerActivity khi đủ ngưỡng.
    - Offline download tracking (Premium) — chưa làm.
    - Test FE: chưa có unit test/UI test.
    - Các thay đổi Detail-Fragment + EditPlaylist + LikedSongs hiện **chưa commit** (working tree).

---

> File này được sinh từ scan source code thực tế. Khi có thay đổi lớn (thêm Activity/ViewModel/Repository, sửa cấu trúc package, đổi base URL, đổi pattern MVVM), nhớ cập nhật lại các bảng tương ứng.
