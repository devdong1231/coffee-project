package com.coffeeproject.domain.menu.controller;

import com.coffeeproject.domain.menu.dto.PopularMenuRankingRecoveryResponse;
import com.coffeeproject.domain.menu.service.PopularMenuRankingRecoveryService;
import com.coffeeproject.domain.menu.service.PopularMenuRankingRecoveryService.PopularMenuRankingRecoveryResult;
import com.coffeeproject.global.response.ApiResponse;
import com.coffeeproject.global.security.AdminApiAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/menus/popular")
public class PopularMenuRankingRecoveryController {

    private final AdminApiAccessValidator adminApiAccessValidator;
    private final PopularMenuRankingRecoveryService popularMenuRankingRecoveryService;

    @PostMapping("/rebuild")
    public ResponseEntity<ApiResponse<PopularMenuRankingRecoveryResponse>> rebuildPopularMenuRanking(
            @RequestHeader(value = AdminApiAccessValidator.ADMIN_TOKEN_HEADER, required = false) String adminToken
    ) {
        adminApiAccessValidator.validate(adminToken);

        PopularMenuRankingRecoveryResult result = popularMenuRankingRecoveryService.rebuildRecentRankings();

        return ResponseEntity.ok(ApiResponse.success(
                "인기 메뉴 랭킹 복구 성공",
                PopularMenuRankingRecoveryResponse.from(result)
        ));
    }
}
