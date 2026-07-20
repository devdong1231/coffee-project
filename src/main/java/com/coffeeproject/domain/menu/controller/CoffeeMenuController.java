package com.coffeeproject.domain.menu.controller;

import com.coffeeproject.domain.menu.dto.CoffeeMenuResponse;
import com.coffeeproject.domain.menu.dto.PopularCoffeeMenuResponse;
import com.coffeeproject.domain.menu.service.CoffeeMenuService;
import com.coffeeproject.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class CoffeeMenuController {

    private final CoffeeMenuService coffeeMenuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CoffeeMenuResponse>>> getMenus() {
        return ResponseEntity.ok(ApiResponse.success(
                "메뉴 조회 성공",
                coffeeMenuService.getMenus()
        ));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularCoffeeMenuResponse>>> getPopularMenus() {
        return ResponseEntity.ok(ApiResponse.success(
                "인기 메뉴 조회 성공",
                coffeeMenuService.getPopularMenus()
        ));
    }
}
