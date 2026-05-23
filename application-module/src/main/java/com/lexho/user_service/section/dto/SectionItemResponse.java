package com.lexho.user_service.section.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionItemResponse {
    private Long id;
    private Long sectionId;
    private String title;
    private String value;
    private String icon;
    private Integer priority;
    private boolean active;
}
