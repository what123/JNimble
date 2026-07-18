package com.jnimble.platform.i18n;

import java.text.MessageFormat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Dynamic message source that supports both system and plugin-specific i18n messages.
 * Allows plugins to register their own message bundles that are resolved alongside
 * system messages with plugin messages taking priority.
 */
public class DynamicPluginMessageSource extends AbstractMessageSource {

    private final ResourceBundleMessageSource systemMessageSource = new ResourceBundleMessageSource();
    private final Map<String, PluginMessageBundle> pluginMessageSources = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code DynamicPluginMessageSource} with system message basenames.
     *
     * @param systemBasenames the basenames for system message bundles
     */
    public DynamicPluginMessageSource(List<String> systemBasenames) {
        systemMessageSource.setBasenames(systemBasenames.toArray(String[]::new));
        systemMessageSource.setDefaultEncoding("UTF-8");
        systemMessageSource.setFallbackToSystemLocale(false);
    }

    /**
     * Registers a plugin's message bundle for i18n support.
     *
     * @param pluginId   the unique identifier of the plugin
     * @param basename   the resource bundle basename
     * @param classLoader the classloader to load messages from
     * @throws IllegalArgumentException if pluginId or basename is blank
     */
    public void registerPluginMessages(String pluginId, String basename, ClassLoader classLoader) {
        String normalizedPluginId = requireNonBlank(pluginId, "pluginId");
        String normalizedBasename = requireNonBlank(basename, "basename");
        pluginMessageSources.put(normalizedPluginId,
                new PluginMessageBundle(normalizedBasename,
                        classLoader == null ? getClass().getClassLoader() : classLoader));
    }

    /**
     * Unregisters a plugin's message bundle.
     *
     * @param pluginId the unique identifier of the plugin
     * @throws IllegalArgumentException if pluginId is blank
     */
    public void unregisterPluginMessages(String pluginId) {
        pluginMessageSources.remove(requireNonBlank(pluginId, "pluginId"));
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String message = resolvePluginMessage(code, locale);
        if (message == null) {
            message = systemMessageSource.getMessage(code, null, null, locale);
        }
        return message == null ? null : createMessageFormat(message, locale);
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        String message = resolvePluginMessage(code, locale);
        return message == null ? systemMessageSource.getMessage(code, null, null, locale) : message;
    }

    private String resolvePluginMessage(String code, Locale locale) {
        List<PluginMessageBundle> snapshot = new ArrayList<>(pluginMessageSources.values());
        for (PluginMessageBundle messageSource : snapshot) {
            String message = messageSource.resolve(code, locale);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    private static String requireNonBlank(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static final class PluginMessageBundle {

        private static final ResourceBundle.Control CONTROL =
                ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES);

        private final String baseResourcePath;
        private final ClassLoader classLoader;
        private final Map<Locale, Properties> messagesByLocale = new ConcurrentHashMap<>();

        private PluginMessageBundle(String basename, ClassLoader classLoader) {
            this.baseResourcePath = basename.replace('.', '/');
            this.classLoader = classLoader;
        }

        private String resolve(String code, Locale locale) {
            return messagesByLocale
                    .computeIfAbsent(normalizeLocale(locale), this::loadMessages)
                    .getProperty(code);
        }

        private Properties loadMessages(Locale locale) {
            Properties messages = new Properties();
            List<Locale> candidateLocales = new ArrayList<>(CONTROL.getCandidateLocales("messages", locale));
            Collections.reverse(candidateLocales);
            candidateLocales.forEach(candidate -> loadResource(messages, resourceName(candidate)));
            return messages;
        }

        private void loadResource(Properties messages, String resourceName) {
            try {
                Enumeration<URL> resources = classLoader.getResources(resourceName);
                for (URL resource : Collections.list(resources)) {
                    try (InputStreamReader reader =
                                 new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)) {
                        messages.load(reader);
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load plugin messages from " + resourceName, ex);
            }
        }

        private String resourceName(Locale locale) {
            if (locale == Locale.ROOT || locale.getLanguage().isBlank()) {
                return baseResourcePath + ".properties";
            }

            List<String> parts = new ArrayList<>();
            parts.add(locale.getLanguage());
            if (!locale.getScript().isBlank()) {
                parts.add(locale.getScript());
            }
            if (!locale.getCountry().isBlank()) {
                parts.add(locale.getCountry());
            }
            if (!locale.getVariant().isBlank()) {
                parts.add(locale.getVariant());
            }
            return baseResourcePath + "_" + String.join("_", parts) + ".properties";
        }

        private Locale normalizeLocale(Locale locale) {
            return locale == null ? Locale.getDefault() : locale;
        }
    }
}
