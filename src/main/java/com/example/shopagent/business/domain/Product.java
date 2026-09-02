package com.example.shopagent.business.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {
    @Id
    private String productId;
    private String name;
    private String category;
    private BigDecimal price;
    private String description;
}
