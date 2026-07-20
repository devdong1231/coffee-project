package com.coffeeproject.domain.outbox.client;

import com.coffeeproject.domain.outbox.entity.OutboxEvent;

public interface OutboxExternalClient {

    void send(OutboxEvent event);
}
