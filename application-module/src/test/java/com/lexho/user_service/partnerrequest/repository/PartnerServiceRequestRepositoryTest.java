package com.lexho.user_service.partnerrequest.repository;

import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = PartnerServiceRequestRepositoryTest.JpaConfig.class)
class PartnerServiceRequestRepositoryTest {

    @Autowired
    private PartnerServiceRequestRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void repositorySupportsPartnerCartSubscriptionAndExpiryQueries() {
        PartnerServiceRequestEntity oldest = repository.save(request(701L, 901L, null, PartnerServiceRequestStatus.PENDING, LocalDateTime.now().minusMinutes(20)));
        PartnerServiceRequestEntity latest = repository.save(request(701L, 901L, null, PartnerServiceRequestStatus.ACCEPTED, LocalDateTime.now().plusMinutes(15)));
        PartnerServiceRequestEntity subscription = repository.save(request(702L, null, 3001L, PartnerServiceRequestStatus.PENDING, LocalDateTime.now().minusMinutes(5)));
        entityManager.flush();

        setCreatedAt(oldest.getId(), LocalDateTime.now().minusHours(2));
        setCreatedAt(latest.getId(), LocalDateTime.now().minusHours(1));
        entityManager.clear();

        Page<PartnerServiceRequestEntity> partnerHistory = repository.findByPartnerIdOrderByCreatedAtDesc(701L, PageRequest.of(0, 10));
        assertThat(partnerHistory.getContent()).extracting(PartnerServiceRequestEntity::getId)
                .containsExactly(latest.getId(), oldest.getId());

        Page<PartnerServiceRequestEntity> accepted = repository.findByPartnerIdAndStatusOrderByCreatedAtDesc(
                701L, PartnerServiceRequestStatus.ACCEPTED, PageRequest.of(0, 10)
        );
        assertThat(accepted.getContent()).extracting(PartnerServiceRequestEntity::getId)
                .containsExactly(latest.getId());

        assertThat(repository.findByIdAndPartnerId(latest.getId(), 701L))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(latest.getId()));
        assertThat(repository.findByIdAndPartnerIdForUpdate(latest.getId(), 701L))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(latest.getId()));
        assertThat(repository.findByIdAndPartnerId(latest.getId(), 999L)).isEmpty();

        assertThat(repository.findByCartIdAndStatus(901L, PartnerServiceRequestStatus.PENDING))
                .extracting(PartnerServiceRequestEntity::getId).containsExactly(oldest.getId());
        assertThat(repository.findByCartId(901L))
                .extracting(PartnerServiceRequestEntity::getId).containsExactlyInAnyOrder(oldest.getId(), latest.getId());

        assertThat(repository.findBySubscriptionVisitIdAndStatus(3001L, PartnerServiceRequestStatus.PENDING))
                .extracting(PartnerServiceRequestEntity::getId).containsExactly(subscription.getId());
        assertThat(repository.findBySubscriptionVisitId(3001L))
                .extracting(PartnerServiceRequestEntity::getId).containsExactly(subscription.getId());

        assertThat(repository.existsByCartIdAndPartnerIdAndStatus(901L, 701L, PartnerServiceRequestStatus.ACCEPTED)).isTrue();
        assertThat(repository.existsBySubscriptionVisitIdAndPartnerIdAndStatus(3001L, 702L, PartnerServiceRequestStatus.PENDING)).isTrue();
        assertThat(repository.countByCartIdAndStatus(901L, PartnerServiceRequestStatus.PENDING)).isEqualTo(1);
        assertThat(repository.countBySubscriptionVisitIdAndStatus(3001L, PartnerServiceRequestStatus.PENDING)).isEqualTo(1);

        List<PartnerServiceRequestEntity> expired = repository.findByStatusAndExpiresAtBefore(
                PartnerServiceRequestStatus.PENDING,
                LocalDateTime.now()
        );
        assertThat(expired).extracting(PartnerServiceRequestEntity::getId)
                .containsExactlyInAnyOrder(oldest.getId(), subscription.getId());
    }

    private void setCreatedAt(Long id, LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE partner_service_requests SET created_at = ? WHERE id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, id)
                .executeUpdate();
    }

    private PartnerServiceRequestEntity request(Long partnerId,
                                                Long cartId,
                                                Long subscriptionVisitId,
                                                PartnerServiceRequestStatus status,
                                                LocalDateTime expiresAt) {
        PartnerServiceRequestEntity entity = new PartnerServiceRequestEntity();
        entity.setPartnerId(partnerId);
        entity.setCartId(cartId == null ? 8000L + partnerId : cartId);
        entity.setSubscriptionVisitId(subscriptionVisitId);
        entity.setSubscriptionId(subscriptionVisitId == null ? null : 4000L + partnerId);
        entity.setCustomerId(5000L + partnerId);
        entity.setServiceId(6000L + partnerId);
        entity.setServiceType("INSTANT");
        entity.setQuantity(1);
        entity.setScheduledTime(LocalDateTime.now().plusHours(1));
        entity.setFullAddress("Block A");
        entity.setCity("Delhi");
        entity.setLatitude(new BigDecimal("28.6139000"));
        entity.setLongitude(new BigDecimal("77.2090000"));
        entity.setTargetPincode("110001");
        entity.setDispatchAttempt(1);
        entity.setDistanceKm(new BigDecimal("2.450"));
        entity.setEtaMinutes(12);
        entity.setExpiresAt(expiresAt);
        entity.setStatus(status);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = PartnerServiceRequestEntity.class)
    @EnableJpaRepositories(basePackageClasses = PartnerServiceRequestRepository.class)
    static class JpaConfig {
    }
}

