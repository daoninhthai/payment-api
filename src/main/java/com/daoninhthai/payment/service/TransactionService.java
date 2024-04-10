package com.daoninhthai.payment.service;

import com.daoninhthai.payment.entity.Transaction;
import com.daoninhthai.payment.entity.enums.TransactionType;
import com.daoninhthai.payment.exception.ResourceNotFoundException;
import com.daoninhthai.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistory(Long walletId, TransactionType type, Pageable pageable) {
        if (type != null) {
            return transactionRepository.findByWalletIdAndTypeOrderByCreatedAtDesc(walletId, type, pageable);
        }
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionByReference(String referenceId) {
        return transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "referenceId", referenceId));
    }
}
