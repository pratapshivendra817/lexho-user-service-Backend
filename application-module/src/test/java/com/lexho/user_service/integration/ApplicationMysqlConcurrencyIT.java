package com.lexho.user_service.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "lexho.it.mysql.concurrency", matches = "true")
class ApplicationMysqlConcurrencyIT {

    private static final MySqlIntegrationSettings SETTINGS = MySqlIntegrationSettings.load();

    @BeforeAll
    static void requireExplicitIsolatedMysqlConfiguration() throws Exception {
        SETTINGS.requireIsolatedDatabase("lexho_application_concurrency_test", "concurrency testing");
        SETTINGS.resetDatabase();
    }

    @Test
    void concurrentPartnerAcceptanceUsesMysqlRowLockAndAllowsOnlyOneAssignment() throws Exception {
        migrateCleanSchema();

        long orderId;
        try (Connection connection = connection()) {
            orderId = insertOrder(connection, "SEARCHING_PARTNER", "INSTANT", null);
            insertPartnerShift(connection, 501L);
            insertPartnerShift(connection, 502L);
        }

        List<AssignmentResult> results = runTwoThreads(
                () -> acceptPartner(orderId, 501L),
                () -> acceptPartner(orderId, 502L)
        );

        assertThat(results).extracting(AssignmentResult::accepted).containsExactlyInAnyOrder(true, false);

        try (Connection connection = connection()) {
            OrderSnapshot order = findOrder(connection, orderId);
            assertThat(order.status()).isEqualTo("PARTNER_ASSIGNED");
            assertThat(order.assignedPartnerId()).isIn(501L, 502L);
            assertThat(activeJobs(connection, 501L) + activeJobs(connection, 502L)).isEqualTo(1);
            assertThat(activeJobs(connection, order.assignedPartnerId())).isEqualTo(1);
        }
    }

    @Test
    void concurrentTerminalBookingTransitionsSerializeOnMysqlRowLock() throws Exception {
        migrateCleanSchema();

        long orderId;
        try (Connection connection = connection()) {
            orderId = insertOrder(connection, "SERVICE_IN_PROGRESS", "INSTANT", 701L);
        }

        List<TransitionResult> results = runTwoThreads(
                () -> transitionTerminalStatus(orderId, "SERVICE_COMPLETED"),
                () -> transitionTerminalStatus(orderId, "CANCELLED")
        );

        assertThat(results).extracting(TransitionResult::changed).containsExactlyInAnyOrder(true, false);

        try (Connection connection = connection()) {
            OrderSnapshot order = findOrder(connection, orderId);
            assertThat(order.status()).isIn("SERVICE_COMPLETED", "CANCELLED");
            assertThat(countBookingEvents(connection, orderId)).isEqualTo(1);
        }
    }

    @Test
    void concurrentBusinessFlowLoadProfileStaysWithinBackendLatencyThresholds() throws Exception {
        migrateCleanSchema();

        int threads = 8;
        int flowsPerThread = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ConcurrentLinkedQueue<Long> durationsMillis = new ConcurrentLinkedQueue<>();
        AtomicInteger failures = new AtomicInteger();
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (int thread = 0; thread < threads; thread++) {
                final int threadIndex = thread;
                futures.add(executor.submit(loadWorker(threadIndex, flowsPerThread, durationsMillis, failures)));
            }
            for (Future<Void> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        List<Long> sortedDurations = new ArrayList<>(durationsMillis);
        Collections.sort(sortedDurations);

        assertThat(failures).hasValue(0);
        assertThat(sortedDurations).hasSize(threads * flowsPerThread);
        assertThat(percentile(sortedDurations, 95)).isLessThan(1000L);
        assertThat(percentile(sortedDurations, 99)).isLessThan(2500L);

        try (Connection connection = connection()) {
            assertThat(countRows(connection, "orders")).isEqualTo(threads * flowsPerThread);
            assertThat(countRows(connection, "payment_transactions")).isEqualTo(threads * flowsPerThread);
            assertThat(countRows(connection, "partner_service_requests")).isEqualTo(threads * flowsPerThread);
            assertThat(countRows(connection, "payouts")).isEqualTo(threads * flowsPerThread);
        }
    }

    private void migrateCleanSchema() {
        Flyway flyway = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(SETTINGS.url(), SETTINGS.username(), SETTINGS.password())
                .locations("classpath:db/migration/application-module", "classpath:db/migration/user-service")
                .load();
        flyway.clean();
        flyway.migrate();
    }

    private Callable<Void> loadWorker(int threadIndex,
                                      int flowsPerThread,
                                      ConcurrentLinkedQueue<Long> durationsMillis,
                                      AtomicInteger failures) {
        return () -> {
            for (int flow = 0; flow < flowsPerThread; flow++) {
                long started = System.nanoTime();
                try {
                    runBusinessFlow(threadIndex, flow);
                    durationsMillis.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                } catch (Exception exception) {
                    failures.incrementAndGet();
                    throw exception;
                }
            }
            return null;
        };
    }

    private void runBusinessFlow(int threadIndex, int flowIndex) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                long orderId = insertOrder(connection, "BOOKING_CREATED", "INSTANT", null);
                insertPartnerServiceRequest(connection, orderId, 900L + threadIndex);
                insertPaymentTransaction(connection, orderId, "load-" + threadIndex + "-" + flowIndex);
                updateOrderStatus(connection, orderId, "SERVICE_COMPLETED");
                insertPayout(connection, orderId, 900L + threadIndex);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private AssignmentResult acceptPartner(long orderId, long partnerId) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                OrderSnapshot order = lockOrder(connection, orderId);
                if (order.assignedPartnerId() != null) {
                    connection.rollback();
                    return new AssignmentResult(false, partnerId);
                }

                updateOrderAssignment(connection, orderId, partnerId);
                incrementActiveJobs(connection, partnerId);
                insertBookingEvent(connection, orderId, "PARTNER_ACCEPTED");
                connection.commit();
                return new AssignmentResult(true, partnerId);
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private TransitionResult transitionTerminalStatus(long orderId, String targetStatus) throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                OrderSnapshot order = lockOrder(connection, orderId);
                if (!"SERVICE_IN_PROGRESS".equals(order.status())) {
                    connection.rollback();
                    return new TransitionResult(false, targetStatus);
                }

                updateOrderStatus(connection, orderId, targetStatus);
                insertBookingEvent(connection, orderId, targetStatus);
                connection.commit();
                return new TransitionResult(true, targetStatus);
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(SETTINGS.url(), SETTINGS.username(), SETTINGS.password());
    }

    private long insertOrder(Connection connection, String status, String bookingType, Long assignedPartnerId)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO orders (
                    user_id, total_amount, status, payment_status, address, scheduled_at, created_at, updated_at,
                    customer_name, customer_phone, latitude, longitude, service_type, amount, payment_mode,
                    assigned_partner_id, booking_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, 77L);
            statement.setDouble(2, 1200.0);
            statement.setString(3, status);
            statement.setString(4, "PENDING");
            statement.setString(5, "Concurrency Test Address");
            statement.setTimestamp(6, Timestamp.valueOf(now.plusDays(1)));
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setString(9, "Customer");
            statement.setString(10, "9999999999");
            statement.setDouble(11, 28.61);
            statement.setDouble(12, 77.20);
            statement.setString(13, "Cleaning");
            statement.setDouble(14, 1200.0);
            statement.setString(15, "ONLINE");
            if (assignedPartnerId == null) {
                statement.setNull(16, java.sql.Types.BIGINT);
            } else {
                statement.setLong(16, assignedPartnerId);
            }
            statement.setString(17, bookingType);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }

    private void insertPartnerShift(Connection connection, long partnerId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO partner_shifts (
                    partner_id, current_pincode, is_online, active_jobs_count, max_active_jobs,
                    deleted, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, partnerId);
            statement.setString(2, "110001");
            statement.setBoolean(3, true);
            statement.setInt(4, 0);
            statement.setInt(5, 2);
            statement.setBoolean(6, false);
            statement.setTimestamp(7, Timestamp.valueOf(now));
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    private void insertPartnerServiceRequest(Connection connection, long orderId, long partnerId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO partner_service_requests (
                    partner_id, cart_id, customer_id, service_id, quantity, scheduled_time, full_address, city,
                    latitude, longitude, status, target_pincode, dispatch_attempt, distance_km, eta_minutes, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, partnerId);
            statement.setLong(2, orderId);
            statement.setLong(3, 77L);
            statement.setLong(4, 55L);
            statement.setInt(5, 1);
            statement.setTimestamp(6, Timestamp.valueOf(now.plusHours(1)));
            statement.setString(7, "Concurrency Test Address");
            statement.setString(8, "Delhi");
            statement.setBigDecimal(9, new java.math.BigDecimal("28.6100000"));
            statement.setBigDecimal(10, new java.math.BigDecimal("77.2000000"));
            statement.setString(11, "PENDING");
            statement.setString(12, "110001");
            statement.setInt(13, 1);
            statement.setBigDecimal(14, new java.math.BigDecimal("1.250"));
            statement.setInt(15, 8);
            statement.setTimestamp(16, Timestamp.valueOf(now.plusMinutes(2)));
            statement.executeUpdate();
        }
    }

    private void insertPaymentTransaction(Connection connection, long orderId, String idempotencyKey) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO payment_transactions (
                    booking_id, customer_id, idempotency_key, razorpay_order_id,
                    amount, currency, payment_status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, orderId);
            statement.setLong(2, 77L);
            statement.setString(3, idempotencyKey);
            statement.setString(4, "order-" + idempotencyKey);
            statement.setBigDecimal(5, new java.math.BigDecimal("1200.00"));
            statement.setString(6, "INR");
            statement.setString(7, "SUCCESS");
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    private void insertPayout(Connection connection, long orderId, long partnerId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO payouts (
                    provider_id, amount, status, reference_booking_id, remarks, processed_at,
                    gross_amount, commission_percent, commission_amount, currency, reversed_amount, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, partnerId);
            statement.setDouble(2, 1020.0);
            statement.setString(3, "COMPLETED");
            statement.setLong(4, orderId);
            statement.setString(5, "load profile");
            statement.setTimestamp(6, Timestamp.valueOf(now));
            statement.setBigDecimal(7, new java.math.BigDecimal("1200.00"));
            statement.setBigDecimal(8, new java.math.BigDecimal("15.00"));
            statement.setBigDecimal(9, new java.math.BigDecimal("180.00"));
            statement.setString(10, "INR");
            statement.setBigDecimal(11, new java.math.BigDecimal("0.00"));
            statement.setTimestamp(12, Timestamp.valueOf(now));
            statement.executeUpdate();
        }
    }

    private OrderSnapshot lockOrder(Connection connection, long orderId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT status, assigned_partner_id FROM orders WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                Object assignedPartnerId = resultSet.getObject("assigned_partner_id");
                return new OrderSnapshot(
                        resultSet.getString("status"),
                        assignedPartnerId == null ? null : ((Number) assignedPartnerId).longValue()
                );
            }
        }
    }

    private OrderSnapshot findOrder(Connection connection, long orderId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT status, assigned_partner_id FROM orders WHERE id = ?")) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                Object assignedPartnerId = resultSet.getObject("assigned_partner_id");
                return new OrderSnapshot(
                        resultSet.getString("status"),
                        assignedPartnerId == null ? null : ((Number) assignedPartnerId).longValue()
                );
            }
        }
    }

    private void updateOrderAssignment(Connection connection, long orderId, long partnerId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                UPDATE orders
                SET status = 'PARTNER_ASSIGNED', assigned_partner_id = ?, sla_minutes = 30,
                    promised_start_time = ?, updated_at = ?
                WHERE id = ?
                """)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setLong(1, partnerId);
            statement.setTimestamp(2, Timestamp.valueOf(now.plusMinutes(30)));
            statement.setTimestamp(3, Timestamp.valueOf(now));
            statement.setLong(4, orderId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private void updateOrderStatus(Connection connection, long orderId, String status) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE orders SET status = ?, updated_at = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(3, orderId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private void incrementActiveJobs(Connection connection, long partnerId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                UPDATE partner_shifts
                SET active_jobs_count = active_jobs_count + 1, updated_at = ?
                WHERE partner_id = ?
                """)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(2, partnerId);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private int activeJobs(Connection connection, long partnerId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT active_jobs_count FROM partner_shifts WHERE partner_id = ?")) {
            statement.setLong(1, partnerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getInt("active_jobs_count");
            }
        }
    }

    private void insertBookingEvent(Connection connection, long orderId, String eventType) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO booking_events (booking_id, event_type, event_payload, created_at)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setLong(1, orderId);
            statement.setString(2, eventType);
            statement.setString(3, "{\"source\":\"mysql-concurrency-it\"}");
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        }
    }

    private long countBookingEvents(Connection connection, long orderId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM booking_events WHERE booking_id = ?")) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getLong(1);
            }
        }
    }

    private long countRows(Connection connection, String tableName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getLong(1);
        }
    }

    private long percentile(List<Long> sortedValues, int percentile) {
        assertThat(sortedValues).isNotEmpty();
        int index = (int) Math.ceil((percentile / 100.0d) * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    private <T> List<T> runTwoThreads(ThrowingSupplier<T> first, ThrowingSupplier<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> firstFuture = executor.submit(() -> awaitStartAndRun(ready, start, first));
            Future<T> secondFuture = executor.submit(() -> awaitStartAndRun(ready, start, second));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    firstFuture.get(20, TimeUnit.SECONDS),
                    secondFuture.get(20, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private <T> T awaitStartAndRun(CountDownLatch ready, CountDownLatch start, ThrowingSupplier<T> supplier)
            throws Exception {
        ready.countDown();
        assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
        return supplier.get();
    }

    private record AssignmentResult(boolean accepted, long partnerId) {
    }

    private record TransitionResult(boolean changed, String targetStatus) {
    }

    private record OrderSnapshot(String status, Long assignedPartnerId) {
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
