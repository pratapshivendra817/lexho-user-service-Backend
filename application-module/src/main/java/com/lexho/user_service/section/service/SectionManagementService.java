package com.lexho.user_service.section.service;

import com.lexho.user_service.section.dto.SectionCreateRequest;
import com.lexho.user_service.section.dto.SectionItemReorderRequest;
import com.lexho.user_service.section.dto.SectionItemRequest;
import com.lexho.user_service.section.dto.SectionItemResponse;
import com.lexho.user_service.section.dto.SectionPageResponse;
import com.lexho.user_service.section.dto.SectionResponse;
import com.lexho.user_service.section.dto.SectionStatusRequest;
import com.lexho.user_service.section.dto.SectionUpdateRequest;

import java.util.List;

public interface SectionManagementService {
    SectionResponse createSection(SectionCreateRequest request);
    List<SectionItemResponse> addSectionItems(Long sectionId, List<SectionItemRequest> request);
    SectionResponse updateSection(Long sectionId, SectionUpdateRequest request);
    List<SectionItemResponse> reorderItems(Long sectionId, List<SectionItemReorderRequest> request);
    SectionResponse updateSectionStatus(Long sectionId, SectionStatusRequest request);
    SectionPageResponse getSections(int page, int size);
    List<SectionResponse> getActiveCustomerSections();
}
