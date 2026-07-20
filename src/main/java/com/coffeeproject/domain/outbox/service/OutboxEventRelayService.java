package com.coffeeproject.domain.outbox.service;

import com.coffeeproject.domain.outbox.client.OutboxExternalClient;
import com.coffeeproject.domain.outbox.config.OutboxProperties;
import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import com.coffeeproject.domain.outbox.entity.OutboxEventStatus;
import com.coffeeproject.domain.outbox.repository.OutboxEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxExternalClient outboxExternalClient;
    private final OutboxProperties outboxProperties;

    @Transactional
    public int publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByIdAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, outboxProperties.getBatchSize())
        );

        events.forEach(this::publish);
        return events.size();
    }

    private void publish(OutboxEvent event) {
        try {
            outboxExternalClient.send(event);
            event.publish();
        } catch (RuntimeException exception) {
            event.recordFailure(outboxProperties.getMaxRetryCount());
            log.warn(
                    "Failed to publish outbox event. eventId={}, retryCount={}, status={}",
                    event.getId(),
                    event.getRetryCount(),
                    event.getStatus(),
                    exception
            );
        }
    }
}
