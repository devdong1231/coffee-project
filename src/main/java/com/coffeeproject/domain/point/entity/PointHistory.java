package com.coffeeproject.domain.point.entity;

import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "point_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PointHistoryType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    private PointHistory(User user, Order order, PointHistoryType type, Long amount, Long balanceAfter) {
        validateUser(user);
        validatePositive(amount);
        validatePositiveOrZero(balanceAfter);
        validateOrder(type, order);
        this.user = user;
        this.order = order;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public static PointHistory charge(User user, Long amount, Long balanceAfter) {
        return new PointHistory(user, null, PointHistoryType.CHARGE, amount, balanceAfter);
    }

    public static PointHistory use(User user, Order order, Long amount, Long balanceAfter) {
        return new PointHistory(user, order, PointHistoryType.USE, amount, balanceAfter);
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }
    }

    private static void validateOrder(PointHistoryType type, Order order) {
        if (type == PointHistoryType.USE && order == null) {
            throw new IllegalArgumentException("포인트 사용 이력에는 주문이 필요합니다.");
        }
        if (type == PointHistoryType.CHARGE && order != null) {
            throw new IllegalArgumentException("포인트 충전 이력에는 주문을 연결하지 않습니다.");
        }
    }

    private static void validatePositive(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
    }

    private static void validatePositiveOrZero(Long amount) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("잔액은 0 이상이어야 합니다.");
        }
    }
}
