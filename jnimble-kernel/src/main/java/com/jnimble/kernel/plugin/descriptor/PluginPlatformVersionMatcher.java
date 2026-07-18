package com.jnimble.kernel.plugin.descriptor;

/**
 * Matches plugin platform version requirements against the current platform version.
 *
 * <p>Supports exact version matching, wildcard ({@code *}), prefix ({@code 1.x}),
 * and greater-than-or-equal ({@code >=}) expressions.</p>
 *
 * 匹配插件声明的平台版本需求与当前平台版本。
 * 支持精确版本匹配、通配符（*）、前缀匹配（1.x）和大于等于（>=）表达式。
 */
public final class PluginPlatformVersionMatcher {

    private PluginPlatformVersionMatcher() {
    }

    /**
     * Checks whether a required platform version matches the current platform version.
     *
     * @param requiredVersion the version expression declared by the plugin
     * @param currentVersion  the current platform version
     * @return {@code true} if the versions are compatible
     */
    public static boolean matches(String requiredVersion, String currentVersion) {
        if (isBlank(requiredVersion) || isBlank(currentVersion)) {
            return false;
        }

        String required = requiredVersion.trim();
        String current = currentVersion.trim();

        if ("*".equals(required)) {
            return true;
        }

        if (required.endsWith(".x")) {
            String prefix = required.substring(0, required.length() - 1);
            return current.startsWith(prefix);
        }

        if (required.startsWith(">=")) {
            return compareVersions(current, required.substring(2).trim()) >= 0;
        }

        return compareVersions(current, required) == 0;
    }

    private static int compareVersions(String left, String right) {
        int[] leftParts = parseVersion(left);
        int[] rightParts = parseVersion(right);

        for (int i = 0; i < Math.max(leftParts.length, rightParts.length); i++) {
            int leftPart = i < leftParts.length ? leftParts[i] : 0;
            int rightPart = i < rightParts.length ? rightParts[i] : 0;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    private static int[] parseVersion(String version) {
        String normalized = version.split("-", 2)[0];
        String[] parts = normalized.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ex) {
                return new int[] {-1};
            }
        }
        return numbers;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
