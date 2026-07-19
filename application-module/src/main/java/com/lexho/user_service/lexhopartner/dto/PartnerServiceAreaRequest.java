package com.lexho.user_service.lexhopartner.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PartnerServiceAreaRequest(@NotEmpty List<ServiceAreaItem> areas) {
    public record ServiceAreaItem(String pincode, String city) {
    }
}
