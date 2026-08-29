package com.example.shopagent.business.domain;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class Refund {
    private String refundId;
    private String orderId;
    private Long userId;
    private String reason;
    private String status; // PENDING | APPROVED | REJECTED
    private Instant createdAt;
}
