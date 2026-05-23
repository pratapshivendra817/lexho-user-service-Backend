package com.lexho.user_service.customer.location.dto;

import java.util.List;

public class PagedResponse<T> {
    public List<T> items;
    public int page;
    public int size;
    public long totalElements;
    public int totalPages;
}
