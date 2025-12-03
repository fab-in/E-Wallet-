package com.example.wallet_service.DTO;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletSummaryDTO {
    private UUID id;
    private UUID userId;
    private String walletName;
    private String accountNumber;
    private LocalDateTime createdAt;
}


