package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Refund;
import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.business.repo.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RefundTool {
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;

    @Tool(description = "为当前用户的指定订单提交退款申请。需要用户已登录；仅能为自己已发货或已送达的订单申请退款。")
    public Refund createRefundOrder(
            @ToolParam(description = "订单号") String orderId,
            @ToolParam(description = "退款原因") String reason) {
        Long uid = UserContext.current().userId();
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ToolAuthException("订单不存在"));
        if (!order.getUserId().equals(uid)) {
            throw new ToolAuthException("无权访问");
        }
        if ("REFUNDED".equals(order.getStatus())) {
            throw new ToolAuthException("订单已退款");
        }
        if ("PENDING".equals(order.getStatus())) {
            throw new ToolAuthException("未发货订单请直接取消");
        }
        return refundRepository.save(Refund.builder()
                .orderId(orderId).userId(uid).reason(reason)
                .status("PENDING").createdAt(Instant.now()).build());
    }
}