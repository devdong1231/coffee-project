package com.coffeeproject.domain.menu.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class PopularMenuRankingPolicy {

    public static final int POPULAR_MENU_LIMIT = 3;
    public static final Duration RANKING_TTL = Duration.ofDays(10);

    private static final String KEY_PREFIX = "coffee:ranking:";
    private static final int RECENT_DAYS = 7;

    private PopularMenuRankingPolicy() {
    }

    public static List<LocalDate> recentDates(Clock clock) {
        LocalDate today = LocalDate.now(clock);
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < RECENT_DAYS; i++) {
            dates.add(today.minusDays(i));
        }
        return dates;
    }

    public static String key(LocalDate date) {
        return KEY_PREFIX + date;
    }
}
