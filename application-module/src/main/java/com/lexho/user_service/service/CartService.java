package com.lexho.user_service.service;

import com.lexho.booking.entity.Cart;
import com.lexho.booking.repository.CartRepository;
import com.lexho.partner.entity.Partner;
import com.lexho.partner.repository.PartnerRepository;
import com.lexho.user_service.dto.CartRequest;
import com.lexho.user_service.dto.CartResponseDTO;
import com.lexho.user_service.dto.CartSummaryDTO;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final PartnerRepository partnerRepo;

    @Transactional
    public CartResponseDTO addToCart(Long userId, CartRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Partner partner = partnerRepo.findById(request.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        Cart cart = cartRepo.findByUserIdAndPartnerId(userId, partner.getId())
                .orElseGet(Cart::new);

        if (cart.getId() == null) {
            cart.setUserId(userId);
            cart.setPartnerId(partner.getId());
            cart.setQuantity(request.getQuantity());
        } else {
            cart.setQuantity(cart.getQuantity() + request.getQuantity());
        }

        cart.setPartnerName(partner.getName());
        cart.setPrice(partner.getPrice());

        Cart saved = cartRepo.save(cart);

        return new CartResponseDTO(
                saved.getId(),
                saved.getPartnerId(),
                saved.getPartnerName(),
                saved.getPrice(),
                saved.getQuantity()
        );
    }

    public CartSummaryDTO getCart(Long userId) {
        List<Cart> cartList = cartRepo.findByUserId(userId);

        double total = cartList.stream()
                .mapToDouble(c -> c.getPrice() * c.getQuantity())
                .sum();

        List<CartResponseDTO> items = cartList.stream()
                .map(cart -> new CartResponseDTO(
                        cart.getId(),
                        cart.getPartnerId(),
                        cart.getPartnerName(),
                        cart.getPrice(),
                        cart.getQuantity()
                ))
                .toList();

        return new CartSummaryDTO(items, total);
    }

    public String removeFromCart(Long userId, Long cartId) {
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!userId.equals(cart.getUserId())) {
            throw new UnauthorizedException("You are not allowed to remove this cart item");
        }
        cartRepo.delete(cart);
        return "Removed successfully";
    }
}

