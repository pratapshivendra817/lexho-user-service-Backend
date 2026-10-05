package com.lexho.user_service.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.coupon.dto.CouponPageResponse;
import com.lexho.user_service.coupon.dto.CouponResponse;
import com.lexho.user_service.coupon.dto.CouponValidationRequest;
import com.lexho.user_service.coupon.dto.CouponValidationResponse;
import com.lexho.user_service.coupon.enums.CouponDiscountType;
import com.lexho.user_service.coupon.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerCouponControllerTest {

    private final CouponService couponService = mock(CouponService.class);
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerCouponController(couponService)).build();
    }

    @Test
    void myRewardsShouldReturnAssignedRewardCouponsForCustomerApp() throws Exception {
        CouponResponse coupon = new CouponResponse(
                501L,
                "REFABC1234",
                null,
                null,
                CouponDiscountType.FIXED,
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                1,
                1,
                null,
                0,
                null,
                null,
                true,
                true,
                false,
                null,
                null,
                null,
                7L,
                "REFERRAL_REWARD",
                "referral-reward-101",
                LocalDateTime.of(2026, 8, 29, 10, 0),
                LocalDateTime.of(2026, 8, 29, 10, 0)
        );
        when(couponService.listMyRewardCoupons(7L, 1, 10))
                .thenReturn(new CouponPageResponse(List.of(coupon), 1, 1, 1, 10));

        mockMvc.perform(get("/api/coupons/my-rewards")
                        .requestAttr("userId", 7L)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reward coupons fetched"))
                .andExpect(jsonPath("$.data.content[0].code").value("REFABC1234"))
                .andExpect(jsonPath("$.data.content[0].assignedUserId").value(7))
                .andExpect(jsonPath("$.data.content[0].sourceType").value("REFERRAL_REWARD"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(couponService).listMyRewardCoupons(7L, 1, 10);
    }

    @Test
    void availableShouldReturnAdminCreatedCouponsForCartCard() throws Exception {
        CouponResponse coupon = new CouponResponse(
                601L,
                "CAMPAIGN50",
                "Campaign 50",
                "Save on your cart",
                CouponDiscountType.FIXED,
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                new BigDecimal("300.00"),
                100,
                1,
                null,
                0,
                null,
                null,
                true,
                true,
                false,
                11L,
                null,
                "INSTANT",
                null,
                null,
                null,
                LocalDateTime.of(2026, 9, 13, 10, 0),
                LocalDateTime.of(2026, 9, 13, 10, 0)
        );
        when(couponService.listAvailableCoupons(7L, 0, 20,
                new BigDecimal("499.00"), 11L, 3L, "INSTANT"))
                .thenReturn(new CouponPageResponse(List.of(coupon), 1, 1, 0, 20));

        mockMvc.perform(get("/api/coupons/available")
                        .requestAttr("userId", 7L)
                        .param("amount", "499.00")
                        .param("planId", "11")
                        .param("categoryId", "3")
                        .param("bookingType", "INSTANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Available coupons fetched"))
                .andExpect(jsonPath("$.data.content[0].code").value("CAMPAIGN50"))
                .andExpect(jsonPath("$.data.content[0].customerVisible").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(couponService).listAvailableCoupons(7L, 0, 20,
                new BigDecimal("499.00"), 11L, 3L, "INSTANT");
    }

    @Test
    void validateShouldExposeDiscountEnvelopeForCustomerApp() throws Exception {
        CouponValidationRequest request = new CouponValidationRequest(
                "FIRSTONLY",
                11L,
                new BigDecimal("499.00"),
                null,
                "INSTANT"
        );
        when(couponService.validate(eq(7L), eq(request))).thenReturn(new CouponValidationResponse(
                true,
                "FIRSTONLY",
                new BigDecimal("499.00"),
                new BigDecimal("50.00"),
                new BigDecimal("449.00")
        ));

        mockMvc.perform(post("/api/coupons/validate")
                        .requestAttr("userId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Coupon validated"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.couponCode").value("FIRSTONLY"))
                .andExpect(jsonPath("$.data.discountAmount").value(50.00))
                .andExpect(jsonPath("$.data.finalAmount").value(449.00));

        verify(couponService).validate(eq(7L), eq(request));
    }
}
