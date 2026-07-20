package com.coffeeproject.domain.order.repository;

import com.coffeeproject.domain.order.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            select distinct o
            from Order o
            left join fetch o.items oi
            left join fetch oi.menu
            where o.createdAt >= :from
              and o.createdAt < :to
            """)
    List<Order> findAllWithItemsCreatedBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
