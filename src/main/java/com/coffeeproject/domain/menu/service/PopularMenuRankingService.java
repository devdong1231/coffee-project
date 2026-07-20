package com.coffeeproject.domain.menu.service;

import com.coffeeproject.global.exception.BusinessException;
import com.coffeeproject.global.exception.ErrorCode;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularMenuRankingService {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public void increaseAfterCommit(List<PopularMenuIncrement> increments) {
        if (increments.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    increase(increments);
                }
            });
            return;
        }

        increase(increments);
    }

    public List<PopularMenuRank> getPopularRanks() {
        Map<Long, Long> menuIdToOrderCount = new HashMap<>();

        try {
            for (var date : PopularMenuRankingPolicy.recentDates(clock)) {
                Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                        .reverseRangeWithScores(PopularMenuRankingPolicy.key(date), 0, -1);
                mergeRanking(menuIdToOrderCount, tuples);
            }
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.POPULAR_MENU_UNAVAILABLE);
        }

        return menuIdToOrderCount.entrySet()
                .stream()
                .map(entry -> new PopularMenuRank(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing(PopularMenuRank::orderCount, Comparator.reverseOrder())
                        .thenComparing(PopularMenuRank::menuId))
                .limit(PopularMenuRankingPolicy.POPULAR_MENU_LIMIT)
                .toList();
    }

    private void increase(List<PopularMenuIncrement> increments) {
        String key = PopularMenuRankingPolicy.key(java.time.LocalDate.now(clock));

        try {
            for (PopularMenuIncrement increment : increments) {
                redisTemplate.opsForZSet()
                        .incrementScore(key, String.valueOf(increment.menuId()), increment.quantity());
            }
            redisTemplate.expire(key, PopularMenuRankingPolicy.RANKING_TTL);
        } catch (RuntimeException exception) {
            log.error("Failed to update popular menu ranking. key={}", key, exception);
        }
    }

    private void mergeRanking(Map<Long, Long> menuIdToOrderCount, Set<TypedTuple<String>> tuples) {
        if (tuples == null || tuples.isEmpty()) {
            return;
        }

        for (TypedTuple<String> tuple : tuples) {
            Long menuId = parseMenuId(tuple.getValue());
            Long orderCount = toOrderCount(tuple.getScore());
            if (menuId == null || orderCount <= 0) {
                continue;
            }

            menuIdToOrderCount.merge(menuId, orderCount, Long::sum);
        }
    }

    private Long parseMenuId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            log.warn("Invalid popular menu ranking value. value={}", value);
            return null;
        }
    }

    private Long toOrderCount(Double score) {
        if (score == null) {
            return 0L;
        }
        return Math.round(score);
    }

    public record PopularMenuIncrement(Long menuId, int quantity) {
    }

    public record PopularMenuRank(Long menuId, Long orderCount) {
    }
}
