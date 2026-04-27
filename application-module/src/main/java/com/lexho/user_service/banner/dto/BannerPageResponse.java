package com.lexho.user_service.banner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BannerPageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
}

