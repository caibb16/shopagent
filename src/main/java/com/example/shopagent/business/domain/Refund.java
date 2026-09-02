package com.example.shopagent.business.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("refund")
public class Refund {
    @TableId
    private String refundId;
    private String orderId;
    private Long userId;
    private String reason;
    private String status; // PENDING | APPROVED | REJECTED
    private Instant createdAt;
}
