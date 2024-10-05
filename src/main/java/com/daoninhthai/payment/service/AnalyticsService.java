package com.daoninhthai.payment.service;

import com.daoninhthai.payment.dto.response.DailyTransactionSummary;
import com.daoninhthai.payment.dto.response.MonthlyRevenue;
import com.daoninhthai.payment.dto.response.TransactionTrend;
import com.daoninhthai.payment.entity.Transaction;
import com.daoninhthai.payment.entity.enums.TransactionType;
import com.daoninhthai.payment.repository.TransactionRepository;
import com.daoninhthai.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    /**
     * Generates a daily transaction summary for a specific date.
     * Aggregates all transactions by type and calculates totals and net flow.
     */
    public DailyTransactionSummary getDailyTransactionSummary(LocalDate date) {
        log.info("Generating daily transaction summary for: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByCreatedAtBetween(startOfDay, endOfDay);

        long deposits = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .count();
        long withdrawals = transactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .count();
        long transfers = transactions.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER)
                .count();
        long refunds = transactions.stream()
                .filter(t -> t.getType() == TransactionType.REFUND)
                .count();

        BigDecimal totalDepositAmount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawalAmount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTransferAmount = transactions.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER)
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DailyTransactionSummary summary = DailyTransactionSummary.builder()
                .date(date)
                .totalTransactions(transactions.size())
                .totalDeposits(deposits)
                .totalWithdrawals(withdrawals)
                .totalTransfers(transfers)
                .totalRefunds(refunds)
                .totalDepositAmount(totalDepositAmount)
                .totalWithdrawalAmount(totalWithdrawalAmount)
                .totalTransferAmount(totalTransferAmount)
                .build();

        summary.calculateNetFlow();
        return summary;
    }

    /**
     * Calculates monthly revenue for a given month and year.
     * Revenue is computed from all completed deposit transactions.
     */
    public MonthlyRevenue getMonthlyRevenue(int year, int month) {
        log.info("Calculating monthly revenue for: {}/{}", month, year);

        LocalDateTime startOfMonth = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);

        List<Transaction> transactions = transactionRepository
                .findByCreatedAtBetween(startOfMonth, endOfMonth);

        BigDecimal totalDeposits = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawals = transactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenue = totalDeposits.subtract(totalWithdrawals);

        MonthlyRevenue monthlyRevenue = MonthlyRevenue.builder()
                .month(month)
                .year(year)
                .revenue(revenue)
                .transactionCount(transactions.size())
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .build();

        monthlyRevenue.calculateAverage();
        return monthlyRevenue;
    }

    /**
     * Analyzes transaction trends over a date range, grouped by day.
     * Shows the daily volume and amount, with cumulative totals.
     */
    public List<TransactionTrend> getTransactionTrends(LocalDate startDate, LocalDate endDate) {
        log.info("Analyzing transaction trends from {} to {}", startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByCreatedAtBetween(start, end);

        // Group transactions by date
        Map<LocalDate, List<Transaction>> groupedByDate = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()));

        List<TransactionTrend> trends = new ArrayList<>();
        BigDecimal cumulativeAmount = BigDecimal.ZERO;
        BigDecimal previousDayAmount = null;

        // Sort by date and compute trends
        List<LocalDate> sortedDates = new ArrayList<>(groupedByDate.keySet());
        Collections.sort(sortedDates);

        for (LocalDate date : sortedDates) {
            List<Transaction> dayTransactions = groupedByDate.get(date);

            BigDecimal dayAmount = dayTransactions.stream()
                    .map(t -> t.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            cumulativeAmount = cumulativeAmount.add(dayAmount);

            double percentageChange = 0.0;
            if (previousDayAmount != null && previousDayAmount.compareTo(BigDecimal.ZERO) > 0) {
                percentageChange = dayAmount.subtract(previousDayAmount)
                        .divide(previousDayAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }

            TransactionTrend trend = TransactionTrend.builder()
                    .date(date)
                    .amount(dayAmount)
                    .count(dayTransactions.size())
                    .cumulativeAmount(cumulativeAmount)
                    .percentageChange(percentageChange)
                    .build();

            trends.add(trend);
            previousDayAmount = dayAmount;
        }

        return trends;
    }

    /**
     * Identifies the top users by transaction volume within a date range.
     * Returns wallet IDs ranked by total transaction amount.
     */
    public List<Map<String, Object>> getTopUsers(int limit, LocalDate startDate, LocalDate endDate) {
        log.info("Finding top {} users by transaction volume from {} to {}", limit, startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByCreatedAtBetween(start, end);

        // Group by wallet and aggregate
        Map<Long, List<Transaction>> byWallet = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getWallet().getId()));

        List<Map<String, Object>> topUsers = byWallet.entrySet().stream()
                .map(entry -> {
                    Long walletId = entry.getKey();
                    List<Transaction> walletTransactions = entry.getValue();

                    BigDecimal totalAmount = walletTransactions.stream()
                            .map(t -> t.getAmount().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long transactionCount = walletTransactions.size();

                    Map<String, Object> userSummary = new LinkedHashMap<>();
                    userSummary.put("walletId", walletId);
                    userSummary.put("totalAmount", totalAmount);
                    userSummary.put("transactionCount", transactionCount);
                    return userSummary;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("totalAmount"))
                        .compareTo((BigDecimal) a.get("totalAmount")))
                .limit(limit)
                .collect(Collectors.toList());

        return topUsers;
    }

    /**
     * Retrieves transactions filtered by type within a date range.
     * Useful for generating type-specific reports.
     */
    public List<TransactionTrend> getTransactionsByType(TransactionType type,
                                                         LocalDate startDate, LocalDate endDate) {
        log.info("Getting transactions by type {} from {} to {}", type, startDate, endDate);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByCreatedAtBetween(start, end)
                .stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());

        // Group by date
        Map<LocalDate, List<Transaction>> groupedByDate = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()));

        List<TransactionTrend> results = new ArrayList<>();
        List<LocalDate> sortedDates = new ArrayList<>(groupedByDate.keySet());
        Collections.sort(sortedDates);

        for (LocalDate date : sortedDates) {
            List<Transaction> dayTransactions = groupedByDate.get(date);

            BigDecimal dayAmount = dayTransactions.stream()
                    .map(t -> t.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            results.add(TransactionTrend.builder()
                    .date(date)
                    .amount(dayAmount)
                    .count(dayTransactions.size())
                    .type(type)
                    .build());
        }

        return results;
    }
}
