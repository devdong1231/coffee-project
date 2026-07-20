package com.coffeeproject.domain.menu.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffeeproject.domain.menu.dto.PopularCoffeeMenuResponse;
import com.coffeeproject.domain.menu.service.CoffeeMenuService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CoffeeMenuControllerUnitTest {

    @Mock
    private CoffeeMenuService coffeeMenuService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CoffeeMenuController(coffeeMenuService))
                .build();
    }

    @Test
    void 인기_메뉴_목록을_조회한다() throws Exception {
        when(coffeeMenuService.getPopularMenus()).thenReturn(List.of(
                new PopularCoffeeMenuResponse(1L, "아메리카노", 4500L, 25L)
        ));

        mockMvc.perform(get("/api/menus/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("인기 메뉴 조회 성공"))
                .andExpect(jsonPath("$.data[0].menuId").value(1))
                .andExpect(jsonPath("$.data[0].name").value("아메리카노"))
                .andExpect(jsonPath("$.data[0].price").value(4500))
                .andExpect(jsonPath("$.data[0].orderCount").value(25));
    }
}
