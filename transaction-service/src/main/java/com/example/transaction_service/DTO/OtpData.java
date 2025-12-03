package com.example.transaction_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpData {
    private UUID transactionId;
    private String hashedOtpCode;
    private UUID userId;
    private String userEmail;
    private String transactionType;
    private Integer attemptCount = 0;
    private Boolean isVerified = false;
}

