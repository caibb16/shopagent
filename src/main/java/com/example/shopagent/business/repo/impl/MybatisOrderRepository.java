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
 * <p>NOTE on the {@code required = false} pattern: the prod profile always
 * ships with {@code mybatis-plus-spring-boot3-starter} on the classpath, so the
 * mapper MUST be wired in at boot. We use {@code required = false} only so that
 * dev tests (which never load this bean because it is {@code @Profile("prod")})
 * still construct cleanly if Spring ever decides to instantiate it. Operations
 * defend against a {@code null} mapper by failing LOUD with
 * {@link IllegalStateException} — silent empty results would mask prod wiring
 * bugs.
 */
@Repository
@Profile("prod")
public class MybatisOrderRepository implements OrderRepository {

    @Autowired(required = false)
    private OrderMapper mapper;

    private void requireMapper() {
        if (mapper == null) {
            throw new IllegalStateException(
                    "OrderMapper is not wired — prod profile requires mybatis-plus-spring-boot3-starter "
                            + "and a configured DataSource. Check pom.xml and application-prod.yml.");
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