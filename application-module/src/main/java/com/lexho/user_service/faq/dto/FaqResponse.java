package com.lexho.user_service.faq.dto;

public record FaqResponse(
        Long id,
        String category,
        String question,
        String answer,
        String userType,
        Integer displayOrder,
        boolean active) {
}
