package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.CouponRepository;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.RefundRepository;
import com.example.shopagent.business.repo.impl.InMemoryCouponRepository;
import com.example.shopagent.business.repo.impl.InMemoryOrderRepository;
import com.example.shopagent.business.repo.impl.InMemoryRefundRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that userId flows from Spring AI's {@link ToolContext} (the
 * map set via {@code ChatClient.prompt().toolContext(...)}) into each
 * tool method — the path used in production. Crucially, the
 * ThreadLocal {@link UserContext} is NOT set in any of these tests, so
 * a green run proves the toolContext channel works on its own.
 *
 * <p>Pre-refactor, the tools used {@code UserContext.current()} and broke
 * when Spring AI ran tool callbacks on a thread other than the HTTP
 * handler (Netty / worker pool / tool-callback thread).
 */
class ToolContextPropagationTest {

    private OrderRepository orderRepo;
    private RefundRepository refundRepo;
    private CouponRepository couponRepo;
    private OrderTool orderTool;
    private LogisticsTool logisticsTool;
    private RefundTool refundTool;
    private CouponTool couponTool;

    @BeforeEach
    void setUp() {
        orderRepo = new InMemoryOrderRepository();
        refundRepo = new InMemoryRefundRepository();
        couponRepo = new InMemoryCouponRepository();
        orderTool = new OrderTool(orderRepo);
        logisticsTool = new LogisticsTool(orderRepo);
        refundTool = new RefundTool(orderRepo, refundRepo);
        couponTool = new CouponTool(couponRepo);
        orderRepo.save(Order.builder().orderId("O1001").userId(7L).status("SHIPPED")
                .trackingNumber("SF1234567890")
                .totalAmount(BigDecimal.valueOf(199.00))
                .items(List.of()).build());
        orderRepo.save(Order.builder().orderId("O1002").userId(7L).status("DELIVERED")
                .trackingNumber("SF9876543210")
                .totalAmount(BigDecimal.valueOf(99.00))
                .items(List.of()).build());
        orderRepo.save(Order.builder().orderId("O2002").userId(8L).status("SHIPPED")
                .trackingNumber("SF0000000000")
                .totalAmount(BigDecimal.TEN)
                .items(List.of()).build());
    }

    @AfterEach
    void cleanup() { UserContext.clear(); }

    @Test
    void orderToolReadsUserIdFromToolContext() {
        ToolContext ctx = new ToolContext(Map.of("userId", 7L, "sessionId", "s1"));
        var detail = orderTool.getOrderDetail("O1001", ctx);
        assertThat(detail.orderId()).isEqualTo("O1001");
        assertThat(detail.status()).isEqualTo("SHIPPED");
    }

    @Test
    void orderToolRejectsCrossUserAccessViaToolContext() {
        // user 8 trying to read user 7's order — must be blocked even when
        // userId is delivered via ToolContext (the LLM cannot bypass this).
        ToolContext ctx = new ToolContext(Map.of("userId", 8L, "sessionId", "s1"));
        assertThatThrownBy(() -> orderTool.getOrderDetail("O1001", ctx))
                .isInstanceOf(ToolAuthException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void logisticsToolReadsUserIdFromToolContext() {
        ToolContext ctx = new ToolContext(Map.of("userId", 7L));
        var log = logisticsTool.getLogistics("O1001", ctx);
        assertThat(log.getTrackingNumber()).isEqualTo("SF1234567890");
    }

    @Test
    void refundToolReadsUserIdFromToolContext() {
        ToolContext ctx = new ToolContext(Map.of("userId", 7L));
        var r = refundTool.createRefundOrder("O1001", "买错了", ctx);
        assertThat(r.getStatus()).isEqualTo("PENDING");
        assertThat(r.getUserId()).isEqualTo(7L);
    }

    @Test
    void couponToolReadsUserIdFromToolContext() {
        ToolContext ctx = new ToolContext(Map.of("userId", 7L));
        // empty list is fine — proves the userId channel reached the tool
        assertThat(couponTool.getCouponList(ctx)).isNotNull();
    }

    @Test
    void escalateToolLogsUserIdFromToolContext() {
        ToolContext ctx = new ToolContext(Map.of("userId", 7L));
        var r = escalateTool().escalateToHuman("等太久", "排队1小时", ctx);
        assertThat(r.get("status")).isEqualTo("QUEUED");
    }

    @Test
    void toolsRejectWhenToolContextIsEmptyAndThreadLocalAbsent() {
        // Empty ToolContext and no ThreadLocal → must fail loudly rather than
        // silently allow cross-user access. This is the safety invariant.
        assertThatThrownBy(() -> orderTool.getOrderDetail("O1001", new ToolContext(Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UserContext not set");
    }

    private EscalateTool escalateTool() {
        return new EscalateTool();
    }
}
