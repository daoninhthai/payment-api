package com.daoninhthai.payment.controller;

import com.daoninhthai.payment.dto.response.TransactionResponse;
import com.daoninhthai.payment.entity.Transaction;
import com.daoninhthai.payment.entity.User;
import com.daoninhthai.payment.entity.Wallet;
import com.daoninhthai.payment.entity.enums.TransactionStatus;
import com.daoninhthai.payment.entity.enums.TransactionType;
import com.daoninhthai.payment.exception.InsufficientBalanceException;
import com.daoninhthai.payment.exception.ResourceNotFoundException;
import com.daoninhthai.payment.security.JwtAuthenticationFilter;
import com.daoninhthai.payment.security.JwtTokenProvider;
import com.daoninhthai.payment.service.IdempotencyService;
import com.daoninhthai.payment.service.TransactionService;
import com.daoninhthai.payment.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WalletController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    @DisplayName("Deposit - should return 200 with transaction details")
    void testDeposit_returns200() throws Exception {
        Transaction mockTransaction = Transaction.builder()
                .id(1L)
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500000"))
                .status(TransactionStatus.COMPLETED)
                .description("Test deposit")
                .balanceBefore(new BigDecimal("1000000"))
                .balanceAfter(new BigDecimal("1500000"))
                .createdAt(LocalDateTime.now())
                .build();

        when(walletService.deposit(eq(1L), any(BigDecimal.class), anyString()))
                .thenReturn(mockTransaction);

        Map<String, Object> request = Map.of(
                "amount", 500000,
                "description", "Test deposit"
        );

        mockMvc.perform(post("/api/wallets/1/deposit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(500000))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.balanceBefore").value(1000000))
                .andExpect(jsonPath("$.balanceAfter").value(1500000));
    }

    @Test
    @WithMockUser
    @DisplayName("Withdraw - should return 400 when insufficient balance")
    void testWithdraw_insufficientBalance_returns400() throws Exception {
        when(walletService.withdraw(eq(1L), any(BigDecimal.class), anyString()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance. Current: 100000, Requested: 500000"));

        Map<String, Object> request = Map.of(
                "amount", 500000,
                "description", "Large withdrawal"
        );

        mockMvc.perform(post("/api/wallets/1/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Balance"))
                .andExpect(jsonPath("$.message").value("Insufficient balance. Current: 100000, Requested: 500000"));
    }

    @Test
    @WithMockUser
    @DisplayName("Get Wallet - should return 404 when wallet not found")
    void testGetWallet_notFound_returns404() throws Exception {
        when(walletService.getWallet(999L))
                .thenThrow(new ResourceNotFoundException("Wallet", "id", 999L));

        mockMvc.perform(get("/api/wallets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Wallet not found with id: '999'"));
    }
}
