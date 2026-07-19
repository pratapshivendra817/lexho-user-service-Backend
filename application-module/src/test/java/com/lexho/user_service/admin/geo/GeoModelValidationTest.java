package com.lexho.user_service.admin.geo;

import com.lexho.user_service.admin.geo.dto.CityResponse;
import com.lexho.user_service.admin.geo.dto.CityUpsertRequest;
import com.lexho.user_service.admin.geo.dto.ZoneResponse;
import com.lexho.user_service.admin.geo.dto.ZoneUpsertRequest;
import com.lexho.user_service.admin.geo.entity.CityEntity;
import com.lexho.user_service.admin.geo.entity.ZoneEntity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeoModelValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void cityAndZoneRequestsShouldValidateConstraints() {
        CityUpsertRequest cityRequest = new CityUpsertRequest();
        Set<ConstraintViolation<CityUpsertRequest>> cityViolations = validator.validate(cityRequest);
        assertThat(cityViolations).extracting(v -> v.getPropertyPath().toString()).contains("name");

        cityRequest.name = "Delhi";
        cityRequest.state = "NCT";
        cityRequest.country = "India";
        cityRequest.active = Boolean.TRUE;
        assertThat(validator.validate(cityRequest)).isEmpty();

        ZoneUpsertRequest zoneRequest = new ZoneUpsertRequest();
        Set<ConstraintViolation<ZoneUpsertRequest>> zoneViolations = validator.validate(zoneRequest);
        assertThat(zoneViolations).extracting(v -> v.getPropertyPath().toString()).contains("cityId", "name");

        zoneRequest.cityId = 1L;
        zoneRequest.name = "North";
        zoneRequest.polygonJson = "{}";
        zoneRequest.active = Boolean.FALSE;
        assertThat(validator.validate(zoneRequest)).isEmpty();
    }

    @Test
    void cityAndZoneEntitiesShouldRunLifecycleMethodsAndExposeFields() throws Exception {
        CityEntity city = new CityEntity();
        city.setId(3L);
        city.setName("Delhi");
        city.setState("NCT");
        city.setCountry("India");
        city.setActive(false);
        invoke(city, "prePersist");
        LocalDateTime cityCreatedAt = city.getCreatedAt();

        assertThat(city.getId()).isEqualTo(3L);
        assertThat(city.getName()).isEqualTo("Delhi");
        assertThat(city.isActive()).isFalse();
        assertThat(cityCreatedAt).isNotNull();
        assertThat(city.getUpdatedAt()).isEqualTo(cityCreatedAt);

        invoke(city, "preUpdate");
        assertThat(city.getUpdatedAt()).isAfterOrEqualTo(cityCreatedAt);

        ZoneEntity zone = new ZoneEntity();
        zone.setId(9L);
        zone.setCity(city);
        zone.setName("North");
        zone.setPolygonJson("{polygon}");
        zone.setActive(false);
        zone.setDeleted(true);
        invoke(zone, "prePersist");
        LocalDateTime zoneCreatedAt = zone.getCreatedAt();

        assertThat(zone.getId()).isEqualTo(9L);
        assertThat(zone.getCity()).isSameAs(city);
        assertThat(zone.getName()).isEqualTo("North");
        assertThat(zone.getPolygonJson()).isEqualTo("{polygon}");
        assertThat(zone.isDeleted()).isTrue();
        assertThat(zoneCreatedAt).isNotNull();

        invoke(zone, "preUpdate");
        assertThat(zone.getUpdatedAt()).isAfterOrEqualTo(zoneCreatedAt);
    }

    @Test
    void responseDtosShouldRetainAssignedValues() {
        CityResponse cityResponse = new CityResponse();
        cityResponse.id = 1L;
        cityResponse.name = "Delhi";
        cityResponse.state = "NCT";
        cityResponse.country = "India";
        cityResponse.active = true;

        ZoneResponse zoneResponse = new ZoneResponse();
        zoneResponse.id = 10L;
        zoneResponse.cityId = 1L;
        zoneResponse.cityName = "Delhi";
        zoneResponse.name = "North";
        zoneResponse.polygonJson = "{polygon}";
        zoneResponse.active = false;

        assertThat(cityResponse.id).isEqualTo(1L);
        assertThat(cityResponse.name).isEqualTo("Delhi");
        assertThat(cityResponse.active).isTrue();
        assertThat(zoneResponse.cityId).isEqualTo(1L);
        assertThat(zoneResponse.cityName).isEqualTo("Delhi");
        assertThat(zoneResponse.active).isFalse();
    }

    private static void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}
