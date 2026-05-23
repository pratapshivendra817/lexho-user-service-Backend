package com.lexho.user_service.section.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SectionPageResponse {
    private List<SectionResponse> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
