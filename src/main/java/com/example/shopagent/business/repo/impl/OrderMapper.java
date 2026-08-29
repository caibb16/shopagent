package com.example.shopagent.business.repo.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shopagent.business.domain.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for {@link Order}. Wired automatically by
 * {@code mybatis-plus-spring-boot3-starter}; the {@code @Mapper} annotation
 * is redundant when {@code MapperScan} covers this package, but keeps the
 * intent explicit.
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}