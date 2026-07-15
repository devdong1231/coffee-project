package com.coffeeproject.domain.point.controller;

import com.coffeeproject.domain.point.dto.PointChargeRequest;
import com.coffeeproject.domain.point.dto.PointChargeResponse;
import com.coffeeproject.domain.point.service.PointService;
import com.coffeeproject.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PointChargeResponse>> charge(
            @Valid @RequestBody PointChargeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "포인트 충전 성공",
                pointService.charge(request)
        ));
    }
}
