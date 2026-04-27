package com.lexho.user_service.dto;

import java.util.List;

public class HomeResponseDTO {

    private Long userId;
    private String greeting;
    private List<HomeBannerDTO> banners;
    private List<HomeCategoryDTO> categories;
    private List<HomeSectionDTO> sections;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public List<HomeBannerDTO> getBanners() {
        return banners;
    }

    public void setBanners(List<HomeBannerDTO> banners) {
        this.banners = banners;
    }

    public List<HomeCategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<HomeCategoryDTO> categories) {
        this.categories = categories;
    }

    public List<HomeSectionDTO> getSections() {
        return sections;
    }

    public void setSections(List<HomeSectionDTO> sections) {
        this.sections = sections;
    }
}

