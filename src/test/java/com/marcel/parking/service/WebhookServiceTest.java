package com.marcel.parking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcel.parking.dto.WebhookEventRequest;
import com.marcel.parking.entity.WebhookEvent;
import com.marcel.parking.enumtype.WebhookEventType;
import com.marcel.parking.exception.BusinessException;
import com.marcel.parking.repository.WebhookEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebhookServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private ParkingEventProcessor parkingEventProcessor;

    @InjectMocks
    private WebhookService webhookService;

    @Test
    @DisplayName("Deve ignorar webhook duplicado pela chave de idempotência")
    void shouldIgnoreDuplicatedWebhookByIdempotencyKey() {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                Instant.parse("2026-05-13T10:00:00Z"),
                null,
                null,
                null,
                WebhookEventType.ENTRY
        );

        when(webhookEventRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(true);

        webhookService.accept(request);

        verify(webhookEventRepository, never()).save(any());
        verify(parkingEventProcessor, never()).process(any());
    }

    @Test
    @DisplayName("Deve salvar webhook novo e processar evento")
    void shouldSaveNewWebhookAndProcessEvent() throws Exception {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                Instant.parse("2026-05-13T10:00:00Z"),
                null,
                null,
                null,
                WebhookEventType.ENTRY
        );

        WebhookEvent savedEvent = new WebhookEvent(
                "generated-key",
                "ABC1D23",
                WebhookEventType.ENTRY,
                "{}"
        );
        setId(savedEvent, 10L);

        when(webhookEventRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        when(objectMapper.writeValueAsString(request))
                .thenReturn("{}");

        when(webhookEventRepository.save(any(WebhookEvent.class)))
                .thenReturn(savedEvent);

        when(webhookEventRepository.findById(10L))
                .thenReturn(Optional.of(savedEvent));

        webhookService.accept(request);

        ArgumentCaptor<WebhookEvent> eventCaptor = ArgumentCaptor.forClass(WebhookEvent.class);

        verify(webhookEventRepository, atLeastOnce()).save(eventCaptor.capture());
        verify(parkingEventProcessor).process(request);

        WebhookEvent firstSavedEvent = eventCaptor.getAllValues().get(0);

        assertThat(firstSavedEvent.getLicensePlate()).isEqualTo("ABC1D23");
        assertThat(firstSavedEvent.getEventType()).isEqualTo(WebhookEventType.ENTRY);
        assertThat(firstSavedEvent.getPayload()).isEqualTo("{}");

        assertThat(savedEvent.getProcessed()).isTrue();
        assertThat(savedEvent.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve ignorar duplicidade detectada pela constraint única do banco")
    void shouldIgnoreDuplicateDetectedByDatabaseConstraint() throws Exception {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                Instant.parse("2026-05-13T10:00:00Z"),
                null,
                null,
                null,
                WebhookEventType.ENTRY
        );

        when(webhookEventRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        when(objectMapper.writeValueAsString(request))
                .thenReturn("{}");

        when(webhookEventRepository.save(any(WebhookEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        webhookService.accept(request);

        verify(parkingEventProcessor, never()).process(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando payload não puder ser serializado")
    void shouldThrowBusinessExceptionWhenPayloadCannotBeSerialized() throws Exception {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                Instant.parse("2026-05-13T10:00:00Z"),
                null,
                null,
                null,
                WebhookEventType.ENTRY
        );

        when(webhookEventRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        when(objectMapper.writeValueAsString(request))
                .thenThrow(new JsonProcessingException("invalid") {
                });

        assertThatThrownBy(() -> webhookService.accept(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid webhook payload");

        verify(webhookEventRepository, never()).save(any());
        verify(parkingEventProcessor, never()).process(any());
    }

    @Test
    @DisplayName("Deve processar evento assíncrono e marcar webhook como processado")
    void shouldProcessAsyncAndMarkWebhookAsProcessed() throws Exception {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                Instant.parse("2026-05-13T10:00:00Z"),
                null,
                null,
                null,
                WebhookEventType.ENTRY
        );

        WebhookEvent event = new WebhookEvent(
                "generated-key",
                "ABC1D23",
                WebhookEventType.ENTRY,
                "{}"
        );
        setId(event, 1L);

        when(webhookEventRepository.findById(1L))
                .thenReturn(Optional.of(event));

        webhookService.processAsync(1L, request);

        verify(parkingEventProcessor).process(request);
        verify(webhookEventRepository).save(event);

        assertThat(event.getProcessed()).isTrue();
        assertThat(event.getProcessedAt()).isNotNull();
    }

    private void setId(WebhookEvent event, Long id) throws Exception {
        Field field = WebhookEvent.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(event, id);
    }
}
