package com.lexho.user_service.admin.geo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CityUpsertRequest {

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must be at most 120 characters")
    public String name;

    @Size(max = 120, message = "state must be at most 120 characters")
    public String state;

    @Size(max = 120, message = "country must be at most 120 characters")
    public String country;

    public Boolean active;
}
