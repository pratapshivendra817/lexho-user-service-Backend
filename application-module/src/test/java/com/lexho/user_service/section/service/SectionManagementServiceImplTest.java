package com.lexho.user_service.section.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.section.dto.SectionCreateRequest;
import com.lexho.user_service.section.dto.SectionItemReorderRequest;
import com.lexho.user_service.section.dto.SectionItemRequest;
import com.lexho.user_service.section.dto.SectionReorderRequest;
import com.lexho.user_service.section.dto.SectionStatusRequest;
import com.lexho.user_service.section.entity.Section;
import com.lexho.user_service.section.entity.SectionItem;
import com.lexho.user_service.section.enums.SectionType;
import com.lexho.user_service.section.repository.SectionItemRepository;
import com.lexho.user_service.section.repository.SectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SectionManagementServiceImplTest {

    private final SectionRepository sectionRepository = mock(SectionRepository.class);
    private final SectionItemRepository sectionItemRepository = mock(SectionItemRepository.class);
    private final SectionManagementServiceImpl service = new SectionManagementServiceImpl(sectionRepository, sectionItemRepository);

    @Test
    void createAndListShouldResolvePriorityAndClampPagination() {
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> {
            Section saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            return saved;
        });
        Section section = section(1L, SectionType.TRUST, 1, true);
        when(sectionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(section)));
        when(sectionItemRepository.findBySectionIdOrderByPriorityAscIdAsc(1L)).thenReturn(List.of());

        var created = service.createSection(createRequest(SectionType.TRUST, null, true));
        var page = service.getSections(-1, 200);

        assertEquals(1, created.getPriority());
        assertEquals(100, page.getSize());
        assertEquals(1, page.getContent().size());
    }

    @Test
    void addUpdateStatusAndDeleteItemShouldValidateOwnership() {
        Section section = section(1L, SectionType.HIGHLIGHTS, 3, true);
        SectionItem item = item(5L, section, "Old", 1, true);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(sectionItemRepository.saveAll(any())).thenReturn(List.of(item));
        when(sectionItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(sectionItemRepository.save(item)).thenReturn(item);

        assertEquals(1, service.addSectionItems(1L, List.of(itemRequest("Item", 1, true))).size());
        assertEquals(false, service.updateSectionItemStatus(1L, 5L, itemStatus(false)).isActive());
        service.deleteSectionItem(1L, 5L);

        verify(sectionItemRepository).delete(item);

        Section other = section(2L, SectionType.STATS, 0, true);
        SectionItem wrongSectionItem = item(6L, other, "Wrong", 1, true);
        when(sectionItemRepository.findById(6L)).thenReturn(Optional.of(wrongSectionItem));
        assertThrows(BadRequestException.class, () -> service.updateSectionItem(1L, 6L, itemRequest("x", 1, true)));
    }

    @Test
    void reorderSectionsAndItemsShouldRejectDuplicatesAndMissingRows() {
        Section section = section(1L, SectionType.STATS, 0, true);
        SectionItem item = item(5L, section, "Item", 1, true);
        when(sectionRepository.findAllById(any())).thenReturn(List.of(section));
        when(sectionRepository.findAll(any(Sort.class))).thenReturn(List.of(section));
        when(sectionItemRepository.findBySectionIdOrderByPriorityAscIdAsc(1L)).thenReturn(List.of(item));
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));

        assertEquals(9, service.reorderSections(List.of(reorder(1L, 9))).get(0).getPriority());
        assertEquals(1, service.reorderItems(1L, List.of(itemReorder(5L, 2))).size());
        assertThrows(BadRequestException.class, () -> service.reorderSections(List.of(reorder(1L, 1), reorder(1L, 2))));
        assertThrows(BadRequestException.class, () -> service.reorderItems(1L, List.of(itemReorder(7L, 1))));
    }

    @Test
    void activeCustomerSectionsAndStatusShouldMapOnlyActiveItems() {
        Section section = section(1L, SectionType.TAGLINE, 2, true);
        SectionItem activeItem = item(5L, section, "Active", 1, true);
        when(sectionRepository.findByActiveTrueOrderByPriorityAscCreatedAtDescIdAsc()).thenReturn(List.of(section));
        when(sectionItemRepository.findBySectionIdAndActiveTrueOrderByPriorityAscIdAsc(1L)).thenReturn(List.of(activeItem));
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(sectionRepository.save(section)).thenReturn(section);
        when(sectionItemRepository.findBySectionIdOrderByPriorityAscIdAsc(1L)).thenReturn(List.of(activeItem));

        assertEquals(1, service.getActiveCustomerSections().get(0).getItems().size());
        assertEquals(false, service.updateSectionStatus(1L, sectionStatus(false)).isActive());
        assertThrows(ResourceNotFoundException.class, () -> service.updateSectionStatus(99L, sectionStatus(true)));
    }

    @Test
    void createUpdateAndReorderShouldCoverDefaultPriorityAndValidationBranches() {
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> {
            Section saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            return saved;
        });
        Section section = section(3L, SectionType.STATS, 0, true);
        when(sectionRepository.findById(3L)).thenReturn(Optional.of(section));
        when(sectionItemRepository.findBySectionIdOrderByPriorityAscIdAsc(3L)).thenReturn(List.of());

        SectionCreateRequest createRequest = createRequest(null, null, false);
        assertEquals(99, service.createSection(createRequest).getPriority());

        var updateRequest = new com.lexho.user_service.section.dto.SectionUpdateRequest();
        updateRequest.setType(SectionType.HIGHLIGHTS);
        updateRequest.setTitle("Updated");
        updateRequest.setPriority(null);
        updateRequest.setIsActive(true);
        assertEquals(3, service.updateSection(3L, updateRequest).getPriority());

        assertThrows(BadRequestException.class, () -> service.addSectionItems(3L, List.of()));
        assertThrows(BadRequestException.class, () -> service.reorderSections(List.of()));
        assertThrows(ResourceNotFoundException.class, () -> service.reorderSections(List.of(reorder(99L, 1))));
    }

    private SectionCreateRequest createRequest(SectionType type, Integer priority, boolean active) {
        SectionCreateRequest request = new SectionCreateRequest();
        request.setType(type);
        request.setTitle("Title");
        request.setPriority(priority);
        request.setIsActive(active);
        return request;
    }

    private SectionItemRequest itemRequest(String title, int priority, boolean active) {
        SectionItemRequest request = new SectionItemRequest();
        request.setTitle(title);
        request.setIcon("icon");
        request.setPriority(priority);
        request.setIsActive(active);
        return request;
    }

    private SectionStatusRequest sectionStatus(boolean active) {
        SectionStatusRequest request = new SectionStatusRequest();
        request.setIsActive(active);
        return request;
    }

    private com.lexho.user_service.section.dto.SectionItemStatusRequest itemStatus(boolean active) {
        com.lexho.user_service.section.dto.SectionItemStatusRequest request =
                new com.lexho.user_service.section.dto.SectionItemStatusRequest();
        request.setIsActive(active);
        return request;
    }

    private SectionReorderRequest reorder(Long id, int priority) {
        SectionReorderRequest request = new SectionReorderRequest();
        request.setId(id);
        request.setPriority(priority);
        return request;
    }

    private SectionItemReorderRequest itemReorder(Long id, int priority) {
        SectionItemReorderRequest request = new SectionItemReorderRequest();
        request.setId(id);
        request.setPriority(priority);
        return request;
    }

    private Section section(Long id, SectionType type, int priority, boolean active) {
        Section section = new Section();
        ReflectionTestUtils.setField(section, "id", id);
        ReflectionTestUtils.setField(section, "createdAt", LocalDateTime.now());
        section.setType(type);
        section.setTitle("Section " + id);
        section.setPriority(priority);
        section.setActive(active);
        return section;
    }

    private SectionItem item(Long id, Section section, String title, int priority, boolean active) {
        SectionItem item = new SectionItem();
        ReflectionTestUtils.setField(item, "id", id);
        item.setSection(section);
        item.setTitle(title);
        item.setIcon("icon");
        item.setPriority(priority);
        item.setActive(active);
        return item;
    }
}
