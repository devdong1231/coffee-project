package com.coffeeproject.domain.order.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.repository.CoffeeMenuRepository;
import com.coffeeproject.domain.order.dto.OrderCreateRequest;
import com.coffeeproject.domain.order.repository.OrderRepository;
import com.coffeeproject.domain.outbox.repository.OutboxEventRepository;
import com.coffeeproject.domain.point.entity.Point;
import com.coffeeproject.domain.point.repository.PointHistoryRepository;
import com.coffeeproject.domain.point.repository.PointRepository;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.domain.user.repository.UserRepository;
import com.coffeeproject.global.exception.BusinessException;
import com.coffeeproject.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderFacadeConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoffeeMenuRepository coffeeMenuRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        pointHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        pointRepository.deleteAll();
        coffeeMenuRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 동일_사용자가_동시에_여러_주문을_요청해도_잔액이_음수가_되지_않고_한_건만_성공한다() throws Exception {
        User user = userRepository.save(User.create("사용자"));
        CoffeeMenu americano = coffeeMenuRepository.save(CoffeeMenu.create("아메리카노", 4500L));
        Point point = pointRepository.save(Point.create(user, 4500L));
        int requestCount = 2;

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executorService.submit(() -> {
                    readyLatch.countDown();
                    if (!startLatch.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 주문 시작 대기 시간이 초과되었습니다.");
                    }
                    return createOrder(user.getId(), americano.getId());
                }));
            }

            assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get());
            }

            executorService.shutdown();
            assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

            Point chargedPoint = pointRepository.findById(point.getId()).orElseThrow();
            assertThat(results).containsExactlyInAnyOrder(true, false);
            assertThat(chargedPoint.getBalance()).isZero();
            assertThat(orderRepository.findAll()).hasSize(1);
            assertThat(pointHistoryRepository.findAll()).hasSize(1);
            assertThat(outboxEventRepository.findAll()).hasSize(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    private boolean createOrder(Long userId, Long menuId) {
        try {
            orderFacade.createOrder(new OrderCreateRequest(
                    userId,
                    List.of(new OrderCreateRequest.OrderItemRequest(menuId, 1))
            ));
            return true;
        } catch (BusinessException exception) {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_POINT);
            return false;
        }
    }
}
