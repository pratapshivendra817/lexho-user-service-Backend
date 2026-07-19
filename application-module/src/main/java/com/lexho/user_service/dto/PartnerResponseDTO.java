package com.lexho.user_service.dto;

import com.lexho.provider.entity.Partner;

public class PartnerResponseDTO {
    private Long id;
    private String name;
    private Double rating;
    private Double price;
    private Integer experience;
    private String image;
    private Long categoryId;
    private Long subCategoryId;
    private Boolean available;
    private Long totalReviews;
    private String verificationStatus;

    public PartnerResponseDTO() {
    }

    public static PartnerResponseDTO from(Partner partner) {
        PartnerResponseDTO dto = new PartnerResponseDTO();
        dto.setId(partner.getId());
        dto.setName(partner.getName());
        dto.setRating(partner.getRating());
        dto.setPrice(partner.getPrice());
        dto.setExperience(partner.getExperience());
        dto.setImage(partner.getImage());
        dto.setCategoryId(partner.getCategoryId());
        dto.setSubCategoryId(partner.getSubCategoryId());
        dto.setAvailable(partner.getAvailable());
        dto.setTotalReviews(partner.getTotalReviews());
        dto.setVerificationStatus(partner.getVerificationStatus());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getSubCategoryId() {
        return subCategoryId;
    }

    public void setSubCategoryId(Long subCategoryId) {
        this.subCategoryId = subCategoryId;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
