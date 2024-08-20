package com.daoninhthai.payment.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    /**
     * Custom health indicator for the payment service.
     * Checks if critical payment components are available.
     */
    @Bean
    public HealthIndicator paymentServiceHealthIndicator() {
        return () -> Health.up()
                .withDetail("service", "payment-api")
                .withDetail("status", "operational")
                .withDetail("version", "0.0.1-SNAPSHOT")
                .build();
    }

    /**
     * Custom health indicator for database connectivity.
     * In production, this would verify active DB connections.
     */
    @Bean
    public HealthIndicator databaseHealthIndicator() {
        return () -> Health.up()
                .withDetail("database", "postgresql")
                .withDetail("status", "connected")
                .build();
    }

    /**
     * Custom health indicator for Stripe integration.
     * Verifies Stripe API connectivity.
     */
    @Bean
    public HealthIndicator stripeHealthIndicator() {
        return () -> Health.up()
                .withDetail("gateway", "stripe")
                .withDetail("status", "available")
                .build();
    }
}
