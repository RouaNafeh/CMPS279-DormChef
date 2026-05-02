package com.cookio.app.utils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CookTimeFormatter {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private CookTimeFormatter() {
    }

    public static String normalize(String rawTime) {
        if (TextUtils.isEmpty(rawTime)) {
            return "";
        }

        String normalized = rawTime.trim().toLowerCase(Locale.ROOT)
                .replace("minutes", "min")
                .replace("minute", "min")
                .replace("mins", "min")
                .replace("hours", "hr")
                .replace("hour", "hr")
                .replaceAll("\\s+", " ");

        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("(\\d+)\\s*(hr|min)").matcher(normalized);
        while (matcher.find()) {
            tokens.add(matcher.group(1) + " " + matcher.group(2));
        }

        if (!tokens.isEmpty()) {
            return TextUtils.join(" ", tokens);
        }

        Matcher firstNumber = NUMBER_PATTERN.matcher(normalized);
        if (firstNumber.find()) {
            int value = Integer.parseInt(firstNumber.group(1));
            if (normalized.contains("hr")) {
                return value + " hr";
            }
            return value + " min";
        }

        return "";
    }

    public static int toSortMinutes(String rawTime) {
        String normalized = normalize(rawTime);
        if (TextUtils.isEmpty(normalized)) {
            return Integer.MAX_VALUE;
        }

        int total = 0;
        Matcher matcher = Pattern.compile("(\\d+)\\s*(hr|min)").matcher(normalized);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            total += "hr".equals(matcher.group(2)) ? value * 60 : value;
        }

        return total == 0 ? Integer.MAX_VALUE : total;
    }
}
