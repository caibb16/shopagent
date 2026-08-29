package com.example.shopagent.business;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.domain.OrderItem;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.impl.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InMemoryOrderRepositoryTest {
    private final OrderRepository repo = new InMemoryOrderRepository();

    @Test
    void saveAndFindByIdRoundTrips() {
        Order o = Order.builder().orderId("O1").userId(1L).totalAmount(BigDecimal.TEN)
                .items(List.of(OrderItem.builder().productId("P1").quantity(1).unitPrice(BigDecimal.TEN).build()))
                .status("PENDING").build();
        repo.save(o);
        assertThat(repo.findById("O1")).contains(o);
    }

    @Test
    void findByUserIdReturnsOnlyMatching() {
        repo.save(Order.builder().orderId("A").userId(1L).status("PENDING").build());
        repo.save(Order.builder().orderId("B").userId(2L).status("PENDING").build());
        assertThat(repo.findByUserId(1L)).extracting(Order::getOrderId).containsExactly("A");
    }

    @Test
    void findByIdMissingReturnsEmpty() {
        assertThat(repo.findById("NOPE")).isEmpty();
    }
}
