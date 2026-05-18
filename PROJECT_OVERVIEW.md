# PROJECT OVERVIEW — Online Music Streaming App

Tài liệu này tóm tắt toàn bộ dự án **online_music_streaming_app**: kiến trúc, luồng nghiệp vụ và vai trò của từng class / enum / interface trong source code. Đọc xong file này bạn sẽ hiểu được cách hệ thống vận hành mà không cần đọc lại từng file.

---

## 1. Tổng quan dự án

- **Mục tiêu:** Backend cho ứng dụng nghe nhạc trực tuyến kiểu **Spotify clone** (giao diện tiếng Việt). Client target là Android (Retrofit + ExoPlayer).
- **Nghiệp vụ chính:**
    - Đăng ký / đăng nhập (JWT)
    - Quản lý nghệ sĩ (Artist), album, bài hát (Track), thể loại (Genre)
    - Tạo / quản lý playlist của user và playlist do admin curate (theo mood)
    - Like bài hát, follow nghệ sĩ
    - Lưu lịch sử nghe (Play History) và đếm số lượt nghe (play count)
    - Tìm kiếm bài hát / nghệ sĩ
    - Trang chủ (Home Feed) với nhiều section: featured, recently played, recommended, chart, mood playlists, new releases, popular artists
    - Bảng xếp hạng (chart) top tracks / artists
    - Gói Premium: FREE / INDIVIDUAL / STUDENT / FAMILY
    - Đề xuất bài hát / nghệ sĩ liên quan, daily mix
    - Stream audio qua HTTP với hỗ trợ `Range` header (cho phép tua / phát từ giữa file)
    - Upload file nhạc và ảnh bìa (admin)

- **Tech stack:**
    - **Spring Boot 4.0.6** trên **Java 21**
    - **MySQL 8** (`localhost:3306/music_app`)
    - **Spring Data JPA + Hibernate** (dialect MySQL)
    - **Spring Security + JWT** (`jjwt 0.12.6`)
    - **Lombok** giảm boilerplate
    - File audio / ảnh lưu trên đĩa tại `D:/music-files`

- **Cấu hình quan trọng:**
    - `server.port = 8080`
    - `context-path = /musicapp` → mọi URL bắt đầu bằng `http://localhost:8080/musicapp/...`
    - JWT expiration = 86400000 ms (24h)
    - DDL auto = `update` (Hibernate tự sinh / sửa schema)

---

## 2. Kiến trúc tổng thể

Dự án đi theo mô hình **Layered Architecture** kinh điển của Spring Boot:

```
        ┌──────────────────────────────────────────┐
        │  Client (Android app, Postman, browser)  │
        └────────────────────┬─────────────────────┘
                             │ HTTP + JWT
                             ▼
        ┌──────────────────────────────────────────┐
        │  Security Filter (JwtFilter)             │  ← chặn request, gắn user vào SecurityContext
        └────────────────────┬─────────────────────┘
                             ▼
        ┌──────────────────────────────────────────┐
        │  Controller layer  (@RestController)     │  ← nhận request, trả ResponseEntity
        └────────────────────┬─────────────────────┘
                             ▼
        ┌──────────────────────────────────────────┐
        │  Service layer    (@Service)             │  ← business logic, transaction
        └────────────────────┬─────────────────────┘
                             ▼
        ┌──────────────────────────────────────────┐
        │  Repository layer (Spring Data JPA)      │  ← truy vấn DB
        └────────────────────┬─────────────────────┘
                             ▼
        ┌──────────────────────────────────────────┐
        │  Entity (JPA)  ↔  MySQL                  │
        └──────────────────────────────────────────┘
```

**Các package trong `com.huce.online_music_streaming_app`:**

| Package      | Vai trò                                                        |
|--------------|----------------------------------------------------------------|
| `entity`     | Các JPA entity ánh xạ với bảng MySQL + enum + composite key    |
| `repository` | Spring Data JPA interfaces — truy vấn DB                       |
| `dto`        | Data Transfer Objects cho request / response                   |
| `service`    | Business logic, gọi repository, xử lý transaction              |
| `controller` | REST endpoint, trả `ResponseEntity<?>`                         |
| `security`   | JWT util, filter, UserDetailsService                           |
| `config`     | Spring Security config, file storage config                    |

---

## 3. Luồng nghiệp vụ chính

### 3.1 Đăng ký & đăng nhập

```
POST /api/auth/register {username, email, password}
   → AuthController.register
   → AuthService.register
       • Kiểm tra username/email chưa tồn tại
       • Hash password bằng BCryptPasswordEncoder
       • Lưu User với role = "user"
   → 200 {message: "Registration successful"}

POST /api/auth/login {username, password}
   → AuthController.login
   → AuthService.login
       • AuthenticationManager xác thực username/password
       • UserDetailsServiceImpl load user từ DB
       • JwtUtil tạo token (chứa subject = username)
   → 200 JwtResponse {token, username, role}
```

Token được client lưu lại, gắn vào header `Authorization: Bearer <token>` cho các request tiếp theo.

### 3.2 Mỗi request tiếp theo

```
Client gửi request có header Authorization
   → JwtFilter (extends OncePerRequestFilter)
       • Đọc token, extract username
       • Load UserDetails từ DB
       • Validate token chưa hết hạn
       • Set SecurityContextHolder
   → SecurityFilterChain cho phép qua nếu endpoint authenticated
   → Controller dùng @AuthenticationPrincipal UserDetails để biết user nào đang gọi
```

Endpoint public không cần token: `/api/auth/**`, `/audio/**`, `/images/**`.
Endpoint admin: `/api/admin/**` cần role `ADMIN`.

### 3.3 Phát nhạc

```
1. Android player gọi GET /audio/{filename} với header Range: bytes=START-END
2. FileController.streamAudio:
       • Đọc file từ D:/music-files/audio/{filename}
       • Nếu có Range → trả 206 Partial Content + chunk
       • Không Range → trả 200 + toàn bộ
3. Sau khi phát: client gọi POST /api/history?trackId=X
       • PlayHistoryService.record lưu PlayHistory
       • TrackService.incrementPlayCount tăng play_count
```

### 3.4 Home feed

```
GET /api/home/feed?filter=all|music|following
   → HomeController → HomeFeedService.buildFeed(userId, filter)
       Sinh các HomeSectionDto theo thứ tự:
         FEATURED        (curated playlist đầu tiên)
         TOP_PICKS       (các curated playlist khác)
         RECENTLY_PLAYED (10 track gần nhất user đã nghe)
         RECOMMENDED     (RecommendationService.dailyMix)
         CHART           (top 10 theo play_count)
         MOOD_PLAYLIST   (NOSTALGIC, WORKOUT, PARTY, HAPPY, RELAX, KARAOKE)
         NEW_RELEASES    (10 album mới nhất)
         POPULAR_ARTISTS (10 nghệ sĩ nhiều follower nhất)
       Filter "following" chỉ trả các artist user đang follow.
```

### 3.5 Thư viện cá nhân

- **Liked tracks:** `POST /api/tracks/{id}/like` toggle, `GET /api/tracks/liked` xem danh sách.
- **Followed artists:** `POST /api/artists/{id}/follow` toggle, `GET /api/artists/followed` xem danh sách.
- **Playlist của user:** `GET/POST /api/playlists`, thêm/bớt track qua `POST /api/playlists/{id}/tracks`.

### 3.6 Subscription (Premium)

```
GET /api/subscriptions/me        → trả Subscription hiện tại (mặc định FREE)
GET /api/subscriptions/plans     → trả 3 PlanInfoDto (INDIVIDUAL/STUDENT/FAMILY) với giá VND
POST /api/subscriptions/subscribe {plan}
       • SubscriptionService.subscribe vô hiệu hóa subscription cũ, tạo bản ghi mới hiệu lực 1 tháng
POST /api/subscriptions/cancel   → set active=false, end_date=today
```

---

## 4. Mô hình dữ liệu (ERD rút gọn)

```
User ──< Playlist ──< PlaylistTrack >── Track >── Album >── Artist
  │                       │                │
  │                       └────────────────┘
  ├──< LikedTrack >───────────────────────── Track
  ├──< FollowedArtist >──────────────────── Artist
  ├──< PlayHistory >─────────────────────── Track
  └──< Subscription
                                          Track ──< Track_Genres >── Genre
```

**Bảng / entity (12):** Users, Artists, Genres, Albums, Tracks, Playlists, Subscriptions, Play_History, Playlist_Tracks, Liked_Tracks, Followed_Artists, Track_Genres (M:N giữa Track-Genre, JPA tự sinh).

---

## 5. Mô tả từng class / enum / interface

### 5.1 Application bootstrap

| Class | Vai trò |
|-------|---------|
| `OnlineMusicStreamingAppApplication` | Entry point. Có `@SpringBootApplication` quét toàn bộ package `com.huce.online_music_streaming_app`, khởi động Spring Boot. |

---

### 5.2 Package `entity` — 10 Entity + 3 Embedded ID + 3 Enum

#### Entity chính

| Class | Bảng | Vai trò & quan hệ |
|-------|------|-------------------|
| `User` | `Users` | Tài khoản người dùng. Trường: `userId`, `username` (unique), `email` (unique), `passwordHash` (`@JsonIgnore`), `role` (default `"user"`), `avatarUrl`, `displayName`, `bio`, `createdAt`. |
| `UserSettings` | `User_Settings` | Cài đặt cá nhân (1-1 với User). 5 field cho màn Cài đặt: `dataSaver`, `pushNotifications`, `streamQualityWifi` (enum), `privateSession`, `personalizedAds`. Auto-create default khi register. |
| `Artist` | `Artists` | Nghệ sĩ. Trường: `artistId`, `name`, `bio` (TEXT), `avatarUrl`, `createdAt`. |
| `Genre` | `Genres` | Thể loại nhạc. Trường: `genreId`, `name` (unique), `coverColor` (mã hex để UI render tile), `coverUrl`. |
| `Album` | `Albums` | Album thuộc 1 Artist. `@ManyToOne` về `Artist`. Trường thêm: `title`, `coverUrl`, `releaseDate`, `albumType` (enum `AlbumType`, lưu STRING), `description`. |
| `Track` | `Tracks` | Bài hát. `@ManyToOne` về `Artist` (bắt buộc) và `Album` (optional). `@ManyToMany` với `Genre` qua bảng `Track_Genres`. Trường: `title`, `duration`, `audioUrl`, `coverUrl`, `playCount` (default 0), `lyrics` (TEXT), `createdAt`. Loại trừ `genres` khỏi `toString` và `equals` để tránh lazy-load loop. |
| `Playlist` | `Playlists` | Danh sách phát. `@ManyToOne` về `User` (chủ playlist). `isPublic`, `isCurated` (true = playlist do admin biên tập), `description`, `coverUrl`, `coverColor`, `mood` (enum `Mood`, lưu STRING). |
| `Subscription` | `Subscriptions` | Gói thuê bao. `@ManyToOne` về `User`. `plan` (enum `SubscriptionPlan`), `startDate`, `endDate`, `active`. |
| `PlayHistory` | `Play_History` | Mỗi lần phát 1 bài. `@ManyToOne` về `User` và `Track`. `playedAt` mặc định `now()`. |

#### Junction entity (bảng trung gian có composite primary key)

| Class | Bảng | Vai trò |
|-------|------|---------|
| `PlaylistTrack` | `Playlist_Tracks` | Một bài trong 1 playlist + thứ tự `position`. Dùng `@EmbeddedId PlaylistTrackId`, `@MapsId` cho quan hệ đến Playlist & Track. |
| `LikedTrack` | `Liked_Tracks` | User đã like Track nào, kèm `likedAt`. Composite key `(userId, trackId)`. |
| `FollowedArtist` | `Followed_Artists` | User đang follow Artist nào, kèm `followedAt`. Composite key `(userId, artistId)`. |

#### Embedded ID (composite primary key)

| Class | Vai trò |
|-------|---------|
| `PlaylistTrackId` | `Embeddable` chứa `(playlistId, trackId)`. `Serializable` theo yêu cầu của JPA. |
| `LikedTrackId` | `(userId, trackId)`. |
| `FollowedArtistId` | `(userId, artistId)`. |

#### Enum

| Enum | Giá trị | Dùng ở đâu |
|------|---------|-----------|
| `AlbumType` | `ALBUM`, `SINGLE`, `EP`, `COMPILATION` | Album phân loại (UI hiện label "Album / Đĩa đơn / EP / Tuyển tập"). |
| `Mood` | `WORKOUT`, `RELAX`, `PARTY`, `HAPPY`, `NOSTALGIC`, `KARAOKE`, `FOCUS`, `SLEEP` | Gắn cho curated Playlist để Home feed nhóm theo mood. |
| `SubscriptionPlan` | `FREE`, `INDIVIDUAL`, `STUDENT`, `FAMILY` | Gói Premium. |
| `StreamQuality` | `LOW`, `NORMAL`, `HIGH`, `VERY_HIGH`, `AUTO` | Chất lượng stream/download trong UserSettings. |

> Tất cả enum đều được `@Enumerated(EnumType.STRING)` để DB lưu chuỗi rõ nghĩa, dễ debug.

---

### 5.3 Package `repository` — 11 Spring Data JPA interface

Mỗi interface kế thừa `JpaRepository<Entity, ID>` để có sẵn `findAll`, `findById`, `save`, `delete`,…

| Repository | Phương thức tuỳ biến đáng chú ý |
|------------|----------------------------------|
| `UserRepository` | `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail` (kiểm tra trùng khi đăng ký). |
| `ArtistRepository` | `findByNameContainingIgnoreCase`; `findPopularWithFollowers` (JPQL `LEFT JOIN FollowedArtist`, `GROUP BY` rồi sort theo follower count); `findRelatedByGenre` (cùng genre, khác artist). |
| `GenreRepository` | Chỉ kế thừa CRUD chuẩn. |
| `AlbumRepository` | `findByArtist_ArtistId`, `findByTitleContainingIgnoreCase`, `findAllByOrderByReleaseDateDesc` (new releases). |
| `TrackRepository` | Tìm theo album/artist/title; `search` JPQL match `title` hoặc `artist.name`; `findAllByOrderByPlayCountDesc` (chart); `findByArtist_ArtistIdOrderByPlayCountDesc` (top tracks của 1 nghệ sĩ); `findRelatedByGenre` (track liên quan); `findByGenreId`. |
| `PlaylistRepository` | `findByUser_UserId` (playlist của user), `findByIsCuratedTrue`, `findByIsCuratedTrueAndMood`. |
| `PlaylistTrackRepository` | `findByIdPlaylistIdOrderByPosition`, `deleteByIdPlaylistIdAndIdTrackId`, `countByIdPlaylistId` (để gán `position` mới). |
| `LikedTrackRepository` | `findByIdUserId`, `existsByIdUserIdAndIdTrackId`, `deleteByIdUserIdAndIdTrackId` (toggle like). |
| `FollowedArtistRepository` | Tương tự `LikedTrackRepository` nhưng cho artist. |
| `PlayHistoryRepository` | `findByUser_UserIdOrderByPlayedAtDesc` (hai overload: có / không Pageable). |
| `SubscriptionRepository` | `findFirstByUser_UserIdAndActiveTrueOrderByStartDateDesc` (lấy subscription đang active mới nhất). |

---

### 5.4 Package `dto` — 8 DTO

| DTO | Hướng | Trường |
|-----|-------|--------|
| `RegisterRequest` | request | `username`, `email`, `password` |
| `LoginRequest` | request | `username`, `password` |
| `JwtResponse` | response | `token`, `username`, `role` |
| `PlaylistRequest` | request | `name`, `isPublic` (default false) |
| `SubscribeRequest` | request | `plan` (`SubscriptionPlan`) |
| `PlanInfoDto` | response | `plan`, `name`, `priceVnd`, `features` (List<String>) — mô tả gói Premium. |
| `HomeSectionDto` | response | `kind` (FEATURED, TOP_PICKS, …), `title`, `subtitle`, `items` (List<?>) — 1 section trong home feed. |

> Các entity được trả thẳng trong nhiều endpoint (ví dụ list track) — không có DTO mapping riêng cho entity vì project học tập, ưu tiên đơn giản.

---

### 5.5 Package `service` — 12 Service

| Service | Trách nhiệm chính |
|---------|-------------------|
| `AuthService` | `register` (kiểm trùng, hash password) và `login` (gọi `AuthenticationManager`, sinh JWT). |
| `TrackService` | CRUD đơn giản; `search`; `incrementPlayCount` (gọi khi user phát bài); `deleteById` (xoá track, JPA tự dọn Track_Genres). |
| `ArtistService` | CRUD đơn giản + `search`; `deleteById` (throw 404 nếu không tồn tại, để DB bắn FK violation nếu còn album/track liên quan). |
| `AlbumService` | CRUD đơn giản + `findByArtist`; `deleteById` (tương tự ArtistService). |
| `GenreService` | `findAll`, `findById`. |
| `PlaylistService` | Tạo playlist, thêm / xoá track (tự sinh `position` = `count + 1`), lấy danh sách track theo `position`. Có `@Transactional` trên `addTrack`/`removeTrack`. |
| `LibraryService` | `toggleLike` (Liked_Tracks) và `toggleFollow` (Followed_Artists). Thao tác idempotent: nếu đã có thì xoá, chưa có thì tạo. |
| `PlayHistoryService` | `record` lưu lượt nghe, `getHistory` trả lịch sử của user. |
| `FileStorageService` | Lưu file upload xuống `D:/music-files/audio` hoặc `/images`, sinh tên `UUID_originalName`, trả URL tương đối `/audio/...` hoặc `/images/...`. |
| `SubscriptionService` | `getCurrent` (default FREE nếu chưa có), `subscribe` (vô hiệu hóa cái cũ rồi tạo cái mới hiệu lực 1 tháng), `cancel`, `getPlans` (hard-code 3 gói INDIVIDUAL / STUDENT / FAMILY với giá VND và tính năng). |
| `RecommendationService` | `relatedTracks` (cùng genre, fallback top tracks của cùng artist), `relatedArtists` (cùng genre), `dailyMix` (hiện tại = top play count, chưa cá nhân hoá). |
| `HomeFeedService` | Sinh `List<HomeSectionDto>` cho `/api/home/feed`. Lấy curated playlist, recently played, recommended, chart, mood playlists, new releases, popular artists. Hỗ trợ filter `music` và `following`. |

---

### 5.6 Package `controller` — 14 REST Controller

Mọi controller đều trả `ResponseEntity<?>` với body JSON. Lỗi trả `Map.of("error", message)`.

| Controller | Base path | Endpoint chính |
|------------|-----------|----------------|
| `AuthController` | `/api/auth` | `POST /register`, `POST /login`, `POST /logout` |
| `TrackController` | `/api/tracks` | `GET /`, `GET /{id}`, `GET /{id}/related`, `GET /liked`, `POST /{id}/like` |
| `ArtistController` | `/api/artists` | `GET /`, `GET /popular`, `GET /followed`, `GET /{id}`, `GET /{id}/albums`, `GET /{id}/tracks/popular`, `GET /{id}/related`, `POST /{id}/follow` |
| `AlbumController` | `/api/albums` | `GET /`, `GET /new`, `GET /{id}`, `GET /{id}/tracks` |
| `GenreController` | `/api/genres` | `GET /`, `GET /{id}/tracks` |
| `PlaylistController` | `/api/playlists` | `GET /` (của user), `GET /curated?mood=`, `POST /`, `GET /{id}`, `GET /{id}/tracks`, `POST /{id}/tracks?trackId=`, `DELETE /{id}/tracks/{trackId}` |
| `PlayHistoryController` | `/api/history` | `GET /`, `GET /recent?limit=`, `POST /?trackId=` (record + tăng playCount) |
| `SearchController` | `/api/search` | `GET /?q=` trả `{tracks, artists}` |
| `HomeController` | `/api/home` | `GET /feed?filter=` |
| `ChartController` | `/api/charts` | `GET /tracks?limit=`, `GET /artists?limit=` |
| `RecommendationController` | `/api/recommendations` | `GET /daily?limit=` |
| `SubscriptionController` | `/api/subscriptions` | `GET /me`, `GET /plans`, `POST /subscribe`, `POST /cancel` |
| `FileController` | (mixed) | `POST /api/admin/tracks/upload` (upload mp3 + cover + lyrics + genreIds, tạo Track — yêu cầu ADMIN), `GET /audio/{filename}` (stream với HTTP Range, trả 206 Partial Content nếu có Range) |
| `AdminController` | `/api/admin` | *(xem chi tiết bên dưới)* — yêu cầu ADMIN |
| `UserController` | `/api/users` | `GET /me`, `PUT /me/profile`, `GET /me/profile`, `GET /{id}/profile` (xem hồ sơ) |
| `UserSettingsController` | `/api/users/me/settings` | `GET /`, `PUT /` (5 field: dataSaver, pushNotifications, streamQualityWifi, privateSession, personalizedAds) |
| `StatsController` | `/api/stats` | `GET /listening?period=week\|month\|year&offset=0` — top artist, top track, totalPlays, totalMinutes |

> Controller lấy user hiện tại từ `@AuthenticationPrincipal UserDetails` rồi tra DB để có `userId` (do JWT chỉ chứa username). Đa số controller có private helper `getUserId(userDetails)`.

#### AdminController — chi tiết endpoint (tất cả đều yêu cầu role ADMIN)

Nhận `multipart/form-data`. Xoá trả `204 No Content`; lỗi FK constraint trả `409 Conflict` với message tiếng Việt.

| Method | Path | Params | Mô tả |
|--------|------|--------|-------|
| `POST` | `/api/admin/artists` | `name`*, `bio`, `avatar` (file) | Tạo nghệ sĩ mới |
| `PUT` | `/api/admin/artists/{id}` | `name`*, `bio`, `avatar` (file) | Cập nhật nghệ sĩ; avatar chỉ đổi nếu upload file mới |
| `DELETE` | `/api/admin/artists/{id}` | — | Xoá nghệ sĩ; 409 nếu còn album/bài hát liên quan |
| `POST` | `/api/admin/albums` | `title`*, `artistId`*, `releaseDate` (ISO), `albumType` (default ALBUM), `description`, `cover` (file) | Tạo album |
| `PUT` | `/api/admin/albums/{id}` | tương tự POST | Cập nhật album |
| `DELETE` | `/api/admin/albums/{id}` | — | Xoá album; 409 nếu còn bài hát thuộc album |
| `PUT` | `/api/admin/tracks/{id}` | `title`*, `artistId`*, `albumId`, `duration`, `lyrics`, `genreIds` (list), `cover` (file) | Cập nhật metadata + lyrics + genres của bài hát |
| `DELETE` | `/api/admin/tracks/{id}` | — | Xoá bài hát (JPA tự dọn Track_Genres); 409 nếu còn trong playlist/history |

> `*` = bắt buộc. `FileController` vẫn giữ `POST /api/admin/tracks/upload` để tạo track mới kèm upload file MP3.

---

### 5.7 Package `security`

| Class | Vai trò |
|-------|---------|
| `JwtUtil` | Sinh token (`generateToken`), parse claims, kiểm tra hết hạn. Đọc `jwt.secret` & `jwt.expiration` từ `application.yaml`. Dùng HMAC-SHA của thư viện `jjwt 0.12.6`. |
| `JwtFilter` | `OncePerRequestFilter` — chạy 1 lần / request. Đọc header `Authorization: Bearer ...`, validate, set `SecurityContextHolder`. Đặt trước `UsernamePasswordAuthenticationFilter` trong chain. |
| `UserDetailsServiceImpl` | Implement `UserDetailsService` của Spring Security. Load user từ MySQL theo username, build `UserDetails` chuẩn (Spring) với authority `ROLE_<role.toUpperCase()>`. |

---

### 5.8 Package `config`

| Class | Vai trò |
|-------|---------|
| `SecurityConfig` | Cấu hình `SecurityFilterChain`: tắt CSRF, session STATELESS, public các path `/api/auth/**`, `/audio/**`, `/images/**`, role ADMIN cho `/api/admin/**`, các endpoint còn lại yêu cầu authenticated. Khai báo bean `PasswordEncoder` (`BCryptPasswordEncoder`) và `AuthenticationManager`. Gắn `JwtFilter` vào trước `UsernamePasswordAuthenticationFilter`. **Thêm CORS** cho phép origin `http://localhost:5173` (React admin dev server) — method GET/POST/PUT/DELETE/OPTIONS, tất cả headers, `allowCredentials = true`. |
| `FileStorageConfig` | Implement `WebMvcConfigurer.addResourceHandlers`, map `/audio/**` → folder `D:/music-files/audio`, `/images/**` → folder `D:/music-files/images`. Cho phép truy cập file tĩnh trực tiếp qua URL. |

---

## 6. Bảng đối chiếu URL → Service → Repository (cheat sheet)

| URL | Controller | Service | Repository |
|-----|------------|---------|------------|
| `POST /api/auth/register` | AuthController | AuthService | UserRepository |
| `POST /api/auth/login` | AuthController | AuthService + JwtUtil | UserRepository |
| `GET /api/tracks` | TrackController | TrackService | TrackRepository |
| `GET /api/tracks/{id}/related` | TrackController | RecommendationService | TrackRepository |
| `POST /api/tracks/{id}/like` | TrackController | LibraryService | LikedTrackRepository |
| `GET /api/artists/popular` | ArtistController | (truy vấn trực tiếp) | ArtistRepository |
| `POST /api/artists/{id}/follow` | ArtistController | LibraryService | FollowedArtistRepository |
| `GET /api/playlists/curated?mood=X` | PlaylistController | — | PlaylistRepository |
| `POST /api/playlists/{id}/tracks` | PlaylistController | PlaylistService | PlaylistTrackRepository |
| `POST /api/history?trackId=X` | PlayHistoryController | PlayHistoryService + TrackService | PlayHistoryRepository + TrackRepository |
| `GET /api/home/feed` | HomeController | HomeFeedService (+RecommendationService) | nhiều repo |
| `GET /api/charts/tracks` | ChartController | — | TrackRepository |
| `GET /api/subscriptions/me` | SubscriptionController | SubscriptionService | SubscriptionRepository |
| `POST /api/admin/tracks/upload` | FileController | FileStorageService + TrackService | ArtistRepository + AlbumRepository + GenreRepository + TrackRepository |
| `GET /audio/{filename}` | FileController | — (đọc file trực tiếp) | — |
| `POST /api/admin/artists` | AdminController | ArtistService + FileStorageService | ArtistRepository |
| `PUT /api/admin/artists/{id}` | AdminController | ArtistService + FileStorageService | ArtistRepository |
| `DELETE /api/admin/artists/{id}` | AdminController | ArtistService | ArtistRepository |
| `POST /api/admin/albums` | AdminController | AlbumService + ArtistService + FileStorageService | AlbumRepository + ArtistRepository |
| `PUT /api/admin/albums/{id}` | AdminController | AlbumService + ArtistService + FileStorageService | AlbumRepository + ArtistRepository |
| `DELETE /api/admin/albums/{id}` | AdminController | AlbumService | AlbumRepository |
| `PUT /api/admin/tracks/{id}` | AdminController | TrackService + ArtistService + AlbumService + FileStorageService | TrackRepository + GenreRepository |
| `DELETE /api/admin/tracks/{id}` | AdminController | TrackService | TrackRepository |

---

## 7. Một số điểm cần lưu ý khi đọc code

1. **JWT chỉ chứa `username`** (subject). Mọi controller phải tra `UserRepository.findByUsername` để lấy `userId` — đây là pattern xuyên suốt project.
2. **Curated playlist** là playlist do admin tạo (`isCurated = true`). Home feed nhóm chúng theo `mood`. Logic này nằm hoàn toàn trong `HomeFeedService`.
3. **`HomeSectionDto` không lưu DB** — nó được sinh runtime mỗi lần gọi `/api/home/feed`.
4. **Track-Genre M:N** được khai báo bằng `@ManyToMany` + `@JoinTable` (Hibernate tự sinh bảng `Track_Genres`), khác với 3 bảng trung gian khác (`Playlist_Tracks`, `Liked_Tracks`, `Followed_Artists`) là entity riêng vì có thêm cột phụ (`position`, `liked_at`, `followed_at`). Khi xoá Track, Hibernate tự xoá hàng trong `Track_Genres` (owning side) nhưng **không** tự xoá `Playlist_Tracks`, `Liked_Tracks`, `Play_History` — nếu còn record ở các bảng đó, DB bắn FK violation → AdminController trả `409 Conflict`.
5. **Stream audio**: `FileController.streamAudio` đọc cả file vào RAM (`Files.readAllBytes`) rồi `arraycopy` chunk — đơn giản nhưng tốn bộ nhớ với file lớn. Đây là điểm có thể tối ưu sau.
6. **Lỗi xử lý đơn giản**: hầu hết service throw `RuntimeException` với message tiếng Anh, controller catch rồi trả 4xx. Chưa có `@ControllerAdvice`/exception handler tập trung.
7. **Không cá nhân hoá daily mix**: `RecommendationService.dailyMix` hiện chỉ trả top theo `play_count` toàn hệ thống, không dùng `userId`.
8. **`ddl-auto: update`** — Hibernate tự ALTER TABLE khi entity đổi. Dùng cho dev, không nên dùng production.
9. **CORS chỉ mở cho `http://localhost:5173`** (React admin dev server). Nếu deploy React lên port/domain khác phải cập nhật `SecurityConfig.corsConfigurationSource()`.
10. **Bug đã fix trong `FileController`**: trước đây `albumId` được nhận nhưng không gán vào Track (track luôn `album = null`). Đã sửa — hiện `albumId` được tra `AlbumRepository` và gán đúng vào entity.

---

## 8. Trạng thái dự án

- Backend phase 1 đã hoàn thành: đủ Entity → Repository → Service → Controller cho 12 bảng + JWT + file streaming + home feed + chart + subscription + recommendation cơ bản.
- **Admin API** đã hoàn thiện: `AdminController` cung cấp đầy đủ CRUD Artist/Album và update/delete Track cho trang admin web. `FileController` đã được vá bug albumId và bổ sung lyrics + genreIds.
- **CORS** đã cấu hình cho phép React admin (port 5173) gọi tất cả endpoint.
- Chưa có: React admin frontend, Android client, test tự động, CI/CD, payment gateway, daily mix cá nhân hoá, audio quality variants, collaborative playlist.

---

## 9. Hướng dẫn tích hợp Android

> Đây là tài liệu **dành cho Android developer**. Mô tả chính xác những gì client phải gọi, theo thứ tự nào, và response trả về dạng gì.

### 9.1 Base URL & Header chung

```
Base URL (emulator):  http://10.0.2.2:8080/musicapp/
Base URL (thiết bị thật trên cùng mạng LAN): http://<IP_PC>:8080/musicapp/

Header bắt buộc cho mọi endpoint (trừ /api/auth/**):
  Authorization: Bearer <jwt_token>
  Content-Type: application/json   (với request có body)
```

Token lấy từ `POST /api/auth/login`, lưu vào SharedPreferences / DataStore, tự động gắn vào mọi request qua Retrofit Interceptor.

---

### 9.2 Luồng xác thực

```
1. POST /api/auth/register
   Body: { "username": "...", "email": "...", "password": "..." }
   Response 200: { "message": "Registration successful" }

2. POST /api/auth/login
   Body: { "username": "...", "password": "..." }
   Response 200: { "token": "eyJ...", "username": "...", "role": "user" }
   → Lưu token, username, role vào local storage

3. POST /api/auth/refresh
   Body: { "refreshToken": "<token>" }
   Response 200: JwtResponse mới
   → Gọi khi nhận 401 (token hết hạn)
```

---

### 9.3 Phát nhạc & ghi lượt nghe (QUAN TRỌNG)

Đây là bước **bắt buộc** để `playCount` được cập nhật đúng — ảnh hưởng trực tiếp đến Top 10 bài hát của nghệ sĩ, bảng xếp hạng chart, và đề xuất.

```
Bước 1 — Lấy URL audio:
  Track entity có trường audioUrl, ví dụ: "/audio/uuid_song.mp3"
  URL đầy đủ để ExoPlayer phát: http://10.0.2.2:8080/musicapp/audio/uuid_song.mp3
  Endpoint /audio/** là PUBLIC — không cần token.
  ExoPlayer tự gửi Range header → server trả 206 Partial Content.

Bước 2 — Ghi lại lượt nghe (gọi SAU KHI bài đã thực sự phát, ví dụ sau 30 giây):
  POST /api/history?trackId={trackId}
  Header: Authorization: Bearer <token>
  Body: (không có)
  Response 200: { "message": "Recorded" }

  Tác dụng kép của API này:
    • Lưu hàng vào bảng Play_History (dùng cho Recently Played, Stats)
    • Tăng track.playCount lên 1 (dùng cho Top 10 nghệ sĩ, Chart, Recommendation)
```

**Lưu ý:** Không gọi endpoint này khi user tua qua mà không thực sự nghe, hoặc khi preview ngắn. Nên gọi sau khi bài đã phát được ít nhất 30 giây.

---

### 9.4 Trang nghệ sĩ — Top 10 bài hát

Đây là tính năng tương ứng ảnh `giao_dien_trang_nghe_si.jpg` và `top10_bai_hat.jpg`.

```
1. Thông tin nghệ sĩ:
   GET /api/artists/{artistId}
   Response: Artist { artistId, name, bio, avatarUrl, createdAt }

2. Top 10 bài hát (xếp hạng theo playCount tích lũy):
   GET /api/artists/{artistId}/tracks/popular?limit=10
   Response: List<Track>

3. Danh sách album / đĩa đơn:
   GET /api/artists/{artistId}/albums
   Response: List<Album> — mỗi album có trường albumType: "ALBUM" | "SINGLE" | "EP" | "COMPILATION"

4. Nghệ sĩ liên quan (phần "Nghe thêm"):
   GET /api/artists/{artistId}/related?limit=10
   Response: List<Artist>

5. Toggle follow nghệ sĩ:
   POST /api/artists/{artistId}/follow
   Response: { "message": "OK" }
   (Gọi lại lần 2 sẽ unfollow — idempotent toggle)

6. Kiểm tra user đang follow không:
   GET /api/artists/followed
   Response: List<Artist> — client tự kiểm tra artistId có trong list không
```

**Cấu trúc Track trả về từ popular endpoint:**
```json
{
  "trackId": 1,
  "title": "Tên bài hát",
  "duration": 210,
  "audioUrl": "/audio/uuid_song.mp3",
  "coverUrl": "/images/uuid_cover.jpg",
  "playCount": 1500,
  "lyrics": "Lời bài hát...",
  "createdAt": "2024-01-15T10:00:00",
  "artist": { "artistId": 5, "name": "...", "avatarUrl": "/images/..." },
  "album":  { "albumId": 3, "title": "...", "coverUrl": "/images/..." }
}
```

`audioUrl` và `coverUrl` là path tương đối → prepend base URL để hiển thị / phát.

---

### 9.5 Màn hình Home Feed

```
GET /api/home/feed?filter=all
  filter options: all | music | following
  Response: List<HomeSectionDto>

HomeSectionDto {
  kind:     "FEATURED" | "TOP_PICKS" | "RECENTLY_PLAYED" | "RECOMMENDED"
          | "CHART" | "MOOD_PLAYLIST" | "NEW_RELEASES" | "POPULAR_ARTISTS"
  title:    String  (tiêu đề hiển thị)
  subtitle: String  (có thể null)
  items:    List<?>  (Playlist, Track, hoặc Artist tùy kind)
}

Mapping kind → loại item trong items[]:
  FEATURED         → Playlist (curated)
  TOP_PICKS        → List<Playlist> (curated)
  RECENTLY_PLAYED  → List<RecentItemDto { track, playedAt }>
  RECOMMENDED      → List<Track>
  CHART            → List<Track>
  MOOD_PLAYLIST    → List<Playlist> (curated, cùng mood)
  NEW_RELEASES     → List<Album>
  POPULAR_ARTISTS  → List<Artist>
```

---

### 9.6 Tìm kiếm

```
GET /api/search?q={từ_khóa}
Response: {
  "tracks": List<Track>,
  "artists": List<Artist>
}
```

---

### 9.7 Thư viện cá nhân

```
Bài hát đã thích:
  GET  /api/tracks/liked            → List<LikedTrack { track, likedAt }>
  POST /api/tracks/{id}/like        → toggle (like / unlike)

Nghệ sĩ đang follow:
  GET  /api/artists/followed        → List<Artist>
  POST /api/artists/{id}/follow     → toggle follow / unfollow

Playlist của user:
  GET  /api/playlists               → List<Playlist> (của user hiện tại)
  POST /api/playlists               Body: { "name": "...", "isPublic": false }
  GET  /api/playlists/{id}/tracks   → List<PlaylistTrack> có trường position
  POST /api/playlists/{id}/tracks?trackId={id}   → thêm track
  DELETE /api/playlists/{id}/tracks/{trackId}    → xoá track

Lịch sử nghe gần đây:
  GET /api/history/recent?limit=10
  Response: List<RecentItemDto { track: Track, playedAt: LocalDateTime }>
```

---

### 9.8 Bảng xếp hạng (Chart)

```
Top tracks toàn hệ thống (theo playCount):
  GET /api/charts/tracks?limit=20
  Response: List<Track>

Top artists (theo follower count):
  GET /api/charts/artists?limit=10
  Response: List<Artist>
```

---

### 9.9 Subscription / Premium

```
Gói hiện tại:
  GET /api/subscriptions/me
  Response: { plan: "FREE"|"INDIVIDUAL"|"STUDENT"|"FAMILY", active: bool, endDate: ... }
  (Trả FREE nếu user chưa subscribe)

Danh sách gói:
  GET /api/subscriptions/plans
  Response: List<PlanInfoDto { plan, name, priceVnd, features: List<String> }>

Đăng ký gói:
  POST /api/subscriptions/subscribe
  Body: { "plan": "INDIVIDUAL" }

Huỷ gói:
  POST /api/subscriptions/cancel
```

---

### 9.10 Hồ sơ người dùng & cài đặt

```
Thông tin user hiện tại:
  GET /api/users/me
  Response: UserMeDto { userId, username, email, role, avatarUrl, displayName, bio }

Cập nhật profile:
  PUT /api/users/me/profile
  Body: { "displayName": "...", "bio": "..." }

Cài đặt:
  GET /api/users/me/settings
  PUT /api/users/me/settings
  Body: {
    "dataSaver": false,
    "pushNotifications": true,
    "streamQualityWifi": "HIGH",   // LOW | NORMAL | HIGH | VERY_HIGH | AUTO
    "privateSession": false,
    "personalizedAds": true
  }
```

---

### 9.11 Checklist Android cần implement

| # | Tính năng | Endpoint liên quan | Ghi chú |
|---|-----------|-------------------|---------|
| 1 | Đăng ký / đăng nhập | `POST /api/auth/register`, `POST /api/auth/login` | Lưu JWT token |
| 2 | **Ghi lượt nghe sau khi phát** | `POST /api/history?trackId=X` | **Bắt buộc** để playCount đúng |
| 3 | Trang nghệ sĩ — info | `GET /api/artists/{id}` | |
| 4 | Trang nghệ sĩ — **Top 10 bài** | `GET /api/artists/{id}/tracks/popular?limit=10` | Xếp theo playCount |
| 5 | Trang nghệ sĩ — album | `GET /api/artists/{id}/albums` | Dùng `albumType` để hiện label |
| 6 | Trang nghệ sĩ — nghệ sĩ liên quan | `GET /api/artists/{id}/related` | |
| 7 | Follow / unfollow nghệ sĩ | `POST /api/artists/{id}/follow` | Toggle |
| 8 | Home feed | `GET /api/home/feed?filter=all` | Parse theo `kind` |
| 9 | Tìm kiếm | `GET /api/search?q=` | |
| 10 | Like / unlike bài hát | `POST /api/tracks/{id}/like` | Toggle |
| 11 | Playlist cá nhân | CRUD `/api/playlists` | |
| 12 | Lịch sử nghe gần đây | `GET /api/history/recent?limit=10` | |
| 13 | Bảng xếp hạng | `GET /api/charts/tracks` | |
| 14 | Premium / Subscription | `/api/subscriptions/*` | |
| 15 | Cài đặt | `GET/PUT /api/users/me/settings` | |

### 9.12 Lưu ý khi build Android

- **URL ảnh / audio:** `coverUrl`, `avatarUrl`, `audioUrl` đều là path tương đối (ví dụ `/images/abc.jpg`). Phải prepend base URL: `http://10.0.2.2:8080/musicapp` + path.
- **ExoPlayer URI:** `http://10.0.2.2:8080/musicapp` + `track.audioUrl`. Endpoint `/audio/**` là public, không cần token.
- **albumType enum:** Giá trị string `"ALBUM"` / `"SINGLE"` / `"EP"` / `"COMPILATION"` → hiển thị label tiếng Việt tương ứng.
- **Lazy load ảnh:** Dùng Glide / Coil, tất cả ảnh đều là HTTP (không HTTPS ở môi trường dev).
- **401 Unauthorized:** Token hết hạn sau 24h → gọi `POST /api/auth/refresh` hoặc redirect về màn login.
- **playCount cần dữ liệu thật:** Top 10 bài hát của nghệ sĩ xếp theo `playCount`. Trong môi trường dev/test, nếu chưa có ai nghe thì tất cả đều = 0 → thứ tự sẽ tuỳ ý. Cần gọi `POST /api/history?trackId=X` khi test để có dữ liệu thật.

---

> File này được sinh tự động dựa trên source code thực tế tại thời điểm đọc. Khi entity hay endpoint thay đổi, hãy cập nhật lại các bảng tương ứng.