package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.Refund;
import java.util.List;

public interface RefundRepository {
    Refund save(Refund refund);
    List<Refund> findByUserId(Long userId);
    List<Refund> findByOrderId(String orderId);
}