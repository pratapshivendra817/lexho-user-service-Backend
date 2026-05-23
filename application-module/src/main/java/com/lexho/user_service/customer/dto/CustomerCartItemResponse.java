package com.lexho.user_service.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCartItemResponse {
    private Long itemId;
    private Long serviceId;
    private String name;
    private String imageUrl;
    private String serviceType;
    private Double price;
    private Integer quantity;
    private Double totalPrice;
}
