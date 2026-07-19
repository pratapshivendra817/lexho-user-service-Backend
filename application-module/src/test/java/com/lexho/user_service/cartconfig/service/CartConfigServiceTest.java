package com.lexho.user_service.cartconfig.service;

import com.lexho.user_service.cartconfig.dto.CartConfigRequest;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cartconfig.repository.CartConfigRepository;
import com.lexho.user_service.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartConfigServiceTest {
    @Mock CartConfigRepository repository;
    @InjectMocks CartConfigService service;

    @Test
    void currentReturnsDefaultsWhenConfigurationDoesNotExist() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        var response = service.current();
        assertEquals(10, response.getMaxCartItems());
        assertEquals(new BigDecimal("0.5"), response.getMinCleaningDurationHours());
        assertTrue(response.getInstantBookingEnabled());
    }

    @Test
    void updatePersistsAllConfigurationValues() {
        CartConfigRequest request = validRequest();
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(request);

        assertEquals(20, response.getMaxCartItems());
        assertEquals(6, response.getMaxServiceQuantity());
        assertFalse(response.getInstantBookingEnabled());
        verify(repository).save(argThat(entity -> entity.getId().equals(1L)));
    }

    @Test
    void updateRejectsInvalidQuantityDurationAndDisabledModes() {
        CartConfigRequest invalidQuantity = validRequest();
        invalidQuantity.setMaxServiceQuantity(1);
        assertThrows(BadRequestException.class, () -> service.update(invalidQuantity));

        CartConfigRequest invalidDuration = validRequest();
        invalidDuration.setMaxCleaningDurationHours(BigDecimal.ONE);
        invalidDuration.setMinCleaningDurationHours(BigDecimal.TEN);
        assertThrows(BadRequestException.class, () -> service.update(invalidDuration));

        CartConfigRequest disabled = validRequest();
        disabled.setInstantBookingEnabled(false);
        disabled.setScheduledBookingEnabled(false);
        disabled.setSubscriptionBookingEnabled(false);
        assertThrows(BadRequestException.class, () -> service.update(disabled));
        verify(repository, never()).save(any());
    }

    @Test
    void toResponseHandlesNullDurations() {
        CartConfigEntity entity = new CartConfigEntity();
        entity.setMinCleaningDurationHours(null);
        entity.setMaxCleaningDurationHours(null);
        assertNull(service.toResponse(entity).getMinCleaningDurationHours());
    }

    private CartConfigRequest validRequest() {
        CartConfigRequest request = new CartConfigRequest();
        request.setMaxCartItems(20); request.setMinServiceQuantity(2); request.setMaxServiceQuantity(6);
        request.setMinCleaningDurationHours(new BigDecimal("1.00"));
        request.setMaxCleaningDurationHours(new BigDecimal("8.00"));
        request.setInstantBookingWindowMinutes(20); request.setScheduledBookingWindowDays(7);
        request.setSlotIntervalMinutes(15); request.setInstantBookingEnabled(false);
        request.setScheduledBookingEnabled(true); request.setSubscriptionBookingEnabled(true);
        request.setRequireSlotRecheck(true); request.setRejectFullSlots(true); request.setMaxBookingsPerSlot(2);
        request.setSubscriptionCheckoutEnabled(true); request.setSubscriptionPaymentRequired(true);
        request.setSubscriptionAutoRenewEnabled(false);
        return request;
    }
}
