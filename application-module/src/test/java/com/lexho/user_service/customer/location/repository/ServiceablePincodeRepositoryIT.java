package com.lexho.user_service.customer.location.repository;

import com.lexho.testconfig.LexhoInfrastructureTest;
import com.lexho.user_service.customer.location.entity.ServiceablePincode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = ServiceablePincodeRepositoryIT.JpaConfig.class)
@LexhoInfrastructureTest
@ActiveProfiles("it")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
class ServiceablePincodeRepositoryIT {

    @Autowired
    private ServiceablePincodeRepository repository;

    @Test
    void shouldPersistAndFindActivePincode() {
        ServiceablePincode p = new ServiceablePincode();
        p.setId("svc-pin-201304");
        p.setPincode("201304");
        p.setCity("Noida");
        p.setActive(true);
        repository.save(p);
        assertTrue(repository.existsByPincodeAndActiveTrue("201304"));
    }

    @org.springframework.boot.SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ServiceablePincode.class)
    @EnableJpaRepositories(
            basePackageClasses = ServiceablePincodeRepository.class,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = ServiceablePincodeRepository.class
            )
    )
    static class JpaConfig {
    }
}
