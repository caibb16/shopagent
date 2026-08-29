package com.example.shopagent.business;

import com.example.shopagent.business.repo.*;
import com.example.shopagent.business.repo.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MockDataLoaderTest {
    private UserRepository users;
    private OrderRepository orders;
    private InMemoryProductRepository products;
    private MockDataLoader loader;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        orders = new InMemoryOrderRepository();
        products = new InMemoryProductRepository();
        loader = new MockDataLoader(users, orders, new InMemoryRefundRepository(),
                new InMemoryCouponRepository(), products);
    }

    @Test
    void loadSeedsThreeUsers() {
        loader.load();
        assertThat(users.findById(1L)).isPresent();
        assertThat(users.findById(2L)).isPresent();
        assertThat(users.findById(999L)).isPresent();
    }

    @Test
    void loadSeedsOrdersForBothUsers() {
        loader.load();
        assertThat(orders.findByUserId(1L)).hasSize(2);
        assertThat(orders.findByUserId(2L)).hasSize(2);
    }
}
