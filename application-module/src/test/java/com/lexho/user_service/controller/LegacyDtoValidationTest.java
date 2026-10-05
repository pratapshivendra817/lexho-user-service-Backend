package com.lexho.user_service.controller;

import com.lexho.user_service.dto.SubCategoryCreateRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void subCategoryCreateRequestRejectsBlankNameAndMissingCategory() {
        SubCategoryCreateRequestDTO request = new SubCategoryCreateRequestDTO();
        request.setName(" ");
        request.setIcon("");

        Set<ConstraintViolation<SubCategoryCreateRequestDTO>> violations = validator.validate(request);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "icon", "categoryId");
    }
}
