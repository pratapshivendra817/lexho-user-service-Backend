package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.CookCoverage;
import com.lexho.user_service.subscription.entity.CookCoverageItem;
import com.lexho.user_service.subscription.entity.CookSubscriptionPricing;
import com.lexho.user_service.subscription.entity.SubscriptionDurationPrice;
import com.lexho.user_service.subscription.entity.SubscriptionFrequency;
import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.enums.VisitStatus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = SubscriptionRepositorySliceTest.JpaConfig.class)
class SubscriptionRepositorySliceTest {

    @Autowired
    private SubscriptionMasterRepository masterRepository;

    @Autowired
    private SubscriptionVisitRepository visitRepository;

    @Autowired
    private SubscriptionFrequencyRepository frequencyRepository;

    @Autowired
    private SubscriptionDurationPriceRepository durationPriceRepository;

    @Autowired
    private CookSubscriptionPricingRepository cookPricingRepository;

    @Autowired
    private CookCoverageRepository cookCoverageRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void subscriptionMasterVisitAndFrequencyQueriesSupportCoreFlows() {
        SubscriptionMaster active = masterRepository.save(subscription(501L, 1001L, SubscriptionStatus.ACTIVE, 9001L));
        SubscriptionMaster paused = masterRepository.save(subscription(501L, 1002L, SubscriptionStatus.PAUSED, 9002L));
        SubscriptionMaster other = masterRepository.save(subscription(502L, 1003L, SubscriptionStatus.ACTIVE, 9003L));

        SubscriptionVisit past = visitRepository.save(visit(active.getId(), LocalDate.now().minusDays(1), VisitStatus.SCHEDULED));
        SubscriptionVisit future1 = visitRepository.save(visit(active.getId(), LocalDate.now().plusDays(1), VisitStatus.SCHEDULED));
        SubscriptionVisit future2 = visitRepository.save(visit(active.getId(), LocalDate.now().plusDays(2), VisitStatus.SCHEDULED));
        SubscriptionVisit completed = visitRepository.save(visit(active.getId(), LocalDate.now().plusDays(3), VisitStatus.COMPLETED));

        frequencyRepository.save(frequency(active.getId(), "MONDAY"));
        frequencyRepository.save(frequency(active.getId(), "WEDNESDAY"));
        frequencyRepository.save(frequency(other.getId(), "FRIDAY"));

        assertThat(masterRepository.findByCustomerIdOrderByIdDesc(501L))
                .extracting(SubscriptionMaster::getId)
                .containsExactly(paused.getId(), active.getId());
        assertThat(masterRepository.findByStatusOrderByIdDesc(SubscriptionStatus.ACTIVE))
                .extracting(SubscriptionMaster::getId)
                .containsExactly(other.getId(), active.getId());
        assertThat(masterRepository.findByCustomerIdAndStatusOrderByIdDesc(501L, SubscriptionStatus.ACTIVE, PageRequest.of(0, 10)).getContent())
                .extracting(SubscriptionMaster::getId)
                .containsExactly(active.getId());
        assertThat(masterRepository.findByBookingIdAndCustomerId(9001L, 501L)).contains(active);

        assertThat(visitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(active.getId()))
                .extracting(SubscriptionVisit::getId)
                .containsExactly(past.getId(), future1.getId(), future2.getId(), completed.getId());
        assertThat(visitRepository.findBySubscriptionIdInAndStatusInOrderByVisitDateAscIdAsc(
                List.of(active.getId(), other.getId()), List.of(VisitStatus.SCHEDULED)
        )).hasSize(3);
        assertThat(visitRepository.findByIdAndSubscriptionId(past.getId(), active.getId())).contains(past);
        assertThat(visitRepository.existsBySubscriptionIdAndVisitDateAndIdNot(active.getId(), past.getVisitDate(), future1.getId())).isTrue();
        assertThat(visitRepository.countBySubscriptionIdAndStatus(active.getId(), VisitStatus.SCHEDULED)).isEqualTo(3);
        assertThat(visitRepository.cancelFutureScheduled(active.getId(), VisitStatus.SCHEDULED, VisitStatus.CANCELLED, LocalDate.now()))
                .isEqualTo(2);
        entityManager.clear();
        assertThat(visitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(active.getId()))
                .extracting(SubscriptionVisit::getStatus)
                .containsExactly(VisitStatus.SCHEDULED, VisitStatus.CANCELLED, VisitStatus.CANCELLED, VisitStatus.COMPLETED);

        assertThat(frequencyRepository.findBySubscriptionIdOrderByIdAsc(active.getId()))
                .extracting(SubscriptionFrequency::getDayName)
                .containsExactly("MONDAY", "WEDNESDAY");
        assertThat(frequencyRepository.findBySubscriptionIdInOrderBySubscriptionIdAscIdAsc(List.of(active.getId(), other.getId())))
                .hasSize(3);
    }

    @Test
    void pricingAndCoverageQueriesReturnServiceSpecificAndDefaultData() {
        durationPriceRepository.save(duration(null, "1.00", "199.00", true));
        durationPriceRepository.save(duration(11L, "2.00", "399.00", true));
        durationPriceRepository.save(duration(11L, "3.00", "499.00", false));

        cookPricingRepository.save(cookPrice(11L, FamilySize.SMALL, "299.00", true));
        cookPricingRepository.save(cookPrice(11L, FamilySize.LARGE, "499.00", true));
        cookPricingRepository.save(cookPrice(12L, FamilySize.SMALL, "199.00", false));

        CookCoverage serviceCoverage = cookCoverageRepository.save(coverage(11L, FamilySize.SMALL, 2, true, "Family", "Members"));
        cookCoverageRepository.save(coverage(null, FamilySize.LARGE, 1, true, "Default", "Reference"));
        cookCoverageRepository.save(coverage(11L, FamilySize.MEDIUM, 3, false, "Inactive", "Reference"));

        assertThat(durationPriceRepository.findByActiveTrueOrderByServiceIdAscDurationHoursAsc()).hasSize(2);
        assertThat(durationPriceRepository.findByServiceIdAndActiveTrueOrderByDurationHoursAsc(11L))
                .extracting(SubscriptionDurationPrice::getDurationHours)
                .containsExactly(new BigDecimal("2.00"));
        assertThat(durationPriceRepository.findByServiceIdIsNullAndDurationHoursAndActiveTrue(new BigDecimal("1.00"))).isPresent();

        assertThat(cookPricingRepository.findByServiceIdAndFamilySizeAndActiveTrue(11L, FamilySize.SMALL)).isPresent();
        assertThat(cookPricingRepository.findByServiceIdAndActiveTrueOrderByFamilySizeAsc(11L))
                .extracting(CookSubscriptionPricing::getFamilySize)
                .containsExactly(FamilySize.LARGE, FamilySize.SMALL);
        assertThat(cookPricingRepository.findByActiveTrueOrderByServiceIdAscFamilySizeAsc()).hasSize(2);

        assertThat(cookCoverageRepository.findByServiceIdAndActiveTrueOrderBySortOrderAscFamilySizeAsc(11L))
                .extracting(CookCoverage::getFamilySize)
                .containsExactly(FamilySize.SMALL);
        assertThat(cookCoverageRepository.findByServiceIdIsNullAndActiveTrueOrderBySortOrderAscFamilySizeAsc())
                .extracting(CookCoverage::getFamilySize)
                .containsExactly(FamilySize.LARGE);
        assertThat(cookCoverageRepository.findById(serviceCoverage.getId()))
                .hasValueSatisfying(found -> assertThat(found.getItems()).hasSize(2));

        assertThatThrownBy(() -> cookPricingRepository.saveAndFlush(cookPrice(11L, FamilySize.SMALL, "399.00", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cookCoverageItemForeignKeyRejectsOrphanCoverageReference() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO cook_coverage_item (coverage_id, icon, name, quantity, sort_order)
                VALUES (?, ?, ?, ?, ?)
                """,
                999999L,
                "utensils",
                "Orphan item",
                1,
                1
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private SubscriptionMaster subscription(Long customerId, Long serviceId, SubscriptionStatus status, Long bookingId) {
        SubscriptionMaster subscription = new SubscriptionMaster();
        subscription.setCustomerId(customerId);
        subscription.setServiceId(serviceId);
        subscription.setAddressId(3001L);
        subscription.setBookingId(bookingId);
        subscription.setBookingMode("APP");
        subscription.setServiceType("COOK");
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));
        subscription.setVisits(12);
        subscription.setDurationHours(new BigDecimal("2.00"));
        subscription.setFamilySize(FamilySize.SMALL);
        subscription.setPerVisitPrice(new BigDecimal("250.00"));
        subscription.setTimeSlot("08:00-10:00");
        subscription.setPrice(new BigDecimal("3000.00"));
        subscription.setStatus(status);
        return subscription;
    }

    private SubscriptionVisit visit(Long subscriptionId, LocalDate date, VisitStatus status) {
        SubscriptionVisit visit = new SubscriptionVisit();
        visit.setSubscriptionId(subscriptionId);
        visit.setVisitDate(date);
        visit.setTimeSlot("08:00-10:00");
        visit.setStatus(status);
        return visit;
    }

    private SubscriptionFrequency frequency(Long subscriptionId, String dayName) {
        SubscriptionFrequency frequency = new SubscriptionFrequency();
        frequency.setSubscriptionId(subscriptionId);
        frequency.setDayName(dayName);
        return frequency;
    }

    private SubscriptionDurationPrice duration(Long serviceId, String durationHours, String price, boolean active) {
        SubscriptionDurationPrice entity = new SubscriptionDurationPrice();
        entity.setServiceId(serviceId);
        entity.setDurationHours(new BigDecimal(durationHours));
        entity.setPerVisitPrice(new BigDecimal(price));
        entity.setActive(active);
        return entity;
    }

    private CookSubscriptionPricing cookPrice(Long serviceId, FamilySize familySize, String price, boolean active) {
        CookSubscriptionPricing entity = new CookSubscriptionPricing();
        entity.setServiceId(serviceId);
        entity.setFamilySize(familySize);
        entity.setPricePerVisit(new BigDecimal(price));
        entity.setActive(active);
        return entity;
    }

    private CookCoverage coverage(Long serviceId, FamilySize familySize, int sortOrder, boolean active, String label, String reference) {
        CookCoverage coverage = new CookCoverage();
        coverage.setServiceId(serviceId);
        coverage.setFamilySize(familySize);
        coverage.setMembers(label);
        coverage.setFamilySizeLabel(label);
        coverage.setReference(reference);
        coverage.setSortOrder(sortOrder);
        coverage.setActive(active);

        CookCoverageItem first = new CookCoverageItem();
        first.setName("Utensils");
        first.setIcon("utensils");
        first.setSortOrder(1);
        first.setQuantity(1);
        coverage.addItem(first);

        CookCoverageItem second = new CookCoverageItem();
        second.setName("Meals");
        second.setIcon("meal");
        second.setSortOrder(2);
        second.setQuantity(2);
        coverage.addItem(second);
        return coverage;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            SubscriptionMaster.class,
            SubscriptionVisit.class,
            SubscriptionFrequency.class,
            SubscriptionDurationPrice.class,
            CookSubscriptionPricing.class,
            CookCoverage.class,
            CookCoverageItem.class
    })
    @EnableJpaRepositories(
            basePackageClasses = {
                    SubscriptionMasterRepository.class,
                    SubscriptionVisitRepository.class,
                    SubscriptionFrequencyRepository.class,
                    SubscriptionDurationPriceRepository.class,
                    CookSubscriptionPricingRepository.class,
                    CookCoverageRepository.class
            },
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SubscriptionMasterRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SubscriptionVisitRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SubscriptionFrequencyRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SubscriptionDurationPriceRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CookSubscriptionPricingRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CookCoverageRepository.class)
            }
    )
    static class JpaConfig {
    }
}

