package com.marcel.parking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcel.parking.dto.WebhookEventRequest;
import com.marcel.parking.entity.WebhookEvent;
import com.marcel.parking.exception.BusinessException;
import com.marcel.parking.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ObjectMapper objectMapper;
    private final WebhookEventRepository webhookEventRepository;
    private final ParkingEventProcessor parkingEventProcessor;

    @Transactional
    public void accept(WebhookEventRequest request) {
        String idempotencyKey = buildIdempotencyKey(request);

        if (webhookEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Ignoring duplicated webhook event. key={}", idempotencyKey);
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(request);

            WebhookEvent webhookEvent = webhookEventRepository.save(
                    new WebhookEvent(
                            idempotencyKey,
                            request.licensePlate(),
                            request.eventType(),
                            payload
                    )
            );

            processAsync(webhookEvent.getId(), request);
        } catch (DataIntegrityViolationException exception) {
            log.info("Duplicated webhook event detected by unique constraint. key={}", idempotencyKey);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Invalid webhook payload");
        }
    }

    @Async
    public void processAsync(Long webhookEventId, WebhookEventRequest request) {
        parkingEventProcessor.process(request);

        webhookEventRepository.findById(webhookEventId).ifPresent(event -> {
            event.markAsProcessed();
            webhookEventRepository.save(event);
        });
    }

    private String buildIdempotencyKey(WebhookEventRequest request) {
        String rawKey = request.licensePlate()
                + "|"
                + request.eventType()
                + "|"
                + nullSafeInstant(request.entryTime())
                + "|"
                + nullSafeInstant(request.exitTime())
                + "|"
                + request.lat()
                + "|"
                + request.lng();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new BusinessException("Could not generate idempotency key");
        }
    }

    private String nullSafeInstant(Instant instant) {
        return instant == null ? "" : instant.toString();
    }
}