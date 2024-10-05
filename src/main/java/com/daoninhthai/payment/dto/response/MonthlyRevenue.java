package com.daoninhthai.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenue {

    private int month;
    private int year;
    private BigDecimal revenue;
    private long transactionCount;
    private BigDecimal averageTransactionAmount;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;

    /**
     * Calculates the average transaction amount from revenue and count.
     */
    public void calculateAverage() {
        if (transactionCount > 0 && revenue != null) {
            this.averageTransactionAmount = revenue.divide(
                    BigDecimal.valueOf(transactionCount), 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.averageTransactionAmount = BigDecimal.ZERO;
        }
    }
}
