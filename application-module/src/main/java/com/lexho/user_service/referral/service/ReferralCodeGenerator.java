package com.lexho.user_service.referral.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ReferralCodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;

    public String nextCode() {
        StringBuilder builder = new StringBuilder(5 + CODE_LENGTH);
        builder.append("LEXHO");
        for (int index = 0; index < CODE_LENGTH; index++) {
            builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}
