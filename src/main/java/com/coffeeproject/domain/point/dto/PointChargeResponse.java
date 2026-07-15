package com.coffeeproject.domain.point.dto;

public record PointChargeResponse(
        Long balance
) {

    public static PointChargeResponse from(Long balance) {
        return new PointChargeResponse(balance);
    }
}
