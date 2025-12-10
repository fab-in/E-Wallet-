package com.example.transaction_service.Service;

import com.example.transaction_service.Client.UserServiceClient;
import com.example.transaction_service.Client.WalletServiceClient;
import com.example.transaction_service.Model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StatementCsvBuilderTest {

    @InjectMocks
    private StatementCsvBuilder statementCsvBuilder;

    private UserServiceClient.UserDTO userDTO;
    private WalletServiceClient.WalletDTO walletDTO;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        userDTO = new UserServiceClient.UserDTO();
        userDTO.setId(userId);
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");

        walletDTO = new WalletServiceClient.WalletDTO();
        walletDTO.setId(walletId);
        walletDTO.setUserId(userId);
        walletDTO.setWalletName("Main Wallet");
        walletDTO.setAccountNumber("1234567890");

        transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setSenderWalletId(walletId);
        transaction.setReceiverWalletId(UUID.randomUUID());
        transaction.setAmount(100.50);
        transaction.setTransactionDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        transaction.setStatus("SUCCESS");
        transaction.setRemarks("Test transaction");
    }

    @Test
    void testBuildStatementCsv_Success() {
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Test User"));
        assertTrue(csvContent.contains("Main Wallet"));
        assertTrue(csvContent.contains("1234567890"));
        assertTrue(csvContent.contains("100.50"));
        assertTrue(csvContent.contains("SUCCESS"));
    }

    @Test
    void testBuildStatementCsv_NullUser() {
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(null, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Account Holder"));
    }

    @Test
    void testBuildStatementCsv_EmptyTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Test User"));
    }

    @Test
    void testBuildStatementCsv_NullWallets() {
        List<Transaction> transactions = Arrays.asList(transaction);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, null);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Test User"));
    }

    @Test
    void testBuildStatementCsv_EmptyWallets() {
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = new ArrayList<>();

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Test User"));
    }

    @Test
    void testBuildStatementCsv_MultipleTransactions() {
        Transaction transaction2 = new Transaction();
        transaction2.setId(UUID.randomUUID());
        transaction2.setSenderWalletId(UUID.randomUUID());
        transaction2.setReceiverWalletId(UUID.randomUUID());
        transaction2.setAmount(200.75);
        transaction2.setTransactionDate(LocalDateTime.of(2024, 1, 16, 11, 0));
        transaction2.setStatus("SUCCESS");
        transaction2.setRemarks("Second transaction");

        List<Transaction> transactions = Arrays.asList(transaction, transaction2);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("100.50"));
        assertTrue(csvContent.contains("200.75"));
    }

    @Test
    void testBuildStatementCsv_FailedTransaction() {
        transaction.setStatus("FAILED");
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("FAILED"));
        assertFalse(csvContent.contains("100.50")); 
    }

    @Test
    void testBuildStatementCsv_WalletWithoutName() {
        walletDTO.setWalletName(null);
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("1234567890"));
    }

    @Test
    void testBuildStatementCsv_WalletWithoutAccountNumber() {
        walletDTO.setAccountNumber(null);
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Main Wallet"));
    }

    @Test
    void testBuildStatementCsv_NullTransactionFields() {
        transaction.setRemarks(null);
        transaction.setTransactionDate(null);
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testBuildStatementCsv_StatusNormalization() {
        transaction.setStatus("success");
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("SUCCESS"));
    }

    @Test
    void testBuildStatementCsv_StatusWithFail() {
        transaction.setStatus("transaction failed");
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("FAILED"));
    }

    @Test
    void testBuildStatementCsv_NullStatus() {
        transaction.setStatus(null);
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testBuildStatementCsv_RemarksWithComma() {
        transaction.setRemarks("Test, transaction with comma");
        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("\"Test, transaction with comma\""));
    }

    @Test
    void testBuildStatementCsv_MultipleWallets() {
        WalletServiceClient.WalletDTO walletDTO2 = new WalletServiceClient.WalletDTO();
        walletDTO2.setWalletName("Savings Wallet");
        walletDTO2.setAccountNumber("9876543210");

        List<Transaction> transactions = Arrays.asList(transaction);
        List<WalletServiceClient.WalletDTO> wallets = Arrays.asList(walletDTO, walletDTO2);

        byte[] result = statementCsvBuilder.buildStatementCsv(userDTO, transactions, wallets);

        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("Main Wallet"));
        assertTrue(csvContent.contains("Savings Wallet"));
    }
}

