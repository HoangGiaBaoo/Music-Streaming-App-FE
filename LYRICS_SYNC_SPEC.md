# Spec: Synchronized Lyrics (karaoke-style) — Backend Changes

## Bối cảnh

Client Android muốn hiển thị lời bài hát theo từng dòng đồng bộ với vị trí phát nhạc giống Spotify.
Hiện tại trường `lyrics` trong bảng `Tracks` đang lưu plain text (đoạn văn bản, ngăn cách bằng `\n`).

---

## Giải pháp đề xuất: Dùng định dạng LRC trong cột `lyrics` có sẵn

**Không cần thêm cột mới.** Thay đổi duy nhất: admin nhập lyrics theo định dạng LRC thay vì plain text.

### Định dạng LRC

```
[00:00.00]
[00:12.34]Lời dòng một
[00:16.89]Lời dòng hai
[00:21.04]Lời dòng ba
[01:05.22]Điệp khúc...
```

Mỗi dòng bắt đầu bằng timestamp `[mm:ss.xx]` (phút:giây.centisecond).
Dòng trống hoặc không có timestamp → bỏ qua (dùng làm ngắt đoạn).

### Quy tắc backward compatibility

- Nếu `lyrics` **không chứa dấu `[`** → coi là plain text, hiển thị toàn bộ (không sync).
- Nếu `lyrics` **có dạng LRC** → Android client parse và sync.

---

## Thay đổi cần làm phía backend

### 1. Không cần thay đổi Entity / Schema

Cột `lyrics TEXT` trong `Tracks` đã đủ — lưu cả plain text lẫn LRC.

### 2. Cập nhật `PUT /api/admin/tracks/{id}`

Endpoint này nhận `lyrics` từ form-data. **Không cần sửa logic** — chỉ cần đảm bảo backend không parse/transform nội dung trường `lyrics`, lưu nguyên văn vào DB.

> Xác nhận: Hiện tại `AdminController.updateTrack` nhận `@RequestParam String lyrics` rồi gán thẳng vào entity — không transform → **không cần sửa gì**.

### 3. Cập nhật `POST /api/admin/tracks/upload` (FileController)

Tương tự — nhận `lyrics` raw string, lưu thẳng. Không cần sửa nếu đã làm vậy.

### 4. `GET /api/tracks/{id}` — response đã trả đủ

Response đang trả nguyên `Track` entity (gồm cả `lyrics`). Android nhận trường `lyrics` và tự parse.

---

## Phía Android sẽ làm gì (để tham khảo)

Android không cần backend thay đổi API response, chỉ cần:

1. **Parse LRC từ `track.getLyrics()`:**
   ```
   Regex: ^\[(\d{2}):(\d{2})\.(\d{2})\](.*)$
   → Tạo List<LrcLine> {timeMs: long, text: String}
   → Sort by timeMs
   ```

2. **Đặt timer poll vị trí ExoPlayer mỗi 500ms:**
   ```java
   long currentMs = PlayerManager.getInstance().getPlayer().getCurrentPosition();
   // Tìm dòng có timeMs <= currentMs và timeMs tiếp theo > currentMs
   // Highlight dòng đó, scroll RecyclerView đến dòng đó
   ```

3. **UI LyricsActivity:** RecyclerView với `LinearLayoutManager`, dòng hiện tại được highlight màu trắng đậm, các dòng khác mờ.

---

## Kết luận

| Hạng mục | Cần làm |
|----------|---------|
| Entity `Track` | ❌ Không đổi |
| Schema DB | ❌ Không đổi |
| `PUT /api/admin/tracks/{id}` | ❌ Không đổi (đã lưu raw) |
| `GET /api/tracks/{id}` | ❌ Không đổi |
| Admin nhập lyrics | ✅ Nhập theo định dạng LRC thay vì plain text |
| Android client | ✅ Parse LRC + sync với ExoPlayer position |

**Tóm lại: Backend không cần sửa gì. Chỉ cần admin nhập lyrics đúng định dạng LRC.**

---

## Lưu ý quan trọng

- LRC timestamp tính bằng **giây** (ví dụ `[01:23.45]` = 83.45 giây = 83450 ms).
- ExoPlayer trả `getCurrentPosition()` bằng **milliseconds** → phải nhân `xx * 10` khi parse centiseconds.
- File LRC có thể lấy từ các nguồn miễn phí: netease-cloud-music API, lrclib.net, hoặc tạo tay bằng Audacity/Subtitle Edit.
