package com.daoninhthai.payment.entity;

import com.daoninhthai.payment.entity.enums.ScheduledPaymentStatus;
import com.daoninhthai.payment.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "recipient_wallet_id")
    private Long recipientWalletId;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(name = "description")
    private String description;

    @Column(name = "next_execution_date", nullable = false)
    private LocalDateTime nextExecutionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScheduledPaymentStatus status = ScheduledPaymentStatus.ACTIVE;

    @Column(name = "execution_count")
    @Builder.Default
    private Integer executionCount = 0;

    @Column(name = "max_executions")
    private Integer maxExecutions;

    @Column(name = "last_execution_date")
    private LocalDateTime lastExecutionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
