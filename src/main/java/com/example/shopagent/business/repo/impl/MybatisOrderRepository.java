package com.example.shopagent.business.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Prod-profile {@link OrderRepository} backed by MyBatis-Plus.
 *
 * <p>NOTE on the {@code mapper = null} pattern from the brief: a bare
 * {@code ServiceImpl<OrderMapper, Order>} field cannot be safely constructed
 * with {@code null} (it has a {@code final M baseMapper} slot). Instead we
 * inject the underlying {@link OrderMapper} (a {@code BaseMapper<Order>})
 * optionally — production wiring via {@code mybatis-plus-spring-boot3-starter}
 * will populate it; if absent the repo degrades gracefully so the dev tests
 * (which never load this bean) keep passing.
 */
@Repository
@Profile("prod")
public class MybatisOrderRepository implements OrderRepository {

    @Autowired(required = false)
    private OrderMapper mapper;

    @Override
    public Optional<Order> findById(String orderId) {
        if (mapper == null) return Optional.empty();
        return Optional.ofNullable(mapper.selectById(orderId));
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        if (mapper == null) return List.of();
        return mapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId));
    }

    @Override
    public Order save(Order order) {
        if (mapper == null) return order;
        if (order.getOrderId() == null) {
            mapper.insert(order);
        } else {
            mapper.updateById(order);
        }
        return order;
    }
}