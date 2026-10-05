package com.lexho.user_service.socialmedia.mapper;

import com.lexho.user_service.socialmedia.dto.PublicSocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.entity.SocialMediaLink;
import org.springframework.stereotype.Component;

@Component
public class SocialMediaLinkMapper {
    public void applyRequest(SocialMediaLink entity, SocialMediaLinkRequest request) {
        entity.setPlatform(request.getPlatform());
        entity.setUrl(trim(request.getUrl()));
        entity.setLabel(trim(request.getLabel()));
        entity.setIcon(trim(request.getIcon()));
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setActive(!Boolean.FALSE.equals(request.getIsActive()));
    }

    public SocialMediaLinkResponse toAdminResponse(SocialMediaLink entity) {
        SocialMediaLinkResponse response = new SocialMediaLinkResponse();
        response.setId(entity.getId());
        response.setPlatform(entity.getPlatform());
        response.setUrl(entity.getUrl());
        response.setLabel(entity.getLabel());
        response.setIcon(entity.getIcon());
        response.setDisplayOrder(entity.getDisplayOrder());
        response.setActive(entity.isActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedBy(entity.getUpdatedBy());
        return response;
    }

    public PublicSocialMediaLinkResponse toPublicResponse(SocialMediaLink entity) {
        return new PublicSocialMediaLinkResponse(
                entity.getPlatform(),
                entity.getUrl(),
                entity.getLabel(),
                entity.getIcon(),
                entity.getDisplayOrder()
        );
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
