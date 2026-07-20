package com.coffeeproject.domain.order.facade;

import com.coffeeproject.domain.order.dto.OrderCreateRequest;
import com.coffeeproject.domain.order.dto.OrderCreateResponse;
import com.coffeeproject.domain.order.service.OrderCompletionService;
import com.coffeeproject.domain.order.service.OrderCreateResult;
import com.coffeeproject.domain.order.service.OrderPaymentService;
import com.coffeeproject.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final OrderPaymentService orderPaymentService;
    private final OrderCompletionService orderCompletionService;

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        OrderCreateResult result = orderService.createOrder(request);
        Long remainingPoint = orderPaymentService.pay(result.user(), result.order(), result.paymentAmount());
        orderCompletionService.complete(result.order(), result.user(), result.paymentAmount());

        return OrderCreateResponse.of(result.order().getId(), result.paymentAmount(), remainingPoint);
    }
}
