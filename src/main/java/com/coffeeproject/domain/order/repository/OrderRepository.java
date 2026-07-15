package com.coffeeproject.domain.order.repository;

import com.coffeeproject.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
