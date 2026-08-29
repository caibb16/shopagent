package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class OrderItem {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
