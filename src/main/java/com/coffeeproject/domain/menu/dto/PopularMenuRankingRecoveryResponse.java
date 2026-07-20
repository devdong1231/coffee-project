package com.coffeeproject.domain.menu.dto;

import com.coffeeproject.domain.menu.service.PopularMenuRankingRecoveryService.PopularMenuRankingRecoveryResult;

public record PopularMenuRankingRecoveryResponse(
        int orderCount,
        long itemCount
) {

    public static PopularMenuRankingRecoveryResponse from(PopularMenuRankingRecoveryResult result) {
        return new PopularMenuRankingRecoveryResponse(
                result.orderCount(),
                result.itemCount()
        );
    }
}
