package com.lexho.payment.service;

import com.lexho.payment.exception.PaymentException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class PaymentSignatureVerifier {

    public boolean verifyPayment(String orderId, String paymentId, String signature, String secret) {
        return verify(orderId + "|" + paymentId, signature, secret);
    }

    public boolean verifyWebhook(String rawBody, String signature, String secret) {
        return verify(rawBody, signature, secret);
    }

    private boolean verify(String payload, String signature, String secret) {
        if (isBlank(payload) || isBlank(signature) || isBlank(secret)) {
            return false;
        }
        byte[] expected = hmacSha256(payload, secret);
        byte[] provided;
        try {
            provided = HexFormat.of().parseHex(signature);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return MessageDigest.isEqual(expected, provided);
    }

    private byte[] hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new PaymentException("HMAC SHA-256 is not available", exception);
        } catch (java.security.InvalidKeyException exception) {
            throw new PaymentException("Payment signature key is invalid", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
