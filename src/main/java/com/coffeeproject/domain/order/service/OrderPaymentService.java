package com.coffeeproject.domain.order.service;

import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.point.entity.Point;
import com.coffeeproject.domain.point.entity.PointHistory;
import com.coffeeproject.domain.point.repository.PointHistoryRepository;
import com.coffeeproject.domain.point.repository.PointRepository;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.global.exception.BusinessException;
import com.coffeeproject.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public Long pay(User user, Order order, Long paymentAmount) {
        Point point = pointRepository.findByUserIdWithLock(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        validateEnoughPoint(point, paymentAmount);
        point.use(paymentAmount);

        pointHistoryRepository.save(PointHistory.use(user, order, paymentAmount, point.getBalance()));
        return point.getBalance();
    }

    private void validateEnoughPoint(Point point, Long paymentAmount) {
        if (point.getBalance() < paymentAmount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
    }
}
