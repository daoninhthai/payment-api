package com.daoninhthai.payment.dto.response;

import com.daoninhthai.payment.entity.ScheduledPayment;
import com.daoninhthai.payment.entity.enums.ScheduledPaymentStatus;
import com.daoninhthai.payment.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPaymentResponse {

    private Long id;
    private Long walletId;
    private BigDecimal amount;
    private TransactionType type;
    private Long recipientWalletId;
    private String cronExpression;
    private String description;
    private LocalDateTime nextExecutionDate;
    private ScheduledPaymentStatus status;
    private Integer executionCount;
    private Integer maxExecutions;
    private LocalDateTime lastExecutionDate;
    private LocalDateTime createdAt;

    public static ScheduledPaymentResponse fromEntity(ScheduledPayment entity) {
        return ScheduledPaymentResponse.builder()
                .id(entity.getId())
                .walletId(entity.getWallet().getId())
                .amount(entity.getAmount())
                .type(entity.getType())
                .recipientWalletId(entity.getRecipientWalletId())
                .cronExpression(entity.getCronExpression())
                .description(entity.getDescription())
                .nextExecutionDate(entity.getNextExecutionDate())
                .status(entity.getStatus())
                .executionCount(entity.getExecutionCount())
                .maxExecutions(entity.getMaxExecutions())
                .lastExecutionDate(entity.getLastExecutionDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
