package com.lexho.user_service.customer.dto;

import java.util.List;

public class CustomerServiceCatalogPageResponse {
    private List<CustomerServiceCatalogResponse> items;
    private long total;
    private int page;
    private int size;

    public CustomerServiceCatalogPageResponse() {
    }

    public CustomerServiceCatalogPageResponse(List<CustomerServiceCatalogResponse> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<CustomerServiceCatalogResponse> getItems() {
        return items;
    }

    public void setItems(List<CustomerServiceCatalogResponse> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
