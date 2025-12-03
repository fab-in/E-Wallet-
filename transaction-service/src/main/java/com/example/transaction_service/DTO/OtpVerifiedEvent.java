package com.example.transaction_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Message DTO for OTP Verified Event
 * Published by Transaction Service when OTP is verified
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpVerifiedEvent {
    private UUID transactionId;
    private UUID userId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private Double amount;
    private String transactionType; // "CREDIT", "WITHDRAW", "TRANSFER"
    private Long timestamp;
}

