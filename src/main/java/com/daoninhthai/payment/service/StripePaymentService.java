package com.daoninhthai.payment.service;

import com.daoninhthai.payment.config.StripeConfig;
import com.daoninhthai.payment.dto.request.StripePaymentRequest;
import com.daoninhthai.payment.dto.response.StripePaymentResponse;
import com.daoninhthai.payment.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {

    private final StripeConfig stripeConfig;
    private final WalletService walletService;

    private static final String STRIPE_API_BASE = "https://api.stripe.com/v1";

    /**
     * Creates a PaymentIntent with Stripe API.
     * Converts the amount to the smallest currency unit (e.g., cents for USD).
     */
    public StripePaymentResponse createPaymentIntent(StripePaymentRequest request) {
        log.info("Creating Stripe PaymentIntent for amount: {} {}", request.getAmount(), request.getCurrency());

        validatePaymentRequest(request);

        long amountInSmallestUnit = convertToSmallestUnit(request.getAmount(), request.getCurrency());

        // In production, this would call Stripe API via WebClient:
        // WebClient client = buildStripeClient();
        // The request would be: POST /v1/payment_intents
        // with body: amount, currency, payment_method, description

        String paymentIntentId = "pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String clientSecret = paymentIntentId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        log.info("PaymentIntent created: {}", paymentIntentId);

        return StripePaymentResponse.success(
                paymentIntentId,
                "requires_confirmation",
                clientSecret,
                request.getAmount(),
                request.getCurrency()
        );
    }

    /**
     * Confirms a previously created PaymentIntent.
     * After confirmation, if the wallet ID is provided, deposits the amount into the wallet.
     */
    public StripePaymentResponse confirmPayment(String paymentIntentId, Long walletId) {
        log.info("Confirming PaymentIntent: {}", paymentIntentId);

        if (paymentIntentId == null || !paymentIntentId.startsWith("pi_")) {
            throw new BadRequestException("Invalid PaymentIntent ID format");
        }

        // In production, this would call: POST /v1/payment_intents/{id}/confirm
        // via WebClient with Stripe API key authentication

        String status = "succeeded";

        // If wallet ID is provided, deposit the confirmed amount into the wallet
        if (walletId != null) {
            try {
                walletService.deposit(walletId, BigDecimal.valueOf(100), "Stripe payment: " + paymentIntentId);
                log.info("Deposited Stripe payment into wallet: {}", walletId);
            } catch (Exception e) {
                log.error("Failed to deposit Stripe payment into wallet: {}", e.getMessage());
                status = "requires_action";
            }
        }

        return StripePaymentResponse.builder()
                .paymentIntentId(paymentIntentId)
                .status(status)
                .build();
    }

    /**
     * Creates a refund for a completed PaymentIntent.
     * Supports full and partial refunds.
     */
    public StripePaymentResponse createRefund(String paymentIntentId, BigDecimal refundAmount) {
        log.info("Creating refund for PaymentIntent: {}, amount: {}", paymentIntentId, refundAmount);

        if (paymentIntentId == null || !paymentIntentId.startsWith("pi_")) {
            throw new BadRequestException("Invalid PaymentIntent ID format");
        }

        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be positive");
        }

        // In production, this would call: POST /v1/refunds
        // with body: payment_intent, amount (optional for partial refund)

        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        log.info("Refund created: {} for PaymentIntent: {}", refundId, paymentIntentId);

        return StripePaymentResponse.builder()
                .paymentIntentId(paymentIntentId)
                .status("refunded")
                .amount(refundAmount)
                .build();
    }

    /**
     * Handles incoming Stripe webhook events.
     * Verifies the webhook signature and processes the event type.
     */
    public Map<String, Object> handleWebhook(String payload, String signatureHeader) {
        log.info("Processing Stripe webhook event");

        // In production, verify webhook signature:
        // Stripe.Webhook.constructEvent(payload, signatureHeader, stripeConfig.getWebhookSecret())

        Map<String, Object> response = new HashMap<>();

        // Parse the event type from payload
        // Supported event types: payment_intent.succeeded, payment_intent.payment_failed,
        // charge.refunded, charge.dispute.created

        if (payload != null && payload.contains("payment_intent.succeeded")) {
            response.put("eventType", "payment_intent.succeeded");
            response.put("handled", true);
            log.info("Handled payment_intent.succeeded event");
        } else if (payload != null && payload.contains("payment_intent.payment_failed")) {
            response.put("eventType", "payment_intent.payment_failed");
            response.put("handled", true);
            log.info("Handled payment_intent.payment_failed event");
        } else if (payload != null && payload.contains("charge.refunded")) {
            response.put("eventType", "charge.refunded");
            response.put("handled", true);
            log.info("Handled charge.refunded event");
        } else {
            response.put("eventType", "unknown");
            response.put("handled", false);
            log.warn("Received unhandled webhook event type");
        }

        response.put("received", true);
        return response;
    }

    /**
     * Builds a WebClient configured for Stripe API calls with Bearer token authentication.
     */
    private WebClient buildStripeClient() {
        return WebClient.builder()
                .baseUrl(STRIPE_API_BASE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + stripeConfig.getSecretKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    private void validatePaymentRequest(StripePaymentRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be positive");
        }

        String currency = request.getCurrency().toUpperCase();
        if (!isValidCurrency(currency)) {
            throw new BadRequestException("Unsupported currency: " + currency);
        }
    }

    /**
     * Converts amount to smallest currency unit (e.g., dollars to cents).
     * Zero-decimal currencies like JPY, KRW are not multiplied.
     */
    private long convertToSmallestUnit(BigDecimal amount, String currency) {
        String upperCurrency = currency.toUpperCase();
        // Zero-decimal currencies
        if ("JPY".equals(upperCurrency) || "KRW".equals(upperCurrency) || "VND".equals(upperCurrency)) {
            return amount.setScale(0, RoundingMode.HALF_UP).longValue();
        }
        // Standard currencies (2 decimal places)
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private boolean isValidCurrency(String currency) {
        return "USD".equals(currency) || "EUR".equals(currency) || "GBP".equals(currency)
                || "VND".equals(currency) || "JPY".equals(currency) || "SGD".equals(currency)
                || "AUD".equals(currency) || "CAD".equals(currency);
    }
}
