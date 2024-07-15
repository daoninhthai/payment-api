package com.daoninhthai.payment.dto.request;

import com.daoninhthai.payment.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduledPaymentRequest {

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    private Long recipientWalletId;

    @NotBlank(message = "Cron expression is required")
    private String cronExpression;

    private String description;

    private Integer maxExecutions;
}
