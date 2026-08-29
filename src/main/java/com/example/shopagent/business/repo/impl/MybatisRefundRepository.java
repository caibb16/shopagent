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
 *
 * <p>Same fail-loud policy as {@link MybatisOrderRepository}: a null mapper
 * indicates broken prod wiring, so we throw rather than silently return
 * empty lists.
 */
@Repository
@Profile("prod")
public class MybatisRefundRepository implements RefundRepository {

    @Autowired(required = false)
    private RefundMapper mapper;

    private void requireMapper() {
        if (mapper == null) {
            throw new IllegalStateException(
                    "RefundMapper is not wired — prod profile requires mybatis-plus-spring-boot3-starter "
                            + "and a configured DataSource. Check pom.xml and application-prod.yml.");
        }
    }

    @Override
    public Refund save(Refund refund) {
        requireMapper();
        if (refund.getRefundId() == null) {
            mapper.insert(refund);
        } else {
            mapper.updateById(refund);
        }
        return refund;
    }

    @Override
    public List<Refund> findByUserId(Long userId) {
        requireMapper();
        return mapper.selectList(new LambdaQueryWrapper<Refund>().eq(Refund::getUserId, userId));
    }

    @Override
    public List<Refund> findByOrderId(String orderId) {
        requireMapper();
        return mapper.selectList(new LambdaQueryWrapper<Refund>().eq(Refund::getOrderId, orderId));
    }
}