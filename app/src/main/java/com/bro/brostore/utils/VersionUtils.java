package com.example.appstoredemo.utils;

/**
 * Utility methods for comparing semantic version strings.
 */
public class VersionUtils {
    /**
     * Compares two dotted version strings component by component.
     *
     * @param v1 first version string.
     * @param v2 second version string.
     * @return positive when {@code v1} is newer, negative when {@code v2} is newer, otherwise zero.
     * @throws IllegalArgumentException if both inputs are {@code null} or empty.
     */
    public static int compare(String v1, String v2) {
        if ((v1 == null || v1.isEmpty()) && (v2 == null || v2.isEmpty())) {
            throw new IllegalArgumentException("At least one version must be provided");
        }
        if (v1 == null && v2 == null) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        String[] s1 = v1.split("\\.");
        String[] s2 = v2.split("\\.");
        int len = Math.max(s1.length, s2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < s1.length ? parsePart(s1[i]) : 0;
            int n2 = i < s2.length ? parsePart(s2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}