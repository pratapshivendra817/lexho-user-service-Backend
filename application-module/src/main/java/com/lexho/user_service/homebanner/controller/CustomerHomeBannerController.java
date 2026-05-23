package com.lexho.user_service.homebanner.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.homebanner.dto.HomeBannerResponse;
import com.lexho.user_service.homebanner.service.HomeBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/home/banners")
@RequiredArgsConstructor
public class CustomerHomeBannerController {

    private final HomeBannerService homeBannerService;

    @GetMapping
    public ApiResponse<List<HomeBannerResponse>> getHomeBanners() {
        return ApiResponse.success("Home banners fetched", homeBannerService.getCustomerBanners());
    }
}
