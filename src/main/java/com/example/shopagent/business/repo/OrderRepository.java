package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(String orderId);
    List<Order> findByUserId(Long userId);
    Order save(Order order);
}