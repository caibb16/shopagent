package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class Product {
    private String productId;
    private String name;
    private String category;
    private BigDecimal price;
    private String description;
}
