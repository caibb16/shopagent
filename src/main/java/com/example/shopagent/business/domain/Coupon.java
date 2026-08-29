package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class Coupon {
    private String couponId;
    private Long userId;
    private String name;
    private BigDecimal discount;
    private Instant expiresAt;
    private boolean used;
}
