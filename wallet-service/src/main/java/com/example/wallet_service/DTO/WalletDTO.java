package com.example.wallet_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletDTO {
    private UUID id;
    private UUID userId;
    private String walletName;
    private String accountNumber;
    private Double balance;
    private LocalDateTime createdAt;
}

