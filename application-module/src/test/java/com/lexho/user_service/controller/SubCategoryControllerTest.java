package com.lexho.user_service.controller;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.entity.SubCategory;
import com.lexho.catalog.repository.SubCategoryRepository;
import com.lexho.user_service.dto.SubCategoryCreateRequestDTO;
import com.lexho.user_service.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubCategoryControllerTest {

    private final SubCategoryRepository repository = mock(SubCategoryRepository.class);
    private final SubCategoryController controller = new SubCategoryController(repository);

    @Test
    void createShouldPersistEntityButReturnDto() {
        SubCategoryCreateRequestDTO request = new SubCategoryCreateRequestDTO();
        request.setName("Kitchen Cleaning");
        request.setIcon("kitchen.png");
        request.setCategoryId(10L);
        when(repository.save(any(SubCategory.class))).thenAnswer(invocation -> {
            SubCategory saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        var response = controller.create(request);

        assertEquals(100L, response.getId());
        assertEquals("Kitchen Cleaning", response.getName());
        assertEquals(10L, response.getCategoryId());
        verify(repository).save(any(SubCategory.class));
    }

    @Test
    void getByCategoryShouldMapEntitiesToDtoList() {
        SubCategory row = subCategory();
        when(repository.findByCategoryId(10L)).thenReturn(List.of(row));

        var response = controller.getByCategory(10L);

        assertEquals(1, response.size());
        assertEquals(20L, response.get(0).getId());
        assertEquals(10L, response.get(0).getCategoryId());
        assertEquals("Cleaning", response.get(0).getCategoryName());
    }

    @Test
    void createShouldRejectNullBodyWithoutSaving() {
        assertThrows(BadRequestException.class, () -> controller.create(null));
        verify(repository, never()).save(any(SubCategory.class));
    }

    private SubCategory subCategory() {
        ServiceCategory category = new ServiceCategory();
        category.setId(10L);
        category.setName("Cleaning");
        SubCategory subCategory = new SubCategory();
        subCategory.setId(20L);
        subCategory.setName("Kitchen Cleaning");
        subCategory.setIcon("kitchen.png");
        subCategory.setCategory(category);
        return subCategory;
    }
}
