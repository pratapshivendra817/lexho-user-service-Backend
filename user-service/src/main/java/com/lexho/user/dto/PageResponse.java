package com.lexho.user.dto;

import java.util.List;

public class PageResponse<T> {
    private List<T> data;
    private long totalElements;
    private int page;
    private int size;

    public PageResponse(List<T> data, long totalElements, int page, int size) {
        this.data = data;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
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
