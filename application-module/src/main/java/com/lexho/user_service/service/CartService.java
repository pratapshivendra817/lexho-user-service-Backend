package com.lexho.user_service.service;

import com.lexho.booking.entity.Cart;
import com.lexho.booking.repository.CartRepository;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.dto.CartRequest;
import com.lexho.user_service.dto.CartResponseDTO;
import com.lexho.user_service.dto.CartSummaryDTO;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final PartnerRepository partnerRepo;
    @Value("${app.media.public-base-url:https://cdn.lexho.com}")
    private String mediaPublicBaseUrl;

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
                toDeviceReachableImageUrl(partner.getImage()),
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
                        toDeviceReachableImageUrl(resolvePartnerImage(cart.getPartnerId())),
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

    private String resolvePartnerImage(Long partnerId) {
        if (partnerId == null) {
            return null;
        }
        return partnerRepo.findById(partnerId).map(Partner::getImage).orElse(null);
    }

    private String toDeviceReachableImageUrl(String raw) {
        String base = sanitizeBaseUrl(mediaPublicBaseUrl);
        if (raw == null || raw.isBlank()) {
            return base + "/defaults/service.png";
        }
        String trimmed = raw.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            if (lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("0.0.0.0")) {
                String path = extractPath(trimmed);
                return path == null ? base + "/defaults/service.png" : base + path;
            }
            return lower.startsWith("http://") ? "https://" + trimmed.substring(7) : trimmed;
        }
        String normalized = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
        return base + normalized;
    }

    private String sanitizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://cdn.lexho.com";
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String extractPath(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            return path.startsWith("/") ? path : "/" + path;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
