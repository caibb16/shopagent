package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.impl.InMemoryOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderToolAuthTest {
    private OrderRepository repo;
    private OrderTool tool;

    @BeforeEach
    void setUp() {
        repo = new InMemoryOrderRepository();
        tool = new OrderTool(repo);
        repo.save(Order.builder().orderId("O1").userId(1L).status("PENDING")
                .totalAmount(BigDecimal.TEN)
                .items(List.of()).build());
    }

    @AfterEach
    void cleanup() { UserContext.clear(); }

    @Test
    void ownerCanRead() {
        UserContext.set(new UserContext(1L, "s"));
        assertThat(tool.getOrderDetail("O1").orderId()).isEqualTo("O1");
    }

    @Test
    void nonOwnerBlocked() {
        UserContext.set(new UserContext(2L, "s"));
        assertThatThrownBy(() -> tool.getOrderDetail("O1"))
                .isInstanceOf(ToolAuthException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void unknownOrderThrows() {
        UserContext.set(new UserContext(1L, "s"));
        assertThatThrownBy(() -> tool.getOrderDetail("NOPE"))
                .isInstanceOf(ToolAuthException.class)
                .hasMessageContaining("订单不存在");
    }
}
