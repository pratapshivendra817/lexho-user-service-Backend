package com.lexho.user_service.lexhopartner.dto;

import java.util.List;

public class PartnerCatalogServicePageResponse {
    private List<PartnerCatalogServiceResponse> items;
    private long total;
    private int page;
    private int size;

    public PartnerCatalogServicePageResponse() {
    }

    public PartnerCatalogServicePageResponse(List<PartnerCatalogServiceResponse> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<PartnerCatalogServiceResponse> getItems() {
        return items;
    }

    public void setItems(List<PartnerCatalogServiceResponse> items) {
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
