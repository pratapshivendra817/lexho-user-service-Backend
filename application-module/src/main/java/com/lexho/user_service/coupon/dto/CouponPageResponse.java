package com.lexho.user_service.coupon.dto;

import java.util.List;

public record CouponPageResponse(
        List<CouponResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
