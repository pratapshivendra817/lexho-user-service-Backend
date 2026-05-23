package com.lexho.user_service.partnerrequest.dto;

import java.util.List;

public class PagedResponse<T> {
    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;

    public PagedResponse(List<T> content, long totalElements, int totalPages, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    public List<T> getContent() { return content; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
