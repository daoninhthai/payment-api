package com.daoninhthai.payment.dto.response;

import com.daoninhthai.payment.entity.enums.TransactionType;
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
public class TransactionTrend {

    private LocalDate date;
    private BigDecimal amount;
    private long count;
    private TransactionType type;
    private BigDecimal cumulativeAmount;
    private double percentageChange;
}
