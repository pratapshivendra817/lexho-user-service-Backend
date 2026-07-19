package com.lexho.user_service.dispatch.service;

import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.dispatch.entity.PartnerServiceSkillEntity;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerSkillServiceTest {

    private final PartnerRepository partners = mock(PartnerRepository.class);
    private final AdminServiceEntityRepository services = mock(AdminServiceEntityRepository.class);
    private final PartnerServiceSkillRepository skills = mock(PartnerServiceSkillRepository.class);
    private final PartnerSkillService service = new PartnerSkillService(partners, services, skills);

    @Test
    void upsertResolvesCategoriesDeduplicatesAndInitializesSubCategory() {
        Partner partner = new Partner();
        AdminServiceEntity first = new AdminServiceEntity();
        ReflectionTestUtils.setField(first, "id", 10L);
        AdminServiceEntity second = new AdminServiceEntity();
        ReflectionTestUtils.setField(second, "id", 20L);
        PartnerSkillsUpdateRequest request = new PartnerSkillsUpdateRequest();
        request.setSubServiceIds(List.of(10L));
        request.setCategoryIds(List.of(5L));
        when(partners.findById(1L)).thenReturn(Optional.of(partner));
        when(services.findByCategory_IdAndDeletedFalse(5L)).thenReturn(List.of(first, second));
        when(services.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(first, second));

        var result = service.upsertSkills(1L, request);

        assertThat(result.get("skillCount")).isEqualTo(2);
        assertThat(partner.getSubCategoryId()).isEqualTo(10L);
        verify(skills).deleteByPartnerId(1L);
        verify(skills).saveAll(anyList());
        verify(partners).save(partner);
    }

    @Test
    void validatesPartnerRequestAndUnknownServices() {
        when(partners.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upsertSkills(1L, null)).isInstanceOf(ResourceNotFoundException.class);

        when(partners.findById(2L)).thenReturn(Optional.of(new Partner()));
        assertThatThrownBy(() -> service.upsertSkills(2L, null)).isInstanceOf(BadRequestException.class);

        PartnerSkillsUpdateRequest request = new PartnerSkillsUpdateRequest();
        request.setSubServiceIds(List.of(99L));
        when(services.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.upsertSkills(2L, request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getSkillsReturnsIdsAndFailsForMissingPartner() {
        PartnerServiceSkillEntity row = new PartnerServiceSkillEntity();
        row.setSubServiceId(11L);
        when(partners.findById(3L)).thenReturn(Optional.of(new Partner()));
        when(skills.findByPartnerId(3L)).thenReturn(List.of(row));

        assertThat(service.getSkills(3L)).containsEntry("skillCount", 1).containsEntry("subServiceIds", List.of(11L));

        when(partners.findById(4L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSkills(4L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
