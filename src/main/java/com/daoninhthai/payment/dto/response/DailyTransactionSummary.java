package com.daoninhthai.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTransactionSummary {

    private LocalDate date;
    private long totalTransactions;
    private long totalDeposits;
    private long totalWithdrawals;
    private long totalTransfers;
    private long totalRefunds;
    private BigDecimal totalDepositAmount;
    private BigDecimal totalWithdrawalAmount;
    private BigDecimal totalTransferAmount;
    private BigDecimal netFlow;

    /**
     * Calculates net flow as deposits minus withdrawals.
     * Positive net flow means more money came in than went out.
     */
    public void calculateNetFlow() {
        BigDecimal deposits = totalDepositAmount != null ? totalDepositAmount : BigDecimal.ZERO;
        BigDecimal withdrawals = totalWithdrawalAmount != null ? totalWithdrawalAmount : BigDecimal.ZERO;
        this.netFlow = deposits.subtract(withdrawals);
    }
}
