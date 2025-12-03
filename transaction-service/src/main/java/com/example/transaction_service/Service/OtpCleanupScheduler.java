package com.example.transaction_service.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.transaction_service.Repository.TransactionRepo;
import com.example.transaction_service.Model.Transaction;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class OtpCleanupScheduler {

    private static final Logger logger = LogManager.getLogger(OtpCleanupScheduler.class);

    @Autowired
    private TransactionRepo transactionRepo;

    @Scheduled(fixedRate = 120000)
    public void cleanupExpiredPendingTransactions() {
        try {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            
            List<Transaction> expiredTransactions = transactionRepo.findAll().stream()
                    .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()))
                    .filter(t -> t.getTransactionDate() != null && t.getTransactionDate().isBefore(fiveMinutesAgo))
                    .toList();

            if (!expiredTransactions.isEmpty()) {
                logger.info("Cleaning up expired pending transactions");
                
                for (Transaction transaction : expiredTransactions) {
                    transaction.setStatus("FAILED");
                    transaction.setRemarks("Transaction expired - OTP not verified within 5 minutes");
                    transactionRepo.save(transaction);
                    logger.debug("Marked transaction {} as failed due to expiration", transaction.getId());
                }
            }
        } catch (Exception e) {
            logger.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }
}

