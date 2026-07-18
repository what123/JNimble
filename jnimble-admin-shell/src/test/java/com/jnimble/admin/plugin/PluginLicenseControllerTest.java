package com.jnimble.admin.plugin;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.license.core.PluginLicenseService;
import com.jnimble.license.core.PluginLicenseView;
import com.jnimble.license.sdk.LicenseStatus;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.sdk.plugin.PluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginLicenseControllerTest {

    @Mock
    private PluginRuntimeService pluginRuntimeService;

    @Mock
    private PluginLicenseService licenseService;

    @Mock
    private ControllerAuthorization authorization;

    @Mock
    private AdminAuditRecorder auditRecorder;

    @Mock
    private MessageSource messageSource;

    private PluginLicenseController controller;
    private PluginDescriptor descriptor;

    @BeforeEach
    void setUp() {
        controller = new PluginLicenseController(
                pluginRuntimeService,
                licenseService,
                authorization,
                auditRecorder,
                messageSource);
        descriptor = mock(PluginDescriptor.class);
        PluginRuntimeSnapshot snapshot = mock(PluginRuntimeSnapshot.class);
        when(snapshot.descriptor()).thenReturn(descriptor);
        when(pluginRuntimeService.find("crm")).thenReturn(Optional.of(snapshot));
        lenient().when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
    }

    @Test
    void activateResultReturnsSuccessAndExpiryForValidLicense() {
        Instant expiresAt = Instant.parse("2030-01-02T03:04:05Z");
        when(licenseService.activate(eq(descriptor), eq("token"), anyString()))
                .thenReturn(new PluginLicenseView(
                        true,
                        LicenseStatus.VALID,
                        "machine",
                        expiresAt,
                        null,
                        null));

        var response = controller.activateResult("crm", "token");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().status()).isEqualTo("VALID");
        assertThat(response.getBody().expiresAt()).isNotBlank().isNotEqualTo("-");
        verify(auditRecorder).success(anyString(), eq("plugin"), eq("crm"), anyString());
    }

    @Test
    void activateResultReturnsValidationErrorForInvalidLicense() {
        when(licenseService.activate(eq(descriptor), eq("bad-token"), anyString()))
                .thenReturn(new PluginLicenseView(
                        true,
                        LicenseStatus.INVALID,
                        "machine",
                        null,
                        "INVALID_SIGNATURE",
                        null));

        var response = controller.activateResult("crm", "bad-token");

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().status()).isEqualTo("INVALID");
        assertThat(response.getBody().expiresAt()).isEqualTo("-");
        assertThat(response.getBody().message()).contains("INVALID");
        verify(auditRecorder).failure(anyString(), eq("plugin"), eq("crm"), anyString());
    }

    @Test
    void activateResultReturnsConcreteServiceError() {
        when(licenseService.activate(eq(descriptor), eq("bad-token"), anyString()))
                .thenThrow(new IllegalArgumentException("License token is malformed"));

        var response = controller.activateResult("crm", "bad-token");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("License token is malformed");
    }
}
