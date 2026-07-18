package com.jnimble.license.core;

import com.jnimble.license.sdk.LicenseStatus;
import com.jnimble.license.sdk.PluginLicense;
import com.jnimble.license.sdk.PluginLicenseBackend;
import com.jnimble.license.sdk.PluginLicenseException;
import com.jnimble.license.sdk.PluginLicenseResult;
import com.jnimble.license.sdk.PluginLicenseVerifier;
import com.jnimble.license.sdk.PluginLicenseVerifierRegistry;
import com.jnimble.license.sdk.PluginMachineCodeGenerator;
import com.jnimble.sdk.hook.RegistrationHandle;
import com.jnimble.sdk.plugin.PluginDescriptor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultPluginLicenseService implements
        PluginLicenseService,
        PluginLicenseBackend,
        PluginLicenseVerifierRegistry {

    private final PluginLicenseStore store;
    private final PluginLicenseProperties properties;
    private final PluginMachineCodeGenerator machineCodeGenerator = new PluginMachineCodeGenerator();
    private final Map<String, PluginLicenseVerifier> verifiers = new ConcurrentHashMap<>();

    public DefaultPluginLicenseService(PluginLicenseStore store, PluginLicenseProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    @PostConstruct
    void installBackend() {
        PluginLicense.installBackend(this);
    }

    @Override
    public PluginLicenseView view(PluginDescriptor descriptor) {
        if (!required(descriptor)) {
            return freeView();
        }
        PluginLicenseRecord record = store.find(descriptor.id()).orElse(null);
        if (record == null) {
            return missingView(descriptor);
        }
        return toView(descriptor, record);
    }

    @Override
    public PluginLicenseView activate(PluginDescriptor descriptor, String token, String operator) {
        requireLicensedDescriptor(descriptor);
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("License token is required");
        }
        Instant now = Instant.now();
        PluginLicenseRecord existing = store.find(descriptor.id()).orElse(null);
        String machineCode = machineCode(descriptor);
        PluginLicenseRecord record = new PluginLicenseRecord(
                descriptor.id(), "pending", token.trim(), sha256(token.trim()), descriptor.license().issuer(),
                "pending", descriptor.license().productCode(), machineCode, now, now, now,
                "pending", 0, LicenseStatus.UNVERIFIED, null,
                existing == null ? now : existing.createdAt(), now);
        store.save(record);
        store.event(descriptor.id(), "ACTIVATE", LicenseStatus.UNVERIFIED.name(), "operator=" + operator);
        PluginLicense.invalidate(descriptor.id());
        PluginLicenseVerifier verifier = verifiers.get(descriptor.id());
        if (verifier == null) {
            return toView(descriptor, record);
        }
        try {
            verifier.verify(true);
        } catch (PluginLicenseException ignored) {
        }
        return view(descriptor);
    }

    @Override
    public PluginLicenseView revalidate(PluginDescriptor descriptor, boolean force) {
        if (!required(descriptor)) {
            return freeView();
        }
        PluginLicenseVerifier verifier = verifiers.get(descriptor.id());
        if (verifier == null) {
            PluginLicenseRecord record = store.find(descriptor.id()).orElse(null);
            if (record == null) {
                return missingView(descriptor);
            }
            return toView(descriptor, record);
        }
        try {
            verifier.verify(force);
        } catch (PluginLicenseException ignored) {
        }
        return view(descriptor);
    }

    @Override
    public void remove(PluginDescriptor descriptor, String operator) {
        if (!required(descriptor)) {
            return;
        }
        store.delete(descriptor.id());
        store.event(descriptor.id(), "REMOVE", LicenseStatus.MISSING.name(), "operator=" + operator);
        PluginLicense.invalidate(descriptor.id());
    }

    @Override
    public LicenseStatus status(String pluginId) {
        return store.find(pluginId).map(PluginLicenseRecord::status).orElse(LicenseStatus.MISSING);
    }

    @Override
    public Optional<String> loadToken(String pluginId) {
        return store.find(pluginId).map(PluginLicenseRecord::token).filter(token -> !token.isBlank());
    }

    @Override
    public String machineCode(String pluginId, String productCode) {
        return machineCodeGenerator.generate(properties.getCanonicalDomain(), productCode).value();
    }

    @Override
    public void report(String pluginId, PluginLicenseResult result) {
        PluginLicenseRecord existing = store.find(pluginId).orElse(null);
        if (existing == null) {
            return;
        }
        Instant now = Instant.now();
        PluginLicenseRecord updated = new PluginLicenseRecord(
                existing.pluginId(), existing.licenseId(), existing.token(), existing.tokenHash(), existing.issuer(),
                existing.keyId(), existing.productCode(),
                result.machineCode() == null ? existing.machineCode() : result.machineCode(),
                existing.issuedAt(), existing.notBefore(),
                result.expiresAt() == null ? existing.expiresAt() : result.expiresAt(),
                existing.timeSnapshot(), existing.snapshotSequence(), result.status(), result.failureCode(),
                existing.createdAt(), now);
        store.save(updated);
        store.event(pluginId, "VERIFY", result.status().name(), result.failureCode());
    }

    @Override
    public RegistrationHandle register(String pluginId, PluginLicenseVerifier verifier) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("Plugin id is required");
        }
        if (verifier == null) {
            throw new IllegalArgumentException("Plugin license verifier is required");
        }
        PluginLicenseVerifier previous = verifiers.putIfAbsent(pluginId, verifier);
        if (previous != null) {
            throw new IllegalStateException("Plugin license verifier already registered: " + pluginId);
        }
        return () -> verifiers.remove(pluginId, verifier);
    }

    @Override
    public Optional<PluginLicenseVerifier> find(String pluginId) {
        return Optional.ofNullable(verifiers.get(pluginId));
    }

    private PluginLicenseView toView(PluginDescriptor descriptor, PluginLicenseRecord record) {
        return new PluginLicenseView(
                true, record.status(), machineCode(descriptor),
                record.status() == LicenseStatus.UNVERIFIED ? null : record.expiresAt(),
                record.failureCode(), purchaseUrl(machineCode(descriptor)));
    }

    private PluginLicenseView missingView(PluginDescriptor descriptor) {
        String machineCode = machineCode(descriptor);
        return new PluginLicenseView(
                true, LicenseStatus.MISSING, machineCode, null, null, purchaseUrl(machineCode));
    }

    private PluginLicenseView freeView() {
        return new PluginLicenseView(false, LicenseStatus.NOT_REQUIRED, null, null, null, null);
    }

    private String machineCode(PluginDescriptor descriptor) {
        return machineCode(descriptor.id(), descriptor.license().productCode());
    }

    private String purchaseUrl(String machineCode) {
        String separator = properties.getIssuerUrl().contains("?") ? "&" : "?";
        return properties.getIssuerUrl() + separator + "machineCode="
                + java.net.URLEncoder.encode(machineCode, StandardCharsets.UTF_8);
    }

    private boolean required(PluginDescriptor descriptor) {
        return descriptor != null && descriptor.license() != null && descriptor.license().required();
    }

    private void requireLicensedDescriptor(PluginDescriptor descriptor) {
        if (!required(descriptor)) {
            throw new IllegalArgumentException("Plugin does not require a license: " + descriptor.id());
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
