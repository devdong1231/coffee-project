package com.coffeeproject.domain.menu.entity;

import com.coffeeproject.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeMenu extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuStatus status;

    private CoffeeMenu(String name, Long price, MenuStatus status) {
        validateName(name);
        validatePrice(price);
        this.name = name;
        this.price = price;
        this.status = status == null ? MenuStatus.ACTIVE : status;
    }

    public static CoffeeMenu create(String name, Long price) {
        return new CoffeeMenu(name, price, MenuStatus.ACTIVE);
    }

    public void changePrice(Long price) {
        validatePrice(price);
        this.price = price;
    }

    public void activate() {
        this.status = MenuStatus.ACTIVE;
    }

    public void soldOut() {
        this.status = MenuStatus.SOLD_OUT;
    }

    public void validateOrderable() {
        if (status != MenuStatus.ACTIVE) {
            throw new IllegalStateException("판매 가능한 메뉴가 아닙니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("메뉴명은 필수입니다.");
        }
    }

    private static void validatePrice(Long price) {
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("메뉴 가격은 0보다 커야 합니다.");
        }
    }
}
