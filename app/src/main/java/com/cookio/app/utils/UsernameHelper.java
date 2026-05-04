package com.cookio.app.utils;

import android.text.TextUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class UsernameHelper {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._]{3,20}$");

    private UsernameHelper() {
    }

    public static String normalize(String rawUsername) {
        if (rawUsername == null) {
            return "";
        }
        return rawUsername.trim().toLowerCase(Locale.getDefault());
    }

    public static boolean isValid(String username) {
        return !TextUtils.isEmpty(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    public static String formatHandle(String username) {
        if (TextUtils.isEmpty(username)) {
            return "";
        }
        return "@" + username;
    }
}
