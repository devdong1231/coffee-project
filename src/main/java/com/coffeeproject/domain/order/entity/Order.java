package com.coffeeproject.domain.order.entity;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.global.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Order(User user) {
        validateUser(user);
        this.user = user;
        this.totalPrice = 0L;
    }

    public static Order create(User user) {
        return new Order(user);
    }

    public void addItem(CoffeeMenu menu, int quantity) {
        validateDuplicateMenu(menu);
        OrderItem item = OrderItem.create(this, menu, quantity);
        items.add(item);
        totalPrice += item.getLinePrice();
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private void validateDuplicateMenu(CoffeeMenu menu) {
        boolean exists = items.stream()
                .anyMatch(item -> item.isSameMenu(menu));
        if (exists) {
            throw new IllegalArgumentException("같은 주문에 동일한 메뉴를 중복 추가할 수 없습니다.");
        }
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }
    }
}
