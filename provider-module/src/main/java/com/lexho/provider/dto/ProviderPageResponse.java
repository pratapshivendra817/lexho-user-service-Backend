package com.lexho.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProviderPageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
}

