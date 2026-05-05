package com.example.musicstreamingapp.util;

import java.util.Locale;

public class TimeUtil {
    public static String formatSeconds(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    public static String formatMs(long ms) {
        return formatSeconds((int)(ms / 1000));
    }
}
