package com.coffeeproject.domain.point.service;

import com.coffeeproject.domain.point.dto.PointChargeRequest;
import com.coffeeproject.domain.point.dto.PointChargeResponse;
import com.coffeeproject.domain.point.entity.Point;
import com.coffeeproject.domain.point.entity.PointHistory;
import com.coffeeproject.domain.point.repository.PointHistoryRepository;
import com.coffeeproject.domain.point.repository.PointRepository;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.domain.user.repository.UserRepository;
import com.coffeeproject.global.exception.BusinessException;
import com.coffeeproject.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;
    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public PointChargeResponse charge(PointChargeRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Point point = pointRepository.findByUserIdWithLock(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        point.charge(request.amount());
        pointHistoryRepository.save(PointHistory.charge(user, request.amount(), point.getBalance()));

        return PointChargeResponse.from(point.getBalance());
    }
}
