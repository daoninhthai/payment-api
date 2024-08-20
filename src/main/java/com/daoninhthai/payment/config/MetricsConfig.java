package com.daoninhthai.payment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MetricsConfig {

    private final AtomicLong activeWalletsCount = new AtomicLong(0);
    private final AtomicLong totalWalletBalance = new AtomicLong(0);

    @Bean
    public Counter paymentTransactionsTotal(MeterRegistry meterRegistry) {
        return Counter.builder("payment_transactions_total")
                .description("Total number of payment transactions processed")
                .tag("application", "payment-api")
                .register(meterRegistry);
    }

    @Bean
    public Counter paymentAmountTotal(MeterRegistry meterRegistry) {
        return Counter.builder("payment_amount_total")
                .description("Total payment amount processed")
                .tag("application", "payment-api")
                .register(meterRegistry);
    }

    @Bean
    public Counter depositCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_deposits_total")
                .description("Total number of deposit transactions")
                .tag("type", "deposit")
                .register(meterRegistry);
    }

    @Bean
    public Counter withdrawalCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_withdrawals_total")
                .description("Total number of withdrawal transactions")
                .tag("type", "withdrawal")
                .register(meterRegistry);
    }

    @Bean
    public Counter transferCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment_transfers_total")
                .description("Total number of transfer transactions")
                .tag("type", "transfer")
                .register(meterRegistry);
    }

    @Bean
    public AtomicLong activeWalletsGaugeValue(MeterRegistry meterRegistry) {
        Gauge.builder("active_wallets_count", activeWalletsCount, AtomicLong::get)
                .description("Current number of active wallets")
                .tag("application", "payment-api")
                .register(meterRegistry);
        return activeWalletsCount;
    }

    @Bean
    public AtomicLong walletBalanceGaugeValue(MeterRegistry meterRegistry) {
        Gauge.builder("wallet_balance_gauge", totalWalletBalance, AtomicLong::get)
                .description("Total balance across all wallets")
                .tag("application", "payment-api")
                .register(meterRegistry);
        return totalWalletBalance;
    }
}
