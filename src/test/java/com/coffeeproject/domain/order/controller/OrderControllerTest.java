package com.coffeeproject.domain.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.repository.CoffeeMenuRepository;
import com.coffeeproject.domain.order.repository.OrderRepository;
import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import com.coffeeproject.domain.outbox.entity.OutboxEventStatus;
import com.coffeeproject.domain.outbox.entity.OutboxEventType;
import com.coffeeproject.domain.outbox.repository.OutboxEventRepository;
import com.coffeeproject.domain.point.entity.Point;
import com.coffeeproject.domain.point.entity.PointHistory;
import com.coffeeproject.domain.point.entity.PointHistoryType;
import com.coffeeproject.domain.point.repository.PointHistoryRepository;
import com.coffeeproject.domain.point.repository.PointRepository;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class OrderControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoffeeMenuRepository coffeeMenuRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();

        outboxEventRepository.deleteAll();
        pointHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        pointRepository.deleteAll();
        coffeeMenuRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 커피를_주문하고_포인트로_결제한다() throws Exception {
        User user = userRepository.save(User.create("사용자"));
        pointRepository.save(Point.create(user, 20000L));
        CoffeeMenu americano = coffeeMenuRepository.save(CoffeeMenu.create("아메리카노", 4500L));
        CoffeeMenu latte = coffeeMenuRepository.save(CoffeeMenu.create("카페라떼", 4500L));

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "items": [
                                    {
                                      "menuId": %d,
                                      "quantity": 2
                                    },
                                    {
                                      "menuId": %d,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(user.getId(), americano.getId(), latte.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("주문 성공"))
                .andExpect(jsonPath("$.data.orderId").isNumber())
                .andExpect(jsonPath("$.data.paymentAmount").value(13500))
                .andExpect(jsonPath("$.data.remainingPoint").value(6500))
                .andReturn();

        Point point = pointRepository.findAll().get(0);
        assertThat(point.getBalance()).isEqualTo(6500L);
        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(countOrderItems()).isEqualTo(2L);

        PointHistory pointHistory = pointHistoryRepository.findAll().get(0);
        assertThat(pointHistory.getType()).isEqualTo(PointHistoryType.USE);
        assertThat(pointHistory.getAmount()).isEqualTo(13500L);
        assertThat(pointHistory.getBalanceAfter()).isEqualTo(6500L);
        assertThat(pointHistory.getOrder()).isNotNull();

        OutboxEvent outboxEvent = outboxEventRepository.findAll().get(0);
        assertThat(outboxEvent.getEventType()).isEqualTo(OutboxEventType.ORDER_COMPLETED);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(outboxEvent.getRetryCount()).isZero();
        assertThat(outboxEvent.getPayload()).contains("\"userId\":" + user.getId());
        assertThat(outboxEvent.getPayload()).contains("\"paymentAmount\":13500");
        assertThat(result.getResponse().getContentAsString()).contains("\"remainingPoint\":6500");
    }

    @Test
    void 포인트가_부족하면_400을_반환하고_주문을_저장하지_않는다() throws Exception {
        User user = userRepository.save(User.create("사용자"));
        pointRepository.save(Point.create(user, 1000L));
        CoffeeMenu americano = coffeeMenuRepository.save(CoffeeMenu.create("아메리카노", 4500L));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "items": [
                                    {
                                      "menuId": %d,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(user.getId(), americano.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("포인트가 부족합니다."));

        Point point = pointRepository.findAll().get(0);
        assertThat(point.getBalance()).isEqualTo(1000L);
        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(pointHistoryRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void 판매_불가능한_메뉴를_주문하면_400을_반환한다() throws Exception {
        User user = userRepository.save(User.create("사용자"));
        pointRepository.save(Point.create(user, 10000L));
        CoffeeMenu soldOutMenu = CoffeeMenu.create("콜드브루", 5500L);
        soldOutMenu.soldOut();
        coffeeMenuRepository.save(soldOutMenu);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "items": [
                                    {
                                      "menuId": %d,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(user.getId(), soldOutMenu.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("판매 가능한 메뉴가 아닙니다."));
    }

    @Test
    void 존재하지_않는_메뉴이면_404를_반환한다() throws Exception {
        User user = userRepository.save(User.create("사용자"));
        pointRepository.save(Point.create(user, 10000L));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "items": [
                                    {
                                      "menuId": 999,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(user.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("메뉴를 찾을 수 없습니다."));
    }

    @Test
    void 주문할_메뉴가_없으면_400을_반환한다() throws Exception {
        User user = userRepository.save(User.create("사용자"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "items": []
                                }
                                """.formatted(user.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("주문할 메뉴는 필수입니다.")));
    }

    @Test
    void 주문_항목이_null이면_400을_반환한다() throws Exception {
        User user = userRepository.save(User.create("사용자"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "items": [
                                    null
                                  ]
                                }
                                """.formatted(user.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("주문 항목은 필수입니다.")));
    }

    private Long countOrderItems() {
        return entityManager
                .createQuery("select count(oi) from OrderItem oi", Long.class)
                .getSingleResult();
    }
}
