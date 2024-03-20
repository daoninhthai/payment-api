package com.daoninhthai.payment.service;

import com.daoninhthai.payment.entity.Transaction;
import com.daoninhthai.payment.entity.User;
import com.daoninhthai.payment.entity.Wallet;
import com.daoninhthai.payment.entity.enums.TransactionStatus;
import com.daoninhthai.payment.entity.enums.TransactionType;
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

        // Send webhook notification
        webhookService.sendWebhookEvent(wallet.getUser().getId(), "transaction.deposit",
                Map.of("walletId", walletId, "amount", amount, "balance", balanceAfter));

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

        // Send webhook notification
        webhookService.sendWebhookEvent(wallet.getUser().getId(), "transaction.withdrawal",
                Map.of("walletId", walletId, "amount", amount, "balance", balanceAfter));

        return transaction;
    }

    @Transactional
    public Transaction[] transfer(Long fromWalletId, Long toWalletId, BigDecimal amount, String description) {
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

        // Send webhook notifications
        webhookService.sendWebhookEvent(fromWallet.getUser().getId(), "transaction.transfer.sent",
                Map.of("fromWalletId", fromWalletId, "toWalletId", toWalletId,
                        "amount", amount, "balance", senderBalanceAfter));

        webhookService.sendWebhookEvent(toWallet.getUser().getId(), "transaction.transfer.received",
                Map.of("fromWalletId", fromWalletId, "toWalletId", toWalletId,
                        "amount", amount, "balance", receiverBalanceAfter));

        return new Transaction[]{debitTransaction, creditTransaction};
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
