package com.marcel.parking.controller;

import com.marcel.parking.dto.WebhookEventRequest;
import com.marcel.parking.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/webhook")
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody WebhookEventRequest request) {
        webhookService.accept(request);
        return ResponseEntity.ok().build();
    }
}
