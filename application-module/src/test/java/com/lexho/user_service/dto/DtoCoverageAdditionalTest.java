package com.lexho.user_service.dto;

import com.lexho.provider.entity.Partner;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoCoverageAdditionalTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @CsvSource({
            "'',Password123,Email is required",
            "invalid,Password123,Email must be valid",
            "admin@lexho.com,'',Password is required",
            "admin@lexho.com,short,Password must be between 8 and 100 characters"
    })
    void adminLoginRequestValidatesFields(String email, String password, String expectedMessage) {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        Set<ConstraintViolation<AdminLoginRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(ConstraintViolation::getMessage).contains(expectedMessage);
    }

    @ParameterizedTest
    @CsvSource({
            "'',123456,Email is required",
            "invalid,123456,Email must be valid",
            "admin@lexho.com,'',OTP is required",
            "admin@lexho.com,12345,OTP must be 6 digits"
    })
    void adminVerifyOtpRequestValidatesFields(String email, String otp, String expectedMessage) {
        AdminVerifyOtpRequest request = new AdminVerifyOtpRequest();
        request.setEmail(email);
        request.setOtp(otp);

        Set<ConstraintViolation<AdminVerifyOtpRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(ConstraintViolation::getMessage).contains(expectedMessage);
    }

    @Test
    void apiResponseFactoriesAndConstructorsPreserveData() {
        ApiResponse<String> success = ApiResponse.success("done", "payload");
        ApiResponse<String> successWithToken = ApiResponse.success("done", "payload", "jwt");
        ApiResponse<String> failure = ApiResponse.failure("bad");
        ApiResponse<String> failureWithCode = ApiResponse.failure("ERR", "broken");
        ApiResponse<Integer> constructorWithCode = new ApiResponse<>(true, "OK", "message", 123);

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getMessage()).isEqualTo("done");
        assertThat(success.getData()).isEqualTo("payload");
        assertThat(success.getTimestamp()).isNotBlank();

        assertThat(successWithToken.getToken()).isEqualTo("jwt");
        assertThat(failure.isSuccess()).isFalse();
        assertThat(failure.getMessage()).isEqualTo("bad");
        assertThat(failureWithCode.getCode()).isEqualTo("ERR");
        assertThat(failureWithCode.getTimestamp()).isNotBlank();
        assertThat(constructorWithCode.getCode()).isEqualTo("OK");
        assertThat(constructorWithCode.getData()).isEqualTo(123);

        ApiResponse<String> mutable = new ApiResponse<>();
        mutable.setSuccess(true);
        mutable.setCode("C1");
        mutable.setMessage("updated");
        mutable.setData("body");
        mutable.setToken("token");
        mutable.setTimestamp("ts");

        assertThat(mutable.isSuccess()).isTrue();
        assertThat(mutable.getCode()).isEqualTo("C1");
        assertThat(mutable.getMessage()).isEqualTo("updated");
        assertThat(mutable.getData()).isEqualTo("body");
        assertThat(mutable.getToken()).isEqualTo("token");
        assertThat(mutable.getTimestamp()).isEqualTo("ts");
    }

    @Test
    void rootDtosExposeAccessorsConstructorsAndMapperLogic() {
        AdminLoginResponse loginResponse = new AdminLoginResponse("admin@lexho.com", 300);
        AdminTokenResponse tokenResponse = new AdminTokenResponse("jwt");
        CartRequest cartRequest = new CartRequest();
        CartResponseDTO cartResponse = new CartResponseDTO(1L, 2L, "Partner", "/img.png", 199.0, 2);
        CartSummaryDTO cartSummary = new CartSummaryDTO(List.of(cartResponse), 398.0);
        HomeBannerDTO homeBanner = new HomeBannerDTO();
        HomeCategoryDTO homeCategory = new HomeCategoryDTO(3L, "Cleaning", "sparkles");
        HomeServiceDTO homeService = new HomeServiceDTO();
        HomeSectionDTO homeSection = new HomeSectionDTO();
        HomeResponseDTO homeResponse = new HomeResponseDTO();
        OrderItemDTO orderItem = new OrderItemDTO();
        OrderResponseDTO orderResponse = new OrderResponseDTO();
        PartnerCreateRequestDTO partnerCreateRequest = new PartnerCreateRequestDTO();
        PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();
        SubCategoryCreateRequestDTO subCategoryCreateRequest = new SubCategoryCreateRequestDTO();
        SubCategoryResponseDTO subCategoryResponse = new SubCategoryResponseDTO(4L, "Kitchen", "icon", 5L, "Home");

        cartRequest.setPartnerId(9L);
        cartRequest.setQuantity(3);
        homeBanner.setLabel("Offer");
        homeBanner.setCtaLabel("Book");
        homeBanner.setSecondaryCtaLabel("Later");
        homeBanner.setTagline("Soon");
        homeService.setPartnerId(7L);
        homeService.setName("Cook");
        homeService.setPrice(499.0);
        homeService.setOriginalPrice(599.0);
        homeService.setRating(4.8);
        homeService.setExperience(5);
        homeService.setImage("/cook.png");
        homeSection.setTitle("Popular");
        homeSection.setTagline("Nearby");
        homeSection.setData(List.of(homeService));
        homeResponse.setUserId(11L);
        homeResponse.setGreeting("Hi");
        homeResponse.setBanners(List.of(homeBanner));
        homeResponse.setCategories(List.of(homeCategory));
        homeResponse.setSections(List.of(homeSection));
        orderItem.setPartnerId(8L);
        orderItem.setPartnerName("Team");
        orderItem.setPrice(250.0);
        orderItem.setQuantity(1);
        orderResponse.setId(6L);
        orderResponse.setTotalAmount(250.0);
        orderResponse.setStatus("CONFIRMED");
        orderResponse.setPaymentStatus("PAID");
        orderResponse.setAddress("Pune");
        orderResponse.setBookingType("INSTANT");
        orderResponse.setPromisedStartTime(LocalDateTime.parse("2026-07-17T10:15:30"));
        orderResponse.setSlaMinutes(90);
        orderResponse.setUnavailableReason("none");
        orderResponse.setItems(List.of(orderItem));
        partnerCreateRequest.setName("Cook");
        partnerCreateRequest.setCategoryId(12L);
        partnerCreateRequest.setSubCategoryId(13L);
        partnerCreateRequest.setRating(4.7);
        partnerCreateRequest.setTotalReviews(99L);
        partnerCreateRequest.setPrice(399.0);
        partnerCreateRequest.setAvailable(true);
        partnerCreateRequest.setExperience(4);
        partnerCreateRequest.setImage("/partner.png");
        partnerCreateRequest.setVerificationStatus("VERIFIED");
        placeOrderRequest.setAddress("Pune");
        placeOrderRequest.setScheduledAt(LocalDateTime.parse("2026-07-17T11:00:00"));
        placeOrderRequest.setBookingType("SCHEDULED");
        placeOrderRequest.setExpectedTotal(BigDecimal.valueOf(599));
        subCategoryCreateRequest.setName("Sub");
        subCategoryCreateRequest.setIcon("icon");
        subCategoryCreateRequest.setCategoryId(14L);

        Partner partner = new Partner();
        partner.setId(100L);
        partner.setName("Mapped");
        partner.setRating(4.5);
        partner.setPrice(299.0);
        partner.setExperience(6);
        partner.setImage("/mapped.png");
        partner.setCategoryId(20L);
        partner.setSubCategoryId(21L);
        partner.setAvailable(true);
        partner.setTotalReviews(50L);
        partner.setVerificationStatus("ACTIVE");
        PartnerResponseDTO partnerResponse = PartnerResponseDTO.from(partner);

        assertThat(loginResponse.getEmail()).isEqualTo("admin@lexho.com");
        assertThat(loginResponse.getOtpExpiresInSeconds()).isEqualTo(300);
        loginResponse.setOtpExpiresInSeconds(120);
        assertThat(loginResponse.getOtpExpiresInSeconds()).isEqualTo(120);
        assertThat(tokenResponse.getToken()).isEqualTo("jwt");
        tokenResponse.setToken("jwt-2");
        assertThat(tokenResponse.getToken()).isEqualTo("jwt-2");
        assertThat(cartRequest.getPartnerId()).isEqualTo(9L);
        assertThat(cartRequest.getQuantity()).isEqualTo(3);
        assertThat(cartSummary.getItems()).singleElement().extracting(CartResponseDTO::getPartnerName).isEqualTo("Partner");
        assertThat(cartSummary.getTotal()).isEqualTo(398.0);
        assertThat(homeResponse.getSections()).singleElement().extracting(HomeSectionDTO::getTitle).isEqualTo("Popular");
        assertThat(orderResponse.getItems()).singleElement().extracting(OrderItemDTO::getPartnerName).isEqualTo("Team");
        assertThat(partnerCreateRequest.getVerificationStatus()).isEqualTo("VERIFIED");
        assertThat(placeOrderRequest.getExpectedTotal()).isEqualByComparingTo("599");
        assertThat(subCategoryCreateRequest.getCategoryId()).isEqualTo(14L);
        assertThat(subCategoryResponse.getCategoryName()).isEqualTo("Home");
        subCategoryResponse.setCategoryName("Kitchen");
        assertThat(subCategoryResponse.getCategoryName()).isEqualTo("Kitchen");

        assertThat(partnerResponse.getId()).isEqualTo(100L);
        assertThat(partnerResponse.getName()).isEqualTo("Mapped");
        assertThat(partnerResponse.getRating()).isEqualTo(4.5);
        assertThat(partnerResponse.getPrice()).isEqualTo(299.0);
        assertThat(partnerResponse.getExperience()).isEqualTo(6);
        assertThat(partnerResponse.getImage()).isEqualTo("/mapped.png");
        assertThat(partnerResponse.getCategoryId()).isEqualTo(20L);
        assertThat(partnerResponse.getSubCategoryId()).isEqualTo(21L);
        assertThat(partnerResponse.getAvailable()).isTrue();
        assertThat(partnerResponse.getTotalReviews()).isEqualTo(50L);
        assertThat(partnerResponse.getVerificationStatus()).isEqualTo("ACTIVE");
    }
}
