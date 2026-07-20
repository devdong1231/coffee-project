package com.coffeeproject.domain.outbox.scheduler;

import com.coffeeproject.domain.outbox.service.OutboxEventRelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coffee.outbox.scheduler.enabled", havingValue = "true")
public class OutboxEventScheduler {

    private final OutboxEventRelayService outboxEventRelayService;

    @Scheduled(
            fixedDelayString = "${coffee.outbox.scheduler.fixed-delay-ms:5000}",
            initialDelayString = "${coffee.outbox.scheduler.initial-delay-ms:5000}"
    )
    public void publishPendingEvents() {
        outboxEventRelayService.publishPendingEvents();
    }
}
