package com.example.transaction_service.Service;

import com.example.transaction_service.DTO.PaginatedResponse;
import com.example.transaction_service.DTO.TransactionDTO;
import com.example.transaction_service.Model.Transaction;
import com.example.transaction_service.Repository.TransactionRepo;
import com.example.transaction_service.Security.SecurityUtil;
import com.example.transaction_service.Client.WalletServiceClient;
import com.example.transaction_service.Client.UserServiceClient;
import com.example.transaction_service.Exceptions.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private WalletServiceClient walletServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private StatementCsvBuilder statementCsvBuilder;

    @Autowired
    private EmailService emailService;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    public PaginatedResponse<TransactionDTO> getTransactions(String type, Pageable pageable) {
        Pageable validatedPageable = validateAndAdjustPageable(pageable);

        String normalizedType = (type == null || type.trim().isEmpty()) 
            ? "all" 
            : type.trim().toLowerCase();
        
        Page<Transaction> transactionPage;
        switch (normalizedType) {
            case "credits":
                transactionPage = filterTransactionsByRemarkContains("credit transaction", validatedPageable);
                break;
                
            case "withdrawals":
                transactionPage = filterTransactionsByRemarkContains("withdrawal transaction", validatedPageable);
                break;
                
            case "transfers":
                transactionPage = filterTransactionsByRemarkContains("transfer", validatedPageable);
                break;
                
            case "failed":
                transactionPage = filterFailedTransactions(validatedPageable);
                break;
                
            case "all":
            default:
                transactionPage = fetchTransactionsForCurrentUser(validatedPageable);
                break;
        }
        
        return convertToPaginatedResponse(transactionPage);
    }

    private Page<Transaction> filterTransactionsByRemarkContains(String remark, Pageable pageable) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        if (securityUtil.isAdmin()) {
            return transactionRepo.findAllByRemarkContaining(remark, pageable);
        } else {
            // Get wallet IDs for the user
            List<UUID> walletIds = getWalletIdsForUser(currentUserId);
            return transactionRepo.findByWalletIdsAndRemarkContaining(walletIds, remark, pageable);
        }
    }
    
    private Page<Transaction> filterFailedTransactions(Pageable pageable) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        if (securityUtil.isAdmin()) {
            return transactionRepo.findAllFailedTransactions(pageable);
        } else {
            // Get wallet IDs for the user
            List<UUID> walletIds = getWalletIdsForUser(currentUserId);
            return transactionRepo.findFailedTransactionsByWalletIds(walletIds, pageable);
        }
    }

    private Page<Transaction> fetchTransactionsForCurrentUser(Pageable pageable) {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        if (securityUtil.isAdmin()) {
            return transactionRepo.findAll(pageable);
        }
        
        // Get wallet IDs for the user
        List<UUID> walletIds = getWalletIdsForUser(currentUserId);
        return transactionRepo.findByWalletIds(walletIds, pageable);
    }

    private List<UUID> getWalletIdsForUser(UUID userId) {
        List<WalletServiceClient.WalletDTO> wallets = walletServiceClient.getUserWallets(userId);
        return wallets.stream()
                .map(WalletServiceClient.WalletDTO::getId)
                .collect(Collectors.toList());
    }

    private PaginatedResponse<TransactionDTO> convertToPaginatedResponse(Page<Transaction> transactionPage) {
        List<TransactionDTO> content = transactionPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PaginatedResponse<TransactionDTO> response = new PaginatedResponse<>();
        response.setContent(content);
        response.setTotalElements(transactionPage.getTotalElements());
        response.setTotalPages(transactionPage.getTotalPages());
        response.setCurrentPage(transactionPage.getNumber());
        response.setPageSize(transactionPage.getSize());
        response.setHasNext(transactionPage.hasNext());
        response.setHasPrevious(transactionPage.hasPrevious());
        response.setFirst(transactionPage.isFirst());
        response.setLast(transactionPage.isLast());

        return response;
    }

    private Pageable validateAndAdjustPageable(Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();

        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        } else if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        if (pageNumber < 0) {
            pageNumber = 0;
        }

        return PageRequest.of(pageNumber, pageSize, pageable.getSort());
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setSenderWalletId(transaction.getSenderWalletId());
        dto.setReceiverWalletId(transaction.getReceiverWalletId());
        dto.setAmount(transaction.getAmount());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setStatus(transaction.getStatus());
        dto.setRemarks(transaction.getRemarks());
        return dto;
    }

    public void generateAndEmailStatement() throws jakarta.mail.MessagingException {
        UUID currentUserId = securityUtil.getCurrentUserId();

        if (currentUserId == null) {
            throw new ValidationException("User not authenticated");
        }

        // Fetch user details from User Service
        UserServiceClient.UserDTO user = userServiceClient.getUserDetails(currentUserId);
        if (user == null) {
            throw new ValidationException("User not found");
        }

        // Fetch wallet IDs for the user
        List<UUID> walletIds = getWalletIdsForUser(currentUserId);
        
        // Fetch transactions
        List<Transaction> transactions = transactionRepo.findAllByWalletIds(walletIds);
        
        // Fetch wallet details from Wallet Service
        List<WalletServiceClient.WalletDTO> wallets = walletServiceClient.getUserWallets(currentUserId);

        // Build CSV statement using DTOs
        byte[] csvBytes = statementCsvBuilder.buildStatementCsv(user, transactions, wallets);

        emailService.sendStatementEmail(
                user.getEmail(),
                user.getName(),
                csvBytes
        );
    }
}
