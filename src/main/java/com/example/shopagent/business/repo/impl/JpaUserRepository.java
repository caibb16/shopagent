package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.User;
import com.example.shopagent.business.repo.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * {@link UserRepository} backed by Spring Data JPA.
 *
 * <p>Spring Data resolves the inherited {@code findById(Long)} and
 * {@code save(User)} from {@link JpaRepository} as the concrete
 * implementation — we intentionally do NOT override them, which would
 * otherwise recurse back through the domain interface.
 */
@Repository
public interface JpaUserRepository extends JpaRepository<User, Long>, UserRepository {
}