package com.daoninhthai.payment.service;

import com.daoninhthai.payment.entity.enums.TransactionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter paymentTransactionsTotal;
    private final Counter paymentAmountTotal;
    private final Counter depositCounter;
    private final Counter withdrawalCounter;
    private final Counter transferCounter;
    private final AtomicLong activeWalletsGaugeValue;
    private final AtomicLong walletBalanceGaugeValue;

    /**
     * Records a transaction event in the metrics system.
     * Increments the appropriate counters based on transaction type.
     */
    public void recordTransaction(TransactionType type) {
        paymentTransactionsTotal.increment();

        switch (type) {
            case DEPOSIT:
                depositCounter.increment();
                log.debug("Recorded deposit transaction metric");
                break;
            case WITHDRAWAL:
                withdrawalCounter.increment();
                log.debug("Recorded withdrawal transaction metric");
                break;
            case TRANSFER:
                transferCounter.increment();
                log.debug("Recorded transfer transaction metric");
                break;
            case REFUND:
                Counter.builder("payment_refunds_total")
                        .description("Total number of refund transactions")
                        .tag("type", "refund")
                        .register(meterRegistry)
                        .increment();
                log.debug("Recorded refund transaction metric");
                break;
        }
    }

    /**
     * Records the payment amount for aggregated metrics.
     * Converts BigDecimal to double for Micrometer counter.
     */
    public void recordPaymentAmount(BigDecimal amount, TransactionType type) {
        double amountValue = amount.abs().doubleValue();
        paymentAmountTotal.increment(amountValue);

        Counter.builder("payment_amount_by_type")
                .description("Payment amount by transaction type")
                .tag("type", type.name().toLowerCase())
                .register(meterRegistry)
                .increment(amountValue);

        log.debug("Recorded payment amount: {} for type: {}", amountValue, type);
    }

    /**
     * Updates the active wallets gauge.
     * Should be called when wallets are created or deactivated.
     */
    public void updateWalletMetrics(long activeCount, BigDecimal totalBalance) {
        activeWalletsGaugeValue.set(activeCount);
        walletBalanceGaugeValue.set(totalBalance.longValue());
        log.debug("Updated wallet metrics: activeCount={}, totalBalance={}", activeCount, totalBalance);
    }

    /**
     * Increments active wallet count by one.
     * Called when a new wallet is created.
     */
    public void incrementActiveWallets() {
        activeWalletsGaugeValue.incrementAndGet();
    }

    /**
     * Records a failed transaction for monitoring and alerting purposes.
     */
    public void recordFailedTransaction(TransactionType type, String reason) {
        Counter.builder("payment_failed_transactions_total")
                .description("Total number of failed transactions")
                .tag("type", type.name().toLowerCase())
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();

        log.warn("Failed transaction recorded: type={}, reason={}", type, reason);
    }
}
