package com.jnimble.kernel.plugin.descriptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link PluginPlatformVersionMatcher} 单元测试。
 *
 * <p>覆盖精确匹配、前缀匹配、通配符匹配、范围匹配等核心逻辑。</p>
 */
class PluginPlatformVersionMatcherTest {

    // ========== 精确匹配 ==========

    /**
     * 测试完全一致的版本号精确匹配。
     */
    @Test
    void exactMatch() {
        assertThat(PluginPlatformVersionMatcher.matches("1.0", "1.0")).isTrue();
    }

    /**
     * 测试三位版本号精确匹配。
     */
    @Test
    void exactMatchThreePartVersion() {
        assertThat(PluginPlatformVersionMatcher.matches("1.0.0", "1.0.0")).isTrue();
    }

    /**
     * 测试版本号不匹配的情况。
     */
    @Test
    void exactMismatch() {
        assertThat(PluginPlatformVersionMatcher.matches("1.0", "2.0")).isFalse();
    }

    /**
     * 测试版本号部分相同但整体不匹配。
     */
    @Test
    void exactMismatchPartialOverlap() {
        assertThat(PluginPlatformVersionMatcher.matches("1.1", "1.0")).isFalse();
    }

    /**
     * 测试版本号位数不同时逐段比较（尾部补零视为相等）。
     */
    @Test
    void exactMatchDifferentSegmentCount() {
        assertThat(PluginPlatformVersionMatcher.matches("1.0.0", "1.0")).isTrue();
    }

    /**
     * 测试版本号位数不同时逐段比较（尾部补零后不等）。
     */
    @Test
    void exactMismatchDifferentSegmentCount() {
        assertThat(PluginPlatformVersionMatcher.matches("1.0.1", "1.0")).isFalse();
    }

    // ========== 通配符匹配 ==========

    /**
     * 测试通配符 * 匹配任意版本号。
     */
    @ParameterizedTest
    @ValueSource(strings = {"1.0", "2.3.4", "0.0.1", "99.99.99"})
    void wildcardMatchesAnyVersion(String currentVersion) {
        assertThat(PluginPlatformVersionMatcher.matches("*", currentVersion)).isTrue();
    }

    // ========== 前缀匹配（.x 后缀） ==========

    /**
     * 测试前缀匹配 — 1.0.x 匹配 1.0 开头的版本号。
     */
    @Test
    void prefixMatchDotX() {
        assertThat(PluginPlatformVersionMatcher.matches("1.0.x", "1.0.3")).isTrue();
    }

    /**
     * 测试前缀匹配 — 1.x 匹配 1 开头的版本号。
     */
    @Test
    void prefixMatchSingleDigit() {
        assertThat(PluginPlatformVersionMatcher.matches("1.x", "1.2.3")).isTrue();
    }

    /**
     * 测试前缀匹配 — 版本号不匹配前缀。
     */
    @Test
    void prefixMismatch() {
        assertThat(PluginPlatformVersionMatcher.matches("2.0.x", "1.0.0")).isFalse();
    }

    /**
     * 测试前缀匹配 — 版本号恰好等于前缀基础部分时不匹配（需要至少多一段）。
     */
    @Test
    void prefixMismatchExactBase() {
        assertThat(PluginPlatformVersionMatcher.matches("1.0.x", "1.0")).isFalse();
    }

    // ========== 范围匹配（>= 前缀） ==========

    /**
     * 测试 >= 匹配 — 当前版本高于要求版本。
     */
    @Test
    void rangeMatchGreaterThan() {
        assertThat(PluginPlatformVersionMatcher.matches(">=1.0", "2.0")).isTrue();
    }

    /**
     * 测试 >= 匹配 — 当前版本等于要求版本。
     */
    @Test
    void rangeMatchEqual() {
        assertThat(PluginPlatformVersionMatcher.matches(">=1.0", "1.0")).isTrue();
    }

    /**
     * 测试 >= 匹配 — 当前版本低于要求版本。
     */
    @Test
    void rangeMatchLessThan() {
        assertThat(PluginPlatformVersionMatcher.matches(">=2.0", "1.0")).isFalse();
    }

    /**
     * 测试 >= 匹配三位版本号。
     */
    @Test
    void rangeMatchThreePartVersion() {
        assertThat(PluginPlatformVersionMatcher.matches(">=1.0.0", "1.0.1")).isTrue();
    }

    /**
     * 测试 >= 匹配 — 带空格的版本表达式。
     */
    @Test
    void rangeMatchWithSpace() {
        assertThat(PluginPlatformVersionMatcher.matches(">= 1.0", "1.5")).isTrue();
    }

    // ========== 版本比较逻辑 ==========

    /**
     * 测试版本号位数不同时逐段比较。
     */
    @Test
    void compareVersionsWithDifferentSegmentCounts() {
        assertThat(PluginPlatformVersionMatcher.matches(">=1.0", "1.0.1")).isTrue();
    }

    /**
     * 测试高版本作为最低要求不匹配低版本。
     */
    @Test
    void higherMinimumVersionRejectsLower() {
        assertThat(PluginPlatformVersionMatcher.matches(">=1.2", "1.1")).isFalse();
    }

    // ========== 边界情况 ==========

    /**
     * 测试 null 和空字符串返回 false。
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankRequiredVersionReturnsFalse(String required) {
        assertThat(PluginPlatformVersionMatcher.matches(required, "1.0")).isFalse();
    }

    /**
     * 测试 null 当前版本返回 false。
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankCurrentVersionReturnsFalse(String current) {
        assertThat(PluginPlatformVersionMatcher.matches("1.0", current)).isFalse();
    }

    /**
     * 测试两个 null 输入返回 false。
     */
    @Test
    void bothNullReturnsFalse() {
        assertThat(PluginPlatformVersionMatcher.matches(null, null)).isFalse();
    }

    /**
     * 测试带有预发布后缀的版本号能正确解析。
     */
    @Test
    void versionWithPreReleaseSuffix() {
        assertThat(PluginPlatformVersionMatcher.matches("1.0.0", "1.0.0-SNAPSHOT")).isTrue();
    }

    /**
     * 测试版本号含有非数字部分时返回 false。
     */
    @Test
    void nonNumericVersionReturnsFalse() {
        assertThat(PluginPlatformVersionMatcher.matches("abc", "1.0")).isFalse();
    }
}
