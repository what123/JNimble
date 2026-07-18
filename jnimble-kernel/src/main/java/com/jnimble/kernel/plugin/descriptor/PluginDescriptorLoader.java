package com.jnimble.kernel.plugin.descriptor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnimble.sdk.plugin.PluginDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads plugin descriptors from various sources (classpath, file system, JAR files).
 *
 * <p>Reads {@code jnimble-plugin.json} from plugin directories or JAR archives,
 * deserializes it with Jackson, and validates the result through
 * {@link PluginDescriptorValidator}.</p>
 *
 * 从多种来源（类路径、文件系统、JAR 文件）加载插件描述符。
 * 从插件目录或 JAR 归档中读取 jnimble-plugin.json，
 * 使用 Jackson 反序列化并通过 PluginDescriptorValidator 校验结果。
 */
public class PluginDescriptorLoader {

    /** Standard plugin descriptor file name. */
    public static final String DESCRIPTOR_FILE_NAME = "jnimble-plugin.json";

    /** Standard META-INF path for the descriptor inside JARs. */
    public static final String META_INF_DESCRIPTOR_PATH = "META-INF/" + DESCRIPTOR_FILE_NAME;

    private final ObjectMapper objectMapper;
    private final PluginDescriptorValidator validator;

    /**
     * Creates a loader with a default Jackson {@link ObjectMapper} and the given validator.
     *
     * @param validator the validator to use for descriptor validation
     */
    public PluginDescriptorLoader(PluginDescriptorValidator validator) {
        this(defaultObjectMapper(), validator);
    }

    /**
     * Creates a loader with a custom Jackson {@link ObjectMapper} and the given validator.
     *
     * @param objectMapper the Jackson ObjectMapper for deserialization
     * @param validator    the validator to use for descriptor validation
     */
    public PluginDescriptorLoader(ObjectMapper objectMapper, PluginDescriptorValidator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Loads a plugin descriptor from a plugin root directory.
     *
     * <p>Searches for the descriptor at {@code META-INF/jnimble-plugin.json} first,
     * then falls back to {@code jnimble-plugin.json} at the root.</p>
     *
     * @param pluginRoot the plugin root directory path
     * @return the loaded and validated plugin descriptor
     * @throws PluginDescriptorLoadException if the descriptor cannot be loaded
     */
    public PluginDescriptor load(Path pluginRoot) {
        if (pluginRoot == null) {
            throw new PluginDescriptorLoadException("Plugin root path is required", null);
        }
        Path metaInfDescriptor = pluginRoot.resolve(META_INF_DESCRIPTOR_PATH);
        if (Files.exists(metaInfDescriptor)) {
            return loadFile(metaInfDescriptor);
        }
        return loadFile(pluginRoot.resolve(DESCRIPTOR_FILE_NAME));
    }

    /**
     * Loads a plugin descriptor from a specific file path.
     *
     * @param descriptorPath the path to the descriptor JSON file
     * @return the loaded and validated plugin descriptor
     * @throws PluginDescriptorLoadException if the file cannot be read
     */
    public PluginDescriptor loadFile(Path descriptorPath) {
        if (descriptorPath == null) {
            throw new PluginDescriptorLoadException("Plugin descriptor path is required", null);
        }
        try (InputStream inputStream = Files.newInputStream(descriptorPath)) {
            return load(inputStream);
        } catch (PluginDescriptorValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new PluginDescriptorLoadException("Failed to load plugin descriptor from " + descriptorPath, ex);
        }
    }

    /**
     * Loads a plugin descriptor from a JAR file.
     *
     * <p>Reads the descriptor from the {@code META-INF/jnimble-plugin.json}
     * entry inside the JAR.</p>
     *
     * @param jarPath the path to the plugin JAR file
     * @return the loaded and validated plugin descriptor
     * @throws PluginDescriptorLoadException if the JAR cannot be read or lacks the descriptor
     */
    public PluginDescriptor loadJar(Path jarPath) {
        if (jarPath == null) {
            throw new PluginDescriptorLoadException("Plugin jar path is required", null);
        }
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry descriptorEntry = jarFile.getJarEntry(META_INF_DESCRIPTOR_PATH);
            if (descriptorEntry == null) {
                throw new PluginDescriptorLoadException(
                        "Plugin jar does not contain " + META_INF_DESCRIPTOR_PATH + ": " + jarPath,
                        null
                );
            }
            try (InputStream inputStream = jarFile.getInputStream(descriptorEntry)) {
                return load(inputStream);
            }
        } catch (PluginDescriptorValidationException | PluginDescriptorLoadException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new PluginDescriptorLoadException("Failed to load plugin descriptor from jar " + jarPath, ex);
        }
    }

    /**
     * Loads a plugin descriptor from an input stream.
     *
     * @param inputStream the input stream containing the descriptor JSON
     * @return the loaded and validated plugin descriptor
     * @throws PluginDescriptorLoadException if the stream cannot be read
     */
    public PluginDescriptor load(InputStream inputStream) {
        if (inputStream == null) {
            throw new PluginDescriptorLoadException("Plugin descriptor input stream is required", null);
        }
        try {
            PluginDescriptor descriptor = objectMapper.readValue(inputStream, PluginDescriptor.class);
            validator.validate(descriptor);
            return descriptor;
        } catch (PluginDescriptorValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new PluginDescriptorLoadException("Failed to read plugin descriptor", ex);
        }
    }

    /**
     * Loads a plugin descriptor from a JSON string.
     *
     * @param descriptorJson the JSON string containing the descriptor
     * @return the loaded and validated plugin descriptor
     * @throws PluginDescriptorLoadException if the JSON string cannot be parsed
     */
    public PluginDescriptor loadJson(String descriptorJson) {
        if (descriptorJson == null || descriptorJson.isBlank()) {
            throw new PluginDescriptorLoadException("Plugin descriptor json is required", null);
        }
        try {
            PluginDescriptor descriptor = objectMapper.readValue(descriptorJson, PluginDescriptor.class);
            validator.validate(descriptor);
            return descriptor;
        } catch (PluginDescriptorValidationException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new PluginDescriptorLoadException("Failed to read plugin descriptor json", ex);
        }
    }

    /**
     * Creates a default Jackson ObjectMapper configured to ignore unknown properties.
     *
     * @return a configured ObjectMapper instance
     */
    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
