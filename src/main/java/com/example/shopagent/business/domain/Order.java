package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class Order {
    private String orderId;
    private Long userId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String status; // PENDING | SHIPPED | DELIVERED | REFUNDED
    private Instant createdAt;
    private String trackingNumber;
}
