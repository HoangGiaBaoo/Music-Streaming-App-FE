package com.example.musicstreamingapp.util;

import com.example.musicstreamingapp.model.Subscription;

/**
 * Quy tắc chuẩn xác định user có phải Premium hay không.
 * Dùng chung cho MainActivity (ẩn/hiện quảng cáo), PlayerActivity (badge HQ), SettingsActivity (gate).
 */
public final class PremiumChecker {

    private PremiumChecker() {}

    public static boolean isPremium(Subscription sub) {
        if (sub == null) return false;
        if (sub.getPlan() == null || "FREE".equals(sub.getPlan())) return false;
        // active phải true
        if (sub.getActive() == null || !sub.getActive()) return false;
        // pending phải false (hoặc null cho sub cũ trước khi backend thêm field)
        if (sub.getPending() != null && sub.getPending()) return false;
        return true;
    }
}
