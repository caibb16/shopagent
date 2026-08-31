package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Logistics;
import com.example.shopagent.business.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LogisticsTool {
    private final OrderRepository orderRepository;

    @Tool(description = "根据订单号查询物流轨迹。需要用户已登录；只能查询当前用户自己的订单。")
    public Logistics getLogistics(
            @ToolParam(description = "订单号") String orderId,
            ToolContext toolContext) {
        Long uid = UserContext.resolveUserId(toolContext);
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ToolAuthException("订单不存在"));
        if (!order.getUserId().equals(uid)) {
            throw new ToolAuthException("无权访问");
        }
        if (order.getTrackingNumber() == null) {
            return Logistics.builder().trackingNumber("(未发货)")
                    .currentLocation("等待出库").events(List.of()).build();
        }
        // dev mock trajectory
        return Logistics.builder()
                .trackingNumber(order.getTrackingNumber())
                .carrier("顺丰")
                .currentLocation("上海中转站")
                .events(List.of(
                    Logistics.Event.builder().timestamp(Instant.now().minusSeconds(86400))
                            .location("杭州").description("已揽收").build(),
                    Logistics.Event.builder().timestamp(Instant.now().minusSeconds(43200))
                            .location("上海中转站").description("运输中").build()))
                .build();
    }
}
