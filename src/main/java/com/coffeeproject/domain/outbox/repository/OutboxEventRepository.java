package com.coffeeproject.domain.outbox.repository;

import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import com.coffeeproject.domain.outbox.entity.OutboxEventStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByIdAsc(OutboxEventStatus status, Pageable pageable);
}
