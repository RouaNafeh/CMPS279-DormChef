package com.cookio.app.models;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public final class CookingStepParser {

    private CookingStepParser() {}

    public static List<CookingStep> parseList(List<String> rawSteps) {
        List<CookingStep> parsed = new ArrayList<>();
        if (rawSteps == null) {
            return parsed;
        }

        for (String rawStep : rawSteps) {
            CookingStep step = parseSingle(rawStep);
            if (!TextUtils.isEmpty(step.getInstruction())) {
                parsed.add(step);
            }
        }
        return parsed;
    }

    public static List<CookingStep> parseDelimited(String rawSteps) {
        List<CookingStep> parsed = new ArrayList<>();
        if (rawSteps == null || rawSteps.trim().isEmpty()) {
            return parsed;
        }

        String normalized = rawSteps.replace("\r", "");
        String[] segments = normalized.contains("||")
                ? normalized.split("\\|\\|")
                : normalized.split("\\n");

        for (String segment : segments) {
            CookingStep step = parseSingle(segment);
            if (!TextUtils.isEmpty(step.getInstruction())) {
                parsed.add(step);
            }
        }
        return parsed;
    }

    public static CookingStep parseSingle(String rawStep) {
        if (rawStep == null) {
            return new CookingStep("", 0);
        }

        String trimmed = rawStep.trim();
        if (trimmed.isEmpty()) {
            return new CookingStep("", 0);
        }

        String[] parts = trimmed.split("\\|");
        if (parts.length >= 2) {
            String possibleMinutes = parts[parts.length - 1].trim();
            int minutes = parseMinutes(possibleMinutes);
            if (isNumeric(possibleMinutes)) {
                StringBuilder instruction = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (instruction.length() > 0) {
                        instruction.append(" | ");
                    }
                    instruction.append(parts[i].trim());
                }
                return new CookingStep(instruction.toString(), minutes);
            }
        }

        return new CookingStep(trimmed, 0);
    }

    public static String encodeSteps(List<CookingStep> steps) {
        List<String> encoded = new ArrayList<>();
        if (steps == null) {
            return "";
        }

        for (CookingStep step : steps) {
            if (step != null && !TextUtils.isEmpty(step.getInstruction())) {
                encoded.add(step.encode());
            }
        }
        return TextUtils.join("\n", encoded);
    }

    private static int parseMinutes(String rawMinutes) {
        try {
            return Math.max(0, Integer.parseInt(rawMinutes));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
