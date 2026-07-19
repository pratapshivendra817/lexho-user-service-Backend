package com.lexho.user_service.customer.dto;

import com.lexho.user_service.addon.dto.CustomerAddonResponse;

import java.math.BigDecimal;
import java.util.List;

public class CustomerServiceCatalogResponse {
    private Long id;
    private Long serviceId;
    private String name;
    private String serviceName;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private String serviceType;
    private List<CustomerAddonResponse> addons;
    private List<String> included;
    private List<String> excluded;
    private List<String> faqs;
    private List<String> instructions;

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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public List<CustomerAddonResponse> getAddons() {
        return addons;
    }

    public void setAddons(List<CustomerAddonResponse> addons) {
        this.addons = addons;
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
}
