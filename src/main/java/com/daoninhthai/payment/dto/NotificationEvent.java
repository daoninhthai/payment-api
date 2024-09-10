package com.daoninhthai.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    public enum NotificationType {
        PAYMENT_CONFIRMATION,
        TRANSFER_SENT,
        TRANSFER_RECEIVED,
        LOW_BALANCE_ALERT,
        DEPOSIT_SUCCESS,
        WITHDRAWAL_SUCCESS,
        REFUND_PROCESSED,
        SCHEDULED_PAYMENT_EXECUTED
    }

    private NotificationType type;
    private String recipientEmail;
    private String recipientPhone;
    private String subject;
    private String body;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public static NotificationEvent paymentConfirmation(String email, String phone,
                                                         String transactionId, String amount) {
        return NotificationEvent.builder()
                .type(NotificationType.PAYMENT_CONFIRMATION)
                .recipientEmail(email)
                .recipientPhone(phone)
                .subject("Payment Confirmation - Transaction #" + transactionId)
                .body("Your payment of " + amount + " has been processed successfully.")
                .metadata(Map.of("transactionId", transactionId, "amount", amount))
                .build();
    }

    public static NotificationEvent transferNotification(String email, String phone,
                                                          String fromWallet, String toWallet,
                                                          String amount, boolean isSender) {
        String direction = isSender ? "sent to wallet #" + toWallet : "received from wallet #" + fromWallet;
        return NotificationEvent.builder()
                .type(isSender ? NotificationType.TRANSFER_SENT : NotificationType.TRANSFER_RECEIVED)
                .recipientEmail(email)
                .recipientPhone(phone)
                .subject("Transfer " + (isSender ? "Sent" : "Received"))
                .body("A transfer of " + amount + " has been " + direction + ".")
                .metadata(Map.of("fromWallet", fromWallet, "toWallet", toWallet, "amount", amount))
                .build();
    }

    public static NotificationEvent lowBalanceAlert(String email, String phone,
                                                     String walletId, String currentBalance,
                                                     String threshold) {
        return NotificationEvent.builder()
                .type(NotificationType.LOW_BALANCE_ALERT)
                .recipientEmail(email)
                .recipientPhone(phone)
                .subject("Low Balance Alert - Wallet #" + walletId)
                .body("Your wallet balance (" + currentBalance + ") is below the threshold of " + threshold + ".")
                .metadata(Map.of("walletId", walletId, "balance", currentBalance, "threshold", threshold))
                .build();
    }
}
