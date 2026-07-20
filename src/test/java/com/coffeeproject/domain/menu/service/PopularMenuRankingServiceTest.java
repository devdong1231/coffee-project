package com.coffeeproject.domain.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coffeeproject.domain.menu.service.PopularMenuRankingService.PopularMenuIncrement;
import com.coffeeproject.domain.menu.service.PopularMenuRankingService.PopularMenuRank;
import com.coffeeproject.global.exception.BusinessException;
import com.coffeeproject.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

@ExtendWith(MockitoExtension.class)
class PopularMenuRankingServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private PopularMenuRankingService popularMenuRankingService;

    @BeforeEach
    void setUp() {
        popularMenuRankingService = new PopularMenuRankingService(redisTemplate, FIXED_CLOCK);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void 최근_7일_판매_수량을_합산하고_상위_3개를_정렬해서_반환한다() {
        Set<TypedTuple<String>> todayRanking = Set.of(tuple("1", 3.0), tuple("2", 3.0), tuple("4", 10.0));
        Set<TypedTuple<String>> yesterdayRanking = Set.of(tuple("1", 2.0), tuple("2", 4.0), tuple("3", 5.0));

        when(zSetOperations.reverseRangeWithScores("coffee:ranking:2026-07-15", 0, -1))
                .thenReturn(todayRanking);
        when(zSetOperations.reverseRangeWithScores("coffee:ranking:2026-07-14", 0, -1))
                .thenReturn(yesterdayRanking);

        List<PopularMenuRank> result = popularMenuRankingService.getPopularRanks();

        assertThat(result).containsExactly(
                new PopularMenuRank(4L, 10L),
                new PopularMenuRank(2L, 7L),
                new PopularMenuRank(1L, 5L)
        );
    }

    @Test
    void Redis_조회에_실패하면_일시적_조회_실패_예외를_던진다() {
        when(zSetOperations.reverseRangeWithScores(anyString(), eq(0L), eq(-1L)))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertThatThrownBy(() -> popularMenuRankingService.getPopularRanks())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POPULAR_MENU_UNAVAILABLE);
    }

    @Test
    void 주문_수량만큼_오늘_날짜의_ZSET을_증가시키고_TTL을_설정한다() {
        popularMenuRankingService.increaseAfterCommit(List.of(
                new PopularMenuIncrement(1L, 2),
                new PopularMenuIncrement(2L, 1)
        ));

        verify(zSetOperations).incrementScore("coffee:ranking:2026-07-15", "1", 2);
        verify(zSetOperations).incrementScore("coffee:ranking:2026-07-15", "2", 1);
        verify(redisTemplate).expire("coffee:ranking:2026-07-15", Duration.ofDays(10));
    }

    private TypedTuple<String> tuple(String value, Double score) {
        TypedTuple<String> tuple = mock(TypedTuple.class);
        when(tuple.getValue()).thenReturn(value);
        when(tuple.getScore()).thenReturn(score);
        return tuple;
    }
}
