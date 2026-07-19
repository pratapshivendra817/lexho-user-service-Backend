package com.lexho.booking.repository;

import com.lexho.booking.entity.OrderAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderAddonRepository extends JpaRepository<OrderAddon, Long> {
    List<OrderAddon> findByOrderId(Long orderId);
}
