package com.example.wallet_service.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletCreateDTO {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Wallet name is required")
    private String walletName;
    
    @NotBlank(message = "Account number is required")
    private String accountNumber;
    
    @Min(value = 0, message = "Balance must be 0 or greater")
    @NotNull(message = "Balance is required")
    private Double balance;
    
    @NotBlank(message = "Passcode is required")
    @Pattern(regexp = "^\\d{4}$", message = "Passcode must be exactly 4 digits")
    private String passcode;
}

