package com.lexho.user_service.controller;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.entity.SubCategory;
import com.lexho.catalog.repository.SubCategoryRepository;
import com.lexho.user_service.dto.SubCategoryCreateRequestDTO;
import com.lexho.user_service.dto.SubCategoryResponseDTO;
import com.lexho.user_service.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
public class SubCategoryController {

    private final SubCategoryRepository repo;

    public SubCategoryController(SubCategoryRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public SubCategoryResponseDTO create(@Valid @RequestBody SubCategoryCreateRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Subcategory request is required");
        }
        return toDto(repo.save(toEntity(request)));
    }

    @GetMapping("/category/{id}")
    public List<SubCategoryResponseDTO> getByCategory(@PathVariable Long id) {
        return repo.findByCategoryId(id).stream()
                .map(this::toDto)
                .toList();
    }

    private SubCategory toEntity(SubCategoryCreateRequestDTO request) {
        SubCategory subCategory = new SubCategory();
        subCategory.setName(request.getName());
        subCategory.setIcon(request.getIcon());
        if (request.getCategoryId() != null) {
            ServiceCategory category = new ServiceCategory();
            category.setId(request.getCategoryId());
            subCategory.setCategory(category);
        }
        return subCategory;
    }

    private SubCategoryResponseDTO toDto(SubCategory subCategory) {
        ServiceCategory category = subCategory.getCategory();
        return new SubCategoryResponseDTO(
                subCategory.getId(),
                subCategory.getName(),
                subCategory.getIcon(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName()
        );
    }
}
