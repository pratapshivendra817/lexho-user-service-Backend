package com.lexho.user_service.customer.dto;

import com.lexho.user_service.addon.dto.CartAddonResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private String bookingMode;
    private Long familySizeId;
    private String familySize;
    private String familySizeLabel;
    private String frequency;
    private Integer visits;
    private String timeSlot;
    private String startDate;
    private String endDate;
    private Double price;
    private Integer quantity;
    private List<CartAddonResponse> addons;
    private Double totalPrice;
}
