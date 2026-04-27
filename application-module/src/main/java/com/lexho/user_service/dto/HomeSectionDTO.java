package com.lexho.user_service.dto;

import java.util.List;

public class HomeSectionDTO {
    private String title;
    private String tagline;
    private List<HomeServiceDTO> data;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public List<HomeServiceDTO> getData() {
        return data;
    }

    public void setData(List<HomeServiceDTO> data) {
        this.data = data;
    }
}

