package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.Product;
import com.example.shopagent.business.repo.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@link ProductRepository} backed by Spring Data JPA.
 * Query methods are derived from method names; do not override
 * {@code findById(String)} to avoid recursion through the domain interface.
 */
@Repository
public interface JpaProductRepository extends JpaRepository<Product, String>, ProductRepository {

    @Override
    List<Product> findByCategory(String category);

    @Override
    List<Product> findByPriceLessThan(BigDecimal maxPrice);
}