package com.lexho.user_service.admin.serviceablearea.service;

import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaStatusRequest;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaUpsertRequest;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceabilityRuleEntity;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceabilityRuleRepository;
import com.lexho.testconfig.LexhoH2DataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoH2DataJpaTest
@ContextConfiguration(classes = ServiceableAreaServiceTest.JpaConfig.class)
class ServiceableAreaServiceTest {

    @Autowired
    private ServiceableAreaService service;

    @Autowired
    private ServiceabilityRuleRepository serviceabilityRuleRepository;

    @Autowired
    private ServiceableAreaRepository serviceableAreaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createAndStatusChangesAreMirroredIntoServiceabilityRules() {
        ServiceableAreaUpsertRequest createRequest = new ServiceableAreaUpsertRequest();
        createRequest.setName("Sector 62");
        createRequest.setCity("Noida");
        createRequest.setState("UP");
        createRequest.setPincode("201309");
        createRequest.setPolygonJson("{\"type\":\"Polygon\"}");

        Long areaId = service.create(createRequest).getId();

        ServiceabilityRuleEntity createdRule = serviceabilityRuleRepository.findById(areaId).orElseThrow();
        assertThat(createdRule.getName()).isEqualTo("Sector 62");
        assertThat(createdRule.getPincode()).isEqualTo("201309");
        assertThat(createdRule.isActive()).isTrue();
        assertThat(createdRule.isDeleted()).isFalse();

        ServiceableAreaStatusRequest statusRequest = new ServiceableAreaStatusRequest();
        statusRequest.setActive(false);
        service.updateStatus(areaId, statusRequest);

        ServiceabilityRuleEntity disabledRule = serviceabilityRuleRepository.findById(areaId).orElseThrow();
        assertThat(disabledRule.isActive()).isFalse();

        service.delete(areaId);

        ServiceabilityRuleEntity deletedRule = serviceabilityRuleRepository.findById(areaId).orElseThrow();
        assertThat(deletedRule.isDeleted()).isTrue();
    }

    @Test
    void updateRewritesExistingServiceabilityRuleRow() {
        ServiceableAreaUpsertRequest createRequest = new ServiceableAreaUpsertRequest();
        createRequest.setName("Alpha");
        createRequest.setCity("Delhi");
        createRequest.setState("Delhi");
        createRequest.setPincode("110001");
        Long areaId = service.create(createRequest).getId();

        ServiceableAreaUpsertRequest updateRequest = new ServiceableAreaUpsertRequest();
        updateRequest.setName("Alpha Prime");
        updateRequest.setCity("New Delhi");
        updateRequest.setState("Delhi");
        updateRequest.setPincode("110002");
        updateRequest.setPolygonJson("{\"updated\":true}");

        service.update(areaId, updateRequest);

        ServiceabilityRuleEntity updatedRule = serviceabilityRuleRepository.findById(areaId).orElseThrow();
        assertThat(updatedRule.getName()).isEqualTo("Alpha Prime");
        assertThat(updatedRule.getCity()).isEqualTo("New Delhi");
        assertThat(updatedRule.getPincode()).isEqualTo("110002");
        assertThat(updatedRule.getPolygonJson()).isEqualTo("{\"updated\":true}");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void createRollsBackWhenMirroredRuleWriteFails() {
        jdbcTemplate.execute("DROP TABLE serviceability_rules");

        ServiceableAreaUpsertRequest request = new ServiceableAreaUpsertRequest();
        request.setName("Rollback Zone");
        request.setCity("Noida");
        request.setState("UP");
        request.setPincode("201301");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(serviceableAreaRepository.count()).isZero();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ServiceableAreaEntity.class, ServiceabilityRuleEntity.class})
    @EnableJpaRepositories(basePackageClasses = {ServiceableAreaRepository.class, ServiceabilityRuleRepository.class})
    @Import(ServiceableAreaService.class)
    static class JpaConfig {
    }
}
