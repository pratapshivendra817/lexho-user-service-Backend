package com.lexho.user_service.addon.repository;

import com.lexho.user_service.addon.entity.AddonEntity;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = AddonRepositoryTest.JpaConfig.class)
class AddonRepositoryTest {

    @Autowired
    private AddonRepository repository;

    @Test
    void searchAndActiveIdQueriesReturnExpectedAddons() {
        AddonEntity mop = repository.save(addon("Mop", "floor cleaning", true, false, 1, 1));
        AddonEntity detergent = repository.save(addon("Detergent", "extra soap", true, true, 1, 3));
        AddonEntity gloves = repository.save(addon("Gloves", "kitchen use", false, true, 2, 5));

        Page<AddonEntity> search = repository.findByAddonNameContainingIgnoreCase("det", PageRequest.of(0, 10));
        assertThat(search.getContent()).extracting(AddonEntity::getAddonName).containsExactly("Detergent");

        List<AddonEntity> active = repository.findByIdInAndActiveTrue(List.of(mop.getId(), detergent.getId(), gloves.getId()));
        assertThat(active).extracting(AddonEntity::getAddonName).containsExactlyInAnyOrder("Mop", "Detergent");

        List<AddonEntity> all = repository.findByIdIn(List.of(mop.getId(), detergent.getId(), gloves.getId()));
        assertThat(all).hasSize(3);
        assertThat(detergent.getMaxQuantity()).isEqualTo(3);
        assertThat(mop.getMaxQuantity()).isEqualTo(1);
    }

    private AddonEntity addon(String name, String description, boolean active, boolean quantityAllowed, int min, int max) {
        AddonEntity entity = new AddonEntity();
        entity.setAddonName(name);
        entity.setDescription(description);
        entity.setPrice(new BigDecimal("99.00"));
        entity.setActive(active);
        entity.setQuantityAllowed(quantityAllowed);
        entity.setMinQuantity(min);
        entity.setMaxQuantity(max);
        entity.setRequired(false);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = AddonEntity.class)
    @EnableJpaRepositories(
            basePackageClasses = AddonRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AddonRepository.class)
    )
    static class JpaConfig {
    }
}

