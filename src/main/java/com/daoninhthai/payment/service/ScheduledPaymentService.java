package com.daoninhthai.payment.service;

import com.daoninhthai.payment.dto.request.CreateScheduledPaymentRequest;
import com.daoninhthai.payment.entity.ScheduledPayment;
import com.daoninhthai.payment.entity.Wallet;
import com.daoninhthai.payment.entity.enums.ScheduledPaymentStatus;
import com.daoninhthai.payment.entity.enums.TransactionType;
import com.daoninhthai.payment.exception.BadRequestException;
import com.daoninhthai.payment.exception.ResourceNotFoundException;
import com.daoninhthai.payment.repository.ScheduledPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final WalletService walletService;

    @Transactional
    public ScheduledPayment create(CreateScheduledPaymentRequest request) {
        log.info("Creating scheduled payment for wallet: {}", request.getWalletId());

        Wallet wallet = walletService.getWallet(request.getWalletId());

        // Validate that transfer type has a recipient
        if (request.getType() == TransactionType.TRANSFER && request.getRecipientWalletId() == null) {
            throw new BadRequestException("Recipient wallet ID is required for transfer type");
        }

        // Validate cron expression format (basic validation)
        validateCronExpression(request.getCronExpression());

        ScheduledPayment scheduledPayment = ScheduledPayment.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .type(request.getType())
                .recipientWalletId(request.getRecipientWalletId())
                .cronExpression(request.getCronExpression())
                .description(request.getDescription())
                .nextExecutionDate(calculateNextExecution(request.getCronExpression()))
                .status(ScheduledPaymentStatus.ACTIVE)
                .executionCount(0)
                .maxExecutions(request.getMaxExecutions())
                .build();

        scheduledPayment = scheduledPaymentRepository.save(scheduledPayment);
        log.info("Scheduled payment created with ID: {}", scheduledPayment.getId());

        return scheduledPayment;
    }

    @Transactional
    public ScheduledPayment pause(Long id) {
        ScheduledPayment payment = getScheduledPayment(id);
        if (payment.getStatus() != ScheduledPaymentStatus.ACTIVE) {
            throw new BadRequestException("Only active scheduled payments can be paused");
        }
        payment.setStatus(ScheduledPaymentStatus.PAUSED);
        log.info("Scheduled payment {} paused", id);
        return scheduledPaymentRepository.save(payment);
    }

    @Transactional
    public ScheduledPayment resume(Long id) {
        ScheduledPayment payment = getScheduledPayment(id);
        if (payment.getStatus() != ScheduledPaymentStatus.PAUSED) {
            throw new BadRequestException("Only paused scheduled payments can be resumed");
        }
        payment.setStatus(ScheduledPaymentStatus.ACTIVE);
        payment.setNextExecutionDate(calculateNextExecution(payment.getCronExpression()));
        log.info("Scheduled payment {} resumed", id);
        return scheduledPaymentRepository.save(payment);
    }

    @Transactional
    public ScheduledPayment cancel(Long id) {
        ScheduledPayment payment = getScheduledPayment(id);
        if (payment.getStatus() == ScheduledPaymentStatus.CANCELLED) {
            throw new BadRequestException("Scheduled payment is already cancelled");
        }
        payment.setStatus(ScheduledPaymentStatus.CANCELLED);
        log.info("Scheduled payment {} cancelled", id);
        return scheduledPaymentRepository.save(payment);
    }

    public ScheduledPayment getScheduledPayment(Long id) {
        return scheduledPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduledPayment", "id", id));
    }

    public List<ScheduledPayment> getByWalletId(Long walletId) {
        return scheduledPaymentRepository.findByWalletId(walletId);
    }

    public List<ScheduledPayment> getActivePayments() {
        return scheduledPaymentRepository.findByStatus(ScheduledPaymentStatus.ACTIVE);
    }

    /**
     * Scheduled task that runs every minute to check and execute due scheduled payments.
     * Uses Spring @Scheduled with a fixed rate of 60 seconds.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void executeScheduledPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledPayment> duePayments = scheduledPaymentRepository
                .findByStatusAndNextExecutionDateBefore(ScheduledPaymentStatus.ACTIVE, now);

        if (duePayments.isEmpty()) {
            return;
        }

        log.info("Found {} scheduled payments due for execution", duePayments.size());

        for (ScheduledPayment payment : duePayments) {
            try {
                executePayment(payment);
            } catch (Exception e) {
                log.error("Failed to execute scheduled payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }

    private void executePayment(ScheduledPayment payment) {
        log.info("Executing scheduled payment: {} type: {} amount: {}",
                payment.getId(), payment.getType(), payment.getAmount());

        switch (payment.getType()) {
            case DEPOSIT:
                walletService.deposit(payment.getWallet().getId(), payment.getAmount(),
                        "Scheduled deposit: " + payment.getDescription());
                break;
            case TRANSFER:
                if (payment.getRecipientWalletId() == null) {
                    log.error("No recipient wallet for scheduled transfer: {}", payment.getId());
                    return;
                }
                walletService.transfer(payment.getWallet().getId(), payment.getRecipientWalletId(),
                        payment.getAmount(), "Scheduled transfer: " + payment.getDescription());
                break;
            case WITHDRAWAL:
                walletService.withdraw(payment.getWallet().getId(), payment.getAmount(),
                        "Scheduled withdrawal: " + payment.getDescription());
                break;
            default:
                log.warn("Unsupported scheduled payment type: {}", payment.getType());
                return;
        }

        // Update execution metadata
        payment.setExecutionCount(payment.getExecutionCount() + 1);
        payment.setLastExecutionDate(LocalDateTime.now());

        // Check if max executions reached
        if (payment.getMaxExecutions() != null && payment.getExecutionCount() >= payment.getMaxExecutions()) {
            payment.setStatus(ScheduledPaymentStatus.COMPLETED);
            log.info("Scheduled payment {} completed after {} executions", payment.getId(), payment.getExecutionCount());
        } else {
            payment.setNextExecutionDate(calculateNextExecution(payment.getCronExpression()));
        }

        scheduledPaymentRepository.save(payment);
    }

    /**
     * Calculate the next execution date based on a simplified cron-like expression.
     * Supports: DAILY, WEEKLY, MONTHLY, or interval in hours (e.g., "EVERY_6H").
     */
    private LocalDateTime calculateNextExecution(String cronExpression) {
        LocalDateTime now = LocalDateTime.now();
        return switch (cronExpression.toUpperCase()) {
            case "DAILY" -> now.plusDays(1).withHour(9).withMinute(0).withSecond(0);
            case "WEEKLY" -> now.plusWeeks(1).withHour(9).withMinute(0).withSecond(0);
            case "MONTHLY" -> now.plusMonths(1).withDayOfMonth(1).withHour(9).withMinute(0).withSecond(0);
            case "EVERY_6H" -> now.plusHours(6);
            case "EVERY_12H" -> now.plusHours(12);
            default -> now.plusDays(1); // Default to daily
        };
    }

    private void validateCronExpression(String cronExpression) {
        List<String> validExpressions = List.of("DAILY", "WEEKLY", "MONTHLY", "EVERY_6H", "EVERY_12H");
        if (!validExpressions.contains(cronExpression.toUpperCase())) {
            throw new BadRequestException(
                    "Invalid cron expression. Supported values: " + String.join(", ", validExpressions));
        }
    }
}
