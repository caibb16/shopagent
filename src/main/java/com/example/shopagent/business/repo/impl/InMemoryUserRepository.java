package com.example.shopagent.business.repo.impl;

import com.example.shopagent.business.domain.User;
import com.example.shopagent.business.repo.UserRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> store = new ConcurrentHashMap<>();
    @Override public Optional<User> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    @Override public User save(User user) { store.put(user.getId(), user); return user; }
}
