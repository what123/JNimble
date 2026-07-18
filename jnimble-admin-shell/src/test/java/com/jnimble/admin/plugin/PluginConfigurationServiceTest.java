package com.jnimble.admin.plugin;

import com.jnimble.platform.persistence.entity.PluginConfigurationEntity;
import com.jnimble.platform.persistence.mapper.PluginConfigurationMapper;
import com.jnimble.sdk.plugin.PluginConfigurationDescriptor;
import com.jnimble.sdk.plugin.PluginConfigurationField;
import com.jnimble.sdk.plugin.PluginConfigurationFieldType;
import com.jnimble.sdk.plugin.PluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluginConfigurationServiceTest {

    @Mock
    private PluginConfigurationMapper mapper;

    @Mock
    private PluginConfigurationCrypto crypto;

    private PluginConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new PluginConfigurationService(mapper, crypto);
    }

    @Test
    void encryptsSecretValuesBeforePersistence() {
        when(crypto.encrypt("payment", "apiKey", "secret-value")).thenReturn("v1.encrypted.value");
        when(mapper.selectOne(any())).thenReturn(null);
        PluginDescriptor descriptor = descriptor(field(
                "apiKey", PluginConfigurationFieldType.SECRET, true, null));

        service.save(descriptor, Map.of("apiKey", "secret-value"), "admin");

        verify(crypto).encrypt("payment", "apiKey", "secret-value");
        verify(mapper).insert(any(PluginConfigurationEntity.class));
    }

    @Test
    void exposesDefaultsWithoutPersistingThem() {
        when(mapper.selectList(any())).thenReturn(List.of());
        PluginDescriptor descriptor = descriptor(field(
                "sandbox", PluginConfigurationFieldType.BOOLEAN, false, "true"));

        PluginConfigurationService.ConfigurationValues values = service.configurationValues(descriptor);

        assertThat(values.displayedValues()).containsEntry("sandbox", "true");
        assertThat(values.configuredKeys()).isEmpty();
    }

    @Test
    void rejectsMissingRequiredValue() {
        PluginDescriptor descriptor = descriptor(field(
                "merchantId", PluginConfigurationFieldType.TEXT, true, null));

        assertThatThrownBy(() -> service.save(descriptor, Map.of(), "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Merchant ID is required");
    }

    private PluginConfigurationField field(
            String key,
            PluginConfigurationFieldType type,
            boolean required,
            String defaultValue
    ) {
        String label = "merchantId".equals(key) ? "Merchant ID" : key;
        return new PluginConfigurationField(
                key, label, null, null, null, null, null,
                type, required, defaultValue, null
        );
    }

    private PluginDescriptor descriptor(PluginConfigurationField field) {
        return new PluginDescriptor(
                "1.0", "payment", "Payment", null, null, null,
                "1.0.0", "1.0", null, null, "example.PaymentBoot", null,
                null, null, null, null,
                new PluginConfigurationDescriptor("Settings", null, null, null, List.of(field)),
                List.of(), null
        );
    }
}
