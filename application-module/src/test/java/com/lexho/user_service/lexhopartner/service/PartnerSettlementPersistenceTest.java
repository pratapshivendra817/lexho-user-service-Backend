package com.lexho.user_service.lexhopartner.service;

import com.lexho.admin.entity.Admin;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.lexhopartner.config.PartnerSettlementProperties;
import com.lexho.user_service.lexhopartner.entity.BaseEntity;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.entity.PartnerSettlementRefund;
import com.lexho.user_service.lexhopartner.entity.PartnerWithdrawalRequest;
import com.lexho.user_service.lexhopartner.entity.PaymentMode;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerSettlementRefundRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWithdrawalRequestRepository;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@Import(PartnerSettlementService.class)
@ContextConfiguration(classes = PartnerSettlementPersistenceTest.JpaConfig.class)
class PartnerSettlementPersistenceTest {

    @Autowired
    private PartnerSettlementService service;

    @Autowired
    private PartnerOrderRepository partnerOrderRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentEnsurePayoutCallsPersistOnlyOnePayoutForSameBooking() throws Exception {
        payoutRepository.deleteAll();
        partnerOrderRepository.deleteAll();
        PartnerOrder order = partnerOrderRepository.saveAndFlush(completedOrder());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        new TransactionTemplate(transactionManager)
                                .executeWithoutResult(status -> service.ensurePayout(order.getId()));
                        return null;
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                }));
            }

            ready.await();
            start.countDown();

            List<Throwable> outcomes = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                outcomes.add(future.get());
            }

            assertThat(outcomes)
                    .allSatisfy(throwable -> {
                        if (throwable != null) {
                            assertThat(throwable).isInstanceOf(UnexpectedRollbackException.class);
                        }
                    });
            assertThat(outcomes.stream().filter(UnexpectedRollbackException.class::isInstance).count())
                    .isLessThanOrEqualTo(1);

            List<Payout> payouts = payoutRepository.findAll();
            assertThat(payouts).hasSize(1);
            assertThat(payouts.get(0).getReferenceBookingId()).isEqualTo(order.getId());
            assertThat(payouts.get(0).getProviderId()).isEqualTo(90L);
        } finally {
            executor.shutdownNow();
        }
    }

    private PartnerOrder completedOrder() {
        PartnerOrder order = new PartnerOrder();
        order.setCustomerName("Customer");
        order.setCustomerPhone("9999999999");
        order.setAddress("Test address");
        order.setLatitude(28.6139);
        order.setLongitude(77.2090);
        order.setServiceType("Cleaning");
        order.setAmount(1200.0);
        order.setPaymentMode(PaymentMode.ONLINE);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.COMPLETED);
        order.setAssignedPartnerId(90L);
        order.setCompletedAt(LocalDateTime.now());
        return order;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(PartnerSettlementProperties.class)
    @EntityScan(basePackageClasses = {
            Admin.class,
            BaseEntity.class,
            PartnerOrder.class,
            Payout.class,
            PartnerWithdrawalRequest.class,
            PartnerSettlementRefund.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            PartnerOrderRepository.class,
            PayoutRepository.class,
            PartnerWithdrawalRequestRepository.class,
            PartnerSettlementRefundRepository.class
    }, includeFilters = {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PartnerOrderRepository.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PayoutRepository.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PartnerWithdrawalRequestRepository.class),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PartnerSettlementRefundRepository.class)
    })
    static class JpaConfig {
    }
}
