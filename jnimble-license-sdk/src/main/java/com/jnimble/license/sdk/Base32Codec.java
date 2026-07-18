package com.jnimble.license.sdk;

import java.io.ByteArrayOutputStream;

final class Base32Codec {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private Base32Codec() {
    }

    static String encode(byte[] input) {
        StringBuilder result = new StringBuilder((input.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : input) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1f]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(ALPHABET[(buffer << (5 - bitsLeft)) & 0x1f]);
        }
        return result.toString();
    }

    static byte[] decode(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(input.length() * 5 / 8);
        int buffer = 0;
        int bitsLeft = 0;
        for (int index = 0; index < input.length(); index++) {
            int value = decodeValue(input.charAt(index));
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        return output.toByteArray();
    }

    private static int decodeValue(char character) {
        char normalized = Character.toUpperCase(character);
        if (normalized >= 'A' && normalized <= 'Z') {
            return normalized - 'A';
        }
        if (normalized >= '2' && normalized <= '7') {
            return normalized - '2' + 26;
        }
        throw new IllegalArgumentException("Invalid base32 character: " + character);
    }
}
