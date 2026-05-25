# THUYẾT TRÌNH ĐỒ ÁN — Music Streaming App (Spotify Clone Android)

> File này dùng để **thuyết trình + bảo vệ** trước thầy. Nó giải thích **mọi thành phần** trong project: layout nào có item gì, drawable nào để làm gì, activity/fragment/adapter/model/viewmodel/network dùng vào việc gì và **luồng dữ liệu chạy ra sao**.
>
> Tài liệu đi kèm: `FRONTEND_OVERVIEW.md` (đào sâu kiến trúc MVVM), `SODO_MAN_HINH_VA_API.md` (sơ đồ điều hướng màn hình + danh sách đầy đủ 53 API) và `app/.claude/CLAUDE.md` (backend Spring Boot). File này tổng hợp lại cho dễ trình bày.

---

## MỤC LỤC

1. [Tóm tắt 30 giây — nói gì khi thầy hỏi "Đây là app gì?"](#1)
2. [Công nghệ dùng (tech stack)](#2)
3. [Bản đồ thư mục — cái gì nằm ở đâu](#3)
4. [Kiến trúc MVVM — luồng dữ liệu tổng](#4)
5. [Tầng Network — nói chuyện với server](#5)
6. [Model — các "cục dữ liệu"](#6)
7. [Repository — kho lấy dữ liệu](#7)
8. [ViewModel — bộ não của mỗi màn hình](#8)
9. [Activity — từng màn dùng layout/VM/adapter gì](#9)
10. [Fragment — các tab và bottom sheet](#10)
11. [Adapter — render danh sách cho RecyclerView](#11)
12. [Custom View + Util — thành phần dùng chung](#12)
13. [📐 CATALOG LAYOUT — từng file XML có những item nào](#13)
14. [🎨 CATALOG DRAWABLE — từng nhóm hình/nền để làm gì](#14)
15. [Quy ước đặt tên id — đọc 1 lần hiểu mọi layout](#15)
16. [Các luồng nghiệp vụ chính (kể từng bước)](#16)
17. [❓ Bộ câu hỏi thầy hay hỏi + cách trả lời](#17)

---

<a name="1"></a>
## 1. Tóm tắt 30 giây

> "Đây là **ứng dụng nghe nhạc trực tuyến kiểu Spotify** cho người Việt, viết bằng **Android Java**. App có 5 tab dưới cùng (Trang chủ · Tìm kiếm · Thư viện · Premium · Tạo), cho phép đăng nhập/đăng ký, nghe nhạc streaming bằng ExoPlayer, xem lời bài hát, quản lý playlist (tạo/sửa/xoá/đổi ảnh bìa), follow nghệ sĩ, like bài hát, xem thống kê nghe nhạc và đăng ký gói Premium. App kết nối tới **backend Spring Boot** qua REST API có xác thực **JWT**. Kiến trúc theo **MVVM** (Model–View–ViewModel) chuẩn của Android."

---

<a name="2"></a>
## 2. Công nghệ dùng (tech stack)

| Hạng mục | Công nghệ | Vai trò |
|----------|-----------|---------|
| Ngôn ngữ | **Java 11**, Android (`minSdk 24`, `targetSdk 36`) | Toàn bộ code app |
| Kiến trúc | **MVVM** + ViewBinding | Tách giao diện ↔ logic ↔ dữ liệu |
| Vòng đời/State | **Lifecycle 2.8.7** (`ViewModel` + `LiveData`) | Giữ state qua xoay màn hình, cập nhật UI tự động |
| Gọi mạng | **Retrofit 2.9 + Gson + OkHttp** | Gọi REST API, chuyển JSON ↔ object |
| Phát nhạc | **ExoPlayer (media3 1.3.1)** | Stream audio, hỗ trợ tua (HTTP Range) |
| Tải ảnh | **Glide 4.16** (+ transformations) | Load ảnh bìa/avatar từ URL, bo góc, cache |
| Giao diện | **Material 3**, CircleImageView, ViewPager2, Palette, Facebook Shimmer | Component UI, ảnh tròn, hiệu ứng loading |
| Backend | **Spring Boot 4 + MySQL + JWT** (dự án riêng) | Cung cấp API, lưu DB, xác thực |

**Base URL** server: `http://10.0.2.2:8080/musicapp/` (máy ảo) hoặc `http://<IP_LAN>:8080/musicapp/` (máy thật).

**Câu chốt nếu thầy hỏi "vì sao chọn MVVM, không gọi API thẳng trong Activity?"** → Vì tách bạch giúp **dễ test, dễ bảo trì**, ViewModel **sống sót khi xoay màn hình** (không mất dữ liệu), và Activity chỉ lo hiển thị nên gọn gàng.

---

<a name="3"></a>
## 3. Bản đồ thư mục — cái gì nằm ở đâu

```
com.example.musicstreamingapp/
├── *Activity.java        → 18 màn hình toàn trang (Login, Player, PlaylistDetail…)
├── adapter/              → 28 Adapter (đổ dữ liệu vào danh sách RecyclerView)
├── data/
│   ├── Resource.java     → bọc trạng thái Loading / Success / Error
│   ├── Event.java        → sự kiện 1-lần (snackbar, điều hướng) tránh phát lại
│   ├── RepoCallback.java → interface callback onSuccess/onError
│   └── repository/       → 7 Repository (kho lấy dữ liệu, bọc Retrofit)
├── fragment/             → 11 Fragment + BottomSheet (5 tab + các sheet)
├── model/                → 21 POJO/DTO (cấu trúc dữ liệu khớp với backend)
├── network/              → ApiService + RetrofitClient + AuthInterceptor
├── ui/                   → PlaylistCoverView (custom view ảnh bìa playlist)
├── util/                 → PlayerManager, TokenManager, AccountStore, LrcParser…
└── viewmodel/            → 22 ViewModel + VmFactory (tạo ViewModel)
```

Tài nguyên giao diện:
```
res/
├── layout/    → 71 file XML (bố cục từng màn + từng item danh sách)
├── drawable/  → ~95 file XML (icon vector + nền/shape + selector + placeholder)
├── values/    → màu (colors), chuỗi (strings), theme (themes), dimens
├── menu/      → bottom_nav_menu (menu 5 tab dưới)
└── font/      → Montserrat (font chữ offline)
```

**Cách giải thích cho thầy:** "Em tổ chức theo **chức năng từng tầng**: `model` là dữ liệu, `network` lo kết nối, `repository` lấy dữ liệu, `viewmodel` xử lý logic màn hình, `Activity/fragment` hiển thị, `adapter` đổ dữ liệu vào list, `res` chứa giao diện."

---

<a name="4"></a>
## 4. Kiến trúc MVVM — luồng dữ liệu tổng

```
   NGƯỜI DÙNG bấm nút
        │
        ▼
 ┌─────────────────┐   observe LiveData    ┌──────────────────┐
 │  View           │ ◄──────────────────── │  ViewModel       │
 │ Activity/Fragment│ ──── gọi hàm ───────► │ (logic màn hình) │
 └─────────────────┘                        └────────┬─────────┘
        ▲ cập nhật                                    │ gọi
        │ giao diện                                   ▼
        │                                    ┌──────────────────┐
        │                                    │  Repository      │
        │                                    │ (kho dữ liệu)    │
        │                                    └────────┬─────────┘
        │                                             │ enqueue
        │                                             ▼
        │                                    ┌──────────────────┐
        │                                    │  ApiService      │ Retrofit
        │                                    └────────┬─────────┘
        │                                             │ HTTP + JWT
        │                                             ▼
        └────────────────────────────────── Spring Boot Backend
```

**Quy tắc vàng (nói được câu này là ăn điểm):**
1. **Activity/Fragment KHÔNG gọi mạng**, chỉ làm 2 việc: *quan sát LiveData* để vẽ giao diện, và *bắt sự kiện người dùng* rồi gọi ViewModel.
2. **ViewModel** giữ trạng thái màn hình bằng `LiveData`, gọi Repository khi cần dữ liệu. Sống sót khi xoay màn hình.
3. **Repository** bọc Retrofit thành callback đơn giản (`onSuccess`/`onError`), không dính tới vòng đời màn hình.
4. **ViewBinding** thay cho `findViewById` — gõ `binding.tvTitle` là ra view, an toàn null.
5. Không dùng coroutine/RxJava cho gọn — chỉ Retrofit `enqueue` (bất đồng bộ) + `LiveData.postValue`.
6. Không dùng Hilt/Dagger — tự viết `VmFactory` để tạo ViewModel kèm Repository.

---

<a name="5"></a>
## 5. Tầng Network — nói chuyện với server (`network/`)

| Class | Vai trò |
|-------|---------|
| **`ApiService`** | Interface Retrofit khai báo **toàn bộ ~46 endpoint** (login, home feed, tracks, albums, artists, playlists CRUD + upload ảnh bìa, charts, search, subscription…). Mỗi hàm = 1 lời gọi API. |
| **`RetrofitClient`** | Singleton tạo đối tượng Retrofit. Giữ `BASE_URL` (gọi API) và `BASE_MEDIA_URL` (ghép trước đường dẫn ảnh/nhạc). Cài sẵn `AuthInterceptor` + log. Có `reset()` để dựng lại khi đổi tài khoản. |
| **`AuthInterceptor`** | Tự động đính kèm header `Authorization: Bearer <token>` vào mọi request (trừ login/register). Gặp lỗi 401/403 (token hết hạn) thì báo `SessionManager` để app đá về màn Login. |

**Điểm cần nhớ:** ảnh và nhạc server trả về **đường dẫn tương đối** (`/images/...`, `/audio/...`). App phải **ghép `BASE_MEDIA_URL` vào trước** rồi mới đưa cho Glide (ảnh) / ExoPlayer (nhạc).

---

<a name="6"></a>
## 6. Model — các "cục dữ liệu" (`model/`, 21 file)

Model là **class chỉ chứa dữ liệu** (POJO), khớp 1-1 với dữ liệu JSON backend trả về. Gson tự chuyển JSON ↔ object.

| Nhóm | Model | Là gì |
|------|-------|-------|
| Người dùng | `User`, `UserMe`, `UserProfile`, `UserSettings` | Thông tin tài khoản, hồ sơ, cài đặt |
| Nội dung nhạc | `Track`, `Album`, `Artist`, `Genre`, `Playlist` | Bài hát, album, nghệ sĩ, thể loại, playlist |
| Trang chủ | `HomeSection` | 1 "khối" trên Home (tiêu đề + loại + danh sách item). `items` để kiểu `Object` rồi đọc theo `kind` lúc chạy |
| Lịch sử | `RecentItem` | Bài vừa nghe gần đây (`track` + thời điểm) |
| Premium | `Subscription`, `PlanInfo`, `SubscribeRequest` | Gói đang dùng, thông tin gói, body đăng ký |
| Auth | `LoginRequest`, `RegisterRequest`, `JwtResponse` | Body đăng nhập/đăng ký, token trả về |
| Khác | `SearchResult` (`{tracks, artists}`), `ListeningStats`, `PlaylistRequest`, `ProfileUpdateRequest` | Kết quả tìm kiếm, thống kê nghe, body tạo/sửa |

---

<a name="7"></a>
## 7. Repository — kho lấy dữ liệu (`data/repository/`, 7 file)

Mỗi Repository nhận `ApiService` qua constructor, cung cấp hàm kiểu `getXxx(tham_số, callback)`.

| Repository | Lo việc gì |
|------------|------------|
| **`AuthRepository`** | Đăng nhập / đăng ký / đăng xuất; lưu token + username + role sau khi login |
| **`UserRepository`** | Hồ sơ cá nhân, sửa hồ sơ, upload avatar, cài đặt (`UserSettings`), thống kê nghe |
| **`HomeRepository`** | Lấy feed trang chủ → trả `List<HomeSection>` |
| **`LibraryRepository`** | **Lớn nhất** — albums, artists, tracks, genres, playlists (CRUD + upload ảnh bìa), like/unlike, follow/unfollow, nghệ sĩ đang follow, bài nghe gần đây… Hầu hết màn nghe nhạc đều dùng repo này |
| **`PlayerRepository`** | Ghi lượt nghe (`recordPlay`), lấy bài liên quan |
| **`SearchRepository`** | Tìm kiếm → trả `SearchResult {tracks, artists}` |
| **`SubscriptionRepository`** | Gói Premium: lấy gói hiện tại, danh sách gói, đăng ký, huỷ |

**2 hàm helper dùng chung trong repo:**
- `enqueue(...)` — biến callback của Retrofit thành `onSuccess`/`onError`; HTTP thành công → onSuccess, ngược lại → onError("HTTP <mã lỗi>").
- `boolCb(...)` — bọc các endpoint trả `Map` thành callback boolean (dùng cho like/follow toggle).

---

<a name="8"></a>
## 8. ViewModel — bộ não của mỗi màn hình (`viewmodel/`, 22 + VmFactory)

Mỗi màn hình có 1 ViewModel. ViewModel **giữ state qua `LiveData`** và phát **sự kiện 1-lần qua `Event<T>`** (snackbar/điều hướng).

| ViewModel | Repo dùng | Cho màn |
|-----------|-----------|---------|
| `LoginViewModel` / `RegisterViewModel` | AuthRepository | Login / Register |
| `MainViewModel` | UserRepository | MainActivity (chữ cái avatar cho drawer) |
| `HomeViewModel` | HomeRepository | HomeFragment |
| `SearchViewModel` | SearchRepository | SearchFragment |
| `LibraryViewModel` | LibraryRepository | LibraryFragment (lọc Playlist/Artist/Album/Liked) |
| `PlaylistsViewModel` / `LikedTracksViewModel` / `FollowingArtistsViewModel` | LibraryRepository | 3 tab con trong Thư viện |
| `AlbumDetailViewModel` / `ArtistDetailViewModel` / `PlaylistDetailViewModel` | LibraryRepository | 3 màn chi tiết |
| `RecentViewModel` | LibraryRepository | "Nghe gần đây" |
| `PlayerViewModel` | PlayerRepository | Màn phát nhạc |
| `SubscriptionViewModel` | SubscriptionRepository | Premium |
| `ProfileViewModel` / `EditProfileViewModel` / `ListeningStatsViewModel` / `SettingsViewModel` | UserRepository | Hồ sơ / sửa hồ sơ / thống kê / cài đặt |
| `AddArtistViewModel` / `AddToPlaylistViewModel` | LibraryRepository | Thêm nghệ sĩ / thêm bài vào playlist |

**`VmFactory`** là "xưởng" tạo ViewModel: nhận `Class<T>`, tự tạo `ApiService` + Repository tương ứng rồi `new` ViewModel. (Vì không dùng Hilt nên tự viết tay.)

**`PlaylistDetailViewModel` đáng nhớ** (hay bị hỏi): có enum `EditResult {UPDATED, DELETED, UPDATE_FAILED, DELETE_FAILED}` để báo kết quả sau khi bottom sheet sửa playlist; `loadSuggestionsIfNeeded()` gợi ý bài khi playlist rỗng; `reload()` refresh ảnh bìa khi quay lại từ màn chọn ảnh.

---

<a name="9"></a>
## 9. Activity — từng màn dùng layout/VM/adapter gì

> Activity dùng **ViewBinding**: tên layout `activity_player.xml` → class binding `ActivityPlayerBinding`.

| Activity | Layout | ViewModel | Adapter/Thành phần chính | Nhiệm vụ |
|----------|--------|-----------|--------------------------|----------|
| `SplashActivity` | `activity_splash` | — | — | Màn mở app. Kiểm tra đã đăng nhập chưa → vào Main hoặc Login |
| `LoginActivity` | `activity_login` | LoginViewModel | — | Đăng nhập |
| `RegisterActivity` | `activity_register` | RegisterViewModel | — | Đăng ký |
| `AddAccountActivity` | `activity_add_account` | LoginViewModel | — | Thêm tài khoản (đa tài khoản) |
| `MainActivity` | `activity_main` | MainViewModel | (chứa 5 fragment) | Khung chính: 5 tab dưới + drawer + mini player |
| `PlayerActivity` | `activity_player` | PlayerViewModel | ExploreArtistAdapter | Màn phát nhạc toàn trang |
| `LyricsActivity` | `activity_lyrics` | — | LyricLineAdapter | Lời bài hát cuộn theo timestamp |
| `AlbumDetailActivity` | `activity_album_detail` | AlbumDetailViewModel | TrackDetailAdapter | Chi tiết album + danh sách bài |
| `ArtistDetailActivity` | `activity_artist_detail` | ArtistDetailViewModel | ArtistTrackAdapter, ArtistAlbumAdapter, ExploreArtistAdapter | Chi tiết nghệ sĩ + follow |
| `PlaylistDetailActivity` | `activity_playlist_detail` | PlaylistDetailViewModel | TrackDetailAdapter, SuggestedTrackAdapter, PlaylistCoverView | Chi tiết playlist + sửa/xoá/đổi bìa |
| `PlaylistCoverPickerActivity` | `activity_playlist_cover_picker` | — | PlaylistCoverView | Chọn ảnh từ máy → upload làm bìa |
| `RecentActivity` | `activity_recent` | RecentViewModel | TrackListAdapter | Bài nghe gần đây |
| `PremiumPlansActivity` | `activity_premium_plans` | SubscriptionViewModel | — | Các gói Premium + đăng ký |
| `ProfileActivity` | `activity_profile` | ProfileViewModel | PlaylistAdapter | Hồ sơ (của mình hoặc người khác) |
| `EditProfileActivity` | `activity_edit_profile` | EditProfileViewModel | — | Sửa tên, tiểu sử, avatar |
| `ListeningStatsActivity` | `activity_listening_stats` | ListeningStatsViewModel | (bơm `item_stats_section`) | Top nghệ sĩ/bài theo tuần/tháng/năm |
| `SettingsActivity` | `activity_settings` | SettingsViewModel | — | 5 công tắc cài đặt + đăng xuất |
| `AddArtistActivity` | `activity_add_artist` | AddArtistViewModel | SelectableArtistAdapter | Tìm + follow nhiều nghệ sĩ |

---

<a name="10"></a>
## 10. Fragment — các tab và bottom sheet (`fragment/`)

| Fragment | Layout | ViewModel | Là gì |
|----------|--------|-----------|-------|
| `HomeFragment` | `fragment_home` | HomeViewModel | Tab **Trang chủ** — feed nhiều khối qua `HomeFeedAdapter` |
| `SearchFragment` | `fragment_search` | SearchViewModel | Tab **Tìm kiếm** — chưa gõ thì hiện lưới thể loại, gõ rồi thì hiện kết quả |
| `LibraryFragment` | `fragment_library` | LibraryViewModel | Tab **Thư viện** — 4 chip lọc, dùng `LibraryAdapter` đa kiểu |
| `PremiumFragment` | `fragment_premium` | SubscriptionViewModel | Tab **Premium** — quảng bá lợi ích + nút bắt đầu |
| `PlaylistsFragment` | `fragment_playlists` | PlaylistsViewModel | Danh sách playlist (dùng `PlaylistAdapter`) |
| `LikedTracksFragment` | `fragment_liked_tracks` | LikedTracksViewModel | Danh sách bài đã thích |
| `FollowingArtistsFragment` | `fragment_following_artists` | FollowingArtistsViewModel | Nghệ sĩ đang follow + FAB thêm nghệ sĩ |
| `CreateBottomSheet` | `bottom_sheet_create` | — | Tab **Tạo** — chọn loại muốn tạo (playlist/cộng tác/blend) |
| `AddToPlaylistBottomSheet` | `bottom_sheet_add_to_playlist` | AddToPlaylistViewModel | Chọn playlist để thêm bài vào (dùng `PlaylistPickerAdapter`) |
| `TrackMenuBottomSheet` | `bottom_sheet_track_menu` | — | Menu cho 1 bài: thích/thêm vào playlist/tới album |
| `PlaylistEditBottomSheet` | `sheet_playlist_edit` | PlaylistDetailViewModel (chung VM với Activity) | Sửa tên + công khai/riêng tư + xoá playlist |

**Điểm hay bị hỏi:** `PlaylistEditBottomSheet` **dùng chung ViewModel** với `PlaylistDetailActivity` (qua `requireActivity()`), nên sửa xong Activity tự cập nhật mà không cần truyền dữ liệu thủ công.

---

<a name="11"></a>
## 11. Adapter — đổ dữ liệu vào RecyclerView (`adapter/`, 28 file)

> Adapter = cầu nối giữa **danh sách dữ liệu** và **danh sách hiển thị** (RecyclerView). Mỗi adapter "thổi phồng" (inflate) 1 layout `item_*` cho mỗi dòng.

| Adapter | Layout item | Dùng ở |
|---------|-------------|--------|
| `HomeFeedAdapter` | `item_home_section`, `item_home_featured` | Home — **đa kiểu**, mỗi khối spawn adapter con khác nhau |
| `PlaylistCardAdapter` | `item_playlist_card` | Carousel playlist trên Home |
| `TrackCardAdapter` | `item_track_card` | Carousel bài hát trên Home |
| `AlbumCardAdapter` / `AlbumLabelAdapter` | `item_album_card` / `item_album_label_card` | Carousel album |
| `ArtistCircleAdapter` / `ArtistCardAdapter` / `ArtistListAdapter` | `item_artist_circle` / `item_artist_card` / `item_artist_list` | Nghệ sĩ (tròn / thẻ / dòng) |
| `ChartCardAdapter` | `item_home_chart` | Khối bảng xếp hạng |
| `RadioCardAdapter` | `item_radio_card` | Khối radio |
| `RecentSectionAdapter` / `RecentTileAdapter` | `item_recent_header`+`item_track_list` / `item_home_recent_tile` | "Nghe gần đây" (lưới 2 cột) |
| `HomeTrackRowAdapter` | `item_track_row` | Dòng bài trong khối Home |
| `ExploreCardAdapter` / `ExploreArtistAdapter` | `item_explore_card` / `item_explore_artist_card` | Khám phá (Search + Player) |
| `GenreTileAdapter` | `item_genre_tile` | Lưới thể loại ở tab Tìm kiếm |
| `LibraryAdapter` | `item_library_row` + `item_library_artist` + `item_library_action` | Thư viện — **đa kiểu** (playlist/artist/album/track/action) |
| `PlaylistAdapter` | `item_playlist_list` | Danh sách playlist (Profile, PlaylistsFragment) |
| `TrackDetailAdapter` | `item_track_detail` | Danh sách bài trong Album/Playlist (có nút "...") |
| `TrackListAdapter` | `item_track_list` | Danh sách bài chung (Recent…) |
| `ArtistTrackAdapter` | `item_artist_track` | Bài phổ biến của nghệ sĩ (có số thứ hạng) |
| `ArtistAlbumAdapter` | `item_artist_album_vertical` | Album của nghệ sĩ |
| `SuggestedTrackAdapter` | `item_suggested_track` | Gợi ý bài khi playlist rỗng (nút "+") |
| `LyricLineAdapter` | `item_lyric_line` | Từng dòng lời bài hát (tô sáng dòng đang hát) |
| `SelectableArtistAdapter` | `item_selectable_artist` | Chọn nhiều nghệ sĩ để follow |
| `PlaylistPickerAdapter` | `item_playlist_picker(+_section)` | Chọn playlist để thêm bài |
| `AccountAdapter` | `item_drawer_account` + `item_drawer_add_account` | Danh sách tài khoản trong drawer |
| `LibraryPagerAdapter` | (ViewPager2) | Quản lý các tab con trong Thư viện |

**Đặc biệt cần nhớ:**
- `HomeFeedAdapter` và `LibraryAdapter` là **đa kiểu (multi view-type)** — 1 danh sách nhưng nhiều dạng dòng khác nhau, chọn layout theo `kind`/`type`.
- Mọi adapter chia sẻ hiệu ứng `ItemAnim` — dòng hiện ra với fade + trượt lên 300ms (chỉ animate lần đầu).

---

<a name="12"></a>
## 12. Custom View + Util — thành phần dùng chung

### Custom View (`ui/`)
| Class | Vai trò |
|-------|---------|
| **`PlaylistCoverView`** | View ảnh bìa playlist "thông minh", inflate `view_playlist_cover.xml`. Hàm `bind(coverUrl, tracks)`:<br>• có `coverUrl` → 1 ảnh<br>• ≥4 bài → ghép lưới 2×2 từ 4 ảnh bìa đầu<br>• 1–3 bài → ảnh bài đầu<br>• rỗng → placeholder (nốt nhạc nền xám). |

### Util (`util/`)
| Class | Vai trò |
|-------|---------|
| **`PlayerManager`** | **Singleton** bọc ExoPlayer. Giữ hàng đợi (queue) + bài hiện tại; có `play/togglePlayPause/seekTo/playNext/playPrevious`; tự next khi hết bài. Báo cho mini-player + PlayerActivity cùng đồng bộ qua listener |
| **`TokenManager`** | Lưu `token`, `username`, `role` vào SharedPreferences; có `isLoggedIn()`, `saveAuth()`, `clear()` |
| **`AccountStore`** | Đa tài khoản: lưu danh sách tài khoản đã đăng nhập để chuyển nhanh |
| **`SessionManager`** | Cờ `expired` báo token hết hạn (do `AuthInterceptor` set) để đá về Login |
| **`LrcParser`** | Phân tích lời dạng `[mm:ss.xx]lời` thành danh sách dòng có timestamp |
| **`TimeUtil`** | Đổi mili-giây → `mm:ss` |
| **`ItemAnim`** | Hiệu ứng xuất hiện cho item RecyclerView |

---

<a name="13"></a>
## 13. 📐 CATALOG LAYOUT — từng file XML có những item nào

> **Cách đọc:** prefix id cho biết loại view — `tv`=TextView (chữ), `iv`=ImageView (ảnh), `btn`=Button/ImageButton (nút), `rv`=RecyclerView (danh sách), `et`=EditText (ô nhập), `til`=TextInputLayout (khung ô nhập), `sw`=Switch (công tắc), `card`=CardView, `fl`=FrameLayout, `fab`=nút tròn nổi, `chip`=Chip lọc, `row`=dòng bấm được, `shimmer`=khung loading nhấp nháy. (Xem thêm mục 15.)

### A. Layout màn hình toàn trang (`activity_*`)

**`activity_splash`** — màn mở app: `iv_logo` (logo), `tv_app_name` (tên app), `progress_bar` (vòng quay).

**`activity_login`** — đăng nhập: `iv_logo`, `tv_title`, `til_username`+`et_username`, `til_password`+`et_password`, `btn_login`, `progress_bar`, `tv_register_link` (chuyển sang đăng ký).

**`activity_register`** — đăng ký: như login + thêm `til_email`+`et_email`, `btn_register`, `tv_login_link`.

**`activity_add_account`** — thêm tài khoản: `toolbar`, `btn_register`, `btn_login`.

**`activity_main`** — khung chính (gốc là `DrawerLayout`):
- `drawer_layout` (cả màn có thể kéo ngăn bên), `main` (vùng nội dung).
- `fragment_container` (chỗ gắn 5 tab), `mini_player` (include `layout_mini_player`, ẩn khi chưa phát), `bottom_nav` (thanh 5 tab).
- Ngăn trái include `drawer_content`.

**`activity_player`** — màn phát nhạc (đã đọc kỹ): `iv_bg_blur` (nền gradient lấy từ màu bìa), `btn_back` (mũi tên xuống), `tv_breadcrumb_top/bottom`, `btn_more`, `iv_cover` (ảnh bìa vuông), `tv_lyrics_teaser`, `tv_title`+`tv_artist`, `btn_like`, `seekbar`+`tv_current_time`+`tv_total_time`, hàng điều khiển `btn_shuffle / btn_prev / btn_play_pause / btn_next / btn_timer`, hàng dưới cast/share/queue, thẻ lời `tv_lyrics_card`+`btn_show_lyrics`, "Khám phá nghệ sĩ" `tv_explore_artist_title`+`rv_explore_artist`, phần credit `iv_credit_avatar`+`tv_credit_artist`+`btn_follow_artist`.

**`activity_lyrics`** — lời bài hát: `btn_lyrics_back`, `tv_lyrics_title`, `tv_lyrics_artist`, `rv_lyrics` (danh sách dòng lời).

**`activity_album_detail`** — chi tiết album (`CollapsingToolbar`): `collapsing_toolbar`, `iv_cover`, `toolbar`, `tv_artist_name`, `btn_play_all`, `btn_shuffle`, `rv_tracks` (list bài), `shimmer_album` (khung loading).

**`activity_artist_detail`** — chi tiết nghệ sĩ: `app_bar`+`collapsing_toolbar`+`iv_artist_banner`+`toolbar`, `tv_followers`, `iv_artist_thumb`, `btn_follow`, `btn_shuffle`, `btn_play`, `rv_top_tracks`+`btn_expand_tracks` (bài phổ biến), `tv_show_all_albums`+`rv_albums`, thẻ giới thiệu `card_about`+`iv_about_image`+`tv_about_name`+`tv_about_followers`, `tv_bio`+`btn_expand_bio`.

**`activity_playlist_detail`** — chi tiết playlist: `app_bar`+`collapsing_toolbar`, `cover_view` (PlaylistCoverView), `toolbar`, hàng nút `row_actions`+`btn_edit_details`+`btn_shuffle`+`btn_play_fab` (FAB phát), `rv_tracks`, `empty_state`+`rv_suggestions` (khi rỗng → gợi ý bài), `shimmer_playlist`.

**`activity_playlist_cover_picker`** — chọn ảnh bìa: `btn_close`, `cover_preview` (xem trước), `btn_change_cover`.

**`activity_premium_plans`** — gói Premium: `btn_back`, `btn_subscribe_individual`, `btn_subscribe_student`, `btn_subscribe_family`.

**`activity_profile`** — hồ sơ: `toolbar`, `iv_avatar`, `tv_name`, `tv_stats` (số follower…), `btn_edit`, `btn_share`, `btn_more`, `rv_playlists`, `empty_state`.

**`activity_edit_profile`** — sửa hồ sơ: `toolbar`, `iv_avatar`, `tv_change_photo`, `et_display_name`, `et_bio`, `btn_save`.

**`activity_listening_stats`** — thống kê nghe: `toolbar`, `btn_share`, `container_sections` (LinearLayout nhồi nhiều `item_stats_section` bằng code).

**`activity_recent`** — nghe gần đây: `toolbar`, `rv_recent`.

**`activity_settings`** — cài đặt: `toolbar` + nhiều dòng bấm `row_account / row_content_display / row_privacy / row_playback / row_notifications / row_devices / row_data / row_quality / row_ads / row_about`, kèm phụ đề `sub_account`, `sub_quality`, **5 công tắc**: `sw_private_session`, `sw_push_notifications`, `sw_data_saver`, `sw_personalized_ads`, và `btn_logout`.

**`activity_add_artist`** — thêm nghệ sĩ: `btn_close`, `et_search`, `rv_artists`, `progress_bar`, `btn_done`.

### B. Layout các tab (`fragment_*`)

**`fragment_home`** — Trang chủ: `avatar_container`+`tv_avatar_letter` (avatar chữ cái), hàng chip lọc `chip_all / chip_music / chip_following / chip_podcast`, `rv_home_feed` (feed chính), `shimmer_home` (loading).

**`fragment_search`** — Tìm kiếm: `search_avatar_container`+`tv_search_avatar`, `et_search` (ô tìm). Hai trạng thái: `default_container` (chưa gõ → `rv_explore` + `rv_browse_all` lưới thể loại) và `results_container` (`tv_results_header`+`rv_results`+`tv_empty`). `shimmer_search_grid` loading.

**`fragment_library`** — Thư viện: `lib_avatar_container`+`tv_lib_avatar`, 4 chip lọc `lib_chip_playlist / lib_chip_artist / lib_chip_album / lib_chip_liked`, `rv_library`, `shimmer_library`.

**`fragment_premium`** — Premium: `btn_premium_start` + 6 lý do dùng Premium `reason_no_ads / reason_download / reason_shuffle / reason_quality / reason_friends / reason_queue` (mỗi cái include `item_premium_reason`).

**`fragment_playlists`** — `rv_playlists`, `tv_empty`, `fab_add` (nút tạo playlist).

**`fragment_liked_tracks`** — `rv_liked`, `tv_empty`.

**`fragment_following_artists`** — `rv_following`, `tv_empty`, `fab_add_artist`.

### C. Bottom sheet & dialog

**`bottom_sheet_create`** (tab Tạo) — 3 dòng `row_playlist / row_collab / row_blend` + `btn_close`.

**`bottom_sheet_add_to_playlist`** — `btn_cancel`, `btn_new_playlist`, `search_container`+`et_search`, `progress_bar`, `rv_playlists`, `btn_done`.

**`bottom_sheet_track_menu`** (menu 1 bài) — header `iv_track_cover`+`tv_track_title`+`tv_track_subtitle`, các mục `item_like / item_add_playlist / item_go_album`.

**`sheet_playlist_edit`** (sửa playlist) — `btn_cancel`, `btn_save`, `cover_preview_container`+`cover_preview`, `et_name`, `row_privacy`+`sw_private` (công khai/riêng tư), `row_delete` (xoá).

**`dialog_create_playlist`** — `btn_close_dialog`, `et_playlist_name`, `btn_create`.

### D. Thành phần ghép vào màn khác

**`layout_mini_player`** (mini player dưới đáy Main) — `pb_mini_progress` (thanh tiến trình mảnh), `iv_mini_cover`, `tv_mini_title`, `tv_mini_artist`, `btn_mini_cast`, `btn_mini_play`, `btn_mini_next`.

**`drawer_content`** (ngăn trái) — `drawer_header`+`drawer_avatar`+`drawer_display_name`, `drawer_account_list` (danh sách tài khoản), `drawer_item_add_account`, và các mục `drawer_item_stats / drawer_item_recent / drawer_item_news / drawer_item_settings`.

**`view_playlist_cover`** (ruột của PlaylistCoverView) — `cover_single` (1 ảnh) và `cover_grid` chứa 4 ô `cover_q0..q3` (lưới 2×2).

### E. Layout 1 dòng danh sách (`item_*`) — gọn theo nhóm

| Item layout | Các view chính | Dùng cho |
|-------------|----------------|----------|
| `item_track_card` | iv_cover, tv_title, tv_artist | thẻ bài (carousel) |
| `item_track_row` | iv_track_cover, tv_track_title, tv_track_artist | dòng bài trong khối Home |
| `item_track_list` | iv_cover, tv_title, tv_artist, btn_more | dòng bài chung |
| `item_track_detail` | tv_number, tv_title, tv_artist, tv_duration, btn_more | dòng bài trong Album/Playlist |
| `item_suggested_track` | iv_cover, tv_title, tv_artist, btn_add | gợi ý bài (playlist rỗng) |
| `item_artist_track` | tv_rank, iv_cover, tv_title, tv_play_count, iv_in_playlist, btn_more | bài phổ biến của nghệ sĩ |
| `item_playlist_card` | iv_playlist_cover, tv_playlist_name, tv_playlist_subtitle | thẻ playlist |
| `item_playlist_list` | card_cover + cover_view (PlaylistCoverView), tv_name, tv_count | dòng playlist |
| `item_playlist_picker(_section)` | iv_cover, tv_name, tv_track_count, iv_check / tv_section_label, tv_clear_all | chọn playlist |
| `item_album_card` | iv_cover, tv_title, tv_artist | thẻ album |
| `item_album_label_card` | iv_album_cover, tv_album_label, tv_album_title, tv_album_artist | thẻ album có nhãn loại |
| `item_artist_album_vertical` | iv_cover, tv_title, tv_subtitle | album của nghệ sĩ |
| `item_artist_card` / `item_artist_list` | iv_avatar, tv_name | nghệ sĩ (thẻ / dòng) |
| `item_artist_circle` | iv_artist_avatar, tv_artist_name | nghệ sĩ tròn |
| `item_selectable_artist` | iv_avatar, fl_check (tích chọn), tv_name | chọn nghệ sĩ follow |
| `item_genre_tile` | tv_genre_name, iv_genre_pic | ô thể loại |
| `item_explore_card` / `item_explore_artist_card` | iv_explore_cover/…artist_cover, tv_explore_tag/label | khám phá |
| `item_radio_card` | fl_radio_bg, iv_radio_artist, tv_radio_artist, tv_radio_subtitle | thẻ radio |
| `item_home_featured` | iv_featured_cover, tv_featured_title | banner nổi bật Home |
| `item_home_section` | tv_section_title, tv_section_more, rv_section_items | khung 1 khối Home (chứa list con) |
| `item_home_chart` | tv_chart_title_top, tv_chart_subtitle, tv_chart_desc | thẻ bảng xếp hạng |
| `item_home_recent_tile` | iv_recent_cover, tv_recent_title | ô "nghe gần đây" 2 cột |
| `item_recent_header` | tv_header | tiêu đề khối recent |
| `item_library_row` | card_lib_cover + lib_cover_view, tv_lib_title, tv_lib_subtitle | dòng playlist/album trong Thư viện |
| `item_library_artist` | iv_lib_avatar, tv_lib_artist_name | dòng nghệ sĩ trong Thư viện |
| `item_library_action` | iv_action_icon, tv_action_label | dòng hành động (vd "Thêm…") |
| `item_lyric_line` | tv_lyric_line | 1 dòng lời |
| `item_premium_reason` | iv_reason_icon, tv_reason_text | 1 lý do dùng Premium |
| `item_stats_section` | tv_label, tv_range, card_artist(iv+tv), card_track(iv+tv), empty_card | 1 mục thống kê (top nghệ sĩ/bài) |
| `item_drawer_account` / `item_drawer_add_account` | iv_account_avatar, tv_account_name | tài khoản trong drawer |

### F. Layout shimmer (khung loading nhấp nháy)

`layout_shimmer_home_section`, `layout_shimmer_track_list`, `layout_shimmer_track_row`, `layout_shimmer_genre_grid` — là **bộ xương xám** hiển thị trong lúc chờ dữ liệu, dùng thư viện **Facebook Shimmer**. Khi data về thì ẩn shimmer, hiện nội dung thật.

---

<a name="14"></a>
## 14. 🎨 CATALOG DRAWABLE — từng nhóm hình/nền để làm gì

> Drawable trong project có **3 loại kỹ thuật**:
> - **Vector icon** (`ic_*`): hình vẽ bằng path, co giãn không vỡ → dùng cho nút, icon.
> - **Shape/Gradient** (`bg_*`): hình nền vẽ bằng `<shape>` (bo góc, màu, viền) hoặc `<gradient>` → dùng làm nền nút, thẻ, pill.
> - **Selector / Layer-list**: `<selector>` đổi hình theo trạng thái (vd chip chọn/không chọn); `<layer-list>` xếp chồng nhiều lớp (vd placeholder = nền xám + nốt nhạc ở giữa).

### A. Icon điều hướng & hệ thống
`ic_home`, `ic_search`, `ic_library` (3 icon bottom nav), `ic_settings`, `ic_arrow_back`, `ic_chevron_down`, `ic_close`, `ic_more_vert` (nút "..."), `ic_check`, `ic_help`.

### B. Icon điều khiển nhạc (Player + mini player)
`ic_play`, `ic_pause`, `ic_skip_next`, `ic_skip_previous`, `ic_replay`, `ic_forward`, `ic_shuffle` (phát ngẫu nhiên), `ic_repeat` (lặp), `ic_timer` (hẹn giờ tắt), `ic_cast` (phát ra thiết bị khác), `ic_queue`, `ic_queue_add`.

### C. Icon hành động trên nội dung
`ic_favorite` / `ic_favorite_filled` (thích — rỗng/đầy), `ic_add`, `ic_plus_circle`, `ic_delete`, `ic_share`, `ic_download`, `ic_edit`, `ic_sort`, `ic_grid`.

### D. Icon tính năng / Premium / khác
`ic_premium`, `ic_no_ads`, `ic_audio_quality`, `ic_friends`, `ic_check_circle_green`, `ic_circle_outline`, `ic_music_note` (nốt nhạc — dùng trong placeholder), `ic_person`, `ic_avatar_placeholder`, `ic_camera`, `ic_podcast`, `ic_event`, `ic_collab`, `ic_blend`, `ic_chart`, `ic_clock`, `ic_megaphone`, `ic_lock_outline` (playlist riêng tư).

### E. Nền nút (button background — shape bo góc)
`bg_button_green` (nút xanh Spotify, bo 24dp — *đã xem: shape chữ nhật, màu #1DB954, bo góc 24dp*), `bg_button_outline` (nút viền), `bg_btn_white_pill` (nút trắng bo tròn), `bg_btn_pill_pink` / `bg_btn_pill_purple` / `bg_btn_pill_green` (nút pill màu), `bg_play_btn` (nút play tròn), `bg_lyrics_button`.

### F. Nền thẻ / bề mặt
`bg_card`, `bg_card_dark`, `bg_card_pink`, `bg_card_top50`, `bg_reasons_card` (thẻ lý do Premium), `bg_dialog_dark` (nền dialog), `bg_lyrics_card`, `bg_circle_dark`, `bg_close_circle`, `bg_check_badge`, `bg_avatar_orange` (nền avatar chữ cái).

### G. Pill / nhãn nhỏ
`bg_pill_pink`, `bg_pill_purple`, `bg_pill_green`.

### H. Gradient gói Premium & tile màu
`bg_premium_individual`, `bg_premium_student`, `bg_premium_family` (3 nền gradient cho 3 gói), `bg_radio_pastel` / `bg_radio_orange` / `bg_radio_teal` / `bg_radio_purple` (nền thẻ radio nhiều màu), `bg_genre_tile` (ô thể loại), `bg_recent_tile`, `bg_artist_header_gradient` (gradient đầu trang nghệ sĩ).

### I. Ô nhập liệu & tìm kiếm
`bg_search_bar`, `bg_search_field`, `bg_edit_field` (nền ô EditText).

### J. Trạng thái (selector)
`bg_chip_selector` (= chọn `bg_chip_active`, không chọn `bg_chip_idle` — *đã xem: selector theo `state_selected`*), `bg_chip_active`, `bg_chip_idle`, `bg_following_outline` (nút Following viền).

### K. Mini player & shimmer & placeholder
`bg_mini_player` (nền mini player), `mini_player_progress` (thanh tiến trình mảnh), `bg_shimmer_box` (ô xám nhấp nháy lúc loading), `placeholder_gradient` (*đã xem: layer-list = nền `spotify_surface` + nốt nhạc 40dp ở giữa* — hiện khi không có ảnh bìa).

### L. Launcher
`ic_launcher_background`, `ic_launcher_foreground` (icon app trên màn hình điện thoại).

> **Mẹo trả lời thầy nếu bị hỏi "sao nhiều drawable thế?"**: "Phần lớn là **icon vector** (nhẹ, không vỡ) và **shape nền** tạo bằng XML thay vì ảnh PNG, nên app nhẹ và đổi màu/bo góc dễ. Nhóm theo tiền tố: `ic_` là icon, `bg_` là nền."

---

<a name="15"></a>
## 15. Quy ước đặt tên id — đọc 1 lần hiểu mọi layout

| Tiền tố | Loại view | Ví dụ |
|---------|-----------|-------|
| `tv_` | TextView (chữ) | `tv_title`, `tv_artist` |
| `iv_` | ImageView (ảnh) | `iv_cover`, `iv_avatar` |
| `btn_` | Button / ImageButton (nút) | `btn_play`, `btn_login` |
| `rv_` | RecyclerView (danh sách cuộn) | `rv_tracks`, `rv_home_feed` |
| `et_` | EditText (ô nhập) | `et_search`, `et_password` |
| `til_` | TextInputLayout (khung ô nhập Material) | `til_username` |
| `sw_` | Switch (công tắc bật/tắt) | `sw_private_session` |
| `chip_` | Chip (nút lọc bo tròn) | `chip_all`, `lib_chip_album` |
| `card_` | CardView / MaterialCardView | `card_about`, `card_cover` |
| `fl_` | FrameLayout (khung chồng lớp) | `fl_radio_bg`, `fl_check` |
| `fab_` | FloatingActionButton (nút tròn nổi) | `fab_add`, `btn_play_fab` |
| `pb_` / `progress_` | ProgressBar | `pb_mini_progress`, `progress_bar` |
| `row_` | 1 dòng bấm được (LinearLayout) | `row_privacy`, `row_account` |
| `shimmer_` | ShimmerFrameLayout (xương loading) | `shimmer_home` |
| `container` / `_container` | khung chứa | `fragment_container` |

→ Nhìn id là đoán được view. Đây là điều giúp bạn **đọc bất kỳ layout nào mà không sợ lạc**.

---

<a name="16"></a>
## 16. Các luồng nghiệp vụ chính (kể từng bước)

### 16.1 Đăng nhập
```
SplashActivity kiểm tra TokenManager.isLoggedIn()
 ├─ Đã login  → MainActivity
 └─ Chưa      → LoginActivity
       người dùng nhập user/pass → bấm Login
       → LoginViewModel.login()
       → AuthRepository.login() → ApiService → server trả JwtResponse (token)
       → TokenManager lưu token + RetrofitClient.reset() (dựng lại có token)
       → mở MainActivity
```

### 16.2 Phát nhạc + mini player
```
Bấm 1 bài ở bất kỳ danh sách nào
 → PlayerManager.play(bài, hàng đợi, vị trí)
     → ExoPlayer setMediaItem(BASE_MEDIA_URL + audioUrl) → prepare() → play()
     → server stream theo HTTP Range (tua được)
 → mở PlayerActivity (quan sát PlayerManager để vẽ bìa, seekbar, nút)
 → mini player ở Main cũng cập nhật cùng lúc (chung listener)
 → hết bài → PlayerManager tự chuyển bài kế (next)
```

### 16.3 Trang chủ (Home feed)
```
HomeFragment → HomeViewModel.load() → HomeRepository.getFeed()
 → server trả List<HomeSection> (mỗi section có kind + title + items)
 → HomeFeedAdapter render: mỗi section là 1 hàng,
   tuỳ kind mà tạo adapter con (playlist/track/artist/album/chart…)
 → bấm item → mở màn chi tiết tương ứng
```

### 16.4 Ảnh bìa playlist tự suy ra (cơ chế hay được hỏi)
```
Một playlist không có ảnh bìa (coverUrl = null)?
 → PlaylistCoverView lấy danh sách bài của playlist
   • ≥4 bài → ghép lưới 2×2 từ 4 ảnh bìa bài hát
   • 1–3 bài → lấy ảnh bài đầu
   • 0 bài  → placeholder nốt nhạc
 → Có cache để cuộn lại không phải tải lại
```
*Lý do:* backend không trả sẵn ảnh mẫu, nên app tự ghép từ các bài trong playlist (giống Spotify).

### 16.5 Sửa / xoá / đổi bìa playlist
```
PlaylistDetailActivity → bấm Sửa → mở PlaylistEditBottomSheet (DÙNG CHUNG ViewModel)
 • Đổi tên + công khai/riêng tư → PUT /api/playlists/{id} → báo UPDATED → Snackbar
 • Xoá → DELETE /api/playlists/{id} → báo DELETED → đóng màn

Bấm vào ảnh bìa → PlaylistCoverPickerActivity
 → chọn ảnh từ máy (Photo Picker, không cần xin quyền)
 → upload (multipart) POST /api/playlists/{id}/cover
 → quay lại → Activity gọi reload() → ảnh bìa mới hiện ra
```

### 16.6 Đa tài khoản
```
Drawer → "Thêm tài khoản" → AddAccountActivity → lưu vào AccountStore
Drawer → chọn tài khoản khác → đổi token → RetrofitClient.reset() → nạp lại Main
```

---

<a name="17"></a>
## 17. ❓ Bộ câu hỏi thầy hay hỏi + cách trả lời

**Q: App dùng kiến trúc gì? Giải thích.**
> MVVM. View (Activity/Fragment) chỉ hiển thị và bắt sự kiện; ViewModel giữ trạng thái + logic màn hình bằng LiveData; Repository lấy dữ liệu (bọc Retrofit); Model là dữ liệu. Tách bạch để dễ test, dễ bảo trì, và ViewModel sống sót khi xoay màn hình.

**Q: LiveData là gì, dùng để làm gì?**
> Là dữ liệu "có thể quan sát". Khi dữ liệu đổi, View đang `observe` sẽ tự được gọi để vẽ lại. Nó tôn trọng vòng đời nên không cập nhật khi màn đã chết → tránh crash, tránh rò bộ nhớ.

**Q: ViewBinding là gì? Khác findViewById chỗ nào?**
> ViewBinding sinh ra class binding từ XML, truy cập view bằng `binding.tvTitle` — an toàn kiểu dữ liệu, không lo gõ sai id hay null như `findViewById`.

**Q: App gọi API thế nào? Có bảo mật không?**
> Dùng Retrofit. Mỗi request được `AuthInterceptor` tự gắn `Authorization: Bearer <JWT>`. Token lưu trong SharedPreferences. Hết hạn (401/403) thì tự đá về Login.

**Q: Phát nhạc bằng gì? Tua được không?**
> ExoPlayer (media3). Server hỗ trợ HTTP Range nên tua (seek) được mà không cần tải hết file. `PlayerManager` là singleton quản lý hàng đợi để mini player và màn Player luôn đồng bộ.

**Q: Tại sao có nhiều adapter thế?**
> Mỗi kiểu hiển thị danh sách (thẻ bài, thẻ album, nghệ sĩ tròn, dòng bài…) cần 1 adapter để đổ dữ liệu vào đúng layout. Riêng `HomeFeedAdapter` và `LibraryAdapter` là đa kiểu — 1 list nhiều dạng dòng.

**Q: Ảnh bìa playlist khi chưa đặt thì hiện gì?**
> `PlaylistCoverView` tự ghép lưới 2×2 từ ảnh bìa 4 bài đầu; ít bài thì lấy bài đầu; rỗng thì hiện placeholder nốt nhạc.

**Q: Drawable nhiều vậy quản lý sao?**
> Nhóm theo tiền tố: `ic_` là icon vector, `bg_` là nền (shape/gradient). Phần lớn vẽ bằng XML nên nhẹ và dễ đổi màu/bo góc.

**Q: Dữ liệu lấy từ đâu?**
> Backend Spring Boot + MySQL riêng, ~46 REST endpoint, xác thực JWT. App chỉ là client.

**Q: Còn gì chưa làm (điểm trung thực)?**
> Ghi lượt nghe sau 30s chưa tự động hoàn toàn; lyrics đồng bộ theo timestamp còn cơ bản; chưa khoá tính năng theo Premium (HQ audio, tải offline); chưa có unit test FE.

---

> **Khi trình bày:** mở app demo theo thứ tự — Splash → Login → Home (giải thích feed nhiều khối) → vào 1 bài (Player + mini player) → Thư viện (chip lọc, ảnh bìa tự ghép) → tạo/sửa playlist → Premium → Cài đặt. Vừa thao tác vừa chỉ ra "đây là Fragment X, dữ liệu từ ViewModel Y qua Repository Z gọi API W".
</content>
</invoke>
