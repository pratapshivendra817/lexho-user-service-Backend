package com.lexho.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.payment.dto.AdminPaymentDashboardResponse;
import com.lexho.payment.dto.AdminPaymentReconciliationResponse;
import com.lexho.payment.dto.AdminPaymentTransactionPageResponse;
import com.lexho.payment.dto.AdminRefundRequest;
import com.lexho.payment.dto.AdminRefundResponse;
import com.lexho.payment.dto.CreatePaymentOrderResponse;
import com.lexho.payment.dto.PaymentResponse;
import com.lexho.payment.entity.PaymentTransactionStatus;
import com.lexho.payment.exception.PaymentException;
import com.lexho.payment.exception.PaymentExceptionHandler;
import com.lexho.payment.service.AdminPaymentService;
import com.lexho.payment.service.PaymentService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllersTest {

    private static final LocalValidatorFactoryBean VALIDATOR = validator();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentService paymentService = mock(PaymentService.class);
    private final AdminPaymentService adminPaymentService = mock(AdminPaymentService.class);
    private final MockMvc paymentMockMvc = MockMvcBuilders
            .standaloneSetup(new PaymentController(paymentService))
            .setControllerAdvice(new PaymentExceptionHandler())
            .setValidator(VALIDATOR)
            .build();
    private final MockMvc adminMockMvc = MockMvcBuilders
            .standaloneSetup(new AdminPaymentController(adminPaymentService))
            .setControllerAdvice(new PaymentExceptionHandler())
            .setValidator(VALIDATOR)
            .build();

    @AfterAll
    static void closeValidator() {
        VALIDATOR.close();
    }

    @Test
    void createOrderReturnsSuccessResponse() throws Exception {
        when(paymentService.createOrder(7L, 42L, "idem-1"))
                .thenReturn(new CreatePaymentOrderResponse(
                        42L, "order_1", "rzp_test", 15000L, new BigDecimal("150.00"), "INR", "CREATED"));

        paymentMockMvc.perform(post("/api/v1/payments/create-order")
                        .requestAttr("userId", 7L)
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Razorpay order created"))
                .andExpect(jsonPath("$.data.bookingId").value(42))
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_1"));
    }

    @Test
    void createOrderRejectsMissingHeaderInvalidBodyAndMissingUserId() throws Exception {
        paymentMockMvc.perform(post("/api/payments/create-order")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":42}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_REQUEST"));

        paymentMockMvc.perform(post("/api/payments/create-order")
                        .requestAttr("userId", 7L)
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_REQUEST"));

        paymentMockMvc.perform(post("/api/payments/create-order")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":42}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_REQUEST"));
    }

    @Test
    void retryUsesOptionalHeaderAndReturnsCreatedOrder() throws Exception {
        when(paymentService.retryOrder(7L, 42L, null))
                .thenReturn(new CreatePaymentOrderResponse(
                        42L, "order_retry", "rzp_test", 15000L, new BigDecimal("150.00"), "INR", "CREATED"));

        paymentMockMvc.perform(post("/api/payments/retry")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Retry payment order created"))
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_retry"));
    }

    @Test
    void verifyReturnsSuccessWhenPaymentIsVerified() throws Exception {
        when(paymentService.verify(eq(7L), any()))
                .thenReturn(new PaymentResponse(
                        42L, "order_1", "pay_1", new BigDecimal("150.00"), "INR",
                        "SUCCESS", "SUCCESS", null, false, true,
                        "2026-06-19T10:00:00Z", "2026-06-19T10:05:00Z"));

        paymentMockMvc.perform(post("/api/v1/payments/verify")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId":"order_1",
                                  "razorpayPaymentId":"pay_1",
                                  "razorpaySignature":"sig_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment verified"))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));
    }

    @Test
    void verifyReturnsFailurePayloadWhenSignatureCheckFails() throws Exception {
        when(paymentService.verify(eq(7L), any()))
                .thenReturn(new PaymentResponse(
                        42L, "order_1", "pay_1", new BigDecimal("150.00"), "INR",
                        "FAILED", "FAILED", null, true, false,
                        "2026-06-19T10:00:00Z", null));

        paymentMockMvc.perform(post("/api/payments/verify")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId":"order_1",
                                  "razorpayPaymentId":"pay_1",
                                  "razorpaySignature":"bad"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.message").value("Payment signature verification failed"))
                .andExpect(jsonPath("$.data.retryPaymentAllowed").value(true));
    }

    @Test
    void verifyRejectsInvalidPayload() throws Exception {
        paymentMockMvc.perform(post("/api/payments/verify")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "razorpayOrderId":"",
                                  "razorpayPaymentId":" ",
                                  "razorpaySignature":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_REQUEST"));
    }

    @Test
    void getLatestAndOptionsReturnExpectedPayloads() throws Exception {
        when(paymentService.getLatest(7L, 42L))
                .thenReturn(new PaymentResponse(
                        42L, "order_1", "pay_1", new BigDecimal("150.00"), "INR",
                        "SUCCESS", "SUCCESS", "ACTIVE", false, true,
                        "2026-06-19T10:00:00Z", "2026-06-19T10:05:00Z"));

        paymentMockMvc.perform(get("/api/payments/42")
                        .requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE"));

        paymentMockMvc.perform(get("/api/v1/payments/options")
                        .requestAttr("userId", 7L)
                        .param("bookingId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options.length()").value(2))
                .andExpect(jsonPath("$.data.options[0].id").value("upi"))
                .andExpect(jsonPath("$.data.options[1].mode").value("CARD"));

        verify(paymentService).assertPayableBooking(7L, 42L);
    }

    @Test
    void optionsRejectsMissingBookingIdAndMapsPaymentExceptions() throws Exception {
        paymentMockMvc.perform(get("/api/payments/options")
                        .requestAttr("userId", 7L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_REQUEST"));

        when(paymentService.getLatest(7L, 404L)).thenThrow(new PaymentException("Payment transaction not found"));

        paymentMockMvc.perform(get("/api/payments/404")
                        .requestAttr("userId", 7L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Payment transaction not found"));
    }

    @Test
    void adminEndpointsReturnDashboardTransactionsReconciliationAndRefund() throws Exception {
        when(adminPaymentService.dashboard())
                .thenReturn(new AdminPaymentDashboardResponse(
                        10, 2, 5, 1, 2, new BigDecimal("550.00"), new BigDecimal("125.00")));
        when(adminPaymentService.transactions(42L, "PAID", 1, 10))
                .thenReturn(new AdminPaymentTransactionPageResponse(List.of(), 1, 10, 0, 0));
        when(adminPaymentService.reconciliation(42L))
                .thenReturn(new AdminPaymentReconciliationResponse(
                        42L, 150.0, "CONFIRMED", "SUCCESS", new BigDecimal("10.00"), List.of(), List.of()));
        when(adminPaymentService.reconcileLatest(42L))
                .thenReturn(new AdminPaymentReconciliationResponse(
                        42L, 150.0, "CONFIRMED", "SUCCESS", BigDecimal.ZERO, List.of(), List.of()));
        when(adminPaymentService.createRefund(eq(99L), eq(42L), eq("refund-1"), any(AdminRefundRequest.class)))
                .thenReturn(new AdminRefundResponse(
                        11L, 42L, "rfnd_1", new BigDecimal("10.00"), "INR", "REQUESTED",
                        "customer requested", null, LocalDateTime.of(2026, 6, 19, 12, 0), null));

        adminMockMvc.perform(get("/api/v1/admin/payments/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAttempts").value(10));

        adminMockMvc.perform(get("/api/v1/admin/payments/transactions")
                        .param("bookingId", "42")
                        .param("status", "PAID")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1));

        adminMockMvc.perform(get("/api/v1/admin/payments/bookings/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingPaymentStatus").value("SUCCESS"));

        adminMockMvc.perform(post("/api/v1/admin/payments/bookings/42/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment reconciled"));

        adminMockMvc.perform(post("/api/v1/admin/payments/bookings/42/refunds")
                        .requestAttr("userId", 99L)
                        .header("Idempotency-Key", "refund-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminRefundRequest(
                                new BigDecimal("10.00"), "customer requested"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.data.razorpayRefundId").value("rfnd_1"));
    }

    @Test
    void adminRefundRejectsInvalidRequestAndMapsBusinessErrors() throws Exception {
        adminMockMvc.perform(post("/api/v1/admin/payments/bookings/42/refunds")
                        .requestAttr("userId", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminRefundRequest(
                                new BigDecimal("0.00"), ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_REQUEST"));

        when(adminPaymentService.reconcileLatest(42L)).thenThrow(new PaymentException("Booking not found"));

        adminMockMvc.perform(post("/api/v1/admin/payments/bookings/42/reconcile"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Booking not found"));
    }

    @Test
    void adminTransactionsPassPaginationAndStatusFilters() throws Exception {
        when(adminPaymentService.transactions(null, null, 0, 20))
                .thenReturn(new AdminPaymentTransactionPageResponse(List.of(), 0, 20, 0, 0));

        adminMockMvc.perform(get("/api/v1/admin/payments/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.message").value("Payment transactions fetched"));

        verify(adminPaymentService).transactions(null, null, 0, 20);
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}
