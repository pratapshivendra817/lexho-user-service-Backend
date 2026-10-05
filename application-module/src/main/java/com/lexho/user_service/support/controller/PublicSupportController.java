package com.lexho.user_service.support.controller;

import com.lexho.user_service.support.dto.PublicSupportTicketRequest;
import com.lexho.user_service.support.service.PublicSupportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/support")
public class PublicSupportController {

    private final PublicSupportService publicSupportService;

    public PublicSupportController(PublicSupportService publicSupportService) {
        this.publicSupportService = publicSupportService;
    }

    @GetMapping({"/faqs", "/faqs/"})
    public ResponseEntity<Map<String, Object>> faqs() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "FAQs fetched",
                "data", publicSupportService.publicFaqs()
        ));
    }

    @PostMapping({"/tickets", "/tickets/"})
    public ResponseEntity<Map<String, Object>> createTicket(
            @Valid @RequestBody PublicSupportTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Ticket created",
                "data", publicSupportService.createTicket(request)
        ));
    }
}
