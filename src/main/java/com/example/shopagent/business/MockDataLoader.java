package com.example.shopagent.business;

import com.example.shopagent.business.domain.*;
import com.example.shopagent.business.repo.*;
import com.example.shopagent.business.repo.impl.InMemoryProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MockDataLoader {

    private final UserRepository users;
    private final OrderRepository orders;
    private final RefundRepository refunds;
    private final CouponRepository coupons;
    private final ProductRepository products;

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        log.info("Loading mock data...");
        users.save(User.builder().id(1L).name("Alice").level("NORMAL").build());
        users.save(User.builder().id(2L).name("Bob").level("VIP").build());
        users.save(User.builder().id(999L).name("Mallory").level("BLACKLIST").build());

        InMemoryProductRepository inMemoryProducts = (InMemoryProductRepository) products;
        inMemoryProducts.register(Product.builder().productId("P1").name("蓝牙耳机").category("数码").price(BigDecimal.valueOf(299)).description("主动降噪").build());
        inMemoryProducts.register(Product.builder().productId("P2").name("保温杯").category("家居").price(BigDecimal.valueOf(89)).description("316不锈钢").build());
        inMemoryProducts.register(Product.builder().productId("P3").name("机械键盘").category("数码").price(BigDecimal.valueOf(599)).description("青轴").build());
        inMemoryProducts.register(Product.builder().productId("P4").name("帆布鞋").category("服饰").price(BigDecimal.valueOf(199)).description("经典款").build());

        orders.save(newOrder("O1001", 1L, "PENDING", null));
        orders.save(newOrder("O1002", 1L, "SHIPPED", "SF1234567890"));
        orders.save(newOrder("O1003", 2L, "DELIVERED", "SF9876543210"));
        orders.save(newOrder("O1004", 2L, "REFUNDED", "YT1122334455"));

        coupons.save(Coupon.builder().couponId("C1").userId(1L).name("新人立减").discount(BigDecimal.valueOf(20)).expiresAt(Instant.now().plus(30, ChronoUnit.DAYS)).used(false).build());
        coupons.save(Coupon.builder().couponId("C2").userId(2L).name("VIP专享").discount(BigDecimal.valueOf(50)).expiresAt(Instant.now().plus(60, ChronoUnit.DAYS)).used(false).build());

        log.info("Mock data loaded: 3 users, 4 orders, 2 coupons, 4 products");
    }

    private Order newOrder(String id, Long uid, String status, String tracking) {
        return Order.builder().orderId(id).userId(uid).totalAmount(BigDecimal.valueOf(299))
                .status(status).createdAt(Instant.now()).trackingNumber(tracking)
                .items(List.of(OrderItem.builder().productId("P1").productName("蓝牙耳机").quantity(1).unitPrice(BigDecimal.valueOf(299)).build()))
                .build();
    }
}
