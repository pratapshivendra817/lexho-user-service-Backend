package com.lexho.user_service.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.dto.HomeResponseDTO;
import com.lexho.user_service.service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<HomeResponseDTO>> home(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(
                ApiResponse.success("Home data fetched successfully", homeService.getHomeData(userId))
        );
    }
}

