package com.lexho.user_service.socialmedia.repository;

import com.lexho.user_service.socialmedia.entity.SocialMediaLink;
import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialMediaLinkRepository extends JpaRepository<SocialMediaLink, Long> {
    boolean existsByPlatform(SocialMediaPlatform platform);

    boolean existsByPlatformAndIdNot(SocialMediaPlatform platform, Long id);

    Optional<SocialMediaLink> findByPlatform(SocialMediaPlatform platform);

    Page<SocialMediaLink> findByActive(boolean active, Pageable pageable);

    Page<SocialMediaLink> findByPlatform(SocialMediaPlatform platform, Pageable pageable);

    Page<SocialMediaLink> findByPlatformAndActive(SocialMediaPlatform platform, boolean active, Pageable pageable);

    List<SocialMediaLink> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
