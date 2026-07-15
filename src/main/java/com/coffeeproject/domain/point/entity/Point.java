package com.coffeeproject.domain.point.entity;

import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Long balance;

    private Point(User user, Long balance) {
        validateUser(user);
        validatePositiveOrZero(balance);
        this.user = user;
        this.balance = balance;
    }

    public static Point create(User user) {
        return new Point(user, 0L);
    }

    public static Point create(User user, Long balance) {
        return new Point(user, balance);
    }

    public void charge(Long amount) {
        validatePositive(amount);
        this.balance += amount;
    }

    public void use(Long amount) {
        validatePositive(amount);
        if (balance < amount) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.balance -= amount;
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
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
