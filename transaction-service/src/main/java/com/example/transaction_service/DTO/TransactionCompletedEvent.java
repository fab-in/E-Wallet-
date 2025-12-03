package com.example.transaction_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Message DTO for Transaction Completed Event
 * Consumed by Transaction Service from Wallet Service
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCompletedEvent {
    private UUID transactionId;
    private String status; // "SUCCESS" or "FAILED"
    private String remarks;
    private Long timestamp;
}

