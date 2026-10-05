package com.lexho.user_service.integration;

import com.lexho.testconfig.LexhoInfrastructureTest;
import com.lexho.admin.entity.Admin;
import com.lexho.user_service.admin.dto.PopularServiceUpsertRequest;
import com.lexho.user_service.admin.entity.ActivityLog;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.PopularServiceEntity;
import com.lexho.user_service.admin.repository.ActivityLogRepository;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.PopularServiceRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.admin.service.PopularServiceManagementService;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PopularServiceSoftDeleteRestoreTest.JpaTestConfig.class)
@LexhoInfrastructureTest
@ActiveProfiles("it")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PopularServiceSoftDeleteRestoreTest {

    @Autowired
    private PopularServiceManagementService popularServiceManagementService;
    @Autowired
    private PopularServiceRepository popularServiceRepository;
    @Autowired
    private AdminCategoryRepository adminCategoryRepository;
    @Autowired
    private AdminServiceEntityRepository adminServiceEntityRepository;
    @Autowired
    private ActivityLogRepository activityLogRepository;

    @MockitoBean
    private SecurityUtils securityUtils;
    @MockitoBean
    private MediaUrlResolver mediaUrlResolver;

    @Test
    void softDeleteThenReAddShouldReactivateSameRow() {
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(999L));
        when(mediaUrlResolver.toPublicUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminCategoryEntity category = new AdminCategoryEntity();
        category.setName("Cleaning");
        category.setActive(true);
        category.setDeleted(false);
        category = adminCategoryRepository.save(category);

        AdminServiceEntity service = new AdminServiceEntity();
        service.setName("Sofa Cleaning");
        service.setCategory(category);
        service.setBasePrice(BigDecimal.valueOf(499));
        service.setAvgRating(4.5);
        service.setImageUrl("/uploads/services/sofa.jpg");
        service.setActive(true);
        service.setDeleted(false);
        service = adminServiceEntityRepository.save(service);

        PopularServiceUpsertRequest request = new PopularServiceUpsertRequest();
        request.setServiceId(service.getId());
        request.setCategoryId(category.getId());
        request.setPriority(1);

        Long firstId = popularServiceManagementService.addPopularService(request).getData().getId();
        assertThat(firstId).isEqualTo(service.getId());

        PopularServiceEntity created = popularServiceRepository
                .findByServiceIdAndCategoryId(service.getId(), category.getId())
                .orElseThrow();
        Long rowId = created.getId();
        assertThat(created.isActive()).isTrue();

        popularServiceManagementService.deletePopularService(rowId, "seasonal cleanup");
        PopularServiceEntity inactive = popularServiceRepository.findById(rowId).orElseThrow();
        assertThat(inactive.isActive()).isFalse();

        Long secondId = popularServiceManagementService.addPopularService(request).getData().getId();
        assertThat(secondId).isEqualTo(service.getId());

        PopularServiceEntity reactivated = popularServiceRepository.findById(rowId).orElseThrow();
        assertThat(reactivated.isActive()).isTrue();
        assertThat(reactivated.getId()).isEqualTo(rowId);
        assertThat(activityLogRepository.findAll())
                .extracting(ActivityLog::getAction)
                .containsExactly("POPULAR_SERVICE_CREATE", "POPULAR_SERVICE_DELETE", "POPULAR_SERVICE_REACTIVATE");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            Admin.class,
            ActivityLog.class,
            AdminCategoryEntity.class,
            AdminServiceEntity.class,
            PopularServiceEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            ActivityLogRepository.class,
            AdminCategoryRepository.class,
            AdminServiceEntityRepository.class,
            PopularServiceRepository.class
    })
    @Import({PopularServiceManagementService.class, AdminAuditService.class})
    static class JpaTestConfig {
    }
}

