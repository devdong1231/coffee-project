package com.coffeeproject.domain.menu.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.repository.CoffeeMenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CoffeeMenuControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private CoffeeMenuRepository coffeeMenuRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();

        coffeeMenuRepository.deleteAll();

        CoffeeMenu americano = CoffeeMenu.create("아메리카노", 4500L);
        CoffeeMenu latte = CoffeeMenu.create("카페라떼", 5000L);
        CoffeeMenu soldOutMenu = CoffeeMenu.create("콜드브루", 5500L);
        soldOutMenu.soldOut();

        coffeeMenuRepository.save(americano);
        coffeeMenuRepository.save(soldOutMenu);
        coffeeMenuRepository.save(latte);
    }

    @Test
    void 판매_가능한_커피_메뉴_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("메뉴 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", contains("아메리카노", "카페라떼")))
                .andExpect(jsonPath("$.data[0].menuId").isNumber())
                .andExpect(jsonPath("$.data[0].price").value(4500))
                .andExpect(jsonPath("$.data[1].menuId").isNumber())
                .andExpect(jsonPath("$.data[1].price").value(5000));
    }
}
