package com.lexho.user_service.cartconfig.service;

import com.lexho.user_service.cartconfig.dto.CartConfigRequest;
import com.lexho.user_service.cartconfig.dto.CartConfigResponse;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cartconfig.repository.CartConfigRepository;
import com.lexho.user_service.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CartConfigService {
    private static final Long CONFIG_ID = 1L;
    private final CartConfigRepository cartConfigRepository;

    public CartConfigService(CartConfigRepository cartConfigRepository) {
        this.cartConfigRepository = cartConfigRepository;
    }

    @Transactional(readOnly = true)
    public CartConfigEntity currentEntity() {
        return cartConfigRepository.findById(CONFIG_ID).orElseGet(CartConfigEntity::new);
    }

    @Transactional(readOnly = true)
    public CartConfigResponse current() {
        return toResponse(currentEntity());
    }

    @Transactional
    public CartConfigResponse update(CartConfigRequest request) {
        validate(request);
        CartConfigEntity entity = cartConfigRepository.findById(CONFIG_ID).orElseGet(CartConfigEntity::new);
        entity.setId(CONFIG_ID);
        entity.setMaxCartItems(request.getMaxCartItems());
        entity.setMinServiceQuantity(request.getMinServiceQuantity());
        entity.setMaxServiceQuantity(request.getMaxServiceQuantity());
        entity.setMinCleaningDurationHours(request.getMinCleaningDurationHours());
        entity.setMaxCleaningDurationHours(request.getMaxCleaningDurationHours());
        entity.setInstantBookingWindowMinutes(request.getInstantBookingWindowMinutes());
        entity.setScheduledBookingWindowDays(request.getScheduledBookingWindowDays());
        entity.setSlotIntervalMinutes(request.getSlotIntervalMinutes());
        entity.setInstantBookingEnabled(request.getInstantBookingEnabled());
        entity.setScheduledBookingEnabled(request.getScheduledBookingEnabled());
        entity.setSubscriptionBookingEnabled(request.getSubscriptionBookingEnabled());
        entity.setRequireSlotRecheck(request.getRequireSlotRecheck());
        entity.setRejectFullSlots(request.getRejectFullSlots());
        entity.setMaxBookingsPerSlot(request.getMaxBookingsPerSlot());
        entity.setSubscriptionCheckoutEnabled(request.getSubscriptionCheckoutEnabled());
        entity.setSubscriptionPaymentRequired(request.getSubscriptionPaymentRequired());
        entity.setSubscriptionAutoRenewEnabled(request.getSubscriptionAutoRenewEnabled());
        return toResponse(cartConfigRepository.save(entity));
    }

    private void validate(CartConfigRequest request) {
        if (request.getMaxServiceQuantity() < request.getMinServiceQuantity()) {
            throw new BadRequestException("maxServiceQuantity must be greater than or equal to minServiceQuantity");
        }
        if (request.getMaxCleaningDurationHours().compareTo(request.getMinCleaningDurationHours()) < 0) {
            throw new BadRequestException("maxCleaningDurationHours must be greater than or equal to minCleaningDurationHours");
        }
        if (!Boolean.TRUE.equals(request.getInstantBookingEnabled())
                && !Boolean.TRUE.equals(request.getScheduledBookingEnabled())
                && !Boolean.TRUE.equals(request.getSubscriptionBookingEnabled())) {
            throw new BadRequestException("At least one booking mode must be enabled");
        }
    }

    public CartConfigResponse toResponse(CartConfigEntity entity) {
        CartConfigResponse response = new CartConfigResponse();
        response.setMaxCartItems(entity.getMaxCartItems());
        response.setMinServiceQuantity(entity.getMinServiceQuantity());
        response.setMaxServiceQuantity(entity.getMaxServiceQuantity());
        response.setMinCleaningDurationHours(strip(entity.getMinCleaningDurationHours()));
        response.setMaxCleaningDurationHours(strip(entity.getMaxCleaningDurationHours()));
        response.setInstantBookingWindowMinutes(entity.getInstantBookingWindowMinutes());
        response.setScheduledBookingWindowDays(entity.getScheduledBookingWindowDays());
        response.setSlotIntervalMinutes(entity.getSlotIntervalMinutes());
        response.setInstantBookingEnabled(entity.getInstantBookingEnabled());
        response.setScheduledBookingEnabled(entity.getScheduledBookingEnabled());
        response.setSubscriptionBookingEnabled(entity.getSubscriptionBookingEnabled());
        response.setRequireSlotRecheck(entity.getRequireSlotRecheck());
        response.setRejectFullSlots(entity.getRejectFullSlots());
        response.setMaxBookingsPerSlot(entity.getMaxBookingsPerSlot());
        response.setSubscriptionCheckoutEnabled(entity.getSubscriptionCheckoutEnabled());
        response.setSubscriptionPaymentRequired(entity.getSubscriptionPaymentRequired());
        response.setSubscriptionAutoRenewEnabled(entity.getSubscriptionAutoRenewEnabled());
        return response;
    }

    private BigDecimal strip(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}
