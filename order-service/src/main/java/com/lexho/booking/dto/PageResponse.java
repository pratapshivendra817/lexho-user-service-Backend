package com.lexho.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int page;
    private int size;
}

