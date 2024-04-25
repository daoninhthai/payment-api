package com.daoninhthai.payment.service;

import com.daoninhthai.payment.entity.Transaction;
import com.daoninhthai.payment.entity.User;
import com.daoninhthai.payment.entity.Wallet;
import com.daoninhthai.payment.entity.enums.TransactionStatus;
import com.daoninhthai.payment.entity.enums.TransactionType;
import com.daoninhthai.payment.exception.BadRequestException;
import com.daoninhthai.payment.exception.InsufficientBalanceException;
import com.daoninhthai.payment.repository.TransactionRepository;
import com.daoninhthai.payment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private WalletService walletService;

    private User testUser;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_password")
                .fullName("Test User")
                .createdAt(LocalDateTime.now())
                .build();

        testWallet = Wallet.builder()
                .id(1L)
                .user(testUser)
                .balance(new BigDecimal("1000000"))
                .currency("VND")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deposit - should increase wallet balance and create transaction")
    void testDeposit_success() {
        BigDecimal depositAmount = new BigDecimal("500000");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(1L);
            return tx;
        });

        Transaction result = walletService.deposit(1L, depositAmount, "Test deposit");

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.getAmount()).isEqualByComparingTo(depositAmount);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(result.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("1500000"));

        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Withdraw - should decrease wallet balance and create transaction")
    void testWithdraw_success() {
        BigDecimal withdrawAmount = new BigDecimal("300000");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(2L);
            return tx;
        });

        Transaction result = walletService.withdraw(1L, withdrawAmount, "Test withdrawal");

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(result.getAmount()).isEqualByComparingTo(withdrawAmount);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(result.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("700000"));

        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Withdraw - should throw InsufficientBalanceException when balance is not enough")
    void testWithdraw_insufficientBalance_throwsException() {
        BigDecimal withdrawAmount = new BigDecimal("2000000");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() -> walletService.withdraw(1L, withdrawAmount, "Large withdrawal"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Transfer - should debit sender and credit receiver")
    void testTransfer_success() {
        Wallet receiverWallet = Wallet.builder()
                .id(2L)
                .user(User.builder().id(2L).email("receiver@example.com")
                        .fullName("Receiver").password("pass").build())
                .balance(new BigDecimal("500000"))
                .currency("VND")
                .createdAt(LocalDateTime.now())
                .build();

        BigDecimal transferAmount = new BigDecimal("200000");

        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(receiverWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(tx.getWallet().getId());
            return tx;
        });

        Transaction[] result = walletService.transfer(1L, 2L, transferAmount, "Test transfer");

        assertThat(result).hasSize(2);
        assertThat(result[0].getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result[0].getBalanceBefore()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(result[0].getBalanceAfter()).isEqualByComparingTo(new BigDecimal("800000"));
        assertThat(result[1].getBalanceBefore()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(result[1].getBalanceAfter()).isEqualByComparingTo(new BigDecimal("700000"));

        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Transfer - should throw BadRequestException when transferring to own wallet")
    void testTransfer_toSameWallet_throwsException() {
        assertThatThrownBy(() -> walletService.transfer(1L, 1L, new BigDecimal("100000"), "Self transfer"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transfer to your own wallet");

        verify(walletRepository, never()).findById(anyLong());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
