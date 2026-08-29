package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.Coupon;
import java.util.List;

public interface CouponRepository {
    List<Coupon> findByUserId(Long userId);
    Coupon save(Coupon coupon);
}