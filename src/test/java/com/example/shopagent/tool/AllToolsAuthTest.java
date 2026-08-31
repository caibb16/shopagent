package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.RefundRepository;
import com.example.shopagent.business.repo.impl.InMemoryOrderRepository;
import com.example.shopagent.business.repo.impl.InMemoryRefundRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AllToolsAuthTest {
    private OrderRepository orderRepo;
    private LogisticsTool logistics;
    private RefundTool refund;
    private RefundRepository refundRepo;

    @BeforeEach
    void setUp() {
        orderRepo = new InMemoryOrderRepository();
        refundRepo = new InMemoryRefundRepository();
        logistics = new LogisticsTool(orderRepo);
        refund = new RefundTool(orderRepo, refundRepo);
        orderRepo.save(Order.builder().orderId("O1").userId(1L).status("SHIPPED")
                .trackingNumber("SF1").totalAmount(BigDecimal.TEN).items(List.of()).build());
        orderRepo.save(Order.builder().orderId("O2").userId(2L).status("SHIPPED")
                .trackingNumber("SF2").totalAmount(BigDecimal.TEN).items(List.of()).build());
    }

    @AfterEach
    void cleanup() { UserContext.clear(); }

    @Test
    void logisticsBlocksNonOwner() {
        UserContext.set(new UserContext(2L, "s"));
        assertThatThrownBy(() -> logistics.getLogistics("O1", null))
                .isInstanceOf(ToolAuthException.class);
    }

    @Test
    void refundBlocksPendingOrder() {
        UserContext.set(new UserContext(1L, "s"));
        orderRepo.save(Order.builder().orderId("PEND").userId(1L).status("PENDING")
                .totalAmount(BigDecimal.TEN).items(List.of()).build());
        assertThatThrownBy(() -> refund.createRefundOrder("PEND", "test", null))
                .isInstanceOf(ToolAuthException.class);
    }

    @Test
    void refundCreatesRecord() {
        UserContext.set(new UserContext(1L, "s"));
        var r = refund.createRefundOrder("O1", "不想要了", null);
        assertThat(r.getStatus()).isEqualTo("PENDING");
        assertThat(r.getUserId()).isEqualTo(1L);
    }
}
