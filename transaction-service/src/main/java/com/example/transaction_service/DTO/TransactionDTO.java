package com.example.transaction_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {
    private UUID id;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private double amount;
    private LocalDateTime transactionDate;
    private String status;
    private String remarks;
}

