package com.jnimble.admin.plugin;

import com.jnimble.kernel.resource.PluginAssetRegistry;
import com.jnimble.kernel.resource.RegisteredPluginAsset;
import com.jnimble.kernel.plugin.PluginClassLoaderRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Spring MVC controller that serves static assets registered by plugins.
 * Handles requests to the {@code /assets/plugins/{pluginId}/**} path pattern.
 */
@Controller
public class PluginAssetController {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    private final PluginAssetRegistry assetRegistry;
    private final PluginClassLoaderRegistry classLoaderRegistry;

    public PluginAssetController(PluginAssetRegistry assetRegistry) {
        this(assetRegistry, PluginClassLoaderRegistry.inMemory());
    }

    @Autowired
    public PluginAssetController(
            PluginAssetRegistry assetRegistry,
            PluginClassLoaderRegistry classLoaderRegistry
    ) {
        this.assetRegistry = assetRegistry;
        this.classLoaderRegistry = classLoaderRegistry;
    }

    /**
     * Serves a plugin asset by its request path.
     *
     * @param pluginId the plugin ID from the URL path
     * @param request  the HTTP request containing the asset path
     * @return the asset resource with appropriate cache headers
     * @throws ResponseStatusException if the asset is not found or inaccessible
     */
    @GetMapping("/assets/plugins/{pluginId}/**")
    public ResponseEntity<Resource> servePluginAsset(
            @PathVariable String pluginId,
            HttpServletRequest request
    ) {
        String requestPath = requestPath(request);
        RegisteredPluginAsset asset = assetRegistry.find(requestPath)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plugin asset not found: " + requestPath));

        if (!asset.pluginId().equals(pluginId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin asset not found: " + requestPath);
        }
        if (!asset.pluginEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Plugin asset is disabled: " + requestPath);
        }

        Resource resource = classpathResource(
                pluginId,
                asset,
                relativeAssetPath(requestPath, asset.fullRequestPath()));
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin asset resource not found: " + requestPath);
        }

        return ResponseEntity.ok()
                .cacheControl(cacheControl(asset))
                .header(X_CONTENT_TYPE_OPTIONS, "nosniff")
                .contentType(MediaTypeFactory.getMediaType(resource)
                        .orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
    }

    private Resource classpathResource(
            String pluginId,
            RegisteredPluginAsset asset,
            String relativePath
    ) {
        String location = asset.definition().resourceLocation();
        if (location == null || location.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Plugin asset location is blank");
        }
        String normalizedLocation = location.trim();
        if (!normalizedLocation.startsWith(CLASSPATH_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Only classpath plugin asset locations are supported");
        }

        String basePath = normalizeClasspathBase(normalizedLocation.substring(CLASSPATH_PREFIX.length()));
        String normalizedRelativePath = normalizeRelativePath(relativePath);
        ClassLoader pluginClassLoader = classLoaderRegistry.find(pluginId)
                .orElseGet(PluginAssetController.class::getClassLoader);
        return new ClassPathResource(basePath + normalizedRelativePath, pluginClassLoader);
    }

    private CacheControl cacheControl(RegisteredPluginAsset asset) {
        if (asset.definition().cacheable()) {
            return CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic();
        }
        return CacheControl.noStore();
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }

    private String relativeAssetPath(String requestPath, String mappingPath) {
        if (requestPath.equals(mappingPath)) {
            return "";
        }
        String prefix = mappingPath.endsWith("/") ? mappingPath : mappingPath + "/";
        if (!requestPath.startsWith(prefix)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin asset not found: " + requestPath);
        }
        return requestPath.substring(prefix.length());
    }

    private String normalizeClasspathBase(String basePath) {
        String normalized = normalizePath(basePath);
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private String normalizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }
        return normalizePath(relativePath);
    }

    private String normalizePath(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("../") || normalized.equals("..") || normalized.contains("/..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid plugin asset path");
        }
        return normalized;
    }
}
