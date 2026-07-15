package com.coffeeproject.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record OrderCreateRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotEmpty(message = "주문할 메뉴는 필수입니다.")
        @Valid
        List<@NotNull(message = "주문 항목은 필수입니다.") @Valid OrderItemRequest> items
) {

    public record OrderItemRequest(
            @NotNull(message = "메뉴 ID는 필수입니다.")
            Long menuId,

            @Positive(message = "주문 수량은 0보다 커야 합니다.")
            int quantity
    ) {
    }
}
