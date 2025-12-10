package com.example.transaction_service.Service;

import com.example.transaction_service.Model.Transaction;
import com.example.transaction_service.Repository.TransactionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpCleanupSchedulerTest {

    @Mock
    private TransactionRepo transactionRepo;

    @InjectMocks
    private OtpCleanupScheduler otpCleanupScheduler;

    private Transaction pendingTransaction;
    private Transaction expiredTransaction;

    @BeforeEach
    void setUp() {
        UUID transactionId1 = UUID.randomUUID();
        UUID transactionId2 = UUID.randomUUID();

        pendingTransaction = new Transaction();
        pendingTransaction.setId(transactionId1);
        pendingTransaction.setStatus("PENDING");
        pendingTransaction.setTransactionDate(LocalDateTime.now().minusMinutes(6));

        expiredTransaction = new Transaction();
        expiredTransaction.setId(transactionId2);
        expiredTransaction.setStatus("PENDING");
        expiredTransaction.setTransactionDate(LocalDateTime.now().minusMinutes(10));
    }

    @Test
    void testCleanupExpiredPendingTransactions_WithExpiredTransactions() {
        Transaction successTransaction = new Transaction();
        successTransaction.setId(UUID.randomUUID());
        successTransaction.setStatus("SUCCESS");
        successTransaction.setTransactionDate(LocalDateTime.now().minusMinutes(6));

        List<Transaction> allTransactions = Arrays.asList(
            pendingTransaction,
            expiredTransaction,
            successTransaction
        );

        when(transactionRepo.findAll()).thenReturn(allTransactions);
        when(transactionRepo.save(any(Transaction.class))).thenReturn(expiredTransaction);

        otpCleanupScheduler.cleanupExpiredPendingTransactions();

        verify(transactionRepo, times(2)).save(any(Transaction.class));
    }

    @Test
    void testCleanupExpiredPendingTransactions_NoExpiredTransactions() {
        Transaction recentTransaction = new Transaction();
        recentTransaction.setId(UUID.randomUUID());
        recentTransaction.setStatus("PENDING");
        recentTransaction.setTransactionDate(LocalDateTime.now().minusMinutes(2));

        List<Transaction> allTransactions = Arrays.asList(recentTransaction);

        when(transactionRepo.findAll()).thenReturn(allTransactions);

        otpCleanupScheduler.cleanupExpiredPendingTransactions();

        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    @Test
    void testCleanupExpiredPendingTransactions_EmptyList() {
        when(transactionRepo.findAll()).thenReturn(Arrays.asList());

        otpCleanupScheduler.cleanupExpiredPendingTransactions();

        verify(transactionRepo, never()).save(any(Transaction.class));
    }

    @Test
    void testCleanupExpiredPendingTransactions_ExceptionHandling() {
        when(transactionRepo.findAll()).thenThrow(new RuntimeException("Database error"));

        assertDoesNotThrow(() -> {
            otpCleanupScheduler.cleanupExpiredPendingTransactions();
        });
    }

    @Test
    void testCleanupExpiredPendingTransactions_NullTransactionDate() {
        Transaction transactionWithNullDate = new Transaction();
        transactionWithNullDate.setId(UUID.randomUUID());
        transactionWithNullDate.setStatus("PENDING");
        transactionWithNullDate.setTransactionDate(null);

        List<Transaction> allTransactions = Arrays.asList(transactionWithNullDate);

        when(transactionRepo.findAll()).thenReturn(allTransactions);

        otpCleanupScheduler.cleanupExpiredPendingTransactions();

        verify(transactionRepo, never()).save(any(Transaction.class));
    }
}

