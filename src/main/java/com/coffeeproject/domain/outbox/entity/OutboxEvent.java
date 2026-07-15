package com.coffeeproject.domain.outbox.entity;

import com.coffeeproject.domain.order.entity.Order;
import com.coffeeproject.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxEventType eventType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int retryCount;

    private LocalDateTime publishedAt;

    private OutboxEvent(OutboxEventType eventType, Order order, String payload) {
        validateOrder(order);
        validatePayload(payload);
        this.eventType = eventType;
        this.order = order;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    public static OutboxEvent orderCompleted(Order order, String payload) {
        return new OutboxEvent(OutboxEventType.ORDER_COMPLETED, order, payload);
    }

    public void publish() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void recordFailure(int maxRetryCount) {
        retryCount++;
        if (retryCount >= maxRetryCount) {
            this.status = OutboxEventStatus.FAILED;
        }
    }

    public void retry() {
        this.status = OutboxEventStatus.PENDING;
    }

    private static void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("주문은 필수입니다.");
        }
    }

    private static void validatePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Outbox payload는 필수입니다.");
        }
    }
}
