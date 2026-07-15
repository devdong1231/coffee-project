package com.coffeeproject.domain.point.controller;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffeeproject.domain.point.dto.PointChargeRequest;
import com.coffeeproject.domain.point.entity.Point;
import com.coffeeproject.domain.point.repository.PointHistoryRepository;
import com.coffeeproject.domain.point.repository.PointRepository;
import com.coffeeproject.domain.point.service.PointService;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.domain.user.repository.UserRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PointControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private PointService pointService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();

        pointHistoryRepository.deleteAll();
        pointRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 포인트를_충전한다() throws Exception {
        User user = userRepository.save(User.create("사용자"));
        Point point = pointRepository.save(Point.create(user, 1000L));

        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "amount": 10000
                                }
                                """.formatted(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("포인트 충전 성공"))
                .andExpect(jsonPath("$.data.balance").value(11000));

        Point chargedPoint = pointRepository.findById(point.getId()).orElseThrow();
        assertThat(chargedPoint.getBalance()).isEqualTo(11000L);
        assertThat(pointHistoryRepository.findAll()).hasSize(1);
        assertThat(pointHistoryRepository.findAll().get(0).getBalanceAfter()).isEqualTo(11000L);
    }

    @Test
    void 충전_금액이_0_이하이면_400을_반환한다() throws Exception {
        User user = userRepository.save(User.create("사용자"));
        pointRepository.save(Point.create(user));

        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "amount": 0
                                }
                                """.formatted(user.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("충전 금액은 0보다 커야 합니다.")));
    }

    @Test
    void 존재하지_않는_사용자이면_404를_반환한다() throws Exception {
        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999,
                                  "amount": 10000
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    void 요청_본문이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("요청 본문이 올바르지 않습니다."));
    }

    @Test
    void 요청_본문이_잘못된_JSON이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/points/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "amount": 10000
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("요청 본문이 올바르지 않습니다."));
    }

    @Test
    void 동일_사용자에게_동시에_포인트를_충전해도_금액이_유실되지_않는다() throws Exception {
        User user = userRepository.save(User.create("사용자"));
        Point point = pointRepository.save(Point.create(user, 0L));
        int requestCount = 10;
        long chargeAmount = 1000L;

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            futures.add(executorService.submit(() -> {
                startLatch.await();
                pointService.charge(new PointChargeRequest(user.getId(), chargeAmount));
                return null;
            }));
        }

        startLatch.countDown();
        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);

        Point chargedPoint = pointRepository.findById(point.getId()).orElseThrow();
        assertThat(terminated).isTrue();
        assertThat(chargedPoint.getBalance()).isEqualTo(requestCount * chargeAmount);
        assertThat(pointHistoryRepository.findAll()).hasSize(requestCount);
    }
}
