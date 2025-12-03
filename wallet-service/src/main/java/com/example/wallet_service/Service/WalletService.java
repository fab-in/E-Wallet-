package com.example.wallet_service.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.wallet_service.Repository.WalletRepo;
import com.example.wallet_service.Model.Wallet;
import com.example.wallet_service.DTO.WalletDTO;
import com.example.wallet_service.DTO.WalletCreateDTO;
import com.example.wallet_service.DTO.WalletUpdateDTO;
import com.example.wallet_service.DTO.CreditRequestDTO;
import com.example.wallet_service.DTO.WithdrawalRequestDTO;
import com.example.wallet_service.DTO.TransferRequestDTO;
import com.example.wallet_service.DTO.WalletSummaryDTO;
import com.example.wallet_service.Exceptions.DuplicateResourceException;
import com.example.wallet_service.Exceptions.ResourceNotFoundException;
import com.example.wallet_service.Exceptions.ValidationException;
import com.example.wallet_service.Security.SecurityUtil;
import com.example.wallet_service.Client.UserServiceClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletService {

    @Autowired
    private WalletRepo walletRepo;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private TransactionEventPublisher transactionEventPublisher;

    /**
     * Get all wallets for the current user, or all wallets if admin
     */
    public List<WalletSummaryDTO> getWallets() {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        List<Wallet> wallets;

        if (securityUtil.isAdmin()) {
            wallets = walletRepo.findAll();
        } else {
            wallets = walletRepo.findByUserId(currentUserId);
        }

        return wallets.stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all wallets with balance for the current user, or all wallets if admin
     * Used for internal services like statement generation
     */
    public List<WalletDTO> getWalletsWithBalance() {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        List<Wallet> wallets;

        if (securityUtil.isAdmin()) {
            wallets = walletRepo.findAll();
        } else {
            wallets = walletRepo.findByUserId(currentUserId);
        }

        return wallets.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get wallet by ID
     */
    public WalletDTO getWalletById(UUID id) {
        Wallet wallet = walletRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + id));

        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        if (!securityUtil.isAdmin()) {
            if (!wallet.getUserId().equals(currentUserId)) {
                throw new ValidationException("Access denied: You can only access your own wallets");
            }
        }

        return convertToDTO(wallet);
    }

    /**
     * Create a new wallet
     */
    public WalletDTO createWallet(WalletCreateDTO walletCreateDTO) {
        // Validate user exists in User Service
        if (!userServiceClient.validateUser(walletCreateDTO.getUserId())) {
            throw new ResourceNotFoundException("User not found with id: " + walletCreateDTO.getUserId());
        }

        if (walletRepo.existsByAccountNumber(walletCreateDTO.getAccountNumber())) {
            throw new DuplicateResourceException(
                    "Account number '" + walletCreateDTO.getAccountNumber() + "' already exists");
        }

        if (walletCreateDTO.getBalance() < 0) {
            throw new ValidationException("Balance must be 0 or greater");
        }

        if (walletCreateDTO.getPasscode() == null || !walletCreateDTO.getPasscode().matches("^\\d{4}$")) {
            throw new ValidationException("Passcode must be exactly 4 digits");
        }

        Wallet wallet = convertToEntity(walletCreateDTO);
        wallet.setCreatedAt(LocalDateTime.now());

        Wallet savedWallet = walletRepo.save(wallet);
        return convertToDTO(savedWallet);
    }

    /**
     * Update wallet - simplified to work with UUID userId
     */
    public WalletDTO updateWallet(WalletUpdateDTO walletUpdateDTO) {
        if (walletUpdateDTO.getWalletName() == null || walletUpdateDTO.getWalletName().trim().isEmpty()) {
            throw new ValidationException("Wallet name is required");
        }

        if (walletUpdateDTO.getUserIdentifier() == null || walletUpdateDTO.getUserIdentifier().trim().isEmpty()) {
            throw new ValidationException("User identifier (userId) is required");
        }

        // Parse userId from userIdentifier
        UUID userId;
        try {
            userId = UUID.fromString(walletUpdateDTO.getUserIdentifier());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid user identifier format. Expected UUID.");
        }

        // Find wallet by name and userId
        List<Wallet> userWallets = walletRepo.findByUserId(userId);
        Wallet wallet = userWallets.stream()
                .filter(w -> w.getWalletName().equals(walletUpdateDTO.getWalletName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found with name '" + walletUpdateDTO.getWalletName() + "' for user '" + userId + "'"));

        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        boolean isAdmin = securityUtil.isAdmin();

        if (!isAdmin) {
            if (!wallet.getUserId().equals(currentUserId)) {
                throw new ValidationException("Access denied: You can only update your own wallets");
            }
        }

        // Update wallet ownership if admin and newUserId provided
        if (walletUpdateDTO.getNewUserIdentifier() != null
                && !walletUpdateDTO.getNewUserIdentifier().trim().isEmpty()) {
            if (!isAdmin) {
                throw new ValidationException("You cannot change wallet ownership");
            }
            UUID newUserId;
            try {
                newUserId = UUID.fromString(walletUpdateDTO.getNewUserIdentifier());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid new user identifier format. Expected UUID.");
            }
            if (!userServiceClient.validateUser(newUserId)) {
                throw new ResourceNotFoundException("User not found with id: " + newUserId);
            }
            wallet.setUserId(newUserId);
        }

        if (walletUpdateDTO.getNewWalletName() != null && !walletUpdateDTO.getNewWalletName().trim().isEmpty()) {
            wallet.setWalletName(walletUpdateDTO.getNewWalletName());
        }

        if (walletUpdateDTO.getAccountNumber() != null && !walletUpdateDTO.getAccountNumber().trim().isEmpty()) {
            if (!walletUpdateDTO.getAccountNumber().equals(wallet.getAccountNumber()) &&
                    walletRepo.existsByAccountNumber(walletUpdateDTO.getAccountNumber())) {
                throw new DuplicateResourceException(
                        "Account number '" + walletUpdateDTO.getAccountNumber() + "' already exists");
            }
            wallet.setAccountNumber(walletUpdateDTO.getAccountNumber());
        }

        if (walletUpdateDTO.getBalance() != null) {
            if (walletUpdateDTO.getBalance() < 0) {
                throw new ValidationException("Balance must be 0 or greater");
            }
            wallet.setBalance(walletUpdateDTO.getBalance());
        }

        if (walletUpdateDTO.getPasscode() != null && !walletUpdateDTO.getPasscode().trim().isEmpty()) {
            if (!walletUpdateDTO.getPasscode().matches("^\\d{4}$")) {
                throw new ValidationException("Passcode must be exactly 4 digits");
            }
            // Hash the passcode before saving
            String hashedPasscode = passwordEncoder.encode(walletUpdateDTO.getPasscode());
            wallet.setPasscode(hashedPasscode);
        }

        Wallet updatedWallet = walletRepo.save(wallet);
        return convertToDTO(updatedWallet);
    }

    /**
     * Delete wallet
     */
    public void deleteWallet(String walletName, String userIdentifier) {
        if (walletName == null || walletName.trim().isEmpty()) {
            throw new ValidationException("Wallet name cannot be null or empty");
        }

        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            throw new ValidationException("User identifier (userId) cannot be null or empty");
        }

        // Parse userId
        UUID userId;
        try {
            userId = UUID.fromString(userIdentifier);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid user identifier format. Expected UUID.");
        }

        // Find wallet
        List<Wallet> userWallets = walletRepo.findByUserId(userId);
        Wallet wallet = userWallets.stream()
                .filter(w -> w.getWalletName().equals(walletName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found with name '" + walletName + "' for user '" + userId + "'"));

        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        if (!securityUtil.isAdmin()) {
            if (!wallet.getUserId().equals(currentUserId)) {
                throw new ValidationException("Access denied: You can only delete your own wallets");
            }
        }

        walletRepo.delete(wallet);
    }

    /**
     * Credit wallet - Uses RabbitMQ to create transaction
     */
    public UUID creditWallet(CreditRequestDTO creditRequestDTO) {
        UUID walletId = creditRequestDTO.getWalletId();
        Double amount = creditRequestDTO.getAmount();

        if (amount == null || amount <= 0) {
            throw new ValidationException("Amount must be greater than 0");
        }

        Wallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found with id: " + walletId));

        UUID currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        if (!wallet.getUserId().equals(currentUserId)) {
            throw new ValidationException("Access denied: You can only credit your own wallets");
        }

        // Verify entered passcode against hashed passcode in database
        if (!passwordEncoder.matches(creditRequestDTO.getPasscode(), wallet.getPasscode())) {
            throw new ValidationException("Invalid passcode");
        }

        // Generate transaction ID
        UUID transactionId = UUID.randomUUID();
        
        // Get user email from header (set by Gateway)
        String userEmail = securityUtil.getCurrentUserEmail();
        if (userEmail == null) {
            userEmail = "user@example.com"; // Fallback - should be set by Gateway
        }

        // Publish transaction created event to RabbitMQ
        transactionEventPublisher.publishTransactionCreated(
            transactionId,
            currentUserId,
            walletId,
            walletId, // sender and receiver are same for credit
            amount,
            "CREDIT",
            "Credit transaction",
            userEmail
        );

        return transactionId;
    }

    /**
     * Withdraw from wallet - Uses RabbitMQ to create transaction
     */
    public UUID withdrawWallet(WithdrawalRequestDTO withdrawalRequestDTO) {
        UUID walletId = withdrawalRequestDTO.getWalletId();
        Double amount = withdrawalRequestDTO.getAmount();

        if (amount == null || amount <= 0) {
            throw new ValidationException("Amount must be greater than 0");
        }

        Wallet wallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found with id: " + walletId));

        UUID currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        if (!wallet.getUserId().equals(currentUserId)) {
            throw new ValidationException("Access denied: You can only withdraw from your own wallets");
        }

        // Verify entered passcode against hashed passcode in database
        if (!passwordEncoder.matches(withdrawalRequestDTO.getPasscode(), wallet.getPasscode())) {
            throw new ValidationException("Invalid passcode");
        }

        if (wallet.getBalance() < amount) {
            throw new ValidationException("Insufficient balance");
        }

        // Generate transaction ID
        UUID transactionId = UUID.randomUUID();

        // Get user email from header (set by Gateway)
        String userEmail = securityUtil.getCurrentUserEmail();
        if (userEmail == null) {
            userEmail = "user@example.com"; // Fallback - should be set by Gateway
        }

        // Publish transaction created event to RabbitMQ
        transactionEventPublisher.publishTransactionCreated(
            transactionId,
            currentUserId,
            walletId,
            walletId, // sender and receiver are same for withdraw
            amount,
            "WITHDRAW",
            "Withdrawal transaction",
            userEmail
        );

        return transactionId;
    }

    /**
     * Transfer funds between wallets - Uses RabbitMQ to create transaction
     */
    public UUID transferFunds(TransferRequestDTO transferRequestDTO) {
        UUID sourceWalletId = transferRequestDTO.getSourceWalletId();
        UUID destinationWalletId = transferRequestDTO.getDestinationWalletId();
        Double amount = transferRequestDTO.getAmount();

        if (amount == null || amount <= 0) {
            throw new ValidationException("Amount must be greater than 0");
        }

        if (sourceWalletId.equals(destinationWalletId)) {
            throw new ValidationException("Source and destination wallets cannot be the same");
        }

        Wallet sourceWallet = walletRepo.findById(sourceWalletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source wallet not found with id: " + sourceWalletId));

        // Validate destination wallet exists
        walletRepo.findById(destinationWalletId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination wallet not found with id: " + destinationWalletId));

        UUID currentUserId = securityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        if (!sourceWallet.getUserId().equals(currentUserId)) {
            throw new ValidationException("Access denied: You can only transfer from your own wallets");
        }

        // Verify entered passcode against hashed passcode in database
        if (!passwordEncoder.matches(transferRequestDTO.getPasscode(), sourceWallet.getPasscode())) {
            throw new ValidationException("Invalid passcode");
        }

        if (sourceWallet.getBalance() < amount) {
            throw new ValidationException("Insufficient balance");
        }

        // Generate transaction ID
        UUID transactionId = UUID.randomUUID();

        // Get user email from header (set by Gateway)
        String userEmail = securityUtil.getCurrentUserEmail();
        if (userEmail == null) {
            userEmail = "user@example.com"; // Fallback - should be set by Gateway
        }

        // Publish transaction created event to RabbitMQ
        transactionEventPublisher.publishTransactionCreated(
            transactionId,
            currentUserId,
            sourceWalletId,
            destinationWalletId,
            amount,
            "TRANSFER",
            "Fund transfer",
            userEmail
        );

        return transactionId;
    }

    /**
     * Convert Wallet entity to DTO
     */
    private WalletDTO convertToDTO(Wallet wallet) {
        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setId(wallet.getId());
        walletDTO.setUserId(wallet.getUserId());
        walletDTO.setWalletName(wallet.getWalletName());
        walletDTO.setAccountNumber(wallet.getAccountNumber());
        walletDTO.setBalance(wallet.getBalance());
        walletDTO.setCreatedAt(wallet.getCreatedAt());
        return walletDTO;
    }

    /**
     * Convert Wallet entity to summary DTO (no balance)
     */
    private WalletSummaryDTO convertToSummaryDTO(Wallet wallet) {
        WalletSummaryDTO summaryDTO = new WalletSummaryDTO();
        summaryDTO.setId(wallet.getId());
        summaryDTO.setUserId(wallet.getUserId());
        summaryDTO.setWalletName(wallet.getWalletName());
        summaryDTO.setAccountNumber(wallet.getAccountNumber());
        summaryDTO.setCreatedAt(wallet.getCreatedAt());
        return summaryDTO;
    }

    /**
     * Convert DTO to Wallet entity
     */
    private Wallet convertToEntity(WalletCreateDTO walletCreateDTO) {
        Wallet wallet = new Wallet();
        wallet.setUserId(walletCreateDTO.getUserId());
        wallet.setWalletName(walletCreateDTO.getWalletName());
        wallet.setAccountNumber(walletCreateDTO.getAccountNumber());
        wallet.setBalance(walletCreateDTO.getBalance());
        // Hash the passcode before saving
        String hashedPasscode = passwordEncoder.encode(walletCreateDTO.getPasscode());
        wallet.setPasscode(hashedPasscode);
        return wallet;
    }
}
