package com.coffeeproject.domain.user.repository;

import com.coffeeproject.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
