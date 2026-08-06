package com.lexho.user_service.controller;

import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.dto.PartnerCreateRequestDTO;
import com.lexho.user_service.dto.PartnerResponseDTO;
import jakarta.validation.Valid;
import com.lexho.user_service.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/partner", "/api/partners"})
public class PartnerController {

    private final PartnerRepository repo;
    private final SecurityUtils securityUtils;

    public PartnerController(PartnerRepository repo, SecurityUtils securityUtils) {
        this.repo = repo;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("hasRole('PARTNER')")
    public ApiResponse<PartnerResponseDTO> create(@Valid @RequestBody PartnerCreateRequestDTO request) {
        if (request == null) {
            return ApiResponse.failure("Partner request is required");
        }
        Partner partner = toEntity(request);
        List<Partner> existing = repo.findByNameAndSubCategoryId(partner.getName(), partner.getSubCategoryId());
        if (!existing.isEmpty()) {
            return ApiResponse.failure("Partner already exists");
        }
        return ApiResponse.success("Partner created", toDto(repo.save(partner)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public ApiResponse<List<PartnerResponseDTO>> getPartners(@RequestParam Long subCategoryId,
                                                             @RequestParam(required = false) String sort) {
        List<Partner> partners;
        if ("rating".equalsIgnoreCase(sort)) {
            partners = repo.findBySubCategoryIdAndAvailableTrueOrderByRatingDesc(subCategoryId);
        } else if ("price".equalsIgnoreCase(sort)) {
            partners = repo.findBySubCategoryIdAndAvailableTrueOrderByPriceAsc(subCategoryId);
        } else {
            partners = repo.findBySubCategoryIdAndAvailableTrue(subCategoryId);
        }

        List<PartnerResponseDTO> response = partners.stream()
                .map(this::toDto)
                .toList();

        return ApiResponse.success("Partners fetched successfully", response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public ApiResponse<PartnerResponseDTO> getById(@PathVariable Long id) {
        assertPartnerAccess(id);
        Partner p = repo.findById(id).orElse(null);
        if (p == null) {
            return ApiResponse.failure("Partner not found");
        }
        return ApiResponse.success("Partner fetched", toDto(p));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ApiResponse<String> delete(@PathVariable Long id) {
        assertPartnerAccess(id);
        if (!repo.existsById(id)) {
            return ApiResponse.failure("Partner not found");
        }
        repo.deleteById(id);
        return ApiResponse.success("Partner deleted", null);
    }

    private Partner toEntity(PartnerCreateRequestDTO request) {
        Partner partner = new Partner();
        partner.setName(request.getName());
        partner.setCategoryId(request.getCategoryId());
        partner.setSubCategoryId(request.getSubCategoryId());
        partner.setRating(request.getRating());
        partner.setTotalReviews(request.getTotalReviews());
        partner.setPrice(request.getPrice());
        partner.setAvailable(request.getAvailable());
        partner.setExperience(request.getExperience());
        partner.setImage(request.getImage());
        partner.setVerificationStatus(request.getVerificationStatus());
        return partner;
    }

    private PartnerResponseDTO toDto(Partner partner) {
        return PartnerResponseDTO.from(partner);
    }

    private void assertPartnerAccess(Long partnerId) {
        String role = securityUtils.getCurrentRole().orElse("");
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        Long currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("You are not allowed to access this partner"));
        if (!currentUserId.equals(partnerId)) {
            throw new AccessDeniedException("You are not allowed to access this partner");
        }
    }
}
