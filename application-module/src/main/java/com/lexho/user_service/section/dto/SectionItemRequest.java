package com.lexho.user_service.section.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionItemRequest {
    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @Size(max = 255, message = "subtitle must be at most 255 characters")
    private String subtitle;

    @Size(max = 255, message = "value must be at most 255 characters")
    private String value;

    @NotBlank(message = "icon is required")
    @Size(max = 255, message = "icon must be at most 255 characters")
    private String icon;

    @NotNull(message = "priority is required")
    private Integer priority;

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
