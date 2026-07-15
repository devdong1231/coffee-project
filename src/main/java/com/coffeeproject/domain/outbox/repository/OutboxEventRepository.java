package com.coffeeproject.domain.outbox.repository;

import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
}
