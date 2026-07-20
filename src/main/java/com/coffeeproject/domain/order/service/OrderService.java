package com.coffeeproject.domain.order.service;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.entity.MenuStatus;
import com.coffeeproject.domain.menu.repository.CoffeeMenuRepository;
import com.coffeeproject.domain.menu.service.PopularMenuRankingService;
import com.coffeeproject.domain.menu.service.PopularMenuRankingService.PopularMenuIncrement;
import com.coffeeproject.domain.order.dto.OrderCreateRequest;
import com.coffeeproject.domain.order.dto.OrderCreateResponse;
import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.order.entity.OrderItem;
import com.coffeeproject.domain.order.repository.OrderRepository;
import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import com.coffeeproject.domain.outbox.repository.OutboxEventRepository;
import com.coffeeproject.domain.point.entity.Point;
import com.coffeeproject.domain.point.entity.PointHistory;
import com.coffeeproject.domain.point.repository.PointHistoryRepository;
import com.coffeeproject.domain.point.repository.PointRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final CoffeeMenuRepository coffeeMenuRepository;
    private final PointRepository pointRepository;
    private final OrderRepository orderRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PopularMenuRankingService popularMenuRankingService;

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Map<Long, CoffeeMenu> menuMap = findMenuMap(request.items());
        long totalPrice = calculateTotalPrice(request.items(), menuMap);

        Point point = pointRepository.findByUserIdWithLock(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateEnoughPoint(point, totalPrice);
        point.use(totalPrice);

        Order order = Order.create(user);
        request.items().forEach(item -> order.addItem(menuMap.get(item.menuId()), item.quantity()));
        Order savedOrder = orderRepository.save(order);

        pointHistoryRepository.save(PointHistory.use(user, savedOrder, totalPrice, point.getBalance()));
        outboxEventRepository.save(OutboxEvent.orderCompleted(
                savedOrder,
                createOrderCompletedPayload(savedOrder, user, totalPrice)
        ));
        popularMenuRankingService.increaseAfterCommit(createPopularMenuIncrements(savedOrder.getItems()));

        return OrderCreateResponse.of(savedOrder.getId(), totalPrice, point.getBalance());
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

    private void validateEnoughPoint(Point point, long totalPrice) {
        if (point.getBalance() < totalPrice) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
    }

    private String createOrderCompletedPayload(Order order, User user, long paymentAmount) {
        String items = order.getItems()
                .stream()
                .map(item -> """
                        {"menuId":%d,"quantity":%d,"orderPrice":%d}"""
                        .formatted(item.getMenu().getId(), item.getQuantity(), item.getOrderPrice()))
                .collect(Collectors.joining(","));

        return """
                {"userId":%d,"orderId":%d,"items":[%s],"paymentAmount":%d}"""
                .formatted(user.getId(), order.getId(), items, paymentAmount);
    }

    private List<PopularMenuIncrement> createPopularMenuIncrements(List<OrderItem> items) {
        return items.stream()
                .map(item -> new PopularMenuIncrement(item.getMenu().getId(), item.getQuantity()))
                .toList();
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
