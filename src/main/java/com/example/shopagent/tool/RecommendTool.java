package com.example.shopagent.tool;

import com.example.shopagent.business.domain.Product;
import com.example.shopagent.business.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecommendTool {
    private final ProductRepository productRepository;

    @Tool(description = "根据商品分类和预算推荐商品。需要用户已登录；返回适合当前用户的商品列表。")
    public List<Product> recommendProducts(
            @ToolParam(description = "商品分类，例如 数码/家居/服饰") String category,
            @ToolParam(description = "预算上限（元）") BigDecimal budget) {
        if (category == null || category.isBlank()) {
            return productRepository.findByPriceLessThan(budget == null ? BigDecimal.valueOf(1000) : budget);
        }
        var inCat = productRepository.findByCategory(category);
        if (budget != null) {
            return inCat.stream().filter(p -> p.getPrice().compareTo(budget) <= 0).toList();
        }
        return inCat;
    }
}
