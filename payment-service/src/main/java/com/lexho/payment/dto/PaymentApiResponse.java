package com.lexho.payment.dto;

public record PaymentApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> PaymentApiResponse<T> success(String message, T data) {
        return new PaymentApiResponse<>(true, null, message, data);
    }

    public static <T> PaymentApiResponse<T> failure(String message) {
        return new PaymentApiResponse<>(false, "PAYMENT_FAILED", message, null);
    }

    public static <T> PaymentApiResponse<T> failure(String code, String message) {
        return new PaymentApiResponse<>(false, code, message, null);
    }
}
