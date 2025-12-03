package com.example.transaction_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Message DTO for Transaction Created Event
 * Consumed by Transaction Service from Wallet Service
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCreatedEvent {
    private UUID transactionId;
    private UUID userId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private Double amount;
    private String transactionType; // "CREDIT", "WITHDRAW", "TRANSFER"
    private String remarks;
    private String userEmail; // User email for sending OTP
    private Long timestamp;
}

