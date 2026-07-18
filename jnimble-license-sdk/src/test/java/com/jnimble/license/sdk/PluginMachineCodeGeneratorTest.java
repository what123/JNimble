package com.jnimble.license.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PluginMachineCodeGeneratorTest {

    private final PluginMachineCodeGenerator generator = new PluginMachineCodeGenerator();

    @Test
    void generatesStableCodeFromCanonicalDomainAndProductCode() {
        MachineCode first = generator.generate("https://pos.Example.COM", "CRM");
        MachineCode second = generator.generate("shop.example.com.", "crm");

        assertEquals(first, second);
        assertEquals("crm", generator.parse(first.value()).productCode());
    }

    @Test
    void understandsMultiPartPublicSuffixes() {
        assertEquals(
                generator.generate("pos.example.co.uk", "crm"),
                generator.generate("shop.example.co.uk", "crm")
        );
    }

    @Test
    void rejectsChangedMachineCode() {
        String value = generator.generate("example.com", "crm").value();
        char replacement = value.endsWith("0") ? '1' : '0';
        String changed = value.substring(0, value.length() - 1) + replacement;

        assertThrows(IllegalArgumentException.class, () -> generator.parse(changed));
    }
}
