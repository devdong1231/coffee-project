package com.coffeeproject.domain.outbox.client;

import com.coffeeproject.domain.outbox.config.OutboxProperties;
import com.coffeeproject.domain.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class RestClientOutboxExternalClient implements OutboxExternalClient {

    private final RestClient.Builder restClientBuilder;
    private final OutboxProperties outboxProperties;

    @Override
    public void send(OutboxEvent event) {
        if (!StringUtils.hasText(outboxProperties.getExternalUrl())) {
            throw new IllegalStateException("Outbox external URL is not configured.");
        }

        restClientBuilder.build()
                .post()
                .uri(outboxProperties.getExternalUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(event.getPayload())
                .retrieve()
                .toBodilessEntity();
    }
}
