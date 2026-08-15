package com.lexho.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenDigestsTest {

    @Test
    void hashesTokensDeterministically() {
        assertThat(TokenDigests.sha256Hex(" token "))
                .isEqualTo(TokenDigests.sha256Hex("token"))
                .hasSize(64)
                .matches("[0-9a-f]+");
    }

    @Test
    void rejectsNullOrBlankValues() {
        assertThatThrownBy(() -> TokenDigests.sha256Hex(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() -> TokenDigests.sha256Hex("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }
}
