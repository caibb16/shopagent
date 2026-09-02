package com.example.shopagent.business.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.shopagent.business.domain.Order;
import com.example.shopagent.business.repo.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@link OrderRepository} backed by MyBatis-Plus.
 *
 * <p>Uses {@code required = false} so that tests without the mapper on the
 * classpath still construct cleanly. Operations defend against a {@code null}
 * mapper by throwing {@link IllegalStateException} rather than returning
 * silent empty results.
 */
@Repository
public class MybatisOrderRepository implements OrderRepository {

    @Autowired(required = false)
    private OrderMapper mapper;

    private void requireMapper() {
        if (mapper == null) {
            throw new IllegalStateException(
                    "OrderMapper is not wired — requires mybatis-plus-spring-boot3-starter "
                            + "and a configured DataSource. Check pom.xml and application.yml.");
        }
    }

    @Override
    public Optional<Order> findById(String orderId) {
        requireMapper();
        return Optional.ofNullable(mapper.selectById(orderId));
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        requireMapper();
        return mapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId));
    }

    @Override
    public Order save(Order order) {
        requireMapper();
        if (order.getOrderId() == null) {
            mapper.insert(order);
        } else {
            mapper.updateById(order);
        }
        return order;
    }
}