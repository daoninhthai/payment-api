package com.daoninhthai.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid:AC_default}")
    private String accountSid;

    @Value("${twilio.auth-token:default_token}")
    private String authToken;

    @Value("${twilio.from-number:+10000000000}")
    private String fromNumber;

    /**
     * Sends an SMS message asynchronously using the Twilio SDK pattern.
     * In production, this would use the Twilio Java SDK to send messages.
     *
     * @param toPhone recipient phone number in E.164 format
     * @param message the SMS message body (max 1600 characters)
     * @return CompletableFuture with the message SID or null on failure
     */
    @Async("smsExecutor")
    public CompletableFuture<String> sendSms(String toPhone, String message) {
        try {
            log.info("Sending SMS to: {}", toPhone);

            // In production, this would use Twilio SDK:
            // Twilio.init(accountSid, authToken);
            // Message twilioMessage = Message.creator(
            //     new PhoneNumber(toPhone),
            //     new PhoneNumber(fromNumber),
            //     message
            // ).create();
            // return CompletableFuture.completedFuture(twilioMessage.getSid());

            // Validate phone number format
            if (toPhone == null || !toPhone.startsWith("+")) {
                log.warn("Invalid phone number format: {}. Expected E.164 format.", toPhone);
                return CompletableFuture.completedFuture(null);
            }

            // Validate message length
            if (message != null && message.length() > 1600) {
                log.warn("SMS message exceeds 1600 character limit. Truncating.");
                message = message.substring(0, 1597) + "...";
            }

            // Simulate SMS sending
            String messageSid = "SM" + System.currentTimeMillis();
            log.info("SMS sent successfully to: {}, SID: {}", toPhone, messageSid);
            return CompletableFuture.completedFuture(messageSid);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhone, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Sends a payment notification SMS with formatted content.
     *
     * @param toPhone       recipient phone number
     * @param transactionId the transaction identifier
     * @param amount        the transaction amount
     * @param type          the notification type (deposit, withdrawal, transfer)
     * @return CompletableFuture with the message SID
     */
    @Async("smsExecutor")
    public CompletableFuture<String> sendPaymentNotificationSms(String toPhone, String transactionId,
                                                                  String amount, String type) {
        String message = String.format("[Payment API] %s of %s processed. Ref: %s", type, amount, transactionId);
        return sendSms(toPhone, message);
    }
}
