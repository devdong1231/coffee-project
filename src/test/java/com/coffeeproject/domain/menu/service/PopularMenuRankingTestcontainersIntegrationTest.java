package com.coffeeproject.domain.menu.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.repository.CoffeeMenuRepository;
import com.coffeeproject.domain.menu.service.PopularMenuRankingRecoveryService.PopularMenuRankingRecoveryResult;
import com.coffeeproject.domain.menu.service.PopularMenuRankingService.PopularMenuIncrement;
import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.order.repository.OrderRepository;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
        "coffee.outbox.scheduler.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.open-in-view=false"
})
@Testcontainers(disabledWithoutDocker = true)
class PopularMenuRankingTestcontainersIntegrationTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.4-alpine");

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("coffee_project")
            .withUsername("test")
            .withPassword("test");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoffeeMenuRepository coffeeMenuRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EntityManager entityManager;

    private PopularMenuRankingService popularMenuRankingService;
    private PopularMenuRankingRecoveryService popularMenuRankingRecoveryService;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });

        popularMenuRankingService = new PopularMenuRankingService(redisTemplate, FIXED_CLOCK);
        popularMenuRankingRecoveryService = new PopularMenuRankingRecoveryService(
                orderRepository,
                redisTemplate,
                FIXED_CLOCK
        );
    }

    @Test
    void Redis_ZSET을_실제로_증가시키고_TTL을_설정한다() {
        String key = PopularMenuRankingPolicy.key(TODAY);

        popularMenuRankingService.increaseAfterCommit(List.of(
                new PopularMenuIncrement(1L, 2),
                new PopularMenuIncrement(2L, 1)
        ));

        assertThat(redisTemplate.opsForZSet().score(key, "1")).isEqualTo(2.0);
        assertThat(redisTemplate.opsForZSet().score(key, "2")).isEqualTo(1.0);
        assertThat(redisTemplate.getExpire(key, TimeUnit.SECONDS))
                .isBetween(1L, PopularMenuRankingPolicy.RANKING_TTL.toSeconds());
    }

    @Test
    @Transactional
    void MySQL_주문_쿼리로_기간_내_주문과_상세_메뉴를_조회한다() {
        CoffeeMenu americano = coffeeMenuRepository.save(CoffeeMenu.create("아메리카노", 4500L));
        CoffeeMenu latte = coffeeMenuRepository.save(CoffeeMenu.create("카페라떼", 5000L));
        User user = userRepository.save(User.create("사용자"));
        Order inRangeOrder = saveOrder(user, americano, 2, LocalDateTime.of(2026, 7, 15, 10, 0));
        saveOrder(user, latte, 1, LocalDateTime.of(2026, 7, 8, 23, 59));

        entityManager.clear();

        List<Order> orders = orderRepository.findAllWithItemsCreatedBetween(
                LocalDateTime.of(2026, 7, 9, 0, 0),
                LocalDateTime.of(2026, 7, 16, 0, 0)
        );

        assertThat(orders).extracting(Order::getId)
                .containsExactly(inRangeOrder.getId());
        assertThat(orders.get(0).getItems()).hasSize(1);
        assertThat(orders.get(0).getItems().get(0).getMenu().getName()).isEqualTo("아메리카노");
    }

    @Test
    @Transactional
    void MySQL_주문_기준으로_Redis_ZSET을_복구한다() {
        CoffeeMenu americano = coffeeMenuRepository.save(CoffeeMenu.create("아메리카노", 4500L));
        CoffeeMenu latte = coffeeMenuRepository.save(CoffeeMenu.create("카페라떼", 5000L));
        User user = userRepository.save(User.create("사용자"));
        saveOrder(user, americano, 2, LocalDateTime.of(2026, 7, 15, 10, 0));
        saveOrder(user, latte, 3, LocalDateTime.of(2026, 7, 14, 11, 0));

        String todayKey = PopularMenuRankingPolicy.key(TODAY);
        String yesterdayKey = PopularMenuRankingPolicy.key(TODAY.minusDays(1));
        redisTemplate.opsForZSet().add(todayKey, "999", 99);
        redisTemplate.opsForZSet().add(yesterdayKey, "999", 99);
        entityManager.clear();

        PopularMenuRankingRecoveryResult result = popularMenuRankingRecoveryService.rebuildRecentRankings();

        assertThat(result.orderCount()).isEqualTo(2);
        assertThat(result.itemCount()).isEqualTo(5);
        assertThat(redisTemplate.opsForZSet().score(todayKey, String.valueOf(americano.getId()))).isEqualTo(2.0);
        assertThat(redisTemplate.opsForZSet().score(yesterdayKey, String.valueOf(latte.getId()))).isEqualTo(3.0);
        assertThat(redisTemplate.opsForZSet().score(todayKey, "999")).isNull();
        assertThat(redisTemplate.opsForZSet().score(yesterdayKey, "999")).isNull();
        assertThat(redisTemplate.getExpire(todayKey, TimeUnit.SECONDS))
                .isBetween(1L, PopularMenuRankingPolicy.RANKING_TTL.toSeconds());
        assertThat(redisTemplate.getExpire(yesterdayKey, TimeUnit.SECONDS))
                .isBetween(1L, PopularMenuRankingPolicy.RANKING_TTL.toSeconds());
    }

    private Order saveOrder(User user, CoffeeMenu menu, int quantity, LocalDateTime createdAt) {
        Order order = Order.create(user);
        order.addItem(menu, quantity);
        orderRepository.saveAndFlush(order);
        entityManager.createNativeQuery("update orders set created_at = ? where id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, order.getId())
                .executeUpdate();
        return order;
    }
}
