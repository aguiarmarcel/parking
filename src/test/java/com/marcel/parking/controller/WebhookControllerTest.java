package com.marcel.parking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcel.parking.dto.WebhookEventRequest;
import com.marcel.parking.enumtype.WebhookEventType;
import com.marcel.parking.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
public class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebhookService webhookService;

    @Test
    @DisplayName("Deve receber evento ENTRY e retornar 200 OK")
    void shouldReceiveEntryWebhookAndReturnOk() throws Exception {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                Instant.parse("2026-05-13T10:00:00Z"),
                null,
                null,
                null,
                WebhookEventType.ENTRY
        );

        mockMvc.perform(post("/webhook")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(webhookService).accept(argThat(argument ->
                argument.licensePlate().equals("ABC1D23")
                        && argument.entryTime().equals(Instant.parse("2026-05-13T10:00:00Z"))
                        && argument.eventType() == WebhookEventType.ENTRY
        ));
    }

    @Test
    @DisplayName("Deve receber evento PARKED e retornar 200 OK")
    void shouldReceiveParkedWebhookAndReturnOk() throws Exception {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                null,
                null,
                null,
                null,
                WebhookEventType.PARKED
        );

        mockMvc.perform(post("/webhook")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(webhookService).accept(argThat(argument ->
                argument.licensePlate().equals("ABC1D23")
                        && argument.eventType() == WebhookEventType.PARKED
        ));
    }

    @Test
    @DisplayName("Deve receber evento EXIT e retornar 200 OK")
    void shouldReceiveExitWebhookAndReturnOk() throws Exception {
        WebhookEventRequest request = new WebhookEventRequest(
                "ABC1D23",
                null,
                Instant.parse("2026-05-13T12:00:00Z"),
                null,
                null,
                WebhookEventType.EXIT
        );

        mockMvc.perform(post("/webhook")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(webhookService).accept(argThat(argument ->
                argument.licensePlate().equals("ABC1D23")
                        && argument.exitTime().equals(Instant.parse("2026-05-13T12:00:00Z"))
                        && argument.eventType() == WebhookEventType.EXIT
        ));
    }

    @Test
    @DisplayName("Deve retornar 400 quando license_plate estiver vazio")
    void shouldReturnBadRequestWhenLicensePlateIsBlank() throws Exception {
        String payload = """
                {
                  "license_plate": "",
                  "entry_time": "2026-05-13T10:00:00Z",
                  "event_type": "ENTRY"
                }
                """;

        mockMvc.perform(post("/webhook")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(webhookService);
    }

    @Test
    @DisplayName("Deve retornar 400 quando event_type não for informado")
    void shouldReturnBadRequestWhenEventTypeIsMissing() throws Exception {
        String payload = """
                {
                  "license_plate": "ABC1D23",
                  "entry_time": "2026-05-13T10:00:00Z"
                }
                """;

        mockMvc.perform(post("/webhook")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(webhookService);
    }
}
