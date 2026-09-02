package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    @Override public Optional<Order> findById(String orderId) { return Optional.ofNullable(store.get(orderId)); }
    @Override public List<Order> findByUserId(Long userId) {
        return store.values().stream().filter(o -> Objects.equals(o.getUserId(), userId)).toList();
    }
    @Override public Order save(Order order) { store.put(order.getOrderId(), order); return order; }
}
