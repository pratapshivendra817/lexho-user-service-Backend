package com.lexho.user_service.dto;

public class SubCategoryResponseDTO {

    private Long id;
    private String name;
    private String icon;
    private Long categoryId;
    private String categoryName;

    public SubCategoryResponseDTO() {
    }

    public SubCategoryResponseDTO(Long id, String name, String icon, Long categoryId, String categoryName) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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
}
