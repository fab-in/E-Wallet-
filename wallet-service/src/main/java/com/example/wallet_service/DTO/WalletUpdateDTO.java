package com.example.wallet_service.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletUpdateDTO {
    
    @NotNull(message = "Wallet name is required")
    private String walletName;
    
    @NotNull(message = "User identifier (name or email) is required")
    private String userIdentifier;
    
    private String newWalletName;
    
    private String accountNumber;
    
    @Min(value = 0, message = "Balance must be 0 or greater")
    private Double balance;
    
    @Pattern(regexp = "^\\d{4}$", message = "Passcode must be exactly 4 digits")
    private String passcode;
    
    private String newUserIdentifier;
}

