package com.lexho.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStatusTest {

    @Test
    void shouldIdentifySuccessAndFailureStates() {
        assertTrue(PaymentStatus.SUCCESS.isSuccess());
        assertFalse(PaymentStatus.SUCCESS.isFailed());
        assertTrue(PaymentStatus.FAILED.isFailed());
        assertFalse(PaymentStatus.PENDING.isSuccess());
    }
}
