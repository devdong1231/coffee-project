package com.coffeeproject.domain.order.service;

import com.coffeeproject.domain.menu.service.PopularMenuRankingService;
import com.coffeeproject.domain.menu.service.PopularMenuRankingService.PopularMenuIncrement;
import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.order.entity.OrderItem;
import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import com.coffeeproject.domain.outbox.repository.OutboxEventRepository;
import com.coffeeproject.domain.user.entity.User;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderCompletionService {

    private final OutboxEventRepository outboxEventRepository;
    private final PopularMenuRankingService popularMenuRankingService;

    public void complete(Order order, User user, Long paymentAmount) {
        outboxEventRepository.save(OutboxEvent.orderCompleted(
                order,
                createOrderCompletedPayload(order, user, paymentAmount)
        ));
        popularMenuRankingService.increaseAfterCommit(createPopularMenuIncrements(order.getItems()));
    }

    private String createOrderCompletedPayload(Order order, User user, Long paymentAmount) {
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
}
