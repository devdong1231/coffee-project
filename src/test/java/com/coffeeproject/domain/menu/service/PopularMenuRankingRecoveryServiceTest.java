package com.coffeeproject.domain.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.service.PopularMenuRankingRecoveryService.PopularMenuRankingRecoveryResult;
import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.order.entity.OrderItem;
import com.coffeeproject.domain.order.repository.OrderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class PopularMenuRankingRecoveryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private PopularMenuRankingRecoveryService popularMenuRankingRecoveryService;

    @BeforeEach
    void setUp() {
        popularMenuRankingRecoveryService = new PopularMenuRankingRecoveryService(
                orderRepository,
                redisTemplate,
                FIXED_CLOCK
        );
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void 최근_7일_Redis_ZSET을_삭제하고_DB_주문_상세_기준으로_재생성한다() {
        LocalDateTime from = LocalDateTime.of(2026, 7, 9, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 16, 0, 0);
        Order todayOrder = order(LocalDateTime.of(2026, 7, 15, 10, 0), item(1L, 2), item(2L, 1));
        Order yesterdayOrder = order(LocalDateTime.of(2026, 7, 14, 11, 0), item(1L, 3));

        when(orderRepository.findAllWithItemsCreatedBetween(from, to))
                .thenReturn(List.of(todayOrder, yesterdayOrder));

        PopularMenuRankingRecoveryResult result = popularMenuRankingRecoveryService.rebuildRecentRankings();

        verify(redisTemplate).delete(List.of(
                "coffee:ranking:2026-07-15",
                "coffee:ranking:2026-07-14",
                "coffee:ranking:2026-07-13",
                "coffee:ranking:2026-07-12",
                "coffee:ranking:2026-07-11",
                "coffee:ranking:2026-07-10",
                "coffee:ranking:2026-07-09"
        ));
        verify(zSetOperations).add("coffee:ranking:2026-07-15", "1", 2);
        verify(zSetOperations).add("coffee:ranking:2026-07-15", "2", 1);
        verify(zSetOperations).add("coffee:ranking:2026-07-14", "1", 3);
        verify(redisTemplate).expire("coffee:ranking:2026-07-15", Duration.ofDays(10));
        verify(redisTemplate).expire("coffee:ranking:2026-07-14", Duration.ofDays(10));

        assertThat(result.orderCount()).isEqualTo(2);
        assertThat(result.itemCount()).isEqualTo(6);
    }

    private Order order(LocalDateTime createdAt, OrderItem... items) {
        Order order = mock(Order.class);
        when(order.getCreatedAt()).thenReturn(createdAt);
        when(order.getItems()).thenReturn(List.of(items));
        return order;
    }

    private OrderItem item(Long menuId, int quantity) {
        CoffeeMenu menu = mock(CoffeeMenu.class);
        when(menu.getId()).thenReturn(menuId);

        OrderItem item = mock(OrderItem.class);
        when(item.getMenu()).thenReturn(menu);
        when(item.getQuantity()).thenReturn(quantity);
        return item;
    }
}
