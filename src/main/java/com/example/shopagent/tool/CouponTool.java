package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Coupon;
import com.example.shopagent.business.repo.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponTool {
    private final CouponRepository couponRepository;

    @Tool(description = "查询当前用户的所有优惠券。需要用户已登录；返回当前用户本人的优惠券列表。")
    public List<Coupon> getCouponList(ToolContext toolContext) {
        Long uid = UserContext.resolveUserId(toolContext);
        return couponRepository.findByUserId(uid);
    }
}
