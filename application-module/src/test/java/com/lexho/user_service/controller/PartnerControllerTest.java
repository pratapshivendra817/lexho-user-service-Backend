package com.lexho.user_service.controller;

import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.dto.PartnerCreateRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerControllerTest {

    private final PartnerRepository repository = mock(PartnerRepository.class);
    private final PartnerController controller = new PartnerController(repository);

    @Test
    void createShouldReturnDtoInsteadOfEntity() {
        PartnerCreateRequestDTO request = request();
        when(repository.findByNameAndSubCategoryId("Partner One", 20L)).thenReturn(List.of());
        when(repository.save(any(Partner.class))).thenAnswer(invocation -> {
            Partner saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        var response = controller.create(request);

        assertTrue(response.isSuccess());
        assertEquals(100L, response.getData().getId());
        assertEquals("Partner One", response.getData().getName());
        assertEquals(4.5, response.getData().getRating());
        assertEquals(12L, response.getData().getTotalReviews());
        assertEquals("APPROVED", response.getData().getVerificationStatus());
        verify(repository).save(any(Partner.class));
    }

    @Test
    void createShouldRejectDuplicateOrMissingRequestWithoutSaving() {
        PartnerCreateRequestDTO request = request();
        when(repository.findByNameAndSubCategoryId("Partner One", 20L)).thenReturn(List.of(new Partner()));

        assertFalse(controller.create(request).isSuccess());
        assertFalse(controller.create(null).isSuccess());
    }

    @Test
    void getByIdShouldMapEntityToDto() {
        Partner partner = partner();
        partner.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(partner));

        var response = controller.getById(7L);

        assertTrue(response.isSuccess());
        assertEquals(7L, response.getData().getId());
        assertEquals(20L, response.getData().getSubCategoryId());
    }

    private PartnerCreateRequestDTO request() {
        PartnerCreateRequestDTO request = new PartnerCreateRequestDTO();
        request.setName("Partner One");
        request.setCategoryId(10L);
        request.setSubCategoryId(20L);
        request.setRating(4.5);
        request.setTotalReviews(12L);
        request.setPrice(299.0);
        request.setAvailable(true);
        request.setExperience(5);
        request.setImage("image.jpg");
        request.setVerificationStatus("APPROVED");
        return request;
    }

    private Partner partner() {
        Partner partner = new Partner();
        partner.setName("Partner One");
        partner.setCategoryId(10L);
        partner.setSubCategoryId(20L);
        partner.setRating(4.5);
        partner.setTotalReviews(12L);
        partner.setPrice(299.0);
        partner.setAvailable(true);
        partner.setExperience(5);
        partner.setImage("image.jpg");
        partner.setVerificationStatus("APPROVED");
        return partner;
    }
}
