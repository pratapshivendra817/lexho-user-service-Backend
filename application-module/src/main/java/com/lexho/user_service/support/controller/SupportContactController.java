package com.lexho.user_service.support.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.support.dto.SupportContactResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support")
public class SupportContactController {
    private final String email;
    private final String mobile;

    public SupportContactController(
            @Value("${lexho.support.email:support@lexho.in}") String email,
            @Value("${lexho.support.mobile:+910000000000}") String mobile) {
        this.email = email;
        this.mobile = mobile;
    }

    @GetMapping("/contact")
    public ApiResponse<SupportContactResponse> contact() {
        return ApiResponse.success("Support contact fetched", new SupportContactResponse(email, mobile));
    }
}
