package com.daoninhthai.payment.service;

import com.daoninhthai.payment.entity.User;
import com.daoninhthai.payment.entity.Webhook;
import com.daoninhthai.payment.entity.WebhookEvent;
import com.daoninhthai.payment.exception.ResourceNotFoundException;
import com.daoninhthai.payment.repository.UserRepository;
import com.daoninhthai.payment.repository.WebhookEventRepository;
import com.daoninhthai.payment.repository.WebhookRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Webhook registerWebhook(Long userId, String url, String secret) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Webhook webhook = Webhook.builder()
                .user(user)
                .url(url)
                .secret(secret)
                .active(true)
                .build();

        return webhookRepository.save(webhook);
    }

    @Async
    public void sendWebhookEvent(Long userId, String eventType, Object payload) {
        List<Webhook> webhooks = webhookRepository.findByUserIdAndActiveTrue(userId);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload", e);
            return;
        }

        for (Webhook webhook : webhooks) {
            WebhookEvent event = WebhookEvent.builder()
                    .webhook(webhook)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(WebhookEvent.WebhookEventStatus.PENDING)
                    .attempts(0)
                    .build();

            event = webhookEventRepository.save(event);
            deliverWebhook(event, webhook);
        }
    }

    private void deliverWebhook(WebhookEvent event, Webhook webhook) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Event", event.getEventType());
            headers.set("X-Webhook-Signature", generateSignature(event.getPayload(), webhook.getSecret()));

            HttpEntity<String> request = new HttpEntity<>(event.getPayload(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    webhook.getUrl(), HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                event.setStatus(WebhookEvent.WebhookEventStatus.SENT);
                event.setSentAt(LocalDateTime.now());
            } else {
                event.setStatus(WebhookEvent.WebhookEventStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("Failed to deliver webhook to {}: {}", webhook.getUrl(), e.getMessage());
            event.setStatus(WebhookEvent.WebhookEventStatus.FAILED);
        }

        event.setAttempts(event.getAttempts() + 1);
        webhookEventRepository.save(event);
    }

    @Transactional
    public void retryFailedWebhooks() {
        List<WebhookEvent> failedEvents = webhookEventRepository
                .findByStatusAndAttemptsLessThan(WebhookEvent.WebhookEventStatus.FAILED, 3);

        for (WebhookEvent event : failedEvents) {
            Webhook webhook = event.getWebhook();
            if (webhook.isActive()) {
                deliverWebhook(event, webhook);
            }
        }
    }

    public List<Webhook> getWebhooks(Long userId) {
        return webhookRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteWebhook(Long webhookId) {
        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", webhookId));
        webhook.setActive(false);
        webhookRepository.save(webhook);
    }

    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate webhook signature", e);
        }
    }
}
