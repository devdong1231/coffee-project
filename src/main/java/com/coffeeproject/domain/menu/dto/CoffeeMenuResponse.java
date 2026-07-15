package com.coffeeproject.domain.menu.dto;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import java.util.List;

public record CoffeeMenuResponse(
        Long menuId,
        String name,
        Long price
) {

    public static CoffeeMenuResponse from(CoffeeMenu menu) {
        return new CoffeeMenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice()
        );
    }

    public static List<CoffeeMenuResponse> fromList(List<CoffeeMenu> menus) {
        return menus.stream()
                .map(CoffeeMenuResponse::from)
                .toList();
    }
}
