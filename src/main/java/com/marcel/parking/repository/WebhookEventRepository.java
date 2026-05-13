package com.marcel.parking.repository;

import com.marcel.parking.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
