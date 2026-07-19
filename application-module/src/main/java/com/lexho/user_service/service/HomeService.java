package com.lexho.user_service.service;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.config.AppProperties;
import com.lexho.user_service.dto.HomeBannerDTO;
import com.lexho.user_service.dto.HomeCategoryDTO;
import com.lexho.user_service.dto.HomeResponseDTO;
import com.lexho.user_service.dto.HomeSectionDTO;
import com.lexho.user_service.dto.HomeServiceDTO;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeService.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PartnerRepository partnerRepository;
    private final AppProperties appProperties;

    public HomeService(UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       PartnerRepository partnerRepository,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.partnerRepository = partnerRepository;
        this.appProperties = appProperties;
    }

    public HomeResponseDTO getHomeData(Long userId) {
        log.info("Home data request received: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<ServiceCategory> categories = categoryRepository.findAll();
        List<Partner> topPartners = partnerRepository.findTop6ByAvailableTrueOrderByRatingDesc();

        HomeResponseDTO response = new HomeResponseDTO();
        response.setUserId(userId);
        response.setGreeting(buildGreeting(user));
        response.setBanners(buildBanners());
        response.setCategories(buildCategoryDtos(categories));
        response.setSections(buildSections(categories, topPartners));
        return response;
    }

    private String buildGreeting(User user) {
        String fallback = appProperties.getHome().getFallbackGreeting() == null
                ? "Hi"
                : appProperties.getHome().getFallbackGreeting();

        String name = user.getFullName();
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return fallback + ", " + name;
    }

    private List<HomeBannerDTO> buildBanners() {
        HomeBannerDTO banner = new HomeBannerDTO();
        banner.setLabel(appProperties.getHome().getPrimaryPromoLabel());
        banner.setCtaLabel(appProperties.getHome().getPrimaryCtaLabel());
        banner.setSecondaryCtaLabel(appProperties.getHome().getSecondaryCtaLabel());
        banner.setTagline(appProperties.getHome().getRecommendationTagline());
        return List.of(banner);
    }

    private List<HomeCategoryDTO> buildCategoryDtos(List<ServiceCategory> categories) {
        List<HomeCategoryDTO> list = new ArrayList<>();
        list.add(new HomeCategoryDTO(0L, "All", null));
        for (ServiceCategory category : categories) {
            list.add(new HomeCategoryDTO(category.getId(), category.getName(), category.getIcon()));
        }
        return list;
    }

    private List<HomeSectionDTO> buildSections(List<ServiceCategory> categories, List<Partner> topPartners) {
        List<HomeSectionDTO> sections = new ArrayList<>();

        HomeSectionDTO recommended = new HomeSectionDTO();
        recommended.setTitle("Recommended");
        recommended.setTagline(appProperties.getHome().getRecommendationTagline());
        recommended.setData(mapPartners(topPartners));
        sections.add(recommended);

        for (ServiceCategory category : categories) {
            List<Partner> categoryTop = partnerRepository
                    .findTop6ByCategoryIdAndAvailableTrueOrderByRatingDesc(category.getId());
            if (categoryTop.isEmpty()) {
                continue;
            }

            HomeSectionDTO section = new HomeSectionDTO();
            section.setTitle(category.getName());
            section.setTagline("Top rated");
            section.setData(mapPartners(categoryTop));
            sections.add(section);
        }

        return sections;
    }

    private List<HomeServiceDTO> mapPartners(List<Partner> partners) {
        return partners.stream().map(partner -> {
            HomeServiceDTO dto = new HomeServiceDTO();
            dto.setPartnerId(partner.getId());
            dto.setName(partner.getName());
            dto.setPrice(partner.getPrice());
            dto.setOriginalPrice(partner.getPrice() == null ? null : partner.getPrice() * 1.2);
            dto.setRating(partner.getRating());
            dto.setExperience(partner.getExperience());
            dto.setImage(partner.getImage());
            return dto;
        }).toList();
    }
}
