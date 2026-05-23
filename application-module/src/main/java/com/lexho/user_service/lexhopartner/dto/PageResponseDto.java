package com.lexho.user_service.lexhopartner.dto;

import java.util.List;

public record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
