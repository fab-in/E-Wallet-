package com.example.wallet_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;


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

