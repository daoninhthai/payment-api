package com.daoninhthai.payment.dto.request;

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
public class StripePaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.50", message = "Minimum amount is 0.50")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;

    private String description;

    private Long walletId;
}
