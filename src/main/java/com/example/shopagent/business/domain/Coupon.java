package com.example.shopagent.business.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupon")
public class Coupon {
    @Id
    private String couponId;
    private Long userId;
    private String name;
    private BigDecimal discount;
    private Instant expiresAt;
    private boolean used;
}
