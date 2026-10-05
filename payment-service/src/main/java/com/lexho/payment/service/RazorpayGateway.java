package com.lexho.payment.service;

import com.lexho.payment.config.RazorpayProperties;
import com.lexho.payment.exception.PaymentException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class RazorpayGateway {

    private static final String AMOUNT_KEY = "amount";

    private final RazorpayProperties properties;

    public RazorpayGateway(RazorpayProperties properties) {
        this.properties = properties;
    }

    public RazorpayOrder createOrder(Long bookingId, BigDecimal amount) {
        requireCredentials();
        try {
            JSONObject request = new JSONObject();
            request.put(AMOUNT_KEY, toPaise(amount));
            request.put("currency", properties.getCurrency());
            request.put("receipt", "lexho_booking_" + bookingId);
            request.put("notes", new JSONObject(Map.of("bookingId", String.valueOf(bookingId))));
            com.razorpay.Order order = client().orders.create(request);
            return new RazorpayOrder(
                    toText(order.get("id")),
                    toLong(order.get(AMOUNT_KEY)),
                    toText(order.get("currency"))
            );
        } catch (RazorpayException exception) {
            throw new PaymentException("Unable to create Razorpay order", exception);
        }
    }

    public RazorpayPayment fetchPayment(String paymentId) {
        requireCredentials();
        try {
            com.razorpay.Payment payment = client().payments.fetch(paymentId);
            return new RazorpayPayment(
                    toText(payment.get("id")),
                    toText(payment.get("order_id")),
                    toLong(payment.get(AMOUNT_KEY)),
                    toText(payment.get("status"))
            );
        } catch (RazorpayException exception) {
            throw new PaymentException("Unable to fetch Razorpay payment", exception);
        }
    }

    public RazorpayRefund createRefund(String paymentId, BigDecimal amount, Long bookingId) {
        requireCredentials();
        try {
            JSONObject request = new JSONObject();
            request.put(AMOUNT_KEY, toPaise(amount));
            request.put("notes", new JSONObject(Map.of("bookingId", String.valueOf(bookingId))));
            com.razorpay.Refund refund = client().payments.refund(paymentId, request);
            return new RazorpayRefund(
                    toText(refund.get("id")),
                    toText(refund.get("payment_id")),
                    toLong(refund.get(AMOUNT_KEY)),
                    toText(refund.get("status"))
            );
        } catch (RazorpayException exception) {
            throw new PaymentException("Unable to create Razorpay refund", exception);
        }
    }

    public long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }

    private RazorpayClient client() throws RazorpayException {
        return new RazorpayClient(properties.getKeyId(), properties.getKeySecret());
    }

    private void requireCredentials() {
        if (isBlank(properties.getKeyId()) || isBlank(properties.getKeySecret())) {
            throw new PaymentException("Razorpay credentials are not configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String toText(Object value) {
        return value == null ? null : value.toString();
    }

    public record RazorpayOrder(String orderId, Long amountInPaise, String currency) {
    }

    public record RazorpayPayment(String paymentId, String orderId, Long amountInPaise, String status) {
        public boolean isCaptured() {
            return "captured".equalsIgnoreCase(status);
        }
    }

    public record RazorpayRefund(String refundId, String paymentId, Long amountInPaise, String status) {
        public boolean isProcessed() {
            return "processed".equalsIgnoreCase(status);
        }
    }
}
