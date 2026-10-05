package com.lexho.user_service.booking.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.exception.CodedValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class BookingIdempotencyService {
    private static final Logger LOG = LoggerFactory.getLogger(BookingIdempotencyService.class);
    private static final int MAX_KEY_LENGTH = 100;
    private static final int RETENTION_DAYS = 7;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final BookingIdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public BookingIdempotencyService(BookingIdempotencyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > MAX_KEY_LENGTH) {
            throw new CodedValidationException("IDEMPOTENCY_KEY_INVALID",
                    "Idempotency-Key must not exceed 100 characters", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    public String requestHash(Long customerId, Object request) {
        try {
            String canonical = customerId + "|" + objectMapper.writeValueAsString(request);
            return sha256(canonical);
        } catch (JsonProcessingException ex) {
            throw new CodedValidationException("IDEMPOTENCY_REQUEST_INVALID",
                    "Unable to process idempotent booking request", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ApiResponse<Map<String, Object>>> reserveOrReplay(Long customerId,
                                                                      String idempotencyKey,
                                                                      String requestHash) {
        BookingIdempotencyRecord existing = repository.findByCustomerAndKeyForUpdate(customerId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            validateHash(existing, requestHash);
            if (existing.getStatus() == BookingIdempotencyRecord.Status.SUCCEEDED) {
                LOG.info("Booking idempotency hit customerId={} bookingId={}", customerId, existing.getBookingId());
                return Optional.of(replay(existing));
            }
            throw new CodedValidationException("IDEMPOTENCY_REQUEST_IN_PROGRESS",
                    "A booking request with this Idempotency-Key is still in progress", HttpStatus.CONFLICT);
        }

        BookingIdempotencyRecord record = new BookingIdempotencyRecord();
        record.setCustomerId(customerId);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(BookingIdempotencyRecord.Status.IN_PROGRESS);
        record.setExpiresAt(LocalDateTime.now().plusDays(RETENTION_DAYS));
        try {
            repository.saveAndFlush(record);
            LOG.info("Booking idempotency miss customerId={}", customerId);
            return Optional.empty();
        } catch (DataIntegrityViolationException ex) {
            throw new CodedValidationException("IDEMPOTENCY_REQUEST_IN_PROGRESS",
                    "A booking request with this Idempotency-Key is still in progress", HttpStatus.CONFLICT);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSucceeded(Long customerId,
                              String idempotencyKey,
                              String requestHash,
                              Long bookingId,
                              Map<String, Object> payload) {
        BookingIdempotencyRecord record = repository.findByCustomerAndKeyForUpdate(customerId, idempotencyKey)
                .orElseThrow(() -> new CodedValidationException("IDEMPOTENCY_RECORD_MISSING",
                        "Idempotency record was not found", HttpStatus.CONFLICT));
        validateHash(record, requestHash);
        record.setStatus(BookingIdempotencyRecord.Status.SUCCEEDED);
        record.setBookingId(bookingId);
        record.setResponseJson(toJson(payload));
        record.setExpiresAt(LocalDateTime.now().plusDays(RETENTION_DAYS));
        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearInProgress(Long customerId, String idempotencyKey) {
        if (idempotencyKey != null) {
            repository.deleteInProgress(customerId, idempotencyKey);
        }
    }

    @Transactional
    public int cleanupExpiredRecords() {
        return repository.deleteExpired(LocalDateTime.now());
    }

    private void validateHash(BookingIdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new CodedValidationException("IDEMPOTENCY_CONFLICT",
                    "Idempotency-Key was already used for a different booking request", HttpStatus.CONFLICT);
        }
    }

    private ApiResponse<Map<String, Object>> replay(BookingIdempotencyRecord record) {
        try {
            Map<String, Object> payload = objectMapper.readValue(record.getResponseJson(), MAP_TYPE);
            return ApiResponse.success("Booking created", payload);
        } catch (JsonProcessingException ex) {
            throw new CodedValidationException("IDEMPOTENCY_REPLAY_FAILED",
                    "Unable to replay idempotent booking response", HttpStatus.CONFLICT);
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new CodedValidationException("IDEMPOTENCY_RESPONSE_INVALID",
                    "Unable to persist idempotent booking response", HttpStatus.CONFLICT);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
