package com.daoninhthai.payment.service;

import com.daoninhthai.payment.entity.Transaction;
import com.daoninhthai.payment.entity.User;
import com.daoninhthai.payment.entity.Wallet;
import com.daoninhthai.payment.entity.enums.TransactionStatus;
import com.daoninhthai.payment.entity.enums.TransactionType;
import com.daoninhthai.payment.exception.BadRequestException;
import com.daoninhthai.payment.exception.InsufficientBalanceException;
import com.daoninhthai.payment.exception.ResourceNotFoundException;
import com.daoninhthai.payment.repository.TransactionRepository;
import com.daoninhthai.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WebhookService webhookService;
    private final NotificationService notificationService;

    @Transactional
    public Wallet createWallet(User user) {
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .build();
        return walletRepository.save(wallet);
    }

    @Transactional
    public Transaction deposit(Long walletId, BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .referenceId(UUID.randomUUID().toString())
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();

        transaction = transactionRepository.save(transaction);

        webhookService.sendWebhookEvent(wallet.getUser().getId(), "transaction.deposit",
                Map.of("walletId", walletId, "amount", amount, "balance", balanceAfter));

        // Send deposit notification
        notificationService.sendPaymentConfirmation(
                wallet.getUser().getEmail(), null,
                transaction.getReferenceId(), amount, "DEPOSIT");

        return transaction;
    }

    @Transactional
    public Transaction withdraw(Long walletId, BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

        BigDecimal balanceBefore = wallet.getBalance();

        if (balanceBefore.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Current: " + balanceBefore + ", Requested: " + amount);
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .type(TransactionType.WITHDRAWAL)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .referenceId(UUID.randomUUID().toString())
                .description(description)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();

        transaction = transactionRepository.save(transaction);

        webhookService.sendWebhookEvent(wallet.getUser().getId(), "transaction.withdrawal",
                Map.of("walletId", walletId, "amount", amount, "balance", balanceAfter));

        // Send withdrawal notification and check for low balance
        notificationService.sendPaymentConfirmation(
                wallet.getUser().getEmail(), null,
                transaction.getReferenceId(), amount, "WITHDRAWAL");
        notificationService.sendLowBalanceAlert(
                wallet.getUser().getEmail(), null, walletId, balanceAfter);

        return transaction;
    }

    @Transactional
    public Transaction[] transfer(Long fromWalletId, Long toWalletId, BigDecimal amount, String description) {
        if (fromWalletId.equals(toWalletId)) {
            throw new BadRequestException("Cannot transfer to your own wallet");
        }

        Wallet fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", fromWalletId));
        Wallet toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", toWalletId));

        BigDecimal senderBalanceBefore = fromWallet.getBalance();

        if (senderBalanceBefore.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Current: " + senderBalanceBefore + ", Requested: " + amount);
        }

        String referenceId = UUID.randomUUID().toString();

        // Debit sender
        BigDecimal senderBalanceAfter = senderBalanceBefore.subtract(amount);
        fromWallet.setBalance(senderBalanceAfter);
        walletRepository.save(fromWallet);

        Transaction debitTransaction = Transaction.builder()
                .wallet(fromWallet)
                .type(TransactionType.TRANSFER)
                .amount(amount.negate())
                .status(TransactionStatus.COMPLETED)
                .referenceId(referenceId)
                .description("Transfer to wallet #" + toWalletId + ": " + description)
                .balanceBefore(senderBalanceBefore)
                .balanceAfter(senderBalanceAfter)
                .build();

        // Credit receiver
        BigDecimal receiverBalanceBefore = toWallet.getBalance();
        BigDecimal receiverBalanceAfter = receiverBalanceBefore.add(amount);
        toWallet.setBalance(receiverBalanceAfter);
        walletRepository.save(toWallet);

        Transaction creditTransaction = Transaction.builder()
                .wallet(toWallet)
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .referenceId(referenceId + "-credit")
                .description("Transfer from wallet #" + fromWalletId + ": " + description)
                .balanceBefore(receiverBalanceBefore)
                .balanceAfter(receiverBalanceAfter)
                .build();

        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);

        webhookService.sendWebhookEvent(fromWallet.getUser().getId(), "transaction.transfer.sent",
                Map.of("fromWalletId", fromWalletId, "toWalletId", toWalletId,
                        "amount", amount, "balance", senderBalanceAfter));

        webhookService.sendWebhookEvent(toWallet.getUser().getId(), "transaction.transfer.received",
                Map.of("fromWalletId", fromWalletId, "toWalletId", toWalletId,
                        "amount", amount, "balance", receiverBalanceAfter));

        // Send transfer notifications to both parties
        notificationService.sendTransferNotification(
                fromWallet.getUser().getEmail(), null,
                toWallet.getUser().getEmail(), null,
                fromWalletId, toWalletId, amount);
        // Check low balance for sender
        notificationService.sendLowBalanceAlert(
                fromWallet.getUser().getEmail(), null, fromWalletId, senderBalanceAfter);

        return new Transaction[]{debitTransaction, creditTransaction};
    }

    @Transactional
    public Transaction refund(Long transactionId, String reason) {
        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        if (originalTransaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BadRequestException("Only completed transactions can be refunded");
        }

        if (originalTransaction.getType() != TransactionType.DEPOSIT
                && originalTransaction.getType() != TransactionType.TRANSFER) {
            throw new BadRequestException("Only deposit and transfer transactions can be refunded");
        }

        Wallet wallet = originalTransaction.getWallet();
        BigDecimal refundAmount = originalTransaction.getAmount().abs();
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(refundAmount);

        // If it was a deposit, we subtract (reverse the deposit)
        if (originalTransaction.getType() == TransactionType.DEPOSIT) {
            if (balanceBefore.compareTo(refundAmount) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient balance for refund. Current: " + balanceBefore + ", Refund: " + refundAmount);
            }
            balanceAfter = balanceBefore.subtract(refundAmount);
        }

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        // Mark original transaction as REFUNDED
        originalTransaction.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(originalTransaction);

        // Create refund transaction
        Transaction refundTransaction = Transaction.builder()
                .wallet(wallet)
                .type(TransactionType.REFUND)
                .amount(refundAmount)
                .status(TransactionStatus.COMPLETED)
                .referenceId(UUID.randomUUID().toString())
                .description("Refund for transaction #" + transactionId + ": " + reason)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();

        refundTransaction = transactionRepository.save(refundTransaction);

        webhookService.sendWebhookEvent(wallet.getUser().getId(), "transaction.refund",
                Map.of("originalTransactionId", transactionId, "refundAmount", refundAmount,
                        "balance", balanceAfter, "reason", reason));

        return refundTransaction;
    }

    public BigDecimal getBalance(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));
        return wallet.getBalance();
    }

    public Wallet getWallet(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));
    }
}
