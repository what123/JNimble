package com.jnimble.kernel.plugin;

import com.jnimble.sdk.plugin.PluginBoot;
import com.jnimble.sdk.plugin.PluginDescriptor;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reflection-based implementation of {@link PluginBootLoader} that loads plugin boot
 * classes using Java reflection. Creates isolated classloaders for plugin artifacts.
 */
public class ReflectivePluginBootLoader implements PluginBootLoader {

    private final ClassLoader classLoader;

    /**
     * Creates a boot loader using the current thread's context class loader.
     */
    public ReflectivePluginBootLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Creates a boot loader with a specific parent class loader.
     *
     * @param classLoader the parent class loader for plugin class loading
     */
    public ReflectivePluginBootLoader(ClassLoader classLoader) {
        this.classLoader = classLoader == null ? ReflectivePluginBootLoader.class.getClassLoader() : classLoader;
    }

    @Override
    public LoadedPluginBoot load(PluginDescriptor descriptor) {
        return load(descriptor, classLoader, null);
    }

    @Override
    public LoadedPluginBoot load(PluginDescriptor descriptor, Path artifactPath) {
        return load(descriptor, artifactPath, List.of());
    }

    @Override
    public LoadedPluginBoot load(
            PluginDescriptor descriptor,
            Path artifactPath,
            List<ClassLoader> dependencyClassLoaders
    ) {
        if (artifactPath == null) {
            return load(descriptor);
        }
        Path normalizedPath = artifactPath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedPath)) {
            throw new PluginRuntimeException("Plugin artifact does not exist: " + normalizedPath);
        }
        try {
            URLClassLoader pluginClassLoader = new DependencyAwarePluginClassLoader(
                    new URL[]{normalizedPath.toUri().toURL()},
                    classLoader,
                    dependencyClassLoaders
            );
            return load(descriptor, pluginClassLoader, pluginClassLoader);
        } catch (MalformedURLException ex) {
            throw new PluginRuntimeException("Invalid plugin artifact URL: " + normalizedPath, ex);
        }
    }

    private LoadedPluginBoot load(
            PluginDescriptor descriptor,
            ClassLoader targetClassLoader,
            AutoCloseable closeable
    ) {
        if (descriptor.bootClass() == null || descriptor.bootClass().isBlank()) {
            throw new PluginRuntimeException("Plugin bootClass is required for " + descriptor.id());
        }
        try {
            Class<?> bootClass = Class.forName(descriptor.bootClass(), true, targetClassLoader);
            if (!PluginBoot.class.isAssignableFrom(bootClass)) {
                throw new PluginRuntimeException("Plugin bootClass does not implement PluginBoot: " + descriptor.bootClass());
            }
            PluginBoot boot = (PluginBoot) bootClass.getDeclaredConstructor().newInstance();
            return new ReflectiveLoadedPluginBoot(boot, targetClassLoader, closeable);
        } catch (PluginRuntimeException ex) {
            closeQuietly(closeable, ex);
            throw ex;
        } catch (ReflectiveOperationException ex) {
            closeQuietly(closeable, ex);
            throw new PluginRuntimeException("Failed to instantiate plugin bootClass " + descriptor.bootClass(), ex);
        }
    }

    private void closeQuietly(AutoCloseable closeable, Exception failure) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private record ReflectiveLoadedPluginBoot(
            PluginBoot boot,
            ClassLoader classLoader,
            AutoCloseable closeable
    ) implements LoadedPluginBoot {

        @Override
        public void close() throws Exception {
            if (closeable != null) {
                closeable.close();
            }
        }
    }

    /**
     * Plugin-first loader for plugin-owned classes/resources with explicit visibility
     * of declared plugin dependencies. Framework API packages always use the parent
     * loader so SDK types are never duplicated across plugin boundaries.
     */
    private static final class DependencyAwarePluginClassLoader extends URLClassLoader {

        private static final List<String> PARENT_FIRST_PACKAGES = List.of(
                "java.",
                "javax.",
                "jakarta.",
                "org.springframework.",
                "org.slf4j.",
                "com.fasterxml.jackson.",
                "com.jnimble.sdk.",
                "com.jnimble.kernel.",
                "com.jnimble.platform."
        );

        private final List<ClassLoader> dependencies;

        private DependencyAwarePluginClassLoader(
                URL[] urls,
                ClassLoader parent,
                List<ClassLoader> dependencies
        ) {
            super(urls, parent);
            this.dependencies = dependencies == null
                    ? List.of()
                    : dependencies.stream().filter(java.util.Objects::nonNull).distinct().toList();
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = parentFirst(name)
                            ? loadFromParent(name)
                            : loadPluginClass(name);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private Class<?> loadPluginClass(String name) throws ClassNotFoundException {
            try {
                return findClass(name);
            } catch (ClassNotFoundException ignored) {
                for (ClassLoader dependency : dependencies) {
                    try {
                        return Class.forName(name, false, dependency);
                    } catch (ClassNotFoundException dependencyMiss) {
                        // Try the next declared dependency.
                    }
                }
                return loadFromParent(name);
            }
        }

        private Class<?> loadFromParent(String name) throws ClassNotFoundException {
            ClassLoader parent = getParent();
            return parent == null ? findSystemClass(name) : parent.loadClass(name);
        }

        private boolean parentFirst(String name) {
            return PARENT_FIRST_PACKAGES.stream().anyMatch(name::startsWith);
        }

        @Override
        public URL getResource(String name) {
            URL own = findResource(name);
            if (own != null) {
                return own;
            }
            for (ClassLoader dependency : dependencies) {
                URL resource = dependency.getResource(name);
                if (resource != null) {
                    return resource;
                }
            }
            ClassLoader parent = getParent();
            return parent == null ? ClassLoader.getSystemResource(name) : parent.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            Set<URL> resources = new LinkedHashSet<>();
            resources.addAll(Collections.list(findResources(name)));
            for (ClassLoader dependency : dependencies) {
                resources.addAll(Collections.list(dependency.getResources(name)));
            }
            ClassLoader parent = getParent();
            resources.addAll(Collections.list(parent == null
                    ? ClassLoader.getSystemResources(name)
                    : parent.getResources(name)));
            return Collections.enumeration(new ArrayList<>(resources));
        }
    }
}
