package com.jnimble.license.sdk;

/** Parsed or generated plugin machine code. */
public record MachineCode(
        int version,
        String productCode,
        String value
) {
}
