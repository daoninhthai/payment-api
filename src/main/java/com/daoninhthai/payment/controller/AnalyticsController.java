package com.daoninhthai.payment.controller;

import com.daoninhthai.payment.dto.response.DailyTransactionSummary;
import com.daoninhthai.payment.dto.response.MonthlyRevenue;
import com.daoninhthai.payment.dto.response.TransactionTrend;
import com.daoninhthai.payment.entity.enums.TransactionType;
import com.daoninhthai.payment.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Returns a summary of all transactions for a given date.
     * Includes counts and amounts broken down by transaction type.
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<DailyTransactionSummary> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyTransactionSummary summary = analyticsService.getDailyTransactionSummary(date);
        return ResponseEntity.ok(summary);
    }

    /**
     * Returns monthly revenue calculated from deposit and withdrawal transactions.
     * Revenue = Total Deposits - Total Withdrawals.
     */
    @GetMapping("/monthly-revenue")
    public ResponseEntity<MonthlyRevenue> getMonthlyRevenue(
            @RequestParam int year,
            @RequestParam int month) {
        MonthlyRevenue revenue = analyticsService.getMonthlyRevenue(year, month);
        return ResponseEntity.ok(revenue);
    }

    /**
     * Returns transaction trends over a date range.
     * Shows daily volume, cumulative totals, and percentage changes.
     */
    @GetMapping("/trends")
    public ResponseEntity<List<TransactionTrend>> getTransactionTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionTrend> trends = analyticsService.getTransactionTrends(startDate, endDate);
        return ResponseEntity.ok(trends);
    }

    /**
     * Returns the top N users ranked by total transaction volume within a date range.
     */
    @GetMapping("/top-users")
    public ResponseEntity<List<Map<String, Object>>> getTopUsers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Map<String, Object>> topUsers = analyticsService.getTopUsers(limit, startDate, endDate);
        return ResponseEntity.ok(topUsers);
    }

    /**
     * Returns transactions filtered by type within a date range.
     * Groups results by day with aggregated amounts and counts.
     */
    @GetMapping("/by-type")
    public ResponseEntity<List<TransactionTrend>> getTransactionsByType(
            @RequestParam TransactionType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TransactionTrend> results = analyticsService.getTransactionsByType(type, startDate, endDate);
        return ResponseEntity.ok(results);
    }
}
