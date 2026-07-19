package com.lexho.user_service.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerServiceCardResponse {
    private Long id;
    private Long serviceId;
    private String name;
    private String serviceName;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Double rating;
    private Long totalReviews;
    private boolean isNew;
    private List<String> bookingModes;
    private BigDecimal hourlyRate;
    private Integer minHours;
    private Integer maxHours;
    private Integer hourStep;
}
