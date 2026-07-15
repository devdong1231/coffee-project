package com.coffeeproject.domain.outbox.entity;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
