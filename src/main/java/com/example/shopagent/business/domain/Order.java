package com.example.shopagent.business.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "orders", autoResultMap = true)
public class Order {
    @TableId
    private String orderId;
    private Long userId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String status; // PENDING | SHIPPED | DELIVERED | REFUNDED
    private Instant createdAt;
    private String trackingNumber;
}
