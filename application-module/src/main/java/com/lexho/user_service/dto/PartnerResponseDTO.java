package com.lexho.user_service.dto;

public class PartnerResponseDTO {
    private Long id;
    private String name;
    private Double rating;
    private Double price;
    private Integer experience;
    private String image;
    private Long categoryId;
    private Long subCategoryId;

    public PartnerResponseDTO() {
    }

    public PartnerResponseDTO(Long id,
                              String name,
                              Double rating,
                              Double price,
                              Integer experience,
                              String image,
                              Long categoryId,
                              Long subCategoryId) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.price = price;
        this.experience = experience;
        this.image = image;
        this.categoryId = categoryId;
        this.subCategoryId = subCategoryId;
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
}

