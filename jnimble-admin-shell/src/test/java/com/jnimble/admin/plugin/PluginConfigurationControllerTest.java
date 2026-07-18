package com.jnimble.admin.plugin;

import com.jnimble.admin.audit.AdminAuditRecorder;
import com.jnimble.kernel.plugin.PluginRuntimeService;
import com.jnimble.kernel.plugin.PluginRuntimeSnapshot;
import com.jnimble.platform.auth.ControllerAuthorization;
import com.jnimble.sdk.plugin.PluginConfigurationDescriptor;
import com.jnimble.sdk.plugin.PluginConfigurationField;
import com.jnimble.sdk.plugin.PluginConfigurationFieldType;
import com.jnimble.sdk.plugin.PluginDescriptor;
import com.jnimble.sdk.plugin.PluginStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginConfigurationControllerTest {

    @Mock
    private PluginRuntimeService pluginRuntimeService;

    @Mock
    private PluginConfigurationService configurationService;

    @Mock
    private ControllerAuthorization authorization;

    @Mock
    private AdminAuditRecorder auditRecorder;

    @Mock
    private MessageSource messageSource;

    private PluginConfigurationController controller;
    private PluginDescriptor descriptor;

    @BeforeEach
    void setUp() {
        controller = new PluginConfigurationController(
                pluginRuntimeService, configurationService, authorization, auditRecorder, messageSource);
        descriptor = descriptor();
        PluginRuntimeSnapshot snapshot = mock(PluginRuntimeSnapshot.class);
        when(snapshot.status()).thenReturn(PluginStatus.ENABLED);
        when(snapshot.descriptor()).thenReturn(descriptor);
        when(pluginRuntimeService.find("payment")).thenReturn(Optional.of(snapshot));
    }

    @Test
    void rendersGeneratedConfigurationForm() {
        when(configurationService.configurationValues(descriptor)).thenReturn(
                new PluginConfigurationService.ConfigurationValues(
                        Map.of("merchantId", "merchant-001"), Set.of("merchantId")));
        when(authorization.hasPermission(any())).thenReturn(true);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.configuration("payment", model);

        assertThat(view).isEqualTo("page/plugin/configuration");
        assertThat(model.get("pluginId")).isEqualTo("payment");
        assertThat(model.get("configurationFields")).asList().hasSize(1);
        verify(authorization).requirePermission(any());
    }

    @Test
    void savesOnlyDeclaredConfigurationFields() {
        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("field.merchantId", "merchant-002");
        parameters.add("field.undeclared", "ignored");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Principal principal = () -> "admin";

        String target = controller.save("payment", parameters, principal, redirect);

        assertThat(target).isEqualTo("redirect:/admin/plugin-configurations/payment");
        verify(configurationService).save(
                eq(descriptor), eq(Map.of("merchantId", "merchant-002")), eq("admin"));
        verify(auditRecorder).success(any(), eq("plugin"), eq("payment"), any());
    }

    private PluginDescriptor descriptor() {
        PluginConfigurationField field = new PluginConfigurationField(
                "merchantId", "Merchant ID", null, null, null, null, null,
                PluginConfigurationFieldType.TEXT, false, null, null
        );
        return new PluginDescriptor(
                "1.0", "payment", "Payment", null, null, null,
                "1.0.0", "1.0", null, null, "example.PaymentBoot", null,
                null, null, null,
                new PluginConfigurationDescriptor(
                        "Payment Settings", null, null, null, List.of(field)),
                List.of(), null
        );
    }
}
