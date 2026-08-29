package com.example.shopagent.tool.dto;

import com.example.shopagent.business.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetail(
    String orderId,
    String status,
    BigDecimal totalAmount,
    Instant createdAt,
    String trackingNumber,
    List<Item> items) {

    public record Item(String productId, String productName, Integer quantity, BigDecimal unitPrice) {}

    public static OrderDetail from(Order o) {
        return new OrderDetail(
            o.getOrderId(),
            o.getStatus(),
            o.getTotalAmount(),
            o.getCreatedAt(),
            o.getTrackingNumber(),
            o.getItems() == null ? List.of() :
                o.getItems().stream().map(i -> new Item(i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice())).toList()
        );
    }
}
