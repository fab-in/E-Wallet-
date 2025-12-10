package com.example.wallet_service.Service;

import com.example.wallet_service.DTO.OtpVerifiedEvent;
import com.example.wallet_service.Model.Wallet;
import com.example.wallet_service.Repository.WalletRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private WalletRepo walletRepo;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @InjectMocks
    private TransactionEventConsumer transactionEventConsumer;

    private UUID transactionId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        senderWalletId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();

        senderWallet = new Wallet();
        senderWallet.setId(senderWalletId);
        senderWallet.setBalance(1000.0);

        receiverWallet = new Wallet();
        receiverWallet.setId(receiverWalletId);
        receiverWallet.setBalance(500.0);
    }

    @Test
    void testHandleOtpVerified_Credit() {
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(100.0);
        event.setTransactionType("CREDIT");

        when(walletRepo.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepo.findById(receiverWalletId)).thenReturn(Optional.of(receiverWallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(senderWallet);
        doNothing().when(transactionEventPublisher).publishTransactionCompleted(any(), any(), any());

        assertDoesNotThrow(() -> transactionEventConsumer.handleOtpVerified(event));

        assertEquals(1100.0, senderWallet.getBalance());
        verify(walletRepo).save(senderWallet);
        verify(transactionEventPublisher).publishTransactionCompleted(eq(transactionId), eq("SUCCESS"), any());
    }

    @Test
    void testHandleOtpVerified_Withdraw_Success() {
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(100.0);
        event.setTransactionType("WITHDRAW");

        when(walletRepo.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepo.findById(receiverWalletId)).thenReturn(Optional.of(receiverWallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(senderWallet);
        doNothing().when(transactionEventPublisher).publishTransactionCompleted(any(), any(), any());

        assertDoesNotThrow(() -> transactionEventConsumer.handleOtpVerified(event));

        assertEquals(900.0, senderWallet.getBalance());
        verify(walletRepo).save(senderWallet);
        verify(transactionEventPublisher).publishTransactionCompleted(eq(transactionId), eq("SUCCESS"), any());
    }

    @Test
    void testHandleOtpVerified_Withdraw_InsufficientBalance() {
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(2000.0);
        event.setTransactionType("WITHDRAW");

        when(walletRepo.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepo.findById(receiverWalletId)).thenReturn(Optional.of(receiverWallet));
        doNothing().when(transactionEventPublisher).publishTransactionCompleted(any(), any(), any());

        assertDoesNotThrow(() -> transactionEventConsumer.handleOtpVerified(event));

        verify(transactionEventPublisher, times(2)).publishTransactionCompleted(eq(transactionId), eq("FAILED"), any());
    }

    @Test
    void testHandleOtpVerified_Transfer_Success() {
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(100.0);
        event.setTransactionType("TRANSFER");

        when(walletRepo.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepo.findById(receiverWalletId)).thenReturn(Optional.of(receiverWallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(senderWallet);
        doNothing().when(transactionEventPublisher).publishTransactionCompleted(any(), any(), any());

        assertDoesNotThrow(() -> transactionEventConsumer.handleOtpVerified(event));

        assertEquals(900.0, senderWallet.getBalance());
        assertEquals(600.0, receiverWallet.getBalance());
        verify(walletRepo).save(senderWallet);
        verify(walletRepo).save(receiverWallet);
        verify(transactionEventPublisher).publishTransactionCompleted(eq(transactionId), eq("SUCCESS"), any());
    }

    @Test
    void testHandleOtpVerified_Transfer_InsufficientBalance() {
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(2000.0);
        event.setTransactionType("TRANSFER");

        when(walletRepo.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepo.findById(receiverWalletId)).thenReturn(Optional.of(receiverWallet));
        doNothing().when(transactionEventPublisher).publishTransactionCompleted(any(), any(), any());

        assertDoesNotThrow(() -> transactionEventConsumer.handleOtpVerified(event));

        verify(transactionEventPublisher, times(2)).publishTransactionCompleted(eq(transactionId), eq("FAILED"), any());
    }

    @Test
    void testHandleOtpVerified_SenderWalletNotFound() {
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(100.0);
        event.setTransactionType("CREDIT");

        when(walletRepo.findById(senderWalletId)).thenReturn(Optional.empty());
        doNothing().when(transactionEventPublisher).publishTransactionCompleted(any(), any(), any());

        assertDoesNotThrow(() -> transactionEventConsumer.handleOtpVerified(event));

        verify(transactionEventPublisher).publishTransactionCompleted(eq(transactionId), eq("FAILED"), any());
    }

    @Test
    void testHandleOtpVerified_ReceiverWalletNotFound() {
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(100.0);
        event.setTransactionType("CREDIT");

        when(walletRepo.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepo.findById(receiverWalletId)).thenReturn(Optional.empty());
        doNothing().when(transactionEventPublisher).publishTransactionCompleted(any(), any(), any());

        assertDoesNotThrow(() -> transactionEventConsumer.handleOtpVerified(event));

        verify(transactionEventPublisher).publishTransactionCompleted(eq(transactionId), eq("FAILED"), any());
    }

    @Test
    void testHandleOtpVerified_ExceptionHandling() {
        OtpVerifiedEvent event = new OtpVerifiedEvent();
        event.setTransactionId(transactionId);
        event.setSenderWalletId(senderWalletId);
        event.setReceiverWalletId(receiverWalletId);
        event.setAmount(100.0);
        event.setTransactionType("CREDIT");

        when(walletRepo.findById(senderWalletId)).thenThrow(new RuntimeException("Database error"));
        doNothing().when(transactionEventPublisher).publishTransactionCompleted(any(), any(), any());

        assertDoesNotThrow(() -> transactionEventConsumer.handleOtpVerified(event));

        verify(transactionEventPublisher).publishTransactionCompleted(eq(transactionId), eq("FAILED"), any());
    }
}

