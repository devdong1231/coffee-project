package com.coffeeproject.domain.order.service;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.entity.MenuStatus;
import com.coffeeproject.domain.menu.repository.CoffeeMenuRepository;
import com.coffeeproject.domain.order.dto.OrderCreateRequest;
import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.order.repository.OrderRepository;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.domain.user.repository.UserRepository;
import com.coffeeproject.global.exception.BusinessException;
import com.coffeeproject.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CoffeeMenuRepository coffeeMenuRepository;
    private final OrderRepository orderRepository;

    public OrderCreateResult createOrder(OrderCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Map<Long, CoffeeMenu> menuMap = findMenuMap(request.items());
        long totalPrice = calculateTotalPrice(request.items(), menuMap);

        Order order = Order.create(user);
        request.items().forEach(item -> order.addItem(menuMap.get(item.menuId()), item.quantity()));
        Order savedOrder = orderRepository.save(order);

        return new OrderCreateResult(user, savedOrder, totalPrice);
    }

    private Map<Long, CoffeeMenu> findMenuMap(List<OrderCreateRequest.OrderItemRequest> items) {
        List<Long> menuIds = items.stream()
                .map(OrderCreateRequest.OrderItemRequest::menuId)
                .distinct()
                .toList();

        Map<Long, CoffeeMenu> menuMap = coffeeMenuRepository.findAllById(menuIds)
                .stream()
                .collect(Collectors.toMap(CoffeeMenu::getId, Function.identity()));

        if (menuMap.size() != menuIds.size()) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }

        menuMap.values().forEach(this::validateOrderable);
        return menuMap;
    }

    private void validateOrderable(CoffeeMenu menu) {
        if (menu.getStatus() != MenuStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MENU_NOT_ORDERABLE);
        }
    }

    private long calculateTotalPrice(
            List<OrderCreateRequest.OrderItemRequest> items,
            Map<Long, CoffeeMenu> menuMap
    ) {
        long totalPrice = 0L;
        for (OrderCreateRequest.OrderItemRequest item : items) {
            CoffeeMenu menu = menuMap.get(item.menuId());
            long linePrice = multiplyExact(menu.getPrice(), item.quantity());
            totalPrice = addExact(totalPrice, linePrice);
        }
        return totalPrice;
    }

    private long multiplyExact(long price, int quantity) {
        try {
            return Math.multiplyExact(price, quantity);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("주문 금액이 허용 범위를 초과합니다.");
        }
    }

    private long addExact(long totalPrice, long linePrice) {
        try {
            return Math.addExact(totalPrice, linePrice);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("주문 금액이 허용 범위를 초과합니다.");
        }
    }

}
