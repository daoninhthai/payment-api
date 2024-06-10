package com.daoninhthai.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {

    @Value("${stripe.api.secret-key:sk_test_default}")
    private String secretKey;

    @Value("${stripe.api.publishable-key:pk_test_default}")
    private String publishableKey;

    @Value("${stripe.webhook.secret:whsec_default}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        // Initialize Stripe API key on application startup
        // In production, this would set com.stripe.Stripe.apiKey = secretKey
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}
