package com.example.transaction_service.Service;

import com.example.transaction_service.Client.UserServiceClient;
import com.example.transaction_service.Client.WalletServiceClient;
import com.example.transaction_service.DTO.PaginatedResponse;
import com.example.transaction_service.DTO.TransactionDTO;
import com.example.transaction_service.Exceptions.ValidationException;
import com.example.transaction_service.Model.Transaction;
import com.example.transaction_service.Repository.TransactionRepo;
import com.example.transaction_service.Security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepo transactionRepo;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private StatementCsvBuilder statementCsvBuilder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TransactionService transactionService;

    private UUID userId;
    private UUID walletId1;
    private UUID walletId2;
    private UUID transactionId;
    private Transaction transaction;
    private WalletServiceClient.WalletDTO walletDTO;
    private UserServiceClient.UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletId1 = UUID.randomUUID();
        walletId2 = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setSenderWalletId(walletId1);
        transaction.setReceiverWalletId(walletId2);
        transaction.setAmount(100.0);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("SUCCESS");
        transaction.setRemarks("Test transaction");

        walletDTO = new WalletServiceClient.WalletDTO();
        walletDTO.setId(walletId1);
        walletDTO.setUserId(userId);

        userDTO = new UserServiceClient.UserDTO();
        userDTO.setId(userId);
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");
    }

    @Test
    void testGetTransactions_AllType_User() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction));
        when(transactionRepo.findByWalletIds(anyList(), any(Pageable.class))).thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("all", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(transactionId, result.getContent().get(0).getId());
        verify(transactionRepo).findByWalletIds(anyList(), any(Pageable.class));
    }

    @Test
    void testGetTransactions_AllType_Admin() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction));
        when(transactionRepo.findAll(any(Pageable.class))).thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("all", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepo).findAll(any(Pageable.class));
        verify(transactionRepo, never()).findByWalletIds(anyList(), any(Pageable.class));
    }

    @Test
    void testGetTransactions_Credits_User() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Transaction creditTransaction = new Transaction();
        creditTransaction.setId(transactionId);
        creditTransaction.setRemarks("credit transaction");
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(creditTransaction));
        when(transactionRepo.findByWalletIdsAndRemarkContaining(anyList(), eq("credit transaction"), any(Pageable.class)))
                .thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("credits", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepo).findByWalletIdsAndRemarkContaining(anyList(), eq("credit transaction"), any(Pageable.class));
    }

    @Test
    void testGetTransactions_Credits_Admin() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        
        Transaction creditTransaction = new Transaction();
        creditTransaction.setId(transactionId);
        creditTransaction.setRemarks("credit transaction");
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(creditTransaction));
        when(transactionRepo.findAllByRemarkContaining(eq("credit transaction"), any(Pageable.class)))
                .thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("credits", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepo).findAllByRemarkContaining(eq("credit transaction"), any(Pageable.class));
    }

    @Test
    void testGetTransactions_Withdrawals_User() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Transaction withdrawalTransaction = new Transaction();
        withdrawalTransaction.setId(transactionId);
        withdrawalTransaction.setRemarks("withdrawal transaction");
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(withdrawalTransaction));
        when(transactionRepo.findByWalletIdsAndRemarkContaining(anyList(), eq("withdrawal transaction"), any(Pageable.class)))
                .thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("withdrawals", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepo).findByWalletIdsAndRemarkContaining(anyList(), eq("withdrawal transaction"), any(Pageable.class));
    }

    @Test
    void testGetTransactions_Transfers_User() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Transaction transferTransaction = new Transaction();
        transferTransaction.setId(transactionId);
        transferTransaction.setRemarks("transfer transaction");
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transferTransaction));
        when(transactionRepo.findByWalletIdsAndRemarkContaining(anyList(), eq("transfer"), any(Pageable.class)))
                .thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("transfers", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepo).findByWalletIdsAndRemarkContaining(anyList(), eq("transfer"), any(Pageable.class));
    }

    @Test
    void testGetTransactions_Failed_User() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Transaction failedTransaction = new Transaction();
        failedTransaction.setId(transactionId);
        failedTransaction.setStatus("FAILED");
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(failedTransaction));
        when(transactionRepo.findFailedTransactionsByWalletIds(anyList(), any(Pageable.class)))
                .thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("failed", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepo).findFailedTransactionsByWalletIds(anyList(), any(Pageable.class));
    }

    @Test
    void testGetTransactions_Failed_Admin() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        
        Transaction failedTransaction = new Transaction();
        failedTransaction.setId(transactionId);
        failedTransaction.setStatus("FAILED");
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(failedTransaction));
        when(transactionRepo.findAllFailedTransactions(any(Pageable.class)))
                .thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("failed", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepo).findAllFailedTransactions(any(Pageable.class));
    }

    @Test
    void testGetTransactions_DefaultCase() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction));
        when(transactionRepo.findByWalletIds(anyList(), any(Pageable.class))).thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("unknown", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(transactionRepo).findByWalletIds(anyList(), any(Pageable.class));
    }

    @Test
    void testGetTransactions_NullType() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction));
        when(transactionRepo.findByWalletIds(anyList(), any(Pageable.class))).thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions(null, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testGetTransactions_EmptyType() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction));
        when(transactionRepo.findByWalletIds(anyList(), any(Pageable.class))).thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("   ", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testGetTransactions_UserNotAuthenticated() {
        when(securityUtil.getCurrentUserId()).thenReturn(null);

        assertThrows(ValidationException.class, () -> {
            transactionService.getTransactions("all", PageRequest.of(0, 20));
        });
    }

    @Test
    void testGetTransactions_PageSizeTooLarge() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction));
        when(transactionRepo.findByWalletIds(anyList(), any(Pageable.class))).thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("all", PageRequest.of(0, 200));

        assertNotNull(result);
        verify(transactionRepo).findByWalletIds(anyList(), argThat(pageable -> pageable.getPageSize() == 100));
    }

    @Test
    void testGetTransactions_PageSizeTooSmall() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction));
        when(transactionRepo.findByWalletIds(anyList(), any(Pageable.class))).thenReturn(transactionPage);

        Pageable invalidPageable = new Pageable() {
            @Override
            public int getPageNumber() { return 0; }
            @Override
            public int getPageSize() { return 0; }
            @Override
            public long getOffset() { return 0; }
            @Override
            public Sort getSort() { return Sort.unsorted(); }
            @Override
            public Pageable next() { return null; }
            @Override
            public Pageable previousOrFirst() { return null; }
            @Override
            public Pageable first() { return null; }
            @Override
            public Pageable withPage(int pageNumber) { return null; }
            @Override
            public boolean hasPrevious() { return false; }
        };

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("all", invalidPageable);

        assertNotNull(result);
        verify(transactionRepo).findByWalletIds(anyList(), argThat(pageable -> pageable.getPageSize() == 20));
    }

    @Test
    void testGetTransactions_NegativePageNumber() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Page<Transaction> transactionPage = new PageImpl<>(Arrays.asList(transaction));
        when(transactionRepo.findByWalletIds(anyList(), any(Pageable.class))).thenReturn(transactionPage);

        Pageable invalidPageable = new Pageable() {
            @Override
            public int getPageNumber() { return -1; }
            @Override
            public int getPageSize() { return 20; }
            @Override
            public long getOffset() { return 0; }
            @Override
            public Sort getSort() { return Sort.unsorted(); }
            @Override
            public Pageable next() { return null; }
            @Override
            public Pageable previousOrFirst() { return null; }
            @Override
            public Pageable first() { return null; }
            @Override
            public Pageable withPage(int pageNumber) { return null; }
            @Override
            public boolean hasPrevious() { return false; }
        };

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("all", invalidPageable);

        assertNotNull(result);
        verify(transactionRepo).findByWalletIds(anyList(), argThat(pageable -> pageable.getPageNumber() == 0));
    }

    @Test
    void testGenerateAndEmailStatement_Success() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(userServiceClient.getUserDetails(userId)).thenReturn(userDTO);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        when(transactionRepo.findAllByWalletIds(anyList())).thenReturn(Arrays.asList(transaction));
        when(statementCsvBuilder.buildStatementCsv(any(), anyList(), anyList())).thenReturn("csv content".getBytes());
        doNothing().when(emailService).sendStatementEmail(anyString(), anyString(), any(byte[].class));

        assertDoesNotThrow(() -> transactionService.generateAndEmailStatement());

        verify(emailService).sendStatementEmail(eq("test@example.com"), eq("Test User"), any(byte[].class));
    }

    @Test
    void testGenerateAndEmailStatement_UserNotAuthenticated() {
        when(securityUtil.getCurrentUserId()).thenReturn(null);

        assertThrows(ValidationException.class, () -> {
            transactionService.generateAndEmailStatement();
        });
    }

    @Test
    void testGenerateAndEmailStatement_UserNotFound() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(userServiceClient.getUserDetails(userId)).thenReturn(null);

        assertThrows(ValidationException.class, () -> {
            transactionService.generateAndEmailStatement();
        });
    }

    @Test
    void testGetTransactions_PaginatedResponseFields() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletServiceClient.getUserWallets(userId)).thenReturn(Arrays.asList(walletDTO));
        
        Page<Transaction> transactionPage = new PageImpl<>(
            Arrays.asList(transaction),
            PageRequest.of(0, 20),
            100L
        );
        when(transactionRepo.findByWalletIds(anyList(), any(Pageable.class))).thenReturn(transactionPage);

        PaginatedResponse<TransactionDTO> result = transactionService.getTransactions("all", PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(100L, result.getTotalElements());
        assertEquals(5, result.getTotalPages());
        assertEquals(0, result.getCurrentPage());
        assertEquals(20, result.getPageSize());
        assertTrue(result.isHasNext());
        assertFalse(result.isHasPrevious());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());
    }
}

