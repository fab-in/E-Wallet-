package com.example.transaction_service.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import com.example.transaction_service.DTO.OtpData;
import com.example.transaction_service.Model.Transaction;
import com.example.transaction_service.Repository.TransactionRepo;
import com.example.transaction_service.Exceptions.ResourceNotFoundException;
import com.example.transaction_service.Exceptions.ValidationException;
import java.util.Random;
import java.util.UUID;

@Service
public class OtpService {

    private static final Logger logger = LogManager.getLogger(OtpService.class);

    @Autowired
    private Cache<UUID, OtpData> otpCache;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionEventPublisher transactionEventPublisher;

    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 3;

    public String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public void createAndSendOtp(UUID transactionId, UUID userId, String userEmail, String transactionType) {
        String otpCode = generateOtp();
        String hashedOtp = passwordEncoder.encode(otpCode);

        OtpData otpData = new OtpData();
        otpData.setTransactionId(transactionId);
        otpData.setHashedOtpCode(hashedOtp);
        otpData.setUserId(userId);
        otpData.setUserEmail(userEmail);
        otpData.setTransactionType(transactionType);
        otpData.setAttemptCount(0);
        otpData.setIsVerified(false);

        otpCache.put(transactionId, otpData);
        
        sendOtpEmail(userEmail, otpCode, transactionType);
        logger.info("OTP generated and stored in cache for transaction: {}", transactionId);
    }

    public boolean verifyOtp(UUID transactionId, String enteredOtp) {
        OtpData otpData = otpCache.getIfPresent(transactionId);
        
        if (otpData == null) {
            throw new ResourceNotFoundException(
                    "OTP not found or expired for transaction: " + transactionId);
        }

        if (otpData.getIsVerified()) {
            throw new ValidationException("OTP has already been verified for this transaction.");
        }

        if (otpData.getAttemptCount() >= MAX_ATTEMPTS) {
            markTransactionAsFailed(transactionId, "Wrong OTP");
            otpCache.invalidate(transactionId); // Remove from cache
            throw new ValidationException("Transaction has failed. Maximum OTP verification attempts exceeded.");
        }

        if (passwordEncoder.matches(enteredOtp, otpData.getHashedOtpCode())) {
            otpData.setIsVerified(true);
            otpCache.put(transactionId, otpData);

            Transaction transaction = transactionRepo.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

            transactionEventPublisher.publishOtpVerified(
                transactionId,
                otpData.getUserId(),
                transaction.getSenderWalletId(),
                transaction.getReceiverWalletId(),
                transaction.getAmount(),
                otpData.getTransactionType()
            );
            
            return true;
        } else {
            otpData.setAttemptCount(otpData.getAttemptCount() + 1);
            otpCache.put(transactionId, otpData);

            if (otpData.getAttemptCount() >= MAX_ATTEMPTS) {
                markTransactionAsFailed(transactionId, "Wrong OTP");
                otpCache.invalidate(transactionId);
                throw new ValidationException("Transaction has failed. Maximum OTP verification attempts exceeded.");
            }

            int remainingAttempts = MAX_ATTEMPTS - otpData.getAttemptCount();
            throw new ValidationException("Incorrect OTP. Attempts remaining: " + remainingAttempts);
        }
    }

    private void markTransactionAsFailed(UUID transactionId, String remark) {
        Transaction transaction = transactionRepo.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        transaction.setStatus("FAILED");
        transaction.setRemarks(remark);
        transactionRepo.save(transaction);
    }

    private void sendOtpEmail(String toEmail, String otpCode, String transactionType) {
        try {
            String subject = "E-Wallet Transaction OTP - " + transactionType;
            String emailBody = buildOtpEmailBody(otpCode, transactionType);
            emailService.sendSimpleEmail(toEmail, subject, emailBody);
        } catch (Exception e) {
            logger.error("Failed to send OTP email: {}", e.getMessage(), e);
        }
    }

    private String buildOtpEmailBody(String otpCode, String transactionType) {
        return "E-Wallet Transaction OTP\n\n" +
                "Dear User,\n\n" +
                "You have initiated a " + transactionType + " transaction on your E-Wallet account.\n\n" +
                "Please use the following OTP to complete your transaction:\n\n" +
                otpCode + "\n\n" +
                "Important:\n" +
                "- This OTP is valid for 10 minutes only\n" +
                "- Do not share this OTP with anyone\n" +
                "- You have 3 attempts to enter the correct OTP\n\n" +
                "If you did not initiate this transaction, please ignore this email or contact support immediately.\n\n"
                +
                "This is an automated email. Please do not reply.";
    }
}
