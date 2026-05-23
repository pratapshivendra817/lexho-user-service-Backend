package com.lexho.user_service.lexho.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class LexhoDtos {

    private LexhoDtos() {
    }

    public record OtpSendRequest(
            @Pattern(regexp = "^[6-9][0-9]{9}$") String mobile,
            @Pattern(regexp = "CUSTOMER|PARTNER") String role
    ) {
    }

    public record OtpVerifyRequest(
            @Pattern(regexp = "^[6-9][0-9]{9}$") String mobile,
            @Pattern(regexp = "^[0-9]{6}$") String otp,
            @Pattern(regexp = "CUSTOMER|PARTNER") String role
    ) {
    }

    public record RefreshTokenRequest(String refreshToken) {
    }

    public record RefreshTokenApiRequest(String refreshToken, String deviceId) {
    }

    public record AuthUserDetails(
            Long id,
            String name,
            String mobile,
            String email,
            String avatar,
            String role
    ) {
    }

    public record AuthVerifyResponse(String token, String refreshToken, AuthUserDetails user) {
    }

    public record UserProfileResponse(Long id, String mobile, String name, String email, String avatar) {
    }

    public record UserProfileUpdateRequest(
            @Size(max = 120) String name,
            @Email @Size(max = 150) String email,
            @Pattern(regexp = "MALE|FEMALE|OTHER") String gender
    ) {
    }

    public record AddressUpsertRequest(
            Long id,
            String label,
            String line1,
            String city,
            Double latitude,
            Double longitude
    ) {
    }

    public record AddressResponse(
            Long id,
            String label,
            String line1,
            String city,
            boolean isDefault
    ) {
    }

    public record IdRequest(@NotNull @Positive Long id) {
    }

    public record HomeDashboardResponse(
            String greeting,
            String location,
            List<java.util.Map<String, Object>> recentBookings
    ) {
    }

    public record CartAddRequest(@NotNull @Positive Long serviceId, @NotNull @Positive Integer quantity) {
    }

    public record FavoriteAddRequest(@NotNull @Positive Long serviceId) {
    }

    public record CartUpdateRequest(@NotNull @Positive Long id, @NotNull @Positive Integer quantity) {
    }

    public record BookingCreateRequest(
            @NotNull @Positive Long serviceId,
            @NotNull @Positive Long addressId,
            @Pattern(regexp = "INSTANT|SCHEDULED", message = "bookingType must be INSTANT or SCHEDULED")
            String bookingType,
            String date,
            String slot,
            String note
    ) {
    }

    public record SlotValidationDebugRequest(
            String date,
            String slot
    ) {
    }

    public record RescheduleRequest(String date) {
    }

    public record PaymentOptionsRequest(String bookingId, Long cartId, BigDecimal amount) {
    }

    public record PaymentInitiateRequest(String mode, String bookingId, BigDecimal amount) {
    }

    public record PaymentRefundRequest(@NotNull String bookingId, @NotNull @Positive BigDecimal amount) {
    }

    public record CouponApplyRequest(String code) {
    }

    public record ReviewAddRequest(
            @NotNull String serviceId,
            @NotNull @Positive Integer rating,
            String comment,
            List<String> imageUrls
    ) {
    }

    public record ReferralApplyRequest(String code) {
    }

    public record SubscriptionCreateRequest(String planId) {
    }

    public record SupportCreateRequest(
            @NotNull @Size(max = 200) String subject,
            @NotNull @Size(max = 2000) String message,
            String categoryId,
            String bookingId,
            @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "priority must be LOW, MEDIUM, HIGH or URGENT")
            String priority
    ) {
    }
}
