package com.example.wallet_service.Service;

import com.example.wallet_service.Client.UserServiceClient;
import com.example.wallet_service.DTO.*;
import com.example.wallet_service.Exceptions.DuplicateResourceException;
import com.example.wallet_service.Exceptions.ResourceNotFoundException;
import com.example.wallet_service.Exceptions.ValidationException;
import com.example.wallet_service.Model.Wallet;
import com.example.wallet_service.Repository.WalletRepo;
import com.example.wallet_service.Security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepo walletRepo;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @InjectMocks
    private WalletService walletService;

    private UUID userId;
    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUserId(userId);
        wallet.setWalletName("Test Wallet");
        wallet.setAccountNumber("1234567890");
        wallet.setBalance(1000.0);
        wallet.setPasscode("$2a$10$hashed");
        wallet.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testGetWallets_AsAdmin() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findAll()).thenReturn(Arrays.asList(wallet));

        List<WalletSummaryDTO> result = walletService.getWallets();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(walletId, result.get(0).getId());
        verify(walletRepo).findAll();
    }

    @Test
    void testGetWallets_AsUser() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));

        List<WalletSummaryDTO> result = walletService.getWallets();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(walletRepo).findByUserId(userId);
    }

    @Test
    void testGetWallets_NotAuthenticated() {
        when(securityUtil.getCurrentUserId()).thenReturn(null);

        assertThrows(ValidationException.class, () -> walletService.getWallets());
    }

    @Test
    void testGetWalletsWithBalance_AsAdmin() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findAll()).thenReturn(Arrays.asList(wallet));

        List<WalletDTO> result = walletService.getWalletsWithBalance();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(walletId, result.get(0).getId());
        assertEquals(1000.0, result.get(0).getBalance());
    }

    @Test
    void testGetWalletsWithBalance_AsUser() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));

        List<WalletDTO> result = walletService.getWalletsWithBalance();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetWalletById_Success() {
        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);

        WalletDTO result = walletService.getWalletById(walletId);

        assertNotNull(result);
        assertEquals(walletId, result.getId());
    }

    @Test
    void testGetWalletById_NotFound() {
        when(walletRepo.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.getWalletById(walletId));
    }

    @Test
    void testGetWalletById_NotAuthenticated() {
        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(null);

        assertThrows(ValidationException.class, () -> walletService.getWalletById(walletId));
    }

    @Test
    void testGetWalletById_AccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(otherUserId);
        when(securityUtil.isAdmin()).thenReturn(false);

        assertThrows(ValidationException.class, () -> walletService.getWalletById(walletId));
    }

    @Test
    void testGetWalletById_AsUser_OwnWallet() {
        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);

        WalletDTO result = walletService.getWalletById(walletId);

        assertNotNull(result);
        assertEquals(walletId, result.getId());
    }

    @Test
    void testGetWalletsWithBalance_NotAuthenticated() {
        when(securityUtil.getCurrentUserId()).thenReturn(null);

        assertThrows(ValidationException.class, () -> walletService.getWalletsWithBalance());
    }

    @Test
    void testCreateWallet_Success() {
        WalletCreateDTO dto = new WalletCreateDTO();
        dto.setUserId(userId);
        dto.setWalletName("New Wallet");
        dto.setAccountNumber("9876543210");
        dto.setBalance(500.0);
        dto.setPasscode("1234");

        when(userServiceClient.validateUser(userId)).thenReturn(true);
        when(walletRepo.existsByAccountNumber("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("$2a$10$hashed");
        when(walletRepo.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            w.setId(walletId);
            return w;
        });

        WalletDTO result = walletService.createWallet(dto);

        assertNotNull(result);
        verify(walletRepo).save(any(Wallet.class));
    }

    @Test
    void testCreateWallet_UserNotFound() {
        WalletCreateDTO dto = new WalletCreateDTO();
        dto.setUserId(userId);
        when(userServiceClient.validateUser(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> walletService.createWallet(dto));
    }

    @Test
    void testCreateWallet_DuplicateAccountNumber() {
        WalletCreateDTO dto = new WalletCreateDTO();
        dto.setUserId(userId);
        dto.setAccountNumber("1234567890");
        when(userServiceClient.validateUser(userId)).thenReturn(true);
        when(walletRepo.existsByAccountNumber("1234567890")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> walletService.createWallet(dto));
    }

    @Test
    void testCreateWallet_NegativeBalance() {
        WalletCreateDTO dto = new WalletCreateDTO();
        dto.setUserId(userId);
        dto.setWalletName("Test Wallet");
        dto.setAccountNumber("1234567890");
        dto.setBalance(-100.0);
        dto.setPasscode("1234");
        when(userServiceClient.validateUser(userId)).thenReturn(true);
        lenient().when(walletRepo.existsByAccountNumber(anyString())).thenReturn(false);

        assertThrows(ValidationException.class, () -> walletService.createWallet(dto));
    }

    @Test
    void testCreateWallet_InvalidPasscode() {
        WalletCreateDTO dto = new WalletCreateDTO();
        dto.setUserId(userId);
        dto.setWalletName("Test Wallet");
        dto.setAccountNumber("1234567890");
        dto.setBalance(100.0);
        dto.setPasscode("123");
        when(userServiceClient.validateUser(userId)).thenReturn(true);
        lenient().when(walletRepo.existsByAccountNumber(anyString())).thenReturn(false);

        assertThrows(ValidationException.class, () -> walletService.createWallet(dto));
    }

    @Test
    void testCreateWallet_NullPasscode() {
        WalletCreateDTO dto = new WalletCreateDTO();
        dto.setUserId(userId);
        dto.setWalletName("Test Wallet");
        dto.setAccountNumber("1234567890");
        dto.setBalance(100.0);
        dto.setPasscode(null);
        when(userServiceClient.validateUser(userId)).thenReturn(true);
        lenient().when(walletRepo.existsByAccountNumber(anyString())).thenReturn(false);

        assertThrows(ValidationException.class, () -> walletService.createWallet(dto));
    }

    @Test
    void testUpdateWallet_Success() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setNewWalletName("Updated Wallet");

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);

        WalletDTO result = walletService.updateWallet(dto);

        assertNotNull(result);
        verify(walletRepo).save(wallet);
    }

    @Test
    void testUpdateWallet_EmptyWalletName() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("");
        dto.setUserIdentifier(userId.toString());

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_NullWalletName() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName(null);
        dto.setUserIdentifier(userId.toString());

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_WhitespaceWalletName() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("   ");
        dto.setUserIdentifier(userId.toString());

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_NullUserIdentifier() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(null);

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_EmptyUserIdentifier() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier("");

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_WhitespaceUserIdentifier() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier("   ");

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_InvalidUserId() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier("invalid-uuid");

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_NotFound() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("NonExistent");
        dto.setUserIdentifier(userId.toString());

        when(walletRepo.findByUserId(userId)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_ChangeOwnership_AsAdmin() {
        UUID newUserId = UUID.randomUUID();
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setNewUserIdentifier(newUserId.toString());

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));
        when(userServiceClient.validateUser(newUserId)).thenReturn(true);
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);

        walletService.updateWallet(dto);

        verify(walletRepo).save(any(Wallet.class));
    }

    @Test
    void testUpdateWallet_ChangeOwnership_AsUser() {
        UUID newUserId = UUID.randomUUID();
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setNewUserIdentifier(newUserId.toString());

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_DuplicateAccountNumber() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setAccountNumber("9999999999");

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));
        when(walletRepo.existsByAccountNumber("9999999999")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_InvalidPasscode() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setPasscode("123");

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_AccountNumberSame() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setAccountNumber("1234567890"); // Same as wallet's account number

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);

        WalletDTO result = walletService.updateWallet(dto);

        assertNotNull(result);
        verify(walletRepo).save(wallet);
    }

    @Test
    void testUpdateWallet_UpdateBalance() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setBalance(2000.0);

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);

        WalletDTO result = walletService.updateWallet(dto);

        assertNotNull(result);
        verify(walletRepo).save(wallet);
    }

    @Test
    void testUpdateWallet_NewUserIdentifierInvalidFormat() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setNewUserIdentifier("invalid-uuid");

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));

        assertThrows(ValidationException.class, () -> walletService.updateWallet(dto));
    }

    @Test
    void testUpdateWallet_NewUserIdentifierEmptyString() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setNewUserIdentifier("");

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);

        WalletDTO result = walletService.updateWallet(dto);

        assertNotNull(result);
        verify(walletRepo).save(wallet);
    }

    @Test
    void testUpdateWallet_NewUserIdentifierWhitespace() {
        WalletUpdateDTO dto = new WalletUpdateDTO();
        dto.setWalletName("Test Wallet");
        dto.setUserIdentifier(userId.toString());
        dto.setNewUserIdentifier("   ");

        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));
        when(walletRepo.save(any(Wallet.class))).thenReturn(wallet);

        WalletDTO result = walletService.updateWallet(dto);

        assertNotNull(result);
        verify(walletRepo).save(wallet);
    }

    @Test
    void testDeleteWallet_Success() {
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.isAdmin()).thenReturn(true);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));

        walletService.deleteWallet("Test Wallet", userId.toString());

        verify(walletRepo).delete(wallet);
    }

    @Test
    void testDeleteWallet_EmptyWalletName() {
        assertThrows(ValidationException.class, () -> walletService.deleteWallet("", userId.toString()));
    }

    @Test
    void testDeleteWallet_InvalidUserId() {
        assertThrows(ValidationException.class, () -> walletService.deleteWallet("Test Wallet", "invalid"));
    }

    @Test
    void testDeleteWallet_NotFound() {
        when(walletRepo.findByUserId(userId)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> walletService.deleteWallet("Test Wallet", userId.toString()));
    }

    @Test
    void testDeleteWallet_AccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        when(securityUtil.getCurrentUserId()).thenReturn(otherUserId);
        when(securityUtil.isAdmin()).thenReturn(false);
        when(walletRepo.findByUserId(userId)).thenReturn(Arrays.asList(wallet));

        assertThrows(ValidationException.class, () -> walletService.deleteWallet("Test Wallet", userId.toString()));
    }

    @Test
    void testCreditWallet_Success() {
        CreditRequestDTO dto = new CreditRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.getCurrentUserEmail()).thenReturn("test@example.com");
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(true);
        doNothing().when(transactionEventPublisher).publishTransactionCreated(any(), any(), any(), any(), any(), any(), any(), any());

        UUID result = walletService.creditWallet(dto);

        assertNotNull(result);
        verify(transactionEventPublisher).publishTransactionCreated(any(), eq(userId), eq(walletId), eq(walletId), eq(100.0), eq("CREDIT"), any(), any());
    }

    @Test
    void testCreditWallet_InvalidAmount() {
        CreditRequestDTO dto = new CreditRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(0.0);

        assertThrows(ValidationException.class, () -> walletService.creditWallet(dto));
    }

    @Test
    void testCreditWallet_NullAmount() {
        CreditRequestDTO dto = new CreditRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(null);

        assertThrows(ValidationException.class, () -> walletService.creditWallet(dto));
    }

    @Test
    void testCreditWallet_UserEmailNull() {
        CreditRequestDTO dto = new CreditRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.getCurrentUserEmail()).thenReturn(null);
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(true);
        doNothing().when(transactionEventPublisher).publishTransactionCreated(any(), any(), any(), any(), any(), any(), any(), any());

        UUID result = walletService.creditWallet(dto);

        assertNotNull(result);
        verify(transactionEventPublisher).publishTransactionCreated(any(), eq(userId), eq(walletId), eq(walletId), eq(100.0), eq("CREDIT"), any(), eq("user@example.com"));
    }

    @Test
    void testCreditWallet_WalletNotFound() {
        CreditRequestDTO dto = new CreditRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(100.0);

        when(walletRepo.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.creditWallet(dto));
    }

    @Test
    void testCreditWallet_InvalidPasscode() {
        CreditRequestDTO dto = new CreditRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(false);

        assertThrows(ValidationException.class, () -> walletService.creditWallet(dto));
    }

    @Test
    void testWithdrawWallet_Success() {
        WithdrawalRequestDTO dto = new WithdrawalRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.getCurrentUserEmail()).thenReturn("test@example.com");
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(true);
        doNothing().when(transactionEventPublisher).publishTransactionCreated(any(), any(), any(), any(), any(), any(), any(), any());

        UUID result = walletService.withdrawWallet(dto);

        assertNotNull(result);
        verify(transactionEventPublisher).publishTransactionCreated(any(), eq(userId), eq(walletId), eq(walletId), eq(100.0), eq("WITHDRAW"), any(), any());
    }

    @Test
    void testWithdrawWallet_InsufficientBalance() {
        WithdrawalRequestDTO dto = new WithdrawalRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(2000.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(true);

        assertThrows(ValidationException.class, () -> walletService.withdrawWallet(dto));
    }

    @Test
    void testWithdrawWallet_NullAmount() {
        WithdrawalRequestDTO dto = new WithdrawalRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(null);

        assertThrows(ValidationException.class, () -> walletService.withdrawWallet(dto));
    }

    @Test
    void testWithdrawWallet_UserEmailNull() {
        WithdrawalRequestDTO dto = new WithdrawalRequestDTO();
        dto.setWalletId(walletId);
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.getCurrentUserEmail()).thenReturn(null);
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(true);
        doNothing().when(transactionEventPublisher).publishTransactionCreated(any(), any(), any(), any(), any(), any(), any(), any());

        UUID result = walletService.withdrawWallet(dto);

        assertNotNull(result);
        verify(transactionEventPublisher).publishTransactionCreated(any(), eq(userId), eq(walletId), eq(walletId), eq(100.0), eq("WITHDRAW"), any(), eq("user@example.com"));
    }

    @Test
    void testTransferFunds_Success() {
        UUID destWalletId = UUID.randomUUID();
        Wallet destWallet = new Wallet();
        destWallet.setId(destWalletId);
        destWallet.setUserId(UUID.randomUUID());

        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setSourceWalletId(walletId);
        dto.setDestinationWalletId(destWalletId);
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepo.findById(destWalletId)).thenReturn(Optional.of(destWallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.getCurrentUserEmail()).thenReturn("test@example.com");
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(true);
        doNothing().when(transactionEventPublisher).publishTransactionCreated(any(), any(), any(), any(), any(), any(), any(), any());

        UUID result = walletService.transferFunds(dto);

        assertNotNull(result);
        verify(transactionEventPublisher).publishTransactionCreated(any(), eq(userId), eq(walletId), eq(destWalletId), eq(100.0), eq("TRANSFER"), any(), any());
    }

    @Test
    void testTransferFunds_SameWallet() {
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setSourceWalletId(walletId);
        dto.setDestinationWalletId(walletId);
        dto.setAmount(100.0);

        assertThrows(ValidationException.class, () -> walletService.transferFunds(dto));
    }

    @Test
    void testTransferFunds_DestinationNotFound() {
        UUID destWalletId = UUID.randomUUID();
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setSourceWalletId(walletId);
        dto.setDestinationWalletId(destWalletId);
        dto.setAmount(100.0);

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepo.findById(destWalletId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.transferFunds(dto));
    }

    @Test
    void testTransferFunds_InsufficientBalance() {
        UUID destWalletId = UUID.randomUUID();
        Wallet destWallet = new Wallet();
        destWallet.setId(destWalletId);

        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setSourceWalletId(walletId);
        dto.setDestinationWalletId(destWalletId);
        dto.setAmount(2000.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepo.findById(destWalletId)).thenReturn(Optional.of(destWallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(true);

        assertThrows(ValidationException.class, () -> walletService.transferFunds(dto));
    }

    @Test
    void testTransferFunds_NullAmount() {
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setSourceWalletId(walletId);
        dto.setDestinationWalletId(UUID.randomUUID());
        dto.setAmount(null);

        assertThrows(ValidationException.class, () -> walletService.transferFunds(dto));
    }

    @Test
    void testTransferFunds_UserEmailNull() {
        UUID destWalletId = UUID.randomUUID();
        Wallet destWallet = new Wallet();
        destWallet.setId(destWalletId);

        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setSourceWalletId(walletId);
        dto.setDestinationWalletId(destWalletId);
        dto.setAmount(100.0);
        dto.setPasscode("1234");

        when(walletRepo.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepo.findById(destWalletId)).thenReturn(Optional.of(destWallet));
        when(securityUtil.getCurrentUserId()).thenReturn(userId);
        when(securityUtil.getCurrentUserEmail()).thenReturn(null);
        when(passwordEncoder.matches("1234", "$2a$10$hashed")).thenReturn(true);
        doNothing().when(transactionEventPublisher).publishTransactionCreated(any(), any(), any(), any(), any(), any(), any(), any());

        UUID result = walletService.transferFunds(dto);

        assertNotNull(result);
        verify(transactionEventPublisher).publishTransactionCreated(any(), eq(userId), eq(walletId), eq(destWalletId), eq(100.0), eq("TRANSFER"), any(), eq("user@example.com"));
    }

    @Test
    void testDeleteWallet_NullWalletName() {
        assertThrows(ValidationException.class, () -> walletService.deleteWallet(null, userId.toString()));
    }

    @Test
    void testDeleteWallet_WhitespaceWalletName() {
        assertThrows(ValidationException.class, () -> walletService.deleteWallet("   ", userId.toString()));
    }

    @Test
    void testDeleteWallet_NullUserIdentifier() {
        assertThrows(ValidationException.class, () -> walletService.deleteWallet("Test Wallet", null));
    }

    @Test
    void testDeleteWallet_WhitespaceUserIdentifier() {
        assertThrows(ValidationException.class, () -> walletService.deleteWallet("Test Wallet", "   "));
    }
}

