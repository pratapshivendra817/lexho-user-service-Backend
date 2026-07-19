package com.lexho.provider.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "partner")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Long categoryId;
    private Long subCategoryId;
    private Double rating;
    private Long totalReviews;
    private Double price;
    private Boolean available;
    private Integer experience;
    private String image;
    private String verificationStatus;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getSubCategoryId() {
        return subCategoryId;
    }

    public Double getRating() {
        return rating;
    }

    public Double getAverageRating() {
        return rating;
    }

    public Long getTotalReviews() {
        if (totalReviews == null) {
            return Long.valueOf(0L);
        }
        return totalReviews;
    }

    public Double getPrice() {
        return price;
    }

    public Boolean getAvailable() {
        return available;
    }

    public Integer getExperience() {
        return experience;
    }

    public String getImage() {
        return image;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setSubCategoryId(Long subCategoryId) {
        this.subCategoryId = subCategoryId;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public void setAverageRating(Double averageRating) {
        this.rating = averageRating;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
