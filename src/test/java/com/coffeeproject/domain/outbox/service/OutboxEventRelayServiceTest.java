package com.coffeeproject.domain.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.domain.outbox.client.OutboxExternalClient;
import com.coffeeproject.domain.outbox.config.OutboxProperties;
import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import com.coffeeproject.domain.outbox.entity.OutboxEventStatus;
import com.coffeeproject.domain.outbox.repository.OutboxEventRepository;
import com.coffeeproject.domain.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OutboxEventRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxExternalClient outboxExternalClient;

    private OutboxEventRelayService outboxEventRelayService;
    private OutboxProperties outboxProperties;

    @BeforeEach
    void setUp() {
        outboxProperties = new OutboxProperties();
        outboxProperties.setBatchSize(10);
        outboxProperties.setMaxRetryCount(3);
        outboxEventRelayService = new OutboxEventRelayService(
                outboxEventRepository,
                outboxExternalClient,
                outboxProperties
        );
    }

    @Test
    void PENDING_이벤트_전송에_성공하면_PUBLISHED로_변경한다() {
        OutboxEvent event = outboxEvent();
        when(outboxEventRepository.findByStatusOrderByIdAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, 10)
        )).thenReturn(List.of(event));

        int publishedCount = outboxEventRelayService.publishPendingEvents();

        assertThat(publishedCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxExternalClient).send(event);
    }

    @Test
    void 외부_API_전송에_실패하면_재시도_횟수를_증가시킨다() {
        OutboxEvent event = outboxEvent();
        when(outboxEventRepository.findByStatusOrderByIdAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, 10)
        )).thenReturn(List.of(event));
        doThrow(new IllegalStateException("external api down"))
                .when(outboxExternalClient)
                .send(any(OutboxEvent.class));

        outboxEventRelayService.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
    }

    @Test
    void 최대_재시도_횟수에_도달하면_FAILED로_변경한다() {
        OutboxEvent event = outboxEvent();
        event.recordFailure(3);
        event.retry();
        event.recordFailure(3);
        event.retry();

        when(outboxEventRepository.findByStatusOrderByIdAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, 10)
        )).thenReturn(List.of(event));
        doThrow(new IllegalStateException("external api down"))
                .when(outboxExternalClient)
                .send(any(OutboxEvent.class));

        outboxEventRelayService.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(3);
    }

    private OutboxEvent outboxEvent() {
        User user = User.create("사용자");
        Order order = Order.create(user);
        return OutboxEvent.orderCompleted(order, "{}");
    }
}
