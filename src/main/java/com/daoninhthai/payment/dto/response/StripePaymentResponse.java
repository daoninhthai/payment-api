package com.daoninhthai.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripePaymentResponse {

    private String paymentIntentId;
    private String status;
    private String clientSecret;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String errorMessage;

    public static StripePaymentResponse success(String paymentIntentId, String status,
                                                 String clientSecret, BigDecimal amount, String currency) {
        return StripePaymentResponse.builder()
                .paymentIntentId(paymentIntentId)
                .status(status)
                .clientSecret(clientSecret)
                .amount(amount)
                .currency(currency)
                .build();
    }

    public static StripePaymentResponse error(String errorMessage) {
        return StripePaymentResponse.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }
}
