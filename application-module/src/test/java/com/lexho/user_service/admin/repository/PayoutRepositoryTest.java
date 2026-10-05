package com.lexho.user_service.admin.repository;

import com.lexho.admin.entity.Admin;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import com.lexho.user_service.admin.entity.AdminOtp;
import com.lexho.user_service.admin.entity.Payout;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = PayoutRepositoryTest.JpaConfig.class)
class PayoutRepositoryTest {

    @Autowired
    private PayoutRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void payoutQueriesSupportProviderSearchWindowAndLatestBookingLookup() {
        Payout latest = repository.save(payout(9001L, 1201L, "PAID", 1100.0));
        Payout another = repository.save(payout(9002L, 1202L, "FAILED", 750.0));
        Payout providerPeer = repository.save(payout(9003L, 1201L, "PENDING", 1000.0));
        entityManager.flush();

        setCreatedAt(latest.getId(), LocalDateTime.now().minusHours(2));
        setCreatedAt(another.getId(), LocalDateTime.now().minusHours(1));
        setCreatedAt(providerPeer.getId(), LocalDateTime.now().minusDays(2));
        entityManager.clear();

        assertThat(repository.findTopByReferenceBookingIdOrderByCreatedAtDesc(9001L))
                .hasValueSatisfying(found -> assertThat(found.getStatus()).isEqualTo("PAID"));
        assertThat(repository.findByIdAndProviderId(latest.getId(), 1201L)).isPresent();
        assertThat(repository.findByIdAndProviderId(latest.getId(), 9999L)).isEmpty();

        Page<Payout> providerPage = repository.findByProviderId(1201L, PageRequest.of(0, 10));
        assertThat(providerPage.getContent()).extracting(Payout::getReferenceBookingId)
                .containsExactlyInAnyOrder(9001L, 9003L);

        Page<Payout> window = repository.findByProviderIdAndCreatedAtBetween(
                1201L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                PageRequest.of(0, 10)
        );
        assertThat(window.getContent()).extracting(Payout::getStatus).containsExactly("PAID");

        assertThat(repository.findByProviderId(1201L)).hasSize(2);

        Page<Payout> search = repository.search("1201", "paid", PageRequest.of(0, 10));
        assertThat(search.getContent()).extracting(Payout::getStatus).containsExactly("PAID");
        assertThat(repository.search("9999", "paid", PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void payoutRejectsDuplicateReferenceBookingIdAtDatabaseLevel() {
        repository.saveAndFlush(payout(9010L, 1201L, "PENDING", 1000.0));

        assertThatThrownBy(() -> repository.saveAndFlush(payout(9010L, 1202L, "PAID", 1100.0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void setCreatedAt(Long payoutId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE payouts SET created_at = ? WHERE id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, payoutId)
                .executeUpdate();
    }

    private Payout payout(Long bookingId, Long providerId, String status, Double amount) {
        Payout entity = new Payout();
        entity.setReferenceBookingId(bookingId);
        entity.setProviderId(providerId);
        entity.setStatus(status);
        entity.setAmount(amount);
        entity.setGrossAmount(new BigDecimal(amount.toString()));
        entity.setCommissionPercent(new BigDecimal("10.00"));
        entity.setCommissionAmount(new BigDecimal("100.00"));
        entity.setReversedAmount(BigDecimal.ZERO);
        entity.setCurrency("INR");
        entity.setRemarks("auto");
        entity.setProcessedAt(LocalDateTime.now());
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {Payout.class, AdminOtp.class, Admin.class})
    @EnableJpaRepositories(
            basePackageClasses = PayoutRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PayoutRepository.class)
    )
    static class JpaConfig {
    }
}

