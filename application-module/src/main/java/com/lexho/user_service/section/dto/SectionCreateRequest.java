package com.lexho.user_service.section.dto;

import com.lexho.user_service.section.enums.SectionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionCreateRequest {
    @NotNull(message = "type is required")
    private SectionType type;

    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @Size(max = 255, message = "subtitle must be at most 255 characters")
    private String subtitle;

    @Size(max = 255, message = "icon must be at most 255 characters")
    private String icon;

    private Integer priority;

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
