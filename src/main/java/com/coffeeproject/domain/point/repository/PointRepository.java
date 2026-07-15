package com.coffeeproject.domain.point.repository;

import com.coffeeproject.domain.point.entity.Point;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointRepository extends JpaRepository<Point, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Point p join fetch p.user where p.user.id = :userId")
    Optional<Point> findByUserIdWithLock(@Param("userId") Long userId);
}
