package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.SubscriptionPartnerAssignment;
import com.lexho.user_service.subscription.entity.SubscriptionVisitPartnerAssignment;
import com.lexho.user_service.subscription.enums.PartnerAssignmentStatus;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = SubscriptionAssignmentRepositoryTest.JpaConfig.class)
class SubscriptionAssignmentRepositoryTest {

    @Autowired
    private SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository;

    @Autowired
    private SubscriptionVisitPartnerAssignmentRepository subscriptionVisitPartnerAssignmentRepository;

    @Test
    void assignmentRepositoriesSupportLookupOrderingAndUniqueness() {
        SubscriptionPartnerAssignment assignment = new SubscriptionPartnerAssignment();
        assignment.setSubscriptionId(101L);
        assignment.setPartnerId(201L);
        assignment.setStatus(PartnerAssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now().minusDays(1));
        subscriptionPartnerAssignmentRepository.saveAndFlush(assignment);

        SubscriptionVisitPartnerAssignment visitB = visitAssignment(101L, 5002L, 201L, PartnerAssignmentStatus.ASSIGNED);
        SubscriptionVisitPartnerAssignment visitA = visitAssignment(101L, 5001L, 202L, PartnerAssignmentStatus.REPLACED);
        subscriptionVisitPartnerAssignmentRepository.saveAndFlush(visitB);
        subscriptionVisitPartnerAssignmentRepository.saveAndFlush(visitA);

        assertThat(subscriptionPartnerAssignmentRepository.findBySubscriptionId(101L))
                .contains(assignment);
        assertThat(subscriptionVisitPartnerAssignmentRepository.findByVisitId(5001L))
                .contains(visitA);
        assertThat(subscriptionVisitPartnerAssignmentRepository.findBySubscriptionIdOrderByVisitIdAsc(101L))
                .extracting(SubscriptionVisitPartnerAssignment::getVisitId)
                .containsExactly(5001L, 5002L);

        SubscriptionPartnerAssignment duplicateSubscription = new SubscriptionPartnerAssignment();
        duplicateSubscription.setSubscriptionId(101L);
        duplicateSubscription.setPartnerId(999L);
        duplicateSubscription.setStatus(PartnerAssignmentStatus.ASSIGNED);

        assertThatThrownBy(() -> subscriptionPartnerAssignmentRepository.saveAndFlush(duplicateSubscription))
                .isInstanceOf(DataIntegrityViolationException.class);

        SubscriptionVisitPartnerAssignment duplicateVisit = visitAssignment(101L, 5001L, 203L, PartnerAssignmentStatus.ASSIGNED);
        assertThatThrownBy(() -> subscriptionVisitPartnerAssignmentRepository.saveAndFlush(duplicateVisit))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private SubscriptionVisitPartnerAssignment visitAssignment(Long subscriptionId,
                                                               Long visitId,
                                                               Long partnerId,
                                                               PartnerAssignmentStatus status) {
        SubscriptionVisitPartnerAssignment entity = new SubscriptionVisitPartnerAssignment();
        entity.setSubscriptionId(subscriptionId);
        entity.setVisitId(visitId);
        entity.setPartnerId(partnerId);
        entity.setStatus(status);
        entity.setReason("route update");
        entity.setAssignedAt(LocalDateTime.now());
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            SubscriptionPartnerAssignment.class,
            SubscriptionVisitPartnerAssignment.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            SubscriptionPartnerAssignmentRepository.class,
            SubscriptionVisitPartnerAssignmentRepository.class
    })
    static class JpaConfig {
    }
}

