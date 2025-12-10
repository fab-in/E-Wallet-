package com.example.transaction_service.Controllers;

import com.example.transaction_service.DTO.MessageResponseDTO;
import com.example.transaction_service.DTO.OtpVerificationDTO;
import com.example.transaction_service.DTO.PaginatedResponse;
import com.example.transaction_service.DTO.TransactionDTO;
import com.example.transaction_service.Exceptions.ValidationException;
import com.example.transaction_service.Service.OtpService;
import com.example.transaction_service.Service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private TransactionController transactionController;

    private UUID transactionId;
    private OtpVerificationDTO otpVerificationDTO;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        otpVerificationDTO = new OtpVerificationDTO();
        otpVerificationDTO.setTransactionId(transactionId);
        otpVerificationDTO.setOtp("123456");
    }

    @Test
    void testGetTransactions_DefaultParams() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", 0, 20, null, null
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(transactionService).getTransactions(eq("all"), any());
    }

    @Test
    void testGetTransactions_WithType() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "credits", 0, 20, null, null
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(eq("credits"), any());
    }

    @Test
    void testGetTransactions_WithOrderOldest() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", 0, 20, null, "oldest"
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(anyString(), any());
    }

    @Test
    void testGetTransactions_WithOrderNewest() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", 0, 20, null, "newest"
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(anyString(), any());
    }

    @Test
    void testGetTransactions_WithSortField() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", 0, 20, "transactionDate,desc", null
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(anyString(), any());
    }

    @Test
    void testGetTransactions_WithSortFieldAsc() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", 0, 20, "transactionDate,asc", null
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(anyString(), any());
    }

    @Test
    void testGetTransactions_NegativePage() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", -1, 20, null, null
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(anyString(), any());
    }

    @Test
    void testGetTransactions_ZeroSize() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", 0, 0, null, null
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(anyString(), any());
    }

    @Test
    void testGetStatement_Success() throws Exception {
        doNothing().when(transactionService).generateAndEmailStatement();

        ResponseEntity<MessageResponseDTO> result = transactionController.getStatement();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Transaction statement has been sent to your registered email address", 
            result.getBody().getMessage());
    }

    @Test
    void testGetStatement_EmailException() throws Exception {
        doThrow(new jakarta.mail.MessagingException("Email error")).when(transactionService).generateAndEmailStatement();

        ResponseEntity<MessageResponseDTO> result = transactionController.getStatement();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Failed to send email", result.getBody().getMessage());
    }

    @Test
    void testVerifyOtp_Success() {
        when(otpService.verifyOtp(any(UUID.class), anyString())).thenReturn(true);

        ResponseEntity<Map<String, String>> result = transactionController.verifyOtp(otpVerificationDTO);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("success", result.getBody().get("status"));
        assertEquals("OTP verified successfully. Transaction is being processed.", result.getBody().get("message"));
    }

    @Test
    void testVerifyOtp_Failed() {
        when(otpService.verifyOtp(any(UUID.class), anyString())).thenReturn(false);

        ResponseEntity<Map<String, String>> result = transactionController.verifyOtp(otpVerificationDTO);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("failed", result.getBody().get("status"));
        assertEquals("OTP verification failed", result.getBody().get("message"));
    }

    @Test
    void testVerifyOtp_ValidationException_TransactionFailed() {
        when(otpService.verifyOtp(any(UUID.class), anyString()))
            .thenThrow(new ValidationException("Transaction has failed. Maximum OTP verification attempts exceeded."));

        ResponseEntity<Map<String, String>> result = transactionController.verifyOtp(otpVerificationDTO);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("failed", result.getBody().get("status"));
        assertTrue(result.getBody().get("message").contains("Transaction has failed"));
    }

    @Test
    void testVerifyOtp_ValidationException_Other() {
        when(otpService.verifyOtp(any(UUID.class), anyString()))
            .thenThrow(new ValidationException("Incorrect OTP"));

        ResponseEntity<Map<String, String>> result = transactionController.verifyOtp(otpVerificationDTO);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("error", result.getBody().get("status"));
        assertEquals("Incorrect OTP", result.getBody().get("message"));
    }

    @Test
    void testGetTransactions_EmptySort() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", 0, 20, "", null
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(anyString(), any());
    }

    @Test
    void testGetTransactions_InvalidSortFormat() {
        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        when(transactionService.getTransactions(anyString(), any())).thenReturn(response);

        ResponseEntity<PaginatedResponse<TransactionDTO>> result = transactionController.getTransactions(
            "all", 0, 20, "invalid", null
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(transactionService).getTransactions(anyString(), any());
    }
}

