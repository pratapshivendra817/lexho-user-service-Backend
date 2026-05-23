package com.lexho.user_service.lexho.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.service.LexhoProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
public class LexhoUserProfileController {

    private final LexhoProfileService profileService;

    public LexhoUserProfileController(LexhoProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LexhoDtos.UserProfileResponse>> profile(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profileService.getProfile(userId)));
    }

}
