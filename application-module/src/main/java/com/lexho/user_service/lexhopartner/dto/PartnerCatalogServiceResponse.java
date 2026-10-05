package com.lexho.user_service.lexhopartner.dto;

import java.math.BigDecimal;
import java.util.List;

public class PartnerCatalogServiceResponse {
    private Long id;
    private Long serviceId;
    private String name;
    private String serviceName;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String serviceType;
    private Integer durationMinutes;
    private Double rating;
    private Long totalReviews;
    private List<String> included;
    private List<String> excluded;
    private List<String> faqs;
    private List<String> instructions;
    private List<String> requirements;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public List<String> getIncluded() {
        return included;
    }

    public void setIncluded(List<String> included) {
        this.included = included;
    }

    public List<String> getExcluded() {
        return excluded;
    }

    public void setExcluded(List<String> excluded) {
        this.excluded = excluded;
    }

    public List<String> getFaqs() {
        return faqs;
    }

    public void setFaqs(List<String> faqs) {
        this.faqs = faqs;
    }

    public List<String> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<String> instructions) {
        this.instructions = instructions;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<String> requirements) {
        this.requirements = requirements;
    }
}
