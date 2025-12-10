package com.example.transaction_service.Service;

import com.example.transaction_service.DTO.OtpData;
import com.example.transaction_service.Exceptions.ResourceNotFoundException;
import com.example.transaction_service.Exceptions.ValidationException;
import com.example.transaction_service.Model.Transaction;
import com.example.transaction_service.Repository.TransactionRepo;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private Cache<UUID, OtpData> otpCache;

    @Mock
    private TransactionRepo transactionRepo;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @InjectMocks
    private OtpService otpService;

    private UUID transactionId;
    private UUID userId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private Transaction transaction;
    private OtpData otpData;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        senderWalletId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();

        transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setSenderWalletId(senderWalletId);
        transaction.setReceiverWalletId(receiverWalletId);
        transaction.setAmount(100.0);
        transaction.setStatus("PENDING");

        otpData = new OtpData();
        otpData.setTransactionId(transactionId);
        otpData.setHashedOtpCode("hashedOtp");
        otpData.setUserId(userId);
        otpData.setUserEmail("test@example.com");
        otpData.setTransactionType("TRANSFER");
        otpData.setAttemptCount(0);
        otpData.setIsVerified(false);
    }

    @Test
    void testGenerateOtp() {
        String otp = otpService.generateOtp();

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    void testCreateAndSendOtp_Success() throws Exception {
        when(passwordEncoder.encode(anyString())).thenReturn("hashedOtp");
        doNothing().when(emailService).sendSimpleEmail(anyString(), anyString(), anyString());

        otpService.createAndSendOtp(transactionId, userId, "test@example.com", "TRANSFER");

        verify(otpCache).put(eq(transactionId), any(OtpData.class));
        verify(emailService).sendSimpleEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void testVerifyOtp_Success() {
        when(otpCache.getIfPresent(transactionId)).thenReturn(otpData);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(transactionRepo.findById(transactionId)).thenReturn(Optional.of(transaction));
        doNothing().when(transactionEventPublisher).publishOtpVerified(any(), any(), any(), any(), any(), any());

        boolean result = otpService.verifyOtp(transactionId, "123456");

        assertTrue(result);
        assertTrue(otpData.getIsVerified());
        verify(transactionEventPublisher).publishOtpVerified(
            eq(transactionId), eq(userId), eq(senderWalletId), eq(receiverWalletId), 
            eq(100.0), eq("TRANSFER"));
    }

    @Test
    void testVerifyOtp_OtpNotFound() {
        when(otpCache.getIfPresent(transactionId)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            otpService.verifyOtp(transactionId, "123456");
        });
    }

    @Test
    void testVerifyOtp_AlreadyVerified() {
        otpData.setIsVerified(true);
        when(otpCache.getIfPresent(transactionId)).thenReturn(otpData);

        assertThrows(ValidationException.class, () -> {
            otpService.verifyOtp(transactionId, "123456");
        });
    }

    @Test
    void testVerifyOtp_MaxAttemptsReached() {
        otpData.setAttemptCount(3);
        when(otpCache.getIfPresent(transactionId)).thenReturn(otpData);
        when(transactionRepo.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepo.save(any(Transaction.class))).thenReturn(transaction);

        assertThrows(ValidationException.class, () -> {
            otpService.verifyOtp(transactionId, "123456");
        });

        verify(transactionRepo).save(any(Transaction.class));
        verify(otpCache).invalidate(transactionId);
    }

    @Test
    void testVerifyOtp_IncorrectOtp_FirstAttempt() {
        when(otpCache.getIfPresent(transactionId)).thenReturn(otpData);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(ValidationException.class, () -> {
            otpService.verifyOtp(transactionId, "wrong");
        });

        assertEquals(1, otpData.getAttemptCount());
        verify(otpCache, times(1)).put(eq(transactionId), any(OtpData.class));
    }

    @Test
    void testVerifyOtp_IncorrectOtp_SecondAttempt() {
        otpData.setAttemptCount(1);
        when(otpCache.getIfPresent(transactionId)).thenReturn(otpData);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(ValidationException.class, () -> {
            otpService.verifyOtp(transactionId, "wrong");
        });

        assertEquals(2, otpData.getAttemptCount());
    }

    @Test
    void testVerifyOtp_IncorrectOtp_ThirdAttempt_MaxReached() {
        otpData.setAttemptCount(2);
        when(otpCache.getIfPresent(transactionId)).thenReturn(otpData);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(transactionRepo.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepo.save(any(Transaction.class))).thenReturn(transaction);

        assertThrows(ValidationException.class, () -> {
            otpService.verifyOtp(transactionId, "wrong");
        });

        verify(transactionRepo).save(any(Transaction.class));
        verify(otpCache).invalidate(transactionId);
    }

    @Test
    void testVerifyOtp_TransactionNotFound() {
        when(otpCache.getIfPresent(transactionId)).thenReturn(otpData);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(transactionRepo.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            otpService.verifyOtp(transactionId, "123456");
        });
    }

    @Test
    void testVerifyOtp_RemainingAttemptsMessage() {
        when(otpCache.getIfPresent(transactionId)).thenReturn(otpData);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            otpService.verifyOtp(transactionId, "wrong");
        });

        assertTrue(exception.getMessage().contains("Attempts remaining: 2"));
    }
}

