package com.example.shopagent.business.repo;

import com.example.shopagent.business.domain.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    User save(User user);
}