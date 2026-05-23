package com.lexho.user_service.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerServiceCardResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Double rating;
    private Long totalReviews;
    private boolean isNew;
}
