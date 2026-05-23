package com.lexho.user_service.dispatch.dto;

import jakarta.validation.constraints.Positive;

import java.util.List;

public class PartnerSkillsUpdateRequest {
    private List<@Positive Long> categoryIds;
    private List<@Positive Long> subServiceIds;

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<Long> getSubServiceIds() {
        return subServiceIds;
    }

    public void setSubServiceIds(List<Long> subServiceIds) {
        this.subServiceIds = subServiceIds;
    }
}
