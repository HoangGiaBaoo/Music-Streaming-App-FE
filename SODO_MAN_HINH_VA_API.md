# SƠ ĐỒ MÀN HÌNH & DANH SÁCH API — Music Streaming App

> File phụ trợ cho `THUYET_TRINH_PROJECT.md`. Gồm 2 phần:
> 1. **Navigation map** — các màn hình nối với nhau ra sao (màn nào mở được màn nào).
> 2. **Danh sách đầy đủ API** — toàn bộ ~53 endpoint khai báo trong `ApiService.java`, kèm method, đường dẫn, công dụng và repository gọi nó.
>
> Dữ liệu lấy trực tiếp từ source: các lệnh `startActivity(Intent)` và file `network/ApiService.java`.

---

## PHẦN 1 — SƠ ĐỒ ĐIỀU HƯỚNG MÀN HÌNH (NAVIGATION MAP)

### 1.1 Sơ đồ tổng (luồng vào app + trung tâm)

```
                        ┌──────────────┐
                        │ SplashActivity│  (mở app, kiểm tra đăng nhập)
                        └──────┬───────┘
                  đã login     │     chưa login
              ┌────────────────┘     └───────────────┐
              ▼                                       ▼
      ┌───────────────┐                       ┌───────────────┐
      │  MainActivity │ ◄──────────────────── │ LoginActivity │
      │  (5 tab+drawer)│   login thành công    └───────┬───────┘
      └───────┬───────┘                                │  ▲
              │                                        ▼  │ quay lại
              │                                ┌───────────────┐
              │                                │RegisterActivity│
              │                                └───────────────┘
              │
   ┌──────────┼────────────────────────────────────────────────┐
   │ MainActivity mở được (qua bottom nav + drawer + bấm item): │
   │                                                            │
   │  • PlayerActivity          (bấm 1 bài bất kỳ)              │
   │  • ArtistDetailActivity    (bấm nghệ sĩ)                   │
   │  • AlbumDetailActivity     (bấm album)                     │
   │  • PlaylistDetailActivity  (bấm playlist)                  │
   │  • PremiumPlansActivity    (tab/Fragment Premium)          │
   │  • ProfileActivity         (drawer → hồ sơ)                │
   │  • ListeningStatsActivity  (drawer → thống kê)             │
   │  • RecentActivity          (drawer → nghe gần đây)         │
   │  • SettingsActivity        (drawer → cài đặt)              │
   │  • AddAccountActivity      (drawer → thêm tài khoản)       │
   └────────────────────────────────────────────────────────────┘
```

### 1.2 Chuỗi màn chi tiết & phát nhạc

```
ArtistDetailActivity ──┬─► AlbumDetailActivity ───► PlayerActivity ───► LyricsActivity
                       ├─► PlaylistDetailActivity ─► PlayerActivity
                       └─► PlayerActivity

PlaylistDetailActivity ─┬─► PlayerActivity
                        └─► PlaylistCoverPickerActivity   (chọn ảnh bìa → upload → quay lại)

ProfileActivity ───────┬─► EditProfileActivity
                       └─► PlaylistDetailActivity

RecentActivity ─────────► PlayerActivity
```

### 1.3 Các tab (Fragment) mở màn nào

| Fragment (tab) | Mở được màn |
|----------------|-------------|
| `HomeFragment` (Trang chủ) | PlayerActivity, ArtistDetailActivity, AlbumDetailActivity, PlaylistDetailActivity |
| `SearchFragment` (Tìm kiếm) | PlayerActivity |
| `LibraryFragment` (Thư viện) | PlayerActivity, ArtistDetailActivity, PlaylistDetailActivity, AddArtistActivity |
| `PlaylistsFragment` | PlaylistDetailActivity |
| `LikedTracksFragment` | PlayerActivity |
| `FollowingArtistsFragment` | ArtistDetailActivity, AddArtistActivity |
| `PremiumFragment` (Premium) | PremiumPlansActivity |

### 1.4 Đường "thoát ra Login" (đăng xuất / hết hạn)

```
SettingsActivity ── bấm Đăng xuất ──► LoginActivity   (xoá token)
SessionManager ── token hết hạn (401/403) ──► LoginActivity   (tự động đá ra)
AddAccountActivity ──► LoginActivity / RegisterActivity
```

### 1.5 Bảng đầy đủ: màn nguồn → màn đích → khi nào

| Từ màn | Tới màn | Khi nào |
|--------|---------|---------|
| Splash | Main / Login | mở app, tuỳ đã đăng nhập |
| Login | Register | bấm "Đăng ký" |
| Login | Main | đăng nhập thành công |
| Register | Login | quay lại / đăng ký xong |
| AddAccount | Login, Register | thêm tài khoản |
| Main | Player | bấm 1 bài (mini player / list) |
| Main | Profile, Stats, Recent, Settings, AddAccount | mục trong drawer |
| Main | Main (khởi động lại) | đổi tài khoản |
| Home (FR) | Player / Artist / Album / Playlist Detail | bấm item trong feed |
| Search (FR) | Player | bấm kết quả bài hát |
| Library (FR) | Player / Artist / Playlist Detail / AddArtist | bấm item / nút thêm |
| Liked (FR) | Player | bấm bài đã thích |
| Following (FR) | ArtistDetail / AddArtist | bấm nghệ sĩ / FAB |
| Premium (FR) | PremiumPlans | bấm "Bắt đầu" |
| ArtistDetail | AlbumDetail / PlaylistDetail / Player | bấm album / playlist / bài |
| AlbumDetail | Player | bấm/phát bài |
| PlaylistDetail | Player | phát bài / FAB play |
| PlaylistDetail | PlaylistCoverPicker | bấm ảnh bìa để đổi |
| Player | Lyrics | bấm "Xem lời bài hát" |
| Profile | EditProfile / PlaylistDetail | bấm Sửa / bấm playlist |
| Recent | Player | bấm bài |
| Settings | Login | đăng xuất |

**Câu trả lời mẫu nếu thầy hỏi "điều hướng giữa các màn làm thế nào?"**
> Dùng `Intent` của Android: `startActivity(new Intent(this, XxxActivity.class))`, kèm dữ liệu truyền qua `putExtra` (ví dụ `EXTRA_PLAYLIST_ID`). 5 tab chính không phải Activity mà là **Fragment** gắn chung vào `MainActivity` qua `fragment_container`, đổi tab thì thay Fragment chứ không mở Activity mới.

---

## PHẦN 2 — DANH SÁCH ĐẦY ĐỦ API (`ApiService.java`)

**Quy ước chung:**
- **Base URL:** `http://10.0.2.2:8080/musicapp/` → đường dẫn đầy đủ = base + path trong bảng.
- **Xác thực:** mọi endpoint cần header `Authorization: Bearer <JWT>` — **trừ** `api/auth/register` và `api/auth/login` (công khai). File ảnh `/images/**` và nhạc `/audio/**` cũng công khai (Glide/ExoPlayer tải trực tiếp).
- **`{id}`** = tham số trên đường dẫn (Path); **`?key=`** = tham số truy vấn (Query); **Body** = JSON gửi lên.
- Tổng cộng: **53 endpoint**.

### 2.1 Auth — xác thực (`AuthRepository`)

| Method | Path | Body / Param | Công dụng |
|--------|------|--------------|-----------|
| POST | `api/auth/register` | `RegisterRequest` | Đăng ký tài khoản |
| POST | `api/auth/login` | `LoginRequest` → trả `JwtResponse` | Đăng nhập, lấy token |
| POST | `api/auth/logout` | — | Đăng xuất |

### 2.2 Users & Profile (`UserRepository`)

| Method | Path | Body / Param | Công dụng |
|--------|------|--------------|-----------|
| GET | `api/users/me` | → `UserMe` | Lấy thông tin tài khoản đang đăng nhập |
| GET | `api/users/me/profile` | → `UserProfile` | Hồ sơ của mình |
| GET | `api/users/{id}/profile` | id | Hồ sơ người khác |
| PUT | `api/users/me/profile` | `ProfileUpdateRequest` | Cập nhật tên/tiểu sử |
| POST | `api/users/me/avatar` | file (multipart) | Upload ảnh đại diện |

### 2.3 Settings — cài đặt (`UserRepository`)

| Method | Path | Body | Công dụng |
|--------|------|------|-----------|
| GET | `api/users/me/settings` | → `UserSettings` | Lấy 5 cài đặt người dùng |
| PUT | `api/users/me/settings` | `UserSettings` | Lưu cài đặt |

### 2.4 Stats — thống kê nghe (`UserRepository`)

| Method | Path | Param | Công dụng |
|--------|------|-------|-----------|
| GET | `api/stats/listening` | `?period=` `&offset=` | Top nghệ sĩ/bài theo tuần/tháng/năm |

### 2.5 Home — trang chủ (`HomeRepository`)

| Method | Path | Param | Công dụng |
|--------|------|-------|-----------|
| GET | `api/home/feed` | `?filter=` | Trả `List<HomeSection>` — feed nhiều khối |

### 2.6 Tracks — bài hát (`LibraryRepository`, `PlayerRepository`)

| Method | Path | Param | Công dụng |
|--------|------|-------|-----------|
| GET | `api/tracks` | — | Danh sách tất cả bài |
| GET | `api/tracks/{id}` | id | Chi tiết 1 bài |
| GET | `api/tracks/liked` | — | Bài đã thích |
| GET | `api/tracks/{id}/related` | id | Bài liên quan (gợi ý sau khi nghe) |
| POST | `api/tracks/{id}/like` | id | Thích / bỏ thích (toggle) |

### 2.7 Artists — nghệ sĩ (`LibraryRepository`)

| Method | Path | Công dụng |
|--------|------|-----------|
| GET | `api/artists` | Tất cả nghệ sĩ (màn thêm nghệ sĩ) |
| GET | `api/artists/{id}` | Chi tiết nghệ sĩ |
| GET | `api/artists/{id}/albums` | Album của nghệ sĩ |
| GET | `api/artists/{id}/tracks/popular` | Bài phổ biến của nghệ sĩ |
| GET | `api/artists/{id}/related` | Nghệ sĩ liên quan |
| GET | `api/artists/followed` | Nghệ sĩ đang follow |
| GET | `api/artists/popular` | Nghệ sĩ nổi bật (Home) |
| POST | `api/artists/{id}/follow` | Follow / unfollow (toggle) |

### 2.8 Albums (`LibraryRepository`)

| Method | Path | Công dụng |
|--------|------|-----------|
| GET | `api/albums` | Tất cả album |
| GET | `api/albums/new` | Album/đĩa mới phát hành |
| GET | `api/albums/{id}` | Chi tiết album |
| GET | `api/albums/{id}/tracks` | Danh sách bài trong album |

### 2.9 Genres — thể loại (`LibraryRepository`)

| Method | Path | Công dụng |
|--------|------|-----------|
| GET | `api/genres` | Lưới thể loại (tab Tìm kiếm) |
| GET | `api/genres/{id}/tracks` | Bài thuộc 1 thể loại |

### 2.10 Playlists (`LibraryRepository`)

| Method | Path | Body / Param | Công dụng |
|--------|------|--------------|-----------|
| GET | `api/playlists` | — | Playlist của tôi |
| GET | `api/playlists/curated` | `?mood=` | Playlist biên tập theo tâm trạng |
| POST | `api/playlists` | `PlaylistRequest` | Tạo playlist mới |
| GET | `api/playlists/{id}` | id | Chi tiết playlist |
| GET | `api/playlists/{id}/tracks` | id | Bài trong playlist (cũng dùng để **ghép ảnh bìa**) |
| POST | `api/playlists/{id}/tracks` | `?trackId=` | Thêm bài vào playlist |
| DELETE | `api/playlists/{id}/tracks/{trackId}` | id, trackId | Xoá bài khỏi playlist |
| PUT | `api/playlists/{id}` | `PlaylistRequest` | Sửa tên / công khai-riêng tư |
| DELETE | `api/playlists/{id}` | id | Xoá playlist |
| POST | `api/playlists/{id}/cover` | file (multipart) | Upload ảnh bìa playlist |

### 2.11 Charts — bảng xếp hạng (`LibraryRepository`)

| Method | Path | Param | Công dụng |
|--------|------|-------|-----------|
| GET | `api/charts/tracks` | `?limit=` | Top bài hát |
| GET | `api/charts/artists` | `?limit=` | Top nghệ sĩ |

### 2.12 Recommendations — gợi ý (`LibraryRepository`)

| Method | Path | Công dụng |
|--------|------|-----------|
| GET | `api/recommendations/daily` | Gợi ý hàng ngày |
| GET | `api/recommendations/mix` | Các mix gợi ý |

### 2.13 History — lịch sử nghe (`LibraryRepository`, `PlayerRepository`)

| Method | Path | Param | Công dụng |
|--------|------|-------|-----------|
| GET | `api/history` | — | Lịch sử nghe |
| GET | `api/history/recent` | `?limit=` | Bài nghe gần đây (Home + RecentActivity) |
| POST | `api/history` | `?trackId=` | Ghi lượt nghe (tăng play count) |

### 2.14 Subscription — Premium (`SubscriptionRepository`)

| Method | Path | Body | Công dụng |
|--------|------|------|-----------|
| GET | `api/subscriptions/me` | → `Subscription` | Gói đang dùng |
| GET | `api/subscriptions/plans` | → `List<PlanInfo>` | Danh sách gói |
| POST | `api/subscriptions/subscribe` | `SubscribeRequest` | Đăng ký gói |
| POST | `api/subscriptions/cancel` | — | Huỷ gói |

### 2.15 Search — tìm kiếm (`SearchRepository`)

| Method | Path | Param | Công dụng |
|--------|------|-------|-----------|
| GET | `api/search` | `?q=` | Trả `SearchResult {tracks, artists}` |

---

### Ghi nhớ nhanh khi bảo vệ

- **App là client thuần** — không có logic nghiệp vụ nặng, chỉ gọi 53 endpoint trên rồi hiển thị.
- Nhiều endpoint trả `Map<String,String>` (như like/follow) → app chỉ quan tâm thành công hay không (boolean).
- Endpoint **upload** (`avatar`, `playlist cover`) dùng `multipart/form-data` (gửi file), khác với các endpoint JSON thường.
- Toàn bộ đi qua **một** `ApiService` duy nhất, được các **Repository** chia nhau gọi theo chức năng (xem cột ngoặc ở mỗi mục).
```
</content>
