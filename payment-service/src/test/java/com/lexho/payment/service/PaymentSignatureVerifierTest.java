package com.lexho.payment.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentSignatureVerifierTest {

    private final PaymentSignatureVerifier verifier = new PaymentSignatureVerifier();

    @Test
    void verifiesKnownPaymentSignature() {
        assertTrue(verifier.verifyPayment(
                "order_123",
                "pay_456",
                "18bfc0baafae8f6367711ee362f2201aaa3654274683100e5367bb9a2bd29cbe",
                "secret"
        ));
    }

    @Test
    void rejectsInvalidPaymentSignature() {
        assertFalse(verifier.verifyPayment("order_123", "pay_456", "invalid", "secret"));
    }
}
