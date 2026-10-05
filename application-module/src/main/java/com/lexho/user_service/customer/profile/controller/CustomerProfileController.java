package com.lexho.user_service.customer.profile.controller;

import com.lexho.user_service.customer.profile.dto.CustomerProfileAddressResponse;
import com.lexho.user_service.customer.profile.dto.CustomerProfileContentResponse;
import com.lexho.user_service.customer.profile.dto.CustomerEmailUpdateRequest;
import com.lexho.user_service.customer.profile.dto.CustomerProfileResponse;
import com.lexho.user_service.customer.profile.dto.CustomerProfileUpdateRequest;
import com.lexho.user_service.customer.profile.dto.UpdateProfileRequest;
import com.lexho.user_service.customer.profile.service.CustomerProfileService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/customer", "/api/v1/customer"})
public class CustomerProfileController {
    private static final Logger log = LoggerFactory.getLogger(CustomerProfileController.class);
    private static final String ATTR_USER_ID = "userId";

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/profile")
    public ApiResponse<CustomerProfileResponse> getProfile(@RequestAttribute(ATTR_USER_ID) Long userId) {
        return ApiResponse.success("Profile fetched successfully", customerProfileService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<CustomerProfileResponse> updateProfile(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                              @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated successfully", customerProfileService.updateProfile(userId, request));
    }

    @PutMapping("/profile/details")
    public ApiResponse<CustomerProfileResponse> updateProfileDetails(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                     @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ApiResponse.success("Profile details updated successfully", customerProfileService.updateProfileDetails(userId, request));
    }

    @PutMapping("/profile/email")
    public ApiResponse<CustomerProfileResponse> updateEmail(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                            @Valid @RequestBody CustomerEmailUpdateRequest request) {
        return ApiResponse.success("Email updated successfully", customerProfileService.updateEmail(userId, request));
    }

    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> uploadProfileImage(@RequestAttribute(ATTR_USER_ID) Long userId,
                                                                                    @RequestParam(value = "image", required = false) MultipartFile image,
                                                                                    @RequestParam(value = "file", required = false) MultipartFile file) {
        MultipartFile uploadFile = image != null ? image : file;
        if (uploadFile == null || uploadFile.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("Image file is required"));
        }
        return ResponseEntity.ok(ApiResponse.success("Profile image uploaded successfully",
                customerProfileService.uploadProfileImage(userId, uploadFile)));
    }

    @DeleteMapping("/profile/image")
    public ApiResponse<CustomerProfileResponse> deleteProfileImage(@RequestAttribute(ATTR_USER_ID) Long userId) {
        return ApiResponse.success("Profile image removed successfully", customerProfileService.deleteProfileImage(userId));
    }

    @GetMapping("/addresses")
    public ApiResponse<List<CustomerProfileAddressResponse>> getAddresses(@RequestAttribute(ATTR_USER_ID) Long userId) {
        return ApiResponse.success("ok", customerProfileService.getAddresses(userId));
    }

    @PostMapping("/account-delete-request")
    public ApiResponse<Map<String, Object>> requestAccountDelete(@RequestAttribute(ATTR_USER_ID) Long userId) {
        customerProfileService.requestAccountDelete(userId);
        return ApiResponse.success("ok", Map.of("requested", true));
    }

    @GetMapping("/account-delete-request/status")
    public ApiResponse<Map<String, Object>> accountDeleteStatus(@RequestAttribute(ATTR_USER_ID) Long userId) {
        return ApiResponse.success("ok", customerProfileService.getAccountDeleteStatus(userId));
    }

    @GetMapping("/profile-content")
    public ApiResponse<CustomerProfileContentResponse> getCustomerProfileContent() {
        return ApiResponse.success("ok", customerProfileService.getProfileContent());
    }
}
