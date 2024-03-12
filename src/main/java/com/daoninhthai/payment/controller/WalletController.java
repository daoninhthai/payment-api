package com.daoninhthai.payment.controller;

import com.daoninhthai.payment.dto.request.DepositRequest;
import com.daoninhthai.payment.dto.request.TransferRequest;
import com.daoninhthai.payment.dto.request.WithdrawRequest;
import com.daoninhthai.payment.dto.response.TransactionResponse;
import com.daoninhthai.payment.dto.response.WalletResponse;
import com.daoninhthai.payment.entity.Transaction;
import com.daoninhthai.payment.entity.Wallet;
import com.daoninhthai.payment.service.TransactionService;
import com.daoninhthai.payment.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long id) {
        Wallet wallet = walletService.getWallet(id);
        return ResponseEntity.ok(WalletResponse.fromEntity(wallet));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable Long id,
            @Valid @RequestBody DepositRequest request) {
        Transaction transaction = walletService.deposit(id, request.getAmount(), request.getDescription());
        return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawRequest request) {
        Transaction transaction = walletService.withdraw(id, request.getAmount(), request.getDescription());
        return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable Long id,
            @Valid @RequestBody TransferRequest request) {
        Transaction[] transactions = walletService.transfer(
                id, request.getToWalletId(), request.getAmount(), request.getDescription());
        return ResponseEntity.ok(TransactionResponse.fromEntity(transactions[0]));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionService.getTransactionHistory(id, pageable);
        Page<TransactionResponse> response = transactions.map(TransactionResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
}
