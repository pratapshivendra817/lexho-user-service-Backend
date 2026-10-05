package com.lexho.user_service.socialmedia.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.socialmedia.dto.PublicSocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.service.SocialMediaManagementService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/social-media")
@RequiredArgsConstructor
public class PublicSocialMediaController {
    private final SocialMediaManagementService socialMediaManagementService;

    @GetMapping
    @Operation(summary = "List active social media links for public website")
    public ResponseEntity<ApiResponse<List<PublicSocialMediaLinkResponse>>> active() {
        return ResponseEntity.ok(ApiResponse.success("Social media links fetched",
                socialMediaManagementService.getActivePublicLinks()));
    }
}
