package com.urban_shop.backend.order.repository;

import com.urban_shop.backend.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

    List<OrderItem> findAllByOrderIdIn(Collection<UUID> orderIds);
}
