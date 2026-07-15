package com.coffeeproject.domain.order.dto;

public record OrderCreateResponse(
        Long orderId,
        Long paymentAmount,
        Long remainingPoint
) {

    public static OrderCreateResponse of(Long orderId, Long paymentAmount, Long remainingPoint) {
        return new OrderCreateResponse(orderId, paymentAmount, remainingPoint);
    }
}
