package com.daoninhthai.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmailService {

    /**
     * Sends an email asynchronously using the notification thread pool.
     * In production, this would use JavaMailSender with SMTP configuration.
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param body    email body content
     * @return CompletableFuture indicating success or failure
     */
    @Async("notificationExecutor")
    public CompletableFuture<Boolean> sendEmail(String to, String subject, String body) {
        try {
            log.info("Sending email to: {}, subject: {}", to, subject);

            // In production, this would use JavaMailSender:
            // MimeMessage message = mailSender.createMimeMessage();
            // MimeMessageHelper helper = new MimeMessageHelper(message, true);
            // helper.setTo(to);
            // helper.setSubject(subject);
            // helper.setText(body, true);
            // helper.setFrom("noreply@payment-api.com");
            // mailSender.send(message);

            // Simulate email sending delay
            Thread.sleep(100);

            log.info("Email sent successfully to: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Sends a templated email with dynamic content replacement.
     *
     * @param to           recipient email address
     * @param subject      email subject
     * @param templateName name of the email template
     * @param variables    template variables to replace
     * @return CompletableFuture indicating success or failure
     */
    @Async("notificationExecutor")
    public CompletableFuture<Boolean> sendTemplatedEmail(String to, String subject,
                                                          String templateName, Map<String, Object> variables) {
        try {
            log.info("Sending templated email to: {}, template: {}", to, templateName);

            // In production, this would:
            // 1. Load the template from resources/templates/{templateName}.html
            // 2. Use Thymeleaf or FreeMarker to process the template
            // 3. Replace variables in the template
            // 4. Send via JavaMailSender

            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Template: ").append(templateName).append("\n");
            variables.forEach((key, value) ->
                    bodyBuilder.append(key).append(": ").append(value).append("\n"));

            log.info("Templated email sent successfully to: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send templated email to {}: {}", to, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
