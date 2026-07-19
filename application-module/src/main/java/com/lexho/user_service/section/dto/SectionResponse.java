package com.lexho.user_service.section.dto;

import com.lexho.user_service.section.enums.SectionType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SectionResponse {
    private Long id;
    private SectionType type;
    private String title;
    private String subtitle;
    private String icon;
    private Integer priority;
    private boolean active;
    private LocalDateTime createdAt;
    private List<SectionItemResponse> items;
}
