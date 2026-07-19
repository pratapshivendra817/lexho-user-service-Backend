package com.lexho.provider.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class ProviderPageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;

    public ProviderPageResponse(List<T> content, long totalElements, int totalPages, int pageNumber) {
        this.content = content == null ? new ArrayList<>() : new ArrayList<>(content);
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.pageNumber = pageNumber;
    }

    public List<T> getContent() {
        return content == null ? Collections.emptyList() : new ArrayList<>(content);
    }
}
