package com.example.transaction_service.Service;

import com.example.transaction_service.DTO.TransactionCompletedEvent;
import com.example.transaction_service.DTO.TransactionCreatedEvent;
import com.example.transaction_service.Model.Transaction;
import com.example.transaction_service.Repository.TransactionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private TransactionRepo transactionRepo;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private TransactionEventConsumer transactionEventConsumer;

    private UUID transactionId;
    private UUID userId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private TransactionCreatedEvent createdEvent;
    private TransactionCompletedEvent completedEvent;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        senderWalletId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();

        createdEvent = new TransactionCreatedEvent();
        createdEvent.setTransactionId(transactionId);
        createdEvent.setUserId(userId);
        createdEvent.setSenderWalletId(senderWalletId);
        createdEvent.setReceiverWalletId(receiverWalletId);
        createdEvent.setAmount(100.0);
        createdEvent.setTransactionType("TRANSFER");
        createdEvent.setRemarks("Test transaction");
        createdEvent.setUserEmail("test@example.com");

        completedEvent = new TransactionCompletedEvent();
        completedEvent.setTransactionId(transactionId);
        completedEvent.setStatus("SUCCESS");
        completedEvent.setRemarks("Transaction completed");
    }

    @Test
    void testHandleTransactionCreated_Success() {
        when(transactionRepo.existsById(transactionId)).thenReturn(false);
        when(transactionRepo.save(any(Transaction.class))).thenReturn(new Transaction());
        doNothing().when(otpService).createAndSendOtp(any(), any(), anyString(), anyString());

        transactionEventConsumer.handleTransactionCreated(createdEvent);

        verify(transactionRepo).save(any(Transaction.class));
        verify(otpService).createAndSendOtp(eq(transactionId), eq(userId), eq("test@example.com"), eq("TRANSFER"));
    }

    @Test
    void testHandleTransactionCreated_AlreadyExists() {
        when(transactionRepo.existsById(transactionId)).thenReturn(true);

        transactionEventConsumer.handleTransactionCreated(createdEvent);

        verify(transactionRepo, never()).save(any(Transaction.class));
        verify(otpService, never()).createAndSendOtp(any(), any(), anyString(), anyString());
    }

    @Test
    void testHandleTransactionCreated_NullRemarks() {
        createdEvent.setRemarks(null);
        when(transactionRepo.existsById(transactionId)).thenReturn(false);
        when(transactionRepo.save(any(Transaction.class))).thenReturn(new Transaction());
        doNothing().when(otpService).createAndSendOtp(any(), any(), anyString(), anyString());

        transactionEventConsumer.handleTransactionCreated(createdEvent);

        verify(transactionRepo).save(any(Transaction.class));
        verify(otpService).createAndSendOtp(any(), any(), anyString(), anyString());
    }

    @Test
    void testHandleTransactionCreated_NullEmail() {
        createdEvent.setUserEmail(null);
        when(transactionRepo.existsById(transactionId)).thenReturn(false);
        when(transactionRepo.save(any(Transaction.class))).thenReturn(new Transaction());
        doNothing().when(otpService).createAndSendOtp(any(), any(), anyString(), anyString());

        transactionEventConsumer.handleTransactionCreated(createdEvent);

        verify(otpService).createAndSendOtp(eq(transactionId), eq(userId), eq("user@example.com"), eq("TRANSFER"));
    }

    @Test
    void testHandleTransactionCreated_EmptyEmail() {
        createdEvent.setUserEmail("   ");
        when(transactionRepo.existsById(transactionId)).thenReturn(false);
        when(transactionRepo.save(any(Transaction.class))).thenReturn(new Transaction());
        doNothing().when(otpService).createAndSendOtp(any(), any(), anyString(), anyString());

        transactionEventConsumer.handleTransactionCreated(createdEvent);

        verify(otpService).createAndSendOtp(eq(transactionId), eq(userId), eq("user@example.com"), eq("TRANSFER"));
    }

    @Test
    void testHandleTransactionCreated_DataIntegrityViolationException() {
        when(transactionRepo.existsById(transactionId)).thenReturn(false);
        when(transactionRepo.save(any(Transaction.class))).thenThrow(new DataIntegrityViolationException("Duplicate"));

        transactionEventConsumer.handleTransactionCreated(createdEvent);

        verify(transactionRepo).save(any(Transaction.class));
    }

    @Test
    void testHandleTransactionCreated_ObjectOptimisticLockingFailureException_TransactionExists() {
        when(transactionRepo.existsById(transactionId)).thenReturn(false, true);
        when(transactionRepo.save(any(Transaction.class))).thenThrow(new ObjectOptimisticLockingFailureException("Locked", new Object()));

        transactionEventConsumer.handleTransactionCreated(createdEvent);

        verify(transactionRepo, atLeastOnce()).existsById(transactionId);
    }

    @Test
    void testHandleTransactionCreated_ObjectOptimisticLockingFailureException_TransactionNotExists() {
        when(transactionRepo.existsById(transactionId)).thenReturn(false);
        when(transactionRepo.save(any(Transaction.class))).thenThrow(new ObjectOptimisticLockingFailureException("Locked", new Object()));
        when(transactionRepo.existsById(transactionId)).thenReturn(false);

        try {
            transactionEventConsumer.handleTransactionCreated(createdEvent);
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Optimistic locking failure"));
        }
    }

    @Test
    void testHandleTransactionCreated_GenericException() {
        when(transactionRepo.existsById(transactionId)).thenReturn(false);
        when(transactionRepo.save(any(Transaction.class))).thenThrow(new RuntimeException("Generic error"));

        assertThrows(RuntimeException.class, () -> {
            transactionEventConsumer.handleTransactionCreated(createdEvent);
        });
    }

    @Test
    void testHandleTransactionCompleted_Success() {
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setStatus("PENDING");

        when(transactionRepo.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepo.save(any(Transaction.class))).thenReturn(transaction);

        transactionEventConsumer.handleTransactionCompleted(completedEvent);

        verify(transactionRepo).save(any(Transaction.class));
        assertEquals("SUCCESS", transaction.getStatus());
        assertEquals("Transaction completed", transaction.getRemarks());
    }

    @Test
    void testHandleTransactionCompleted_TransactionNotFound() {
        when(transactionRepo.findById(transactionId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> {
            transactionEventConsumer.handleTransactionCompleted(completedEvent);
        });
        
        verify(transactionRepo).findById(transactionId);
    }

    @Test
    void testHandleTransactionCompleted_Exception() {
        when(transactionRepo.findById(transactionId)).thenThrow(new RuntimeException("Database error"));

        transactionEventConsumer.handleTransactionCompleted(completedEvent);

        verify(transactionRepo).findById(transactionId);
    }
}

