package com.lexho.user_service.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ServicePinHasher {

    private ServicePinHasher() {
    }

    public static String expectedPin(Long orderId, String purpose, String nonce, String secret) {
        String input = orderId + ":" + purpose + ":" + nonce + ":" + secret;
        byte[] digest = sha256(input);
        int value = ((digest[0] & 0xFF) << 24)
                | ((digest[1] & 0xFF) << 16)
                | ((digest[2] & 0xFF) << 8)
                | (digest[3] & 0xFF);
        return String.format("%04d", Math.floorMod(value, 10_000));
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to generate service PIN");
        }
    }
}
