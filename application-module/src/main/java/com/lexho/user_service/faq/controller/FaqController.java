package com.lexho.user_service.faq.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.faq.dto.FaqResponse;
import com.lexho.user_service.faq.service.FaqService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/faqs", "/api/support/faqs"})
public class FaqController {
    private final FaqService service;

    public FaqController(FaqService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<FaqResponse>> faqs(@RequestParam(required = false) String userType) {
        return ApiResponse.success("FAQs fetched", service.publicFaqs(userType));
    }
}
