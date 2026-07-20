package com.coffeeproject.domain.menu.service;

import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.order.entity.OrderItem;
import com.coffeeproject.domain.order.repository.OrderRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularMenuRankingRecoveryService {

    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public PopularMenuRankingRecoveryResult rebuildRecentRankings() {
        List<LocalDate> dates = PopularMenuRankingPolicy.recentDates(clock);
        LocalDateTime from = dates.get(dates.size() - 1).atStartOfDay();
        LocalDateTime to = dates.get(0).plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findAllWithItemsCreatedBetween(from, to);
        Map<LocalDate, Map<Long, Long>> rankingByDate = aggregateByDate(orders);

        List<String> keys = dates.stream()
                .map(PopularMenuRankingPolicy::key)
                .toList();
        redisTemplate.delete(keys);

        long rebuiltItemCount = 0L;
        for (LocalDate date : dates) {
            Map<Long, Long> ranking = rankingByDate.getOrDefault(date, Map.of());
            if (ranking.isEmpty()) {
                continue;
            }

            String key = PopularMenuRankingPolicy.key(date);
            for (Map.Entry<Long, Long> entry : ranking.entrySet()) {
                redisTemplate.opsForZSet()
                        .add(key, String.valueOf(entry.getKey()), entry.getValue());
                rebuiltItemCount += entry.getValue();
            }
            redisTemplate.expire(key, PopularMenuRankingPolicy.RANKING_TTL);
        }

        log.info(
                "Rebuilt popular menu rankings. from={}, to={}, orderCount={}, itemCount={}",
                from,
                to,
                orders.size(),
                rebuiltItemCount
        );

        return new PopularMenuRankingRecoveryResult(orders.size(), rebuiltItemCount);
    }

    private Map<LocalDate, Map<Long, Long>> aggregateByDate(List<Order> orders) {
        Map<LocalDate, Map<Long, Long>> rankingByDate = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getCreatedAt() == null) {
                continue;
            }

            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            Map<Long, Long> ranking = rankingByDate.computeIfAbsent(orderDate, ignored -> new LinkedHashMap<>());
            for (OrderItem item : order.getItems()) {
                ranking.merge(item.getMenu().getId(), (long) item.getQuantity(), Long::sum);
            }
        }

        return rankingByDate;
    }

    public record PopularMenuRankingRecoveryResult(
            int orderCount,
            long itemCount
    ) {
    }
}
