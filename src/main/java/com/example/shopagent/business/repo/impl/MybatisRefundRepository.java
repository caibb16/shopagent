package com.example.shopagent.business.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shopagent.business.domain.Refund;
import com.example.shopagent.business.repo.RefundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Prod-profile {@link RefundRepository} backed by MyBatis-Plus.
 * Same optional-injection pattern as {@link MybatisOrderRepository}.
 */
@Repository
@Profile("prod")
public class MybatisRefundRepository implements RefundRepository {

    @Autowired(required = false)
    private RefundMapper mapper;

    @Override
    public Refund save(Refund refund) {
        if (mapper == null) return refund;
        if (refund.getRefundId() == null) {
            mapper.insert(refund);
        } else {
            mapper.updateById(refund);
        }
        return refund;
    }

    @Override
    public List<Refund> findByUserId(Long userId) {
        if (mapper == null) return List.of();
        return mapper.selectList(new LambdaQueryWrapper<Refund>().eq(Refund::getUserId, userId));
    }

    @Override
    public List<Refund> findByOrderId(String orderId) {
        if (mapper == null) return List.of();
        return mapper.selectList(new LambdaQueryWrapper<Refund>().eq(Refund::getOrderId, orderId));
    }
}