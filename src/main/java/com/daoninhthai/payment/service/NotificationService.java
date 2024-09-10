package com.daoninhthai.payment.service;

import com.daoninhthai.payment.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final SmsService smsService;

    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("100000");

    /**
     * Sends a payment confirmation notification via both email and SMS.
     * Runs asynchronously to avoid blocking the main transaction flow.
     */
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendPaymentConfirmation(String email, String phone,
                                                            String transactionId, BigDecimal amount,
                                                            String transactionType) {
        log.info("Sending payment confirmation for transaction: {}", transactionId);

        NotificationEvent event = NotificationEvent.paymentConfirmation(
                email, phone, transactionId, amount.toPlainString());

        // Send email notification
        if (email != null && !email.isBlank()) {
            emailService.sendEmail(email, event.getSubject(), event.getBody());
        }

        // Send SMS notification
        if (phone != null && !phone.isBlank()) {
            smsService.sendPaymentNotificationSms(phone, transactionId,
                    amount.toPlainString(), transactionType);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Sends transfer notifications to both sender and receiver.
     * Each party receives appropriate messaging about the transfer direction.
     */
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendTransferNotification(String senderEmail, String senderPhone,
                                                             String receiverEmail, String receiverPhone,
                                                             Long fromWalletId, Long toWalletId,
                                                             BigDecimal amount) {
        log.info("Sending transfer notification: wallet {} -> wallet {}, amount: {}",
                fromWalletId, toWalletId, amount);

        // Notify sender
        NotificationEvent senderEvent = NotificationEvent.transferNotification(
                senderEmail, senderPhone,
                fromWalletId.toString(), toWalletId.toString(),
                amount.toPlainString(), true);

        if (senderEmail != null && !senderEmail.isBlank()) {
            emailService.sendEmail(senderEmail, senderEvent.getSubject(), senderEvent.getBody());
        }
        if (senderPhone != null && !senderPhone.isBlank()) {
            smsService.sendSms(senderPhone, senderEvent.getBody());
        }

        // Notify receiver
        NotificationEvent receiverEvent = NotificationEvent.transferNotification(
                receiverEmail, receiverPhone,
                fromWalletId.toString(), toWalletId.toString(),
                amount.toPlainString(), false);

        if (receiverEmail != null && !receiverEmail.isBlank()) {
            emailService.sendEmail(receiverEmail, receiverEvent.getSubject(), receiverEvent.getBody());
        }
        if (receiverPhone != null && !receiverPhone.isBlank()) {
            smsService.sendSms(receiverPhone, receiverEvent.getBody());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Sends a low balance alert when wallet balance drops below the threshold.
     * Checks the balance after withdrawals and transfers.
     */
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendLowBalanceAlert(String email, String phone,
                                                        Long walletId, BigDecimal currentBalance) {
        if (currentBalance.compareTo(LOW_BALANCE_THRESHOLD) >= 0) {
            return CompletableFuture.completedFuture(null);
        }

        log.warn("Low balance alert for wallet {}: {}", walletId, currentBalance);

        NotificationEvent event = NotificationEvent.lowBalanceAlert(
                email, phone,
                walletId.toString(),
                currentBalance.toPlainString(),
                LOW_BALANCE_THRESHOLD.toPlainString());

        if (email != null && !email.isBlank()) {
            emailService.sendEmail(email, event.getSubject(), event.getBody());
        }

        if (phone != null && !phone.isBlank()) {
            smsService.sendSms(phone, event.getBody());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Sends a generic notification event via all configured channels.
     */
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendNotification(NotificationEvent event) {
        log.info("Sending notification: type={}, to={}", event.getType(), event.getRecipientEmail());

        if (event.getRecipientEmail() != null && !event.getRecipientEmail().isBlank()) {
            emailService.sendEmail(event.getRecipientEmail(), event.getSubject(), event.getBody());
        }

        if (event.getRecipientPhone() != null && !event.getRecipientPhone().isBlank()) {
            smsService.sendSms(event.getRecipientPhone(), event.getBody());
        }

        return CompletableFuture.completedFuture(null);
    }
}
