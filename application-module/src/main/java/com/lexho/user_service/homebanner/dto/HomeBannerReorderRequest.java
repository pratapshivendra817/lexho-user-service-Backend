package com.lexho.user_service.homebanner.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class HomeBannerReorderRequest {

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<HomeBannerReorderItemRequest> items;
}
