package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Refund;
import com.example.shopagent.business.repo.RefundRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryRefundRepository implements RefundRepository {
    private final Map<String, Refund> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    @Override public Refund save(Refund refund) {
        if (refund.getRefundId() == null) refund.setRefundId("R" + seq.incrementAndGet());
        store.put(refund.getRefundId(), refund); return refund;
    }
    @Override public List<Refund> findByUserId(Long userId) {
        return store.values().stream().filter(r -> Objects.equals(r.getUserId(), userId)).toList();
    }
    @Override public List<Refund> findByOrderId(String orderId) {
        return store.values().stream().filter(r -> Objects.equals(r.getOrderId(), orderId)).toList();
    }
}
