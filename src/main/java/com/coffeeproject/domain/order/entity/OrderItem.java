package com.coffeeproject.domain.order.entity;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "order_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_items_order_menu",
                columnNames = {"order_id", "menu_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private CoffeeMenu menu;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Long orderPrice;

    private OrderItem(Order order, CoffeeMenu menu, int quantity) {
        validateOrder(order);
        validateMenu(menu);
        validateQuantity(quantity);
        menu.validateOrderable();
        this.order = order;
        this.menu = menu;
        this.quantity = quantity;
        this.orderPrice = menu.getPrice();
    }

    static OrderItem create(Order order, CoffeeMenu menu, int quantity) {
        return new OrderItem(order, menu, quantity);
    }

    public Long getLinePrice() {
        return orderPrice * quantity;
    }

    boolean isSameMenu(CoffeeMenu menu) {
        return this.menu != null && menu != null && Objects.equals(this.menu.getId(), menu.getId());
    }

    private static void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("주문은 필수입니다.");
        }
    }

    private static void validateMenu(CoffeeMenu menu) {
        if (menu == null) {
            throw new IllegalArgumentException("메뉴는 필수입니다.");
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 0보다 커야 합니다.");
        }
    }
}
