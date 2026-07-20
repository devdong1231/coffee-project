package com.coffeeproject.domain.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 주문_상세와_메뉴를_함께_조회하고_조회_기간_밖_주문은_제외한다() {
        CoffeeMenu americano = saveMenu("아메리카노", 4500L);
        CoffeeMenu latte = saveMenu("카페라떼", 5000L);
        User user = saveUser("사용자");

        Order inRangeOrder = saveOrder(user, LocalDateTime.of(2026, 7, 15, 10, 0), americano, 2);
        saveOrder(user, LocalDateTime.of(2026, 7, 8, 23, 59), latte, 1);

        entityManager.clear();

        List<Order> orders = orderRepository.findAllWithItemsCreatedBetween(
                LocalDateTime.of(2026, 7, 9, 0, 0),
                LocalDateTime.of(2026, 7, 16, 0, 0)
        );

        assertThat(orders).extracting(Order::getId)
                .containsExactly(inRangeOrder.getId());
        assertThat(orders.get(0).getItems()).hasSize(1);
        assertThat(orders.get(0).getItems().get(0).getMenu().getName()).isEqualTo("아메리카노");
    }

    private User saveUser(String name) {
        User user = User.create(name);
        entityManager.persist(user);
        return user;
    }

    private CoffeeMenu saveMenu(String name, Long price) {
        CoffeeMenu menu = CoffeeMenu.create(name, price);
        entityManager.persist(menu);
        return menu;
    }

    private Order saveOrder(User user, LocalDateTime createdAt, CoffeeMenu menu, int quantity) {
        Order order = Order.create(user);
        order.addItem(menu, quantity);
        entityManager.persist(order);
        entityManager.flush();
        entityManager.createNativeQuery("update orders set created_at = ? where id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, order.getId())
                .executeUpdate();
        return order;
    }
}
