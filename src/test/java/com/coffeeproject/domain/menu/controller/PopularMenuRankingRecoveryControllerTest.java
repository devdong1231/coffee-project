package com.coffeeproject.domain.menu.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffeeproject.domain.menu.service.PopularMenuRankingRecoveryService;
import com.coffeeproject.domain.menu.service.PopularMenuRankingRecoveryService.PopularMenuRankingRecoveryResult;
import com.coffeeproject.global.exception.GlobalExceptionHandler;
import com.coffeeproject.global.security.AdminApiAccessValidator;
import com.coffeeproject.global.security.AdminApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PopularMenuRankingRecoveryControllerTest {

    @Mock
    private PopularMenuRankingRecoveryService popularMenuRankingRecoveryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminApiProperties adminApiProperties = new AdminApiProperties();
        adminApiProperties.setToken("admin-token");

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PopularMenuRankingRecoveryController(
                        new AdminApiAccessValidator(adminApiProperties),
                        popularMenuRankingRecoveryService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 인기_메뉴_랭킹_복구를_실행한다() throws Exception {
        when(popularMenuRankingRecoveryService.rebuildRecentRankings())
                .thenReturn(new PopularMenuRankingRecoveryResult(2, 6L));

        mockMvc.perform(post("/api/admin/menus/popular/rebuild")
                        .header(AdminApiAccessValidator.ADMIN_TOKEN_HEADER, "admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("인기 메뉴 랭킹 복구 성공"))
                .andExpect(jsonPath("$.data.orderCount").value(2))
                .andExpect(jsonPath("$.data.itemCount").value(6));

        verify(popularMenuRankingRecoveryService).rebuildRecentRankings();
    }

    @Test
    void 관리자_토큰이_일치하지_않으면_인기_메뉴_랭킹_복구를_실행하지_않는다() throws Exception {
        mockMvc.perform(post("/api/admin/menus/popular/rebuild")
                        .header(AdminApiAccessValidator.ADMIN_TOKEN_HEADER, "invalid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("관리자 권한이 필요합니다."));

        verifyNoInteractions(popularMenuRankingRecoveryService);
    }
}
