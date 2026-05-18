package com.example.musicstreamingapp.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcParser {

    public static class LrcLine {
        public final long timeMs;
        public final String text;

        public LrcLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    // Hỗ trợ cả dấu chấm và dấu hai chấm trước phần thập phân: [mm:ss.xx] hoặc [mm:ss:xx]
    private static final Pattern LINE_PATTERN =
        Pattern.compile("^\\[(\\d{2}):(\\d{2})[.：:](\\d{2,3})](.*)");;

    // [offset:N] hoặc [offset:+N] hoặc [offset:-N] — chuẩn LRC
    private static final Pattern OFFSET_PATTERN =
        Pattern.compile("^\\[offset:([+-]?\\d+)]$", Pattern.CASE_INSENSITIVE);

    public static boolean isLrc(String raw) {
        if (raw == null) return false;
        for (String line : raw.split("\n")) {
            if (LINE_PATTERN.matcher(line.trim()).matches()) return true;
        }
        return false;
    }

    /**
     * Parse LRC lyrics string.
     *
     * Hỗ trợ:
     * - [mm:ss.xx] / [mm:ss:xx] với 2-3 chữ số thập phân
     * - [offset:N] — dịch tất cả timestamp N ms (âm = hiện sớm hơn, dương = hiện muộn hơn)
     * - Nếu không phải LRC, parse plain text từng dòng với timeMs=0
     */
    public static List<LrcLine> parse(String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        if (!isLrc(raw)) {
            List<LrcLine> result = new ArrayList<>();
            for (String line : raw.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) result.add(new LrcLine(0, trimmed));
            }
            return result;
        }

        // Bước 1: tìm offset (nếu có)
        long offsetMs = 0;
        for (String rawLine : raw.split("\n")) {
            Matcher m = OFFSET_PATTERN.matcher(rawLine.trim());
            if (m.matches()) {
                try { offsetMs = Long.parseLong(m.group(1)); } catch (NumberFormatException ignored) {}
                break;
            }
        }

        // Bước 2: parse từng dòng
        List<LrcLine> result = new ArrayList<>();
        for (String rawLine : raw.split("\n")) {
            Matcher m = LINE_PATTERN.matcher(rawLine.trim());
            if (!m.matches()) continue;
            int min = Integer.parseInt(m.group(1));
            int sec = Integer.parseInt(m.group(2));
            String decStr = m.group(3);
            // 2 chữ số = centiseconds (×10 → ms), 3 chữ số = ms trực tiếp
            long decMs = decStr.length() >= 3
                ? Long.parseLong(decStr.substring(0, 3))
                : Long.parseLong(decStr) * 10L;
            long timeMs = min * 60_000L + sec * 1_000L + decMs + offsetMs;
            String text = m.group(4).trim();
            if (!text.isEmpty() && timeMs >= 0) result.add(new LrcLine(timeMs, text));
        }
        result.sort((a, b) -> Long.compare(a.timeMs, b.timeMs));
        return result;
    }

    /** Join all line texts without timestamps, for plain-text display. */
    public static String toCleanText(List<LrcLine> lines) {
        StringBuilder sb = new StringBuilder();
        for (LrcLine line : lines) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(line.text);
        }
        return sb.toString();
    }

    /**
     * Offset toàn cục (ms) cộng vào vị trí phát khi tìm dòng active.
     * Tăng giá trị này nếu lyrics hiện muộn, giảm nếu hiện sớm.
     * Ví dụ: lyrics chậm 2 giây → đặt 2000.
     */
    public static final long LYRICS_OFFSET_MS = 0;

    /** Binary search: last line whose timeMs <= (positionMs + LYRICS_OFFSET_MS) */
    public static int findActiveIndex(List<LrcLine> lines, long positionMs) {
        positionMs += LYRICS_OFFSET_MS;
        if (lines.isEmpty()) return -1;
        int lo = 0, hi = lines.size() - 1, result = 0;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (lines.get(mid).timeMs <= positionMs) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }
}
