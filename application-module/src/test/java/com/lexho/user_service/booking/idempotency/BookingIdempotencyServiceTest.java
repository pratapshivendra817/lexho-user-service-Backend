package com.lexho.user_service.booking.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingIdempotencyServiceTest {

    @Mock
    private BookingIdempotencyRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void reserveShouldCreateCustomerScopedInProgressRecordOnMiss() {
        BookingIdempotencyService service = new BookingIdempotencyService(repository, objectMapper);
        when(repository.findByCustomerAndKeyForUpdate(10L, "key-1")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(BookingIdempotencyRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<?> replay = service.reserveOrReplay(10L, "key-1", "hash");

        assertThat(replay).isEmpty();
        verify(repository).saveAndFlush(any(BookingIdempotencyRecord.class));
    }

    @Test
    void reserveShouldReplayOriginalSuccessfulResultForSameCustomerKeyAndPayload() {
        BookingIdempotencyService service = new BookingIdempotencyService(repository, objectMapper);
        BookingIdempotencyRecord record = succeededRecord(10L, "key-1", "hash", 99L, Map.of("bookingId", 99L));
        when(repository.findByCustomerAndKeyForUpdate(10L, "key-1")).thenReturn(Optional.of(record));

        var response = service.reserveOrReplay(10L, "key-1", "hash").orElseThrow();

        assertThat(response.getData()).containsEntry("bookingId", 99);
    }

    @Test
    void reserveShouldRejectSameCustomerKeyWithDifferentPayload() {
        BookingIdempotencyService service = new BookingIdempotencyService(repository, objectMapper);
        BookingIdempotencyRecord record = succeededRecord(10L, "key-1", "old-hash", 99L, Map.of("bookingId", 99L));
        when(repository.findByCustomerAndKeyForUpdate(10L, "key-1")).thenReturn(Optional.of(record));

        CodedValidationException ex = assertThrows(
                CodedValidationException.class,
                () -> service.reserveOrReplay(10L, "key-1", "new-hash")
        );

        assertThat(ex.getErrorCode()).isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void sameRawKeyCanBeReservedByDifferentCustomersIndependently() {
        BookingIdempotencyService service = new BookingIdempotencyService(repository, objectMapper);
        when(repository.findByCustomerAndKeyForUpdate(10L, "shared-key")).thenReturn(Optional.empty());
        when(repository.findByCustomerAndKeyForUpdate(20L, "shared-key")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(BookingIdempotencyRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.reserveOrReplay(10L, "shared-key", "hash-a")).isEmpty();
        assertThat(service.reserveOrReplay(20L, "shared-key", "hash-b")).isEmpty();

        verify(repository).findByCustomerAndKeyForUpdate(10L, "shared-key");
        verify(repository).findByCustomerAndKeyForUpdate(20L, "shared-key");
    }

    @Test
    void markSucceededShouldPersistReplayPayloadOnlyForMatchingHash() {
        BookingIdempotencyService service = new BookingIdempotencyService(repository, objectMapper);
        BookingIdempotencyRecord record = inProgressRecord(10L, "key-1", "hash");
        when(repository.findByCustomerAndKeyForUpdate(10L, "key-1")).thenReturn(Optional.of(record));

        service.markSucceeded(10L, "key-1", "hash", 123L, Map.of("bookingId", 123L));

        assertThat(record.getStatus()).isEqualTo(BookingIdempotencyRecord.Status.SUCCEEDED);
        assertThat(record.getBookingId()).isEqualTo(123L);
        assertThat(record.getResponseJson()).contains("bookingId");
    }

    @Test
    void cleanupShouldDeleteExpiredRecords() {
        BookingIdempotencyService service = new BookingIdempotencyService(repository, objectMapper);
        when(repository.deleteExpired(any(LocalDateTime.class))).thenReturn(3);

        assertThat(service.cleanupExpiredRecords()).isEqualTo(3);
    }

    @Test
    void requestHashShouldIncludeCustomerAndMaterialPayload() {
        BookingIdempotencyService service = new BookingIdempotencyService(repository, objectMapper);
        LexhoDtos.BookingCreateRequest request = new LexhoDtos.BookingCreateRequest(
                1L, "addr-1", "INSTANT", null, null, null,
                10.0, 20.0, null, null, null, null, BigDecimal.TEN, null);

        String first = service.requestHash(10L, request);
        String same = service.requestHash(10L, request);
        String otherCustomer = service.requestHash(11L, request);

        assertThat(first).isEqualTo(same);
        assertThat(first).isNotEqualTo(otherCustomer);
    }

    private BookingIdempotencyRecord inProgressRecord(Long customerId, String key, String hash) {
        BookingIdempotencyRecord record = new BookingIdempotencyRecord();
        record.setCustomerId(customerId);
        record.setIdempotencyKey(key);
        record.setRequestHash(hash);
        record.setStatus(BookingIdempotencyRecord.Status.IN_PROGRESS);
        record.setExpiresAt(LocalDateTime.now().plusDays(7));
        return record;
    }

    private BookingIdempotencyRecord succeededRecord(Long customerId,
                                                     String key,
                                                     String hash,
                                                     Long bookingId,
                                                     Map<String, Object> payload) {
        BookingIdempotencyRecord record = inProgressRecord(customerId, key, hash);
        record.setStatus(BookingIdempotencyRecord.Status.SUCCEEDED);
        record.setBookingId(bookingId);
        try {
            record.setResponseJson(objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return record;
    }
}
