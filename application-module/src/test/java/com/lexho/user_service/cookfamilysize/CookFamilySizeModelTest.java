package com.lexho.user_service.cookfamilysize;

import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeAdminResponse;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeRequest;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeStatusRequest;
import com.lexho.user_service.cookfamilysize.entity.CookFamilySizeEntity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CookFamilySizeModelTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void cookFamilySizeEntityShouldExposeDefaultsAndLifecycleHooks() throws Exception {
        CookFamilySizeEntity entity = new CookFamilySizeEntity();

        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getDisplayOrder()).isZero();

        entity.setId(4L);
        entity.setFamilySize("SMALL");
        entity.setFamilySizeLabel("1-2 Members");
        entity.setActive(false);
        entity.setDisplayOrder(9);

        invoke(entity, "prePersist");
        LocalDateTime createdAt = entity.getCreatedAt();

        assertThat(entity.getId()).isEqualTo(4L);
        assertThat(entity.getFamilySize()).isEqualTo("SMALL");
        assertThat(entity.getFamilySizeLabel()).isEqualTo("1-2 Members");
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getDisplayOrder()).isEqualTo(9);
        assertThat(createdAt).isNotNull();
        assertThat(entity.getUpdatedAt()).isEqualTo(createdAt);

        invoke(entity, "preUpdate");
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void cookFamilySizeRequestShouldValidateRequiredFieldsAndBounds() {
        CookFamilySizeRequest invalid = new CookFamilySizeRequest();
        Set<ConstraintViolation<CookFamilySizeRequest>> violations = validator.validate(invalid);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("familySize", "familySizeLabel", "active", "displayOrder");

        CookFamilySizeRequest valid = new CookFamilySizeRequest();
        valid.setFamilySize("MEDIUM");
        valid.setFamilySizeLabel("3-5 Members");
        valid.setActive(Boolean.TRUE);
        valid.setDisplayOrder(0);

        assertThat(validator.validate(valid)).isEmpty();
        assertThat(valid.getFamilySize()).isEqualTo("MEDIUM");
    }

    @Test
    void responseAndStatusDtosShouldExposeStateThroughGeneratedMethods() {
        CookFamilySizeAdminResponse response =
                new CookFamilySizeAdminResponse(8L, "LARGE", "6-8 Members", true, 3);

        assertThat(response.getFamilySizeId()).isEqualTo(8L);
        assertThat(response.getFamilySize()).isEqualTo("LARGE");
        assertThat(response.getFamilySizeLabel()).isEqualTo("6-8 Members");
        assertThat(response.getActive()).isTrue();
        assertThat(response.getDisplayOrder()).isEqualTo(3);
        assertThat(response.toString()).contains("LARGE", "6-8 Members");

        CookFamilySizeStatusRequest statusRequest = new CookFamilySizeStatusRequest();
        statusRequest.setActive(Boolean.FALSE);
        assertThat(statusRequest.getActive()).isFalse();
    }

    private static void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}
