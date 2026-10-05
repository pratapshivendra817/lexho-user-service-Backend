package com.lexho.user_service.lexho.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record RefreshTokenApiRequest(@NotBlank String refreshToken, String deviceId) {
    }

    public record AuthUserDetails(
            Long id,
            String name,
            String mobile,
            String email,
            String avatar,
            String role
    ) {
        @JsonProperty("userId")
        public Long userId() {
            return id;
        }

        @JsonProperty("partnerId")
        public Long partnerId() {
            return isPartner() ? id : null;
        }

        @JsonProperty("providerId")
        public Long providerId() {
            return isPartner() ? id : null;
        }

        private boolean isPartner() {
            return "PARTNER".equalsIgnoreCase(role);
        }
    }

    public record AuthVerifyResponse(String token, String refreshToken, AuthUserDetails user) {
        @JsonProperty("accessToken")
        public String accessToken() {
            return token;
        }

        @JsonProperty("userId")
        public Long userId() {
            return user == null ? null : user.id();
        }

        @JsonProperty("partnerId")
        public Long partnerId() {
            return user == null ? null : user.partnerId();
        }

        @JsonProperty("providerId")
        public Long providerId() {
            return user == null ? null : user.providerId();
        }

        @JsonProperty("role")
        public String role() {
            return user == null ? null : user.role();
        }
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
            @NotBlank @Size(max = 64) String addressId,
            @Pattern(regexp = "INSTANT|SCHEDULED", message = "bookingType must be INSTANT or SCHEDULED")
            String bookingType,
            String date,
            String slot,
            String note,
            @JsonAlias({"lat"})
            Double latitude,
            @JsonAlias({"lng", "lon"})
            Double longitude,
              Double addressLatitude,
              Double addressLongitude,
              CoordinatePair coordinates,
              CoordinatePair addressCoordinates,
              @JsonAlias({"totalAmount", "payableAmount", "expectedAmount"})
              BigDecimal expectedTotal,
              @Size(max = 40) String couponCode
      ) {
        public BookingCreateRequest(
                  Long serviceId,
                  Long addressId,
                  String bookingType,
                  String date,
                  String slot,
                  String note
          ) {
              this(serviceId, addressId == null ? null : String.valueOf(addressId), bookingType, date, slot, note,
                      null, null, null, null, null, null, null, null);
          }
      }

    public record CoordinatePair(
            @JsonAlias({"lat"})
            Double latitude,
            @JsonAlias({"lng", "lon"})
            Double longitude
    ) {
    }

    public record SlotValidationDebugRequest(
            String date,
            String slot
    ) {
    }

    public record RescheduleRequest(@NotBlank String date) {
    }

    public record RescheduleSlotRequest(
            @NotBlank String date,
            @NotBlank String slot
    ) {
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

    public record SupportCreateRequest(
            @NotNull @Size(max = 200) String subject,
            @NotNull @Size(max = 2000) String message,
            String categoryId,
            String bookingId,
            @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "priority must be LOW, MEDIUM, HIGH or URGENT")
            String priority,
            List<String> attachmentUrls
    ) {
    }

    public record SupportReplyRequest(
            @NotNull @Size(max = 2000) String message,
            List<String> attachmentUrls
    ) {
    }

    public record SupportStatusUpdateRequest(
            @NotNull
            @Pattern(regexp = "OPEN|CLOSED", message = "status must be OPEN or CLOSED")
            String status
    ) {
    }
}
