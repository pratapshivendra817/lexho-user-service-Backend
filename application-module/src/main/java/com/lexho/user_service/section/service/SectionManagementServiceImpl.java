package com.lexho.user_service.section.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.section.dto.SectionCreateRequest;
import com.lexho.user_service.section.dto.SectionItemReorderRequest;
import com.lexho.user_service.section.dto.SectionItemRequest;
import com.lexho.user_service.section.dto.SectionItemResponse;
import com.lexho.user_service.section.dto.SectionItemStatusRequest;
import com.lexho.user_service.section.dto.SectionPageResponse;
import com.lexho.user_service.section.dto.SectionReorderRequest;
import com.lexho.user_service.section.dto.SectionResponse;
import com.lexho.user_service.section.dto.SectionStatusRequest;
import com.lexho.user_service.section.dto.SectionUpdateRequest;
import com.lexho.user_service.section.entity.Section;
import com.lexho.user_service.section.entity.SectionItem;
import com.lexho.user_service.section.enums.SectionType;
import com.lexho.user_service.section.repository.SectionItemRepository;
import com.lexho.user_service.section.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionManagementServiceImpl implements SectionManagementService {

    private final SectionRepository sectionRepository;
    private final SectionItemRepository sectionItemRepository;

    @Override
    @Transactional
    public SectionResponse createSection(SectionCreateRequest request) {
        Section section = new Section();
        section.setType(request.getType());
        section.setTitle(request.getTitle());
        section.setSubtitle(request.getSubtitle());
        section.setIcon(request.getIcon());
        section.setPriority(resolvePriority(request.getPriority(), request.getType()));
        section.setActive(Boolean.TRUE.equals(request.getIsActive()));
        Section saved = sectionRepository.save(section);
        return toSectionResponse(saved, List.of());
    }

    @Override
    @Transactional
    public List<SectionItemResponse> addSectionItems(Long sectionId, List<SectionItemRequest> request) {
        if (request == null || request.isEmpty()) {
            throw new BadRequestException("At least one section item is required");
        }
        Section section = findSection(sectionId);
        List<SectionItem> newItems = request.stream().map(row -> mapItemRequest(section, row)).toList();
        return sectionItemRepository.saveAll(newItems).stream().map(this::toItemResponse).toList();
    }

    @Override
    @Transactional
    public SectionItemResponse updateSectionItem(Long sectionId, Long itemId, SectionItemRequest request) {
        findSection(sectionId);
        SectionItem item = findSectionItem(sectionId, itemId);
        item.setTitle(request.getTitle());
        item.setSubtitle(request.getSubtitle());
        item.setValue(request.getValue());
        item.setIcon(request.getIcon());
        item.setPriority(request.getPriority());
        item.setActive(Boolean.TRUE.equals(request.getIsActive()));
        return toItemResponse(sectionItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deleteSectionItem(Long sectionId, Long itemId) {
        findSection(sectionId);
        SectionItem item = findSectionItem(sectionId, itemId);
        sectionItemRepository.delete(item);
    }

    @Override
    @Transactional
    public SectionItemResponse updateSectionItemStatus(Long sectionId, Long itemId, SectionItemStatusRequest request) {
        findSection(sectionId);
        SectionItem item = findSectionItem(sectionId, itemId);
        item.setActive(Boolean.TRUE.equals(request.getIsActive()));
        return toItemResponse(sectionItemRepository.save(item));
    }

    @Override
    @Transactional
    public SectionResponse updateSection(Long sectionId, SectionUpdateRequest request) {
        Section section = findSection(sectionId);
        section.setType(request.getType());
        section.setTitle(request.getTitle());
        section.setSubtitle(request.getSubtitle());
        section.setIcon(request.getIcon());
        section.setPriority(resolvePriority(request.getPriority(), request.getType()));
        section.setActive(Boolean.TRUE.equals(request.getIsActive()));
        Section saved = sectionRepository.save(section);
        return toSectionResponse(saved, getItems(sectionId));
    }

    @Override
    @Transactional
    public List<SectionResponse> reorderSections(List<SectionReorderRequest> request) {
        if (request == null || request.isEmpty()) {
            throw new BadRequestException("At least one reorder row is required");
        }
        Set<Long> ids = new HashSet<>();
        for (SectionReorderRequest row : request) {
            if (!ids.add(row.getId())) {
                throw new BadRequestException("Duplicate section id in reorder request: " + row.getId());
            }
        }
        Map<Long, Section> sectionMap = sectionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Section::getId, Function.identity()));
        for (SectionReorderRequest row : request) {
            Section section = sectionMap.get(row.getId());
            if (section == null) {
                throw new ResourceNotFoundException("Section not found: " + row.getId());
            }
            section.setPriority(row.getPriority());
        }
        sectionRepository.saveAll(sectionMap.values());
        return sectionRepository.findAll(sectionSort()).stream()
                .map(section -> toSectionResponse(section, getItems(section.getId())))
                .toList();
    }

    @Override
    @Transactional
    public List<SectionItemResponse> reorderItems(Long sectionId, List<SectionItemReorderRequest> request) {
        if (request == null || request.isEmpty()) {
            throw new BadRequestException("At least one reorder row is required");
        }
        findSection(sectionId);
        Set<Long> ids = new HashSet<>();
        for (SectionItemReorderRequest row : request) {
            if (!ids.add(row.getId())) {
                throw new BadRequestException("Duplicate item id in reorder request: " + row.getId());
            }
        }
        List<SectionItem> currentItems = sectionItemRepository.findBySectionIdOrderByPriorityAscIdAsc(sectionId);
        Map<Long, SectionItem> itemMap = currentItems.stream().collect(Collectors.toMap(SectionItem::getId, Function.identity()));
        for (SectionItemReorderRequest row : request) {
            SectionItem item = itemMap.get(row.getId());
            if (item == null) {
                throw new BadRequestException("Item does not belong to section: " + row.getId());
            }
            item.setPriority(row.getPriority());
        }
        sectionItemRepository.saveAll(currentItems);
        return getItems(sectionId);
    }

    @Override
    @Transactional
    public SectionResponse updateSectionStatus(Long sectionId, SectionStatusRequest request) {
        Section section = findSection(sectionId);
        section.setActive(Boolean.TRUE.equals(request.getIsActive()));
        Section saved = sectionRepository.save(section);
        return toSectionResponse(saved, getItems(sectionId));
    }

    @Override
    @Transactional(readOnly = true)
    public SectionPageResponse getSections(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, sectionSort());
        Page<Section> sectionPage = sectionRepository.findAll(pageable);
        List<SectionResponse> content = sectionPage.getContent().stream()
                .map(section -> toSectionResponse(section, getItems(section.getId())))
                .toList();
        return new SectionPageResponse(content, sectionPage.getTotalElements(), sectionPage.getTotalPages(), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionResponse> getActiveCustomerSections() {
        return sectionRepository.findByActiveTrueOrderByPriorityAscCreatedAtDescIdAsc().stream()
                .map(section -> toSectionResponse(section, getActiveItems(section.getId())))
                .toList();
    }

    private Section findSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
    }

    private List<SectionItemResponse> getItems(Long sectionId) {
        return sectionItemRepository.findBySectionIdOrderByPriorityAscIdAsc(sectionId).stream()
                .map(this::toItemResponse)
                .toList();
    }

    private List<SectionItemResponse> getActiveItems(Long sectionId) {
        return sectionItemRepository.findBySectionIdAndActiveTrueOrderByPriorityAscIdAsc(sectionId).stream()
                .map(this::toItemResponse)
                .toList();
    }

    private SectionItem findSectionItem(Long sectionId, Long itemId) {
        SectionItem item = sectionItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Section item not found: " + itemId));
        if (!item.getSection().getId().equals(sectionId)) {
            throw new BadRequestException("Item does not belong to section: " + itemId);
        }
        return item;
    }

    private SectionItem mapItemRequest(Section section, SectionItemRequest request) {
        SectionItem item = new SectionItem();
        item.setTitle(request.getTitle());
        item.setSubtitle(request.getSubtitle());
        item.setValue(request.getValue());
        item.setIcon(request.getIcon());
        item.setPriority(request.getPriority());
        item.setSection(section);
        item.setActive(Boolean.TRUE.equals(request.getIsActive()));
        return item;
    }

    private SectionItemResponse toItemResponse(SectionItem item) {
        SectionItemResponse response = new SectionItemResponse();
        response.setId(item.getId());
        response.setTitle(item.getTitle());
        response.setSubtitle(item.getSubtitle());
        response.setValue(item.getValue());
        response.setIcon(item.getIcon());
        response.setPriority(item.getPriority());
        response.setSectionId(item.getSection().getId());
        response.setActive(item.isActive());
        return response;
    }

    private SectionResponse toSectionResponse(Section section, List<SectionItemResponse> items) {
        SectionResponse response = new SectionResponse();
        response.setId(section.getId());
        response.setType(section.getType());
        response.setTitle(section.getTitle());
        response.setSubtitle(section.getSubtitle());
        response.setIcon(section.getIcon());
        response.setPriority(section.getPriority());
        response.setCreatedAt(section.getCreatedAt());
        response.setActive(section.isActive());
        response.setItems(items);
        return response;
    }

    private int resolvePriority(Integer priority, SectionType type) {
        return priority != null ? priority : sectionTypeOrder(type);
    }

    private Sort sectionSort() {
        return Sort.by(Sort.Order.asc("priority"), Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
    }

    private int sectionTypeOrder(SectionType type) {
        if (type == null) {
            return 99;
        }
        return switch (type) {
            case STATS -> 0;
            case TRUST -> 1;
            case TAGLINE -> 2;
            case HIGHLIGHTS -> 3;
        };
    }
}
