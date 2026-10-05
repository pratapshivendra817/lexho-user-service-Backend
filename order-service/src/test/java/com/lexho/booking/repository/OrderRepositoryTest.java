package com.lexho.booking.repository;

import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user.entity.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledIfSystemProperty(named = "lexho.it.mysql.order", matches = "true")
@DataJpaTest(properties = {
        "spring.datasource.url=${IT_DB_URL:}",
        "spring.datasource.username=${IT_DB_USERNAME:}",
        "spring.datasource.password=${IT_DB_PASSWORD:}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@ContextConfiguration(classes = OrderRepositoryTest.TestJpaApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @BeforeAll
    static void requireExplicitIsolatedMysqlConfiguration() {
        String url = System.getProperty("IT_DB_URL", System.getenv().getOrDefault("IT_DB_URL", ""));
        String username = System.getProperty("IT_DB_USERNAME", System.getenv().getOrDefault("IT_DB_USERNAME", ""));
        assumeTrue(!url.isBlank(), "Set IT_DB_URL for isolated MySQL order repository testing");
        assumeTrue(url.contains("lexho_order_test"),
                "IT_DB_URL must point to the isolated lexho_order_test database");
        assumeTrue(!username.isBlank(), "Set IT_DB_USERNAME for isolated MySQL order repository testing");
    }

    @Autowired
    private OrderRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void searchOrdersAndRecentUserLookupUsePersistedRows() {
        Order matching = order(77L, 450.0, OrderStatus.CONFIRMED, PaymentStatus.PENDING, LocalDateTime.now().plusDays(1));
        repository.saveAndFlush(matching);
        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?", LocalDateTime.now().minusHours(2), matching.getId());

        Order latest = order(77L, 600.0, OrderStatus.CONFIRMED, PaymentStatus.SUCCESS, LocalDateTime.now().plusDays(2));
        repository.saveAndFlush(latest);
        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?", LocalDateTime.now().minusHours(1), latest.getId());

        Order other = order(88L, 200.0, OrderStatus.CANCELLED, PaymentStatus.FAILED, LocalDateTime.now().plusDays(3));
        repository.saveAndFlush(other);
        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?", LocalDateTime.now().minusMinutes(30), other.getId());

        List<Order> found = repository.searchOrders("77", OrderStatus.CONFIRMED, PageRequest.of(0, 10)).getContent();

        assertThat(found).extracting(Order::getUserId).containsOnly(77L);
        assertThat(repository.findTopByUserIdOrderByCreatedAtDesc(77L)).map(Order::getId).contains(latest.getId());
        assertThat(repository.findByIdForUpdate(latest.getId())).map(Order::getId).contains(latest.getId());
        assertThat(repository.countByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusIn(
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(10),
                Set.of(OrderStatus.CONFIRMED)
        )).isEqualTo(2);
    }

    @Test
    void revenueQueriesAggregateSuccessfulPayments() {
        repository.saveAndFlush(order(10L, 300.0, OrderStatus.DELIVERED, PaymentStatus.SUCCESS, LocalDateTime.now().plusDays(1)));
        repository.saveAndFlush(order(11L, 450.0, OrderStatus.DELIVERED, PaymentStatus.SUCCESS, LocalDateTime.now().plusDays(2)));
        repository.saveAndFlush(order(12L, 200.0, OrderStatus.CANCELLED, PaymentStatus.FAILED, LocalDateTime.now().plusDays(3)));

        assertThat(repository.sumCompletedRevenue()).isEqualTo(750.0);

        List<Object[]> monthlyRevenue = repository.monthlyRevenue(LocalDateTime.now().minusMonths(1));
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        assertThat(monthlyRevenue).hasSize(1);
        assertThat(monthlyRevenue.get(0)[0]).isEqualTo(currentMonth);
        assertThat(((Number) monthlyRevenue.get(0)[1]).doubleValue()).isEqualTo(750.0);
    }

    private Order order(Long userId,
                        Double totalAmount,
                        OrderStatus status,
                        PaymentStatus paymentStatus,
                        LocalDateTime scheduledAt) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setAddress("Test address");
        order.setScheduledAt(scheduledAt);
        order.setBookingType("INSTANT");
        return order;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {Order.class, OrderItem.class, User.class, ServiceCategory.class})
    @EnableJpaRepositories(
            basePackageClasses = OrderRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OrderRepository.class)
    )
    static class TestJpaApplication {
    }
}
