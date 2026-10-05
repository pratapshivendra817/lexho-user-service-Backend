package com.lexho.user_service.customer.repository;

import com.lexho.admin.entity.Admin;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminOtp;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.PopularServiceEntity;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = CustomerRepositorySliceTest.JpaConfig.class)
class CustomerRepositorySliceTest {

    @Autowired
    private CustomerCatalogRepository catalogRepository;

    @Autowired
    private CustomerCartItemRepository cartItemRepository;

    @Autowired
    private CustomerPopularServiceRepository popularServiceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void catalogCartAndPopularServiceQueriesReturnOnlyExpectedRecords() {
        AdminCategoryEntity cleaning = category("Cleaning", true);
        AdminCategoryEntity plumbing = category("Plumbing", true);

        AdminServiceEntity kitchen = service("Kitchen Cleaning", cleaning, true, false, "499");
        AdminServiceEntity sofa = service("Sofa Cleaning", cleaning, true, false, "699");
        AdminServiceEntity inactive = service("Deep Cleaning", cleaning, false, false, "999");
        service("Pipe Repair", plumbing, true, false, "399");

        popularServiceRepository.save(popular(sofa, cleaning, 2, true));
        popularServiceRepository.save(popular(kitchen, cleaning, 1, true));
        popularServiceRepository.save(popular(inactive, cleaning, 3, true));

        cartItemRepository.save(cartItem(101L, kitchen.getId(), "NONE", 1));
        cartItemRepository.save(cartItem(101L, sofa.getId(), "ADDON-A", 2));
        cartItemRepository.save(cartItem(102L, sofa.getId(), "NONE", 1));

        assertThat(catalogRepository.findByCategory_IdAndActiveTrueAndDeletedFalseOrderByNameAscIdAsc(
                cleaning.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(AdminServiceEntity::getName)
                .containsExactly("Kitchen Cleaning", "Sofa Cleaning");

        assertThat(popularServiceRepository.findActivePopularServicesByCategory(cleaning.getId(), PageRequest.of(0, 10)))
                .extracting(CustomerPopularServiceView::getName)
                .containsExactly("Kitchen Cleaning", "Sofa Cleaning");

        assertThat(cartItemRepository.findByUserIdAndServiceId(101L, kitchen.getId()))
                .hasValueSatisfying(item -> assertThat(item.getAddonSignature()).isEqualTo("NONE"));
        assertThat(cartItemRepository.findByUserIdAndServiceIdAndAddonSignature(101L, sofa.getId(), "ADDON-A"))
                .hasValueSatisfying(item -> assertThat(item.getQuantity()).isEqualTo(2));
        assertThat(cartItemRepository.findByUserId(101L)).hasSize(2);
        assertThat(cartItemRepository.countByUserId(101L)).isEqualTo(2);

        cartItemRepository.deleteByUserId(101L);
        assertThat(cartItemRepository.findByUserId(101L)).isEmpty();

        cartItemRepository.saveAndFlush(cartItem(103L, sofa.getId(), "ADDON-DUP", 1));
        assertThatThrownBy(() -> cartItemRepository.saveAndFlush(cartItem(103L, sofa.getId(), "ADDON-DUP", 5)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AdminCategoryEntity category(String name, boolean active) {
        AdminCategoryEntity category = new AdminCategoryEntity();
        category.setName(name);
        category.setIconUrl(name.toLowerCase());
        category.setActive(active);
        category.setDeleted(false);
        category.setDisplayOrder(1);
        entityManager.persist(category);
        return category;
    }

    private AdminServiceEntity service(String name, AdminCategoryEntity category, boolean active, boolean deleted, String price) {
        AdminServiceEntity service = new AdminServiceEntity();
        service.setName(name);
        service.setCategory(category);
        service.setDescription(name + " desc");
        service.setServiceType("STANDARD");
        service.setBasePrice(new BigDecimal(price));
        service.setDiscountedPrice(new BigDecimal(price));
        service.setDurationMinutes(60);
        service.setAvgRating(4.5);
        service.setTotalBookings(10L);
        service.setTotalReviews(2L);
        service.setImageUrl(name.toLowerCase());
        service.setNew(false);
        service.setActive(active);
        service.setDeleted(deleted);
        entityManager.persist(service);
        return service;
    }

    private PopularServiceEntity popular(AdminServiceEntity service, AdminCategoryEntity category, int priority, boolean active) {
        PopularServiceEntity entity = new PopularServiceEntity();
        entity.setService(service);
        entity.setCategory(category);
        entity.setPriority(priority);
        entity.setActive(active);
        return entity;
    }

    private CustomerCartItemEntity cartItem(Long userId, Long serviceId, String addonSignature, int quantity) {
        CustomerCartItemEntity item = new CustomerCartItemEntity();
        item.setUserId(userId);
        item.setServiceId(serviceId);
        item.setAddonSignature(addonSignature);
        item.setQuantity(quantity);
        item.setFamilySize("MEDIUM");
        item.setFamilySizeLabel("Medium");
        item.setFamilySizeId(1L);
        return item;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            AdminCategoryEntity.class,
            AdminServiceEntity.class,
            PopularServiceEntity.class,
            CustomerCartItemEntity.class,
            AdminOtp.class,
            Admin.class
    })
    @EnableJpaRepositories(
            basePackageClasses = {CustomerCatalogRepository.class, CustomerCartItemRepository.class, CustomerPopularServiceRepository.class},
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CustomerCatalogRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CustomerCartItemRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CustomerPopularServiceRepository.class)
            }
    )
    static class JpaConfig {
    }
}

