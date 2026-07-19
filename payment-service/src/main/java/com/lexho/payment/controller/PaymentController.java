package com.lexho.payment.controller;

import com.lexho.payment.dto.CreatePaymentOrderRequest;
import com.lexho.payment.dto.CreatePaymentOrderResponse;
import com.lexho.payment.dto.PaymentApiResponse;
import com.lexho.payment.dto.PaymentResponse;
import com.lexho.payment.dto.VerifyPaymentRequest;
import com.lexho.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public PaymentApiResponse<CreatePaymentOrderResponse> createOrder(
            @RequestAttribute("userId") Long customerId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        return PaymentApiResponse.success(
                "Razorpay order created",
                paymentService.createOrder(customerId, request.bookingId(), idempotencyKey)
        );
    }

    @PostMapping("/retry")
    public PaymentApiResponse<CreatePaymentOrderResponse> retry(
            @RequestAttribute("userId") Long customerId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        return PaymentApiResponse.success(
                "Retry payment order created",
                paymentService.retryOrder(customerId, request.bookingId(), idempotencyKey)
        );
    }

    @PostMapping("/verify")
    public PaymentApiResponse<PaymentResponse> verify(
            @RequestAttribute("userId") Long customerId,
            @Valid @RequestBody VerifyPaymentRequest request) {
        PaymentResponse response = paymentService.verify(customerId, request);
        String message = response.verified() ? "Payment verified" : "Payment signature verification failed";
        return new PaymentApiResponse<>(response.verified(), response.verified() ? null : "PAYMENT_FAILED", message, response);
    }

    @GetMapping("/{bookingId}")
    public PaymentApiResponse<PaymentResponse> getLatest(
            @RequestAttribute("userId") Long customerId,
            @PathVariable Long bookingId) {
        return PaymentApiResponse.success("Payment fetched", paymentService.getLatest(customerId, bookingId));
    }

    @GetMapping("/options")
    public PaymentApiResponse<PaymentOptionsResponse> options(@RequestAttribute("userId") Long customerId,
                                                              @RequestParam @jakarta.validation.constraints.NotNull Long bookingId) {
        paymentService.assertPayableBooking(customerId, bookingId);
        return PaymentApiResponse.success("Payment options fetched", new PaymentOptionsResponse(List.of(
                new PaymentOption("upi", "UPI", "UPI", "Pay using any UPI app"),
                new PaymentOption("card", "CARD", "Card", "Credit and debit cards")
        )));
    }

    public record PaymentOptionsResponse(List<PaymentOption> options) {
        public PaymentOptionsResponse {
            options = List.copyOf(options);
        }
    }

    public record PaymentOption(String id, String mode, String label, String description) {
    }
}
