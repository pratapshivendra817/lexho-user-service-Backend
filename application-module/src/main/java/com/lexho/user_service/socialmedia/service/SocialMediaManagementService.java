package com.lexho.user_service.socialmedia.service;

import com.lexho.user_service.socialmedia.dto.PublicSocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaLinkResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaPageResponse;
import com.lexho.user_service.socialmedia.dto.SocialMediaReorderRequest;
import com.lexho.user_service.socialmedia.dto.SocialMediaStatusRequest;
import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;

import java.util.List;

public interface SocialMediaManagementService {
    SocialMediaPageResponse getAdminLinks(int page, int size, Boolean active, SocialMediaPlatform platform);

    SocialMediaLinkResponse getById(Long id);

    SocialMediaLinkResponse create(SocialMediaLinkRequest request);

    SocialMediaLinkResponse update(Long id, SocialMediaLinkRequest request);

    SocialMediaLinkResponse updateStatus(Long id, SocialMediaStatusRequest request);

    void delete(Long id);

    List<SocialMediaLinkResponse> reorder(List<SocialMediaReorderRequest> request);

    List<PublicSocialMediaLinkResponse> getActivePublicLinks();
}
