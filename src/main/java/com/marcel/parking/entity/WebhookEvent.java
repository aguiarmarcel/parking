package com.marcel.parking.entity;

import com.marcel.parking.enumtype.WebhookEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "webhook_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_event_idempotency_key", columnNames = "idempotencyKey"),
        indexes = {
                @Index(name = "idx_webhook_event_processed", columnList = "processed"),
                @Index(name = "idx_webhook_event_license_type", columnList = "licensePlate,eventType")
        }
)
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookEventType eventType;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Column(nullable = false)
    private Boolean processed = false;

    @Column(nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();

    private Instant processedAt;

    public WebhookEvent(String idempotencyKey, String licensePlate, WebhookEventType eventType, String payload) {
        this.idempotencyKey = idempotencyKey;
        this.licensePlate = licensePlate;
        this.eventType = eventType;
        this.payload = payload;
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }
}
