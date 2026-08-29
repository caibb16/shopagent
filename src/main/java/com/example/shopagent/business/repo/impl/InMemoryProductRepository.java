package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Product;
import com.example.shopagent.business.repo.ProductRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProductRepository implements ProductRepository {
    private final Map<String, Product> store = new ConcurrentHashMap<>();
    @Override public Optional<Product> findById(String id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<Product> findByCategory(String category) {
        return store.values().stream().filter(p -> Objects.equals(p.getCategory(), category)).toList();
    }
    @Override public List<Product> findByPriceLessThan(BigDecimal maxPrice) {
        return store.values().stream().filter(p -> p.getPrice().compareTo(maxPrice) < 0).toList();
    }
    public void register(Product p) { store.put(p.getProductId(), p); }
}
