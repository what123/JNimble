package com.jnimble.kernel.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * {@link PluginIds} 单元测试。
 *
 * <p>覆盖 requireValid() 和 isValid() 的各种边界情况，
 * 包括合法 ID、非法 ID、null 值、空白值等。</p>
 */
class PluginIdsTest {

    // ========== requireValid() 测试 ==========

    /**
     * 测试合法的简单插件 ID 校验通过。
     */
    @Test
    void requireValidSimpleId() {
        assertThat(PluginIds.requireValid("hello")).isEqualTo("hello");
    }

    /**
     * 测试包含连字符的合法插件 ID 校验通过。
     */
    @Test
    void requireValidIdWithHyphens() {
        assertThat(PluginIds.requireValid("my-plugin")).isEqualTo("my-plugin");
    }

    /**
     * 测试包含数字的合法插件 ID 校验通过。
     */
    @Test
    void requireValidIdWithNumbers() {
        assertThat(PluginIds.requireValid("plugin123")).isEqualTo("plugin123");
    }

    /**
     * 测试混合字母、数字和连字符的合法插件 ID 校验通过。
     */
    @Test
    void requireValidIdWithMixedCharacters() {
        assertThat(PluginIds.requireValid("my-plugin-v2")).isEqualTo("my-plugin-v2");
    }

    /**
     * 测试单字符合法插件 ID 校验通过。
     */
    @Test
    void requireValidSingleCharacterId() {
        assertThat(PluginIds.requireValid("a")).isEqualTo("a");
    }

    /**
     * 测试 null 插件 ID 抛出异常。
     */
    @Test
    void requireValidNullThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid(null))
                .withMessageContaining("must not be blank");
    }

    /**
     * 测试空白插件 ID 抛出异常。
     */
    @Test
    void requireValidBlankThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid("   "))
                .withMessageContaining("must not be blank");
    }

    /**
     * 测试空字符串插件 ID 抛出异常。
     */
    @Test
    void requireValidEmptyThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid(""))
                .withMessageContaining("must not be blank");
    }

    /**
     * 测试包含大写字母的插件 ID 抛出异常。
     */
    @Test
    void requireValidWithUppercaseThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid("MyPlugin"))
                .withMessageContaining("must match");
    }

    /**
     * 测试以数字开头的插件 ID 抛出异常。
     */
    @Test
    void requireValidStartingWithDigitThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid("1plugin"))
                .withMessageContaining("must match");
    }

    /**
     * 测试包含特殊字符的插件 ID 抛出异常。
     */
    @Test
    void requireValidWithSpecialCharactersThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid("my@plugin"))
                .withMessageContaining("must match");
    }

    /**
     * 测试包含下划线的插件 ID 抛出异常（只允许连字符）。
     */
    @Test
    void requireValidWithUnderscoreThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid("my_plugin"))
                .withMessageContaining("must match");
    }

    /**
     * 测试以连字符开头的插件 ID 抛出异常（必须以小写字母开头）。
     */
    @Test
    void requireValidStartingWithHyphenThrowsException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid("-plugin"))
                .withMessageContaining("must match");
    }

    /**
     * 测试带前后空格的合法插件 ID 被 trim 后校验通过。
     */
    @Test
    void requireValidTrimsWhitespace() {
        assertThat(PluginIds.requireValid("  hello  ")).isEqualTo("hello");
    }

    /**
     * 测试自定义字段名的错误消息。
     */
    @Test
    void requireValidCustomFieldName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginIds.requireValid(null, "pluginId"))
                .withMessageContaining("pluginId");
    }

    // ========== isValid() 测试 ==========

    /**
     * 测试合法插件 ID 返回 true。
     */
    @Test
    void isValidReturnsTrueForValidId() {
        assertThat(PluginIds.isValid("my-plugin")).isTrue();
    }

    /**
     * 测试 null 插件 ID 返回 false。
     */
    @Test
    void isValidReturnsFalseForNull() {
        assertThat(PluginIds.isValid(null)).isFalse();
    }

    /**
     * 测试空字符串插件 ID 返回 false。
     */
    @Test
    void isValidReturnsFalseForEmpty() {
        assertThat(PluginIds.isValid("")).isFalse();
    }

    /**
     * 测试包含大写字母的插件 ID 返回 false。
     */
    @Test
    void isValidReturnsFalseForUppercase() {
        assertThat(PluginIds.isValid("MyPlugin")).isFalse();
    }

    /**
     * 测试包含特殊字符的插件 ID 返回 false。
     */
    @Test
    void isValidReturnsFalseForSpecialCharacters() {
        assertThat(PluginIds.isValid("my@plugin")).isFalse();
    }

    /**
     * 测试以数字开头的插件 ID 返回 false。
     */
    @Test
    void isValidReturnsFalseForDigitStart() {
        assertThat(PluginIds.isValid("1plugin")).isFalse();
    }

    /**
     * 测试单个小写字母返回 true。
     */
    @Test
    void isValidReturnsTrueForSingleLetter() {
        assertThat(PluginIds.isValid("a")).isTrue();
    }

    // ========== PATTERN 常量测试 ==========

    /**
     * 测试 PATTERN 正则表达式格式正确。
     */
    @Test
    void patternMatchesExpectedRegex() {
        assertThat(PluginIds.PATTERN.pattern()).isEqualTo("^[a-z][a-z0-9-]*$");
    }
}
