package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Coupon;
import com.example.shopagent.business.repo.CouponRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Prod-profile {@link CouponRepository} backed by Spring Data JPA.
 * The query method {@link #findByUserId(Long)} is derived from its name.
 */
@Repository
@Profile("!dev")
public interface JpaCouponRepository extends JpaRepository<Coupon, String>, CouponRepository {

    @Override
    List<Coupon> findByUserId(Long userId);
}