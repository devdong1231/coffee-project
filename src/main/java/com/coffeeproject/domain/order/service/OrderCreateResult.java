package com.coffeeproject.domain.order.service;

import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.user.entity.User;

public record OrderCreateResult(
        User user,
        Order order,
        Long paymentAmount
) {
}
