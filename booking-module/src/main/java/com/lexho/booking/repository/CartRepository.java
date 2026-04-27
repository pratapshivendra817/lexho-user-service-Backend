package com.lexho.booking.repository;


import com.lexho.booking.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByUserId(Long userId);

    Optional<Cart> findByUserIdAndPartnerId(Long userId, Long partnerId); // ✅ FIXED
}
