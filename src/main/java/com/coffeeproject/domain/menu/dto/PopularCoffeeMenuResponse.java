package com.coffeeproject.domain.menu.dto;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;

public record PopularCoffeeMenuResponse(
        Long menuId,
        String name,
        Long price,
        Long orderCount
) {

    public static PopularCoffeeMenuResponse of(CoffeeMenu menu, Long orderCount) {
        return new PopularCoffeeMenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                orderCount
        );
    }
}
