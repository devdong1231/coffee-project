package com.coffeeproject.domain.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.order.repository.OrderRepository;
import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import com.coffeeproject.domain.outbox.entity.OutboxEventStatus;
import com.coffeeproject.domain.outbox.repository.OutboxEventRepository;
import com.coffeeproject.domain.point.repository.PointHistoryRepository;
import com.coffeeproject.domain.point.repository.PointRepository;
import com.coffeeproject.domain.user.entity.User;
import com.coffeeproject.domain.user.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "coffee.outbox.batch-size=10",
        "coffee.outbox.max-retry-count=3"
})
class OutboxEventRelayServiceIntegrationTest {

    private static final String REQUEST_PATH = "/mock-api/orders/completed";
    private static final BlockingQueue<RecordedRequest> RECORDED_REQUESTS = new LinkedBlockingQueue<>();
    private static final AtomicInteger RESPONSE_STATUS = new AtomicInteger(200);
    private static HttpServer httpServer;

    @Autowired
    private OutboxEventRelayService outboxEventRelayService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void outboxProperties(DynamicPropertyRegistry registry) {
        startHttpServer();
        registry.add(
                "coffee.outbox.external-url",
                () -> "http://localhost:" + httpServer.getAddress().getPort() + REQUEST_PATH
        );
    }

    @BeforeEach
    void setUp() {
        RECORDED_REQUESTS.clear();
        RESPONSE_STATUS.set(200);

        cleanDatabase();
    }

    @AfterEach
    void cleanUp() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        outboxEventRepository.deleteAll();
        pointHistoryRepository.deleteAll();
        orderRepository.deleteAll();
        pointRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterAll
    static void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void PENDING_이벤트를_실제_HTTP_서버로_전송하고_PUBLISHED로_변경한다() {
        OutboxEvent event = saveOutboxEvent("""
                {"orderId":1,"paymentAmount":13500}
                """);

        int publishedCount = outboxEventRelayService.publishPendingEvents();

        RecordedRequest request = RECORDED_REQUESTS.poll();
        OutboxEvent publishedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(publishedCount).isEqualTo(1);
        assertThat(request).isNotNull();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo(REQUEST_PATH);
        assertThat(request.contentType()).contains("application/json");
        assertThat(request.body()).isEqualTo("""
                {"orderId":1,"paymentAmount":13500}
                """);
        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(publishedEvent.getRetryCount()).isZero();
        assertThat(publishedEvent.getPublishedAt()).isNotNull();
    }

    @Test
    void 실제_HTTP_서버가_오류를_반환하면_재시도_횟수를_증가시키고_PENDING을_유지한다() {
        RESPONSE_STATUS.set(500);
        OutboxEvent event = saveOutboxEvent("""
                {"orderId":2,"paymentAmount":4500}
                """);

        int publishedCount = outboxEventRelayService.publishPendingEvents();

        RecordedRequest request = RECORDED_REQUESTS.poll();
        OutboxEvent failedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();

        assertThat(publishedCount).isEqualTo(1);
        assertThat(request).isNotNull();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo(REQUEST_PATH);
        assertThat(failedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(failedEvent.getRetryCount()).isEqualTo(1);
        assertThat(failedEvent.getPublishedAt()).isNull();
    }

    private OutboxEvent saveOutboxEvent(String payload) {
        User user = userRepository.save(User.create("사용자"));
        Order order = orderRepository.save(Order.create(user));
        return outboxEventRepository.save(OutboxEvent.orderCompleted(order, payload));
    }

    private static void startHttpServer() {
        if (httpServer != null) {
            return;
        }

        try {
            httpServer = HttpServer.create(new InetSocketAddress(0), 0);
            httpServer.createContext(REQUEST_PATH, OutboxEventRelayServiceIntegrationTest::handle);
            httpServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start test HTTP server.", exception);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        List<String> contentTypes = exchange.getRequestHeaders().getOrDefault("Content-Type", List.of());
        RECORDED_REQUESTS.add(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                contentTypes.isEmpty() ? "" : contentTypes.get(0),
                body
        ));

        int status = RESPONSE_STATUS.get();
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private record RecordedRequest(
            String method,
            String path,
            String contentType,
            String body
    ) {
    }
}
