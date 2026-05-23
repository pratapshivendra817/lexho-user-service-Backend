package com.lexho.user_service.customer.banner.controller;

import com.lexho.user_service.customer.banner.dto.CustomerBannerResponse;
import com.lexho.user_service.customer.banner.service.CustomerBannerService;
import com.lexho.user_service.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerBannerController {

    private final CustomerBannerService customerBannerService;

    @GetMapping("/banners")
    public ApiResponse<List<CustomerBannerResponse>> getBanners(@RequestParam(required = false) Long categoryId) {
        return ApiResponse.success("Banners fetched", customerBannerService.getBanners(categoryId));
    }
}
