package com.finance.tracker.repository;

import com.finance.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    
    // Spring translates this to: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);
    
}