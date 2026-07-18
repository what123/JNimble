package com.jnimble.kernel.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Small version-expression matcher for plugin dependency declarations.
 * Supports exact versions, {@code *}, {@code 1.x}, {@code 1.2.x}, and the
 * comparison operators {@code >=}, {@code >}, {@code <=}, and {@code <}.
 */
final class PluginVersionMatcher {

    private PluginVersionMatcher() {
    }

    static boolean matches(String version, String expression) {
        if (version == null || version.isBlank()) {
            return false;
        }
        if (expression == null || expression.isBlank() || "*".equals(expression.trim())) {
            return true;
        }
        String expected = expression.trim();
        if (expected.startsWith(">=")) {
            return compare(version, expected.substring(2)) >= 0;
        }
        if (expected.startsWith("<=")) {
            return compare(version, expected.substring(2)) <= 0;
        }
        if (expected.startsWith(">")) {
            return compare(version, expected.substring(1)) > 0;
        }
        if (expected.startsWith("<")) {
            return compare(version, expected.substring(1)) < 0;
        }
        String[] expectedParts = normalize(expected).split("\\.");
        String[] actualParts = normalize(version).split("\\.");
        for (int i = 0; i < expectedParts.length; i++) {
            String expectedPart = expectedParts[i];
            if ("x".equalsIgnoreCase(expectedPart) || "*".equals(expectedPart)) {
                return true;
            }
            String actualPart = i < actualParts.length ? actualParts[i] : "0";
            if (!expectedPart.equals(actualPart)) {
                return false;
            }
        }
        return actualParts.length == expectedParts.length;
    }

    private static int compare(String left, String right) {
        List<Integer> leftParts = numericParts(left);
        List<Integer> rightParts = numericParts(right);
        int length = Math.max(leftParts.size(), rightParts.size());
        for (int i = 0; i < length; i++) {
            int leftPart = i < leftParts.size() ? leftParts.get(i) : 0;
            int rightPart = i < rightParts.size() ? rightParts.get(i) : 0;
            int comparison = Integer.compare(leftPart, rightPart);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static List<Integer> numericParts(String value) {
        String[] parts = normalize(value).split("\\.");
        List<Integer> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                result.add(Integer.parseInt(part));
            } catch (NumberFormatException ex) {
                throw new PluginRuntimeException("Unsupported plugin version: " + value, ex);
            }
        }
        return result;
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        int qualifier = normalized.indexOf('-');
        return qualifier < 0 ? normalized : normalized.substring(0, qualifier);
    }
}
