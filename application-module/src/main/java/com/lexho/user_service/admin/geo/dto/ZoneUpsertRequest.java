package com.lexho.user_service.admin.geo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ZoneUpsertRequest {

    @NotNull(message = "cityId is required")
    public Long cityId;

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must be at most 120 characters")
    public String name;

    public String polygonJson;

    public Boolean active;
}
