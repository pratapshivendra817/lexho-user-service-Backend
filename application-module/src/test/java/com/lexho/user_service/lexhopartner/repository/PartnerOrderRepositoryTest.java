package com.lexho.user_service.lexhopartner.repository;

import com.lexho.common.enums.PaymentStatus;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.entity.PaymentMode;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = PartnerOrderRepositoryTest.JpaConfig.class)
class PartnerOrderRepositoryTest {

    @Autowired
    private PartnerOrderRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void repositoryQueriesFilterUnassignedAssignedAndWindowedRevenue() {
        PartnerOrder created = repository.save(order("Asha", 1200.0, OrderStatus.CREATED, null));
        PartnerOrder assigned = repository.save(order("Neha", 2200.0, OrderStatus.ASSIGNED, 44L));
        PartnerOrder completedRecent = repository.save(order("Riya", 1800.0, OrderStatus.COMPLETED, 44L));
        PartnerOrder completedOld = repository.save(order("Meera", 900.0, OrderStatus.COMPLETED, 44L));
        repository.save(order("Tina", 700.0, OrderStatus.REJECTED, 88L));
        entityManager.flush();

        setUpdatedAt(created.getId(), LocalDateTime.now().minusHours(5));
        setUpdatedAt(assigned.getId(), LocalDateTime.now().minusHours(4));
        setUpdatedAt(completedRecent.getId(), LocalDateTime.now().minusHours(1));
        setUpdatedAt(completedOld.getId(), LocalDateTime.now().minusDays(2));
        entityManager.clear();

        Page<PartnerOrder> unassigned = repository.findByStatusInAndAssignedPartnerIdIsNull(
                List.of(OrderStatus.CREATED, OrderStatus.ASSIGNED),
                PageRequest.of(0, 10)
        );
        assertThat(unassigned.getContent()).extracting(PartnerOrder::getCustomerName)
                .containsExactly("Asha");

        Page<PartnerOrder> assignedTo44 = repository.findByAssignedPartnerId(44L, PageRequest.of(0, 10));
        assertThat(assignedTo44.getContent()).extracting(PartnerOrder::getCustomerName)
                .containsExactlyInAnyOrder("Neha", "Riya", "Meera");

        assertThat(repository.countByAssignedPartnerIdAndStatus(44L, OrderStatus.COMPLETED)).isEqualTo(2);
        assertThat(repository.countByAssignedPartnerId(44L)).isEqualTo(3);
        assertThat(repository.sumAmountByPartnerAndStatus(44L, OrderStatus.COMPLETED)).isEqualTo(2700.0);
        assertThat(repository.countByPartnerAndStatus(44L, OrderStatus.COMPLETED)).isEqualTo(2L);

        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().plusMinutes(5);

        assertThat(repository.sumAmountByPartnerAndStatusAndUpdatedAtBetween(44L, OrderStatus.COMPLETED, start, end))
                .isEqualTo(1800.0);
        assertThat(repository.countByPartnerAndStatusAndUpdatedAtBetween(44L, OrderStatus.COMPLETED, start, end))
                .isEqualTo(1L);

        Page<PartnerOrder> windowed = repository.findByAssignedPartnerIdAndStatusAndUpdatedAtBetween(
                44L,
                OrderStatus.COMPLETED,
                start,
                end,
                PageRequest.of(0, 10)
        );
        assertThat(windowed.getContent()).extracting(PartnerOrder::getCustomerName)
                .containsExactly("Riya");
    }

    private void setUpdatedAt(Long orderId, LocalDateTime updatedAt) {
        entityManager.createNativeQuery("UPDATE orders SET updated_at = ? WHERE id = ?")
                .setParameter(1, updatedAt)
                .setParameter(2, orderId)
                .executeUpdate();
    }

    private PartnerOrder order(String customerName, Double amount, OrderStatus status, Long assignedPartnerId) {
        PartnerOrder entity = new PartnerOrder();
        entity.setCustomerName(customerName);
        entity.setCustomerPhone("9999999999");
        entity.setAddress("Sector 21");
        entity.setLatitude(28.61);
        entity.setLongitude(77.20);
        entity.setServiceType("Cleaning");
        entity.setAmount(amount);
        entity.setPaymentMode(PaymentMode.CASH);
        entity.setPaymentStatus(PaymentStatus.PENDING);
        entity.setStatus(status);
        entity.setAssignedPartnerId(assignedPartnerId);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = PartnerOrder.class)
    @EnableJpaRepositories(basePackageClasses = PartnerOrderRepository.class)
    static class JpaConfig {
    }
}

