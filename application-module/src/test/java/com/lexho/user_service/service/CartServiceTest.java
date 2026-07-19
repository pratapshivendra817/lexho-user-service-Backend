package com.lexho.user_service.service;

import com.lexho.booking.entity.Cart;
import com.lexho.booking.repository.CartRepository;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.dto.CartRequest;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock CartRepository cartRepository;
    @Mock PartnerRepository partnerRepository;
    private CartService service;

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepository, partnerRepository);
        ReflectionTestUtils.setField(service, "mediaPublicBaseUrl", "https://cdn.test/");
    }

    @Test
    void addToCartCreatesNewItemAndNormalizesLocalImage() {
        Partner partner = partner(9L, "Cleaner", 250.0, "http://localhost:8080/uploads/a.png");
        CartRequest request = new CartRequest();
        request.setPartnerId(9L);
        request.setQuantity(2);
        when(partnerRepository.findById(9L)).thenReturn(Optional.of(partner));
        when(cartRepository.findByUserIdAndPartnerId(3L, 9L)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            ReflectionTestUtils.setField(cart, "id", 12L);
            return cart;
        });

        var result = service.addToCart(3L, request);

        assertEquals(12L, result.getId());
        assertEquals(2, result.getQuantity());
        assertEquals("https://cdn.test/uploads/a.png", result.getImageUrl());
    }

    @Test
    void addToCartRejectsInvalidQuantityAndMissingPartner() {
        CartRequest request = new CartRequest();
        request.setPartnerId(9L);
        request.setQuantity(0);
        assertThrows(IllegalArgumentException.class, () -> service.addToCart(3L, request));

        request.setQuantity(1);
        when(partnerRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.addToCart(3L, request));
    }

    @Test
    void getCartCalculatesTotalAndUsesDefaultImage() {
        Cart cart = cart(1L, 3L, 9L, 100.0, 3);
        when(cartRepository.findByUserId(3L)).thenReturn(List.of(cart));
        when(partnerRepository.findById(9L)).thenReturn(Optional.of(partner(9L, "Cleaner", 100.0, null)));

        var result = service.getCart(3L);

        assertEquals(300.0, result.getTotal());
        assertEquals("https://cdn.test/defaults/service.png", result.getItems().get(0).getImageUrl());
    }

    @Test
    void removeFromCartChecksOwnership() {
        Cart cart = cart(1L, 4L, 9L, 100.0, 1);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        assertThrows(UnauthorizedException.class, () -> service.removeFromCart(3L, 1L));

        assertEquals("Removed successfully", service.removeFromCart(4L, 1L));
        verify(cartRepository).delete(cart);
    }

    private Partner partner(Long id, String name, Double price, String image) {
        Partner partner = new Partner();
        partner.setId(id); partner.setName(name); partner.setPrice(price); partner.setImage(image);
        return partner;
    }

    private Cart cart(Long id, Long userId, Long partnerId, Double price, Integer quantity) {
        Cart cart = new Cart();
        ReflectionTestUtils.setField(cart, "id", id);
        cart.setUserId(userId); cart.setPartnerId(partnerId);
        cart.setPartnerName("Cleaner"); cart.setPrice(price); cart.setQuantity(quantity);
        return cart;
    }
}
