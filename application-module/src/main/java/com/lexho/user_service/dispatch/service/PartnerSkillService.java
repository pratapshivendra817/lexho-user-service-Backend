package com.lexho.user_service.dispatch.service;

import com.lexho.provider.entity.Partner;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.dispatch.entity.PartnerServiceSkillEntity;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.service.PartnerMarketplaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PartnerSkillService {

    private final PartnerMarketplaceService partnerMarketplaceService;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final PartnerServiceSkillRepository partnerServiceSkillRepository;

    public PartnerSkillService(com.lexho.provider.repository.PartnerRepository partnerRepository,
                               AdminServiceEntityRepository adminServiceEntityRepository,
                               PartnerServiceSkillRepository partnerServiceSkillRepository) {
        this.partnerMarketplaceService = new PartnerMarketplaceService(partnerRepository);
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.partnerServiceSkillRepository = partnerServiceSkillRepository;
    }

    @Transactional
    public Map<String, Object> upsertSkills(Long partnerId, PartnerSkillsUpdateRequest request) {
        Partner partner = partnerMarketplaceService.require(partnerId);

        Set<Long> resolvedSubServiceIds = new LinkedHashSet<>();
        if (request != null && request.getSubServiceIds() != null) {
            resolvedSubServiceIds.addAll(request.getSubServiceIds());
        }
        if (request != null && request.getCategoryIds() != null) {
            for (Long categoryId : request.getCategoryIds()) {
                resolvedSubServiceIds.addAll(
                        adminServiceEntityRepository.findByCategory_IdAndDeletedFalse(categoryId)
                                .stream()
                                .map(AdminServiceEntity::getId)
                                .toList()
                );
            }
        }

        if (resolvedSubServiceIds.isEmpty()) {
            throw new BadRequestException("At least one category or sub-service is required");
        }

        List<AdminServiceEntity> validServices = adminServiceEntityRepository.findAllById(resolvedSubServiceIds);
        if (validServices.size() != resolvedSubServiceIds.size()) {
            throw new BadRequestException("One or more sub-service ids are invalid");
        }

        partnerServiceSkillRepository.deleteByPartnerId(partnerId);
        List<PartnerServiceSkillEntity> rows = new ArrayList<>();
        for (Long subServiceId : resolvedSubServiceIds) {
            PartnerServiceSkillEntity skill = new PartnerServiceSkillEntity();
            skill.setPartnerId(partnerId);
            skill.setSubServiceId(subServiceId);
            rows.add(skill);
        }
        partnerServiceSkillRepository.saveAll(rows);

        if (partner.getSubCategoryId() == null && !resolvedSubServiceIds.isEmpty()) {
            partner.setSubCategoryId(resolvedSubServiceIds.iterator().next());
        }
        partnerMarketplaceService.save(partner);

        return Map.of(
                "partnerId", partnerId,
                "subServiceIds", resolvedSubServiceIds,
                "skillCount", resolvedSubServiceIds.size()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSkills(Long partnerId) {
        partnerMarketplaceService.require(partnerId);
        List<Long> subServiceIds = partnerServiceSkillRepository.findByPartnerId(partnerId)
                .stream()
                .map(PartnerServiceSkillEntity::getSubServiceId)
                .toList();
        return Map.of(
                "partnerId", partnerId,
                "subServiceIds", subServiceIds,
                "skillCount", subServiceIds.size()
        );
    }
}
