package com.lexho.user_service.dto;

import java.util.List;

public class CartSummaryDTO {

    private List<CartResponseDTO> items;
    private double total;

    public CartSummaryDTO() {
    }

    public CartSummaryDTO(List<CartResponseDTO> items, double total) {
        this.items = items;
        this.total = total;
    }

    public List<CartResponseDTO> getItems() {
        return items;
    }

    public void setItems(List<CartResponseDTO> items) {
        this.items = items;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}

