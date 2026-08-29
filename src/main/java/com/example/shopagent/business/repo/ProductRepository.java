package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(String productId);
    List<Product> findByCategory(String category);
    List<Product> findByPriceLessThan(java.math.BigDecimal maxPrice);
}