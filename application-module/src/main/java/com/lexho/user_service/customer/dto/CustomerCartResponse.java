package com.lexho.user_service.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCartResponse {
    private Long cartId;
    private Double grandTotal;
    private Double itemTotal;
    private Double gstAmount;
    private Double serviceFee;
    private Double taxAmount;
    private Double toPay;
    private Double totalSaved;
    private String currency;
    private String scheduledTime;
    private String requestStatus;
    private Integer matchedPartners;
    private String assignmentMode;
    private String contactName;
    private String contactPhone;
    private Address address;
    private List<CustomerCartItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private Long id;
        private String fullAddress;
        private String city;
        private String pincode;
    }
}
