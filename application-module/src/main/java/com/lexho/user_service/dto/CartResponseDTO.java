package com.lexho.user_service.dto;

public class CartResponseDTO {

    private Long id;
    private Long partnerId;
    private String partnerName;
    private Double price;
    private Integer quantity;

    public CartResponseDTO() {
    }

    public CartResponseDTO(Long id, Long partnerId, String partnerName, Double price, Integer quantity) {
        this.id = id;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.price = price;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

