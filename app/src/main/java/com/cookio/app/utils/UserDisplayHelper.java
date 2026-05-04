package com.cookio.app.utils;

import android.text.TextUtils;

import java.util.Locale;

public final class UserDisplayHelper {
    private UserDisplayHelper() {
    }

    public static String resolveDisplayName(String name, String username, String fallback) {
        if (!TextUtils.isEmpty(name)) {
            return name.trim();
        }
        if (!TextUtils.isEmpty(username)) {
            return username.trim();
        }
        return fallback;
    }

    public static String resolveHandle(String username) {
        return UsernameHelper.formatHandle(UsernameHelper.normalize(username));
    }

    public static String resolveInitial(String value, String fallback) {
        String source = TextUtils.isEmpty(value) ? fallback : value.trim();
        if (TextUtils.isEmpty(source)) {
            return "C";
        }
        return source.substring(0, 1).toUpperCase(Locale.getDefault());
    }
}
