package com.example.shopagent.tool;

import com.example.shopagent.business.repo.OrderRepository;
import com.example.shopagent.tool.dto.OrderDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderTool {
    private final OrderRepository orderRepository;

    @Tool(description = "根据订单号查询订单详情。需要用户已登录；只能查询当前用户自己的订单。")
    public OrderDetail getOrderDetail(
            @ToolParam(description = "订单号") String orderId,
            ToolContext toolContext) {
        Long uid = UserContext.resolveUserId(toolContext);
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ToolAuthException("订单不存在"));
        if (!order.getUserId().equals(uid)) {
            throw new ToolAuthException("无权访问");
        }
        return OrderDetail.from(order);
    }
}
