package com.daoninhthai.payment.controller;

import com.daoninhthai.payment.dto.request.StripePaymentRequest;
import com.daoninhthai.payment.dto.response.StripePaymentResponse;
import com.daoninhthai.payment.service.StripePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripePaymentService stripePaymentService;

    /**
     * Creates a new Stripe PaymentIntent.
     * Returns the client_secret needed for frontend confirmation.
     */
    @PostMapping("/create-intent")
    public ResponseEntity<StripePaymentResponse> createPaymentIntent(
            @Valid @RequestBody StripePaymentRequest request) {
        StripePaymentResponse response = stripePaymentService.createPaymentIntent(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirms a PaymentIntent and optionally deposits into a wallet.
     */
    @PostMapping("/confirm")
    public ResponseEntity<StripePaymentResponse> confirmPayment(
            @RequestParam String paymentIntentId,
            @RequestParam(required = false) Long walletId) {
        StripePaymentResponse response = stripePaymentService.confirmPayment(paymentIntentId, walletId);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a refund for a completed payment.
     */
    @PostMapping("/refund")
    public ResponseEntity<StripePaymentResponse> createRefund(
            @RequestParam String paymentIntentId,
            @RequestParam(required = false) BigDecimal amount) {
        StripePaymentResponse response = stripePaymentService.createRefund(paymentIntentId, amount);
        return ResponseEntity.ok(response);
    }

    /**
     * Receives Stripe webhook events.
     * Stripe sends events for payment lifecycle updates.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {
        Map<String, Object> result = stripePaymentService.handleWebhook(payload, signatureHeader);
        return ResponseEntity.ok(result);
    }
}
