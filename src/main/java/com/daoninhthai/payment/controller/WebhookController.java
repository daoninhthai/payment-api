package com.daoninhthai.payment.controller;

import com.daoninhthai.payment.dto.request.WebhookRequest;
import com.daoninhthai.payment.entity.Webhook;
import com.daoninhthai.payment.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public ResponseEntity<Webhook> registerWebhook(
            @RequestParam Long userId,
            @Valid @RequestBody WebhookRequest request) {
        Webhook webhook = webhookService.registerWebhook(userId, request.getUrl(), request.getSecret());
        return new ResponseEntity<>(webhook, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Webhook>> getWebhooks(@RequestParam Long userId) {
        List<Webhook> webhooks = webhookService.getWebhooks(userId);
        return ResponseEntity.ok(webhooks);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.noContent().build();
    }
}
