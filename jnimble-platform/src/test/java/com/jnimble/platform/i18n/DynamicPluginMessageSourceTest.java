package com.jnimble.platform.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DynamicPluginMessageSource} 的单元测试。
 *
 * <p>测试插件消息的注册、注销、解析优先级以及与系统消息的集成。</p>
 */
class DynamicPluginMessageSourceTest {

    private static final Locale LOCALE_CN = Locale.SIMPLIFIED_CHINESE;
    private static final Locale LOCALE_EN = Locale.US;
    private static final Locale LOCALE_ROOT = Locale.ROOT;

    private DynamicPluginMessageSource messageSource;

    @BeforeEach
    void setUp() {
        messageSource = new DynamicPluginMessageSource(List.of("messages"));
    }

    // ======================== getMessage ========================

    @Nested
    @DisplayName("getMessage 方法测试")
    class GetMessageTests {

        @Test
        @DisplayName("从系统消息中获取已定义的国际化文本")
        void shouldResolveSystemMessageByKey() {
            String result = messageSource.getMessage("system.greeting", null, LOCALE_CN);
            assertThat(result).isEqualTo("你好");
        }

        @Test
        @DisplayName("从系统消息中获取英文版本")
        void shouldResolveSystemMessageInEnglish() {
            String result = messageSource.getMessage("system.greeting", null, LOCALE_EN);
            assertThat(result).isEqualTo("Hello");
        }

        @Test
        @DisplayName("查询不存在的 key 时返回默认值")
        void shouldReturnDefaultWhenKeyNotFound() {
            String result = messageSource.getMessage("nonexistent.key", null, "默认值", LOCALE_CN);
            assertThat(result).isEqualTo("默认值");
        }

        @Test
        @DisplayName("查询不存在的 key 且无默认值时返回 null")
        void shouldReturnNullWhenKeyNotFoundAndNoDefault() {
            String result = messageSource.getMessage("nonexistent.key", null, null, LOCALE_CN);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("使用带参数的 MessageFormat 消息进行格式化")
        void shouldFormatMessageWithArguments() {
            String result = messageSource.getMessage("system.format", new Object[]{"张三", 25}, LOCALE_CN);
            assertThat(result).isEqualTo("用户 张三 年龄 25");
        }
    }

    // ======================== registerPluginMessages / unregisterPluginMessages ========================

    @Nested
    @DisplayName("插件消息注册与注销测试")
    class PluginRegistrationTests {

        @Test
        @DisplayName("注册插件消息后应能解析插件自定义的 key")
        void shouldResolvePluginMessageAfterRegistration() {
            messageSource.registerPluginMessages("test-plugin", "plugin-messages", getClass().getClassLoader());

            String result = messageSource.getMessage("plugin.hello", null, LOCALE_CN);
            assertThat(result).isEqualTo("插件你好");
        }

        @Test
        @DisplayName("插件消息优先于系统消息")
        void shouldPrioritizePluginMessageOverSystemMessage() {
            messageSource.registerPluginMessages("override-plugin", "plugin-override-messages",
                    getClass().getClassLoader());

            String result = messageSource.getMessage("system.greeting", null, LOCALE_CN);
            assertThat(result).isEqualTo("插件覆盖的你好");
        }

        @Test
        @DisplayName("注销插件消息后应回退到系统消息")
        void shouldFallbackToSystemMessageAfterUnregister() {
            messageSource.registerPluginMessages("override-plugin", "plugin-override-messages",
                    getClass().getClassLoader());
            String before = messageSource.getMessage("system.greeting", null, LOCALE_CN);
            assertThat(before).isEqualTo("插件覆盖的你好");

            messageSource.unregisterPluginMessages("override-plugin");
            String after = messageSource.getMessage("system.greeting", null, LOCALE_CN);
            assertThat(after).isEqualTo("你好");
        }

        @Test
        @DisplayName("注销不存在的插件应不抛出异常")
        void shouldNotThrowWhenUnregisteringNonexistentPlugin() {
            messageSource.unregisterPluginMessages("nonexistent-plugin");
        }

        @Test
        @DisplayName("pluginId 为 null 时应抛出 NullPointerException")
        void shouldThrowWhenPluginIdIsNull() {
            assertThatThrownBy(() ->
                    messageSource.registerPluginMessages(null, "basename", getClass().getClassLoader()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("pluginId 为空白时应抛出 IllegalArgumentException")
        void shouldThrowWhenPluginIdIsBlank() {
            assertThatThrownBy(() ->
                    messageSource.registerPluginMessages("  ", "basename", getClass().getClassLoader()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pluginId is required");
        }

        @Test
        @DisplayName("basename 为空白时应抛出 IllegalArgumentException")
        void shouldThrowWhenBasenameIsBlank() {
            assertThatThrownBy(() ->
                    messageSource.registerPluginMessages("plugin", "  ", getClass().getClassLoader()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("basename is required");
        }

        @Test
        @DisplayName("classLoader 为 null 时应使用默认 ClassLoader")
        void shouldUseDefaultClassLoaderWhenNull() {
            messageSource.registerPluginMessages("test-plugin", "plugin-messages", null);

            String result = messageSource.getMessage("plugin.hello", null, LOCALE_CN);
            assertThat(result).isEqualTo("插件你好");
        }

        @Test
        @DisplayName("多个插件注册相同 key 时，应返回其中一个插件的消息而非系统消息")
        void shouldReturnPluginMessageWhenMultiplePluginsRegisterSameKey() {
            messageSource.registerPluginMessages("plugin-a", "plugin-messages-a",
                    getClass().getClassLoader());
            messageSource.registerPluginMessages("plugin-b", "plugin-messages-b",
                    getClass().getClassLoader());

            String result = messageSource.getMessage("plugin.shared", null, LOCALE_CN);
            assertThat(result).isIn("来自插件A", "来自插件B");
        }
    }

    // ======================== resolveCode 内部逻辑验证 ========================

    @Nested
    @DisplayName("消息解析优先级测试")
    class MessageResolutionTests {

        @Test
        @DisplayName("系统消息应通过默认 MessageFormat 正确解析")
        void shouldResolveSystemMessageViaMessageFormat() {
            String result = messageSource.getMessage("system.greeting", null, LOCALE_CN);
            assertThat(result).isEqualTo("你好");
        }

        @Test
        @DisplayName("查询不存在的 key 时默认 getMessage 应返回 null")
        void shouldReturnNullWhenKeyNotFoundViaGetMessage() {
            String result = messageSource.getMessage("nonexistent.key", null, null, LOCALE_CN);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("插件消息应覆盖系统消息并正确解析")
        void shouldResolvePluginMessageOverSystemMessage() {
            messageSource.registerPluginMessages("test-plugin", "plugin-messages", getClass().getClassLoader());

            String result = messageSource.getMessage("plugin.hello", null, LOCALE_CN);
            assertThat(result).isEqualTo("插件你好");
        }

        @Test
        @DisplayName("插件注销后应恢复使用系统消息")
        void shouldRestoreSystemMessageAfterPluginUnregister() {
            messageSource.registerPluginMessages("test-plugin", "plugin-messages", getClass().getClassLoader());
            messageSource.unregisterPluginMessages("test-plugin");

            String result = messageSource.getMessage("system.greeting", null, LOCALE_CN);
            assertThat(result).isEqualTo("你好");
        }
    }

    // ======================== 辅助方法 ========================

    private static Properties createProperties(String... keyValuePairs) {
        Properties props = new Properties();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            props.setProperty(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return props;
    }
}
