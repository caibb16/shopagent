package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Coupon;
import com.example.shopagent.business.repo.CouponRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCouponRepository implements CouponRepository {
    private final Map<String, Coupon> store = new ConcurrentHashMap<>();
    @Override public List<Coupon> findByUserId(Long userId) {
        return store.values().stream().filter(c -> Objects.equals(c.getUserId(), userId)).toList();
    }
    @Override public Coupon save(Coupon coupon) { store.put(coupon.getCouponId(), coupon); return coupon; }
}
